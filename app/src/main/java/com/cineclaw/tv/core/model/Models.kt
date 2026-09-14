package com.cineclaw.tv.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object ApiConfig {
    @Volatile
    var baseUrl: String = "http://192.168.88.126:3000"
}

fun resolveImageUrl(pathOrUrl: String?, defaultPrefix: String = "https://image.tmdb.org/t/p/w500"): String? {
    val clean = pathOrUrl?.takeIf { it.isNotBlank() } ?: return null
    return when {
        clean.startsWith("http://") || clean.startsWith("https://") -> clean
        clean.startsWith("/poster/") -> null
        clean.startsWith("/") -> "$defaultPrefix$clean"
        else -> "$defaultPrefix/$clean"
    }
}

@Serializable
data class MediaItem(
    val id: Long? = null,
    val tconst: String = "",
    val title: String = "",
    @SerialName("ru_title") val ruTitle: String? = null,
    @SerialName("original_title") val originalTitle: String? = null,
    val year: Int? = null,
    val rating: Double? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("poster_url") val posterUrl: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("backdrop_url") val backdropUrl: String? = null,
    val overview: String? = null,
    val genres: List<String> = emptyList(),
    @SerialName("media_type") val mediaType: String? = null,
    val type: String = "Movie",
    @SerialName("seasons_count") val seasonsCount: Int? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    val seeds: Int? = null,
    @SerialName("imdb_id") val imdbId: String? = null,
    @SerialName("runtime_minutes") val runtimeMinutes: Int? = null
) {
    val displayTitle: String get() = ruTitle?.takeIf { it.isNotBlank() } ?: title
    val effectiveId: Long? get() = id?.takeIf { it > 0L } ?: tconst.removePrefix("tmdb_").toLongOrNull() ?: imdbId?.removePrefix("tmdb_")?.toLongOrNull()
    val effectiveTconst: String get() = tconst.ifEmpty { imdbId.orEmpty() }.ifEmpty { "tmdb_${effectiveId ?: 0}" }
    val isTv: Boolean get() = mediaType.equals("tv", ignoreCase = true) ||
                              type.equals("tvSeries", ignoreCase = true) ||
                              type.equals("tvMiniSeries", ignoreCase = true) ||
                              (seasonsCount ?: 0) > 0
    val effectivePoster: String? get() = resolveImageUrl(posterUrl ?: posterPath, "https://image.tmdb.org/t/p/w500")
    val effectiveBackdrop: String? get() = resolveImageUrl(backdropUrl ?: backdropPath, "https://image.tmdb.org/t/p/w1280")

    fun toHomeItem(): HomeItem {
        return HomeItem(
            id = effectiveTconst,
            tmdbId = effectiveId,
            tconst = effectiveTconst,
            mediaType = mediaType ?: if (isTv) "tv" else "movie",
            title = displayTitle,
            originalTitle = originalTitle,
            year = year,
            rating = rating ?: 0.0,
            posterPath = posterPath ?: posterUrl,
            backdropPath = backdropPath ?: backdropUrl,
            overview = overview,
            seeds = seeds ?: 0
        )
    }
}

@Serializable
data class TorrentRelease(
    val id: String = "",
    val title: String = "",
    @SerialName("info_hash") val infoHash: String = "",
    val size: Long = 0L,
    val seeds: Int = 0,
    val peers: Int = 0,
    val tracker: String = "",
    val resolution: String = "1080p",
    @SerialName("audio_label") val audioLabel: String? = null,
    @SerialName("video_codec") val videoCodec: String? = null,
    val magnet: String = "",
    val tier: String = "1080p",
    @SerialName("bitrate_mbps") val bitrateMbps: Double = 0.0,
    @SerialName("stream_url") val streamUrl: String? = null,
    val quality: String = "1080p",
    val hash: String = "",
    @SerialName("seasons") val seasons: List<Int> = emptyList()
) {
    val effectiveHash: String get() = hash.ifEmpty { infoHash.ifEmpty { id } }
    val sizeFormatted: String get() {
        val gb = size / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1.0) String.format("%.2f ГБ", gb) else String.format("%.0f МБ", size / (1024.0 * 1024.0))
    }

    val bitrateFormatted: String get() {
        return if (bitrateMbps > 0) String.format("%.1f Мбит/с", bitrateMbps) else ""
    }
}

