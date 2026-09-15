// app/src/main/java/com/yrocket/nasmusic/data/subsonic/SubsonicApi.kt
// Author: yRocket
package com.yrocket.nasmusic.data.subsonic

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Subsonic REST endpoints used by this app.
 *
 * Authentication and the fixed parameters (v, c, f) are added by [SubsonicAuth] in an OkHttp
 * interceptor, so they are absent here.
 */
interface SubsonicApi {

    @GET("rest/ping.view")
    suspend fun ping(): SubsonicEnvelope

    @GET("rest/getArtists.view")
    suspend fun getArtists(): SubsonicEnvelope

    @GET("rest/getArtist.view")
    suspend fun getArtist(@Query("id") id: String): SubsonicEnvelope

    @GET("rest/getAlbum.view")
    suspend fun getAlbum(@Query("id") id: String): SubsonicEnvelope
}
