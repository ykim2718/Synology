// app/src/main/java/com/yrocket/nasmusic/ui/NowPlaying.kt
// Author: yRocket
package com.yrocket.nasmusic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import kotlinx.coroutines.delay

/** What the now-playing UI shows, copied out of [Player] so Compose can diff it. */
data class PlayerUiState(
    val title: String,
    val artist: String,
    val album: String,
    val isPlaying: Boolean,
    val positionMs: Long,
    /** Zero while the duration is still unknown; the seek bar is disabled until it is known. */
    val durationMs: Long,
    val shuffle: Boolean,
    val repeatMode: Int,
    val hasNext: Boolean,
)

/** Interval at which the playback position is re-read while audio is playing. */
private const val POSITION_POLL_MS = 500L

/**
 * Observes [player] and returns its state, or null while there is no player or nothing is queued.
 *
 * Player.Listener reports discrete changes only, so the position is polled while playing.
 */
@Composable
fun rememberPlayerState(player: Player?): PlayerUiState? {
    var snapshot by remember(player) { mutableStateOf(player?.let(::snapshotOf)) }

    DisposableEffect(player) {
        if (player == null) return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                snapshot = snapshotOf(player)
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(player, snapshot?.isPlaying) {
        if (player == null || snapshot?.isPlaying != true) return@LaunchedEffect
        while (true) {
            delay(POSITION_POLL_MS)
            snapshot = snapshotOf(player)
        }
    }

    return snapshot
}

private fun snapshotOf(player: Player): PlayerUiState? {
    val item = player.currentMediaItem ?: return null
    val metadata = item.mediaMetadata
    val duration = player.duration
    return PlayerUiState(
        title = metadata.title?.toString() ?: item.mediaId,
        artist = metadata.artist?.toString() ?: "",
        album = metadata.albumTitle?.toString() ?: "",
        isPlaying = player.isPlaying,
        positionMs = player.currentPosition.coerceAtLeast(0L),
        durationMs = if (duration == C.TIME_UNSET) 0L else duration,
        shuffle = player.shuffleModeEnabled,
        repeatMode = player.repeatMode,
        hasNext = player.hasNextMediaItem(),
    )
}

/** Bar under the library lists; tapping it opens [NowPlayingScreen]. */
@Composable
fun MiniPlayer(state: PlayerUiState, player: Player, onExpand: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        // The activity is edge-to-edge, so the bar keeps itself clear of the navigation bar.
        Column(Modifier.navigationBarsPadding()) {
            LinearProgressIndicator(
                progress = { fractionOf(state.positionMs, state.durationMs) },
                modifier = Modifier.fillMaxWidth(),
            )
            ListItem(
                headlineContent = { Text(state.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = { Text(state.artist, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                trailingContent = { PlayPauseButton(state, player) },
                modifier = Modifier.clickable(onClick = onExpand),
            )
        }
    }
}

/** Full-screen player: track info, seek bar, transport, shuffle and repeat. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(state: PlayerUiState, player: Player, onCollapse: () -> Unit) {
    // Non-null only while the thumb is held, so polling does not yank it back mid-drag.
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val seekable = state.durationMs > 0L
    val fraction = dragFraction ?: fractionOf(state.positionMs, state.durationMs)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now playing") },
                navigationIcon = {
                    IconButton(onClick = onCollapse) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Close player")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(state.title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(state.artist, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(state.album, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)

            Spacer(Modifier.size(24.dp))

            Slider(
                value = fraction,
                onValueChange = { dragFraction = it },
                onValueChangeFinished = {
                    dragFraction?.let { player.seekTo((it * state.durationMs).toLong()) }
                    dragFraction = null
                },
                enabled = seekable,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatMs((fraction * state.durationMs).toLong()), style = MaterialTheme.typography.labelMedium)
                Text(if (seekable) formatMs(state.durationMs) else "--:--", style = MaterialTheme.typography.labelMedium)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { player.shuffleModeEnabled = !state.shuffle }) {
                    Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle", tint = activeTint(state.shuffle))
                }
                // seekToPrevious restarts the current track first, so it is useful even on the first item.
                IconButton(onClick = player::seekToPrevious) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous")
                }
                PlayPauseButton(state, player, large = true)
                IconButton(onClick = player::seekToNext, enabled = state.hasNext) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next")
                }
                IconButton(onClick = { player.repeatMode = nextRepeatMode(state.repeatMode) }) {
                    Icon(
                        imageVector = if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = "Repeat",
                        tint = activeTint(state.repeatMode != Player.REPEAT_MODE_OFF),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayPauseButton(state: PlayerUiState, player: Player, large: Boolean = false) {
    val icon = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow
    val label = if (state.isPlaying) "Pause" else "Play"
    val onClick = { if (state.isPlaying) player.pause() else player.play() }
    if (large) {
        FilledIconButton(onClick = onClick, modifier = Modifier.size(64.dp)) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(36.dp))
        }
    } else {
        IconButton(onClick = onClick) { Icon(icon, contentDescription = label) }
    }
}

@Composable
private fun activeTint(active: Boolean): Color =
    if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

/** Cycles off, all, one, then off again. */
private fun nextRepeatMode(mode: Int): Int = when (mode) {
    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
    else -> Player.REPEAT_MODE_OFF
}

private fun fractionOf(positionMs: Long, durationMs: Long): Float =
    if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)

private fun formatMs(ms: Long): String {
    val seconds = ms / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
