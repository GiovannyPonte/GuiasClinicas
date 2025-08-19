package com.gio.guiasclinicas.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gio.guiasclinicas.data.repo.GuidesRepository
import com.gio.guiasclinicas.ui.state.ChapterUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChapterViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GuidesRepository(app)

    private val _uiState = MutableStateFlow<ChapterUiState>(ChapterUiState.Idle)
    val uiState: StateFlow<ChapterUiState> = _uiState.asStateFlow()

    fun load(guideDir: String, chapterPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = ChapterUiState.Loading
            try {
                val content = repo.loadChapterContent(guideDir, chapterPath)
                _uiState.value = ChapterUiState.Ready(content)
            } catch (e: Exception) {
                _uiState.value = ChapterUiState.Error(e.message ?: "Error")
            }
        }
    }
}
