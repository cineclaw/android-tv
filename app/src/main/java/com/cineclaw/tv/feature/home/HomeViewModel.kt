package com.cineclaw.tv.feature.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cineclaw.tv.CineClawApp
import com.cineclaw.tv.core.model.HomePayload
import com.cineclaw.tv.core.model.HomeShelf
import com.cineclaw.tv.core.model.MediaItem
import com.cineclaw.tv.core.model.ResumeItem
import com.cineclaw.tv.core.model.Shelf
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    var homePayload by mutableStateOf<HomePayload?>(null)
        private set

    var featured by mutableStateOf<List<MediaItem>>(emptyList())
        private set
    var continueWatching by mutableStateOf<List<ResumeItem>>(emptyList())
        private set
    var watchlistItems by mutableStateOf<List<MediaItem>>(emptyList())
        private set
    var trackerFresh by mutableStateOf<List<MediaItem>>(emptyList())
        private set
    var trackerHotlist by mutableStateOf<List<MediaItem>>(emptyList())
        private set
    var uhd4k by mutableStateOf<List<MediaItem>>(emptyList())
        private set
    var shelves by mutableStateOf<List<Shelf>>(emptyList())
        private set
    var homeShelves by mutableStateOf<List<HomeShelf>>(emptyList())
        private set

    var isLoaded by mutableStateOf(false)
        private set

    var lastFocusedCardKey by mutableStateOf<String?>(null)

    fun loadData(app: CineClawApp, forceRefresh: Boolean = false) {
        if (isLoaded && !forceRefresh) return
        viewModelScope.launch {
            val api = app.apiClient.getApi()
            try {
                // Unified Server-Driven UI: Exactly 1 single network request (<15ms)
                val payload = api.getHomeFeed(platform = "tv", refresh = forceRefresh)
                applyHomePayload(payload)
                isLoaded = true
            } catch (e: Exception) {
                android.util.Log.w("HomeViewModel", "getHomeFeed failed, falling back to legacy multi-call: ${e.message}")
                loadDataLegacy(app)
            }
        }
    }

    private fun applyHomePayload(payload: HomePayload) {
        homePayload = payload
        homeShelves = payload.shelves

        // Map hero items to MediaItem for TvHeroCarousel
        featured = payload.hero.map { it.toMediaItem() }

        // Populate typed lists for both legacy and server-driven consumers
        val cwShelf = payload.shelves.find { it.id == "continue_watching" }
        continueWatching = cwShelf?.items?.map { it.toResumeItem() } ?: emptyList()

        val wlShelf = payload.shelves.find { it.id == "watchlist" }
        watchlistItems = wlShelf?.items?.map { it.toMediaItem() } ?: emptyList()

        val freshShelf = payload.shelves.find { it.id == "tracker_fresh" }
        trackerFresh = freshShelf?.items?.map { it.toMediaItem() } ?: emptyList()

        val hotlistShelf = payload.shelves.find { it.id == "tracker_hotlist" }
        trackerHotlist = hotlistShelf?.items?.map { it.toMediaItem() } ?: emptyList()

        val uhdShelf = payload.shelves.find { it.id == "uhd_4k" }
        uhd4k = uhdShelf?.items?.map { it.toMediaItem() } ?: emptyList()

        // Shelves from TMDB feeds (filtering out special tracker/cw/wl shelves)
        val specialIds = setOf("continue_watching", "watchlist", "tracker_fresh", "tracker_hotlist", "uhd_4k")
        shelves = payload.shelves
            .filter { it.id !in specialIds }
            .map { shelf ->
                Shelf(
                    id = shelf.id,
                    title = shelf.title,
                    items = shelf.items.map { it.toMediaItem() }
                )
            }
    }

    private fun loadDataLegacy(app: CineClawApp) {
        viewModelScope.launch {
            val api = app.apiClient.getApi()
            launch {
                try {
                    val fetchedShelves = api.getFeeds()
                    shelves = fetchedShelves
                    if (featured.isEmpty() && fetchedShelves.isNotEmpty()) {
                        featured = fetchedShelves.first().items.take(5)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("HomeViewModel", "getFeeds failed: ${e.message}")
                }
            }
            launch {
                try {
                    continueWatching = api.getContinueWatching()
                } catch (e: Exception) {
                    android.util.Log.w("HomeViewModel", "getContinueWatching failed: ${e.message}")
                }
            }
            launch {
                try {
                    watchlistItems = api.getWatchlist()
                } catch (e: Exception) {
                    android.util.Log.w("HomeViewModel", "getWatchlist failed: ${e.message}")
                }
            }
            launch {
                try {
                    trackerFresh = api.getHotlist(type = "new_movie").items
                } catch (e: Exception) {
                    android.util.Log.w("HomeViewModel", "getHotlist new_movie failed: ${e.message}")
                }
            }
            launch {
                try {
                    trackerHotlist = api.getHotlist(type = "movie").items
                } catch (e: Exception) {
                    android.util.Log.w("HomeViewModel", "getHotlist movie failed: ${e.message}")
                }
            }
            launch {
                try {
                    uhd4k = api.getHotlist(type = "movie", quality = "4k").items
                } catch (e: Exception) {
                    android.util.Log.w("HomeViewModel", "getHotlist 4k failed: ${e.message}")
                }
            }
            isLoaded = true
        }
    }

    fun refreshWatchState(app: CineClawApp) {
        viewModelScope.launch {
            val api = app.apiClient.getApi()
            try {
                // Single fast request (<1ms) to update watch state
                val payload = api.getHomeFeed(platform = "tv", refresh = true)
                applyHomePayload(payload)
            } catch (e: Exception) {
                // Fallback to legacy
                launch {
                    try {
                        continueWatching = api.getContinueWatching()
                    } catch (e: Exception) {}
                }
                launch {
                    try {
                        watchlistItems = api.getWatchlist()
                    } catch (e: Exception) {}
                }
            }
        }
    }

    fun removeFromContinueWatching(app: CineClawApp, tconst: String) {
        continueWatching = continueWatching.filter { it.effectiveTconst != tconst }
        homeShelves = homeShelves.map { shelf ->
            if (shelf.id == "continue_watching") {
                shelf.copy(items = shelf.items.filter { it.tconst != tconst && it.id != tconst })
            } else shelf
        }.filter { it.id != "continue_watching" || it.items.isNotEmpty() }

        viewModelScope.launch {
            try {
                app.apiClient.getApi().deleteResumeItem(tconst = tconst, all = true)
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "deleteResumeItem failed: ${e.message}")
            }
        }
    }

    fun addToWatchlist(app: CineClawApp, media: MediaItem) {
        if (watchlistItems.none { it.effectiveTconst == media.effectiveTconst }) {
            watchlistItems = listOf(media) + watchlistItems
            var found = false
            homeShelves = homeShelves.map { shelf ->
                if (shelf.id == "watchlist") {
                    found = true
                    shelf.copy(
                        items = listOf(media.toHomeItem()) + shelf.items,
                        badge = watchlistItems.size.toString()
                    )
                } else shelf
            }
            if (!found) {
                val newWlShelf = HomeShelf(
                    id = "watchlist",
                    title = "Буду смотреть",
                    type = "poster",
                    badge = "1",
                    actionRoute = "watchlist",
                    items = listOf(media.toHomeItem())
                )
                val insertIdx = if (homeShelves.any { it.id == "continue_watching" }) 1 else 0
                val updated = homeShelves.toMutableList()
                updated.add(insertIdx.coerceAtMost(updated.size), newWlShelf)
                homeShelves = updated
            }
        }

        viewModelScope.launch {
            try {
                val req = com.cineclaw.tv.core.model.WatchlistAddRequest(
                    imdbId = media.effectiveTconst,
                    mediaType = media.mediaType ?: if (media.isTv) "tv" else "movie",
                    title = media.displayTitle,
                    originalTitle = media.originalTitle,
                    year = media.year,
                    rating = media.rating,
                    posterPath = media.posterUrl ?: media.posterPath,
                    backdropPath = media.backdropUrl ?: media.backdropPath
                )
                app.apiClient.getApi().addToWatchlist(req)
                refreshWatchState(app)
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "addToWatchlist failed: ${e.message}")
            }
        }
    }

    fun removeFromWatchlist(app: CineClawApp, tconst: String) {
        watchlistItems = watchlistItems.filter { it.effectiveTconst != tconst }
        homeShelves = homeShelves.map { shelf ->
            if (shelf.id == "watchlist") {
                shelf.copy(
                    items = shelf.items.filter { it.tconst != tconst && it.id != tconst },
                    badge = watchlistItems.size.toString()
                )
            } else shelf
        }.filter { it.id != "watchlist" || it.items.isNotEmpty() }

        viewModelScope.launch {
            try {
                app.apiClient.getApi().removeFromWatchlist(tconst = tconst)
                refreshWatchState(app)
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "removeFromWatchlist failed: ${e.message}")
            }
        }
    }
}
