package com.wallora.app.ui.favorites

import androidx.lifecycle.ViewModel
import com.wallora.app.data.repository.WallpaperRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    repository: WallpaperRepository,
) : ViewModel() {
    val favorites = repository.observeFavorites()
}
