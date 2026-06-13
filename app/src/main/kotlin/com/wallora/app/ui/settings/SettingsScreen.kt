package com.wallora.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Dataset
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SettingsRemote
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallora.app.R
import com.wallora.app.ui.settings.components.SettingsRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    onNavigate: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val theme by viewModel.theme.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var searchExpanded by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is SettingsEvent.ShowMessage) snackbarHostState.showSnackbar(event.message)
        }
    }

    fun openTarget(target: SettingsTarget) {
        when (target) {
            is SettingsTarget.Page -> {
                searchQuery = ""
                searchExpanded = false
                onNavigate(target.route)
            }
            is SettingsTarget.Popup -> {
                searchQuery = ""
                searchExpanded = false
                when (target.id) {
                    SettingsPopupId.THEME -> showThemeDialog = true
                    SettingsPopupId.CLEAR_CACHE -> showClearCacheConfirm = true
                }
            }
        }
    }

    val searchResults by remember(searchQuery) {
        derivedStateOf {
            val q = searchQuery.trim().lowercase()
            if (q.isEmpty()) emptyList()
            else SETTINGS_SEARCH_INDEX.filter { entry ->
                context.getString(entry.titleRes).lowercase().contains(q) ||
                    entry.keywords.any { it.contains(q) }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(scaffoldPadding),
        ) {
            // ── Search bar ────────────────────────────────────────────────
            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it; if (it.isNotEmpty()) searchExpanded = true },
                        onSearch = {},
                        expanded = searchExpanded,
                        onExpandedChange = { searchExpanded = it },
                        placeholder = { Text(stringResource(R.string.settings_search_hint)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = ""; searchExpanded = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                    )
                },
                expanded = searchExpanded,
                onExpandedChange = { searchExpanded = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                if (searchResults.isEmpty() && searchQuery.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.settings_search_no_results, searchQuery),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    LazyColumn {
                        items(searchResults, key = { it.titleRes.toString() + it.target }) { entry ->
                            ListItem(
                                leadingContent = { Icon(entry.icon, contentDescription = null) },
                                headlineContent = { Text(stringResource(entry.titleRes)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { openTarget(entry.target) },
                            )
                        }
                    }
                }
            }

            // ── Master grouped list (visible when search is closed) ───────
            if (!searchExpanded) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {

                    // ── CONTENT ──────────────────────────────────────────
                    item { GroupHeader(stringResource(R.string.settings_group_content)) }
                    item {
                        SettingsNavRow(
                            icon = Icons.Outlined.Dataset,
                            title = stringResource(R.string.settings_sources_title),
                            subtitle = stringResource(R.string.settings_sources_subtitle),
                            onClick = { onNavigate(WalloraSettingsRoute.SOURCES) },
                        )
                    }
                    item {
                        SettingsNavRow(
                            icon = Icons.Outlined.Label,
                            title = stringResource(R.string.settings_categories_title),
                            subtitle = stringResource(R.string.settings_categories_subtitle),
                            onClick = { onNavigate(WalloraSettingsRoute.CATEGORIES) },
                        )
                    }
                    item { HorizontalDivider() }

                    // ── WALLPAPER ─────────────────────────────────────────
                    item { GroupHeader(stringResource(R.string.settings_group_wallpaper)) }
                    item {
                        SettingsNavRow(
                            icon = Icons.Outlined.Loop,
                            title = stringResource(R.string.settings_rotation_title),
                            subtitle = stringResource(R.string.settings_rotation_subtitle),
                            onClick = { onNavigate(WalloraSettingsRoute.ROTATION) },
                        )
                    }
                    item {
                        SettingsNavRow(
                            icon = Icons.Outlined.Wallpaper,
                            title = stringResource(R.string.settings_live_title),
                            subtitle = stringResource(R.string.settings_live_subtitle),
                            onClick = { onNavigate(WalloraSettingsRoute.LIVE) },
                        )
                    }
                    item {
                        SettingsNavRow(
                            icon = Icons.Outlined.SettingsRemote,
                            title = stringResource(R.string.settings_remote_title),
                            subtitle = stringResource(R.string.settings_remote_subtitle),
                            onClick = { onNavigate(WalloraSettingsRoute.REMOTE) },
                        )
                    }
                    item { HorizontalDivider() }

                    // ── APP ───────────────────────────────────────────────
                    item { GroupHeader(stringResource(R.string.settings_group_app)) }
                    item {
                        val themeLabel = when (theme) {
                            "LIGHT" -> stringResource(R.string.settings_theme_light)
                            "DARK" -> stringResource(R.string.settings_theme_dark)
                            else -> stringResource(R.string.settings_theme_system)
                        }
                        SettingsRow(
                            icon = Icons.Outlined.Palette,
                            title = stringResource(R.string.settings_appearance_title),
                            subtitle = themeLabel,
                            showChevron = false,
                            onClick = { showThemeDialog = true },
                        )
                    }
                    item {
                        SettingsRow(
                            icon = Icons.Outlined.DeleteSweep,
                            title = stringResource(R.string.settings_clear_cache),
                            subtitle = stringResource(R.string.settings_clear_cache_subtitle),
                            showChevron = false,
                            onClick = { showClearCacheConfirm = true },
                        )
                    }
                    item {
                        SettingsNavRow(
                            icon = Icons.Outlined.Info,
                            title = stringResource(R.string.settings_about),
                            subtitle = stringResource(R.string.settings_about_subtitle),
                            onClick = { onNavigate(WalloraSettingsRoute.ABOUT) },
                        )
                    }
                }
            }
        }
    }

    // ── Theme dialog ──────────────────────────────────────────────────────
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.settings_theme_title)) },
            text = {
                Column {
                    listOf(
                        "SYSTEM" to R.string.settings_theme_system,
                        "LIGHT" to R.string.settings_theme_light,
                        "DARK" to R.string.settings_theme_dark,
                    ).forEach { (key, labelRes) ->
                        ListItem(
                            headlineContent = { Text(stringResource(labelRes)) },
                            trailingContent = {
                                if (key == theme) {
                                    Text("✓", color = MaterialTheme.colorScheme.primary)
                                }
                            },
                            modifier = Modifier.clickable {
                                viewModel.setTheme(key)
                                showThemeDialog = false
                            },
                        )
                    }
                }
            },
            confirmButton = {},
        )
    }

    // ── Clear cache confirm ───────────────────────────────────────────────
    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            title = { Text(stringResource(R.string.settings_clear_cache_confirm_title)) },
            text = { Text(stringResource(R.string.settings_clear_cache_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearCache()
                    showClearCacheConfirm = false
                }) { Text(stringResource(R.string.settings_clear_cache)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun GroupHeader(label: String) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    SettingsRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        showChevron = true,
        onClick = onClick,
    )
}
