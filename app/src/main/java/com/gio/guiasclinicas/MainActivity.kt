@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class // ← para FlowRow
)


package com.gio.guiasclinicas

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gio.guiasclinicas.data.search.SearchNavEvent
import com.gio.guiasclinicas.data.search.SearchScreen
import com.gio.guiasclinicas.ui.components.ChapterContentView
import com.gio.guiasclinicas.ui.components.ChapterContentViewWithSearch
import com.gio.guiasclinicas.ui.components.ClinicalGuidesMenuTopBar
import com.gio.guiasclinicas.ui.state.GuideDetailUiState
import com.gio.guiasclinicas.ui.theme.GuiasClinicasTheme
import com.gio.guiasclinicas.ui.viewmodel.GuidesViewModel
import kotlinx.coroutines.launch

/// MainActivity.kt — imports NUEVOS
import com.gio.guiasclinicas.data.search.SearchUiState
import com.gio.guiasclinicas.ui.state.ChapterUiState
import com.gio.guiasclinicas.data.search.SearchHit
import com.gio.guiasclinicas.ui.components.FavoritesSheet


private const val TAG = "SearchFlow"


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GuiasClinicasTheme {
                GuidesApp()
            }
        }
    }
}

@Composable
fun GuidesApp(vm: GuidesViewModel = viewModel()) {
    val searchUi by vm.searchUi.collectAsStateWithLifecycle()
    val detailState by vm.detailState.collectAsStateWithLifecycle()
    val chapterState by vm.chapterState.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var searchOpen by remember { mutableStateOf(false) }
    var favoritesOpen by remember { mutableStateOf(false) }   // ✅ AQUÍ SÍ

    // Abrir/cerrar drawer según estado de detalle
    LaunchedEffect(detailState) {
        when (detailState) {
            is GuideDetailUiState.Ready -> drawerState.open()
            GuideDetailUiState.Idle     -> drawerState.close()
            else -> Unit
        }
    }

    // Navegación de búsqueda: abrir capítulo del hit
    LaunchedEffect(vm, detailState) {
        vm.searchNav.collect { ev ->
            when (ev) {
                is SearchNavEvent.OpenHit -> {
                    val ready = detailState as? GuideDetailUiState.Ready
                    if (ready == null) {
                        Log.w(TAG, "OpenHit sin guía seleccionada")
                        return@collect
                    }
                    vm.selectChapter(
                        guideDir = ready.guideDir,
                        chapterPath = ev.hit.chapterPath
                    )
                    // si quieres cerrar el sheet al abrir:
                    // searchOpen = false
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = detailState is GuideDetailUiState.Ready,
        drawerContent = {
            ModalDrawerSheet {
                when (val st = detailState) {
                    is GuideDetailUiState.Ready -> {
                        Text(
                            text = st.guideTitle,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                        val chapters = st.chapters
                        if (chapters.isEmpty()) {
                            Text("Esta guía no tiene capítulos definidos.", modifier = Modifier.padding(16.dp))
                        } else {
                            LazyColumn {
                                items(chapters) { chapter ->
                                    NavigationDrawerItem(
                                        label = { Text(chapter.title) },
                                        selected = false,
                                        onClick = {
                                            val cp = chapterPathOf(chapter)
                                            vm.selectChapter(guideDir = st.guideDir, chapterPath = cp)
                                            scope.launch { drawerState.close() }
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                    is GuideDetailUiState.Loading -> Text("Cargando capítulos...", modifier = Modifier.padding(16.dp))
                    is GuideDetailUiState.Error   -> Text("Error: ${st.message}", modifier = Modifier.padding(16.dp))
                    GuideDetailUiState.Idle       -> Text("Selecciona una guía", modifier = Modifier.padding(16.dp))
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                val favs by vm.favorites.collectAsStateWithLifecycle()
                val readyDetail = detailState as? GuideDetailUiState.Ready
                val readyChapter = chapterState as? com.gio.guiasclinicas.ui.state.ChapterUiState.Ready

                val isFav: Boolean = remember(favs, readyDetail, readyChapter) {
                    val dir = readyDetail?.guideDir
                    val slug = readyChapter?.content?.chapter?.slug
                    if (dir == null || slug == null) false
                    else favs.any { it.guideDir == dir && it.chapterSlug == slug }
                }

                ClinicalGuidesMenuTopBar(
                    vm = vm,
                    onGuideSelected = { slug -> vm.selectGuide(slug) },
                    showMenuIcon = detailState is GuideDetailUiState.Ready,
                    onMenuClick = {
                        if (detailState !is GuideDetailUiState.Ready) return@ClinicalGuidesMenuTopBar
                        scope.launch {
                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                        }
                    },

                    onSearchClick = { searchOpen = true },
                    isFavorite = isFav,                                  // ← NUEVO
                    onToggleFavorite = { vm.toggleFavoriteForCurrentChapter() }  // ← NUEVO
                )

            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = false,
                        onClick = { searchOpen = true }, // si quieres también desde bottom
                        icon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { favoritesOpen = true },   // ← ANTES: {}
                        icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favoritos") }
                    )

                    NavigationBarItem(
                        selected = false, onClick = {},
                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Ajustes") }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopStart
            ) {
                val chapterState by vm.chapterState.collectAsStateWithLifecycle()
                val pendingHit   by vm.pendingFocus.collectAsStateWithLifecycle()
                val matchIndex   by vm.currentMatchIndex.collectAsStateWithLifecycle()

                // --- REEMPLAZA SOLO ESTA LLAMADA ---

                // Antes de la llamada:
                val uiReady = (searchUi as? SearchUiState.Ready)
                val chapterSlug = (chapterState as? ChapterUiState.Ready)?.content?.chapter?.slug
                val chapterHits: List<SearchHit> =
                    uiReady?.results?.hits?.filter { it.chapterSlug == chapterSlug } ?: emptyList()

                // Estado de búsqueda para overlay y foco activo
                val uiState by vm.searchUi.collectAsStateWithLifecycle()
                val readyUi = uiState as? SearchUiState.Ready
                val activeHit = readyUi?.results?.hits?.getOrNull(readyUi.currentHitIndex)

// Llama ahora con el nuevo parámetro chapterHits
                ChapterContentViewWithSearch(
                    state = chapterState,
                    pendingHit = pendingHit,                 // solo para hacer scroll una vez
                    activeHighlight = activeHit,             // ← este mantiene el verde
                    activeMatchIndex = matchIndex,
                    onHitConsumed = { vm.consumePendingFocus() },

                    totalHits = readyUi?.results?.hits?.size ?: 0,
                    currentHitIndex = readyUi?.currentHitIndex ?: 0,
                    onPrev = vm::goToPrevHit,
                    onNext = vm::goToNextHit,
                    onExit = vm::exitSearchMode
                )






                // Hoja de búsqueda
                if (searchOpen) {
                    ModalBottomSheet(
                        onDismissRequest = { searchOpen = false; vm.exitSearchMode() },
                        dragHandle = { BottomSheetDefaults.DragHandle() }
                    ) {
                        val ready = detailState as? GuideDetailUiState.Ready
                        val recents by vm.recentQueries.collectAsStateWithLifecycle()
                        val ui by vm.searchUi.collectAsStateWithLifecycle()

                        SearchScreen(
                            ui = ui,
                            recentQueries = recents,
                            onPickRecent = { /* opcional */ },
                            onClearHistory = vm::clearHistory,
                            onQuery = { raw, flags ->
                                if (ready == null) vm.startSearchAll(raw, flags)
                                else vm.startSearch(ready.guideDir, ready.guideTitle, raw, flags)
                            },
                            onOpenHit = { hit ->
                                vm.handleOpenHit(hit)
                                // Cerrar la hoja inmediatamente (queda la navegación flotante)
                                searchOpen = false
                            },
                            onExit = { searchOpen = false; vm.exitSearchMode() }
                        )


                    }



                }
                if (favoritesOpen) {
                    ModalBottomSheet(
                        onDismissRequest = { favoritesOpen = false }
                    ) {
                        val favs by vm.favorites.collectAsStateWithLifecycle()
                        FavoritesSheet(
                            items = favs,
                            onOpen = { f ->
                                vm.selectChapter(guideDir = f.guideDir, chapterPath = f.chapterPath)
                                favoritesOpen = false
                            },
                            onRemoveOne = { f -> vm.removeFavorite(f.id) },
                            onRemoveAll = { vm.clearAllFavorites() }
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }

            }
        }
    }
}

/** Obtiene la ruta de un capítulo intentando nombres comunes: path / chapterPath / file / manifestPath */
private fun chapterPathOf(chapter: Any): String {
    val candidates = listOf("path", "chapterPath", "file", "manifestPath")
    for (name in candidates) {
        runCatching {
            val f = chapter.javaClass.getDeclaredField(name)
            f.isAccessible = true
            val v = f.get(chapter) as? String
            if (!v.isNullOrBlank()) return v
        }
    }
    throw IllegalStateException("No se encontró un campo de ruta en ChapterEntry (path/chapterPath/file/manifestPath).")
}
