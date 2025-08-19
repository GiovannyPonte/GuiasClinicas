package com.gio.guiasclinicas.ui.state

import com.gio.guiasclinicas.data.model.ChapterContent

sealed interface ChapterUiState {
    data object Idle : ChapterUiState
    data object Loading : ChapterUiState
    data class Ready(val content: ChapterContent) : ChapterUiState
    data class Error(val message: String) : ChapterUiState
}
