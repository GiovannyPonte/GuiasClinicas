@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.gio.guiasclinicas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gio.guiasclinicas.ui.components.ClinicalGuidesMenuTopBar
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

    val isGuideReady = detailState is GuideDetailUiState.Ready

    LaunchedEffect(isGuideReady) {
        if (isGuideReady) drawerState.open()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isGuideReady,
        drawerContent = {
            ModalDrawerSheet {
                when (val st = detailState) {
                    is GuideDetailUiState.Ready -> {
                        Text(
                            text = st.guideTitle,
                            modifier = Modifier.padding(all = 16.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                        for (chapter in st.chapters) {
                            NavigationDrawerItem(
                                label = { Text(chapter.title) },
                                selected = false,
                                onClick = {
                                    val cp = chapterPathOf(chapter) // <- path/chapterPath/file
                                    vm.selectChapter(guideDir = st.guideDir, chapterPath = cp)
                                    scope.launch { drawerState.close() }
                                }
                            )
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
                    onGuideSelected = { slug -> vm.selectGuide(slug) },
                    showMenuIcon = isGuideReady,
                    onMenuClick = {
                        if (!isGuideReady) return@ClinicalGuidesMenuTopBar
                        scope.launch {
                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = false, onClick = {},
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
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.TopStart
            ) {
                when (val st = chapterState) {
                    is ChapterUiState.Ready -> Text("Capítulo cargado", modifier = Modifier.padding(16.dp))
                    is ChapterUiState.Loading -> Text("Cargando contenido...", modifier = Modifier.padding(16.dp))
                    is ChapterUiState.Error -> Text("Error: ${st.message}", modifier = Modifier.padding(16.dp))
                    ChapterUiState.Idle -> Text("Selecciona una guía y luego un capítulo", modifier = Modifier.padding(16.dp))
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
