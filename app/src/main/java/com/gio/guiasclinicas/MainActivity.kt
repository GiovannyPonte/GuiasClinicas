@file:OptIn(ExperimentalMaterial3Api::class)

package com.gio.guiasclinicas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gio.guiasclinicas.ui.components.ChapterContentView
import com.gio.guiasclinicas.ui.components.ClinicalGuidesMenuTopBar
import com.gio.guiasclinicas.ui.state.ChapterUiState
import com.gio.guiasclinicas.ui.state.GuideDetailUiState
import com.gio.guiasclinicas.ui.theme.GuiasClinicasTheme
import com.gio.guiasclinicas.ui.viewmodel.GuidesViewModel
import kotlinx.coroutines.launch
import com.gio.guiasclinicas.data.model.ChapterEntry


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
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val detailState by vm.detailState.collectAsStateWithLifecycle()
    val chapterState by vm.chapterState.collectAsStateWithLifecycle()

    LaunchedEffect(detailState) {
        if (detailState is GuideDetailUiState.Ready) {
            drawerState.open()
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
                        st.chapters.forEach { chapter ->
                            NavigationDrawerItem(
                                label = { Text(chapter.title) },
                                selected = false,
                                onClick = {
                                    vm.selectChapter(chapter.slug)
                                    scope.launch { drawerState.close() }
                                }
                            )
                        }
                    }
                    is GuideDetailUiState.Loading -> {
                        Text("Cargando capítulos...", modifier = Modifier.padding(16.dp))
                    }
                    is GuideDetailUiState.Error -> {
                        Text("Error: ${st.message}", modifier = Modifier.padding(16.dp))
                    }
                    GuideDetailUiState.Idle -> {}
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                ClinicalGuidesMenuTopBar(
                    onGuideSelected = { slug ->
                        vm.selectGuide(slug)
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favoritos") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Ajustes") }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.TopStart
            ) {
                when (val st = chapterState) {
                    is ChapterUiState.Ready -> ChapterContentView(st.content.rawJson)
                    is ChapterUiState.Loading -> Text("Cargando contenido...", modifier = Modifier.padding(16.dp))
                    is ChapterUiState.Error -> Text("Error: ${st.message}", modifier = Modifier.padding(16.dp))
                    ChapterUiState.Idle -> Text("Selecciona una guía y luego un capítulo", modifier = Modifier.padding(16.dp))
                }

                if (detailState is GuideDetailUiState.Ready) {
                    SmallFloatingActionButton(
                        onClick = {
                            scope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(Icons.Filled.Menu, contentDescription = "Abrir menú")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GuidesAppPreview() {
    GuiasClinicasTheme {
        GuidesApp()
    }
}
