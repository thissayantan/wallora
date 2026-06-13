package com.wallora.app.ui.settings

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallora.app.R
import com.wallora.app.domain.model.Category
import com.wallora.app.domain.model.EditParams
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
    val sourceConfiguredMap by viewModel.sourceConfiguredMap.collectAsStateWithLifecycle()
    val selectedCategories by viewModel.selectedCategories.collectAsStateWithLifecycle()
    val customKeywords by viewModel.customKeywords.collectAsStateWithLifecycle()
    val userSubreddits by viewModel.userSubreddits.collectAsStateWithLifecycle()
    val rotationEnabled by viewModel.rotationEnabled.collectAsStateWithLifecycle()
    val intervalMs by viewModel.rotationIntervalMs.collectAsStateWithLifecycle()
    val playlist by viewModel.rotationPlaylist.collectAsStateWithLifecycle()
    val times by viewModel.rotationTimes.collectAsStateWithLifecycle()
    val wifiOnly by viewModel.rotationWifiOnly.collectAsStateWithLifecycle()
    val chargingOnly by viewModel.rotationChargingOnly.collectAsStateWithLifecycle()
    val rotationOnUnlock by viewModel.rotationOnUnlock.collectAsStateWithLifecycle()
    val nextChange by viewModel.nextChangeLabel.collectAsStateWithLifecycle()
    val parallaxEnabled by viewModel.parallaxEnabled.collectAsStateWithLifecycle()
    val defaultParams by viewModel.defaultEditParams.collectAsStateWithLifecycle()
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val userPexelsKey by viewModel.userPexelsKey.collectAsStateWithLifecycle()
    val userUnsplashKey by viewModel.userUnsplashKey.collectAsStateWithLifecycle()
    val userWallhavenKey by viewModel.userWallhavenKey.collectAsStateWithLifecycle()

    var showIntervalPicker by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var showAddTimePicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showAddKeywordDialog by remember { mutableStateOf(false) }
    var showAddSubredditDialog by remember { mutableStateOf(false) }
    var addTimeInput by remember { mutableStateOf("") }
    var addKeywordInput by remember { mutableStateOf("") }
    var addSubredditInput by remember { mutableStateOf("") }
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
                val isConfigured = sourceConfiguredMap[source] ?: false
                ListItem(
                    headlineContent = { Text(source.displayName) },
                    supportingContent = {
                        if (!isConfigured) Text("Add API key below to enable")
                    },
                    trailingContent = {
                        Switch(
                            checked = source in enabledSources && isConfigured,
                            onCheckedChange = { viewModel.setSourceEnabled(source, it) },
                            enabled = isConfigured,
                        )
                    },
                )
            }

            HorizontalDivider()

            // ── Browse categories ─────────────────────────────────────────────
            SettingsSectionHeader("Browse categories")
            Text(
                text = "Select categories to focus on — leave all off to show everything",
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
                    FilterChip(
                        selected = cat in selectedCategories,
                        onClick = { viewModel.toggleCategory(cat) },
                        label = { Text(cat.displayName) },
                    )
                }
                // Custom keywords
                customKeywords.sorted().forEach { keyword ->
                    InputChip(
                        selected = true,
                        onClick = {},
                        label = { Text(keyword) },
                        trailingIcon = {
                            IconButton(onClick = { viewModel.removeCustomKeyword(keyword) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove $keyword")
                            }
                        },
                    )
                }
                AssistChip(
                    onClick = { showAddKeywordDialog = true },
                    label = { Text("Add keyword") },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                )
            }

            HorizontalDivider()

            // ── Reddit subreddits ─────────────────────────────────────────────
            SettingsSectionHeader("Reddit sources")
            Text(
                text = "Subreddits Wallora browses for wallpapers",
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
                userSubreddits.forEach { sub ->
                    InputChip(
                        selected = false,
                        onClick = {},
                        label = { Text("r/$sub") },
                        trailingIcon = {
                            IconButton(onClick = { viewModel.removeSubreddit(sub) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove r/$sub")
                            }
                        },
                    )
                }
                AssistChip(
                    onClick = { showAddSubredditDialog = true },
                    label = { Text("Add subreddit") },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                )
            }
            ListItem(
                headlineContent = { Text("Reset to defaults") },
                modifier = Modifier.clickable { viewModel.resetSubreddits() },
            )

            HorizontalDivider()

            // ── Auto-change wallpaper ─────────────────────────────────────────
            SettingsSectionHeader("Auto-change wallpaper")
            ListItem(
                headlineContent = { Text("Enable auto-change") },
                supportingContent = {
                    if (nextChange.isNotEmpty()) Text("Next: $nextChange")
                    else Text("Rotates wallpaper on a schedule or at set times")
                },
                trailingContent = {
                    Switch(checked = rotationEnabled, onCheckedChange = viewModel::setRotationEnabled)
                },
            )
            // Interval — always visible, not hidden behind the toggle
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
                    Text(if (playlist == "FAVORITES") "Favorites only" else "Current categories")
                },
                modifier = Modifier.clickable { showPlaylistPicker = true },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_rotation_times)) },
                supportingContent = {
                    if (times.isEmpty()) Text("No specific times set")
                    else Text(times.sorted().joinToString(", "))
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
            if (rotationEnabled) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_rotation_on_unlock)) },
                    supportingContent = {
                        Text("Requires live wallpaper mode")
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

            // ── Change from anywhere ──────────────────────────────────────────
            SettingsSectionHeader("Change wallpaper from anywhere")
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "No need to open the app — Wallora integrates with your launcher, " +
                            "notification shade, and automation apps.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            ListItem(
                headlineContent = { Text("Long-press app icon") },
                supportingContent = { Text("Tap \"Next wallpaper\" — works with any launcher") },
            )
            ListItem(
                headlineContent = { Text("Nova / Lawnchair gesture") },
                supportingContent = {
                    Text("Launcher settings → Gestures → App shortcuts → Wallora → Next wallpaper")
                },
            )
            ListItem(
                headlineContent = { Text("Quick Settings tile") },
                supportingContent = {
                    Text("Pull down shade → edit tiles → drag \"Next wallpaper\" into active tiles")
                },
            )
            ListItem(
                headlineContent = { Text("Tasker / automation") },
                supportingContent = {
                    Text("Intent action: com.wallora.app.action.NEXT_WALLPAPER\nClass: com.wallora.app.ui.NextWallpaperActivity")
                },
            )

            HorizontalDivider()

            // ── Live wallpaper & parallax ─────────────────────────────────────
            SettingsSectionHeader("Live wallpaper & parallax")
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_parallax)) },
                supportingContent = {
                    Text("Scrolls the wallpaper as you swipe between home screens. Requires live wallpaper mode and a launcher that sends scroll offsets (Nova Launcher: Settings → Scroll wallpaper).")
                },
                trailingContent = {
                    Switch(checked = parallaxEnabled, onCheckedChange = viewModel::setParallaxEnabled)
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_set_live_wallpaper)) },
                supportingContent = {
                    Text("Enables parallax, on-unlock rotation, and auto-change without keeping the screen on")
                },
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

            // ── Default filters ───────────────────────────────────────────────
            SettingsSectionHeader("Default filters")
            val paramsLabel = if (defaultParams == EditParams.Default) {
                "None applied"
            } else {
                buildString {
                    if (defaultParams.blur > 0) append("blur=${defaultParams.blur.toInt()}% ")
                    if (defaultParams.brightness != 0f) append("brightness=${String.format("%+.0f", defaultParams.brightness * 100)}% ")
                    if (defaultParams.contrast != 1f) append("contrast=${String.format("%.1f", defaultParams.contrast)}× ")
                    if (defaultParams.saturation != 1f) append("saturation=${String.format("%.1f", defaultParams.saturation)}×")
                }.trim()
            }
            ListItem(
                headlineContent = { Text("Wallpaper filters") },
                supportingContent = {
                    Text(paramsLabel + if (paramsLabel == "None applied") "\nTap Edit & Set in any wallpaper preview to configure" else "")
                },
                trailingContent = {
                    if (defaultParams != EditParams.Default) {
                        TextButton(onClick = viewModel::resetDefaultEditParams) { Text("Reset") }
                    }
                },
            )

            HorizontalDivider()

            // ── API Keys ──────────────────────────────────────────────────────
            SettingsSectionHeader("API Keys")
            Text(
                text = "Add your own free API keys to enable more sources. Keys are stored on-device only.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            ApiKeyField(
                label = "Pexels API key",
                hint = "Get free key at pexels.com/api",
                currentKey = userPexelsKey,
                onSave = viewModel::saveUserPexelsKey,
            )
            ApiKeyField(
                label = "Unsplash Access Key",
                hint = "Get free key at unsplash.com/developers",
                currentKey = userUnsplashKey,
                onSave = viewModel::saveUserUnsplashKey,
            )
            ApiKeyField(
                label = "Wallhaven API key (optional)",
                hint = "wallhaven.cc/settings/account — unlocks higher limits",
                currentKey = userWallhavenKey,
                onSave = viewModel::saveUserWallhavenKey,
            )

            HorizontalDivider()

            // ── Appearance ────────────────────────────────────────────────────
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

            // ── Storage ───────────────────────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_cache))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_clear_cache)) },
                supportingContent = { Text("Thumbnails, full-res images, and page cache") },
                modifier = Modifier.clickable { viewModel.clearCache() },
            )

            HorizontalDivider()

            // ── About ─────────────────────────────────────────────────────────
            SettingsSectionHeader(stringResource(R.string.settings_about))
            ListItem(
                headlineContent = { Text("Wallora v1.0") },
                supportingContent = { Text("Open-source · MIT License") },
            )
            ListItem(
                headlineContent = { Text("Developer") },
                supportingContent = { Text("Sayantan Dey") },
                trailingContent = {
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/thissayantan/wallora"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }) { Text("GitHub") }
                },
            )
            ListItem(
                headlineContent = { Text("Photo credits") },
                supportingContent = {
                    Text("Photos from Pexels · Wallhaven · Unsplash · Reddit. Attribution per each source's license.")
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
                    listOf("CATEGORIES" to "Current categories", "FAVORITES" to "Favorites only").forEach { (key, label) ->
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
                    listOf(
                        "SYSTEM" to R.string.settings_theme_system,
                        "LIGHT" to R.string.settings_theme_light,
                        "DARK" to R.string.settings_theme_dark,
                    ).forEach { (key, labelRes) ->
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

    if (showAddKeywordDialog) {
        AlertDialog(
            onDismissRequest = { showAddKeywordDialog = false; addKeywordInput = "" },
            title = { Text("Add custom category") },
            text = {
                Column {
                    Text(
                        "Type any keyword to add it as a browsable category.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    OutlinedTextField(
                        value = addKeywordInput,
                        onValueChange = { addKeywordInput = it },
                        label = { Text("Keyword") },
                        placeholder = { Text("e.g. ocean sunset") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (addKeywordInput.isNotBlank()) {
                                viewModel.addCustomKeyword(addKeywordInput)
                                addKeywordInput = ""
                                showAddKeywordDialog = false
                            }
                        }),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (addKeywordInput.isNotBlank()) {
                        viewModel.addCustomKeyword(addKeywordInput)
                        addKeywordInput = ""
                        showAddKeywordDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddKeywordDialog = false; addKeywordInput = "" }) { Text("Cancel") }
            },
        )
    }

    if (showAddSubredditDialog) {
        AlertDialog(
            onDismissRequest = { showAddSubredditDialog = false; addSubredditInput = "" },
            title = { Text("Add subreddit") },
            text = {
                OutlinedTextField(
                    value = addSubredditInput,
                    onValueChange = { addSubredditInput = it },
                    label = { Text("Subreddit name") },
                    placeholder = { Text("e.g. wallpapers or r/wallpapers") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (addSubredditInput.isNotBlank()) {
                            viewModel.addSubreddit(addSubredditInput)
                            addSubredditInput = ""
                            showAddSubredditDialog = false
                        }
                    }),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (addSubredditInput.isNotBlank()) {
                        viewModel.addSubreddit(addSubredditInput)
                        addSubredditInput = ""
                        showAddSubredditDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubredditDialog = false; addSubredditInput = "" }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ApiKeyField(
    label: String,
    hint: String,
    currentKey: String,
    onSave: (String) -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember(currentKey) { mutableStateOf(currentKey) }
    var showKey by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(label) },
        supportingContent = {
            if (editing) {
                Column {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = { Text(hint) },
                        singleLine = true,
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            onSave(draft)
                            editing = false
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showKey = !showKey }) {
                            Text(if (showKey) "Hide" else "Show")
                        }
                        TextButton(onClick = { onSave(draft); editing = false }) {
                            Text("Save")
                        }
                        TextButton(onClick = { onSave(""); draft = ""; editing = false }) {
                            Text("Clear")
                        }
                    }
                }
            } else {
                Text(
                    if (currentKey.isNotBlank()) "Key configured (${currentKey.take(6)}…)" else hint,
                    color = if (currentKey.isNotBlank()) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = {
            if (!editing) {
                TextButton(onClick = { editing = true }) { Text("Edit") }
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
