package com.movienight.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movienight.data.Movie
import com.movienight.viewmodel.HomeViewModel

@Composable
fun SearchScreen(viewModel: HomeViewModel, onMovieClick: (Movie) -> Unit) {
    val categoryStates by viewModel.categoryStates.collectAsState()
    var query by remember { mutableStateOf("") }

    val allMovies by remember(categoryStates) {
        derivedStateOf {
            categoryStates.values
                .flatMap { it.movies }
                .distinctBy { it.url }
        }
    }

    val results = if (query.isBlank()) allMovies
                  else allMovies.filter { it.title.contains(query, ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141414))
    ) {
        Text(
            text = "Search",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 32.dp, top = 28.dp, bottom = 20.dp),
        )

        BasicTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            cursorBrush = SolidColor(Color(0xFFE50914)),
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2A2A2A))
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    if (query.isEmpty()) {
                        Text("Search movies...", color = Color(0xFF666666), fontSize = 16.sp)
                    }
                    innerTextField()
                }
            },
        )

        Text(
            text = if (query.isBlank()) "${results.size} movies loaded"
                   else "${results.size} result${if (results.size != 1) "s" else ""} for \"$query\"",
            color = Color(0xFF888888),
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 32.dp, top = 16.dp, bottom = 8.dp),
        )

        if (results.isEmpty() && query.isNotBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No movies found", color = Color(0xFF555555), fontSize = 18.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 170.dp),
                contentPadding = PaddingValues(start = 32.dp, end = 32.dp, bottom = 32.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(results) { movie ->
                    MovieCard(movie = movie, onClick = { onMovieClick(movie) })
                }
            }
        }
    }
}
