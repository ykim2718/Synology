// app/src/main/java/com/yrocket/nasmusic/ui/NasMusicApp.kt
// Author: yRocket
package com.yrocket.nasmusic.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.yrocket.nasmusic.data.subsonic.Song

/**
 * Whole UI: server setup, then the artist to album to song drill-down.
 *
 * @param player Null until the MediaController has connected; the player UI stays hidden until then.
 * @param onPlay Receives the queue built from one album and the index to start at.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NasMusicApp(viewModel: LibraryViewModel, player: Player?, onPlay: (List<MediaItem>, Int) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playerState = rememberPlayerState(player)
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }

    // Falls back to the library on its own once the queue is cleared.
    if (showNowPlaying && player != null && playerState != null) {
        BackHandler { showNowPlaying = false }
        NowPlayingScreen(playerState, player, onCollapse = { showNowPlaying = false })
        return
    }

    BackHandler(enabled = state.screen is Screen.Albums || state.screen is Screen.Songs) {
        viewModel.back()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleOf(state.screen)) },
                navigationIcon = {
                    if (state.screen is Screen.Albums || state.screen is Screen.Songs) {
                        IconButton(onClick = viewModel::back) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (state.screen !is Screen.Setup) {
                        TextButton(onClick = viewModel::signOut) { Text("Sign out") }
                    }
                },
            )
        },
        bottomBar = {
            if (player != null && playerState != null) {
                MiniPlayer(playerState, player, onExpand = { showNowPlaying = true })
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val screen = state.screen) {
                is Screen.Setup -> SetupScreen(state, viewModel::connect)
                is Screen.Artists -> LazyColumn {
                    items(state.artists, key = { it.id }) { artist ->
                        ListItem(
                            headlineContent = { Text(artist.name) },
                            supportingContent = { Text("${artist.albumCount} albums") },
                            modifier = Modifier.clickable { viewModel.openArtist(artist) },
                        )
                    }
                }
                is Screen.Albums -> LazyColumn {
                    items(state.albums, key = { it.id }) { album ->
                        ListItem(
                            headlineContent = { Text(album.name) },
                            supportingContent = { Text(album.year?.toString() ?: "") },
                            modifier = Modifier.clickable { viewModel.openAlbum(album) },
                        )
                    }
                }
                is Screen.Songs -> LazyColumn {
                    items(state.songs, key = { it.id }) { song ->
                        ListItem(
                            headlineContent = { Text(song.title) },
                            supportingContent = { Text(formatDuration(song.duration)) },
                            modifier = Modifier.clickable {
                                val repository = viewModel.repository ?: return@clickable
                                val queue = state.songs.map { mediaItemOf(it, repository.streamUrl(it.id)) }
                                onPlay(queue, state.songs.indexOf(song))
                                showNowPlaying = true
                            },
                        )
                    }
                    if (state.songs.isEmpty() && !state.loading) {
                        item { Text("No songs in ${screen.album.name}.", Modifier.padding(16.dp)) }
                    }
                }
            }

            if (state.loading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }

            state.error?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun SetupScreen(state: LibraryUiState, onConnect: (String, String, String) -> Unit) {
    var baseUrl by rememberSaveable { mutableStateOf("https://") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Subsonic server", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("Server URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onConnect(baseUrl, username, password) },
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Connect")
        }
    }
}

private fun titleOf(screen: Screen): String = when (screen) {
    is Screen.Setup -> "NasMusic"
    is Screen.Artists -> "Artists"
    is Screen.Albums -> screen.artist.name
    is Screen.Songs -> screen.album.name
}

private fun mediaItemOf(song: Song, streamUrl: String): MediaItem =
    MediaItem.Builder()
        .setMediaId(song.id)
        .setUri(streamUrl)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)
                .setTrackNumber(song.track)
                .build(),
        )
        .build()

private fun formatDuration(seconds: Int?): String {
    if (seconds == null) return ""
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