@Serializable
data class QualityGroup(
    val tier: String,
    val title: String,
    val badge: String,
    val releases: List<TorrentRelease>
)

@Serializable
data class EpisodeInfo(
    val id: String = "",
    val name: String = "",
    @SerialName("season_number") val seasonNumber: Int = 1,
    @SerialName("episode_number") val episodeNumber: Int = 1,
    @SerialName("still_path") val stillPath: String? = null,
    val overview: String? = null,
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("resume_seconds") val resumeSeconds: Double = 0.0,
    @SerialName("is_played") val isPlayed: Boolean = false
) {
    val effectiveStill: String? get() = resolveImageUrl(stillPath, "https://image.tmdb.org/t/p/w500")
}

@Serializable
data class SeasonProgressSummary(
    @SerialName("season_number") val seasonNumber: Int = 1,
    @SerialName("total_episodes") val totalEpisodes: Int = 0,
    @SerialName("watched_episodes") val watchedEpisodes: Int = 0,
    @SerialName("is_completed") val isCompleted: Boolean = false
)

@Serializable
data class EpisodeProgressStatus(
    @SerialName("season_number") val seasonNumber: Int = 1,
    @SerialName("episode_number") val episodeNumber: Int = 1,
    @SerialName("position_seconds") val positionSeconds: Double = 0.0,
    @SerialName("duration_seconds") val durationSeconds: Double = 0.0,
    @SerialName("playback_percent") val playbackPercent: Double = 0.0,
    @SerialName("is_completed") val isCompleted: Boolean = false
)

@Serializable
data class SeriesProgressResponse(
    @SerialName("imdb_id") val imdbId: String = "",
    @SerialName("total_episodes") val totalEpisodes: Int = 0,
    @SerialName("total_watched") val totalWatched: Int = 0,
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("has_unwatched_prior") val hasUnwatchedPrior: Boolean = false,
    @SerialName("latest_watched_season") val latestWatchedSeason: Int? = null,
    @SerialName("latest_watched_episode") val latestWatchedEpisode: Int? = null,
    val seasons: Map<String, SeasonProgressSummary> = emptyMap(),
    val episodes: Map<String, EpisodeProgressStatus> = emptyMap()
)

@Serializable
data class MarkWatchedRequest(
    @SerialName("imdb_id") val imdbId: String,
    val mode: String,
    val title: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    @SerialName("up_to_season") val upToSeason: Int? = null,
    @SerialName("up_to_episode") val upToEpisode: Int? = null,
    val completed: Boolean = true
)

@Serializable
data class AudioTrack(
    val index: Int = 0,
    val title: String = "",
    val language: String = "rus",
    val codec: String = "",
    val channels: Int = 2,
    @SerialName("is_default") val isDefault: Boolean = false
)

@Serializable
data class SubtitleTrack(
    val index: Int = 0,
    val title: String = "",
    val language: String = "rus",
    val codec: String = "",
    @SerialName("is_default") val isDefault: Boolean = false
)

@Serializable
data class CastMember(
    val id: Long? = null,
    val name: String = "",
    val character: String? = null,
    @SerialName("profile_path") val profilePath: String? = null
) {
    val effectiveAvatar: String? get() = resolveImageUrl(profilePath, "https://image.tmdb.org/t/p/w185")
}

@Serializable
data class CrewMember(
    val id: Long? = null,
    val name: String = "",
    val job: String? = null,
    val department: String? = null,
    @SerialName("profile_path") val profilePath: String? = null
) {
    val effectiveAvatar: String? get() = resolveImageUrl(profilePath, "https://image.tmdb.org/t/p/w185")
}

@Serializable
data class CriticScores(
    @SerialName("rotten_tomatoes") val rottenTomatoes: Int? = null,
    val metacritic: Int? = null,
    val imdb: Double? = null,
    @SerialName("imdb_votes") val imdbVotes: String? = null,
    val awards: String? = null
)

@Serializable
data class CriticSummaryResponse(
    val tconst: String = "",
    val verdict: String = "",
    val tone: String = "positive",
    val scores: CriticScores = CriticScores(),
    val pros: List<String> = emptyList(),
    val cons: List<String> = emptyList(),
    @SerialName("target_audience") val targetAudience: String = "",
    val model: String = "Gemini 2.5 Flash",
    val cached: Boolean = false
)

