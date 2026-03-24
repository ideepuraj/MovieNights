package com.movienight.ui

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.movienight.viewmodel.PlayerUiState
import com.movienight.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay

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

    val activity = context as android.app.Activity
    DisposableEffect(Unit) {
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

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
                        .setSeekBackIncrementMs(10_000)
                        .setSeekForwardIncrementMs(10_000)
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
                        PlayerView(ctx).apply {
                            this.player = player
                            keepScreenOn = true
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
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
                    .background(Color(0xAA000000), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}
