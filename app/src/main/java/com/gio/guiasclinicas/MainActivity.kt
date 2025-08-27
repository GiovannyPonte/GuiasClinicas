@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.gio.guiasclinicas

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
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.material3.SheetValue

import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gio.guiasclinicas.ui.components.ClinicalGuidesMenuTopBar
import com.gio.guiasclinicas.ui.components.ChapterContentView
import com.gio.guiasclinicas.ui.search.SearchResult
import com.gio.guiasclinicas.ui.search.searchSections
import com.gio.guiasclinicas.ui.state.ChapterUiState
import com.gio.guiasclinicas.ui.state.GuideDetailUiState
import com.gio.guiasclinicas.ui.theme.GuiasClinicasTheme
import com.gio.guiasclinicas.ui.viewmodel.GuidesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
    var searchQuery by remember { mutableStateOf("") }
    var ignoreCase by remember { mutableStateOf(true) }
    var ignoreAccents by remember { mutableStateOf(true) }

    val searchResults = remember { mutableStateListOf<SearchResult>() }
    var currentResult by remember { mutableStateOf(0) }
    val searchHistory = remember { mutableStateListOf<String>() }

    // Abre/cierra el drawer según el estado de detalle
    LaunchedEffect(detailState) {
        when (detailState) {
            is GuideDetailUiState.Ready -> drawerState.open()
            GuideDetailUiState.Idle -> drawerState.close()
            else -> Unit
        }
    }

    LaunchedEffect(searchQuery, chapterState, searchVisible, ignoreCase, ignoreAccents) {
        if (searchVisible && chapterState is ChapterUiState.Ready) {
            val sections = (chapterState as ChapterUiState.Ready).content.content.sections
            searchResults.clear()
            searchResults.addAll(
                searchSections(sections, searchQuery, ignoreCase, ignoreAccents)
            )

            currentResult = 0
        } else {
            searchResults.clear()
            currentResult = 0
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
        val bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden,
            skipHiddenState = false
        )
        val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState)

        LaunchedEffect(searchVisible, searchResults.size) {
            if (searchVisible && searchResults.isNotEmpty()) {
                bottomSheetState.partialExpand()
            } else {
                bottomSheetState.hide()
            }
        }

        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = 56.dp,
            sheetContent = {
                SearchResultsList(
                    results = searchResults,
                    current = currentResult,
                    onResultClick = { idx -> currentResult = idx }
                )
            },
            topBar = {
                // Usa el MISMO ViewModel que arriba
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
                        onClick = { searchVisible = !searchVisible },
                        icon = { androidx.compose.material3.Icon(Icons.Filled.Search, contentDescription = "Buscar") }
                    )
                    NavigationBarItem(
                        selected = false, onClick = {},
                        icon = { androidx.compose.material3.Icon(Icons.Filled.Favorite, contentDescription = "Favoritos") }
                    )
                    NavigationBarItem(
                        selected = false, onClick = {},
                        icon = { androidx.compose.material3.Icon(Icons.Filled.Settings, contentDescription = "Ajustes") }
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (searchVisible) {
                    ChapterSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        history = searchHistory,
                        onAddHistory = { q ->
                            if (!searchHistory.contains(q)) searchHistory.add(0, q)
                        },
                        onNext = {
                            if (searchResults.isNotEmpty()) {
                                currentResult = (currentResult + 1) % searchResults.size
                            }
                        },
                        onPrev = {
                            if (searchResults.isNotEmpty()) {
                                currentResult = (currentResult - 1 + searchResults.size) % searchResults.size
                            }
                        },
                        onClose = {
                            searchVisible = false
                            searchResults.clear()
                            currentResult = 0
                        },
                        ignoreCase = ignoreCase,
                        onToggleCase = { ignoreCase = !ignoreCase },
                        ignoreAccents = ignoreAccents,
                        onToggleAccents = { ignoreAccents = !ignoreAccents }
                    )
                }

                // Renderiza el contenido del capítulo (ready/loading/error/idle)
                ChapterContentView(
                    state = chapterState,
                    searchResults = searchResults,
                    currentResult = currentResult
                )
            }
        }
    }
}

@Composable
private fun ChapterSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    history: List<String>,
    onAddHistory: (String) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onClose: () -> Unit,
    ignoreCase: Boolean,
    onToggleCase: () -> Unit,
    ignoreAccents: Boolean,
    onToggleAccents: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxWidth()) {
        var historyExpanded by remember { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                IconButton(onClick = {
                    if (query.isNotBlank()) onAddHistory(query)
                    historyExpanded = !historyExpanded
                }) {
                    androidx.compose.material3.Icon(Icons.Filled.History, contentDescription = "Historial")
                }
                DropdownMenu(expanded = historyExpanded, onDismissRequest = { historyExpanded = false }) {
                    history.forEach { past ->
                        DropdownMenuItem(
                            text = { Text(past) },
                            onClick = {
                                onQueryChange(past)
                                historyExpanded = false
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
                            androidx.compose.material3.Icon(Icons.Filled.Clear, contentDescription = "Borrar")
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
            Text(
                text = res.preview,
                color = color,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onResultClick(res.index) }
                    .padding(8.dp)
            )
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