@Serializable
data class PersonCreditItem(
    val id: Long = 0,
    @SerialName("media_type") val mediaType: String = "movie",
    val title: String = "",
    @SerialName("original_title") val originalTitle: String? = null,
    val character: String? = null,
    val job: String? = null,
    val department: String? = null,
    val year: Int? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null
) {
    val displayTitle: String get() = title.ifEmpty { originalTitle.orEmpty() }
    val effectivePoster: String? get() = resolveImageUrl(posterPath, "https://image.tmdb.org/t/p/w500")
    val effectiveBackdrop: String? get() = resolveImageUrl(backdropPath, "https://image.tmdb.org/t/p/w1280")
}

@Serializable
data class PersonDetailsResponse(
    val id: Long = 0,
    val name: String = "",
    val biography: String = "",
    val birthday: String? = null,
    val deathday: String? = null,
    @SerialName("place_of_birth") val placeOfBirth: String? = null,
    @SerialName("known_for_department") val knownForDepartment: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
    @SerialName("imdb_id") val imdbId: String? = null,
    val cast: List<PersonCreditItem> = emptyList(),
    val crew: List<PersonCreditItem> = emptyList()
) {
    val effectiveAvatar: String? get() = resolveImageUrl(profilePath, "https://image.tmdb.org/t/p/w500")
}

@Serializable
data class VideoItem(
    val id: String = "",
    val name: String = "",
    val key: String = "",
    val site: String = "YouTube",
    @SerialName("type") val type: String = "Trailer",
    val official: Boolean = false,
    @SerialName("published_at") val publishedAt: String? = null
) {
    val thumbnailUrl: String get() = "https://img.youtube.com/vi/$key/hqdefault.jpg"
    val isYouTube: Boolean get() = site.equals("YouTube", ignoreCase = true)
}

@Serializable
data class MovieMetadataResponse(
    val tconst: String = "",
    @SerialName("tmdb_id") val tmdbId: Long? = null,
    val title: String = "",
    @SerialName("original_title") val originalTitle: String? = null,
    val overview: String? = null,
    val year: Int? = null,
    val rating: Double? = null,
    val genres: List<String> = emptyList(),
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    val cast: List<CastMember> = emptyList(),
    val crew: List<CrewMember> = emptyList(),
    val videos: List<VideoItem> = emptyList(),
    val backdrops: List<String> = emptyList(),
    @SerialName("runtime_minutes") val runtimeMinutes: Int? = null
)

@Serializable
data class SeasonSummary(
    @SerialName("season_number") val seasonNumber: Int = 1,
    val name: String = "",
    @SerialName("episode_count") val episodeCount: Int = 0,
    @SerialName("poster_path") val posterPath: String? = null
)

@Serializable
data class SeriesSeasonsResponse(
    val tconst: String = "",
    @SerialName("tmdb_id") val tmdbId: Long? = null,
    val seasons: List<SeasonSummary> = emptyList()
)

@Serializable
data class TmdbResolveResponse(
    val tconst: String = "",
    @SerialName("title_ru") val titleRu: String? = null,
    @SerialName("title_orig") val titleOrig: String? = null,
    @SerialName("title_primary") val titlePrimary: String? = null,
    val year: Int? = null,
    @SerialName("title_type") val titleType: String? = null,
    val rating: Double? = null,
    val genres: List<String> = emptyList(),
    @SerialName("runtime_minutes") val runtimeMinutes: Int? = null
)

@Serializable
data class PlayerInfoResponse(
    val success: Boolean = true,
    val error: String? = null,
    @SerialName("item_id") val itemId: String = "",
    @SerialName("stream_url") val streamUrl: String? = null,
    @SerialName("active_stream_url") val activeStreamUrl: String? = null,
    @SerialName("direct_stream_url") val directStreamUrl: String? = null,
    @SerialName("media_type") val mediaType: String = "Movie",
    @SerialName("video_codec") val videoCodec: String? = null,
    @SerialName("audio_tracks") val audioTracks: List<AudioTrack> = emptyList(),
    @SerialName("subtitle_tracks") val subtitleTracks: List<SubtitleTrack> = emptyList(),
    val subtitles: List<SubtitleTrack> = emptyList(),
    @SerialName("resume_position_seconds") val resumePositionSeconds: Double? = null,
    @SerialName("resume_seconds") val resumeSeconds: Double? = null,
    @SerialName("duration_seconds") val durationSeconds: Double? = null,
    @SerialName("is_played") val isPlayed: Boolean = false,
    val releases: List<TorrentRelease> = emptyList(),
    val torrents: List<TorrentRelease> = emptyList(),
    val qualityGroups: List<QualityGroup> = emptyList()
) {
    val effectiveSubtitles: List<SubtitleTrack> get() = subtitleTracks.ifEmpty { subtitles }
    val effectiveResumeSeconds: Double get() = (resumePositionSeconds?.takeIf { it > 0 } ?: resumeSeconds) ?: 0.0
    val effectiveDurationSeconds: Double get() = durationSeconds ?: 0.0
    val effectiveStreamUrl: String? get() = directStreamUrl ?: activeStreamUrl ?: streamUrl ?: releases.firstOrNull()?.streamUrl
}

