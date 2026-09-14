package com.cineclaw.tv.feature.home

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.tv.material3.*
import coil3.compose.AsyncImage
import com.cineclaw.tv.core.designsystem.*
import com.cineclaw.tv.core.model.MediaItem
import com.cineclaw.tv.core.model.ResumeItem
import com.cineclaw.tv.core.model.Shelf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class CardActionTarget {
    data class ContinueWatching(val item: ResumeItem) : CardActionTarget()
    data class Watchlist(val media: MediaItem) : CardActionTarget()
    data class General(val media: MediaItem) : CardActionTarget()
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun HomeScreen(
    featuredItems: List<MediaItem>,
    continueWatchingItems: List<ResumeItem>,
    watchlistItems: List<MediaItem> = emptyList(),
    trackerFreshItems: List<MediaItem> = emptyList(),
    trackerHotlistItems: List<MediaItem> = emptyList(),
    uhd4kItems: List<MediaItem> = emptyList(),
    shelves: List<Shelf> = emptyList(),
    lastFocusedCardKey: String? = null,
    onCardFocused: (String) -> Unit = {},
    onSelectMedia: (MediaItem) -> Unit,
    onResumeClick: (ResumeItem) -> Unit,
    onOpenShelf: (shelfId: String, title: String) -> Unit = { _, _ -> },
    onNavigateNavRail: (String) -> Unit,
    onDeleteResumeItem: ((ResumeItem) -> Unit)? = null,
    onToggleWatchlist: ((MediaItem) -> Unit)? = null,
    onRemoveFromWatchlist: ((MediaItem) -> Unit)? = null
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var selectedActionTarget by remember { mutableStateOf<CardActionTarget?>(null) }

    var internalLastFocusedCardKey by rememberSaveable { mutableStateOf<String?>(null) }
    val effectiveCardKey = lastFocusedCardKey ?: internalLastFocusedCardKey
    val handleCardFocused: (String) -> Unit = { key ->
        internalLastFocusedCardKey = key
        onCardFocused(key)
    }

    val cardFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val drawerFocusRequester = remember { FocusRequester() }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        scope.launch {
            drawerState.setValue(DrawerValue.Closed)
            val key = effectiveCardKey
            if (key != null) {
                // Multi-frame retry: when navigating back from Details,
                // layout attachment occurs over 1-2 frames (~30-60ms).
                for (attempt in 0..5) {
                    delay(30)
                    val req = cardFocusRequesters[key]
                    if (req != null) {
                        try {
                            req.requestFocus()
                            break
                        } catch (e: Exception) {
                            // Node not yet attached to composition hierarchy
                        }
                    }
                }
            }
        }
    }

    BackHandler(enabled = drawerState.currentValue == DrawerValue.Open) {
        scope.launch {
            drawerState.setValue(DrawerValue.Closed)
            val key = effectiveCardKey
            if (key != null) {
                try {
                    cardFocusRequesters[key]?.requestFocus()
                } catch (e: Exception) {}
            }
        }
    }

    var isHeroFocused by remember { mutableStateOf(false) }

    val tvVerticalBringIntoViewSpec = remember(listState) {
        object : BringIntoViewSpec {
            override val scrollAnimationSpec: AnimationSpec<Float> = tween(
                durationMillis = 80,
                easing = FastOutSlowInEasing
            )

            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
                val item0 = listState.layoutInfo.visibleItemsInfo.find { it.index == 0 }
                val isInsideHero = isHeroFocused || (item0 != null && offset >= item0.offset && (offset + size) <= (item0.offset + item0.size + 40f))

                if (isInsideHero) {
                    val topOffset = item0?.offset?.toFloat() ?: -containerSize
                    return if (kotlin.math.abs(topOffset) < 4f) 0f else topOffset
                }

                val pivot = containerSize * 0.32f
                if (kotlin.math.abs(offset - pivot) < 14f) {
                    return 0f
                }
                return offset - pivot
            }
        }
    }

    val tvHorizontalBringIntoViewSpec = remember {
        object : BringIntoViewSpec {
            override val scrollAnimationSpec: AnimationSpec<Float> = tween(
                durationMillis = 75,
                easing = FastOutSlowInEasing
            )

            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
                val leadingEdge = offset
                val trailingEdge = offset + size
                val leftSafe = 68f
                val rightSafe = containerSize - 90f

                if (leadingEdge >= leftSafe && trailingEdge <= rightSafe) {
                    return 0f
                }
                return if (leadingEdge < leftSafe) {
                    leadingEdge - leftSafe
                } else {
                    trailingEdge - rightSafe
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { currentDrawerValue ->
            val isDrawerOpen = currentDrawerValue == DrawerValue.Open
            val drawerItemColors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = EmeraldPrimary,
                focusedContainerColor = EmeraldPrimary,
                selectedContentColor = ObsidianBackground,
                focusedContentColor = ObsidianBackground,
                containerColor = Color.Transparent,
                contentColor = TextSecondary,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = TextSecondary
            )

            val drawerItemModifier = Modifier
                .focusProperties {
                    canFocus = isDrawerOpen
                }
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                        keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                    ) {
                        scope.launch {
                            drawerState.setValue(DrawerValue.Closed)
                            val key = effectiveCardKey
                            if (key != null && cardFocusRequesters.containsKey(key)) {
                                try {
                                    cardFocusRequesters[key]?.requestFocus()
                                } catch (e: Exception) {}
                            }
                        }
                        true
                    } else {
                        false
                    }
                }

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(ObsidianSurface)
                    .padding(vertical = 24.dp, horizontal = 12.dp)
                    .focusProperties {
                        canFocus = isDrawerOpen
                    },
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Logo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(start = 6.dp, bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(EmeraldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "C", color = ObsidianBackground, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                    if (isDrawerOpen) {
                        Text(text = "CineClaw", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Navigation Items
                Column(
                    modifier = Modifier.focusProperties {
                        canFocus = isDrawerOpen
                    },
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NavigationDrawerItem(
                        selected = false,
                        enabled = isDrawerOpen,
                        onClick = { onNavigateNavRail("home") },
                        modifier = drawerItemModifier.focusRequester(drawerFocusRequester),
                        leadingContent = {
                            Icon(Icons.Default.Home, contentDescription = "Главная", modifier = Modifier.size(20.dp))
                        },
                        colors = drawerItemColors
                    ) {
                        Text(text = "Главная", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    NavigationDrawerItem(
                        selected = false,
                        enabled = isDrawerOpen,
                        onClick = { onNavigateNavRail("search") },
                        modifier = drawerItemModifier,
                        leadingContent = {
                            Icon(Icons.Default.Search, contentDescription = "Поиск", modifier = Modifier.size(20.dp))
                        },
                        colors = drawerItemColors
                    ) {
                        Text(text = "Поиск", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    NavigationDrawerItem(
                        selected = false,
                        enabled = isDrawerOpen,
                        onClick = { onNavigateNavRail("movies") },
                        modifier = drawerItemModifier,
                        leadingContent = {
                            Icon(Icons.Default.Movie, contentDescription = "Фильмы", modifier = Modifier.size(20.dp))
                        },
                        colors = drawerItemColors
                    ) {
                        Text(text = "Фильмы", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    NavigationDrawerItem(
                        selected = false,
                        enabled = isDrawerOpen,
                        onClick = { onNavigateNavRail("series") },
                        modifier = drawerItemModifier,
                        leadingContent = {
                            Icon(Icons.Default.Tv, contentDescription = "Сериалы", modifier = Modifier.size(20.dp))
                        },
                        colors = drawerItemColors
                    ) {
                        Text(text = "Сериалы", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    NavigationDrawerItem(
                        selected = false,
                        enabled = isDrawerOpen,
                        onClick = { onNavigateNavRail("4k") },
                        modifier = drawerItemModifier,
                        leadingContent = {
                            Icon(Icons.Default.HighQuality, contentDescription = "4K UHD", modifier = Modifier.size(20.dp))
                        },
                        colors = drawerItemColors
                    ) {
                        Text(text = "4K UHD", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    NavigationDrawerItem(
                        selected = false,
                        enabled = isDrawerOpen,
                        onClick = { onNavigateNavRail("watchlist") },
                        modifier = drawerItemModifier,
                        leadingContent = {
                            Icon(Icons.Default.Bookmark, contentDescription = "Буду смотреть", modifier = Modifier.size(20.dp))
                        },
                        colors = drawerItemColors
                    ) {
                        Text(text = "Буду смотреть", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Settings Item
                NavigationDrawerItem(
                    selected = false,
                    enabled = isDrawerOpen,
                    onClick = { onNavigateNavRail("settings") },
                    modifier = drawerItemModifier,
                    leadingContent = {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки", modifier = Modifier.size(20.dp))
                    },
                    colors = drawerItemColors
                ) {
                    Text(text = "Настройки", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        CompositionLocalProvider(LocalBringIntoViewSpec provides tvVerticalBringIntoViewSpec) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 68.dp)
                    .focusRestorer(),
                contentPadding = PaddingValues(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Top Featured Hero Carousel
                if (featuredItems.isNotEmpty()) {
                    item(key = "hero_carousel") {
                        TvHeroCarousel(
                            items = featuredItems,
                            cardFocusRequesters = cardFocusRequesters,
                            onCardFocused = handleCardFocused,
                            onRequestOpenDrawer = {
                                scope.launch {
                                    drawerState.setValue(DrawerValue.Open)
                                    drawerFocusRequester.requestFocus()
                                }
                            },
                            onWatchClick = { onSelectMedia(it) },
                            onDetailsClick = { onSelectMedia(it) },
                            onHeroFocusChange = { isHeroFocused = it }
                        )
                    }
                }

                // Continue Watching Shelf
                if (continueWatchingItems.isNotEmpty()) {
                    item(key = "continue_watching_shelf") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusGroup()
                                .focusRestorer()
                        ) {
                            Text(
                                text = "Продолжить просмотр",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 36.dp, end = 48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            CompositionLocalProvider(LocalBringIntoViewSpec provides tvHorizontalBringIntoViewSpec) {
                                val cwRowState = rememberLazyListState()
                                val (cwLazyRow, cwFirstItem) = remember { FocusRequester.createRefs() }
                                LazyRow(
                                    state = cwRowState,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(start = 36.dp, end = 56.dp, top = 12.dp, bottom = 18.dp),
                                    modifier = Modifier
                                        .focusRequester(cwLazyRow)
                                        .focusRestorer { cwFirstItem }
                                ) {
                                    itemsIndexed(continueWatchingItems, key = { index, item ->
                                        val idPart = item.effectiveTconst.ifBlank { item.id.toString() }
                                        "cw_${idPart}_${item.season}_${item.episode}_$index"
                                    }) { index, item ->
                                        val idPart = item.effectiveTconst.ifBlank { item.id.toString() }
                                        val cardKey = "cw_${idPart}_${item.season}_${item.episode}_$index"
                                        val requester = cardFocusRequesters.getOrPut(cardKey) { FocusRequester() }
                                        val itemFocusModifier = if (index == 0) Modifier.focusRequester(cwFirstItem) else Modifier
                                        TvWideCard(
                                            title = item.displayTitle,
                                            subtitle = if (item.season > 0) "Сезон ${item.season} • Серия ${item.episode}" else item.year?.toString(),
                                            backdropUrl = item.effectiveBackdrop ?: item.effectivePoster,
                                            progressPercent = item.playbackPercent.toInt(),
                                            timecode = item.timecodeFormatted,
                                            isNextUp = item.isNextUp,
                                            surfaceModifier = Modifier
                                                .then(itemFocusModifier)
                                                .focusRequester(requester)
                                                .onFocusChanged { if (it.isFocused) handleCardFocused(cardKey) }
                                                .onPreviewKeyEvent { event ->
                                                    if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                                                        event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_LEFT &&
                                                        index == 0
                                                    ) {
                                                        scope.launch {
                                                            drawerState.setValue(DrawerValue.Open)
                                                            drawerFocusRequester.requestFocus()
                                                        }
                                                        true
                                                    } else false
                                                },
                                            onClick = {
                                                cwLazyRow.saveFocusedChild()
                                                handleCardFocused(cardKey)
                                                onResumeClick(item)
                                            },
                                            onLongClick = {
                                                selectedActionTarget = CardActionTarget.ContinueWatching(item)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                val openDrawerAction: () -> Unit = {
                    scope.launch {
                        drawerState.setValue(DrawerValue.Open)
                        drawerFocusRequester.requestFocus()
                    }
                }

                // Watchlist Shelf ("Буду смотреть")
                if (watchlistItems.isNotEmpty()) {
                    item(key = "watchlist_shelf") {
                        TvMediaShelfRow(
                            shelfId = "watchlist",
                            title = "Буду смотреть",
                            items = watchlistItems,
                            cardFocusRequesters = cardFocusRequesters,
                            onCardFocused = handleCardFocused,
                            onRequestOpenDrawer = openDrawerAction,
                            onSelectMedia = onSelectMedia,
                            onOpenShelf = { onNavigateNavRail("watchlist") },
                            horizontalBringIntoViewSpec = tvHorizontalBringIntoViewSpec,
                            onLongClickMedia = { media ->
                                selectedActionTarget = CardActionTarget.Watchlist(media)
                            }
                        )
                    }
                }

                // Fresh Releases on Trackers
                if (trackerFreshItems.isNotEmpty()) {
                    item(key = "tracker_fresh_shelf") {
                        TvMediaShelfRow(
                            shelfId = "tracker_fresh",
                            title = "Новинки на трекерах",
                            items = trackerFreshItems,
                            cardFocusRequesters = cardFocusRequesters,
                            onCardFocused = handleCardFocused,
                            onRequestOpenDrawer = openDrawerAction,
                            onSelectMedia = onSelectMedia,
                            onOpenShelf = { onOpenShelf("tracker_fresh", "Новинки на трекерах") },
                            horizontalBringIntoViewSpec = tvHorizontalBringIntoViewSpec,
                            onLongClickMedia = { media ->
                                selectedActionTarget = CardActionTarget.General(media)
                            }
                        )
                    }
                }

                // Popular on Trackers (Swarm Hotlist)
                if (trackerHotlistItems.isNotEmpty()) {
                    item(key = "tracker_hotlist_shelf") {
                        TvMediaShelfRow(
                            shelfId = "tracker_hotlist",
                            title = "Популярно на трекерах",
                            items = trackerHotlistItems,
                            cardFocusRequesters = cardFocusRequesters,
                            onCardFocused = handleCardFocused,
                            onRequestOpenDrawer = openDrawerAction,
                            onSelectMedia = onSelectMedia,
                            onOpenShelf = { onOpenShelf("tracker_hotlist", "Популярно на трекерах") },
                            horizontalBringIntoViewSpec = tvHorizontalBringIntoViewSpec,
                            onLongClickMedia = { media ->
                                selectedActionTarget = CardActionTarget.General(media)
                            }
                        )
                    }
                }

                // 4K UHD Кинозал
                if (uhd4kItems.isNotEmpty()) {
                    item(key = "uhd_4k_shelf") {
                        TvMediaShelfRow(
                            shelfId = "uhd_4k",
                            title = "4K UHD Кинозал",
                            items = uhd4kItems,
                            cardFocusRequesters = cardFocusRequesters,
                            onCardFocused = handleCardFocused,
                            onRequestOpenDrawer = openDrawerAction,
                            onSelectMedia = onSelectMedia,
                            onOpenShelf = { onOpenShelf("uhd_4k", "4K UHD Кинозал") },
                            horizontalBringIntoViewSpec = tvHorizontalBringIntoViewSpec,
                            onLongClickMedia = { media ->
                                selectedActionTarget = CardActionTarget.General(media)
                            }
                        )
                    }
                }

                // Curated Shelves (TMDB Feeds)
                shelves.forEachIndexed { shelfIndex, shelf ->
                    if (shelf.items.isNotEmpty()) {
                        item(key = "shelf_${shelf.id}_$shelfIndex") {
                            TvMediaShelfRow(
                                shelfId = shelf.id,
                                title = shelf.title,
                                items = shelf.items,
                                cardFocusRequesters = cardFocusRequesters,
                                onCardFocused = handleCardFocused,
                                onRequestOpenDrawer = openDrawerAction,
                                onSelectMedia = onSelectMedia,
                                onOpenShelf = { onOpenShelf(shelf.id, shelf.title) },
                                horizontalBringIntoViewSpec = tvHorizontalBringIntoViewSpec,
                                onLongClickMedia = { media ->
                                    selectedActionTarget = CardActionTarget.General(media)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Long Press Action Dialog Modal
    selectedActionTarget?.let { target ->
        when (target) {
            is CardActionTarget.ContinueWatching -> {
                val subtitle = if (target.item.season > 0) {
                    "Сезон ${target.item.season} • Серия ${target.item.episode}"
                } else target.item.year?.toString()
                CardActionDialog(
                    title = target.item.displayTitle,
                    subtitle = subtitle,
                    imageUrl = target.item.effectiveBackdrop ?: target.item.effectivePoster,
                    actions = listOf(
                        CardAction(
                            title = "Удалить из «Продолжить просмотр»",
                            icon = Icons.Default.Delete,
                            isDestructive = true,
                            onClick = {
                                onDeleteResumeItem?.invoke(target.item)
                            }
                        )
                    ),
                    onDismiss = { selectedActionTarget = null }
                )
            }
            is CardActionTarget.Watchlist -> {
                CardActionDialog(
                    title = target.media.displayTitle,
                    subtitle = target.media.originalTitle ?: target.media.year?.toString(),
                    imageUrl = target.media.effectivePoster,
                    actions = listOf(
                        CardAction(
                            title = "Удалить из «Буду смотреть»",
                            icon = Icons.Default.BookmarkBorder,
                            isDestructive = true,
                            onClick = {
                                onRemoveFromWatchlist?.invoke(target.media)
                            }
                        ),
                        CardAction(
                            title = "Открыть описание",
                            icon = Icons.Default.Info,
                            isDestructive = false,
                            onClick = {
                                onSelectMedia(target.media)
                            }
                        )
                    ),
                    onDismiss = { selectedActionTarget = null }
                )
            }
            is CardActionTarget.General -> {
                val isInWl = watchlistItems.any { it.effectiveTconst == target.media.effectiveTconst }
                CardActionDialog(
                    title = target.media.displayTitle,
                    subtitle = target.media.originalTitle ?: target.media.year?.toString(),
                    imageUrl = target.media.effectivePoster,
                    actions = listOf(
                        if (isInWl) {
                            CardAction(
                                title = "Удалить из «Буду смотреть»",
                                icon = Icons.Default.BookmarkBorder,
                                isDestructive = true,
                                onClick = {
                                    onRemoveFromWatchlist?.invoke(target.media)
                                }
                            )
                        } else {
                            CardAction(
                                title = "Добавить в «Буду смотреть»",
                                icon = Icons.Default.Bookmark,
                                isDestructive = false,
                                onClick = {
                                    onToggleWatchlist?.invoke(target.media)
                                }
                            )
                        },
                        CardAction(
                            title = "Открыть описание",
                            icon = Icons.Default.Info,
                            isDestructive = false,
                            onClick = {
                                onSelectMedia(target.media)
                            }
                        )
                    ),
                    onDismiss = { selectedActionTarget = null }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun TvMediaShelfRow(
    shelfId: String,
    title: String,
    items: List<MediaItem>,
    cardFocusRequesters: MutableMap<String, FocusRequester>,
    onCardFocused: (String) -> Unit,
    onRequestOpenDrawer: () -> Unit,
    onSelectMedia: (MediaItem) -> Unit,
    onOpenShelf: () -> Unit,
    horizontalBringIntoViewSpec: BringIntoViewSpec,
    onLongClickMedia: ((MediaItem) -> Unit)? = null
) {
    val shelfRowState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusGroup()
            .focusRestorer()
    ) {
        // Shelf Title (Header without focusable "Ещё" to eliminate vertical D-Pad conflicts)
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 36.dp, end = 48.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Edge-to-edge LazyRow with safe padding so scale & glow are never clipped
        CompositionLocalProvider(LocalBringIntoViewSpec provides horizontalBringIntoViewSpec) {
            val (lazyRow, firstItem) = remember { FocusRequester.createRefs() }
            LazyRow(
                state = shelfRowState,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(start = 36.dp, end = 56.dp, top = 14.dp, bottom = 20.dp),
                modifier = Modifier
                    .focusRequester(lazyRow)
                    .focusRestorer { firstItem }
            ) {
                itemsIndexed(items, key = { index, media ->
                    val idPart = media.effectiveTconst.ifBlank { media.id?.toString() ?: media.title }
                    "${shelfId}_${idPart}_$index"
                }) { index, media ->
                    val idPart = media.effectiveTconst.ifBlank { media.id?.toString() ?: media.title }
                    val cardKey = "${shelfId}_${idPart}_$index"
                    val requester = cardFocusRequesters.getOrPut(cardKey) { FocusRequester() }
                    val seedBadge = if ((media.seeds ?: 0) > 0) "🌱 ${media.seeds}" else null
                    val itemFocusModifier = if (index == 0) Modifier.focusRequester(firstItem) else Modifier
                    TvCinemaCard(
                        title = media.displayTitle,
                        posterUrl = media.effectivePoster,
                        year = media.year,
                        rating = media.rating,
                        qualityBadge = seedBadge ?: if (shelfId.contains("4k") || shelfId.contains("uhd")) "4K UHD" else null,
                        surfaceModifier = Modifier
                            .then(itemFocusModifier)
                            .focusRequester(requester)
                            .onFocusChanged { if (it.isFocused) onCardFocused(cardKey) }
                            .onPreviewKeyEvent { event ->
                                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                                    event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_LEFT &&
                                    index == 0
                                ) {
                                    onRequestOpenDrawer()
                                    true
                                } else false
                            },
                        onClick = {
                            lazyRow.saveFocusedChild()
                            onCardFocused(cardKey)
                            onSelectMedia(media)
                        },
                        onLongClick = onLongClickMedia?.let { { it(media) } }
                    )
                }

                // Trailing "Показать все" card
                item {
                    var isCardFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .height(195.dp)
                            .tvFocusable(
                                focusedScale = 1.05f,
                                cornerRadius = 16.dp,
                                onFocusChange = { isCardFocused = it }
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isCardFocused) ObsidianSurfaceVariant else ObsidianSurface)
                            .border(
                                width = if (isCardFocused) 2.dp else 1.dp,
                                color = if (isCardFocused) EmeraldPrimary else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .tvClickable { onOpenShelf() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = if (isCardFocused) EmeraldPrimary else TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Все\nрелизы",
                                color = if (isCardFocused) TextPrimary else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun TvHeroCarousel(
    items: List<MediaItem>,
    cardFocusRequesters: MutableMap<String, FocusRequester>,
    onCardFocused: (String) -> Unit,
    onRequestOpenDrawer: () -> Unit,
    onWatchClick: (MediaItem) -> Unit,
    onDetailsClick: (MediaItem) -> Unit,
    onHeroFocusChange: (Boolean) -> Unit = {}
) {
    val heroItems = remember(items) { items.take(5) }
    if (heroItems.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(0) }
    var isInteracting by remember { mutableStateOf(false) }

    // Auto-advance timer every 8 seconds when user is not focused on hero actions
    LaunchedEffect(heroItems, isInteracting) {
        if (heroItems.size > 1 && !isInteracting) {
            while (true) {
                delay(8000)
                if (!isInteracting) {
                    currentIndex = (currentIndex + 1) % heroItems.size
                }
            }
        }
    }

    val currentMedia = heroItems.getOrNull(currentIndex) ?: heroItems.first()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .focusGroup()
            .focusRestorer()
            .onFocusChanged { focusState ->
                isInteracting = focusState.hasFocus
                onHeroFocusChange(focusState.hasFocus)
            }
    ) {
        // Smooth crossfade between background backdrops
        Crossfade(
            targetState = currentMedia,
            animationSpec = tween(600),
            label = "hero_backdrop"
        ) { media ->
            val backdrop = media.effectiveBackdrop ?: media.backdropUrl
            Box(modifier = Modifier.fillMaxSize()) {
                if (!backdrop.isNullOrBlank()) {
                    AsyncImage(
                        model = backdrop,
                        contentDescription = media.displayTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Multi-Stop Cinema Obsidian Gradients
        // 1. Horizontal gradient: deep obsidian on text side, smooth ease to crystal clear backdrop on right
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to ObsidianBackground,
                        0.22f to ObsidianBackground.copy(alpha = 0.95f),
                        0.42f to ObsidianBackground.copy(alpha = 0.68f),
                        0.68f to ObsidianBackground.copy(alpha = 0.15f),
                        1.0f to Color.Transparent
                    )
                )
        )
        // 2. Vertical bottom gradient: smooth blend into ObsidianBackground for next shelf
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.48f to Color.Transparent,
                        0.74f to ObsidianBackground.copy(alpha = 0.65f),
                        1.0f to ObsidianBackground
                    )
                )
        )
        // 3. Top subtle vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to ObsidianBackground.copy(alpha = 0.35f),
                        0.15f to Color.Transparent
                    )
                )
        )

        // Title, Badges, Plot & Actions
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 36.dp, bottom = 24.dp)
                .widthIn(max = 680.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Crossfade(
                targetState = currentMedia,
                animationSpec = tween(400),
                label = "hero_info"
            ) { media ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Badges Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TvBadge(
                            text = "🔥 Тренды недели",
                            backgroundColor = AmberGlow,
                            textColor = AmberUHD
                        )
                        if (media.rating != null && media.rating > 0) {
                            TvRatingBadge(rating = media.rating)
                        }
                        if (media.year != null && media.year > 0) {
                            TvBadge(text = media.year.toString())
                        }
                        TvBadge(
                            text = if (media.type == "TvSeries" || media.isTv) "Сериал" else "Фильм",
                            backgroundColor = EmeraldGlow,
                            textColor = EmeraldPrimary
                        )
                    }

                    // Main Title
                    Text(
                        text = media.displayTitle,
                        color = TextPrimary,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Plot Overview
                    if (!media.overview.isNullOrBlank()) {
                        Text(
                            text = media.overview,
                            color = TextSecondary,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 19.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Action Buttons (Only Смотреть & Подробнее - NO ARROWS)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val watchRequester = cardFocusRequesters.getOrPut("hero_watch") { FocusRequester() }
                TvActionButton(
                    text = "Смотреть",
                    icon = Icons.Default.PlayArrow,
                    isPrimary = true,
                    onClick = {
                        onCardFocused("hero_watch")
                        onWatchClick(currentMedia)
                    },
                    modifier = Modifier
                        .focusRequester(watchRequester)
                        .onFocusChanged { if (it.isFocused) onCardFocused("hero_watch") }
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                                keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                            ) {
                                onRequestOpenDrawer()
                                true
                            } else false
                        }
                )

                val detailsRequester = cardFocusRequesters.getOrPut("hero_details") { FocusRequester() }
                TvActionButton(
                    text = "Подробнее",
                    icon = Icons.Default.Info,
                    isPrimary = false,
                    onClick = {
                        onCardFocused("hero_details")
                        onDetailsClick(currentMedia)
                    },
                    modifier = Modifier
                        .focusRequester(detailsRequester)
                        .onFocusChanged { if (it.isFocused) onCardFocused("hero_details") }
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                                keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                            ) {
                                if (heroItems.size > 1) {
                                    currentIndex = (currentIndex + 1) % heroItems.size
                                    true
                                } else false
                            } else false
                        }
                )
            }
        }

        // Slide Indicators in Bottom-Right Corner
        if (heroItems.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 48.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                heroItems.indices.forEach { idx ->
                    val isActive = idx == currentIndex
                    val width by animateDpAsState(
                        targetValue = if (isActive) 24.dp else 8.dp,
                        animationSpec = tween(300),
                        label = "hero_dot_w"
                    )
                    val color by animateColorAsState(
                        targetValue = if (isActive) EmeraldPrimary else Color.White.copy(alpha = 0.28f),
                        animationSpec = tween(300),
                        label = "hero_dot_c"
                    )
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(width)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color)
                    )
                }
            }
        }
    }
}
