package com.gio.guiasclinicas.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gio.guiasclinicas.data.repo.GuidesRepository
import com.gio.guiasclinicas.ui.components.image.ImageMemoryCache
import com.gio.guiasclinicas.ui.state.ChapterUiState
import com.gio.guiasclinicas.ui.state.GuideDetailUiState
import com.gio.guiasclinicas.ui.state.GuideListUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// ==== Búsqueda ====
import com.gio.guiasclinicas.data.search.SearchHit
import com.gio.guiasclinicas.data.search.SearchIndexEvent
import com.gio.guiasclinicas.data.search.SearchIndexer
import com.gio.guiasclinicas.data.search.SearchNavEvent
import com.gio.guiasclinicas.data.search.SearchQuery
import com.gio.guiasclinicas.data.search.SearchResult
import com.gio.guiasclinicas.data.search.SearchUiState
import com.gio.guiasclinicas.util.SearchFlags
import com.gio.guiasclinicas.util.normalizeForSearch

// FAVORITOS
import com.gio.guiasclinicas.data.favorites.FavoritesRepository
import com.gio.guiasclinicas.data.favorites.FavoriteChapter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map

// FAVORITOS (pegalo junto a los imports existentes)







private const val TAG = "GuidesVM"

class GuidesViewModel(app: Application) : AndroidViewModel(app) {


    // ---------- Favoritos ----------
    private val favoritesRepo = FavoritesRepository(app)
    val favorites = favoritesRepo.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())


    // ---------- Dependencias ----------
    private val repo    = GuidesRepository(app)
    private val indexer = SearchIndexer(repo)

    // ---------- UI state ----------
    private val _listState    = MutableStateFlow<GuideListUiState>(GuideListUiState.Loading)
    val listState: StateFlow<GuideListUiState> = _listState.asStateFlow()

    private val _detailState  = MutableStateFlow<GuideDetailUiState>(GuideDetailUiState.Idle)
    val detailState: StateFlow<GuideDetailUiState> = _detailState.asStateFlow()

    private val _chapterState = MutableStateFlow<ChapterUiState>(ChapterUiState.Idle)
    val chapterState: StateFlow<ChapterUiState> = _chapterState.asStateFlow()

    private val _searchUi     = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchUi: StateFlow<SearchUiState> = _searchUi.asStateFlow()

    private val _searchNav    = MutableSharedFlow<SearchNavEvent>(extraBufferCapacity = 8)
    val searchNav: SharedFlow<SearchNavEvent> = _searchNav.asSharedFlow()





    // Hit que debe enfocarse al abrir el capítulo (scroll + highlight)
    private val _pendingFocus = MutableStateFlow<com.gio.guiasclinicas.data.search.SearchHit?>(null)
    val pendingFocus = _pendingFocus.asStateFlow()

    // === Highlight persistente y navegación por coincidencias del CAPÍTULO ACTIVO ===
