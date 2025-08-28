@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.gio.guiasclinicas

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gio.guiasclinicas.ui.components.ChapterContentView
import com.gio.guiasclinicas.ui.components.ClinicalGuidesMenuTopBar
import com.gio.guiasclinicas.ui.components.SearchScreen
import com.gio.guiasclinicas.ui.search.ScopedSearchResult
import com.gio.guiasclinicas.ui.search.SearchResult
import com.gio.guiasclinicas.ui.search.searchSections
import com.gio.guiasclinicas.ui.state.ChapterUiState
import com.gio.guiasclinicas.ui.state.GuideDetailUiState
import com.gio.guiasclinicas.ui.theme.GuiasClinicasTheme
import com.gio.guiasclinicas.ui.viewmodel.GuidesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// Resaltado en previews
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.graphics.Color

private enum class MainScreen { CONTENT, EXPLORE }

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
    val scope: CoroutineScope = rememberCoroutineScope()
    // Fusión: uso posicional para compatibilidad Material3 recientes
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val detailState by vm.detailState.collectAsStateWithLifecycle()
    val chapterState by vm.chapterState.collectAsStateWithLifecycle()

    // Pantalla actual (contenido o explorar guías)
    var currentScreen by remember { mutableStateOf(MainScreen.CONTENT) }

    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") } // inicia vacía
    var ignoreCase by remember { mutableStateOf(true) }
    var ignoreAccents by remember { mutableStateOf(true) }

    // Resultados por capítulo
    val searchResults = remember { mutableStateListOf<SearchResult>() }
    var currentChapterResultIndex by remember { mutableStateOf(0) }

    // Resultados globales
    val globalResults = remember { mutableStateListOf<ScopedSearchResult>() }
    var currentGlobalIndex by remember { mutableStateOf<Int?>(null) }

    // Sheet de búsqueda — parcialmente expandible
    var showSearchSheet by remember { mutableStateOf(false) }
    var usingGlobalNavigation by remember { mutableStateOf(false) }
    val searchSheetState = rememberModalBottomSheetState(
        // initialValue = SheetValue.PartiallyExpanded, // opcional
        skipPartiallyExpanded = false
    )

    // Forzar partialExpand cuando se abre
    LaunchedEffect(showSearchSheet) {
        if (showSearchSheet) searchSheetState.partialExpand()
    }

    val context = LocalContext.current
    val searchHistory = remember {
        mutableStateListOf<String>().apply { addAll(loadSearchHistory(context)) }
    }

    // Drawer: solo si no se vino de navegación global
    LaunchedEffect(detailState) {
        when (detailState) {
            is GuideDetailUiState.Ready -> if (!usingGlobalNavigation) drawerState.open()
            GuideDetailUiState.Idle -> drawerState.close()
            else -> Unit
        }
    }

    // Recalcular resultados del capítulo
    LaunchedEffect(searchQuery, chapterState, searchVisible, ignoreCase, ignoreAccents) {
        if (currentScreen == MainScreen.CONTENT && searchVisible && chapterState is ChapterUiState.Ready) {
            val sections = (chapterState as ChapterUiState.Ready).content.content.sections
            searchResults.clear()
            searchResults.addAll(searchSections(sections, searchQuery, ignoreCase, ignoreAccents))

            if (searchQuery.isNotBlank() &&
                (searchResults.isNotEmpty() || globalResults.isNotEmpty()) &&
                searchQuery !in searchHistory
            ) {
                searchHistory.add(0, searchQuery)
                saveSearchHistory(context, searchHistory)
            }
            currentChapterResultIndex = 0
        } else {
            searchResults.clear()
            currentChapterResultIndex = 0
        }
    }

    // Recalcular resultados globales
    LaunchedEffect(searchQuery, searchVisible, ignoreCase, ignoreAccents, currentScreen) {
        if (currentScreen == MainScreen.CONTENT && searchVisible && searchQuery.isNotBlank()) {
            globalResults.clear()
            val all = vm.searchAllGuides(searchQuery, ignoreCase, ignoreAccents)
            globalResults.addAll(all)
            if (globalResults.isEmpty()) currentGlobalIndex = null
        } else {
            globalResults.clear()
            currentGlobalIndex = null
        }
    }

    // Cerrar sheet si no hay resultados
    LaunchedEffect(searchResults.size, globalResults.size, currentScreen) {
        if (currentScreen != MainScreen.CONTENT || (searchResults.isEmpty() && globalResults.isEmpty())) {
            showSearchSheet = false
        }
    }

    // Selección de resultado global -> navegar y posicionar índice local
    LaunchedEffect(currentGlobalIndex) {
        val idx = currentGlobalIndex
        if (idx != null) {
            val target = globalResults.getOrNull(idx)
            if (target != null) {
                vm.selectGuide(target.guideSlug)
                vm.selectChapter(target.guideDir, target.chapterPath)
                currentChapterResultIndex = target.result.index
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
                            modifier = Modifier.padding(all = 16.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                        val chapters = st.chapters
                        if (chapters.isEmpty()) {
                            Text("Esta guía no tiene capítulos definidos.", Modifier.padding(16.dp))
                        } else {
                            LazyColumn {
                                items(chapters) { chapter ->
                                    NavigationDrawerItem(
                                        label = { Text(chapter.title) },
                                        selected = false,
                                        onClick = {
                                            val cp = chapterPathOf(chapter) // path/chapterPath/file/manifestPath
                                            vm.selectChapter(guideDir = st.guideDir, chapterPath = cp)
                                            scope.launch { drawerState.close() }
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                    is GuideDetailUiState.Loading ->
                        Text("Cargando capítulos...", Modifier.padding(16.dp))
                    is GuideDetailUiState.Error ->
                        Text("Error: ${st.message}", Modifier.padding(16.dp))
                    GuideDetailUiState.Idle ->
                        Text("Selecciona una guía", Modifier.padding(16.dp))
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                ClinicalGuidesMenuTopBar(
                    vm = vm,
                    onGuideSelected = { slug -> vm.selectGuide(slug) },
                    showMenuIcon = detailState is GuideDetailUiState.Ready,
                    onMenuClick = {
                        if (detailState !is GuideDetailUiState.Ready) return@ClinicalGuidesMenuTopBar
                        scope.launch {
                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    // 1) Buscar
                    NavigationBarItem(
                        selected = currentScreen == MainScreen.CONTENT && searchVisible,
                        onClick = {
                            if (currentScreen != MainScreen.CONTENT) {
                                currentScreen = MainScreen.CONTENT
                            }
                            searchVisible = !searchVisible
                            if (!searchVisible) {
                                showSearchSheet = false
                                usingGlobalNavigation = false
                            } else if (searchQuery.isNotBlank() &&
                                (searchResults.isNotEmpty() || globalResults.isNotEmpty())
                            ) {
                                showSearchSheet = true
                            }
                        },
                        icon = { androidx.compose.material3.Icon(Icons.Filled.Search, contentDescription = "Buscar") },
                        label = { Text("Buscar") },
                        alwaysShowLabel = false
                    )
                    // 2) Explorar
                    NavigationBarItem(
                        selected = currentScreen == MainScreen.EXPLORE,
                        onClick = {
                            currentScreen = MainScreen.EXPLORE
                            searchVisible = false
                            showSearchSheet = false
                            usingGlobalNavigation = false
                        },
                        icon = { androidx.compose.material3.Icon(Icons.Filled.History, contentDescription = "Explorar") },
                        label = { Text("Explorar") },
                        alwaysShowLabel = false
                    )
                    // 3) Ajustes
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { androidx.compose.material3.Icon(Icons.Filled.Settings, contentDescription = "Ajustes") },
                        label = { Text("Ajustes") },
                        alwaysShowLabel = false
                    )
                }
            }
        ) { outerPadding ->
            // Contenido principal
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(outerPadding)
            ) {
                when (currentScreen) {
                    MainScreen.EXPLORE -> {
                        SearchScreen(
                            vm = vm,
                            onGuideSelected = { slug ->
                                vm.selectGuide(slug)
                                currentScreen = MainScreen.CONTENT
                            }
                        )
                    }
                    MainScreen.CONTENT -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(1f)) {
                                ChapterContentView(
                                    state = chapterState,
                                    searchResults = searchResults,
                                    currentResult = currentChapterResultIndex
                                )
                            }

                            if (searchVisible) {
                                Column(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                    ChapterSearchBar(
                                        query = searchQuery,
                                        onQueryChange = {
                                            searchQuery = it
                                            usingGlobalNavigation = false
                                            showSearchSheet = it.isNotBlank()
                                        },
                                        onNext = {
                                            if (!usingGlobalNavigation && searchResults.isNotEmpty()) {
                                                currentChapterResultIndex =
                                                    (currentChapterResultIndex + 1) % searchResults.size
                                            } else if (usingGlobalNavigation && globalResults.isNotEmpty()) {
                                                val size = globalResults.size
                                                currentGlobalIndex = when (val c = currentGlobalIndex) {
                                                    null -> 0
                                                    else -> (c + 1) % size
                                                }
                                            } else if (searchResults.isNotEmpty()) {
                                                currentChapterResultIndex =
                                                    (currentChapterResultIndex + 1) % searchResults.size
                                            }
                                        },
                                        onPrev = {
                                            if (!usingGlobalNavigation && searchResults.isNotEmpty()) {
                                                currentChapterResultIndex =
                                                    (currentChapterResultIndex - 1 + searchResults.size) % searchResults.size
                                            } else if (usingGlobalNavigation && globalResults.isNotEmpty()) {
                                                val size = globalResults.size
                                                currentGlobalIndex = when (val c = currentGlobalIndex) {
                                                    null -> size - 1
                                                    else -> (c - 1 + size) % size
                                                }
                                            } else if (searchResults.isNotEmpty()) {
                                                currentChapterResultIndex =
                                                    (currentChapterResultIndex - 1 + searchResults.size) % searchResults.size
                                            }
                                        },
                                        onClose = {
                                            searchVisible = false
                                            searchResults.clear()
                                            globalResults.clear()
                                            currentChapterResultIndex = 0
                                            currentGlobalIndex = null
                                            showSearchSheet = false
                                            usingGlobalNavigation = false
                                        },
                                        ignoreCase = ignoreCase,
                                        onToggleCase = { ignoreCase = !ignoreCase },
                                        ignoreAccents = ignoreAccents,
                                        onToggleAccents = { ignoreAccents = !ignoreAccents },
                                        history = searchHistory,
                                        onHistorySelected = {
                                            searchQuery = it
                                            showSearchSheet = it.isNotBlank()
                                        },
                                        onRemoveHistory = {
                                            searchHistory.remove(it)
                                            saveSearchHistory(context, searchHistory)
                                        },
                                        onClearHistory = {
                                            searchHistory.clear()
                                            saveSearchHistory(context, searchHistory)
                                        }
                                    )
                                }
                            }
                        }

                        // Sheet de resultados (local + global)
                        if (showSearchSheet && (searchResults.isNotEmpty() || globalResults.isNotEmpty())) {
                            val searchBarHeight = 56.dp
                            val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.5f

                            ModalBottomSheet(
                                sheetState = searchSheetState,
                                onDismissRequest = { showSearchSheet = false },
                                scrimColor = Color.Transparent, // Fusión: sin oscurecer fondo
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.5f)
                                    .heightIn(max = maxSheetHeight)
                            ) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = maxSheetHeight)
                                        .navigationBarsPadding()
                                        .padding(bottom = searchBarHeight)
                                ) {
                                    if (searchResults.isNotEmpty()) {
                                        items(searchResults) { res ->
                                            val color =
                                                if (res.index == currentChapterResultIndex)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onSurface

                                            val previewAnn = buildAnnotatedString {
                                                append(res.preview)
                                                val start = res.previewStart
                                                val len = res.length
                                                if (start >= 0 && len > 0 && start + len <= res.preview.length) {
                                                    addStyle(
                                                        SpanStyle(background = Color.Yellow),
                                                        start,
                                                        start + len
                                                    )
                                                }
                                            }

                                            Text(
                                                text = previewAnn,
                                                color = color,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        currentChapterResultIndex = res.index
                                                        usingGlobalNavigation = false
                                                        scope.launch { searchSheetState.hide() }
                                                        showSearchSheet = false
                                                    }
                                                    .padding(8.dp)
                                            )
                                        }
                                    }
                                    if (searchResults.isNotEmpty() && globalResults.isNotEmpty()) {
                                        item { Divider() }
                                    }
                                    if (globalResults.isNotEmpty()) {
                                        itemsIndexed(globalResults) { index, res ->
                                            val color =
                                                if (currentGlobalIndex == index)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onSurface

                                            val header = "${res.guideTitle} > ${res.chapterTitle}: "
                                            val previewAnn = buildAnnotatedString {
                                                append(header)
                                                val startBase = length
                                                append(res.result.preview)
                                                val start = res.result.previewStart
                                                val len = res.result.length
                                                if (start >= 0 && len > 0 && start + len <= res.result.preview.length) {
                                                    addStyle(
                                                        SpanStyle(background = Color.Yellow),
                                                        startBase + start,
                                                        startBase + start + len
                                                    )
                                                }
                                            }

                                            Text(
                                                text = previewAnn,
                                                color = color,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        currentGlobalIndex = index
                                                        usingGlobalNavigation = true
                                                        scope.launch { searchSheetState.hide() }
                                                        showSearchSheet = false
                                                    }
                                                    .padding(8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Helpers: historial y localización de capítulo **/
private fun loadSearchHistory(context: Context): MutableList<String> {
    val prefs = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)
    val raw = prefs.getString("entries", "") ?: ""
    return if (raw.isEmpty()) mutableListOf() else raw.split("|").toMutableList()
}

private fun saveSearchHistory(context: Context, history: List<String>) {
    val prefs = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)
    prefs.edit().putString("entries", history.joinToString("|")).apply()
}

/** Obtiene la ruta de un capítulo intentando nombres comunes: path/chapterPath/file/manifestPath */
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
