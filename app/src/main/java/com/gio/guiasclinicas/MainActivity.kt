@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.gio.guiasclinicas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gio.guiasclinicas.ui.components.ChapterContentView
import com.gio.guiasclinicas.ui.components.ClinicalGuidesMenuTopBar
import com.gio.guiasclinicas.ui.state.GuideDetailUiState
import com.gio.guiasclinicas.ui.theme.AppDesignSystem
import com.gio.guiasclinicas.ui.theme.Brand
import com.gio.guiasclinicas.ui.theme.DesignConfig
import com.gio.guiasclinicas.ui.theme.ShapeStyle
import com.gio.guiasclinicas.ui.theme.Typo
import com.gio.guiasclinicas.ui.viewmodel.GuidesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.gio.guiasclinicas.R

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppDesignSystem(
                config = DesignConfig(
                    brand = Brand.AtriaMed,
                    typo = Typo.Inter,
                    shapes = ShapeStyle.Mixto,
                    useDynamicColor = false
                )
            ) {
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

    val currentTitle = rememberSaveable { mutableStateOf("Guías Clínicas") }
    fun topBarTitle(): String =
        if (currentTitle.value.isNotBlank() && currentTitle.value != "Guías Clínicas") {
            currentTitle.value
        } else {
            when (val st = detailState) {
                is GuideDetailUiState.Ready -> st.guideTitle
                else -> "Guías Clínicas"
            }
        }

    LaunchedEffect(detailState) {
        when (detailState) {
            is GuideDetailUiState.Ready -> drawerState.open()
            GuideDetailUiState.Idle -> drawerState.close()
            else -> Unit
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = detailState is GuideDetailUiState.Ready,
        drawerContent = {
            ModalDrawerSheet {
                when (val st = detailState) {
                    is GuideDetailUiState.Ready -> DrawerPanelContent(
                        guideTitle   = st.guideTitle,
                        chapters     = st.chapters.map { it.title to chapterPathOf(it) },
                        currentTitle = currentTitle,
                        onChapter = { path ->
                            vm.selectChapter(guideDir = st.guideDir, chapterPath = path)
                            scope.launch { drawerState.close() }
                        }
                    )
                    else -> Text("Selecciona una guía", Modifier.padding(16.dp))
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                ClinicalGuidesMenuTopBar(
                    vm = vm,
                    onGuideSelected = { slug ->
                        vm.selectGuide(slug)
                        scope.launch { drawerState.open() }
                    },
                    showMenuIcon = detailState is GuideDetailUiState.Ready,
                    onMenuClick = {
                        scope.launch {
                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                        }
                    },
                    title = topBarTitle()
                )
            }
        ) { outerPadding ->
            val guideDir = (detailState as? GuideDetailUiState.Ready)?.guideDir ?: ""

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(outerPadding)
            ) {
                ChapterContentView(
                    state = chapterState,
                    guideDir = guideDir
                )
            }
        }
    }
}

/** Contenido del cajón lateral (drawer) */
@Composable
private fun DrawerPanelContent(
    guideTitle: String,
    chapters: List<Pair<String, String>>,
    currentTitle: MutableState<String>,
    onChapter: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme

    val drawerColors = NavigationDrawerItemDefaults.colors(
        selectedContainerColor   = cs.secondaryContainer,
        selectedIconColor        = cs.onSecondaryContainer,
        selectedTextColor        = cs.onSecondaryContainer,
        unselectedIconColor      = cs.onSurfaceVariant,
        unselectedTextColor      = cs.onSurface,
        unselectedContainerColor = Color.Transparent
    )

    Text(
        text = guideTitle,
        style = MaterialTheme.typography.titleMedium,
        color = cs.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
    )
    Divider()
    Text(
        text = "Capítulos",
        style = MaterialTheme.typography.labelLarge,
        color = cs.primary,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
    )

    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        items(items = chapters) { item ->
            val (title, path) = item
            NavigationDrawerItem(
                label = { Text(title, maxLines = 2) },
                icon  = {
                    Icon(
                        painter = painterResource(R.drawable.ic_dictionary_24),
                        contentDescription = null
                    )
                },
                selected = currentTitle.value == title,
                onClick = {
                    currentTitle.value = title
                    onChapter(path)
                },
                colors = drawerColors,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clip(MaterialTheme.shapes.large)
            )
        }

        item {
            Spacer(Modifier.height(8.dp))
            Divider(Modifier.padding(vertical = 8.dp))
            NavigationDrawerItem(
                label = { Text("Tablas") },
                icon  = { Icon(Icons.Outlined.TableChart, null) },
                selected = false,
                onClick = { /* navegar a tablas */ },
                colors = drawerColors,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clip(MaterialTheme.shapes.large)
            )
        }
    }
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
