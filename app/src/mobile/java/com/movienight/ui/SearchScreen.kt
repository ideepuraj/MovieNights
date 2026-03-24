package com.movienight.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movienight.data.Movie
import com.movienight.data.MovieCategory
import com.movienight.viewmodel.HomeViewModel

@Composable
fun SearchScreen(viewModel: HomeViewModel, onMovieClick: (Movie) -> Unit) {
    val categoryStates by viewModel.categoryStates.collectAsState()
    var query by remember { mutableStateOf("") }

    val allMovies = remember(categoryStates) {
        MovieCategory.entries.flatMap { cat ->
            categoryStates[cat]?.movies ?: emptyList()
        }.distinctBy { it.url }
    }

    val filtered = remember(query, allMovies) {
        if (query.isBlank()) emptyList()
        else allMovies.filter { it.title.contains(query, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141414))
            .padding(16.dp)
    ) {
        Text("Search", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search movies...", color = Color(0xFF555555)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF2A2A2A),
                unfocusedContainerColor = Color(0xFF2A2A2A),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = Color(0xFFE50914),
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Color(0xFFE50914),
            ),
            shape = RoundedCornerShape(10.dp),
        )
        Spacer(Modifier.height(16.dp))
        if (filtered.isEmpty() && query.isNotBlank()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No results for \"$query\"", color = Color(0xFF555555))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered) { movie ->
                    MobilePosterCard(movie = movie, onClick = { onMovieClick(movie) })
                }
            }
        }
    }
}
