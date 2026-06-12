package com.wallora.app.ui.detail

import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.wallora.app.R
import com.wallora.app.domain.model.Wallpaper
import com.wallora.app.domain.usecase.WallpaperTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    wallpaper: Wallpaper,
    onBack: () -> Unit,
    onSetWallpaper: (Wallpaper) -> Unit,
    onEditAndSet: (Wallpaper) -> Unit,
    onMoreLikeThis: (Wallpaper) -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isApplying by viewModel.isApplying.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showSetDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(wallpaper) { viewModel.loadWallpaper(wallpaper) }

    // Collect one-shot events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DetailEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    val isFavorite = (uiState as? DetailUiState.Success)?.isFavorite ?: false

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleFavorite) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(
                                if (isFavorite) R.string.action_unfavorite else R.string.action_favorite
                            ),
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.Unspecified,
                        )
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
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    FilledTonalButton(
                        onClick = { showSetDialog = true },
                        enabled = !isApplying,
                    ) {
                        if (isApplying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Default.Wallpaper, null)
                        }
                        Text(
                            stringResource(R.string.action_set_wallpaper),
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                    IconButton(onClick = { onEditAndSet(wallpaper) }) {
                        Icon(
                            Icons.Default.Brush,
                            contentDescription = stringResource(R.string.action_edit_and_set),
                        )
                    }
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, wallpaper.sourcePageUrl)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share wallpaper"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.action_share))
                    }
                    IconButton(onClick = { onMoreLikeThis(wallpaper) }) {
                        Icon(Icons.Default.GridView, contentDescription = stringResource(R.string.action_more_like_this))
                    }
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Full-bleed wallpaper preview
            AsyncImage(
                model = wallpaper.fullUrl,
                contentDescription = wallpaper.author,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )

            // Full-screen progress overlay while applying
            if (isApplying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            // Attribution overlay at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(R.string.detail_resolution, wallpaper.width, wallpaper.height),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.detail_by, wallpaper.author),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                )
                Text(
                    text = stringResource(R.string.detail_on_source, wallpaper.sourceId.displayName),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                )
            }
        }
    }

    // Set wallpaper target selection sheet
    if (showSetDialog) {
        ModalBottomSheet(onDismissRequest = { showSetDialog = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                            showSetDialog = false
                            viewModel.applyWallpaper(target)
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
