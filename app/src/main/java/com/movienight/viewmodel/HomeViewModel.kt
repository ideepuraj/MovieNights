package com.movienight.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.movienight.data.Movie
import com.movienight.data.MovieCategory
import com.movienight.data.MovieRulzScraper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CategoryState(
    val movies: List<Movie> = emptyList(),
    val page: Int = 0,
    val isLoading: Boolean = false,
    val isDone: Boolean = false,
    val error: String? = null,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("movienight", Context.MODE_PRIVATE)

    private val _baseUrl = MutableStateFlow(
        prefs.getString("base_url", "https://www.5movierulz.florist")
            ?: "https://www.5movierulz.florist"
    )
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    private var scraper = MovieRulzScraper(application, _baseUrl.value)

    private val _categoryStates = MutableStateFlow(
        MovieCategory.entries.associateWith { CategoryState() }
    )
    val categoryStates: StateFlow<Map<MovieCategory, CategoryState>> =
        _categoryStates.asStateFlow()

    init {
        MovieCategory.entries.forEach { loadMore(it) }
    }

    fun loadMore(category: MovieCategory) {
        val state = _categoryStates.value[category] ?: return
        if (state.isLoading || state.isDone) return
        viewModelScope.launch {
            val nextPage = state.page + 1
            updateState(category) { it.copy(isLoading = true, error = null) }
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
        }
    }

    fun updateBaseUrl(url: String) {
        val trimmed = url.trim()
        prefs.edit().putString("base_url", trimmed).apply()
        _baseUrl.value = trimmed
        scraper = MovieRulzScraper(getApplication(), trimmed)
        _categoryStates.value = MovieCategory.entries.associateWith { CategoryState() }
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
