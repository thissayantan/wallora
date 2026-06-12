package com.wallora.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallora.app.data.repository.WallpaperRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: WallpaperRepository,
) : ViewModel() {
    val history = repository.observeHistory()

    fun clearHistory() { viewModelScope.launch { repository.clearHistory() } }
}
