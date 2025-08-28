@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.gio.guiasclinicas

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gio.guiasclinicas.ui.components.ChapterContentView
import com.gio.guiasclinicas.ui.components.ClinicalGuidesMenuTopBar
import com.gio.guiasclinicas.ui.search.ScopedSearchResult
import com.gio.guiasclinicas.ui.search.SearchResult
import com.gio.guiasclinicas.ui.search.searchSections
import com.gio.guiasclinicas.ui.state.ChapterUiState
import com.gio.guiasclinicas.ui.state.GuideDetailUiState
import com.gio.guiasclinicas.ui.theme.GuiasClinicasTheme
import com.gio.guiasclinicas.ui.viewmodel.GuidesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// 🔶 Para el resaltado en previews
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.graphics.Color

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
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val detailState by vm.detailState.collectAsStateWithLifecycle()
    val chapterState by vm.chapterState.collectAsStateWithLifecycle()

    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") } // inicia vacía
    var ignoreCase by remember { mutableStateOf(true) }
    var ignoreAccents by remember { mutableStateOf(true) }

    // Resultados por capítulo (para resaltar dentro del contenido cargado)
    val searchResults = remember { mutableStateListOf<SearchResult>() }
    var currentChapterResultIndex by remember { mutableStateOf(0) }

    // Resultados globales (todas las guías/capítulos)
    val globalResults = remember { mutableStateListOf<ScopedSearchResult>() }
    var currentGlobalIndex by remember { mutableStateOf<Int?>(null) }

    // Hoja modal de búsqueda
    var showSearchSheet by remember { mutableStateOf(false) }
    var usingGlobalNavigation by remember { mutableStateOf(false) }
    val searchSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val context = LocalContext.current
    val searchHistory = remember {
        mutableStateListOf<String>().apply { addAll(loadSearchHistory(context)) }
    }

    // Abre/cierra el drawer según el estado de detalle
    LaunchedEffect(detailState) {
        when (detailState) {
            is GuideDetailUiState.Ready -> drawerState.open()
            GuideDetailUiState.Idle -> drawerState.close()
            else -> Unit
        }
    }

    // Recalcula resultados del capítulo ACTUAL cuando cambia el capítulo o el query
    LaunchedEffect(searchQuery, chapterState, searchVisible, ignoreCase, ignoreAccents) {
        if (searchVisible && chapterState is ChapterUiState.Ready) {
            val sections = (chapterState as ChapterUiState.Ready).content.content.sections
            searchResults.clear()
            searchResults.addAll(
                searchSections(sections, searchQuery, ignoreCase, ignoreAccents)
            )
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

    // Recalcula resultados GLOBALES (todas las guías) cuando cambia el query o visibilidad
    LaunchedEffect(searchQuery, searchVisible, ignoreCase, ignoreAccents) {
        if (searchVisible && searchQuery.isNotBlank()) {
            globalResults.clear()
            val all = vm.searchAllGuides(searchQuery, ignoreCase, ignoreAccents)
            globalResults.addAll(all)
            if (globalResults.isEmpty()) currentGlobalIndex = null
        } else {
            globalResults.clear()
            currentGlobalIndex = null
        }
    }

    // Cierra el sheet si no hay ningún resultado
    LaunchedEffect(searchResults.size, globalResults.size) {
        if (searchResults.isEmpty() && globalResults.isEmpty()) {
            showSearchSheet = false
        }
    }

    // Cuando el usuario selecciona un resultado GLOBAL, navega y posiciona el índice del capítulo
    LaunchedEffect(currentGlobalIndex) {
        val idx = currentGlobalIndex
        if (idx != null) {
            val target = globalResults.getOrNull(idx)
            if (target != null) {
                vm.selectGuide(target.guideSlug)
                vm.selectChapter(target.guideDir, target.chapterPath)
                // Posiciona el resaltado dentro del capítulo
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
                            Text(
                                text = "Esta guía no tiene capítulos definidos.",
                                modifier = Modifier.padding(16.dp)
                            )
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
                    is GuideDetailUiState.Loading -> {
                        Text(text = "Cargando capítulos...", modifier = Modifier.padding(all = 16.dp))
                    }
                    is GuideDetailUiState.Error -> {
                        Text(text = "Error: ${st.message}", modifier = Modifier.padding(all = 16.dp))
                    }
                    GuideDetailUiState.Idle -> {
                        Text(text = "Selecciona una guía", modifier = Modifier.padding(all = 16.dp))
                    }
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
                    NavigationBarItem(
                        selected = searchVisible,
                        onClick = {
                            searchVisible = !searchVisible
                            if (!searchVisible) {
                                showSearchSheet = false
                                usingGlobalNavigation = false
                            } else if (searchQuery.isNotBlank() && (searchResults.isNotEmpty() || globalResults.isNotEmpty())) {
                                showSearchSheet = true
                            }
                        },
                        icon = { androidx.compose.material3.Icon(Icons.Filled.Search, contentDescription = "Buscar") },
                        label = { Text("Buscar") },
                        alwaysShowLabel = false
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { androidx.compose.material3.Icon(Icons.Filled.Favorite, contentDescription = "Favoritos") },
                        label = { Text("Favoritos") },
                        alwaysShowLabel = false
                    )
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
                    ModalBottomSheet(
                        sheetState = searchSheetState,
                        onDismissRequest = { showSearchSheet = false }
                    ) {
                        if (searchResults.isNotEmpty()) {
                            SearchResultsList(
                                results = searchResults,
                                current = currentChapterResultIndex,
                                onResultClick = { idx ->
                                    currentChapterResultIndex = idx
                                    usingGlobalNavigation = false
                                    showSearchSheet = false
                                }
                            )
                        }
                        if (searchResults.isNotEmpty() && globalResults.isNotEmpty()) {
                            Divider()
                        }
                        if (globalResults.isNotEmpty()) {
                            SearchGlobalResultsList(
                                results = globalResults,
                                current = currentGlobalIndex,
                                onResultClick = { idx ->
                                    currentGlobalIndex = idx
                                    usingGlobalNavigation = true
                                    showSearchSheet = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onClose: () -> Unit,
    ignoreCase: Boolean,
    onToggleCase: () -> Unit,
    ignoreAccents: Boolean,
    onToggleAccents: () -> Unit,
    history: List<String>,
    onHistorySelected: (String) -> Unit,
    onRemoveHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            var historyExpanded by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { historyExpanded = !historyExpanded }) {
                    androidx.compose.material3.Icon(Icons.Filled.History, contentDescription = "Historial")
                }
                DropdownMenu(expanded = historyExpanded, onDismissRequest = { historyExpanded = false }) {
                    if (history.isNotEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Borrar historial") },
                            onClick = {
                                onClearHistory()
                                historyExpanded = false
                            },
                            leadingIcon = {
                                androidx.compose.material3.Icon(Icons.Filled.Delete, contentDescription = "Borrar historial")
                            }
                        )
                        Divider()
                    }
                    history.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {
                                onHistorySelected(item)
                                historyExpanded = false
                            },
                            leadingIcon = {
                                IconButton(onClick = { onRemoveHistory(item) }) {
                                    androidx.compose.material3.Icon(Icons.Filled.Clear, contentDescription = "Eliminar")
                                }
                            }
                        )
                    }
                }
            }
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            androidx.compose.material3.Icon(Icons.Filled.Clear, contentDescription = "Borrar texto")
                        }
                    }
                }
            )
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { Text(if (ignoreCase) "Ignorar mayúsculas" else "Distinguir mayúsculas") },
                state = rememberTooltipState()
            ) {
                IconToggleButton(checked = ignoreCase, onCheckedChange = { onToggleCase() }) {
                    androidx.compose.material3.Icon(Icons.Filled.FormatSize, contentDescription = "Mayúsculas")
                }
            }
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { Text(if (ignoreAccents) "Ignorar acentos" else "Distinguir acentos") },
                state = rememberTooltipState()
            ) {
                IconToggleButton(checked = ignoreAccents, onCheckedChange = { onToggleAccents() }) {
                    androidx.compose.material3.Icon(Icons.Filled.Translate, contentDescription = "Acentos")
                }
            }
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { Text("Anterior") },
                state = rememberTooltipState()
            ) {
                IconButton(onClick = onPrev) {
                    androidx.compose.material3.Icon(Icons.Filled.ArrowBack, contentDescription = "Anterior")
                }
            }
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { Text("Siguiente") },
                state = rememberTooltipState()
            ) {
                IconButton(onClick = onNext) {
                    androidx.compose.material3.Icon(Icons.Filled.ArrowForward, contentDescription = "Siguiente")
                }
            }
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { Text("Cancelar") },
                state = rememberTooltipState()
            ) {
                IconButton(onClick = onClose) {
                    androidx.compose.material3.Icon(Icons.Filled.Close, contentDescription = "Cancelar")
                }
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    results: List<SearchResult>,
    current: Int,
    onResultClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(results) { res ->
            val color = if (res.index == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

            // 🔶 Resaltado seguro por rango en preview local
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
                    .clickable { onResultClick(res.index) }
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun SearchGlobalResultsList(
    results: List<ScopedSearchResult>,
    current: Int?,
    onResultClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        itemsIndexed(results) { index, res ->
            val color = if (current == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

            val header = "${res.guideTitle} > ${res.chapterTitle}: "
            // 🔶 Resaltado seguro por rango en preview global (tras el header)
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
                    .clickable { onResultClick(index) }
                    .padding(8.dp)
            )
        }
    }
}

private fun loadSearchHistory(context: Context): MutableList<String> {
    val prefs = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)
    val raw = prefs.getString("entries", "") ?: ""
    return if (raw.isEmpty()) mutableListOf() else raw.split("|").toMutableList()
}

private fun saveSearchHistory(context: Context, history: List<String>) {
    val prefs = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)
    prefs.edit().putString("entries", history.joinToString("|")).apply()
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
