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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movienight.viewmodel.HomeViewModel

@Composable
fun SettingsScreen(viewModel: HomeViewModel) {
    val serverUrl by viewModel.serverUrl.collectAsState()
    var urlInput by remember(serverUrl) { mutableStateOf(serverUrl) }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141414))
            .padding(horizontal = 48.dp, vertical = 32.dp)
    ) {
        Text(
            text = "Settings",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(40.dp))

        // ── Backend URL ───────────────────────────────────────────────────────
        Text(
            text = "Backend API URL",
            color = Color(0xFFAAAAAA),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        BasicTextField(
            value = urlInput,
            onValueChange = {
                urlInput = it
                saved = false
            },
            singleLine = true,
            cursorBrush = SolidColor(Color(0xFFE50914)),
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2A2A2A))
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    if (urlInput.isEmpty()) {
                        Text(
                            text = "http://192.168.1.x:8000/",
                            color = Color(0xFF555555),
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Include the trailing slash and port number.",
            color = Color(0xFF555555),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Save button ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (saved) Color(0xFF2A2A2A) else Color(0xFFE50914))
                .clickable {
                    if (urlInput.isNotBlank()) {
                        viewModel.updateServerUrl(urlInput)
                        saved = true
                    }
                }
                .padding(horizontal = 28.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (saved) "Saved — reconnecting…" else "Save & Reconnect",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // ── Current value display ─────────────────────────────────────────────
        Text(
            text = "Current: $serverUrl",
            color = Color(0xFF555555),
            fontSize = 12.sp
        )
    }
}
