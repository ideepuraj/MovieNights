package com.movienight.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.movienight.viewmodel.HomeUiState
import com.movienight.viewmodel.HomeViewModel

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141414))
    ) {
        // Header — always visible immediately on launch
        Text(
            text = "Movie Nights",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 32.dp, top = 28.dp, bottom = 8.dp)
        )

        // Thin red progress bar under the title — only visible while fetching
        if (uiState is HomeUiState.Loading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 32.dp, bottom = 16.dp)
                    .height(2.dp),
                color = Color(0xFFE50914),
                trackColor = Color(0xFF2A2A2A)
            )
        } else {
            Spacer(modifier = Modifier.height(24.dp))
        }

        when (val state = uiState) {
            is HomeUiState.Loading -> {
                // Home screen is already visible above; just show a centred spinner
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFE50914),
                        strokeWidth = 3.dp
                    )
                }
            }

            is HomeUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Could not connect to server",
                            color = Color(0xFFAAAAAA),
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { viewModel.loadMovies() }) {
                            Text(text = "Retry", color = Color(0xFFE50914), fontSize = 18.sp)
                        }
                    }
                }
            }

            is HomeUiState.Success -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 170.dp),
                    contentPadding = PaddingValues(start = 32.dp, end = 32.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.movies) { movie ->
                        MovieCard(
                            movie = movie,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(movie.streamUrl), "video/*")
                                    putExtra("title", movie.title)
                                }
                                runCatching { context.startActivity(intent) }
                            }
                        )
                    }
                }
            }
        }
    }
}
