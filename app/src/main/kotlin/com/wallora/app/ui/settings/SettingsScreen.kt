package com.wallora.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallora.app.R
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val rotationEnabled by viewModel.rotationEnabled.collectAsStateWithLifecycle()
    val intervalMs by viewModel.rotationIntervalMs.collectAsStateWithLifecycle()
    val playlist by viewModel.rotationPlaylist.collectAsStateWithLifecycle()
    val times by viewModel.rotationTimes.collectAsStateWithLifecycle()
    val wifiOnly by viewModel.rotationWifiOnly.collectAsStateWithLifecycle()
    val chargingOnly by viewModel.rotationChargingOnly.collectAsStateWithLifecycle()
    val nextChange by viewModel.nextChangeLabel.collectAsStateWithLifecycle()

    var showIntervalPicker by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var showAddTimePicker by remember { mutableStateOf(false) }
    var addTimeInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Auto-rotation section ─────────────────────────────────────────────
        SettingsSectionHeader(stringResource(R.string.settings_rotation))

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_rotation)) },
            supportingContent = {
                if (nextChange.isNotEmpty()) Text("Next change: $nextChange")
            },
            trailingContent = {
                Switch(
                    checked = rotationEnabled,
                    onCheckedChange = viewModel::setRotationEnabled,
                )
            },
        )

        if (rotationEnabled) {
            HorizontalDivider()

            // Interval
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_rotation_interval)) },
                supportingContent = {
                    Text(INTERVAL_OPTIONS.firstOrNull { it.first == intervalMs }?.second ?: "Custom")
                },
                modifier = Modifier.clickable { showIntervalPicker = true },
            )

            // Playlist
            ListItem(
                headlineContent = { Text("Playlist") },
                supportingContent = {
                    Text(if (playlist == "FAVORITES") "Favorites" else "Current categories")
                },
                modifier = Modifier.clickable { showPlaylistPicker = true },
            )

            // Exact alarm times
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_rotation_times)) },
                supportingContent = {
                    if (times.isEmpty()) {
                        Text("No specific times set")
                    } else {
                        Text(times.sorted().joinToString(", "))
                    }
                },
                trailingContent = {
                    TextButton(onClick = { showAddTimePicker = true }) { Text("Add") }
                },
            )

            if (times.isNotEmpty()) {
                times.sorted().forEach { time ->
                    ListItem(
                        headlineContent = { Text(time) },
                        trailingContent = {
                            TextButton(onClick = { viewModel.removeRotationTime(time) }) {
                                Text("Remove")
                            }
                        },
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }

            HorizontalDivider()

            // Wi-Fi only
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_wifi_only)) },
                trailingContent = {
                    Switch(checked = wifiOnly, onCheckedChange = viewModel::setWifiOnly)
                },
            )

            // Charging only
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_charging_only)) },
                trailingContent = {
                    Switch(checked = chargingOnly, onCheckedChange = viewModel::setChargingOnly)
                },
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Live wallpaper section ────────────────────────────────────────────
        SettingsSectionHeader("Live Wallpaper")
        LiveWallpaperSection()

        Spacer(Modifier.height(24.dp))

        // Placeholder for sections added in P6-a
        SettingsSectionHeader("More settings")
        ListItem(headlineContent = { Text("Additional settings — coming in Phase 6") })
        Spacer(Modifier.height(16.dp))
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
                                if (ms == intervalMs) {
                                    Text("✓", color = MaterialTheme.colorScheme.primary)
                                }
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
            onDismissRequest = {
                showAddTimePicker = false
                addTimeInput = ""
            },
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
                TextButton(
                    onClick = {
                        val trimmed = addTimeInput.trim()
                        if (trimmed.matches(Regex("\\d{2}:\\d{2}"))) {
                            viewModel.addRotationTime(trimmed)
                            addTimeInput = ""
                            showAddTimePicker = false
                        }
                    },
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddTimePicker = false
                    addTimeInput = ""
                }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun LiveWallpaperSection() {
    val context = androidx.compose.ui.platform.LocalContext.current
    ListItem(
        headlineContent = { Text(androidx.compose.ui.res.stringResource(R.string.settings_set_live_wallpaper)) },
        supportingContent = { Text("Opens the system wallpaper picker") },
        modifier = Modifier.clickable {
            val intent = Intent(android.app.WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    android.app.WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    android.content.ComponentName(context, com.wallora.app.service.WalloraWallpaperService::class.java),
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try { context.startActivity(intent) } catch (_: Exception) {
                context.startActivity(
                    Intent(android.app.WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        },
    )
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
