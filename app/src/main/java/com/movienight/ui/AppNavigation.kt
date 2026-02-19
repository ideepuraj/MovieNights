package com.movienight.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.movienight.R
import com.movienight.viewmodel.HomeViewModel

enum class Screen { Home, Search, Settings }

@Composable
fun AppNavigation() {
    val viewModel: HomeViewModel = viewModel()
    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var isSidebarExpanded by remember { mutableStateOf(false) }

    val sidebarWidth by animateDpAsState(
        targetValue = if (isSidebarExpanded) 210.dp else 68.dp,
        animationSpec = tween(durationMillis = 200),
        label = "sidebar_width"
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141414))
    ) {
        // ── Sidebar ──────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .width(sidebarWidth)
                .fillMaxHeight()
                .background(Color(0xFF1A1A1A))
                .padding(vertical = 32.dp)
                .onFocusChanged { isSidebarExpanded = it.hasFocus },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Logo
            AsyncImage(
                model = R.mipmap.ic_launcher,
                contentDescription = "Movie Nights",
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            NavItem(
                icon = Icons.Filled.Home,
                label = "Home",
                selected = currentScreen == Screen.Home,
                expanded = isSidebarExpanded,
                onClick = { currentScreen = Screen.Home }
            )

            Spacer(modifier = Modifier.height(8.dp))

            NavItem(
                icon = Icons.Filled.Search,
                label = "Search",
                selected = currentScreen == Screen.Search,
                expanded = isSidebarExpanded,
                onClick = { currentScreen = Screen.Search }
            )

            Spacer(modifier = Modifier.weight(1f))

            NavItem(
                icon = Icons.Filled.Settings,
                label = "Settings",
                selected = currentScreen == Screen.Settings,
                expanded = isSidebarExpanded,
                onClick = { currentScreen = Screen.Settings }
            )
        }

        // ── Content ───────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            when (currentScreen) {
                Screen.Home     -> HomeScreen(viewModel = viewModel)
                Screen.Search   -> SearchScreen(viewModel = viewModel)
                Screen.Settings -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val bgColor = when {
        selected  -> Color(0xFFE50914).copy(alpha = 0.18f)
        isFocused -> Color(0xFF2A2A2A)
        else      -> Color.Transparent
    }
    val contentColor = when {
        selected || isFocused -> Color.White
        else                  -> Color(0xFF888888)
    }

    Row(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Color(0xFFE50914) else contentColor,
            modifier = Modifier.size(24.dp)
        )
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(150)) + expandHorizontally(tween(200)),
            exit  = fadeOut(tween(100)) + shrinkHorizontally(tween(150))
        ) {
            Text(
                text = label,
                color = contentColor,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                modifier = Modifier.padding(start = 14.dp)
            )
        }
    }
}
