package com.wallora.app.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.wallora.app.R
import com.wallora.app.ui.components.WallpaperGrid
import com.wallora.app.ui.components.adaptive.gridColumns

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    contentPadding: PaddingValues,
    onWallpaperClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val isActive by viewModel.isSearchActive.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val searchResults = viewModel.searchResults.collectAsLazyPagingItems()
    val columns = gridColumns()

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = viewModel::onQueryChange,
                    onSearch = { viewModel.onSearch(it) },
                    expanded = isActive,
                    onExpandedChange = viewModel::setSearchActive,
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(Icons.Default.Clear, null)
                            }
                        }
                    },
                )
            },
            expanded = isActive,
            onExpandedChange = viewModel::setSearchActive,
            modifier = Modifier.fillMaxWidth().padding(horizontal = if (!isActive) 16.dp else 0.dp),
        ) {
            // Recent searches list shown when search is active but no query yet
            if (recentSearches.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.search_recent),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = viewModel::clearRecentSearches) {
                        Text(stringResource(R.string.search_clear_history))
                    }
                }
                LazyColumn {
                    items(recentSearches) { recentQuery ->
                        ListItem(
                            headlineContent = { Text(recentQuery) },
                            leadingContent = { Icon(Icons.Default.History, null) },
                            trailingContent = {
                                IconButton(onClick = { viewModel.removeRecentSearch(recentQuery) }) {
                                    Icon(Icons.Default.Clear, null)
                                }
                            },
                            modifier = Modifier.clickable {
                                viewModel.onSearch(recentQuery)
                                viewModel.setSearchActive(false)
                            },
                        )
                    }
                }
            }
        }

        // Results
        if (query.length >= 2) {
            WallpaperGrid(
                items = searchResults,
                columns = columns,
                contentPadding = contentPadding,
                onWallpaperClick = { wallpaper -> onWallpaperClick(wallpaper.globalKey) },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.search_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
