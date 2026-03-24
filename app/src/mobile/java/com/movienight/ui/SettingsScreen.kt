package com.movienight.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movienight.viewmodel.HomeViewModel

@Composable
fun SettingsScreen(viewModel: HomeViewModel) {
    val baseUrl by viewModel.baseUrl.collectAsState()
    var urlInput by remember(baseUrl) { mutableStateOf(baseUrl) }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141414))
            .padding(24.dp)
    ) {
        Text("Settings", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))
        Text("MovieRulz Domain", color = Color(0xFFAAAAAA), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        TextField(
            value = urlInput,
            onValueChange = { urlInput = it; saved = false },
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
        Spacer(Modifier.height(8.dp))
        Text(
            "Domain is auto-updated from remote config on each launch. Override manually only if auto-update fails.",
            color = Color(0xFF555555),
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (saved) Color(0xFF2A2A2A) else Color(0xFFE50914))
                .clickable {
                    if (urlInput.isNotBlank()) {
                        viewModel.updateBaseUrl(urlInput)
                        saved = true
                    }
                }
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (saved) "Saved — reloading..." else "Save & Reload",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("Current: $baseUrl", color = Color(0xFF555555), fontSize = 12.sp)
    }
}
