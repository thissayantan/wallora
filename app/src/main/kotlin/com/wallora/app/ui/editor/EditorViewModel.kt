package com.wallora.app.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallora.app.data.repository.SettingsRepository
import com.wallora.app.domain.model.EditParams
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Events emitted to the EditorScreen for snackbar messages and navigation. */
sealed class EditorEvent {
    data class ShowMessage(val message: String) : EditorEvent()
    data object ApplyDone : EditorEvent()
}

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val applyWallpaperUseCase: ApplyWallpaperUseCase,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _wallpaper = MutableStateFlow<Wallpaper?>(null)
    val wallpaper: StateFlow<Wallpaper?> = _wallpaper.asStateFlow()

    private val _editParams = MutableStateFlow(EditParams.Default)
    val editParams: StateFlow<EditParams> = _editParams.asStateFlow()

    /** True while the full-res apply is in progress. */
    private val _isApplying = MutableStateFlow(false)
    val isApplying: StateFlow<Boolean> = _isApplying.asStateFlow()

    /** Whether the current settings should be saved as the global default look. */
    private val _setAsDefault = MutableStateFlow(false)
    val setAsDefault: StateFlow<Boolean> = _setAsDefault.asStateFlow()

    private val _events = MutableSharedFlow<EditorEvent>()
    val events: SharedFlow<EditorEvent> = _events.asSharedFlow()

    fun loadWallpaper(wallpaper: Wallpaper) {
        if (_wallpaper.value?.globalKey == wallpaper.globalKey) return
        _wallpaper.value = wallpaper
    }

    // ── Slider updates ─────────────────────────────────────────────────────────

    fun setBlur(value: Float) = _editParams.update { it.copy(blur = value) }
    fun setBrightness(value: Float) = _editParams.update { it.copy(brightness = value) }
    fun setContrast(value: Float) = _editParams.update { it.copy(contrast = value) }
    fun setSaturation(value: Float) = _editParams.update { it.copy(saturation = value) }
    fun setPanX(value: Float) = _editParams.update { it.copy(panX = value) }
    fun setPanY(value: Float) = _editParams.update { it.copy(panY = value) }

    fun resetBlur() = _editParams.update { it.copy(blur = EditParams.Default.blur) }
    fun resetBrightness() = _editParams.update { it.copy(brightness = EditParams.Default.brightness) }
    fun resetContrast() = _editParams.update { it.copy(contrast = EditParams.Default.contrast) }
    fun resetSaturation() = _editParams.update { it.copy(saturation = EditParams.Default.saturation) }
    fun resetPan() = _editParams.update { it.copy(panX = 0f, panY = 0f) }
    fun resetAll() { _editParams.value = EditParams.Default }

    fun toggleSetAsDefault() = _setAsDefault.update { !it }

    // ── Apply ─────────────────────────────────────────────────────────────────

    fun confirmApply(target: WallpaperTarget) {
        val w = _wallpaper.value ?: return
        if (_isApplying.value) return
        viewModelScope.launch {
            _isApplying.value = true
            val params = _editParams.value

            // Save as default look if requested
            if (_setAsDefault.value) {
                settingsRepository.setDefaultEditParams(params)
            }

            val result = applyWallpaperUseCase(
                wallpaper = w,
                target = target,
                editParams = params,
            )
            _isApplying.value = false

            val message = when (result) {
                is ApplyResult.Success -> "Wallpaper set!"
                is ApplyResult.Failure -> "Failed: ${result.message}"
            }
            _events.emit(EditorEvent.ShowMessage(message))
            if (result is ApplyResult.Success) {
                _events.emit(EditorEvent.ApplyDone)
            }
        }
    }
}