@Serializable
data class MountTorrentRequest(
    val tconst: String,
    val magnet: String? = null,
    val hash: String? = null,
    val title: String? = null,
    val type: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    @SerialName("file_index") val fileIndex: Int? = null,
    @SerialName("position_seconds") val positionSeconds: Double? = null
)

fun classifyTier(res: String, title: String): String {
    val r = res.lowercase()
    val t = title.lowercase()
    if (r == "4k" || r == "2160p" || t.contains("2160p") || t.contains("4k uhd") || t.contains("uhd")) {
        return "4k"
    }
    if (r == "1080p" || t.contains("1080p") || t.contains("1080i") || t.contains("fhd")) {
        return "1080p"
    }
    if (r == "720p" || t.contains("720p") || t.contains("hd")) {
        return "720p"
    }
    return "sd"
}

fun groupTorrents(torrents: List<TorrentRelease>): List<QualityGroup> {
    val groups = linkedMapOf(
        "4k" to mutableListOf<TorrentRelease>(),
        "1080p" to mutableListOf<TorrentRelease>(),
        "720p" to mutableListOf<TorrentRelease>(),
        "sd" to mutableListOf<TorrentRelease>()
    )
    for (t in torrents) {
        val tier = classifyTier(t.resolution, t.title)
        groups[tier]?.add(t)
    }
    val result = mutableListOf<QualityGroup>()
    val tierMeta = mapOf(
        "4k" to Pair("4K Ultra HD", "4K UHD"),
        "1080p" to Pair("1080p Full HD", "1080p"),
        "720p" to Pair("720p HD", "720p"),
        "sd" to Pair("SD Качество", "SD")
    )
    for ((tier, list) in groups) {
        if (list.isNotEmpty()) {
            list.sortWith(
                compareByDescending<TorrentRelease> { it.seeds }
                    .thenByDescending { it.bitrateMbps }
                    .thenByDescending { it.size }
            )
            val meta = tierMeta[tier] ?: Pair(tier, tier)
            result.add(QualityGroup(tier = tier, title = meta.first, badge = meta.second, releases = list))
        }
    }
    return result
}

@Serializable
data class WatchProgressRequest(
    @SerialName("imdb_id") val tconst: String,
    val title: String? = null,
    @SerialName("media_type") val mediaType: String? = null,
    @SerialName("season_number") val season: Int = 0,
    @SerialName("episode_number") val episode: Int = 0,
    @SerialName("position_seconds") val positionSeconds: Double = 0.0,
    @SerialName("duration_seconds") val durationSeconds: Double = 0.0,
    @SerialName("playback_percent") val playbackPercent: Double = 0.0,
    @SerialName("is_completed") val isCompleted: Boolean = false
)

@Serializable
data class AudioPreferenceRequest(
    @SerialName("imdb_id") val imdbId: String,
    @SerialName("audio_title") val audioTitle: String,
    @SerialName("audio_index") val audioIndex: Int
)

@Serializable
data class WatchlistCheckResponse(
    @SerialName("in_watchlist") val inWatchlist: Boolean = false,
    @SerialName("is_bookmarked") val isBookmarked: Boolean = false
) {
    val isInWatchlist: Boolean get() = inWatchlist || isBookmarked
}

@Serializable
data class WatchlistAddRequest(
    @SerialName("imdb_id") val imdbId: String,
    @SerialName("media_type") val mediaType: String,
    val title: String,
    @SerialName("original_title") val originalTitle: String? = null,
    val year: Int? = null,
    val rating: Double? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null
)

