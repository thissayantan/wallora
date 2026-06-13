package com.wallora.app.ui.settings

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallora.app.BuildConfig
import com.wallora.app.R
import com.wallora.app.domain.model.Category
import com.wallora.app.domain.model.SourceId
import com.wallora.app.service.WalloraWallpaperService
import java.util.concurrent.TimeUnit

private val INTERVAL_OPTIONS = listOf(
    TimeUnit.MINUTES.toMillis(15) to "15 minutes",
    TimeUnit.MINUTES.toMillis(30) to "30 minutes",
    TimeUnit.HOURS.toMillis(1) to "1 hour",
    TimeUnit.HOURS.toMillis(3) to "3 hours",
    TimeUnit.HOURS.toMillis(6) to "6 hours",
    TimeUnit.HOURS.toMillis(12) to "12 hours",
    TimeUnit.HOURS.toMillis(24) to "24 hours",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val enabledSources by viewModel.enabledSources.collectAsStateWithLifecycle()
    val selectedCategories by viewModel.selectedCategories.collectAsStateWithLifecycle()
    val rotationEnabled by viewModel.rotationEnabled.collectAsStateWithLifecycle()
    val intervalMs by viewModel.rotationIntervalMs.collectAsStateWithLifecycle()
    val playlist by viewModel.rotationPlaylist.collectAsStateWithLifecycle()
    val times by viewModel.rotationTimes.collectAsStateWithLifecycle()
    val wifiOnly by viewModel.rotationWifiOnly.collectAsStateWithLifecycle()
    val chargingOnly by viewModel.rotationChargingOnly.collectAsStateWithLifecycle()
    val rotationOnUnlock by viewModel.rotationOnUnlock.collectAsStateWithLifecycle()
    val nextChange by viewModel.nextChangeLabel.collectAsStateWithLifecycle()
    val doubleTapEnabled by viewModel.doubleTapEnabled.collectAsStateWithLifecycle()
    val parallaxEnabled by viewModel.parallaxEnabled.collectAsStateWithLifecycle()
    val defaultParams by viewModel.defaultEditParams.collectAsStateWithLifecycle()
    val theme by viewModel.theme.collectAsStateWithLifecycle()

    var showIntervalPicker by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var showAddTimePicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var addTimeInput by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(scaffoldPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Sources ───────────────────────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_sources))
            SourceId.entries.forEach { source ->
                val isConfigured = viewModel.sourceConfiguredMap[source] ?: false
                val isEnabled = source in enabledSources
                ListItem(
                    headlineContent = { Text(source.displayName) },
                    supportingContent = {
                        if (!isConfigured) Text(stringResource(R.string.source_key_missing))
                    },
                    trailingContent = {
                        Switch(
                            checked = isEnabled && isConfigured,
                            onCheckedChange = { viewModel.setSourceEnabled(source, it) },
                            enabled = isConfigured,
                        )
                    },
                )
            }

            HorizontalDivider()

            // ── Categories ────────────────────────────────────────────────────
            SettingsSectionHeader("Default categories")
            Text(
                text = "Leave empty to show all",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Category.entries.forEach { cat ->
                    val selected = cat in selectedCategories
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.toggleCategory(cat) },
                        label = { Text(cat.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            HorizontalDivider()

            // ── Auto-rotation ─────────────────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_rotation))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_rotation)) },
                supportingContent = {
                    if (nextChange.isNotEmpty()) Text("Next change: $nextChange")
                },
                trailingContent = {
                    Switch(checked = rotationEnabled, onCheckedChange = viewModel::setRotationEnabled)
                },
            )
            if (rotationEnabled) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_rotation_interval)) },
                    supportingContent = {
                        Text(INTERVAL_OPTIONS.firstOrNull { it.first == intervalMs }?.second ?: "Custom")
                    },
                    modifier = Modifier.clickable { showIntervalPicker = true },
                )
                ListItem(
                    headlineContent = { Text("Playlist") },
                    supportingContent = {
                        Text(if (playlist == "FAVORITES") "Favorites" else "Current categories")
                    },
                    modifier = Modifier.clickable { showPlaylistPicker = true },
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_rotation_times)) },
                    supportingContent = {
                        if (times.isEmpty()) Text("No specific times") else Text(times.sorted().joinToString(", "))
                    },
                    trailingContent = {
                        TextButton(onClick = { showAddTimePicker = true }) { Text("Add") }
                    },
                )
                times.sorted().forEach { t ->
                    ListItem(
                        headlineContent = { Text(t) },
                        trailingContent = {
                            TextButton(onClick = { viewModel.removeRotationTime(t) }) { Text("Remove") }
                        },
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_rotation_on_unlock)) },
                    supportingContent = {
                        Text(stringResource(R.string.settings_rotation_on_unlock_static_hint))
                    },
                    trailingContent = {
                        Switch(checked = rotationOnUnlock, onCheckedChange = viewModel::setRotationOnUnlock)
                    },
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_wifi_only)) },
                    trailingContent = { Switch(checked = wifiOnly, onCheckedChange = viewModel::setWifiOnly) },
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_charging_only)) },
                    trailingContent = { Switch(checked = chargingOnly, onCheckedChange = viewModel::setChargingOnly) },
                )
            }

            HorizontalDivider()

            // ── Gesture & parallax ────────────────────────────────────────────
            SettingsSectionHeader("Live Wallpaper & Gestures")
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_gesture)) },
                supportingContent = { Text(stringResource(R.string.settings_gesture_hint)) },
                trailingContent = {
                    Switch(checked = doubleTapEnabled, onCheckedChange = viewModel::setDoubleTapEnabled)
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_parallax)) },
                supportingContent = {
                    Text("${stringResource(R.string.settings_parallax_hint)} — Nova Launcher: enable Settings → Scroll wallpaper")
                },
                trailingContent = {
                    Switch(checked = parallaxEnabled, onCheckedChange = viewModel::setParallaxEnabled)
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_set_live_wallpaper)) },
                supportingContent = { Text("Opens system wallpaper picker") },
                modifier = Modifier.clickable {
                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                        putExtra(
                            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            ComponentName(context, WalloraWallpaperService::class.java),
                        )
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try { context.startActivity(intent) } catch (_: Exception) {
                        context.startActivity(
                            Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                },
            )

            HorizontalDivider()

            // ── Default look ──────────────────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_default_look))
            val paramsLabel = if (defaultParams == com.wallora.app.domain.model.EditParams.Default) "Default" else
                "blur=${defaultParams.blur}, bright=${String.format("%.1f", defaultParams.brightness)}, contrast=${String.format("%.1f", defaultParams.contrast)}"
            ListItem(
                headlineContent = { Text("Current adjustments") },
                supportingContent = { Text(paramsLabel) },
                trailingContent = {
                    TextButton(onClick = viewModel::resetDefaultEditParams) { Text(stringResource(R.string.editor_reset)) }
                },
            )

            HorizontalDivider()

            // ── Theme ─────────────────────────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_theme))
            val themeLabel = when (theme) {
                "LIGHT" -> stringResource(R.string.settings_theme_light)
                "DARK" -> stringResource(R.string.settings_theme_dark)
                else -> stringResource(R.string.settings_theme_system)
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_theme)) },
                supportingContent = { Text(themeLabel) },
                modifier = Modifier.clickable { showThemePicker = true },
            )

            HorizontalDivider()

            // ── Cache ─────────────────────────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_cache))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_clear_cache)) },
                supportingContent = { Text("Clears browsed wallpapers cache") },
                modifier = Modifier.clickable { viewModel.clearCache() },
            )

            HorizontalDivider()

            // ── About / Licenses ──────────────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_about))
            ListItem(
                headlineContent = { Text("Version") },
                supportingContent = { Text("1.0") },
            )
            ListItem(
                headlineContent = { Text("Photo credits") },
                supportingContent = {
                    Text(
                        "Photos from Pexels (pexels.com) • Wallhaven (wallhaven.cc) • " +
                        "Unsplash (unsplash.com) • Reddit (reddit.com). " +
                        "Attribution per each source's license."
                    )
                },
            )
            // API key status for transparency
            ListItem(
                headlineContent = { Text("API key status") },
                supportingContent = {
                    val configured = buildList {
                        if (BuildConfig.PEXELS_API_KEY.isNotBlank()) add("Pexels")
                        if (BuildConfig.UNSPLASH_ACCESS_KEY.isNotBlank()) add("Unsplash")
                        if (BuildConfig.WALLHAVEN_API_KEY.isNotBlank()) add("Wallhaven")
                        add("Wallhaven (keyless SFW)")
                    }
                    Text(configured.joinToString(", "))
                },
            )
            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    if (showIntervalPicker) {
        AlertDialog(
            onDismissRequest = { showIntervalPicker = false },
            title = { Text(stringResource(R.string.settings_rotation_interval)) },
            text = {
                Column {
                    INTERVAL_OPTIONS.forEach { (ms, label) ->
                        ListItem(
                            headlineContent = { Text(label) },
                            trailingContent = {
                                if (ms == intervalMs) Text("✓", color = MaterialTheme.colorScheme.primary)
                            },
                            modifier = Modifier.clickable {
                                viewModel.setRotationInterval(ms)
                                showIntervalPicker = false
                            },
                        )
                    }
                }
            },
            confirmButton = {},
        )
    }

    if (showPlaylistPicker) {
        AlertDialog(
            onDismissRequest = { showPlaylistPicker = false },
            title = { Text("Playlist source") },
            text = {
                Column {
                    listOf("CATEGORIES" to "Current categories", "FAVORITES" to "Favorites").forEach { (key, label) ->
                        ListItem(
                            headlineContent = { Text(label) },
                            trailingContent = {
                                if (key == playlist) Text("✓", color = MaterialTheme.colorScheme.primary)
                            },
                            modifier = Modifier.clickable {
                                viewModel.setRotationPlaylist(key)
                                showPlaylistPicker = false
                            },
                        )
                    }
                }
            },
            confirmButton = {},
        )
    }

    if (showAddTimePicker) {
        AlertDialog(
            onDismissRequest = { showAddTimePicker = false; addTimeInput = "" },
            title = { Text(stringResource(R.string.settings_rotation_times)) },
            text = {
                OutlinedTextField(
                    value = addTimeInput,
                    onValueChange = { addTimeInput = it },
                    label = { Text("Time (HH:mm)") },
                    placeholder = { Text("e.g. 08:00") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = addTimeInput.trim()
                    if (trimmed.matches(Regex("\\d{2}:\\d{2}"))) {
                        viewModel.addRotationTime(trimmed)
                        addTimeInput = ""
                        showAddTimePicker = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddTimePicker = false; addTimeInput = "" }) { Text("Cancel") }
            },
        )
    }

    if (showThemePicker) {
        AlertDialog(
            onDismissRequest = { showThemePicker = false },
            title = { Text(stringResource(R.string.settings_theme)) },
            text = {
                Column {
                    listOf("SYSTEM" to R.string.settings_theme_system, "LIGHT" to R.string.settings_theme_light, "DARK" to R.string.settings_theme_dark)
                        .forEach { (key, labelRes) ->
                            ListItem(
                                headlineContent = { Text(stringResource(labelRes)) },
                                trailingContent = {
                                    if (key == theme) Text("✓", color = MaterialTheme.colorScheme.primary)
                                },
                                modifier = Modifier.clickable {
                                    viewModel.setTheme(key)
                                    showThemePicker = false
                                },
                            )
                        }
                }
            },
            confirmButton = {},
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
