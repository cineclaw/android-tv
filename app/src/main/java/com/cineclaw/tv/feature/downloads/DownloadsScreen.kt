package com.cineclaw.tv.feature.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.cineclaw.tv.BuildConfig
import com.cineclaw.tv.core.designsystem.*
import com.cineclaw.tv.core.download.*
import java.io.File

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onPlayOfflineFile: (file: File, metadata: OfflineMetadata) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val downloadManager = remember { DownloadManager.getInstance(context) }
    val storageManager = remember { downloadManager.getStorageManager() }

    val activeDownloads by downloadManager.downloadsState.collectAsState()
    val offlineMedia by downloadManager.offlineMediaState.collectAsState()

    var storageVolumes by remember { mutableStateOf(storageManager.getAvailableStorageVolumes()) }
    var itemToDelete by remember { mutableStateOf<OfflineMediaItem?>(null) }

    LaunchedEffect(Unit) {
        storageVolumes = storageManager.getAvailableStorageVolumes()
        downloadManager.refreshCompletedDownloads()
    }

    val primaryVolume = storageVolumes.firstOrNull { it.isRemovable } ?: storageVolumes.firstOrNull()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(horizontal = 36.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ObsidianSurface)
                            .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                            .clickable { onBackClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Офлайн-медиатека",
                                color = TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (BuildConfig.IS_AUTOMOTIVE) {
                                TvBadge(
                                    text = "АВТОНОМНЫЙ РЕЖИМ",
                                    backgroundColor = EmeraldGlow,
                                    textColor = EmeraldPrimary
                                )
                            }
                        }
                        Text(
                            text = "Просмотр скачанных фильмов в дороге без подключения к интернету",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

                // Storage Volume Stats Card
                if (primaryVolume != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(ObsidianCard)
                            .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (primaryVolume.isRemovable) Icons.Default.Usb else Icons.Default.Storage,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = primaryVolume.name,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${String.format("%.1f", primaryVolume.freeSpaceGb)} GB свободно",
                                        color = EmeraldPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { primaryVolume.usedPercent / 100f },
                                    modifier = Modifier
                                        .width(160.dp)
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = EmeraldPrimary,
                                    trackColor = ObsidianSurface
                                )
                            }
                        }
                    }
                }
            }

            // Active Downloads Progress (if any)
            val downloadingItems = activeDownloads.filter { it.status != DownloadStatus.COMPLETED }
            if (downloadingItems.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Загрузка прямо сейчас (${downloadingItems.size})",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    for (item in downloadingItems) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(ObsidianSurface)
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        progress = { item.progressPercent / 100f },
                                        color = EmeraldPrimary,
                                        modifier = Modifier.size(36.dp),
                                        strokeWidth = 3.dp
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = item.metadata.title,
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${item.progressPercent}% • ${String.format("%.1f", item.downloadedMb)} / ${String.format("%.1f", item.totalMb)} MB • ${item.status.name}",
                                            color = TextMuted,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (item.status == DownloadStatus.DOWNLOADING) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(ObsidianCard)
                                                .clickable { downloadManager.pauseDownload(item.id) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Pause, contentDescription = "Пауза", tint = TextPrimary)
                                        }
                                    } else if (item.status == DownloadStatus.PAUSED) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(EmeraldGlow)
                                                .clickable { downloadManager.resumeDownload(item.id) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Продолжить", tint = EmeraldPrimary)
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(ObsidianCard)
                                            .clickable { downloadManager.cancelDownload(item.id) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Отмена", tint = Color(0xFFFF5252))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Completed Media Grid
            if (offlineMedia.isEmpty() && downloadingItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Накопитель пуст",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Скачивайте фильмы и сериалы в память или на USB-флешку\nчерез кнопку «📥 Скачать» на карточке фильма",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(180.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(offlineMedia) { item ->
                        OfflineMediaCard(
                            item = item,
                            onPlay = { onPlayOfflineFile(item.file, item.metadata) },
                            onDelete = { itemToDelete = item }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (itemToDelete != null) {
        val target = itemToDelete!!
        androidx.compose.ui.window.Dialog(onDismissRequest = { itemToDelete = null }) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(ObsidianCard)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(20.dp))
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Удалить из памяти?",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Файл «${target.metadata.title}» (${String.format("%.1f", target.metadata.fileSize / (1024.0 * 1024.0 * 1024.0))} ГБ) будет удален с накопителя.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TvActionButton(
                            text = "Отмена",
                            icon = Icons.Default.Close,
                            isPrimary = false,
                            onClick = { itemToDelete = null }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        TvActionButton(
                            text = "Удалить",
                            icon = Icons.Default.Delete,
                            isPrimary = true,
                            onClick = {
                                downloadManager.deleteOfflineMedia(target.file)
                                itemToDelete = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun OfflineMediaCard(
    item: OfflineMediaItem,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val meta = item.metadata
    val sizeGb = meta.fileSize / (1024.0 * 1024.0 * 1024.0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianCard)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
            .clickable { onPlay() }
    ) {
        Column {
            // Poster / Thumbnail Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .background(ObsidianSurface)
            ) {
                if (!meta.posterUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = meta.posterUrl,
                        contentDescription = meta.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Movie, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                    }
                }

                // Delete Button Overlay
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC000000))
                        .align(Alignment.TopEnd)
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                }

                // Quality & Size Badge
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.BottomStart)
                ) {
                    TvBadge(
                        text = "${meta.quality} • ${String.format("%.1f", sizeGb)} ГБ",
                        backgroundColor = EmeraldGlow,
                        textColor = EmeraldPrimary
                    )
                }
            }

            // Info
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = meta.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!meta.subtitle.isNullOrBlank()) {
                    Text(
                        text = meta.subtitle,
                        color = EmeraldPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "▶ Нажмите для воспроизведения",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}
