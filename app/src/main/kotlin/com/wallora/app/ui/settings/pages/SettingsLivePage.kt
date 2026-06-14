package com.wallora.app.ui.settings.pages

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wallora.app.R
import com.wallora.app.domain.model.EditParams
import com.wallora.app.service.WalloraWallpaperService
import com.wallora.app.ui.settings.SettingsViewModel
import com.wallora.app.ui.settings.components.SettingsScaffold
import androidx.compose.foundation.clickable

@Composable
fun SettingsLivePage(
    vm: SettingsViewModel,
    onBack: () -> Unit,
) {
    val parallaxEnabled by vm.parallaxEnabled.collectAsStateWithLifecycle()
    val rotationOnUnlock by vm.rotationOnUnlock.collectAsStateWithLifecycle()
    val defaultParams by vm.defaultEditParams.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val paramsLabel = if (defaultParams == EditParams.Default) {
        stringResource(R.string.settings_filters_none)
    } else {
        buildString {
            if (defaultParams.blur > 0) append("blur=${defaultParams.blur.toInt()}% ")
            if (defaultParams.brightness != 0f) {
                append("brightness=${String.format("%+.0f", defaultParams.brightness * 100)}% ")
            }
            if (defaultParams.contrast != 1f) {
                append("contrast=${String.format("%.1f", defaultParams.contrast)}× ")
            }
            if (defaultParams.saturation != 1f) {
                append("saturation=${String.format("%.1f", defaultParams.saturation)}×")
            }
        }.trim()
    }

    SettingsScaffold(
        title = stringResource(R.string.settings_live_title),
        onBack = onBack,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader("Live wallpaper")

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_set_live_wallpaper)) },
                supportingContent = {
                    Text(stringResource(R.string.settings_set_live_wallpaper_desc))
                },
                modifier = Modifier.clickable {
                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                        putExtra(
                            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            ComponentName(context, WalloraWallpaperService::class.java),
                        )
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        context.startActivity(
                            Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_rotation_on_unlock)) },
                supportingContent = { Text(stringResource(R.string.settings_rotation_on_unlock_live_desc)) },
                trailingContent = {
                    Switch(checked = rotationOnUnlock, onCheckedChange = vm::setRotationOnUnlock)
                },
            )

            HorizontalDivider()

            SectionHeader("Parallax")

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_parallax)) },
                supportingContent = { Text(stringResource(R.string.settings_parallax_desc)) },
                trailingContent = {
                    Switch(
                        checked = parallaxEnabled,
                        onCheckedChange = vm::setParallaxEnabled,
                    )
                },
            )

            HorizontalDivider()

            SectionHeader(stringResource(R.string.settings_filters_section))

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_filters_section)) },
                supportingContent = {
                    Text(
                        paramsLabel + if (defaultParams == EditParams.Default) {
                            "\n" + stringResource(R.string.settings_filters_desc)
                        } else "",
                    )
                },
                trailingContent = {
                    if (defaultParams != EditParams.Default) {
                        TextButton(onClick = vm::resetDefaultEditParams) {
                            Text(stringResource(R.string.settings_filters_reset))
                        }
                    }
                },
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
