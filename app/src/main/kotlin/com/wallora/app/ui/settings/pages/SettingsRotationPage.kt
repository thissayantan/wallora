package com.wallora.app.ui.settings.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallora.app.R
import com.wallora.app.ui.settings.SettingsViewModel
import com.wallora.app.ui.settings.components.SettingsScaffold
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

@Composable
fun SettingsRotationPage(
    vm: SettingsViewModel,
    onBack: () -> Unit,
) {
    val rotationEnabled by vm.rotationEnabled.collectAsStateWithLifecycle()
    val intervalMs by vm.rotationIntervalMs.collectAsStateWithLifecycle()
    val playlist by vm.rotationPlaylist.collectAsStateWithLifecycle()
    val times by vm.rotationTimes.collectAsStateWithLifecycle()
    val wifiOnly by vm.rotationWifiOnly.collectAsStateWithLifecycle()
    val chargingOnly by vm.rotationChargingOnly.collectAsStateWithLifecycle()
    val nextChange by vm.nextChangeLabel.collectAsStateWithLifecycle()

    var showIntervalPicker by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var showAddTimePicker by remember { mutableStateOf(false) }
    var addTimeInput by remember { mutableStateOf("") }

    SettingsScaffold(
        title = stringResource(R.string.settings_rotation_title),
        onBack = onBack,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_rotation_enable)) },
                supportingContent = {
                    val desc = if (nextChange.isNotEmpty()) "Next: $nextChange"
                    else stringResource(R.string.settings_rotation_enable_desc)
                    Text(desc)
                },
                trailingContent = {
                    Switch(checked = rotationEnabled, onCheckedChange = vm::setRotationEnabled)
                },
            )

            HorizontalDivider()

            SectionHeader("Schedule")

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_rotation_interval)) },
                supportingContent = {
                    Text(INTERVAL_OPTIONS.firstOrNull { it.first == intervalMs }?.second ?: "Custom")
                },
                modifier = Modifier.clickable { showIntervalPicker = true },
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_rotation_playlist)) },
                supportingContent = {
                    Text(
                        if (playlist == "FAVORITES") stringResource(R.string.settings_rotation_playlist_favorites)
                        else stringResource(R.string.settings_rotation_playlist_categories),
                    )
                },
                modifier = Modifier.clickable { showPlaylistPicker = true },
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_rotation_times)) },
                supportingContent = {
                    Text(
                        if (times.isEmpty()) stringResource(R.string.settings_rotation_no_times)
                        else times.sorted().joinToString(", "),
                    )
                },
                trailingContent = {
                    TextButton(onClick = { showAddTimePicker = true }) {
                        Text(stringResource(R.string.settings_rotation_add_time))
                    }
                },
            )
            times.sorted().forEach { t ->
                ListItem(
                    headlineContent = { Text(t) },
                    trailingContent = {
                        TextButton(onClick = { vm.removeRotationTime(t) }) { Text("Remove") }
                    },
                    modifier = Modifier.padding(start = 16.dp),
                )
            }

            HorizontalDivider()

            SectionHeader("Constraints")

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_wifi_only)) },
                trailingContent = { Switch(checked = wifiOnly, onCheckedChange = vm::setWifiOnly) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_charging_only)) },
                trailingContent = {
                    Switch(checked = chargingOnly, onCheckedChange = vm::setChargingOnly)
                },
            )
        }
    }

    // Dialogs
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
                                vm.setRotationInterval(ms)
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
            title = { Text(stringResource(R.string.settings_rotation_playlist)) },
            text = {
                Column {
                    listOf(
                        "CATEGORIES" to R.string.settings_rotation_playlist_categories,
                        "FAVORITES" to R.string.settings_rotation_playlist_favorites,
                    ).forEach { (key, labelRes) ->
                        ListItem(
                            headlineContent = { Text(stringResource(labelRes)) },
                            trailingContent = {
                                if (key == playlist) {
                                    Text("✓", color = MaterialTheme.colorScheme.primary)
                                }
                            },
                            modifier = Modifier.clickable {
                                vm.setRotationPlaylist(key)
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
            title = { Text(stringResource(R.string.settings_rotation_add_time)) },
            text = {
                OutlinedTextField(
                    value = addTimeInput,
                    onValueChange = { addTimeInput = it },
                    label = { Text("Time (HH:mm)") },
                    placeholder = { Text(stringResource(R.string.settings_rotation_time_placeholder)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = addTimeInput.trim()
                    if (trimmed.matches(Regex("\\d{2}:\\d{2}"))) {
                        vm.addRotationTime(trimmed)
                        addTimeInput = ""
                        showAddTimePicker = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddTimePicker = false; addTimeInput = "" }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
    )
}
