package com.wallora.app.ui.history

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallora.app.ui.components.EmptyState
import com.wallora.app.ui.components.WallpaperThumbCard
import com.wallora.app.ui.components.adaptive.gridColumns

@Composable
fun HistoryScreen(
    contentPadding: PaddingValues,
    onWallpaperClick: (String) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val history by viewModel.history.collectAsStateWithLifecycle(emptyList())
    val columns = gridColumns()

    if (history.isEmpty()) {
        EmptyState()
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(history, key = { it.globalKey }) { wallpaper ->
                WallpaperThumbCard(
                    wallpaper = wallpaper,
                    onClick = { onWallpaperClick(wallpaper.globalKey) },
                )
            }
        }
    }
}
