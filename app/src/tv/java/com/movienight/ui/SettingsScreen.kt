package com.movienight.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
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
import com.movienight.viewmodel.UrlTestState

@Composable
fun SettingsScreen(viewModel: HomeViewModel) {
    val baseUrl by viewModel.baseUrl.collectAsState()
    val urlTestState by viewModel.urlTestState.collectAsState()
    var urlInput by remember(baseUrl) { mutableStateOf(baseUrl) }
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
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "MovieRulz Domain",
            color = Color(0xFFAAAAAA),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        BasicTextField(
            value = urlInput,
            onValueChange = {
                urlInput = it
                saved = false
                viewModel.resetUrlTest()
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
                            text = "https://www.5movierulz.florist",
                            color = Color(0xFF555555),
                            fontSize = 16.sp,
                        )
                    }
                    innerTextField()
                }
            },
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Domain is auto-updated from remote config on each launch. Override manually only if auto-update fails.",
            color = Color(0xFF555555),
            fontSize = 12.sp,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Test URL button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2A2A2A))
                    .clickable(enabled = urlInput.isNotBlank() && urlTestState !is UrlTestState.Testing) {
                        viewModel.testUrl(urlInput)
                    }
                    .padding(horizontal = 28.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (urlTestState is UrlTestState.Testing) {
                    CircularProgressIndicator(
                        color = Color(0xFFE50914),
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Test URL", color = Color(0xFFAAAAAA), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.size(16.dp))

            // Save button
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
                    .padding(horizontal = 28.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (saved) "Saved — reloading..." else "Save & Reload",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Test result feedback
        Spacer(modifier = Modifier.height(16.dp))
        when (val testState = urlTestState) {
            is UrlTestState.Success -> Text(
                "✓ Valid — found ${testState.movieCount} movies",
                color = Color(0xFF4CAF50),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            is UrlTestState.Failure -> Text(
                "✗ ${testState.reason}",
                color = Color(0xFFE50914),
                fontSize = 14.sp,
            )
            else -> {}
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Current: $baseUrl",
            color = Color(0xFF555555),
            fontSize = 12.sp,
        )
    }
}
