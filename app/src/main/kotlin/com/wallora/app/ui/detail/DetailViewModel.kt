package com.wallora.app.ui.detail

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallora.app.data.repository.WallpaperRepository
import com.wallora.app.domain.model.Wallpaper
import com.wallora.app.domain.usecase.ApplyResult
import com.wallora.app.domain.usecase.ApplyWallpaperUseCase
import com.wallora.app.domain.usecase.WallpaperTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DetailUiState {
    data object Loading : DetailUiState()
    data class Success(val wallpaper: Wallpaper, val isFavorite: Boolean) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}

/** One-shot events sent to the UI (snackbar messages). */
sealed class DetailEvent {
    data class ShowMessage(val message: String) : DetailEvent()
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: WallpaperRepository,
    private val applyWallpaperUseCase: ApplyWallpaperUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // wallpaperId is the globalKey passed via nav argument
    private val wallpaperId: String = savedStateHandle["wallpaperId"] ?: ""

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _isApplying = MutableStateFlow(false)
    val isApplying: StateFlow<Boolean> = _isApplying.asStateFlow()

    private val _events = MutableSharedFlow<DetailEvent>()
    val events: SharedFlow<DetailEvent> = _events.asSharedFlow()

    private var currentWallpaper: Wallpaper? = null

    fun loadWallpaper(wallpaper: Wallpaper) {
        currentWallpaper = wallpaper
        viewModelScope.launch {
            repository.observeIsFavorite(wallpaper.globalKey).collect { isFav ->
                _uiState.value = DetailUiState.Success(wallpaper, isFav)
            }
        }
    }

    fun toggleFavorite() {
        val wallpaper = currentWallpaper ?: return
        viewModelScope.launch {
            val current = _uiState.value
            if (current is DetailUiState.Success) {
                if (current.isFavorite) {
                    repository.removeFavorite(wallpaper.globalKey)
                } else {
                    repository.addFavorite(wallpaper)
                }
            }
        }
    }

    fun applyWallpaper(target: WallpaperTarget, editedBitmap: Bitmap? = null) {
        val wallpaper = currentWallpaper ?: return
        if (_isApplying.value) return
        viewModelScope.launch {
            _isApplying.value = true
            val result = applyWallpaperUseCase(wallpaper, target, editedBitmap)
            _isApplying.value = false
            val message = when (result) {
                is ApplyResult.Success -> "Wallpaper set successfully"
                is ApplyResult.Failure -> "Failed: ${result.message}"
            }
            _events.emit(DetailEvent.ShowMessage(message))
        }
    }
}
