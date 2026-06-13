package com.wallora.app.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.wallora.app.R
import com.wallora.app.domain.model.Category
import com.wallora.app.domain.model.Wallpaper
import com.wallora.app.ui.components.EmptyState
import com.wallora.app.ui.components.ErrorState
import com.wallora.app.ui.components.OfflineState
import com.wallora.app.ui.components.WallpaperGrid
import com.wallora.app.ui.components.adaptive.gridColumns

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onWallpaperClick: (Wallpaper) -> Unit,
    onSearchClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val selectedCategories by viewModel.selectedCategories.collectAsStateWithLifecycle()
    val filterSheetVisible by viewModel.filterSheetVisible.collectAsStateWithLifecycle()
    val enabledSources by viewModel.enabledSources.collectAsStateWithLifecycle()
    val wallpapers = viewModel.wallpapers.collectAsLazyPagingItems()
    val columns = gridColumns()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Wallora") },
            actions = {
                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Default.Search, stringResource(R.string.nav_home))
                }
                IconButton(onClick = viewModel::showFilterSheet) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter sources")
                }
            },
        )

        // Category chip row — horizontal scroll, multi-select
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(Category.entries) { category ->
                FilterChip(
                    selected = category in selectedCategories,
                    onClick = { viewModel.toggleCategory(category) },
                    label = { Text(category.displayName) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }

        // Main grid with pull-to-refresh
        val isRefreshing = wallpapers.loadState.refresh is LoadState.Loading

        when {
            wallpapers.loadState.refresh is LoadState.Error -> {
                val error = (wallpapers.loadState.refresh as LoadState.Error).error
                if (error is java.io.IOException) {
                    OfflineState(
                        onRetry = wallpapers::refresh,
                        modifier = Modifier.padding(contentPadding),
                    )
                } else {
                    ErrorState(
                        onRetry = wallpapers::refresh,
                        modifier = Modifier.padding(contentPadding),
                    )
                }
            }
            !isRefreshing && wallpapers.itemCount == 0 -> {
                EmptyState(modifier = Modifier.padding(contentPadding))
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = wallpapers::refresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    WallpaperGrid(
                        items = wallpapers,
                        columns = columns,
                        contentPadding = contentPadding,
                        onWallpaperClick = onWallpaperClick,
                    )
                }
            }
        }
    }

    // Source filter bottom sheet
    if (filterSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = viewModel::hideFilterSheet,
            sheetState = sheetState,
        ) {
            SourceFilterSheet(
                enabledSources = enabledSources,
                onToggleSource = viewModel::toggleSource,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}