// Hit “activo” (se pinta en VERDE). No se borra al consumir el scroll.
    private val _activeHighlight = MutableStateFlow<SearchHit?>(null)
    val activeHighlight: StateFlow<SearchHit?> = _activeHighlight.asStateFlow()

    // Lista de hits SOLO del capítulo actualmente abierto (para ◀/▶ y n/N)
    private val _chapterHits = MutableStateFlow<List<SearchHit>>(emptyList())
    val chapterHits: StateFlow<List<SearchHit>> = _chapterHits.asStateFlow()

    // Índice del hit activo dentro de chapterHits
    private val _currentChapterHitIndex = MutableStateFlow(0)
    val currentChapterHitIndex: StateFlow<Int> = _currentChapterHitIndex.asStateFlow()


    // ---------- Trackers búsqueda ----------
    private var searchJob: Job? = null
    private var lastQuery: SearchQuery? = null
    private var lastResults: SearchResult? = null
    private var currentHitIndex: Int = 0
    var isSearchMode: Boolean = false
        private set

    // ---------- Track mínimos para “capítulo activo” ----------
    private var currentGuideDir: String? = null
    private var currentGuideTitle: String? = null
    private var currentChapterPath: String? = null


    // --- Historial de búsquedas (LRU hasta 10) ---
    private val _recentQueries = MutableStateFlow<List<String>>(emptyList())
    val recentQueries: StateFlow<List<String>> = _recentQueries.asStateFlow()

    private fun rememberQuery(raw: String) {
        val now = raw.trim()
        if (now.isEmpty()) return
        val cur = _recentQueries.value.toMutableList()
        cur.removeAll { it.equals(now, ignoreCase = true) }
        cur.add(0, now)
        while (cur.size > 10) cur.removeLast()
        _recentQueries.value = cur
    }

    fun clearHistory() { _recentQueries.value = emptyList() }

    fun goToNextHit() {
        val list = _chapterHits.value
        if (list.isEmpty()) return
        val next = (_currentChapterHitIndex.value + 1) % list.size
        _currentChapterHitIndex.value = next
        _activeHighlight.value = list[next]   // pinta VERDE el nuevo
        _pendingFocus.value = list[next]      // y hace scroll puntual
        currentHitIndex = next
        refreshUiCurrentIndex()
    }

    fun goToPrevHit() {
        val list = _chapterHits.value
        if (list.isEmpty()) return
        val prev = if (_currentChapterHitIndex.value - 1 < 0) list.lastIndex else _currentChapterHitIndex.value - 1
        _currentChapterHitIndex.value = prev
        _activeHighlight.value = list[prev]
        _pendingFocus.value = list[prev]
        currentHitIndex = prev
        refreshUiCurrentIndex()
    }




    // ---------- Carga inicial de guías ----------
    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val root = repo.loadRootManifest()
                Log.d(TAG, "Root loaded: ${root.guides.size} guides")
                _listState.value = GuideListUiState.Success(root.guides)
            } catch (e: Exception) {
                _listState.value = GuideListUiState.Error(e.message ?: "Error")
            }
        }
    }

    // ---------- Selección de guía/capítulo ----------
    fun selectGuide(slug: String) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "selectGuide($slug)")
            _detailState.value = GuideDetailUiState.Loading
            try {
                val ref = repo.findGuideBySlug(slug)
                    ?: throw IllegalArgumentException("Guía no encontrada")
                val manifest = repo.loadGuideManifestByPath(ref.manifestPath)
                val dir = repo.guideDirFromManifestPath(ref.manifestPath)

                // Punteros para alcance ACTIVE_CHAPTER
                currentGuideDir = dir
                currentGuideTitle = manifest.guide.title
                currentChapterPath = null

                _detailState.value = GuideDetailUiState.Ready(
                    guideTitle = manifest.guide.title,
                    guideDir   = dir,
                    chapters   = manifest.chapters
                )
                _chapterState.value = ChapterUiState.Idle
            } catch (e: Exception) {
                _detailState.value = GuideDetailUiState.Error(e.message ?: "Error")
            }
        }
    }

    fun selectChapter(guideDir: String, chapterPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "selectChapter(dir=$guideDir, path=$chapterPath)")
            _chapterState.value = ChapterUiState.Loading
            try {
                ImageMemoryCache.clear()
                val content = repo.loadChapterContent(guideDir, chapterPath)
                // Punteros para alcance ACTIVE_CHAPTER
                currentGuideDir = guideDir
                currentChapterPath = chapterPath

                _chapterState.value = ChapterUiState.Ready(content)
            } catch (e: Exception) {
                _chapterState.value = ChapterUiState.Error(e.message ?: "Error")
            }
        }
    }

    fun handleOpenHit(hit: SearchHit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1) Resolver guía y abrir capítulo (igual que tenías)
                var ref = repo.findGuideBySlug(hit.guideSlug)
                if (ref == null) {
                    val root = repo.loadRootManifest()
                    ref = root.guides.firstOrNull { g ->
                        runCatching { repo.loadGuideManifestByPath(g.manifestPath).guide.title }.getOrNull() == hit.guideSlug
                    }
                }

                if (ref != null) {
                    val dir = repo.guideDirFromManifestPath(ref.manifestPath)
                    currentGuideDir = dir
                    currentGuideTitle = hit.guideSlug
                    selectChapter(dir, hit.chapterPath)
                } else {
                    currentGuideDir?.let { selectChapter(it, hit.chapterPath) }
                }

                // 2) Preparar highlight persistente (VERDE) y scroll puntual
                _activeHighlight.value = hit               // ← pinta en verde en la UI
                _pendingFocus.value = hit                  // ← dispara scroll a la sección

                // 3) Construir lista de hits del capítulo para ◀/▶/n/N
                val list = lastResults?.hits?.filter { it.chapterPath == hit.chapterPath }.orEmpty()
                _chapterHits.value = list
                val idx = list.indexOfFirst { it.sectionId == hit.sectionId }.let { if (it >= 0) it else 0 }
                _currentChapterHitIndex.value = idx

                // 4) Index UI opcional (si usas currentHitIndex en SearchUiState, actualízalo)
                currentHitIndex = idx
                refreshUiCurrentIndex()
            } catch (_: Exception) {
                // no cortar experiencia si algún hit aislado falla
            }
        }
    }


    fun consumePendingFocus() {
        _pendingFocus.value = null
    }


    // ======================
    //   GLOBAL por defecto
    // ======================
    fun startSearchAll(
        rawQuery: String,
        flags: SearchFlags = SearchFlags()
    ) {
        Log.d(TAG, "startSearchAll(raw='$rawQuery', flags=$flags)")
        if (rawQuery.isBlank()) return
        _activeHighlight.value = null
        _chapterHits.value = emptyList()
        _currentChapterHitIndex.value = 0
        currentHitIndex = 0
        rememberQuery(rawQuery)

        val norm  = rawQuery.normalizeForSearch(flags).normalized
        val query = SearchQuery(raw = rawQuery, normalized = norm, flags = flags)

        lastQuery = query
        isSearchMode = true
        _searchUi.value = SearchUiState.Indexing(progress = null, message = "Indexando todas las guías…")

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val acc = mutableListOf<SearchHit>()

            indexer.searchEverywhere(query).collectLatest { ev: SearchIndexEvent ->
                when (ev) {
                    is SearchIndexEvent.PartialResults -> {
                        acc.addAll(ev.hits)
                        val totalNow = acc.sumOf { it.matchesCount }
                        _searchUi.value = SearchUiState.Ready(
                            results = SearchResult(acc.toList(), total = totalNow),
                            currentHitIndex = currentHitIndex
                        )
                    }
                    is SearchIndexEvent.Progress -> {
                        val p = if (ev.total == 0) null else ev.done.toFloat() / ev.total
                        _searchUi.value = SearchUiState.Indexing(progress = p, message = "Indexando todas las guías…")
                    }
                    is SearchIndexEvent.Done -> {
                        val totalNow = acc.sumOf { it.matchesCount }
                        lastResults = SearchResult(acc.toList(), total = totalNow)
                        currentHitIndex = 0
                        _searchUi.value = SearchUiState.Ready(results = lastResults!!, currentHitIndex = 0)
                        Log.d(TAG, "Done (GLOBAL): totalCoincidencias=$totalNow")
                    }
                }
            }
        }
    }

    // ======================
    //   LOCAL – buscar en guía abierta
    // ======================
    fun startSearch(
        guideDir: String,
        guideSlug: String,
        rawQuery: String,
        flags: SearchFlags = SearchFlags()
    ) {
        Log.d(TAG, "startSearch(dir=$guideDir, slug=$guideSlug, raw='$rawQuery', flags=$flags)")
        if (rawQuery.isBlank()) return
        _activeHighlight.value = null
        _chapterHits.value = emptyList()
        _currentChapterHitIndex.value = 0
        currentHitIndex = 0
        rememberQuery(rawQuery)

        val norm  = rawQuery.normalizeForSearch(flags).normalized
        val query = SearchQuery(raw = rawQuery, normalized = norm, flags = flags)

        lastQuery = query
        isSearchMode = true
        _searchUi.value = SearchUiState.Indexing(progress = null, message = "Indexando…")

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val acc = mutableListOf<SearchHit>()

            indexer.searchInGuide(
                guideDir = guideDir,
                guideSlug = guideSlug,
                query = query,
                scope = viewModelScope
            ).collectLatest { ev: SearchIndexEvent ->
                when (ev) {
                    is SearchIndexEvent.PartialResults -> {
                        acc.addAll(ev.hits)
                        val totalNow = acc.sumOf { it.matchesCount }
                        _searchUi.value = SearchUiState.Ready(
                            results = SearchResult(acc.toList(), total = totalNow),
                            currentHitIndex = currentHitIndex
                        )
                    }
                    is SearchIndexEvent.Progress -> {
                        val p = if (ev.total == 0) null else ev.done.toFloat() / ev.total
                        _searchUi.value = SearchUiState.Indexing(progress = p, message = "Indexando…")
                    }
                    is SearchIndexEvent.Done -> {
                        val totalNow = acc.sumOf { it.matchesCount }
                        lastResults = SearchResult(acc.toList(), total = totalNow)
                        currentHitIndex = 0
                        _searchUi.value = SearchUiState.Ready(results = lastResults!!, currentHitIndex = 0)
                        Log.d(TAG, "Done (LOCAL): totalCoincidencias=$totalNow")
                    }
                }
            }
        }
    }

    // ======================
    //   CAPÍTULO ACTIVO
    // ======================
    fun startSearchInActiveChapter(
        rawQuery: String,
        flags: SearchFlags = SearchFlags()
    ) {
        Log.d(TAG, "startSearchInActiveChapter(raw='$rawQuery', flags=$flags)")
        if (rawQuery.isBlank()) return
        _activeHighlight.value = null
        _chapterHits.value = emptyList()
        _currentChapterHitIndex.value = 0
        currentHitIndex = 0

        val dir   = currentGuideDir
        val ch    = currentChapterPath
        val title = currentGuideTitle ?: "—"

        if (dir.isNullOrBlank() || ch.isNullOrBlank()) {
            Log.w(TAG, "No hay capítulo activo; ejecuto búsqueda GLOBAL.")
            startSearchAll(rawQuery, flags)
            return
        }

        startSearch(guideDir = dir, guideSlug = title, rawQuery = rawQuery, flags = flags)
    }

    // ---------- Navegación/limpieza búsqueda ----------
    fun openHit(hit: SearchHit) {
        viewModelScope.launch {
            _searchNav.emit(SearchNavEvent.OpenHit(hit))
        }
    }

    fun exitSearchMode() {
        lastQuery = null
        lastResults = null
        isSearchMode = false
        currentHitIndex = 0
        _searchUi.value = SearchUiState.Idle
        searchJob?.cancel()
        searchJob = null

        _activeHighlight.value = null
        _chapterHits.value = emptyList()
        _currentChapterHitIndex.value = 0

    }


    private fun refreshUiCurrentIndex() {
        val st = _searchUi.value
        if (st is com.gio.guiasclinicas.data.search.SearchUiState.Ready) {
            _searchUi.value = st.copy(currentHitIndex = currentHitIndex)
        }
    }


    // Helper privado para obtener la ruta de un capítulo (path / chapterPath / file / manifestPath)
    private fun chapterPathOf(entry: Any): String? {
        val candidates = listOf("path", "chapterPath", "file", "manifestPath")
        for (name in candidates) {
            runCatching {
                val f = entry.javaClass.getDeclaredField(name)
                f.isAccessible = true
                val v = f.get(entry) as? String
                if (!v.isNullOrBlank()) return v
            }
        }
        return null
    }


    fun toggleFavoriteForCurrentChapter() {
        viewModelScope.launch(Dispatchers.IO) {
            val dir = currentGuideDir ?: return@launch
            val path = currentChapterPath ?: return@launch

            // Títulos (seguro desde estado actual; si no, fallback a path)
            val guideTitle = currentGuideTitle ?: "—"
            val chapterTitle = when (val st = detailState.value) {
                is com.gio.guiasclinicas.ui.state.GuideDetailUiState.Ready -> {
                    val match = st.chapters.firstOrNull { chapterPathOf(it) == path }
                    match?.title ?: path
                }
                else -> path
            }

            // Slug del capítulo (para pintar estrella) si está cargado
            val chapterSlug = (chapterState.value as? com.gio.guiasclinicas.ui.state.ChapterUiState.Ready)
                ?.content?.chapter?.slug

            favoritesRepo.toggle(
                guideTitle = guideTitle,
                guideDir = dir,
                chapterTitle = chapterTitle,
                chapterSlug = chapterSlug,
                chapterPath = path
            )
        }
    }

    fun removeFavorite(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { favoritesRepo.removeById(id) }
    }

    fun clearAllFavorites() {
        viewModelScope.launch(Dispatchers.IO) { favoritesRepo.clearAll() }
    }


}
