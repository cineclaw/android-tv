package com.cineclaw.tv.core.network

import android.util.Log
import com.cineclaw.tv.core.model.DiscoveredServer
import com.cineclaw.tv.core.model.HealthResponse
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object ServerDiscovery {
    private const val TAG = "ServerDiscovery"
    private const val UDP_PORT = 9118
    private const val DEFAULT_HTTP_PORT = 3000

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val probeClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(600, TimeUnit.MILLISECONDS)
            .readTimeout(800, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(false)
            .build()
    }

    /**
     * Finds active CineClaw servers on the local LAN using both UDP broadcast and fast subnet HTTP sweep.
     */
    suspend fun discoverServers(
        customClient: OkHttpClient? = null,
        knownUrls: List<String> = emptyList()
    ): List<DiscoveredServer> = withContext(Dispatchers.IO) {
        val client = customClient ?: probeClient
        val foundMap = ConcurrentHashMap<String, DiscoveredServer>()

        val localPrefixes = getLocalSubnetPrefixes()
        Log.d(TAG, "Local subnet prefixes detected: $localPrefixes")

        coroutineScope {
            // 1. Fast UDP Broadcast Probe (port 9118)
            launch {
                runCatching {
                    probeUdpBroadcast(foundMap)
                }.onFailure {
                    Log.w(TAG, "UDP discovery error: ${it.message}")
                }
            }

            // 2. Check well-known URLs first for instant discovery
            for (rawUrl in knownUrls) {
                if (rawUrl.isBlank()) continue
                launch {
                    val normalized = normalizeUrl(rawUrl)
                    if (normalized != null && !foundMap.containsKey(normalized)) {
                        checkHttpHealth(client, normalized)?.let { srv ->
                            foundMap[srv.url] = srv
                        }
                    }
                }
            }

            // 3. Fast Subnet Sweep on port 3000
            for (prefix in localPrefixes) {
                launch {
                    sweepSubnet(client, prefix, foundMap)
                }
            }
        }

        foundMap.values.toList().sortedByDescending { it.isOnline }
    }

    /**
     * Sends UDP discovery datagram and listens for responder beacons.
     */
    private fun probeUdpBroadcast(foundMap: ConcurrentHashMap<String, DiscoveredServer>) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket().apply {
                broadcast = true
                soTimeout = 900
            }

            val payload = "{\"cmd\":\"discover\"}".toByteArray(Charsets.UTF_8)
            val broadcastAddr = InetAddress.getByName("255.255.255.255")
            val packet = DatagramPacket(payload, payload.size, broadcastAddr, UDP_PORT)
            socket.send(packet)

            val receiveBuf = ByteArray(2048)
            val receivePacket = DatagramPacket(receiveBuf, receiveBuf.size)

            val deadline = System.currentTimeMillis() + 1000
            while (System.currentTimeMillis() < deadline) {
                try {
                    socket.receive(receivePacket)
                    val senderIp = receivePacket.address.hostAddress ?: continue
                    val text = String(receivePacket.data, 0, receivePacket.length, Charsets.UTF_8).trim()

                    Log.d(TAG, "Received UDP response from $senderIp: $text")
                    val health = runCatching { json.decodeFromString<HealthResponse>(text) }.getOrNull()
                    val srvUrl = "http://$senderIp:$DEFAULT_HTTP_PORT"
                    val server = DiscoveredServer(
                        name = health?.name?.ifBlank { "CineClaw ($senderIp)" } ?: "CineClaw ($senderIp)",
                        url = srvUrl,
                        version = health?.version,
                        isOnline = true
                    )
                    foundMap[srvUrl] = server
                } catch (e: SocketTimeoutException) {
                    break
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "UDP probe exception: ${e.message}")
        } finally {
            runCatching { socket?.close() }
        }
    }

    /**
     * Fast subnet sweep on port 3000 /api/health with concurrency limit.
     */
    private suspend fun sweepSubnet(
        client: OkHttpClient,
        prefix: String,
        foundMap: ConcurrentHashMap<String, DiscoveredServer>
    ) = coroutineScope {
        val semaphore = Semaphore(35)
        val jobs = (1..254).map { host ->
            launch {
                semaphore.withPermit {
                    val targetUrl = "http://$prefix$host:$DEFAULT_HTTP_PORT"
                    if (!foundMap.containsKey(targetUrl)) {
                        checkHttpHealth(client, targetUrl)?.let { srv ->
                            Log.i(TAG, "Discovered server at $targetUrl (${srv.name})")
                            foundMap[srv.url] = srv
                        }
                    }
                }
            }
        }
        jobs.joinAll()
    }

    /**
     * Probes /api/health of a target server.
     */
    suspend fun checkHttpHealth(client: OkHttpClient, baseUrl: String): DiscoveredServer? =
        withContext(Dispatchers.IO) {
            val cleanUrl = baseUrl.trimEnd('/')
            val endpoint = "$cleanUrl/api/health"
            try {
                val req = Request.Builder()
                    .url(endpoint)
                    .get()
                    .build()

                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string().orEmpty()
                        val health = runCatching { json.decodeFromString<HealthResponse>(body) }.getOrNull()
                        if (health?.service == "cineclaw" || health?.status == "ok") {
                            return@withContext DiscoveredServer(
                                name = health.name.ifBlank { "CineClaw" },
                                url = cleanUrl,
                                version = health.version.takeIf { it.isNotBlank() },
                                isOnline = true
                            )
                        }
                    }
                }
                null
            } catch (e: Exception) {
                null
            }
        }

    /**
     * Determines local subnet prefixes like "192.168.88." from active IPv4 network interfaces.
     */
    private fun getLocalSubnetPrefixes(): List<String> {
        val prefixes = mutableSetOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
            for (iface in interfaces) {
                if (!iface.isUp || iface.isLoopback || iface.isVirtual) continue
                for (addr in iface.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress ?: continue
                        val parts = host.split(".")
                        if (parts.size == 4) {
                            prefixes.add("${parts[0]}.${parts[1]}.${parts[2]}.")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read network interfaces: ${e.message}")
        }
        return prefixes.toList()
    }

    private fun normalizeUrl(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        val full = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "http://$trimmed"
        } else {
            trimmed
        }
        return full.trimEnd('/')
    }
}
