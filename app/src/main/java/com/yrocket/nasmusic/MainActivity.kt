// app/src/main/java/com/yrocket/nasmusic/MainActivity.kt
// Author: yRocket
package com.yrocket.nasmusic

import android.content.ComponentName
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.yrocket.nasmusic.playback.PlaybackService
import com.yrocket.nasmusic.ui.LibraryViewModel
import com.yrocket.nasmusic.ui.NasMusicApp
import com.yrocket.nasmusic.ui.theme.NasMusicTheme

/**
 * Hosts the UI and owns the [MediaController] that drives [PlaybackService].
 *
 * The controller is bound in onStart and released in onStop, the lifecycle Media3 documents, so the
 * service survives configuration changes without leaking a connection.
 */
class MainActivity : ComponentActivity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    // Compose state, so the player UI appears once the asynchronous connection completes.
    private var controller: MediaController? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NasMusicTheme {
                NasMusicApp(
                    viewModel = viewModel<LibraryViewModel>(),
                    player = controller,
                    onPlay = ::play,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                runCatching { future.get() }
                    .onSuccess { controller = it }
                    .onFailure { Log.e(TAG, "MediaController connection failed", it) }
            },
            ContextCompat.getMainExecutor(this),
        )
    }

    override fun onStop() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
        super.onStop()
    }

    private fun play(queue: List<MediaItem>, startIndex: Int) {
        val player: Player = controller ?: run {
            Log.w(TAG, "play() ignored: MediaController is not connected yet.")
            return
        }
        player.setMediaItems(queue, startIndex, 0L)
        player.prepare()
        player.play()
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}
