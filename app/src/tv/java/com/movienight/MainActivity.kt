package com.movienight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.movienight.ui.AppNavigation
import com.movienight.ui.theme.MovieNightTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MovieNightTheme {
                AppNavigation()
            }
        }
    }
}
