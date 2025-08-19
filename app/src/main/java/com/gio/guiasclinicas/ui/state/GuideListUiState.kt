package com.gio.guiasclinicas.ui.state

import com.gio.guiasclinicas.data.model.GuideItem
import com.gio.guiasclinicas.data.model.ChapterEntry
import com.gio.guiasclinicas.data.model.ChapterContent

sealed interface GuideListUiState {
    data object Loading : GuideListUiState
    data class Success(val guides: List<GuideItem>) : GuideListUiState
    data class Error(val message: String) : GuideListUiState
}

sealed interface GuideDetailUiState {
    data object Idle : GuideDetailUiState
    data object Loading : GuideDetailUiState
    data class Ready(val guideTitle: String, val guideDir: String, val chapters: List<ChapterEntry>) : GuideDetailUiState
    data class Error(val message: String) : GuideDetailUiState
}

sealed interface ChapterUiState {
    data object Idle : ChapterUiState
    data object Loading : ChapterUiState
    data class Ready(val content: ChapterContent) : ChapterUiState
    data class Error(val message: String) : ChapterUiState
}
