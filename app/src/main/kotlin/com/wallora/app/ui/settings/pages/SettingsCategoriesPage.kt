package com.wallora.app.ui.settings.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallora.app.R
import com.wallora.app.domain.model.Category
import com.wallora.app.ui.settings.SettingsViewModel
import com.wallora.app.ui.settings.components.SettingsScaffold

/** Known subreddits with human-readable descriptions, shown as toggle rows. */
private data class KnownSubreddit(val name: String, val description: String)

private val KNOWN_SUBREDDITS = listOf(
    KnownSubreddit("wallpapers",        "General wallpaper community"),
    KnownSubreddit("wallpaper",         "Wallpaper collection & requests"),
    KnownSubreddit("EarthPorn",         "Stunning natural landscapes"),
    KnownSubreddit("spaceporn",         "Space & astronomy photography"),
    KnownSubreddit("CityPorn",          "Urban & cityscape photography"),
    KnownSubreddit("Amoledbackgrounds", "Dark AMOLED-optimized wallpapers"),
    KnownSubreddit("MobileWallpaper",   "Portrait wallpapers for mobile"),
    KnownSubreddit("AIArt",             "AI-generated artwork"),
    KnownSubreddit("midjourney",        "Midjourney AI creations"),
    KnownSubreddit("ImaginaryLandscapes","Fantasy & fictional landscapes"),
    KnownSubreddit("carporn",           "Cars & automotive photography"),
    KnownSubreddit("ArchitecturePorn",  "Architecture & building photography"),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsCategoriesPage(
    vm: SettingsViewModel,
    onBack: () -> Unit,
) {
    val selectedCategories by vm.selectedCategories.collectAsStateWithLifecycle()
    val customKeywords by vm.customKeywords.collectAsStateWithLifecycle()
    val userSubreddits by vm.userSubreddits.collectAsStateWithLifecycle()

    var showAddKeywordDialog by remember { mutableStateOf(false) }
    var showAddSubredditDialog by remember { mutableStateOf(false) }
    var addKeywordInput by remember { mutableStateOf("") }
    var addSubredditInput by remember { mutableStateOf("") }

    // Custom subreddits = user-added ones not in the known list
    val knownNames = KNOWN_SUBREDDITS.map { it.name }
    val customSubreddits = userSubreddits.filter { it !in knownNames }

    SettingsScaffold(
        title = stringResource(R.string.settings_categories_title),
        onBack = onBack,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {

            // ── Categories ────────────────────────────────────────────────
            SectionHeader("Categories")
            Text(
                text = stringResource(R.string.settings_categories_focus_hint),
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
                        onClick = { vm.toggleCategory(cat) },
                        label = { Text(cat.displayName) },
                    )
                }
            }

            HorizontalDivider()

            // ── Custom topics ─────────────────────────────────────────────
            SectionHeader("Custom topics")
            Text(
                text = "Your own search keywords added as browsable categories.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            if (customKeywords.isEmpty()) {
                Text(
                    text = "No custom topics yet — tap Add to create one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            } else {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    customKeywords.sorted().forEach { keyword ->
                        InputChip(
                            selected = true,
                            onClick = {},
                            label = { Text(keyword) },
                            trailingIcon = {
                                IconButton(onClick = { vm.removeCustomKeyword(keyword) }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(
                                            R.string.settings_remove_content_desc, keyword,
                                        ),
                                    )
                                }
                            },
                        )
                    }
                }
            }
            ListItem(
                leadingContent = {
                    Icon(Icons.Default.Add, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                },
                headlineContent = {
                    Text(
                        stringResource(R.string.settings_custom_keyword_add),
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAddKeywordDialog = true },
            )

            HorizontalDivider()

            // ── Reddit sources ────────────────────────────────────────────
            SectionHeader(stringResource(R.string.settings_reddit_section))
            Text(
                text = "Toggle the subreddits Wallora browses for wallpapers.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            // Known subreddits as toggle rows
            KNOWN_SUBREDDITS.forEach { sub ->
                val isActive = sub.name in userSubreddits
                ListItem(
                    headlineContent = { Text("r/${sub.name}") },
                    supportingContent = {
                        Text(
                            sub.description,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = isActive,
                            onCheckedChange = { enabled ->
                                if (enabled) vm.addSubreddit(sub.name)
                                else vm.removeSubreddit(sub.name)
                            },
                        )
                    },
                )
            }

            // Custom / user-added subreddits (not in the known list)
            if (customSubreddits.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Text(
                    text = "Custom",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
                customSubreddits.forEach { sub ->
                    ListItem(
                        headlineContent = { Text("r/$sub") },
                        trailingContent = {
                            IconButton(onClick = { vm.removeSubreddit(sub) }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(
                                        R.string.settings_remove_content_desc, "r/$sub",
                                    ),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                    )
                }
            }

            // Add + Reset row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { showAddSubredditDialog = true }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Text(stringResource(R.string.settings_reddit_add))
                }
                TextButton(onClick = { vm.resetSubreddits() }) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Text(stringResource(R.string.settings_reddit_reset))
                }
            }
        }
    }

    // ── Add keyword dialog ────────────────────────────────────────────────
    if (showAddKeywordDialog) {
        AlertDialog(
            onDismissRequest = { showAddKeywordDialog = false; addKeywordInput = "" },
            title = { Text(stringResource(R.string.settings_custom_keyword_dialog_title)) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.settings_custom_keyword_dialog_body),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    OutlinedTextField(
                        value = addKeywordInput,
                        onValueChange = { addKeywordInput = it },
                        label = { Text("Keyword") },
                        placeholder = { Text(stringResource(R.string.settings_custom_keyword_placeholder)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (addKeywordInput.isNotBlank()) {
                                vm.addCustomKeyword(addKeywordInput)
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
                        vm.addCustomKeyword(addKeywordInput)
                        addKeywordInput = ""
                        showAddKeywordDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddKeywordDialog = false; addKeywordInput = "" }) {
                    Text("Cancel")
                }
            },
        )
    }

    // ── Add subreddit dialog ──────────────────────────────────────────────
    if (showAddSubredditDialog) {
        AlertDialog(
            onDismissRequest = { showAddSubredditDialog = false; addSubredditInput = "" },
            title = { Text(stringResource(R.string.settings_reddit_add)) },
            text = {
                OutlinedTextField(
                    value = addSubredditInput,
                    onValueChange = { addSubredditInput = it },
                    label = { Text("Subreddit name") },
                    placeholder = { Text(stringResource(R.string.settings_reddit_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (addSubredditInput.isNotBlank()) {
                            vm.addSubreddit(addSubredditInput)
                            addSubredditInput = ""
                            showAddSubredditDialog = false
                        }
                    }),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (addSubredditInput.isNotBlank()) {
                        vm.addSubreddit(addSubredditInput)
                        addSubredditInput = ""
                        showAddSubredditDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddSubredditDialog = false; addSubredditInput = ""
                }) { Text("Cancel") }
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
