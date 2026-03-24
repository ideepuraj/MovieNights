package com.movienight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.movienight.ui.MobileNavigation
import com.movienight.ui.theme.MovieNightTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MovieNightTheme {
                MobileNavigation()
            }
        }
    }
}
