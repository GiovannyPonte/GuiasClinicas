package com.gio.guiasclinicas.ui.state

import com.gio.guiasclinicas.data.model.GuideItem

sealed interface GuideListUiState {
    data object Loading : GuideListUiState
    data class Success(val guides: List<GuideItem>) : GuideListUiState
    data class Error(val message: String) : GuideListUiState
}


