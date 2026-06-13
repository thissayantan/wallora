package com.wallora.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.wallora.app.data.local.dao.RecentSearchDao
import com.wallora.app.data.local.entity.RecentSearchEntity
import com.wallora.app.data.repository.SettingsRepository
import com.wallora.app.data.repository.WallpaperRepository
import com.wallora.app.domain.model.SourceId
import com.wallora.app.domain.model.Wallpaper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: WallpaperRepository,
    private val settingsRepo: SettingsRepository,
    private val recentSearchDao: RecentSearchDao,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    val recentSearches = recentSearchDao.observe(20)
        .map { list -> list.map { it.query } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val enabledSources: StateFlow<Set<SourceId>> = settingsRepo.enabledSources
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SourceId.entries.toSet())

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val searchResults: Flow<PagingData<Wallpaper>> = _query
        .debounce(400L)
        .filter { it.length >= 2 }
        .flatMapLatest { q ->
            repository.search(q, enabledSources.value)
        }
        .cachedIn(viewModelScope)

    fun onQueryChange(q: String) { _query.value = q }

    fun setSearchActive(active: Boolean) { _isSearchActive.value = active }

    fun onSearch(q: String) {
        _query.value = q
        if (q.isNotBlank()) {
            viewModelScope.launch {
                recentSearchDao.insert(
                    RecentSearchEntity(query = q, searchedAt = System.currentTimeMillis())
                )
            }
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch { recentSearchDao.deleteAll() }
    }

    fun removeRecentSearch(query: String) {
        viewModelScope.launch { recentSearchDao.delete(query) }
    }
}
