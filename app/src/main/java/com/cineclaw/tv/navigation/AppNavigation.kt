package com.cineclaw.tv.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cineclaw.tv.CineClawApp
import com.cineclaw.tv.core.model.*
import com.cineclaw.tv.core.player.CinemaPlayer
import com.cineclaw.tv.feature.auth.AuthScreen
import com.cineclaw.tv.feature.catalog.CatalogScreen
import com.cineclaw.tv.feature.details.DetailsScreen
import com.cineclaw.tv.feature.home.HomeScreen
import com.cineclaw.tv.feature.home.HomeViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import com.cineclaw.tv.feature.person.PersonScreen
import com.cineclaw.tv.feature.player.PlayerScreen
import com.cineclaw.tv.feature.player.ResumeConfirmDialog
import com.cineclaw.tv.core.designsystem.ObsidianBackground
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import com.cineclaw.tv.feature.search.SearchScreen
import com.cineclaw.tv.feature.settings.SettingsScreen
import com.cineclaw.tv.feature.shelf.ShelfScreen
import com.cineclaw.tv.feature.watchlist.WatchlistScreen
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder

import com.cineclaw.tv.feature.downloads.DownloadsScreen
import com.cineclaw.tv.core.download.DownloadManager
import com.cineclaw.tv.core.download.DownloadService
import com.cineclaw.tv.core.download.OfflineMetadata

private fun safeDecode(str: String?): String {
    if (str.isNullOrBlank()) return ""
    return runCatching { URLDecoder.decode(str, "UTF-8") }.getOrDefault(str.replace('+', ' '))
}

