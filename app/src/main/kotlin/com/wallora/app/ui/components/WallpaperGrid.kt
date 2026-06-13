package com.wallora.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wallora.app.domain.model.Wallpaper

/**
 * Staggered/masonry wallpaper grid driven by [LazyPagingItems].
 *
 * Grids always load thumbs — Coil handles placeholders and errors.
 */
@Composable
fun WallpaperGrid(
    items: LazyPagingItems<Wallpaper>,
    columns: Int,
    contentPadding: PaddingValues,
    onWallpaperClick: (Wallpaper) -> Unit,
    modifier: Modifier = Modifier,
    itemSpacing: Dp = 4.dp,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        verticalItemSpacing = itemSpacing,
    ) {
        items(
            count = items.itemCount,
            key = { index -> items.peek(index)?.globalKey ?: index.toString() },
        ) { index ->
            val wallpaper = items[index] ?: return@items
            WallpaperThumbCard(
                wallpaper = wallpaper,
                onClick = { onWallpaperClick(wallpaper) },
            )
        }

        // Loading footer
        if (items.loadState.append is LoadState.Loading) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun WallpaperThumbCard(
    wallpaper: Wallpaper,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val aspect = if (wallpaper.height > 0 && wallpaper.width > 0) {
        wallpaper.width.toFloat() / wallpaper.height
    } else 0.5625f // default portrait 9:16

    val context = LocalContext.current
    val imageRequest = ImageRequest.Builder(context)
        .data(wallpaper.thumbUrl)
        .diskCacheKey(wallpaper.globalKey + "_thumb")
        .crossfade(true)
        .build()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = wallpaper.author,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    }
}
