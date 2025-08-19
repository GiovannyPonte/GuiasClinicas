package com.gio.guiasclinicas.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gio.guiasclinicas.data.repo.GuidesRepository
import com.gio.guiasclinicas.ui.state.ChapterUiState
import com.gio.guiasclinicas.ui.state.GuideDetailUiState
import com.gio.guiasclinicas.ui.state.GuideListUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GuidesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GuidesRepository(app)

    private val _listState = MutableStateFlow<GuideListUiState>(GuideListUiState.Loading)
    val listState: StateFlow<GuideListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow<GuideDetailUiState>(GuideDetailUiState.Idle)
    val detailState: StateFlow<GuideDetailUiState> = _detailState.asStateFlow()

    private val _chapterState = MutableStateFlow<ChapterUiState>(ChapterUiState.Idle)
    val chapterState: StateFlow<ChapterUiState> = _chapterState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.loadRootManifest() }
                .onSuccess { root ->
                    _listState.value = GuideListUiState.Success(root.guides)
                }
                .onFailure { e ->
                    _listState.value = GuideListUiState.Error(e.message ?: "Error")
                }
        }
    }

    fun selectGuide(slug: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _detailState.value = GuideDetailUiState.Loading
            runCatching {
                val ref = repo.findGuideBySlug(slug) ?: error("Guía no encontrada")
                val manifest = repo.loadGuideManifestByPath(ref.manifestPath)
                val dir = repo.guideDirFromManifestPath(ref.manifestPath)
                Triple(manifest.guide.title, dir, manifest.chapters) // List<ChapterEntry>
            }.onSuccess { (title, dir, chapters) ->
                _detailState.value = GuideDetailUiState.Ready(
                    guideTitle = title,
                    guideDir = dir,
                    chapters = chapters
                )
                _chapterState.value = ChapterUiState.Idle
            }.onFailure { e ->
                _detailState.value = GuideDetailUiState.Error(e.message ?: "Error")
            }
        }
    }

    fun selectChapter(guideDir: String, chapterPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _chapterState.value = ChapterUiState.Loading
            runCatching { repo.loadChapterContent(guideDir, chapterPath) }
                .onSuccess { content -> _chapterState.value = ChapterUiState.Ready(content) }
                .onFailure { e -> _chapterState.value = ChapterUiState.Error(e.message ?: "Error") }
        }
    }
}
