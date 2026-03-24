package com.movienight.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.movienight.data.Movie
import com.movienight.viewmodel.HomeViewModel
import com.movienight.viewmodel.PlayerViewModel

enum class MobileScreen { Home, Search, Settings }

@Composable
fun MobileNavigation() {
    val viewModel: HomeViewModel = viewModel()
    val playerViewModel: PlayerViewModel = viewModel()
    val baseUrl by viewModel.baseUrl.collectAsState()

    var currentScreen by remember { mutableStateOf(MobileScreen.Home) }
    var nowPlayingUrl by remember { mutableStateOf<String?>(null) }

    nowPlayingUrl?.let { url ->
        PlayerScreen(
            movieUrl = url,
            baseUrl = baseUrl,
            onClose = { nowPlayingUrl = null },
            playerViewModel = playerViewModel,
        )
        return
    }

    val onMovieClick: (Movie) -> Unit = { movie -> nowPlayingUrl = movie.url }

    Scaffold(
        containerColor = Color(0xFF141414),
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF1A1A1A)) {
                NavigationBarItem(
                    selected = currentScreen == MobileScreen.Home,
                    onClick = { currentScreen = MobileScreen.Home },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFE50914),
                        selectedTextColor = Color(0xFFE50914),
                        unselectedIconColor = Color(0xFF888888),
                        unselectedTextColor = Color(0xFF888888),
                        indicatorColor = Color(0xFF2A2A2A),
                    ),
                )
                NavigationBarItem(
                    selected = currentScreen == MobileScreen.Search,
                    onClick = { currentScreen = MobileScreen.Search },
                    icon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                    label = { Text("Search") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFE50914),
                        selectedTextColor = Color(0xFFE50914),
                        unselectedIconColor = Color(0xFF888888),
                        unselectedTextColor = Color(0xFF888888),
                        indicatorColor = Color(0xFF2A2A2A),
                    ),
                )
                NavigationBarItem(
                    selected = currentScreen == MobileScreen.Settings,
                    onClick = { currentScreen = MobileScreen.Settings },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFE50914),
                        selectedTextColor = Color(0xFFE50914),
                        unselectedIconColor = Color(0xFF888888),
                        unselectedTextColor = Color(0xFF888888),
                        indicatorColor = Color(0xFF2A2A2A),
                    ),
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (currentScreen) {
                MobileScreen.Home -> HomeScreen(viewModel = viewModel, onMovieClick = onMovieClick)
                MobileScreen.Search -> SearchScreen(viewModel = viewModel, onMovieClick = onMovieClick)
                MobileScreen.Settings -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
