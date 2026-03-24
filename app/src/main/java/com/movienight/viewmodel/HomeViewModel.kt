package com.movienight.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.movienight.data.Movie
import com.movienight.data.MovieCategory
import com.movienight.data.MovieRulzScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class CategoryState(
    val movies: List<Movie> = emptyList(),
    val page: Int = 0,
    val isLoading: Boolean = false,
    val isDone: Boolean = false,
    val error: String? = null,
)

sealed class UrlTestState {
    object Idle : UrlTestState()
    object Testing : UrlTestState()
    data class Success(val movieCount: Int) : UrlTestState()
    data class Failure(val reason: String) : UrlTestState()
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("movienight", Context.MODE_PRIVATE)

    private val _baseUrl = MutableStateFlow(
        prefs.getString("base_url", "https://www.5movierulz.florist")
            ?: "https://www.5movierulz.florist"
    )
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    private var scraper = MovieRulzScraper(application, _baseUrl.value)
    private val httpClient = OkHttpClient()

    private val _categoryStates = MutableStateFlow(
        MovieCategory.entries.associateWith { CategoryState() }
    )
    val categoryStates: StateFlow<Map<MovieCategory, CategoryState>> =
        _categoryStates.asStateFlow()

    private val _urlTestState = MutableStateFlow<UrlTestState>(UrlTestState.Idle)
    val urlTestState: StateFlow<UrlTestState> = _urlTestState.asStateFlow()

    init {
        fetchRemoteBaseUrl()
        MovieCategory.entries.forEach { loadMore(it) }
    }

    /** Silently fetches the remote config and auto-updates baseUrl if it has changed. */
    private fun fetchRemoteBaseUrl() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("http://getmyapps.in/movienight/baseurl")
                    .build()
                val remoteUrl = httpClient.newCall(request).execute()
                    .use { it.body?.string()?.trim() }
                    ?: return@launch
                if (remoteUrl.isNotBlank() && remoteUrl != _baseUrl.value) {
                    withContext(Dispatchers.Main) { updateBaseUrl(remoteUrl) }
                }
            } catch (_: Exception) {
                // Server unreachable — continue with stored baseUrl
            }
        }
    }

    fun loadMore(category: MovieCategory) {
        val state = _categoryStates.value[category] ?: return
        if (state.isLoading || state.isDone) return
        viewModelScope.launch {
            val nextPage = state.page + 1
            updateState(category) { it.copy(isLoading = true, error = null) }
            try {
                val movies = scraper.getMovies(category, nextPage)
                if (movies.isEmpty()) {
                    updateState(category) { it.copy(isLoading = false, isDone = true) }
                } else {
                    updateState(category) { it.copy(
                        movies = it.movies + movies,
                        page = nextPage,
                        isLoading = false,
                    )}
                }
            } catch (e: Exception) {
                updateState(category) { it.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load",
                )}
            }
        }
    }

    fun retryCategory(category: MovieCategory) {
        updateState(category) { it.copy(error = null) }
        loadMore(category)
    }

    /** Tests [url] without saving it. Updates [urlTestState] with the result. */
    fun testUrl(url: String) {
        _urlTestState.value = UrlTestState.Idle
        if (url.isBlank()) return
        viewModelScope.launch {
            _urlTestState.value = UrlTestState.Testing
            try {
                val tempScraper = MovieRulzScraper(getApplication(), url.trim())
                val count = tempScraper.testUrl(url.trim())
                _urlTestState.value = UrlTestState.Success(count)
            } catch (e: Exception) {
                _urlTestState.value = UrlTestState.Failure(e.message ?: "Unknown error")
            }
        }
    }

    fun resetUrlTest() {
        _urlTestState.value = UrlTestState.Idle
    }

    fun updateBaseUrl(url: String) {
        val trimmed = url.trim()
        prefs.edit().putString("base_url", trimmed).apply()
        _baseUrl.value = trimmed
        scraper = MovieRulzScraper(getApplication(), trimmed)
        _categoryStates.value = MovieCategory.entries.associateWith { CategoryState() }
        _urlTestState.value = UrlTestState.Idle
        MovieCategory.entries.forEach { loadMore(it) }
    }

    private fun updateState(
        category: MovieCategory,
        update: (CategoryState) -> CategoryState,
    ) {
        _categoryStates.value = _categoryStates.value.toMutableMap().also { map ->
            map[category] = update(map[category] ?: CategoryState())
        }
    }
}
