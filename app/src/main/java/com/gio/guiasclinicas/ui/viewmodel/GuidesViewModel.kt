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
import com.gio.guiasclinicas.ui.components.image.ImageMemoryCache

// 👇 añadidos por la fusión Codex (búsqueda global)
import com.gio.guiasclinicas.ui.search.ScopedSearchResult
import com.gio.guiasclinicas.ui.search.searchAllGuides

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
            try {
                val root = repo.loadRootManifest()
                _listState.value = GuideListUiState.Success(root.guides)
            } catch (e: Exception) {
                _listState.value = GuideListUiState.Error(e.message ?: "Error")
            }
        }
    }

    fun selectGuide(slug: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _detailState.value = GuideDetailUiState.Loading
            try {
                val ref = repo.findGuideBySlug(slug)
                    ?: throw IllegalArgumentException("Guía no encontrada")
                val manifest = repo.loadGuideManifestByPath(ref.manifestPath)
                val dir = repo.guideDirFromManifestPath(ref.manifestPath)

                _detailState.value = GuideDetailUiState.Ready(
                    guideTitle = manifest.guide.title,
                    guideDir = dir,
                    chapters = manifest.chapters
                )
                _chapterState.value = ChapterUiState.Idle
            } catch (e: Exception) {
                _detailState.value = GuideDetailUiState.Error(e.message ?: "Error")
            }
        }
    }

    fun selectChapter(guideDir: String, chapterPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _chapterState.value = ChapterUiState.Loading
            try {
                // libera bitmaps de capítulos previos
                ImageMemoryCache.clear()

                val content = repo.loadChapterContent(guideDir, chapterPath)
                _chapterState.value = ChapterUiState.Ready(content)
            } catch (e: Exception) {
                _chapterState.value = ChapterUiState.Error(e.message ?: "Error")
            }
        }
    }

    // ✅ Añadido (fusión Codex): búsqueda global suspendida
    suspend fun searchAllGuides(
        query: String,
        ignoreCase: Boolean,
        ignoreAccents: Boolean
    ): List<ScopedSearchResult> {
        // delega a la función helper del paquete ui.search
        return searchAllGuides(
            repo = repo,
            query = query,
            ignoreCase = ignoreCase,
            ignoreAccents = ignoreAccents
        )
    }
}
