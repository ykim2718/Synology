// app/src/main/java/com/yrocket/nasmusic/ui/LibraryViewModel.kt
// Author: yRocket
package com.yrocket.nasmusic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yrocket.nasmusic.data.ServerConfig
import com.yrocket.nasmusic.data.SettingsStore
import com.yrocket.nasmusic.data.subsonic.Album
import com.yrocket.nasmusic.data.subsonic.Artist
import com.yrocket.nasmusic.data.subsonic.Song
import com.yrocket.nasmusic.data.subsonic.SubsonicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Where the user currently is in the artist to album to song drill-down. */
sealed interface Screen {
    data object Setup : Screen
    data object Artists : Screen
    data class Albums(val artist: Artist) : Screen
    data class Songs(val artist: Artist, val album: Album) : Screen
}

data class LibraryUiState(
    val screen: Screen = Screen.Setup,
    val loading: Boolean = false,
    val error: String? = null,
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val songs: List<Song> = emptyList(),
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = SettingsStore(application)

    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    /** Null until a server has been configured; the UI reads stream URLs through it. */
    var repository: SubsonicRepository? = null
        private set

    init {
        viewModelScope.launch {
            val saved = settings.config.first()
            if (saved == null) {
                _state.value = LibraryUiState(screen = Screen.Setup)
            } else {
                repository = SubsonicRepository(saved)
                loadArtists()
            }
        }
    }

    /** Verifies the credentials against the server before storing them. */
    fun connect(baseUrl: String, username: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val config = runCatching { ServerConfig.of(baseUrl, username, password) }
                .getOrElse { failure ->
                    _state.value = _state.value.copy(loading = false, error = failure.message)
                    return@launch
                }
            val candidate = SubsonicRepository(config)
            runCatching { candidate.ping() }
                .onFailure { failure ->
                    _state.value = _state.value.copy(loading = false, error = describe(failure))
                }
                .onSuccess {
                    settings.save(config)
                    repository = candidate
                    loadArtists()
                }
        }
    }

    fun openArtist(artist: Artist) {
        val repo = repository ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(screen = Screen.Albums(artist), loading = true, error = null)
            runCatching { repo.albums(artist.id) }
                .onSuccess { _state.value = _state.value.copy(loading = false, albums = it) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = describe(it)) }
        }
    }

    fun openAlbum(album: Album) {
        val repo = repository ?: return
        val artist = (_state.value.screen as? Screen.Albums)?.artist ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(screen = Screen.Songs(artist, album), loading = true, error = null)
            runCatching { repo.songs(album.id) }
                .onSuccess { _state.value = _state.value.copy(loading = false, songs = it) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = describe(it)) }
        }
    }

    fun back() {
        val current = _state.value.screen
        _state.value = when (current) {
            is Screen.Songs -> _state.value.copy(screen = Screen.Albums(current.artist), songs = emptyList())
            is Screen.Albums -> _state.value.copy(screen = Screen.Artists, albums = emptyList())
            else -> return
        }
    }

    fun signOut() {
        viewModelScope.launch {
            settings.clear()
            repository = null
            _state.value = LibraryUiState(screen = Screen.Setup)
        }
    }

    private fun loadArtists() {
        val repo = repository ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(screen = Screen.Artists, loading = true, error = null)
            runCatching { repo.artists() }
                .onSuccess { _state.value = _state.value.copy(loading = false, artists = it) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = describe(it)) }
        }
    }

    private fun describe(failure: Throwable): String =
        failure.message ?: failure::class.simpleName ?: "Unknown failure"
}
