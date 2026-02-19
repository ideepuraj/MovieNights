package com.movienight.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.movienight.data.Movie
import com.movienight.data.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val movies: List<Movie>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("movienight", Context.MODE_PRIVATE)

    private val _serverUrl = MutableStateFlow(
        prefs.getString("server_url", "http://192.168.1.69:8000/") ?: "http://192.168.1.69:8000/"
    )
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadMovies()
    }

    fun loadMovies() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val movies = MovieRepository(_serverUrl.value).getMovies()
                _uiState.value = HomeUiState.Success(movies)
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun updateServerUrl(url: String) {
        val normalized = url.trimEnd().let { if (it.endsWith("/")) it else "$it/" }
        prefs.edit().putString("server_url", normalized).apply()
        _serverUrl.value = normalized
        loadMovies()
    }
}
