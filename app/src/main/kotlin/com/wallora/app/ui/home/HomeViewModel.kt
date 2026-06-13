package com.wallora.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.wallora.app.data.repository.SettingsRepository
import com.wallora.app.data.repository.WallpaperRepository
import com.wallora.app.domain.model.Category
import com.wallora.app.domain.model.SourceId
import com.wallora.app.domain.model.Wallpaper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: WallpaperRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    // Selected categories (empty = all) — persisted in DataStore so they survive navigation
    // and process death. Single source of truth shared with SettingsViewModel. (D1 fix)
    val selectedCategories: StateFlow<Set<Category>> =
        settingsRepo.selectedCategories
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    // Source filter sheet visible
    private val _filterSheetVisible = kotlinx.coroutines.flow.MutableStateFlow(false)
    val filterSheetVisible: StateFlow<Boolean> = _filterSheetVisible

    // Enabled sources from settings
    val enabledSources: StateFlow<Set<SourceId>> = settingsRepo.enabledSources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SourceId.entries.toSet())

    @OptIn(ExperimentalCoroutinesApi::class)
    val wallpapers: Flow<PagingData<Wallpaper>> =
        combine(selectedCategories, enabledSources) { cats, sources -> cats to sources }
            .flatMapLatest { (cats, sources) ->
                repository.browse(cats.toList(), sources)
            }
            .cachedIn(viewModelScope)

    fun toggleCategory(category: Category) {
        viewModelScope.launch {
            val current = selectedCategories.value.toMutableSet()
            if (category in current) current.remove(category) else current.add(category)
            settingsRepo.setSelectedCategories(current)
        }
    }

    fun clearCategories() {
        viewModelScope.launch { settingsRepo.setSelectedCategories(emptySet()) }
    }

    fun showFilterSheet() { _filterSheetVisible.value = true }
    fun hideFilterSheet() { _filterSheetVisible.value = false }

    fun toggleSource(sourceId: SourceId, enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setSourceEnabled(sourceId, enabled) }
    }
}
