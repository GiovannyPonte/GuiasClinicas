package com.gio.guiasclinicas.ui.state

import com.gio.guiasclinicas.data.model.ChapterEntry

sealed interface GuideDetailUiState {
    data object Idle : GuideDetailUiState
    data object Loading : GuideDetailUiState
    data class Ready(
        val guideTitle: String,
        val guideDir: String,
        val chapters: List<ChapterEntry>
    ) : GuideDetailUiState
    data class Error(val message: String) : GuideDetailUiState
}
