package com.wallora.app.ui.settings.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallora.app.R
import com.wallora.app.domain.model.SourceId
import com.wallora.app.ui.settings.SettingsEvent
import com.wallora.app.ui.settings.SettingsViewModel
import com.wallora.app.ui.settings.components.ApiKeyField
import com.wallora.app.ui.settings.components.SettingsScaffold

@Composable
fun SettingsSourcesPage(
    vm: SettingsViewModel,
    onBack: () -> Unit,
) {
    val enabledSources by vm.enabledSources.collectAsStateWithLifecycle()
    val sourceConfiguredMap by vm.sourceConfiguredMap.collectAsStateWithLifecycle()
    val userPexelsKey by vm.userPexelsKey.collectAsStateWithLifecycle()
    val userUnsplashKey by vm.userUnsplashKey.collectAsStateWithLifecycle()
    val userWallhavenKey by vm.userWallhavenKey.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            if (event is SettingsEvent.ShowMessage) snackbarHostState.showSnackbar(event.message)
        }
    }

    SettingsScaffold(
        title = stringResource(R.string.settings_sources_title),
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Source enable toggles
            SectionHeader(stringResource(R.string.settings_sources))
            SourceId.entries.forEach { source ->
                val isConfigured = sourceConfiguredMap[source] ?: false
                ListItem(
                    headlineContent = { Text(source.displayName) },
                    supportingContent = {
                        if (!isConfigured) {
                            Text(stringResource(R.string.settings_source_needs_key))
                        }
                    },
                    trailingContent = {
                        Switch(
                            checked = source in enabledSources && isConfigured,
                            onCheckedChange = { vm.setSourceEnabled(source, it) },
                            enabled = isConfigured,
                        )
                    },
                )
            }

            HorizontalDivider()

            // API Keys
            SectionHeader(stringResource(R.string.settings_api_keys_section))
            Text(
                text = stringResource(R.string.settings_api_keys_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            ApiKeyField(
                label = stringResource(R.string.settings_api_pexels_label),
                hint = stringResource(R.string.settings_api_pexels_hint),
                currentKey = userPexelsKey,
                onSave = vm::saveUserPexelsKey,
            )
            ApiKeyField(
                label = stringResource(R.string.settings_api_unsplash_label),
                hint = stringResource(R.string.settings_api_unsplash_hint),
                currentKey = userUnsplashKey,
                onSave = vm::saveUserUnsplashKey,
            )
            ApiKeyField(
                label = stringResource(R.string.settings_api_wallhaven_label),
                hint = stringResource(R.string.settings_api_wallhaven_hint),
                currentKey = userWallhavenKey,
                onSave = vm::saveUserWallhavenKey,
            )
        }
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
