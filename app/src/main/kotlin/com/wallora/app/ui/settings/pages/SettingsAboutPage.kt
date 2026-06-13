package com.wallora.app.ui.settings.pages

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wallora.app.R
import com.wallora.app.ui.settings.components.SettingsScaffold

@Composable
fun SettingsAboutPage(onBack: () -> Unit) {
    val context = LocalContext.current

    SettingsScaffold(
        title = stringResource(R.string.settings_about),
        onBack = onBack,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_about_version)) },
                supportingContent = { Text(stringResource(R.string.settings_about_license)) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_developer)) },
                supportingContent = { Text(stringResource(R.string.settings_developer_name)) },
                trailingContent = {
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/thissayantan/wallora"),
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }) {
                        Text(stringResource(R.string.settings_github))
                    }
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_credits_title)) },
                supportingContent = { Text(stringResource(R.string.settings_credits_desc)) },
            )
        }
    }
}
