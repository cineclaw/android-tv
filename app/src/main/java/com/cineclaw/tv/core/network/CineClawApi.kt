package com.cineclaw.tv.core.network

import com.cineclaw.tv.core.model.*
import retrofit2.http.*

interface CineClawApi {
    @POST("/api/auth/login")
    suspend fun login(@Body req: LoginRequest): LoginResponse

    @GET("/api/home")
    suspend fun getHomeFeed(
        @Query("platform") platform: String = "tv",
        @Query("refresh") refresh: Boolean = false
    ): HomePayload

    @GET("/api/feeds")
    suspend fun getFeeds(): List<Shelf>

    @GET("/api/feeds/{id}")
    suspend fun getShelf(
        @Path("id") id: String,
        @Query("page") page: Int = 1,
        @Query("type") type: String? = null
    ): Shelf

    @GET("/api/catalog/discover")
    suspend fun discoverCatalog(
        @QueryMap params: Map<String, String>
    ): Shelf

    @GET("/torrents/hotlist")
    suspend fun getHotlist(
        @Query("type") type: String,
        @Query("quality") quality: String? = null,
        @Query("page") page: Int = 1
    ): HotlistResponse

    @GET("/api/ai/critics/{tconst}")
    suspend fun getCriticSummary(@Path("tconst") tconst: String): CriticSummaryResponse

    @GET("/api/person/{person_id}")
    suspend fun getPersonDetails(@Path("person_id") personId: Long): PersonDetailsResponse

    @GET("/api/playback/resume")
    suspend fun getContinueWatching(): List<ResumeItem>

    @DELETE("/api/playback/progress")
    suspend fun deleteResumeItem(
        @Query("tconst") tconst: String,
        @Query("all") all: Boolean = true
    )

    @GET("/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 30
    ): SearchResponse

    @GET("/torrents")
    suspend fun getTorrents(
        @Query("imdb_id") imdbId: String? = null,
        @Query("q") query: String? = null,
        @Query("type") type: String? = null
    ): List<TorrentRelease>

    @GET("/api/movie/{tconst}/metadata")
    suspend fun getMovieMetadata(@Path("tconst") tconst: String): MovieMetadataResponse

    @GET("/api/series/{tconst}/seasons")
    suspend fun getSeriesSeasons(@Path("tconst") tconst: String): SeriesSeasonsResponse

    @GET("/api/tmdb/{media_type}/{tmdb_id}/movie")
    suspend fun resolveTmdbMovie(
        @Path("media_type") mediaType: String,
        @Path("tmdb_id") tmdbId: Long
    ): TmdbResolveResponse

    @GET("/api/series/{tconst}/episodes")
    suspend fun getSeriesEpisodes(
        @Path("tconst") tconst: String,
        @Query("season") season: Int = 1
    ): List<EpisodeInfo>

    @GET("/api/stream/player/info")
    suspend fun getPlayerInfo(
        @Query("tconst") tconst: String,
        @Query("season") season: Int? = null,
        @Query("episode") episode: Int? = null
    ): PlayerInfoResponse

    @GET("/api/stream/stats")
    suspend fun getStreamStats(
        @Query("hash") hash: String? = null,
        @Query("tconst") tconst: String? = null,
        @Query("file_idx") fileIdx: Int? = null,
        @Query("season") season: Int? = null,
        @Query("episode") episode: Int? = null,
        @Query("duration") duration: Double? = null
    ): StreamStats

    @POST("/api/stream/mount")
    suspend fun mountTorrent(@Body req: MountTorrentRequest): PlayerInfoResponse

    @POST("/api/playback/progress")
    suspend fun reportProgress(@Body req: WatchProgressRequest)

    @POST("/api/playback/audio")
    suspend fun setAudioPreference(@Body req: AudioPreferenceRequest)

    @GET("/api/playback/series-progress")
    suspend fun getSeriesProgress(@Query("imdb_id") imdbId: String): SeriesProgressResponse

    @POST("/api/playback/mark-watched")
    suspend fun markWatched(@Body req: MarkWatchedRequest)

    @GET("/api/watchlist")
    suspend fun getWatchlist(): List<MediaItem>

    @GET("/api/watchlist/check")
    suspend fun checkWatchlist(@Query("imdb_id") tconst: String): WatchlistCheckResponse

    @POST("/api/watchlist")
    suspend fun addToWatchlist(@Body item: WatchlistAddRequest)

    @DELETE("/api/watchlist")
    suspend fun removeFromWatchlist(@Query("imdb_id") tconst: String)

    @GET("/api/auth/pair/status")
    suspend fun checkPairingStatus(@Query("code") code: String): PairingStatusResponse
}