private data class PendingResumeState(
    val playerInfo: PlayerInfoResponse,
    val fullStreamUrl: String,
    val resolvedQuality: String
)

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Home : Screen("home")
    object Details : Screen("details/{tconst}?id={id}&type={type}&title={title}&poster={poster}&backdrop={backdrop}&overview={overview}&year={year}&rating={rating}") {
        fun createRoute(
            tconst: String,
            id: Long? = null,
            type: String? = null,
            title: String? = null,
            poster: String? = null,
            backdrop: String? = null,
            overview: String? = null,
            year: Int? = null,
            rating: Double? = null
        ): String {
            val enc = { s: String? -> if (!s.isNullOrBlank()) URLEncoder.encode(s, "UTF-8") else "" }
            val realId = id?.takeIf { it > 0L } ?: tconst.removePrefix("tmdb_").toLongOrNull() ?: 0L
            val cleanTconst = tconst.trim()
            val base = if (cleanTconst.isNotBlank() && cleanTconst != "tmdb_0") cleanTconst else "tmdb_$realId"
            return "details/$base?id=$realId&type=${enc(type ?: "movie")}&title=${enc(title ?: "")}&poster=${enc(poster ?: "")}&backdrop=${enc(backdrop ?: "")}&overview=${enc(overview ?: "")}&year=${year ?: 0}&rating=${rating ?: 0.0}"
        }
    }
    object Player : Screen("player/{tconst}?title={title}&season={season}&episode={episode}&quality={quality}") {
        fun createRoute(tconst: String, title: String, season: Int? = null, episode: Int? = null, quality: String? = null): String {
            val encTitle = URLEncoder.encode(title, "UTF-8")
            val encQuality = if (!quality.isNullOrBlank()) URLEncoder.encode(quality, "UTF-8") else ""
            val s = season ?: -1
            val e = episode ?: -1
            return "player/$tconst?title=$encTitle&season=$s&episode=$e&quality=$encQuality"
        }
    }
    object Person : Screen("person/{personId}") {
        fun createRoute(personId: Long): String = "person/$personId"
    }
    object Shelf : Screen("shelf/{shelfId}?title={title}&type={type}") {
        fun createRoute(shelfId: String, title: String, type: String? = null): String {
            val encTitle = URLEncoder.encode(title, "UTF-8")
            val encType = if (!type.isNullOrBlank()) URLEncoder.encode(type, "UTF-8") else ""
            return "shelf/$shelfId?title=$encTitle&type=$encType"
        }
    }
    object Catalog : Screen("catalog/{type}") {
        fun createRoute(type: String): String = "catalog/$type"
    }
    object Watchlist : Screen("watchlist")
    object Downloads : Screen("downloads")
    object Search : Screen("search")
    object Settings : Screen("settings")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val app = CineClawApp.instance
    val scope = rememberCoroutineScope()

    val currentServerUrl by app.sessionManager.serverUrl.collectAsState(initial = "http://192.168.88.19:3000")
    val currentUsername by app.sessionManager.username.collectAsState(initial = null)
    val isAudioPassthrough by app.sessionManager.audioPassthrough.collectAsState(initial = false)
    val preferredQuality by app.sessionManager.preferredQuality.collectAsState(initial = "4k")
    val serverUrl = currentServerUrl

    val currentToken by app.sessionManager.authToken.collectAsState(initial = null)
    var hasCheckedAuth by remember { mutableStateOf(false) }
    var initialIsAuthenticated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val token = app.sessionManager.authToken.first()
        initialIsAuthenticated = !token.isNullOrBlank()
        hasCheckedAuth = true
    }

    LaunchedEffect(hasCheckedAuth, currentToken) {
        if (hasCheckedAuth && currentToken.isNullOrBlank()) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute != null && currentRoute != Screen.Auth.route) {
                navController.navigate(Screen.Auth.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    if (!hasCheckedAuth) {
        // Initial splash loading
        return
    }

    val startDestination = if (initialIsAuthenticated) Screen.Home.route else Screen.Auth.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(animationSpec = tween(150)) },
        exitTransition = { fadeOut(animationSpec = tween(150)) },
        popEnterTransition = { fadeIn(animationSpec = tween(150)) },
        popExitTransition = { fadeOut(animationSpec = tween(150)) }
    ) {
        composable(Screen.Auth.route) {
            var authError by remember { mutableStateOf<String?>(null) }
            var isLoggingIn by remember { mutableStateOf(false) }

            AuthScreen(
                currentServerUrl = currentServerUrl,
                onConnectManual = { url, username, password ->
                    scope.launch {
                        isLoggingIn = true
                        authError = null
                        val (success, errorMsg) = app.apiClient.loginWithResult(username, password, url)
                        isLoggingIn = false
                        if (success) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Auth.route) { inclusive = true }
                            }
                        } else {
                            authError = errorMsg ?: "Не удалось войти"
                        }
                    }
                },
                onPairSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                errorMessage = authError,
                isLoggingIn = isLoggingIn,
                onCheckPing = { url ->
                    app.apiClient.pingServer(url)
                },
                onPollPairing = { code ->
                    val resp = app.apiClient.checkPairingStatus(code)
                    resp?.paired == true
                }
            )
        }

        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = viewModel()

            LaunchedEffect(Unit) {
                homeViewModel.loadData(app)
            }

            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                homeViewModel.refreshWatchState(app)
            }

            HomeScreen(
                featuredItems = homeViewModel.featured,
                continueWatchingItems = homeViewModel.continueWatching,
                watchlistItems = homeViewModel.watchlistItems,
                trackerFreshItems = homeViewModel.trackerFresh,
                trackerHotlistItems = homeViewModel.trackerHotlist,
                uhd4kItems = homeViewModel.uhd4k,
                shelves = homeViewModel.shelves,
                isLoading = homeViewModel.isLoading,
                errorMessage = homeViewModel.errorMessage,
                lastFocusedCardKey = homeViewModel.lastFocusedCardKey,
                onCardFocused = { homeViewModel.lastFocusedCardKey = it },
                onRetry = { homeViewModel.loadData(app, forceRefresh = true) },
                onSelectMedia = { media ->
                    navController.navigate(
                        Screen.Details.createRoute(
                            tconst = media.effectiveTconst,
                            id = media.effectiveId,
                            type = media.mediaType ?: if (media.isTv) "tv" else "movie",
                            title = media.displayTitle,
                            poster = media.effectivePoster,
                            backdrop = media.effectiveBackdrop,
                            overview = media.overview,
                            year = media.year,
                            rating = media.rating
                        )
                    )
                },
                onResumeClick = { resume ->
                    val isTv = resume.season > 0
                    navController.navigate(
                        Screen.Details.createRoute(
                            tconst = resume.effectiveTconst,
                            type = if (isTv) "tv" else "movie",
                            title = resume.displayTitle,
                            poster = resume.effectivePoster,
                            backdrop = resume.effectiveBackdrop,
                            year = resume.year
                        )
                    )
                    navController.navigate(
                        Screen.Player.createRoute(
                            tconst = resume.effectiveTconst,
                            title = resume.displayTitle,
                            season = resume.season.takeIf { it > 0 },
                            episode = resume.episode.takeIf { it > 0 }
                        )
                    )
                },
                onOpenShelf = { shelfId, title ->
                    navController.navigate(Screen.Shelf.createRoute(shelfId, title))
                },
                onNavigateNavRail = { route ->
                    when (route) {
                        "search" -> navController.navigate(Screen.Search.route)
                        "settings" -> navController.navigate(Screen.Settings.route)
                        "movies" -> navController.navigate(Screen.Catalog.createRoute("movies"))
                        "series" -> navController.navigate(Screen.Catalog.createRoute("series"))
                        "4k" -> navController.navigate(Screen.Catalog.createRoute("4k"))
                        "watchlist" -> navController.navigate(Screen.Watchlist.route)
                        "downloads" -> navController.navigate(Screen.Downloads.route)
                    }
                },
                onDeleteResumeItem = { resume ->
                    homeViewModel.removeFromContinueWatching(app, resume.effectiveTconst)
                },
                onToggleWatchlist = { media ->
                    val inWl = homeViewModel.watchlistItems.any { it.effectiveTconst == media.effectiveTconst }
                    if (inWl) {
                        homeViewModel.removeFromWatchlist(app, media.effectiveTconst)
                    } else {
                        homeViewModel.addToWatchlist(app, media)
                    }
                },
                onRemoveFromWatchlist = { media ->
                    homeViewModel.removeFromWatchlist(app, media.effectiveTconst)
                }
            )
        }

        composable(
            route = Screen.Details.route,
            arguments = listOf(
                navArgument("tconst") { type = NavType.StringType },
                navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                navArgument("type") { type = NavType.StringType; defaultValue = "movie" },
                navArgument("title") { type = NavType.StringType; defaultValue = "" },
                navArgument("poster") { type = NavType.StringType; defaultValue = "" },
                navArgument("backdrop") { type = NavType.StringType; defaultValue = "" },
                navArgument("overview") { type = NavType.StringType; defaultValue = "" },
                navArgument("year") { type = NavType.IntType; defaultValue = 0 },
                navArgument("rating") { type = NavType.StringType; defaultValue = "0.0" }
            )
        ) { backStackEntry ->
            val tconstArg = backStackEntry.arguments?.getString("tconst") ?: ""
            val idArg = backStackEntry.arguments?.getLong("id") ?: 0L
            val typeArg = backStackEntry.arguments?.getString("type") ?: "movie"
            val titleArg = safeDecode(backStackEntry.arguments?.getString("title"))
            val posterArg = backStackEntry.arguments?.getString("poster") ?: ""
            val backdropArg = backStackEntry.arguments?.getString("backdrop") ?: ""
            val overviewArg = safeDecode(backStackEntry.arguments?.getString("overview"))
            val yearArg = backStackEntry.arguments?.getInt("year")?.takeIf { it > 0 }
            val ratingArg = backStackEntry.arguments?.getString("rating")?.toDoubleOrNull()?.takeIf { it > 0.0 }

            val effectiveId = idArg.takeIf { it > 0L } ?: tconstArg.removePrefix("tmdb_").toLongOrNull()
            val isTv = typeArg.contains("tv", ignoreCase = true) || typeArg.contains("series", ignoreCase = true)
            val resolveType = if (isTv) "tv" else "movie"

            val initialItem = MediaItem(
                tconst = if (tconstArg.startsWith("tt")) tconstArg else "",
                id = effectiveId,
                title = titleArg,
                ruTitle = titleArg,
                posterUrl = posterArg.takeIf { it.isNotBlank() },
                backdropUrl = backdropArg.takeIf { it.isNotBlank() },
                overview = overviewArg.takeIf { it.isNotBlank() },
                year = yearArg,
                rating = ratingArg,
                mediaType = resolveType,
                type = if (isTv) "TvSeries" else "Movie"
            )

            var media by remember { mutableStateOf(initialItem) }
            var qualityGroups by remember { mutableStateOf<List<QualityGroup>>(emptyList()) }
            var seasons by remember { mutableStateOf<List<Int>>(emptyList()) }
            var episodes by remember { mutableStateOf<List<EpisodeInfo>>(emptyList()) }
            var cast by remember { mutableStateOf<List<CastMember>>(emptyList()) }
            var crew by remember { mutableStateOf<List<CrewMember>>(emptyList()) }
            var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
            var backdrops by remember { mutableStateOf<List<String>>(emptyList()) }
            var criticSummary by remember { mutableStateOf<CriticSummaryResponse?>(null) }
            var isLoadingCritics by remember { mutableStateOf(false) }
            var isLoadingMetadata by remember { mutableStateOf(true) }
            var isInWatchlist by remember { mutableStateOf(false) }
            var seriesProgress by remember { mutableStateOf<SeriesProgressResponse?>(null) }
            var isRefreshingTorrents by remember { mutableStateOf(false) }

            val onRefreshTorrents: () -> Unit = {
                scope.launch {
                    isRefreshingTorrents = true
                    try {
                        val api = app.apiClient.getApi()
                        val torrentQuery = media.ruTitle?.ifEmpty { media.title } ?: media.title
                        val torrentType = if (media.isTv) "tv" else "movie"
                        val realTconst = media.effectiveTconst
                        val torrents = api.getTorrents(
                            imdbId = realTconst.takeIf { it.isNotBlank() },
                            query = torrentQuery.takeIf { it.isNotBlank() },
                            type = torrentType,
                            refreshCache = true
                        )
                        qualityGroups = groupTorrents(torrents)
                    } catch (e: Exception) {
                        android.util.Log.e("CineClaw", "onRefreshTorrents failed", e)
                    } finally {
                        isRefreshingTorrents = false
                    }
                }
            }

            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                if (media.isTv && media.effectiveTconst.isNotBlank()) {
                    scope.launch {
                        try {
                            val api = app.apiClient.getApi()
                            seriesProgress = api.getSeriesProgress(media.effectiveTconst)
                        } catch (e: Exception) {}
                    }
                }
            }

            LaunchedEffect(tconstArg, idArg) {
                isLoadingMetadata = true
                try {
                    val api = app.apiClient.getApi()
                    var realTconst = if (tconstArg.startsWith("tt")) tconstArg else ""
                    val lookupId = effectiveId ?: 0L
                    if (realTconst.isEmpty() && lookupId > 0L) {
                        try {
                            val resolved = api.resolveTmdbMovie(resolveType, lookupId)
                            if (resolved.tconst.isNotBlank()) {
                                realTconst = resolved.tconst
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("CineClaw", "resolveTmdbMovie failed: ${e.message}")
                        }
                    }

                    if (realTconst.isNotBlank()) {
                        media = media.copy(tconst = realTconst)
                        // 1. Metadata
                        try {
                            val meta = api.getMovieMetadata(realTconst)
                            cast = meta.cast
                            crew = meta.crew
                            videos = meta.videos
                            backdrops = meta.backdrops
                            media = media.copy(
                                title = meta.title.ifEmpty { media.title },
                                ruTitle = meta.title.ifEmpty { media.ruTitle },
                                overview = meta.overview ?: media.overview,
                                year = meta.year ?: media.year,
                                rating = meta.rating ?: media.rating,
                                posterPath = meta.posterPath ?: media.posterPath,
                                backdropPath = meta.backdropPath ?: media.backdropPath,
                                genres = meta.genres.ifEmpty { media.genres },
                                runtimeMinutes = meta.runtimeMinutes ?: media.runtimeMinutes
                            )
                        } catch (e: Exception) {}

                        // 1.1 AI Critics Consensus
                        launch {
                            isLoadingCritics = true
                            try {
                                criticSummary = api.getCriticSummary(realTconst)
                            } catch (e: Exception) {}
                            isLoadingCritics = false
                        }

                        // 3. Watchlist check
                        try {
                            isInWatchlist = api.checkWatchlist(realTconst).isInWatchlist
                        } catch (e: Exception) {}

                        // 4. If series, seasons & episodes
                        if (media.isTv) {
                            try {
                                val sResp = api.getSeriesSeasons(realTconst)
                                val sNums = sResp.seasons.filter { it.seasonNumber > 0 }.map { it.seasonNumber }.sorted()
                                seasons = sNums
                                if (sNums.isNotEmpty()) {
                                    episodes = api.getSeriesEpisodes(realTconst, sNums.first())
                                }
                            } catch (e: Exception) {}

                            try {
                                seriesProgress = api.getSeriesProgress(realTconst)
                            } catch (e: Exception) {}
                        }
                    }

                    // 2. Torrents & Quality groups (Always fetch if we have realTconst OR title!)
                    try {
                        val torrentQuery = media.ruTitle?.ifEmpty { media.title } ?: media.title
                        val torrentType = if (media.isTv) "tv" else "movie"
                        val torrents = api.getTorrents(
                            imdbId = realTconst.takeIf { it.isNotBlank() },
                            query = torrentQuery.takeIf { it.isNotBlank() },
                            type = torrentType
                        )
                        qualityGroups = groupTorrents(torrents)
                    } catch (e: Exception) {
                        android.util.Log.e("CineClaw", "getTorrents failed", e)
                    }
                } catch (e: Exception) {}
                finally {
                    isLoadingMetadata = false
                }
            }

            DetailsScreen(
                media = media,
                qualityGroups = qualityGroups,
                seasons = seasons,
                episodes = episodes,
                cast = cast,
                crew = crew,
                videos = videos,
                backdrops = backdrops,
                criticSummary = criticSummary,
                isLoadingCritics = isLoadingCritics,
                isLoadingMetadata = isLoadingMetadata,
                isInWatchlist = isInWatchlist,
                seriesProgress = seriesProgress,
                isRefreshingTorrents = isRefreshingTorrents,
                onRefreshTorrents = onRefreshTorrents,
                onPersonClick = { personId ->
                    navController.navigate(Screen.Person.createRoute(personId))
                },
                onSelectSeason = { sNum ->
                    scope.launch {
                        try {
                            val api = app.apiClient.getApi()
                            episodes = api.getSeriesEpisodes(media.effectiveTconst, sNum)
                        } catch (e: Exception) {}
                    }
                },
                onMarkWatched = { req ->
                    scope.launch {
                        try {
                            val api = app.apiClient.getApi()
                            api.markWatched(req)
                            seriesProgress = api.getSeriesProgress(media.effectiveTconst)
                        } catch (e: Exception) {}
                    }
                },
                onPlayClick = { season, episode ->
                    scope.launch {
                        val bestRelease = selectBestReleaseForQuality(
                            qualityGroups = qualityGroups,
                            preferredQuality = preferredQuality,
                            targetSeason = season
                        )

                        if (bestRelease != null && bestRelease.magnet.isNotBlank()) {
                            try {
                                val api = app.apiClient.getApi()
                                api.mountTorrent(
                                    MountTorrentRequest(
                                        tconst = media.effectiveTconst,
                                        magnet = bestRelease.magnet,
                                        title = bestRelease.title,
                                        type = if (media.isTv) "tvSeries" else "movie",
                                        season = season,
                                        episode = episode
                                    )
                                )
                            } catch (e: Exception) {}
                        }
                        navController.navigate(
                            Screen.Player.createRoute(
                                tconst = media.effectiveTconst,
                                title = media.displayTitle,
                                season = season,
                                episode = episode,
                                quality = bestRelease?.resolution?.ifBlank { bestRelease.tier }
                            )
                        )
                    }
                },
                onSelectQualityRelease = { release ->
                    scope.launch {
                        try {
                            val api = app.apiClient.getApi()
                            api.mountTorrent(
                                MountTorrentRequest(
                                    tconst = media.effectiveTconst,
                                    magnet = release.magnet,
                                    title = release.title,
                                    type = if (media.isTv) "tvSeries" else "movie"
                                )
                            )
                        } catch (e: Exception) {}
                        navController.navigate(
                            Screen.Player.createRoute(
                                tconst = media.effectiveTconst,
                                title = media.displayTitle,
                                quality = release.resolution.ifBlank { release.tier }
                            )
                        )
                    }
                },
                onToggleWatchlist = {
                    scope.launch {
                        try {
                            val api = app.apiClient.getApi()
                            val tconstToUse = media.effectiveTconst
                            if (isInWatchlist) {
                                api.removeFromWatchlist(tconstToUse)
                                isInWatchlist = false
                            } else {
                                val req = WatchlistAddRequest(
                                    imdbId = tconstToUse,
                                    mediaType = if (media.isTv) "tv" else "movie",
                                    title = media.displayTitle,
                                    originalTitle = media.originalTitle,
                                    year = media.year,
                                    rating = media.rating,
                                    posterPath = media.posterPath ?: media.posterUrl,
                                    backdropPath = media.backdropPath ?: media.backdropUrl
                                )
                                api.addToWatchlist(req)
                                isInWatchlist = true
                            }
                        } catch (e: Exception) {}
                    }
                },
                onDownloadClick = { season, episode ->
                    scope.launch {
                        val bestRelease = selectBestReleaseForQuality(
                            qualityGroups = qualityGroups,
                            preferredQuality = preferredQuality,
                            targetSeason = season
                        )
                        if (bestRelease != null && bestRelease.magnet.isNotBlank()) {
                            try {
                                val api = app.apiClient.getApi()
                                api.mountTorrent(
                                    MountTorrentRequest(
                                        tconst = media.effectiveTconst,
                                        magnet = bestRelease.magnet,
                                        title = bestRelease.title,
                                        type = if (media.isTv) "tvSeries" else "movie",
                                        season = season,
                                        episode = episode
                                    )
                                )
                                var info = api.getPlayerInfo(media.effectiveTconst, season, episode)
                                if (!info.success || info.effectiveStreamUrl == null) {
                                    delay(1000)
                                    info = api.getPlayerInfo(media.effectiveTconst, season, episode)
                                }
                                val rawUrl = info.effectiveStreamUrl
                                if (rawUrl != null) {
                                    val fullUrl = if (rawUrl.startsWith("http")) rawUrl else "$serverUrl$rawUrl"
                                    val downloadId = if (season != null && episode != null) "${media.effectiveTconst}_s${season}_e$episode" else media.effectiveTconst
                                    val meta = OfflineMetadata(
                                        id = downloadId,
                                        tconst = media.effectiveTconst,
                                        title = media.displayTitle,
                                        subtitle = if (season != null && episode != null) "Сезон $season, Серия $episode" else null,
                                        posterUrl = media.posterUrl,
                                        year = media.year,
                                        season = season,
                                        episode = episode,
                                        mediaType = if (media.isTv) "tv" else "movie",
                                        quality = bestRelease.resolution.ifBlank { "1080p" },
                                        fileSize = bestRelease.size
                                    )
                                    val dm = DownloadManager.getInstance(context)
                                    dm.enqueueDownload(downloadId, meta, fullUrl)
                                    DownloadService.startService(context)
                                    Toast.makeText(context, "Загрузка «${media.displayTitle}» запущена", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Ошибка скачивания: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "Нет доступных торрентов для скачивания", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("tconst") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType; defaultValue = "" },
                navArgument("season") { type = NavType.IntType; defaultValue = -1 },
                navArgument("episode") { type = NavType.IntType; defaultValue = -1 },
                navArgument("quality") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val tconst = backStackEntry.arguments?.getString("tconst") ?: return@composable
            val title = safeDecode(backStackEntry.arguments?.getString("title"))
            val seasonArg = backStackEntry.arguments?.getInt("season")?.takeIf { it != -1 }
            val episodeArg = backStackEntry.arguments?.getInt("episode")?.takeIf { it != -1 }
            val qualityArg = backStackEntry.arguments?.getString("quality")?.takeIf { it.isNotBlank() } ?: ""
            val initialQuality = when {
                qualityArg.contains("4k", ignoreCase = true) || qualityArg.contains("2160", ignoreCase = true) -> "4K UHD"
                qualityArg.contains("1080", ignoreCase = true) -> "1080p FHD"
                qualityArg.contains("720", ignoreCase = true) -> "720p HD"
                qualityArg.contains("sd", ignoreCase = true) -> "SD"
                qualityArg.isNotBlank() -> qualityArg
                else -> "1080p"
            }

            val cinemaPlayer = remember(tconst, isAudioPassthrough) {
                CinemaPlayer(
                    context = context,
                    apiClient = app.apiClient,
                    initialQualityTier = initialQuality,
                    audioPassthrough = isAudioPassthrough
                )
            }
            var qualityGroups by remember { mutableStateOf<List<QualityGroup>>(emptyList()) }
            var pendingResumeInfo by remember { mutableStateOf<PendingResumeState?>(null) }
            var seasonsList by remember { mutableStateOf<List<Int>>(emptyList()) }
            var episodesList by remember { mutableStateOf<List<EpisodeInfo>>(emptyList()) }
            var activePlayerInfo by remember { mutableStateOf<PlayerInfoResponse?>(null) }
            var activeStreamUrl by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(tconst, seasonArg, episodeArg) {
                // Check local offline downloads first for instant 100% offline playback
                val dm = DownloadManager.getInstance(context)
                val offlineItem = dm.offlineMediaState.value.find { 
                    it.metadata.tconst == tconst || it.metadata.id == tconst ||
                    (seasonArg != null && episodeArg != null && it.metadata.id == "${tconst}_s${seasonArg}_e${episodeArg}")
                }
                if (offlineItem != null && offlineItem.file.exists()) {
                    val localUri = offlineItem.file.toURI().toString()
                    activeStreamUrl = localUri
                    cinemaPlayer.prepare(
                        streamUrl = localUri,
                        tconst = tconst,
                        title = title,
                        season = seasonArg,
                        episode = episodeArg,
                        initialPositionMs = 0L,
                        quality = offlineItem.metadata.quality
                    )
                    return@LaunchedEffect
                }

                try {
                    val api = app.apiClient.getApi()
                    // Fetch series seasons and episodes if playing a TV show
                    if (seasonArg != null) {
                        try {
                            val sResp = api.getSeriesSeasons(tconst)
                            val sNums = sResp.seasons.filter { it.seasonNumber > 0 }.map { it.seasonNumber }.sorted()
                            seasonsList = sNums
                            episodesList = api.getSeriesEpisodes(tconst, seasonArg)
                        } catch (e: Exception) {
                            android.util.Log.w("CineClaw", "Failed to fetch seasons/episodes for player: ${e.message}")
                        }
                    }

                    // Fetch torrents for quality switcher in player
                    try {
                        val torrents = api.getTorrents(
                            imdbId = tconst.takeIf { it.startsWith("tt") },
                            query = title.takeIf { it.isNotBlank() }
                        )
                        qualityGroups = groupTorrents(torrents)
                    } catch (e: Exception) {}

                    var info: PlayerInfoResponse? = null
                    var attempts = 0
                    while (attempts < 5) {
                        try {
                            val candidate = api.getPlayerInfo(tconst, seasonArg, episodeArg)
                            if (candidate.success && candidate.effectiveStreamUrl != null) {
                                info = candidate
                                break
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("CineClaw", "getPlayerInfo attempt ${attempts + 1} failed: ${e.message}")
                        }
                        attempts++
                        if (attempts < 5) kotlinx.coroutines.delay(1500)
                    }

                    if (info == null) {
                        try {
                            info = api.getPlayerInfo(tconst, seasonArg, episodeArg)
                        } catch (e: Exception) {
                            android.util.Log.e("CineClaw", "getPlayerInfo final attempt failed", e)
                        }
                    }

                    val playerInfo = info
                    val rawStreamUrl = playerInfo?.effectiveStreamUrl
                    if (playerInfo != null && rawStreamUrl != null) {
                        val fullStreamUrl = if (rawStreamUrl.startsWith("http")) rawStreamUrl else "$serverUrl$rawStreamUrl"
                        activeStreamUrl = fullStreamUrl
                        activePlayerInfo = playerInfo

                        val resolvedQuality = when {
                            qualityArg.contains("4k", ignoreCase = true) || qualityArg.contains("2160", ignoreCase = true) -> "4K UHD"
                            qualityArg.contains("1080", ignoreCase = true) -> "1080p FHD"
                            qualityArg.contains("720", ignoreCase = true) -> "720p HD"
                            qualityArg.contains("sd", ignoreCase = true) -> "SD"
                            qualityArg.isNotBlank() -> qualityArg
                            playerInfo.releases.any { it.resolution.contains("4k", ignoreCase = true) || it.resolution.contains("2160", ignoreCase = true) } -> "4K UHD"
                            playerInfo.releases.any { it.resolution.contains("1080", ignoreCase = true) } -> "1080p FHD"
                            else -> "1080p"
                        }

                        val resumeSec = playerInfo.effectiveResumeSeconds
                        val totalSec = playerInfo.effectiveDurationSeconds
                        val hasResume = resumeSec > 20.0 && !playerInfo.isPlayed && (totalSec <= 0 || resumeSec < totalSec - 25.0)
                        android.util.Log.d("CineClaw", "CHECK RESUME: resumeSec=$resumeSec, totalSec=$totalSec, hasResume=$hasResume, isPlayed=${playerInfo.isPlayed}")

                        if (hasResume) {
                            pendingResumeInfo = PendingResumeState(
                                playerInfo = playerInfo,
                                fullStreamUrl = fullStreamUrl,
                                resolvedQuality = resolvedQuality
                            )
                        } else {
                            cinemaPlayer.prepare(
                                streamUrl = fullStreamUrl,
                                tconst = tconst,
                                title = title,
                                season = seasonArg,
                                episode = episodeArg,
                                initialPositionMs = (playerInfo.effectiveResumeSeconds * 1000.0).toLong().coerceAtLeast(0L),
                                audioTracks = playerInfo.audioTracks,
                                subtitleTracks = playerInfo.effectiveSubtitles,
                                audioPassthrough = isAudioPassthrough,
                                quality = resolvedQuality
                            )
                        }
                    } else {
                        val errMsg = playerInfo?.error ?: "Не удалось загрузить поток. Проверьте подключение или выберите другую раздачу."
                        cinemaPlayer.setErrorMessage(errMsg)
                    }
                } catch (e: Exception) {
                    cinemaPlayer.setErrorMessage("Ошибка подготовки видео: ${e.localizedMessage}")
                }
            }

            val failedHashes = remember { mutableStateListOf<String>() }
            val currentQualityGroups by rememberUpdatedState(qualityGroups)

            DisposableEffect(cinemaPlayer) {
                cinemaPlayer.onDecoderFallback = {
                    scope.launch {
                        val allReleases = currentQualityGroups.flatMap { it.releases }
                        val currentInfo = try { app.apiClient.getApi().getPlayerInfo(tconst, seasonArg, episodeArg) } catch (_: Exception) { null }
                        val currentHash = currentInfo?.effectiveStreamUrl?.let { url ->
                            val match = Regex("link=([a-fA-F0-9]{40})").find(url)
                            match?.groupValues?.getOrNull(1)
                        }
                        if (!currentHash.isNullOrBlank()) {
                            failedHashes.add(currentHash.lowercase())
                        }

                        val nextCandidate = allReleases
                            .filter { !failedHashes.contains(it.effectiveHash.lowercase()) && !isHardwareIncompatible(it) }
                            .maxWithOrNull(
                                compareBy<TorrentRelease> { seasonMatchScore(it, seasonArg) }
                                    .thenBy { it.seeds }
                                    .thenBy { it.size }
                            )

                        if (nextCandidate != null) {
                            android.util.Log.i("CineClaw", "onDecoderFallback: auto-switching to candidate ${nextCandidate.title}")
                            cinemaPlayer.setErrorMessage("Аппаратный декодер ТВ не поддерживает видео. Автопереключение на совместимую раздачу...")
                            try {
                                val api = app.apiClient.getApi()
                                api.mountTorrent(
                                    MountTorrentRequest(
                                        tconst = tconst,
                                        magnet = nextCandidate.magnet,
                                        title = nextCandidate.title,
                                        type = if (seasonArg != null) "tvSeries" else "movie",
                                        season = seasonArg,
                                        episode = episodeArg
                                    )
                                )
                                kotlinx.coroutines.delay(1000)
                                val newInfo = api.getPlayerInfo(tconst, seasonArg, episodeArg)
                                val newRawUrl = newInfo.effectiveStreamUrl
                                if (newRawUrl != null) {
                                    val fullUrl = if (newRawUrl.startsWith("http")) newRawUrl else "$serverUrl$newRawUrl"
                                    val resolvedTier = when {
                                        nextCandidate.resolution.contains("4k", ignoreCase = true) || nextCandidate.tier == "4k" -> "4K UHD"
                                        nextCandidate.resolution.contains("1080", ignoreCase = true) || nextCandidate.tier == "1080p" -> "1080p FHD"
                                        nextCandidate.resolution.contains("720", ignoreCase = true) || nextCandidate.tier == "720p" -> "720p HD"
                                        else -> nextCandidate.resolution.ifBlank { nextCandidate.tier }
                                    }
                                    cinemaPlayer.switchQuality(
                                        fullUrl,
                                        resolvedTier,
                                        newInfo.audioTracks,
                                        newInfo.effectiveSubtitles
                                    )
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("CineClaw", "onDecoderFallback failed", e)
                                cinemaPlayer.setErrorMessage("Ошибка автопереключения: ${e.localizedMessage}")
                            }
                        } else {
                            cinemaPlayer.setErrorMessage("Формат видео не поддерживается декодером ТВ, а совместимых раздач не найдено.")
                        }
                    }
                }
                onDispose {
                    cinemaPlayer.onDecoderFallback = null
                    cinemaPlayer.release()
                }
            }

            val pendingResume = pendingResumeInfo
            if (pendingResume != null) {
                ResumeConfirmDialog(
                    title = title,
                    subtitle = if (seasonArg != null && episodeArg != null) "Сезон $seasonArg, Серия $episodeArg" else null,
                    resumeSeconds = pendingResume.playerInfo.effectiveResumeSeconds,
                    durationSeconds = pendingResume.playerInfo.effectiveDurationSeconds,
                    onResume = {
                        val info = pendingResume.playerInfo
                        val resumeSec = info.effectiveResumeSeconds
                        val targetStreamUrl = pendingResume.fullStreamUrl
                        val targetQuality = pendingResume.resolvedQuality
                        pendingResumeInfo = null
                        cinemaPlayer.prepare(
                            streamUrl = targetStreamUrl,
                            tconst = tconst,
                            title = title,
                            season = seasonArg,
                            episode = episodeArg,
                            initialPositionMs = (resumeSec * 1000.0).toLong().coerceAtLeast(0L),
                            audioTracks = info.audioTracks,
                            subtitleTracks = info.effectiveSubtitles,
                            audioPassthrough = isAudioPassthrough,
                            quality = targetQuality
                        )
                    },
                    onStartFromBeginning = {
                        val info = pendingResume.playerInfo
                        val targetStreamUrl = pendingResume.fullStreamUrl
                        val targetQuality = pendingResume.resolvedQuality
                        pendingResumeInfo = null
                        cinemaPlayer.prepare(
                            streamUrl = targetStreamUrl,
                            tconst = tconst,
                            title = title,
                            season = seasonArg,
                            episode = episodeArg,
                            initialPositionMs = 0L,
                            audioTracks = info.audioTracks,
                            subtitleTracks = info.effectiveSubtitles,
                            audioPassthrough = isAudioPassthrough,
                            quality = targetQuality
                        )
                    },
                    onDismiss = {
                        pendingResumeInfo = null
                        navController.popBackStack()
                    }
                )
            } else {
                PlayerScreen(
                    title = title,
                    subtitle = if (seasonArg != null && episodeArg != null) "Сезон $seasonArg, Серия $episodeArg" else null,
                    cinemaPlayer = cinemaPlayer,
                    exoPlayer = cinemaPlayer.exoPlayer,
                    qualityGroups = qualityGroups,
                    season = seasonArg,
                    episode = episodeArg,
                    seasons = seasonsList,
                    episodes = episodesList,
                    mediaSourceId = activePlayerInfo?.mediaSourceId ?: tconst,
                    directStreamUrl = activeStreamUrl,
                    baseUrl = serverUrl,
                    skipSegments = activePlayerInfo?.skipSegments ?: emptyList(),
                    onSelectQualityRelease = { release ->
                        scope.launch {
                            try {
                                val api = app.apiClient.getApi()
                                api.mountTorrent(
                                    MountTorrentRequest(
                                        tconst = tconst,
                                        magnet = release.magnet,
                                        title = release.title,
                                        type = if (seasonArg != null) "tvSeries" else "movie",
                                        season = seasonArg,
                                        episode = episodeArg
                                    )
                                )
                                var info: PlayerInfoResponse? = null
                                var retry = 0
                                while (retry < 4) {
                                    try {
                                        val cand = api.getPlayerInfo(tconst, seasonArg, episodeArg)
                                        if (cand.success && cand.effectiveStreamUrl != null) {
                                            info = cand
                                            break
                                        }
                                    } catch (e: Exception) {}
                                    retry++
                                    if (retry < 4) kotlinx.coroutines.delay(1000)
                                }
                                if (info == null) {
                                    info = api.getPlayerInfo(tconst, seasonArg, episodeArg)
                                }
                                val rawUrl = info?.effectiveStreamUrl
                                if (rawUrl != null) {
                                    val fullUrl = if (rawUrl.startsWith("http")) rawUrl else "$serverUrl$rawUrl"
                                    val resolvedTier = when {
                                        release.resolution.contains("4k", ignoreCase = true) || release.tier == "4k" -> "4K UHD"
                                        release.resolution.contains("1080", ignoreCase = true) || release.tier == "1080p" -> "1080p FHD"
                                        release.resolution.contains("720", ignoreCase = true) || release.tier == "720p" -> "720p HD"
                                        else -> release.resolution.ifBlank { release.tier }
                                    }
                                    cinemaPlayer.switchQuality(
                                        fullUrl,
                                        resolvedTier,
                                        info.audioTracks,
                                        info.effectiveSubtitles
                                    )
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("CineClaw", "switchQuality failed", e)
                            }
                        }
                    },
                    onNextEpisodeClick = {
                        if (seasonArg != null && episodeArg != null) {
                            navController.navigate(
                                Screen.Player.createRoute(
                                    tconst = tconst,
                                    title = title,
                                    season = seasonArg,
                                    episode = episodeArg + 1,
                                    quality = qualityArg
                                )
                            ) {
                                popUpTo(Screen.Player.route) { inclusive = true }
                            }
                        }
                    },
                    onSelectEpisode = { targetSeason, targetEpisode ->
                        navController.navigate(
                            Screen.Player.createRoute(
                                tconst = tconst,
                                title = title,
                                season = targetSeason,
                                episode = targetEpisode,
                                quality = qualityArg
                            )
                        ) {
                            popUpTo(Screen.Player.route) { inclusive = true }
                        }
                    },
                    onFetchSeasonEpisodes = { targetSeason ->
                        scope.launch {
                            try {
                                episodesList = app.apiClient.getApi().getSeriesEpisodes(tconst, targetSeason)
                            } catch (e: Exception) {
                                android.util.Log.w("CineClaw", "Failed to fetch episodes for season $targetSeason: ${e.message}")
                            }
                        }
                    },
                    onClosePlayer = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.Search.route) {
            val navContext = LocalContext.current
            var searchQuery by remember { mutableStateOf("") }
            var searchResults by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
            var isSearching by remember { mutableStateOf(false) }
            var searchJob by remember { mutableStateOf<Job?>(null) }

            val executeSearch: (String) -> Unit = { query ->
                searchJob?.cancel()
                val trimmed = query.trim()
                if (trimmed.length >= 2) {
                    searchJob = scope.launch {
                        delay(250)
                        isSearching = true
                        try {
                            val api = app.apiClient.getApi()
                            val resp = api.search(trimmed, limit = 30)
                            searchResults = resp.hits.map { it.toMediaItem(serverUrl) }
                        } catch (e: Exception) {
                            if (e !is CancellationException) {
                                searchResults = emptyList()
                            }
                        } finally {
                            isSearching = false
                        }
                    }
                } else {
                    searchResults = emptyList()
                    isSearching = false
                }
            }

            val voiceLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    val spokenText = matches?.firstOrNull()
                    if (!spokenText.isNullOrBlank()) {
                        searchQuery = spokenText
                        executeSearch(spokenText)
                    }
                }
            }

            SearchScreen(
                query = searchQuery,
                results = searchResults,
                isLoading = isSearching,
                onQueryChange = { newQuery ->
                    searchQuery = newQuery
                    executeSearch(newQuery)
                },
                onVoiceSearchClick = {
                    try {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Назовите фильм или сериал")
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                        }
                        voiceLauncher.launch(intent)
                    } catch (e: Exception) {
                        Toast.makeText(navContext, "Голосовой поиск недоступен", Toast.LENGTH_SHORT).show()
                    }
                },
                onSelectMedia = { media ->
                    navController.navigate(
                        Screen.Details.createRoute(
                            tconst = media.effectiveTconst,
                            id = media.effectiveId,
                            type = media.mediaType ?: if (media.isTv) "tv" else "movie",
                            title = media.displayTitle,
                            poster = media.effectivePoster,
                            backdrop = media.effectiveBackdrop,
                            overview = media.overview,
                            year = media.year,
                            rating = media.rating
                        )
                    )
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                currentServerUrl = currentServerUrl,
                currentUsername = currentUsername ?: "admin",
                isAudioPassthrough = isAudioPassthrough,
                preferredQuality = preferredQuality,
                onSaveServerUrl = { newUrl ->
                    scope.launch {
                        app.sessionManager.saveServerUrl(newUrl)
                    }
                },
                onToggleAudioPassthrough = { enabled ->
                    scope.launch {
                        app.sessionManager.setAudioPassthrough(enabled)
                    }
                },
                onSetPreferredQuality = { quality ->
                    scope.launch {
                        app.sessionManager.setPreferredQuality(quality)
                    }
                },
                onLogout = {
                    scope.launch {
                        app.sessionManager.clearSession()
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onChangeServer = {
                    scope.launch {
                        app.sessionManager.clearSession()
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onPingCheck = { url ->
                    app.apiClient.pingServer(url)
                }
            )
        }

        composable(
            route = Screen.Person.route,
            arguments = listOf(navArgument("personId") { type = NavType.LongType })
        ) { backStackEntry ->
            val personId = backStackEntry.arguments?.getLong("personId") ?: 0L
            var personDetails by remember { mutableStateOf<PersonDetailsResponse?>(null) }
            var isLoading by remember { mutableStateOf(true) }

            LaunchedEffect(personId) {
                isLoading = true
                try {
                    val api = app.apiClient.getApi()
                    personDetails = api.getPersonDetails(personId)
                } catch (e: Exception) {}
                isLoading = false
            }

            PersonScreen(
                personDetails = personDetails,
                isLoading = isLoading,
                onSelectMovie = { credit ->
                    scope.launch {
                        var realTconst = ""
                        try {
                            val res = app.apiClient.getApi().resolveTmdbMovie(credit.mediaType, credit.id)
                            realTconst = res.tconst
                        } catch (e: Exception) {}
                        navController.navigate(
                            Screen.Details.createRoute(
                                tconst = realTconst,
                                id = credit.id,
                                type = credit.mediaType,
                                title = credit.displayTitle,
                                poster = credit.effectivePoster,
                                backdrop = credit.backdropPath,
                                year = credit.year,
                                rating = credit.voteAverage
                            )
                        )
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Shelf.route,
            arguments = listOf(
                navArgument("shelfId") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType; defaultValue = "" },
                navArgument("type") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val shelfId = backStackEntry.arguments?.getString("shelfId") ?: ""
            val titleArg = safeDecode(backStackEntry.arguments?.getString("title"))
            val typeArg = backStackEntry.arguments?.getString("type") ?: ""

            var items by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
            var page by remember { mutableStateOf(1) }
            var hasMorePages by remember { mutableStateOf(true) }
            var isLoading by remember { mutableStateOf(false) }

            suspend fun loadPage(p: Int) {
                isLoading = true
                try {
                    val api = app.apiClient.getApi()
                    val newItems = when (shelfId) {
                        "tracker_fresh" -> api.getHotlist(type = "new_movie", page = p).items
                        "tracker_hotlist" -> api.getHotlist(type = "movie", page = p).items
                        "uhd_4k", "uhd_movie" -> api.getHotlist(type = "movie", quality = "4k", page = p).items
                        "uhd_fresh" -> api.getHotlist(type = "new_movie", quality = "4k", page = p).items
                        "tracker_fresh_tv" -> api.getHotlist(type = "new_tv", page = p).items
                        "tracker_tv" -> api.getHotlist(type = "tv", page = p).items
                        else -> api.getShelf(id = shelfId, page = p, type = typeArg.ifEmpty { null }).items
                    }
                    if (newItems.isEmpty()) {
                        hasMorePages = false
                    } else {
                        items = if (p == 1) newItems else items + newItems
                        page = p
                    }
                } catch (e: Exception) {
                    hasMorePages = false
                }
                isLoading = false
            }

            LaunchedEffect(shelfId) {
                page = 1
                hasMorePages = true
                loadPage(1)
            }

            ShelfScreen(
                title = titleArg.ifEmpty { shelfId },
                items = items,
                isLoading = isLoading,
                hasMorePages = hasMorePages,
                onLoadMore = { scope.launch { loadPage(page + 1) } },
                onSelectMedia = { media ->
                    navController.navigate(
                        Screen.Details.createRoute(
                            tconst = media.effectiveTconst,
                            id = media.effectiveId,
                            type = media.mediaType ?: if (media.isTv) "tv" else "movie",
                            title = media.displayTitle,
                            poster = media.effectivePoster,
                            backdrop = media.effectiveBackdrop,
                            overview = media.overview,
                            year = media.year,
                            rating = media.rating
                        )
                    )
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Catalog.route,
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { backStackEntry ->
            val catalogType = backStackEntry.arguments?.getString("type") ?: "movies"
            var catalogShelves by remember { mutableStateOf<List<Shelf>>(emptyList()) }
            var isLoading by remember { mutableStateOf(true) }

            LaunchedEffect(catalogType) {
                isLoading = true
                try {
                    val api = app.apiClient.getApi()
                    catalogShelves = when (catalogType) {
                        "series" -> {
                            val popSeries = api.getShelf(id = "popular_series", type = "tv")
                            val freshTv = api.getHotlist(type = "new_tv").let { Shelf(id = "tracker_fresh_tv", title = "Новинки сериалов на трекерах", items = it.items) }
                            val appleTv = api.getShelf(id = "apple_tv", type = "tv")
                            val hboMax = api.getShelf(id = "hbo_max", type = "tv")
                            val swarmTv = api.getHotlist(type = "tv").let { Shelf(id = "tracker_tv", title = "Популярно на трекерах (Сериалы)", items = it.items) }
                            listOf(popSeries, freshTv, appleTv, hboMax, swarmTv).filter { it.items.isNotEmpty() }
                        }
                        "4k" -> {
                            val uhdMovie = api.getHotlist(type = "movie", quality = "4k").let { Shelf(id = "uhd_movie", title = "4K UHD Фильмы", items = it.items) }
                            val uhdFresh = api.getHotlist(type = "new_movie", quality = "4k").let { Shelf(id = "uhd_fresh", title = "4K UHD Новинки на трекерах", items = it.items) }
                            listOf(uhdMovie, uhdFresh).filter { it.items.isNotEmpty() }
                        }
                        else -> { // "movies"
                            val freshMovie = api.getHotlist(type = "new_movie").let { Shelf(id = "tracker_fresh", title = "Новинки кино на трекерах", items = it.items) }
                            val hotlistMovie = api.getHotlist(type = "movie").let { Shelf(id = "tracker_hotlist", title = "Популярно на трекерах", items = it.items) }
                            val uhdMovie = api.getHotlist(type = "movie", quality = "4k").let { Shelf(id = "uhd_4k", title = "4K UHD Кинозал", items = it.items) }
                            val trending = api.getShelf(id = "trending", type = "movie")
                            listOf(freshMovie, hotlistMovie, uhdMovie, trending).filter { it.items.isNotEmpty() }
                        }
                    }
                } catch (e: Exception) {}
                isLoading = false
            }

            CatalogScreen(
                catalogType = catalogType,
                shelves = catalogShelves,
                isLoading = isLoading,
                onSelectMedia = { media ->
                    navController.navigate(
                        Screen.Details.createRoute(
                            tconst = media.effectiveTconst,
                            id = media.effectiveId,
                            type = media.mediaType ?: if (media.isTv) "tv" else "movie",
                            title = media.displayTitle,
                            poster = media.effectivePoster,
                            backdrop = media.effectiveBackdrop,
                            overview = media.overview,
                            year = media.year,
                            rating = media.rating
                        )
                    )
                },
                onOpenShelf = { shelfId, title ->
                    navController.navigate(Screen.Shelf.createRoute(shelfId, title, type = if (catalogType == "series") "tv" else "movie"))
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Watchlist.route) {
            var watchlistItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
            var isLoading by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                isLoading = true
                try {
                    watchlistItems = app.apiClient.getApi().getWatchlist()
                } catch (e: Exception) {}
                isLoading = false
            }

            WatchlistScreen(
                items = watchlistItems,
                isLoading = isLoading,
                onSelectMedia = { media ->
                    navController.navigate(
                        Screen.Details.createRoute(
                            tconst = media.effectiveTconst,
                            id = media.effectiveId,
                            type = media.mediaType ?: if (media.isTv) "tv" else "movie",
                            title = media.displayTitle,
                            poster = media.effectivePoster,
                            backdrop = media.effectiveBackdrop,
                            overview = media.overview,
                            year = media.year,
                            rating = media.rating
                        )
                    )
                },
                onRemoveMedia = { item ->
                    scope.launch {
                        try {
                            app.apiClient.getApi().removeFromWatchlist(item.effectiveTconst)
                            watchlistItems = watchlistItems.filter { it.effectiveTconst != item.effectiveTconst }
                        } catch (e: Exception) {}
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Downloads.route) {
            DownloadsScreen(
                onPlayOfflineFile = { file, meta ->
                    val fileUri = file.toURI().toString()
                    navController.navigate(
                        Screen.Player.createRoute(
                            tconst = meta.tconst,
                            title = meta.title,
                            season = meta.season,
                            episode = meta.episode,
                            quality = meta.quality
                        )
                    )
                },
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

fun isHardwareIncompatible(r: TorrentRelease): Boolean {
    val lower = r.title.lowercase()
    val has10Bit = lower.contains("10-bit") || lower.contains("10bit") || lower.contains("hi10p") || lower.contains("high 10")
    if (!has10Bit) return false
    // H.265 / HEVC / AV1 / VP9 10-bit is standard and fully supported by Android TV hardware decoders
    val isModernCodec = lower.contains("h.265") || lower.contains("h265") || lower.contains("x265") ||
            lower.contains("hevc") || lower.contains("av1") || lower.contains("vp9")
    if (isModernCodec) return false

    // H.264 / AVC / x264 10-bit (Hi10P) is strictly NOT supported by Android TV hardware decoders (c2.mtk.avc.decoder fails)
    val isAvc = lower.contains("h.264") || lower.contains("h264") || lower.contains("x264") || lower.contains("avc")
    return isAvc
}

fun seasonMatchScore(r: TorrentRelease, targetSeason: Int?): Int {
    if (targetSeason == null || targetSeason <= 0) return 0
    if (r.seasons.isNotEmpty()) {
        if (r.seasons.size == 1 && r.seasons.contains(targetSeason)) return 20 // exact single season pack
        if (r.seasons.contains(targetSeason)) return 10 // multi-season pack containing target
        return -100 // different season
    }
    val lower = r.title.lowercase()
    val sToken = String.format("s%02d", targetSeason)
    val altToken = "сезон $targetSeason"
    val isSingleSeason = (lower.contains(sToken) || lower.contains(altToken) || lower.contains("сезон: $targetSeason")) &&
            !lower.contains("s01-") && !lower.contains("сезон 1-") && !lower.contains("сезоны 1-")
    if (isSingleSeason) return 20
    if (lower.contains("s01-") || lower.contains("сезон 1-") || lower.contains("сезоны 1-")) return 10
    val otherSeasonFound = (1..20).any {
        it != targetSeason && (lower.contains(String.format("s%02d", it)) || lower.contains("сезон $it"))
    }
    if (otherSeasonFound) return -100
    return 5
}

fun selectBestReleaseForQuality(
    qualityGroups: List<QualityGroup>,
    preferredQuality: String,
    targetSeason: Int? = null
): TorrentRelease? {
    if (qualityGroups.isEmpty()) return null

    val targetTiers = when (preferredQuality.lowercase().trim()) {
        "4k", "4k uhd", "2160p" -> listOf("4k", "1080p", "720p", "sd")
        "1080p", "1080p fhd" -> listOf("1080p", "720p", "4k", "sd")
        "720p", "720p hd" -> listOf("720p", "1080p", "sd", "4k")
        "sd" -> listOf("sd", "720p", "1080p", "4k")
        else -> listOf("1080p", "4k", "720p", "sd")
    }

    // Pass 1: find in cascade tiers matching target season, strictly excluding hardware-incompatible releases
    for (tier in targetTiers) {
        val group = qualityGroups.firstOrNull { it.tier.equals(tier, ignoreCase = true) } ?: continue
        val matching = group.releases.filter { !isHardwareIncompatible(it) && seasonMatchScore(it, targetSeason) >= 10 }
        val candidate = matching.maxWithOrNull(
            compareBy<TorrentRelease> { seasonMatchScore(it, targetSeason) }
                .thenBy { it.seeds }
                .thenBy { it.size }
        )
        if (candidate != null) return candidate
    }

    // Pass 2: find in cascade tiers with general season matching
    for (tier in targetTiers) {
        val group = qualityGroups.firstOrNull { it.tier.equals(tier, ignoreCase = true) } ?: continue
        val candidate = group.releases
            .filter { !isHardwareIncompatible(it) && seasonMatchScore(it, targetSeason) >= 0 }
            .maxWithOrNull(compareBy<TorrentRelease> { it.seeds }.thenBy { it.size })
        if (candidate != null) return candidate
    }

    // Pass 3: find in cascade tiers regardless of season filter, excluding hardware-incompatible
    for (tier in targetTiers) {
        val group = qualityGroups.firstOrNull { it.tier.equals(tier, ignoreCase = true) } ?: continue
        val candidate = group.releases
            .filter { !isHardwareIncompatible(it) }
            .maxWithOrNull(compareBy<TorrentRelease> { it.seeds }.thenBy { it.size })
        if (candidate != null) return candidate
    }

    // Pass 4: fallback across all tiers, excluding hardware-incompatible
    val compatFallback = qualityGroups.flatMap { it.releases }
        .filter { !isHardwareIncompatible(it) }
        .maxByOrNull { it.seeds }
    if (compatFallback != null) return compatFallback

    // Absolute emergency fallback
    return qualityGroups.flatMap { it.releases }.maxByOrNull { it.seeds }
}

