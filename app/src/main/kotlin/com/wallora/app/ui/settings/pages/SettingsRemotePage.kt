package com.wallora.app.ui.settings.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wallora.app.R
import com.wallora.app.ui.settings.components.SettingsScaffold

@Composable
fun SettingsRemotePage(onBack: () -> Unit) {
    SettingsScaffold(
        title = stringResource(R.string.settings_remote_title),
        onBack = onBack,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_remote_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(12.dp),
                )
            }

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_remote_longpress_title)) },
                supportingContent = { Text(stringResource(R.string.settings_remote_longpress_desc)) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_remote_gesture_title)) },
                supportingContent = { Text(stringResource(R.string.settings_remote_gesture_desc)) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_remote_tile_title)) },
                supportingContent = { Text(stringResource(R.string.settings_remote_tile_desc)) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_remote_tasker_title)) },
                supportingContent = { Text(stringResource(R.string.settings_remote_tasker_desc)) },
            )
        }
    }
}
