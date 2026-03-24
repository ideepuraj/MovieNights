package com.movienight.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    LaunchedEffect(malayalamMovies.size) {
        while (true) {
            delay(8_000)
            if (malayalamMovies.size > 1) bannerIndex = (bannerIndex + 1) % malayalamMovies.size
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141414))
    ) {
        // Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clickable { bannerMovie?.let(onMovieClick) }
            ) {
                Crossfade(targetState = bannerMovie?.thumbnail ?: "", label = "banner") { url ->
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
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(0f to Color.Transparent, 1f to Color(0xFF141414))
                    )
                )
                bannerMovie?.let { movie ->
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                    ) {
                        Text(
                            text = movie.title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 280.dp),
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE50914))
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                        ) {
                            Text("▶  Play", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        MovieCategory.entries.forEach { category ->
            val state = categoryStates[category] ?: CategoryState()
            item(key = category.name) {
                MobileCategoryRow(
                    title = category.displayName,
                    state = state,
                    onLoadMore = { viewModel.loadMore(category) },
                    onMovieClick = onMovieClick,
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun MobileCategoryRow(
    title: String,
    state: CategoryState,
    onLoadMore: () -> Unit,
    onMovieClick: (Movie) -> Unit,
) {
    val listState = rememberLazyListState()
    val isNearEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && last >= total - 3
        }
    }
    LaunchedEffect(isNearEnd) { if (isNearEnd) onLoadMore() }

    Column {
        Text(
            text = title,
            color = Color(0xFFE5E5E5),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 8.dp),
        )
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.movies.isEmpty() && state.isLoading) {
                items(List(6) { it }) { MobileSkeletonCard() }
            } else {
                itemsIndexed(state.movies) { _, movie ->
                    MobilePosterCard(movie = movie, onClick = { onMovieClick(movie) })
                }
                if (state.isLoading) item { MobileSkeletonCard() }
            }
        }
    }
}

@Composable
internal fun MobilePosterCard(movie: Movie, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.size(width = 100.dp, height = 155.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(8.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = movie.thumbnail.ifEmpty { null },
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xDD000000))))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            ) {
                Text(
                    text = movie.title,
                    color = Color(0xFFCCCCCC),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MobileSkeletonCard() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.12f, targetValue = 0.32f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "shimmer_alpha",
    )
    Box(
        Modifier.size(width = 100.dp, height = 155.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = alpha))
    )
}
