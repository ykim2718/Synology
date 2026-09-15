// app/src/main/java/com/yrocket/nasmusic/data/ServerConfig.kt
// Author: yRocket
package com.yrocket.nasmusic.data

/**
 * Connection settings for one Subsonic-compatible server.
 *
 * @param baseUrl Server root, e.g. "https://music.example.com/". A trailing slash is required by Retrofit.
 * @param username Subsonic account name.
 * @param password Subsonic password, used to derive the per-request salted token.
 */
data class ServerConfig(
    val baseUrl: String,
    val username: String,
    val password: String,
) {
    init {
        require(baseUrl.isNotBlank()) { "baseUrl must not be blank." }
        require(baseUrl.endsWith("/")) { "baseUrl must end with '/': $baseUrl" }
        require(baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) {
            "baseUrl must start with http:// or https://: $baseUrl"
        }
        require(username.isNotBlank()) { "username must not be blank." }
        require(password.isNotEmpty()) { "password must not be empty." }
    }

    companion object {
        /** Accepts user input that omits the trailing slash, which is the common typing mistake. */
        fun of(baseUrl: String, username: String, password: String): ServerConfig {
            val normalized = baseUrl.trim().let { if (it.endsWith("/")) it else "$it/" }
            return ServerConfig(normalized, username.trim(), password)
        }
    }
}
