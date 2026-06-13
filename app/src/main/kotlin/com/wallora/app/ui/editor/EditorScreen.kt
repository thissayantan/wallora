package com.wallora.app.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.wallora.app.R
import com.wallora.app.data.util.ImageAdjustments
import com.wallora.app.domain.model.EditParams
import com.wallora.app.domain.model.Wallpaper
import com.wallora.app.domain.usecase.WallpaperTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    wallpaper: Wallpaper,
    onBack: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val editParams by viewModel.editParams.collectAsStateWithLifecycle()
    val isApplying by viewModel.isApplying.collectAsStateWithLifecycle()
    val setAsDefault by viewModel.setAsDefault.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showTargetSheet by remember { mutableStateOf(false) }

    LaunchedEffect(wallpaper) { viewModel.loadWallpaper(wallpaper) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EditorEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is EditorEvent.ApplyDone -> onBack()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Edit wallpaper") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::resetAll) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.editor_reset))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (setAsDefault) stringResource(R.string.editor_apply_default)
                               else stringResource(R.string.editor_apply_this),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = setAsDefault,
                        onCheckedChange = { viewModel.toggleSetAsDefault() },
                    )
                    FilledTonalButton(
                        onClick = { showTargetSheet = true },
                        enabled = !isApplying,
                        modifier = Modifier.padding(start = 12.dp),
                    ) {
                        if (isApplying) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Wallpaper, null)
                        }
                        Text(
                            stringResource(R.string.action_set_wallpaper),
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Live preview — upper 55% of screen
            LivePreview(
                wallpaper = wallpaper,
                editParams = editParams,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.55f),
            )

            // Sliders — lower 45% scrollable
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                AdjustmentSlider(
                    label = stringResource(R.string.editor_blur),
                    value = editParams.blur,
                    valueRange = 0f..25f,
                    onValueChange = viewModel::setBlur,
                    onReset = viewModel::resetBlur,
                    isDefault = editParams.blur == EditParams.Default.blur,
                )
                AdjustmentSlider(
                    label = stringResource(R.string.editor_brightness),
                    value = editParams.brightness,
                    valueRange = -1f..1f,
                    onValueChange = viewModel::setBrightness,
                    onReset = viewModel::resetBrightness,
                    isDefault = editParams.brightness == EditParams.Default.brightness,
                )
                AdjustmentSlider(
                    label = stringResource(R.string.editor_contrast),
                    value = editParams.contrast,
                    valueRange = 0f..2f,
                    onValueChange = viewModel::setContrast,
                    onReset = viewModel::resetContrast,
                    isDefault = editParams.contrast == EditParams.Default.contrast,
                )
                AdjustmentSlider(
                    label = stringResource(R.string.editor_saturation),
                    value = editParams.saturation,
                    valueRange = 0f..2f,
                    onValueChange = viewModel::setSaturation,
                    onReset = viewModel::resetSaturation,
                    isDefault = editParams.saturation == EditParams.Default.saturation,
                )
                // Pan controls
                AdjustmentSlider(
                    label = "Pan X",
                    value = editParams.panX,
                    valueRange = -1f..1f,
                    onValueChange = viewModel::setPanX,
                    onReset = viewModel::resetPan,
                    isDefault = editParams.panX == 0f,
                )
                AdjustmentSlider(
                    label = "Pan Y",
                    value = editParams.panY,
                    valueRange = -1f..1f,
                    onValueChange = viewModel::setPanY,
                    onReset = viewModel::resetPan,
                    isDefault = editParams.panY == 0f,
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Target selection sheet
    if (showTargetSheet) {
        ModalBottomSheet(onDismissRequest = { showTargetSheet = false }) {
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .padding(16.dp),
            ) {
                Text(
                    stringResource(R.string.action_set_wallpaper),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(16.dp))
                listOf(
                    WallpaperTarget.HOME to R.string.action_set_home,
                    WallpaperTarget.LOCK to R.string.action_set_lock,
                    WallpaperTarget.BOTH to R.string.action_set_both,
                ).forEach { (target, labelRes) ->
                    TextButton(
                        onClick = {
                            showTargetSheet = false
                            viewModel.confirmApply(target)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(labelRes))
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Live preview of the wallpaper with [EditParams] applied in real-time using
 * Compose [ColorFilter] for b/c/s and [Modifier.blur] for blur.
 * Pan is applied via translation.
 */
@Composable
private fun LivePreview(
    wallpaper: Wallpaper,
    editParams: EditParams,
    modifier: Modifier = Modifier,
) {
    val colorMatrix = remember(editParams.brightness, editParams.contrast, editParams.saturation) {
        val cm = ImageAdjustments.buildColorMatrix(
            editParams.brightness,
            editParams.contrast,
            editParams.saturation,
        )
        ColorMatrix(cm.array)
    }

    Box(modifier = modifier) {
        AsyncImage(
            model = wallpaper.thumbUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.colorMatrix(colorMatrix),
            modifier = Modifier
                .fillMaxSize()
                // Blur — Modifier.blur works on API 31+ (our minSdk)
                .blur(editParams.blur.dp)
                // Pan — normalized offset clamped to ±50%
                .graphicsLayer {
                    translationX = editParams.panX * size.width * 0.1f
                    translationY = editParams.panY * size.height * 0.1f
                },
        )
    }
}

/** A labeled slider with a reset icon button. */
@Composable
private fun AdjustmentSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onReset: () -> Unit,
    isDefault: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isDefault) FontWeight.Normal else FontWeight.Bold,
            )
            Text(
                text = "%.2f".format(value),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(
                onClick = onReset,
                enabled = !isDefault,
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.editor_reset),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