@Serializable
data class ResumeItem(
    val id: Long = 0,
    @SerialName("imdb_id") val imdbId: String = "",
    val tconst: String = "",
    val title: String = "",
    @SerialName("ru_title") val ruTitle: String? = null,
    val year: Int? = null,
    @SerialName("season_number") val season: Int = 0,
    @SerialName("episode_number") val episode: Int = 0,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("poster_url") val posterUrl: String? = null,
    @SerialName("backdrop_url") val backdropUrl: String? = null,
    @SerialName("position_seconds") val positionSeconds: Double = 0.0,
    @SerialName("duration_seconds") val durationSeconds: Double = 0.0,
    @SerialName("playback_percent") val playbackPercent: Double = 0.0,
    @SerialName("is_next_up") val isNextUp: Boolean = false
) {
    val effectiveTconst: String get() = if (imdbId.isNotBlank()) imdbId else tconst
    val displayTitle: String get() = ruTitle?.takeIf { it.isNotBlank() } ?: title

    val effectivePoster: String? get() = resolveImageUrl(posterUrl ?: posterPath, "https://image.tmdb.org/t/p/w500")
    val effectiveBackdrop: String? get() = resolveImageUrl(backdropUrl ?: backdropPath, "https://image.tmdb.org/t/p/w1280")

    val timecodeFormatted: String get() {
        val curM = (positionSeconds / 60).toInt()
        val curS = (positionSeconds % 60).toInt()
        val totH = (durationSeconds / 3600).toInt()
        val totM = ((durationSeconds % 3600) / 60).toInt()
        return if (totH > 0) {
            String.format("%02d:%02d / %02d:%02d:00", curM, curS, totH, totM)
        } else {
            String.format("%02d:%02d / %02d:00", curM, curS, totM)
        }
    }
}

@Serializable
data class Shelf(
    val id: String = "",
    val title: String = "",
    val icon: String? = null,
    val page: Int? = 1,
    @SerialName("total_pages") val totalPages: Int? = 1,
    @SerialName("total_results") val totalResults: Int? = null,
    val items: List<MediaItem> = emptyList()
)

@Serializable
data class FeedResponse(
    val shelves: List<Shelf> = emptyList()
)

@Serializable
data class HomeItem(
    val id: String = "",
    val tconst: String = "",
    @SerialName("tmdb_id") val tmdbId: Long? = null,
    @SerialName("media_type") val mediaType: String = "movie",
    val title: String = "",
    @SerialName("original_title") val originalTitle: String? = null,
    val year: Int? = null,
    val rating: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    val overview: String? = null,

    // Playback progress fields
    val season: Int = 0,
    val episode: Int = 0,
    @SerialName("episode_title") val episodeTitle: String? = null,
    @SerialName("episode_still") val episodeStill: String? = null,
    @SerialName("position_seconds") val positionSeconds: Long = 0L,
    @SerialName("duration_seconds") val durationSeconds: Long = 0L,
    @SerialName("playback_percent") val playbackPercent: Double = 0.0,
    val timecode: String? = null,
    @SerialName("is_next_up") val isNextUp: Boolean = false,

    // Swarm fields
    val seeds: Int = 0,
    @SerialName("quality_badge") val qualityBadge: String? = null
) {
    fun toMediaItem(): MediaItem {
        return MediaItem(
            id = tmdbId ?: tconst.removePrefix("tmdb_").toLongOrNull(),
            tconst = tconst.ifEmpty { id },
            title = title,
            ruTitle = title,
            originalTitle = originalTitle,
            year = year,
            rating = rating,
            posterPath = posterPath,
            backdropPath = backdropPath,
            overview = overview,
            mediaType = mediaType,
            type = if (mediaType.equals("tv", ignoreCase = true)) "tvSeries" else "Movie",
            voteCount = voteCount,
            seeds = if (seeds > 0) seeds else null
        )
    }

    fun toResumeItem(): ResumeItem {
        return ResumeItem(
            imdbId = tconst.ifEmpty { id },
            tconst = tconst.ifEmpty { id },
            title = title,
            ruTitle = title,
            year = year,
            season = season,
            episode = episode,
            posterPath = posterPath,
            backdropPath = backdropPath,
            positionSeconds = positionSeconds.toDouble(),
            durationSeconds = durationSeconds.toDouble(),
            playbackPercent = playbackPercent,
            isNextUp = isNextUp
        )
    }
}

