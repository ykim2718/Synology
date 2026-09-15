// app/src/main/java/com/yrocket/nasmusic/data/subsonic/SubsonicRepository.kt
// Author: yRocket
package com.yrocket.nasmusic.data.subsonic

import com.yrocket.nasmusic.data.ServerConfig
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** Thrown when the server answers with status "failed". */
class SubsonicException(val code: Int, message: String) : Exception("Subsonic error $code: $message")

/**
 * Reads the library of one server.
 *
 * Every call unwraps the envelope and turns a "failed" status into [SubsonicException], so a caller
 * never sees an empty list standing in for a rejected request.
 */
class SubsonicRepository(val config: ServerConfig) {

    private val json = Json { ignoreUnknownKeys = true }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(SubsonicAuthInterceptor(config))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val api: SubsonicApi = Retrofit.Builder()
        .baseUrl(config.baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(SubsonicApi::class.java)

    /** OkHttp instance ExoPlayer reuses, so streaming shares the connection pool and timeouts. */
    val httpClient: OkHttpClient get() = client

    suspend fun ping() {
        api.ping().require()
    }

    suspend fun artists(): List<Artist> {
        val indexes = api.getArtists().require().artists
            ?: throw SubsonicException(0, "getArtists returned no 'artists' payload.")
        return indexes.index.flatMap { it.artist }
    }

    suspend fun albums(artistId: String): List<Album> {
        val detail = api.getArtist(artistId).require().artist
            ?: throw SubsonicException(0, "getArtist($artistId) returned no 'artist' payload.")
        return detail.album
    }

    suspend fun songs(albumId: String): List<Song> {
        val detail = api.getAlbum(albumId).require().album
            ?: throw SubsonicException(0, "getAlbum($albumId) returned no 'album' payload.")
        return detail.song
    }

    fun streamUrl(songId: String): String =
        authorized("rest/stream.view") { it.setQueryParameter("id", songId) }

    fun coverArtUrl(coverArtId: String, size: Int): String =
        authorized("rest/getCoverArt.view") {
            it.setQueryParameter("id", coverArtId).setQueryParameter("size", size.toString())
        }

    private fun authorized(path: String, extra: (okhttp3.HttpUrl.Builder) -> okhttp3.HttpUrl.Builder): String {
        val builder = (config.baseUrl + path).toHttpUrl().newBuilder()
        return SubsonicAuth.appendTo(extra(builder), config).build().toString()
    }

    private fun SubsonicEnvelope.require(): SubsonicResponse {
        if (response.status != "ok") {
            val error = response.error
            throw SubsonicException(error?.code ?: -1, error?.message ?: "status=${response.status}")
        }
        return response
    }
}
