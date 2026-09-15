// app/src/main/java/com/yrocket/nasmusic/data/subsonic/SubsonicAuth.kt
// Author: yRocket
package com.yrocket.nasmusic.data.subsonic

import com.yrocket.nasmusic.data.ServerConfig
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Salted-token authentication of the Subsonic API (protocol 1.13.0 and later).
 *
 * The same parameters are needed by URLs handed to ExoPlayer and Coil, which never pass through
 * Retrofit, so building them is exposed as [appendTo] rather than living inside the interceptor.
 */
object SubsonicAuth {

    private const val PROTOCOL_VERSION = "1.16.1"
    private const val CLIENT_NAME = "NasMusic"
    private const val SALT_BYTES = 8

    private val random = SecureRandom()

    fun appendTo(builder: HttpUrl.Builder, config: ServerConfig): HttpUrl.Builder {
        val salt = newSalt()
        return builder
            .setQueryParameter("u", config.username)
            .setQueryParameter("t", md5(config.password + salt))
            .setQueryParameter("s", salt)
            .setQueryParameter("v", PROTOCOL_VERSION)
            .setQueryParameter("c", CLIENT_NAME)
            .setQueryParameter("f", "json")
    }

    private fun newSalt(): String {
        val bytes = ByteArray(SALT_BYTES)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun md5(text: String): String =
        MessageDigest.getInstance("MD5")
            .digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}

/** Adds the authentication parameters to every Retrofit request. */
class SubsonicAuthInterceptor(private val config: ServerConfig) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = SubsonicAuth.appendTo(request.url.newBuilder(), config).build()
        return chain.proceed(request.newBuilder().url(url).build())
    }
}
