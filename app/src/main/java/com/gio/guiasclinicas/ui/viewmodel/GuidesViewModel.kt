package com.gio.guiasclinicas.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gio.guiasclinicas.data.model.GuideItem
import com.gio.guiasclinicas.data.repo.GuidesRepository
import com.gio.guiasclinicas.ui.state.GuideListUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GuidesViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow<GuideListUiState>(GuideListUiState.Loading)
    val uiState: StateFlow<GuideListUiState> = _uiState

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val manifest = GuidesRepository.loadRootManifest(getApplication())
                val items = manifest.guides.map { GuideItem(it.slug, it.title) }
                _uiState.value = GuideListUiState.Success(items)
            } catch (e: Exception) {
                _uiState.value = GuideListUiState.Error(e.message ?: "Error cargando guías")
            }
        }
    }
}
