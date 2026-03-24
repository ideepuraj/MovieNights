package com.movienight.ui

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.movienight.viewmodel.PlayerUiState
import com.movienight.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

private data class SeekOverlayState(
    val position: Long,
    val duration: Long,
    val forward: Boolean,
)

/**
 * Two-mode D-pad handling:
 *  - Controller HIDDEN → arrow keys seek/volume silently; OK toggles play/pause + shows controller
 *  - Controller VISIBLE → all keys pass through to default PlayerView (scrubber, buttons, etc.)
 */
private class InterceptPlayerView(context: Context) : PlayerView(context) {
    var audioManager: AudioManager? = null
    var onSeek: ((SeekOverlayState) -> Unit)? = null

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (isControllerFullyVisible) return super.dispatchKeyEvent(event)

        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    val pos = maxOf(0, player!!.currentPosition - 30_000)
                    player?.seekTo(pos)
                    onSeek?.invoke(SeekOverlayState(pos, player!!.duration, false))
                }
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    val pos = player!!.currentPosition + 30_000
                    player?.seekTo(pos)
                    onSeek?.invoke(SeekOverlayState(pos, player!!.duration, true))
                }
                true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (event.action == KeyEvent.ACTION_DOWN)
                    audioManager?.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI
                    )
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (event.action == KeyEvent.ACTION_DOWN)
                    audioManager?.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI
                    )
                true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    player?.let { if (it.isPlaying) it.pause() else it.play() }
                    showController()
                }
                true
            }
            else -> super.dispatchKeyEvent(event)
        }
    }
}

@Composable
fun PlayerScreen(
    movieUrl: String,
    baseUrl: String,
    onClose: () -> Unit,
    playerViewModel: PlayerViewModel = viewModel(),
) {
    val uiState by playerViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var backPressedOnce by remember { mutableStateOf(false) }
    var seekOverlay by remember { mutableStateOf<SeekOverlayState?>(null) }

    BackHandler {
        if (backPressedOnce) {
            playerViewModel.reset()
            onClose()
        } else {
            backPressedOnce = true
        }
    }

    LaunchedEffect(backPressedOnce) {
        if (backPressedOnce) {
            delay(2_000)
            backPressedOnce = false
        }
    }

    // Auto-hide seek overlay after 1.5s of no seek activity
    LaunchedEffect(seekOverlay) {
        if (seekOverlay != null) {
            delay(1_500)
            seekOverlay = null
        }
    }

    LaunchedEffect(movieUrl) {
        playerViewModel.extractAndPlay(movieUrl, baseUrl)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (val state = uiState) {
            is PlayerUiState.Idle, is PlayerUiState.Extracting -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFE50914),
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "LOADING STREAM...",
                        color = Color(0xFF888888),
                        fontSize = 12.sp,
                        letterSpacing = 2.sp,
                    )
                }
            }

            is PlayerUiState.Ready -> {
                val player = remember(state.streamInfo.rawUrl) {
                    ExoPlayer.Builder(context)
                        .setMediaSourceFactory(
                            DefaultMediaSourceFactory(state.streamInfo.dataSourceFactory)
                        )
                        .setSeekBackIncrementMs(30_000)
                        .setSeekForwardIncrementMs(30_000)
                        .build()
                        .apply {
                            setMediaItem(
                                MediaItem.Builder()
                                    .setUri(state.streamInfo.rawUrl)
                                    .setMimeType(MimeTypes.APPLICATION_M3U8)
                                    .build()
                            )
                            prepare()
                            playWhenReady = true
                        }
                }

                DisposableEffect(player) {
                    onDispose { player.release() }
                }

                AndroidView(
                    factory = { ctx ->
                        InterceptPlayerView(ctx).apply {
                            this.player = player
                            this.audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                            this.onSeek = { seekOverlay = it }
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            keepScreenOn = true
                            controllerAutoShow = false
                            hideController()
                            isFocusable = true
                            isFocusableInTouchMode = true
                            requestFocus()
                            setControllerVisibilityListener(
                                PlayerView.ControllerVisibilityListener { visibility ->
                                    if (visibility == android.view.View.VISIBLE) {
                                        findViewById<android.view.View>(
                                            androidx.media3.ui.R.id.exo_play_pause
                                        )?.requestFocus() ?: requestFocus()
                                    } else {
                                        requestFocus()
                                    }
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                // Seek overlay — only progress bar + time, no full controls
                seekOverlay?.let { seek ->
                    SeekOverlay(seek)
                }
            }

            is PlayerUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Stream unavailable", color = Color.White, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(state.message, color = Color(0xFF888888), fontSize = 12.sp)
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { playerViewModel.extractAndPlay(movieUrl, baseUrl) }) {
                        Text("Retry", color = Color(0xFFE50914))
                    }
                }
            }
        }

        if (backPressedOnce) {
            Text(
                text = "Press back again to exit",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .background(Color(0xAA000000), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun SeekOverlay(seek: SeekOverlayState) {
    val progress = if (seek.duration > 0) seek.position.toFloat() / seek.duration else 0f
    val positionText = formatMs(seek.position)
    val durationText = if (seek.duration > 0) formatMs(seek.duration) else "--:--"
    val arrow = if (seek.forward) "▶  +30s" else "◀  -30s"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 36.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Seek direction badge
            Text(
                text = arrow,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Color(0xAA000000), RoundedCornerShape(6.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )

            Spacer(Modifier.height(10.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = Color(0xFFE50914),
                trackColor = Color(0x66FFFFFF),
            )

            Spacer(Modifier.height(6.dp))

            // Position / duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(positionText, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Text(durationText, color = Color(0xFFAAAAAA), fontSize = 13.sp)
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}
