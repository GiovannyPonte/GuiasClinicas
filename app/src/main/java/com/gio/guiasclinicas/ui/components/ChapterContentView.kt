package com.gio.guiasclinicas.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gio.guiasclinicas.data.model.*
import com.gio.guiasclinicas.ui.components.workflow.WorkflowSectionView
import com.gio.guiasclinicas.ui.components.zoom.ZoomResetHost
import com.gio.guiasclinicas.ui.components.zoom.resetZoomOnParentVerticalScroll
import com.gio.guiasclinicas.ui.state.ChapterUiState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.sp
import com.gio.guiasclinicas.ui.theme.ChapterTheme
import com.gio.guiasclinicas.ui.theme.LocalChapterTheme
import com.gio.guiasclinicas.ui.theme.LocalChapterSectionTheme
import com.gio.guiasclinicas.ui.theme.ChapterSectionThemeProvider



// --- Espaciados consistentes para toda la pantalla ---
private val DefaultSectionSpacing = 12.dp
private val ImageAfterTableSpacing = 20.dp     // más aire Tabla -> Imagen
private val ScreenHorizontalPadding = 16.dp
private val ScreenVerticalPadding = 8.dp
private val ScreenBottomSafePadding = 24.dp    // que no choque con el bottom bar

@Composable
fun ChapterContentView(
    state: ChapterUiState,
    guideDir: String                         // base de assets de la guía
) {
    when (state) {
        is ChapterUiState.Ready ->
            ChapterBodyView(
                sections = state.content.content.sections,
                guideDir = guideDir
            )

        is ChapterUiState.Loading ->
            Text(
                text = "Cargando contenido...",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )

        is ChapterUiState.Error ->
            Text(
                text = "Error: ${state.message}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )

        ChapterUiState.Idle ->
            Text(
                text = "Seleccione un capítulo",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
    }
}

@Composable
private fun ChapterBodyView(
    sections: List<ChapterSection>,
    guideDir: String
) {
    // Proveemos el tema de sección (usa tus defaults; podrás cambiarlo desde Theme)
    ChapterSectionThemeProvider {
        val secTheme = LocalChapterSectionTheme.current

        val scope = rememberCoroutineScope()
        val expandedMap: SnapshotStateMap<String, Boolean> = rememberSaveable(
            saver = mapSaver<SnapshotStateMap<String, Boolean>>(
                save = { it.toMap() },
                restore = { map: Map<String, Any?> ->
                    map.map { (key, value) -> key to (value as Boolean) }.toMutableStateMap()
                }
            )
        ) {
            mutableStateMapOf<String, Boolean>()
        }

        val listState = rememberLazyListState()

        ZoomResetHost {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = ScreenHorizontalPadding, vertical = ScreenVerticalPadding)
            ) {
                // Botonera expandir/contraer todas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = {
                        sections.forEachIndexed { index, section ->
                            val key = section.id ?: "sec-$index-${section::class.simpleName}"
                            expandedMap[key] = true
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.UnfoldMore,
                            contentDescription = "Desplegar todos"
                        )
                    }
                    IconButton(onClick = {
                        sections.forEachIndexed { index, section ->
                            val key = section.id ?: "sec-$index-${section::class.simpleName}"
                            expandedMap[key] = false
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.UnfoldLess,
                            contentDescription = "Contraer todos"
                        )
                    }
                }

                Spacer(Modifier.height(DefaultSectionSpacing))

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .resetZoomOnParentVerticalScroll(scope),
                    contentPadding = PaddingValues(bottom = ScreenBottomSafePadding)
                ) {
                    itemsIndexed(
                        items = sections,
                        key = { index, item -> item.id ?: "sec-$index-${item::class.simpleName}" }
                    ) { index, section ->
                        val key = section.id ?: "sec-$index-${section::class.simpleName}"
                        val expanded = expandedMap[key] ?: false

                        // Espaciado entre tarjetas
                        if (index > 0) {
                            val prev = sections[index - 1]
                            val topSpace = when {
                                prev is TableSection && section is ImageSection -> ImageAfterTableSpacing
                                else -> DefaultSectionSpacing
                            }
                            Spacer(Modifier.height(topSpace))
                        }

                        // Tarjeta de sección con forma del tema (evita cortes de texto)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                            shape = secTheme.containerShape,
                            onClick = { expandedMap[key] = !expanded }
                        ) {
                            Column {
                                val title = section.title
                                    ?: (section as? TextSection)?.heading
                                    ?: (section as? ImageSection)?.caption
                                    ?: "Sección ${index + 1}"

                                // Header con padding del tema — el top extra evita que
                                // el texto toque el radio y se “corte” visualmente.
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            start = secTheme.headerPadStart.dp,
                                            top = secTheme.headerPadTop.dp,
                                            end = secTheme.headerPadEnd.dp,
                                            bottom = secTheme.headerPadBottom.dp
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                        contentDescription = if (expanded) "Contraer" else "Expandir"
                                    )
                                }

                                if (expanded) {
                                    // Contenido con padding homogéneo del tema
                                    Column(
                                        Modifier.padding(
                                            horizontal = secTheme.containerPadH.dp,
                                            vertical = secTheme.containerPadV.dp
                                        )
                                    ) {
                                        RenderSection(
                                            section = section,
                                            guideDir = guideDir
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


@Composable
private fun RenderSection(
    section: ChapterSection,
    guideDir: String
) {
    ChapterTheme {
        when (section) {
            is TextSection -> {
                val spec = LocalChapterTheme.current
// Contenedor legible con fondo suave + padding – SIN colores hardcode
                androidx.compose.material3.Surface(
                    color = spec.bodyCardBg,
                    contentColor = spec.bodyCardOnBg,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(spec.bodyCornerRadiusDp.dp)
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(
                            horizontal = spec.bodyPaddingH.dp,
                            vertical = spec.bodyPaddingV.dp
                        )
                    ) {
                        section.body?.let { md ->
// Markdown reutilizable (del workflow), con interlineado cómodo
                            val base = MaterialTheme.typography.bodyMedium
                            androidx.compose.material3.ProvideTextStyle(
                                value = base.copy(
                                    lineHeight = spec.bodyLineHeightSp.sp,
                                    fontSize = spec.bodyTextMaxSp.sp
                                )
                            ) {
                                com.gio.guiasclinicas.ui.components.markdown.MarkdownText(md)
                            }
                        }
                    }
                }
                section.footnote?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalChapterTheme.current.footnoteText
                    )
                }
            }


            is WorkflowSection -> {
                WorkflowSectionView(
                    relativePath = "${guideDir.trimEnd('/')}/${section.path}",
                    title = section.title ?: "Evaluación paso a paso",
                    startButtonLabel = section.startButtonLabel ?: "Comenzar evaluación",
                    locale = section.locale ?: "es"
                )
            }

            is TableSection -> {
                // Versión sin search: TableSectionView no recibe matches
                TableSectionView(section = section)
            }

            is ImageSection -> {
                ImageSectionView(
                    section = section,
                    captionText = section.caption?.let { androidx.compose.ui.text.AnnotatedString(it) },
                    footnoteText = section.footnote?.let {
                        androidx.compose.ui.text.AnnotatedString(
                            it
                        )
                    }
                )
            }


            is OrganigramaSection -> {
                // Construye ruta SOLO con datos de los JSON (guideDir + path)
                val rel = guideDir.trimEnd('/') + "/" + section.path.trimStart('/')
                GuidelineDiagramFromAssets(
                    relativePath = rel,
                    locale = section.locale ?: "es",
                    showKeyTablesBelow = section.showKeyTables ?: true,
                    embedded = true
                )
            }
        }
    }
}
