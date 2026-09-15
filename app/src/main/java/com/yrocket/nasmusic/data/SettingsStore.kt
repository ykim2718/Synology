// app/src/main/java/com/yrocket/nasmusic/data/SettingsStore.kt
// Author: yRocket
package com.yrocket.nasmusic.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "server_config")

/**
 * Persists the single server configuration.
 *
 * The password is stored in clear text; see the Further Work section of README.md.
 */
class SettingsStore(private val context: Context) {

    private val keyBaseUrl = stringPreferencesKey("base_url")
    private val keyUsername = stringPreferencesKey("username")
    private val keyPassword = stringPreferencesKey("password")

    /** Emits null until a complete configuration has been saved. */
    val config: Flow<ServerConfig?> = context.dataStore.data.map { prefs ->
        val baseUrl = prefs[keyBaseUrl]
        val username = prefs[keyUsername]
        val password = prefs[keyPassword]
        if (baseUrl.isNullOrBlank() || username.isNullOrBlank() || password.isNullOrEmpty()) {
            null
        } else {
            ServerConfig(baseUrl, username, password)
        }
    }

    suspend fun save(config: ServerConfig) {
        context.dataStore.edit { prefs ->
            prefs[keyBaseUrl] = config.baseUrl
            prefs[keyUsername] = config.username
            prefs[keyPassword] = config.password
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
