package com.wallora.app.ui.favorites

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallora.app.ui.components.EmptyState
import com.wallora.app.ui.components.WallpaperThumbCard
import com.wallora.app.ui.components.adaptive.gridColumns

@Composable
fun FavoritesScreen(
    contentPadding: PaddingValues,
    onWallpaperClick: (String) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle(emptyList())
    val columns = gridColumns()

    if (favorites.isEmpty()) {
        EmptyState()
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(favorites, key = { it.globalKey }) { wallpaper ->
                WallpaperThumbCard(
                    wallpaper = wallpaper,
                    onClick = { onWallpaperClick(wallpaper.globalKey) },
                )
            }
        }
    }
}
