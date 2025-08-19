package com.gio.guiasclinicas.ui.state

import com.gio.guiasclinicas.data.model.GuideRef

sealed interface GuideListUiState {
    data object Idle : GuideListUiState
    data object Loading : GuideListUiState
    data class Success(val guides: List<GuideRef>) : GuideListUiState
    data class Error(val message: String) : GuideListUiState
}
