package com.gio.guiasclinicas.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gio.guiasclinicas.data.model.GuideItem
import com.gio.guiasclinicas.data.model.ChapterEntry
import com.gio.guiasclinicas.data.repo.GuidesRepository
import com.gio.guiasclinicas.ui.state.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GuidesViewModel(app: Application) : AndroidViewModel(app) {

    private val _listState = MutableStateFlow<GuideListUiState>(GuideListUiState.Loading)
    val listState: StateFlow<GuideListUiState> = _listState

    private val _detailState = MutableStateFlow<GuideDetailUiState>(GuideDetailUiState.Idle)
    val detailState: StateFlow<GuideDetailUiState> = _detailState

    private val _chapterState = MutableStateFlow<ChapterUiState>(ChapterUiState.Idle)
    val chapterState: StateFlow<ChapterUiState> = _chapterState

    init {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val root = GuidesRepository.loadRootManifest(getApplication())
                root.guides.map { GuideItem(it.slug, it.title) }
            }.onSuccess { items ->
                _listState.value = GuideListUiState.Success(items)
            }.onFailure {
                _listState.value = GuideListUiState.Error(it.message ?: "Error cargando guías")
            }
        }
    }

    fun selectGuide(slug: String) {
        _chapterState.value = ChapterUiState.Idle
        _detailState.value = GuideDetailUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val ctx = getApplication<Application>()
                val ref = GuidesRepository.findGuideBySlug(ctx, slug) ?: error("Guía no encontrada")
                val manifest = GuidesRepository.loadGuideManifestByPath(ctx, ref.manifestPath)
                val guideDir = GuidesRepository.guideDirFromManifestPath(ref.manifestPath)
                Triple(manifest.guide.title, guideDir, manifest.chapters) // chapters: List<ChapterEntry>
            }.onSuccess { (title, dir, chapters) ->
                _detailState.value = GuideDetailUiState.Ready(title, dir, chapters)
            }.onFailure {
                _detailState.value = GuideDetailUiState.Error(it.message ?: "Error cargando guía")
            }
        }
    }

    fun selectChapter(chapterSlug: String) {
        val current = _detailState.value
        if (current !is GuideDetailUiState.Ready) return
        _chapterState.value = ChapterUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val ctx = getApplication<Application>()
                val chapter = current.chapters.firstOrNull { it.slug == chapterSlug }
                    ?: error("Capítulo no encontrado")
                GuidesRepository.loadChapterContent(ctx, current.guideDir, chapter.manifestPath)
            }.onSuccess { content ->
                _chapterState.value = ChapterUiState.Ready(content)
            }.onFailure {
                _chapterState.value = ChapterUiState.Error(it.message ?: "Error cargando capítulo")
            }
        }
    }
}
