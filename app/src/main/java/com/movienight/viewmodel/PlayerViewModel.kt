package com.movienight.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movierulz.extractor.MovierulzConfig
import com.movierulz.extractor.MovierulzExtractor
import com.movierulz.extractor.StreamInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PlayerUiState {
    object Idle : PlayerUiState()
    object Extracting : PlayerUiState()
    data class Ready(val streamInfo: StreamInfo) : PlayerUiState()
    data class Error(val message: String) : PlayerUiState()
}

class PlayerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Idle)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var pendingUrl: String? = null

    fun extractAndPlay(movieUrl: String, baseUrl: String) {
        if (_uiState.value is PlayerUiState.Extracting && pendingUrl == movieUrl) return
        pendingUrl = movieUrl
        _uiState.value = PlayerUiState.Extracting
        viewModelScope.launch {
            MovierulzExtractor(MovierulzConfig(baseUrl = baseUrl))
                .extract(movieUrl)
                .onSuccess { _uiState.value = PlayerUiState.Ready(it) }
                .onFailure { _uiState.value = PlayerUiState.Error(it.message ?: "Extraction failed") }
        }
    }

    fun reset() {
        pendingUrl = null
        _uiState.value = PlayerUiState.Idle
    }
}
