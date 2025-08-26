package com.gio.guiasclinicas.ui.search

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import com.gio.guiasclinicas.data.search.*

class SearchStateController {
    private val _ui = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val ui = _ui.asStateFlow()

    private val _nav = MutableSharedFlow<SearchNavEvent>()
    val nav = _nav.asSharedFlow()

    var lastQuery: SearchQuery? = null
        private set
    var lastResults: SearchResult? = null
        private set

    fun setUi(state: SearchUiState) { _ui.value = state }
}
