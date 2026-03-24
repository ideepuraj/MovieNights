package com.movienight.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import coil.compose.AsyncImage
import com.movienight.data.Movie
import com.movienight.data.MovieCategory
import com.movienight.viewmodel.CategoryState
import com.movienight.viewmodel.HomeViewModel
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(viewModel: HomeViewModel, onMovieClick: (Movie) -> Unit) {
    val categoryStates by viewModel.categoryStates.collectAsState()

    val malayalamMovies by remember(categoryStates) {
        derivedStateOf {
            categoryStates[MovieCategory.MALAYALAM]
                ?.movies?.filter { it.thumbnail.isNotEmpty() }
                ?: emptyList()
        }
    }
    var bannerIndex by remember { mutableIntStateOf(0) }
    val bannerMovie = malayalamMovies.getOrNull(bannerIndex)

    val firstCardFocusRequester = remember { FocusRequester() }
    var firstFocusRequested by remember { mutableStateOf(false) }
    LaunchedEffect(malayalamMovies.size) {
        if (!firstFocusRequested && malayalamMovies.isNotEmpty()) {
            firstFocusRequested = true
            delay(100)
            try { firstCardFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    // Auto-cycle banner every 8 seconds
    LaunchedEffect(malayalamMovies.size) {
        while (true) {
            delay(8_000)
            if (malayalamMovies.size > 1) {
                bannerIndex = (bannerIndex + 1) % malayalamMovies.size
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141414))
            .verticalScroll(rememberScrollState())
    ) {
        HeroBanner(
            movie = bannerMovie,
            onPlay = { bannerMovie?.let(onMovieClick) },
        )

        MovieCategory.entries.forEachIndexed { index, category ->
            val state = categoryStates[category] ?: CategoryState()
            CategoryRow(
                title = category.displayName,
                state = state,
                onLoadMore = { viewModel.loadMore(category) },
                onMovieClick = onMovieClick,
                firstItemFocusRequester = if (index == 0) firstCardFocusRequester else null,
                onMovieFocus = { movie ->
                    if (category == MovieCategory.MALAYALAM) {
                        val idx = malayalamMovies.indexOf(movie)
                        if (idx >= 0) bannerIndex = idx
                    }
                },
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun HeroBanner(movie: Movie?, onPlay: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
    ) {
        Crossfade(targetState = movie?.thumbnail ?: "", label = "banner_bg") { url ->
            if (url.isNotEmpty()) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize().background(Color(0xFF1A1A2E)))
            }
        }

        // Bottom gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.5f to Color(0x88141414),
                        1f to Color(0xFF141414),
                    )
                )
        )

        // Left gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color(0xCC000000),
                        0.6f to Color.Transparent,
                    )
                )
        )

        if (movie != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = 28.dp)
            ) {
                Text(
                    text = movie.title,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 420.dp),
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE50914))
                        .clickable(onClick = onPlay)
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = "\u25BA  Play",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    title: String,
    state: CategoryState,
    onLoadMore: () -> Unit,
    onMovieClick: (Movie) -> Unit,
    onMovieFocus: (Movie) -> Unit,
    firstItemFocusRequester: FocusRequester? = null,
) {
    val listState = rememberLazyListState()

    val isNearEnd by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                ?: return@derivedStateOf false
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 3
        }
    }

    LaunchedEffect(isNearEnd) {
        if (isNearEnd) onLoadMore()
    }

    Column {
        Text(
            text = title,
            color = Color(0xFFE5E5E5),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 10.dp),
        )

        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.movies.isEmpty() && state.isLoading) {
                items(List(8) { it }) { SkeletonPosterCard() }
            } else {
                itemsIndexed(state.movies) { index, movie ->
                    PosterCard(
                        movie = movie,
                        onClick = { onMovieClick(movie) },
                        onFocus = { onMovieFocus(movie) },
                        focusRequester = if (index == 0) firstItemFocusRequester else null,
                    )
                }
                if (state.isLoading) {
                    item { SkeletonPosterCard() }
                }
            }
        }
    }
}

@Composable
private fun PosterCard(
    movie: Movie,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    val focusModifier = if (focusRequester != null)
        Modifier.focusRequester(focusRequester) else Modifier
    Card(
        onClick = onClick,
        modifier = Modifier
            .size(width = 120.dp, height = 190.dp)
            .then(focusModifier)
            .onFocusChanged { if (it.isFocused) onFocus() },
        border = CardDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, Color(0xFFE50914))),
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = movie.thumbnail.ifEmpty { null },
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (movie.thumbnail.isEmpty()) {
                Text(
                    text = "\uD83C\uDFAC",
                    modifier = Modifier.align(Alignment.Center),
                    fontSize = 28.sp,
                )
            }
            // Title overlay at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xDD000000))
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            ) {
                Text(
                    text = movie.title,
                    color = Color(0xFFCCCCCC),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SkeletonPosterCard() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmer_alpha",
    )
    Box(
        Modifier
            .size(width = 120.dp, height = 190.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = alpha))
    )
}