@Serializable
data class HomeShelf(
    val id: String = "",
    val title: String = "",
    val type: String = "poster",
    val badge: String? = null,
    @SerialName("action_route") val actionRoute: String? = null,
    val items: List<HomeItem> = emptyList()
)

@Serializable
data class HomePayload(
    val hero: List<HomeItem> = emptyList(),
    val shelves: List<HomeShelf> = emptyList()
)

@Serializable
data class HotlistResponse(
    val id: String = "",
    val title: String = "",
    @SerialName("media_type") val mediaType: String? = null,
    val page: Int? = 1,
    @SerialName("total_pages") val totalPages: Int? = 1,
    @SerialName("total_results") val totalResults: Int? = null,
    val items: List<MediaItem> = emptyList()
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
    @SerialName("remember_me") val rememberMe: Boolean = true
)

@Serializable
data class LoginResponse(
    val success: Boolean = false,
    val token: String? = null,
    val username: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null
)

@Serializable
data class PairingStatusResponse(
    val paired: Boolean = false,
    val token: String? = null,
    val username: String? = null
)

@Serializable
data class SearchPosterUrls(
    val thumbnail: String? = null,
    val small: String? = null,
    val medium: String? = null,
    val large: String? = null,
    val xl: String? = null
)

@Serializable
data class SearchMovieDoc(
    val tconst: String = "",
    @SerialName("title_ru") val titleRu: String? = null,
    @SerialName("title_orig") val titleOrig: String = "",
    @SerialName("title_primary") val titlePrimary: String = "",
    @SerialName("russian_titles") val russianTitles: List<String> = emptyList(),
    val year: Int? = null,
    @SerialName("title_type") val titleType: String = "movie",
    val rating: Double? = null,
    @SerialName("num_votes") val numVotes: Int? = null,
    val genres: List<String> = emptyList(),
    @SerialName("runtime_minutes") val runtimeMinutes: Int? = null
)

@Serializable
data class SearchHit(
    val movie: SearchMovieDoc,
    val score: Double = 0.0,
    @SerialName("bm25_score") val bm25Score: Double = 0.0,
    @SerialName("popularity_multiplier") val popularityMultiplier: Double = 0.0,
    @SerialName("poster_url") val posterUrl: String? = null,
    val posters: SearchPosterUrls? = null
) {
    fun toMediaItem(serverUrl: String): MediaItem {
        val rawPoster = posters?.large ?: posters?.medium ?: posterUrl ?: posters?.xl ?: posters?.small
        val finalPoster = if (!rawPoster.isNullOrBlank()) {
            if (rawPoster.startsWith("http")) rawPoster else "${serverUrl.trimEnd('/')}${if (rawPoster.startsWith("/")) "" else "/"}$rawPoster"
        } else null

        val display = movie.titleRu?.takeIf { it.isNotBlank() }
            ?: movie.russianTitles.firstOrNull { it.isNotBlank() }
            ?: movie.titlePrimary.takeIf { it.isNotBlank() }
            ?: movie.titleOrig

        val isTv = movie.titleType.contains("tv", ignoreCase = true) || movie.titleType.contains("series", ignoreCase = true)

        return MediaItem(
            tconst = movie.tconst,
            title = movie.titlePrimary.ifEmpty { movie.titleOrig },
            ruTitle = display,
            originalTitle = movie.titleOrig,
            year = movie.year,
            rating = movie.rating,
            posterUrl = finalPoster,
            posterPath = finalPoster,
            genres = movie.genres,
            mediaType = if (isTv) "tv" else "movie",
            type = movie.titleType,
            voteCount = movie.numVotes,
            runtimeMinutes = movie.runtimeMinutes
        )
    }
}

fun formatRuntime(minutes: Int?): String? {
    val m = minutes?.takeIf { it > 0 } ?: return null
    val h = m / 60
    val remM = m % 60
    return if (h > 0) {
        if (remM > 0) "$h ч $remM мин" else "$h ч"
    } else {
        "$remM мин"
    }
}

@Serializable
data class SearchResponse(
    val query: String = "",
    @SerialName("total_hits") val totalHits: Int = 0,
    @SerialName("took_ms") val tookMs: Double = 0.0,
    val hits: List<SearchHit> = emptyList()
)
