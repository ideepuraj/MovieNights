package com.movienight.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MovieNightColors = darkColorScheme(
    primary = Color(0xFFE50914),
    surface = Color(0xFF1A1A1A),
    background = Color(0xFF141414),
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun MovieNightTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MovieNightColors,
        content = content
    )
}
