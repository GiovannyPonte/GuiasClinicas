package com.gio.guiasclinicas.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.gio.guiasclinicas.data.model.*
import com.gio.guiasclinicas.data.search.SearchHit
import com.gio.guiasclinicas.ui.components.zoom.ZoomResetHost
import com.gio.guiasclinicas.ui.components.zoom.resetZoomOnParentVerticalScroll

/* --- espaciados --- */
private val DefaultSectionSpacing = 12.dp
private val ImageAfterTableSpacing = 20.dp
private val ScreenHorizontalPadding = 16.dp
private val ScreenVerticalPadding = 8.dp
private val ScreenBottomSafePadding = 24.dp

/* --- helpers para mapear ids base/sufijos (#body, #footnote, etc.) --- */
private fun baseId(id: String?): String? = id?.substringBefore('#')
private fun partOf(id: String?): String? = id?.substringAfter('#', "")
    ?.ifEmpty { null }

/** ¿`hitSectionId` (con o sin sufijo) pertenece a esta sección? */
private fun sameSection(section: ChapterSection, index: Int, hitSectionId: String?): Boolean {
    val t = baseId(hitSectionId) ?: return false
    val a = section.id
    val b = "sec-$index"
    val c = "sec-$index-${section::class.simpleName}"
    return (t == a || t == b || t == c)
}

/** Devuelve el índice de la sección en `sections` para un id con o sin sufijo `#...` */
private fun indexOfSectionIdCompat(sections: List<ChapterSection>, targetId: String?): Int {
    if (targetId.isNullOrBlank()) return -1
    val base = baseId(targetId) ?: return -1
    for (i in sections.indices) {
        val s = sections[i]
        val a = s.id
        val b = "sec-$i"
        val c = "sec-$i-${s::class.simpleName}"
        if (base == a || base == b || base == c) return i
    }
    return -1
}

/* ============== Entrada simple (sin búsqueda) ============== */
@Composable
fun ChapterContentView(state: com.gio.guiasclinicas.ui.state.ChapterUiState) {
    when (state) {
        is com.gio.guiasclinicas.ui.state.ChapterUiState.Ready   ->
            ChapterBodyView(sections = state.content.content.sections)

        is com.gio.guiasclinicas.ui.state.ChapterUiState.Loading ->
            Text("Cargando contenido...", modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium)

        is com.gio.guiasclinicas.ui.state.ChapterUiState.Error   ->
            Text("Error: ${state.message}", modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium)

        com.gio.guiasclinicas.ui.state.ChapterUiState.Idle       ->
            Text("Selecciona un capítulo", modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ChapterBodyView(sections: List<ChapterSection>) {
    val scope = rememberCoroutineScope()
    ZoomResetHost {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .resetZoomOnParentVerticalScroll(scope)
                .padding(horizontal = ScreenHorizontalPadding, vertical = ScreenVerticalPadding),
            contentPadding = PaddingValues(bottom = ScreenBottomSafePadding)
        ) {
            itemsIndexed(
                items = sections,
                key = { index, item -> item.id ?: "sec-$index-${item::class.simpleName}" }
            ) { index, section ->
                if (index > 0) {
                    val prev = sections[index - 1]
                    val topSpace = when {
                        prev is TableSection && section is ImageSection -> ImageAfterTableSpacing
                        else -> DefaultSectionSpacing
                    }
                    Spacer(Modifier.height(topSpace))
                }
                RenderSection(section)
            }
        }
    }
}

@Composable
private fun RenderSection(section: ChapterSection) {
    when (section) {
        is TextSection -> {
            section.heading?.let { Text(text = it, style = MaterialTheme.typography.titleMedium) }
            section.body?.let { body ->
                Spacer(Modifier.height(4.dp))
                Text(text = body, style = MaterialTheme.typography.bodyMedium)
            }
            section.footnote?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        is TableSection -> TableSectionView(section)
        is ImageSection -> ImageSectionView(section)
    }
}

/* ============== Versión con búsqueda (resalta y hace scroll) ============== */
@Composable
fun ChapterContentViewWithSearch(
    state: com.gio.guiasclinicas.ui.state.ChapterUiState,
    pendingHit: SearchHit?,          // solo para hacer scroll al llegar
    activeHighlight: SearchHit?,     // el hit activo que debe quedar pintado (verde)
    currentMatchIndex: Int,
    onHitConsumed: () -> Unit,
    chapterHits: List<SearchHit>,

    totalHits: Int,
    currentHitIndex: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit
) {
    when (state) {
        is com.gio.guiasclinicas.ui.state.ChapterUiState.Ready -> {
            val sections = state.content.content.sections
            val listState = rememberLazyListState()

            val tableFootnoteMatches = remember(chapterHits, sections) {
                val map = mutableMapOf<Int, MutableList<IntRange>>()
                chapterHits.forEach { hit ->
                    if (partOf(hit.sectionId) == "footnote") {
                        val idx = indexOfSectionIdCompat(sections, hit.sectionId)
                        if (idx >= 0) {
                            map.getOrPut(idx) { mutableListOf() }.addAll(hit.matchRanges)
                        }
                    }
                }
                map
            }

            // Scroll al llegar un pendingHit (no borra el resaltado)
            LaunchedEffect(pendingHit?.sectionId, state.content.chapter.slug) {
                val idx = indexOfSectionIdCompat(sections, pendingHit?.sectionId)
                if (idx >= 0) listState.animateScrollToItem(idx, -24)
                onHitConsumed()
            }

            Box(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .resetZoomOnParentVerticalScroll(rememberCoroutineScope())
                        .padding(horizontal = ScreenHorizontalPadding, vertical = ScreenVerticalPadding),
                    contentPadding = PaddingValues(bottom = ScreenBottomSafePadding)
                ) {
                    itemsIndexed(
                        items = sections,
                        key = { index, item -> item.id ?: "sec-$index-${item::class.simpleName}" }
                    ) { index, section ->
                        if (index > 0) {
                            val prev = sections[index - 1]
                            val topSpace = when {
                                prev is TableSection && section is ImageSection -> ImageAfterTableSpacing
                                else -> DefaultSectionSpacing
                            }
                            Spacer(Modifier.height(topSpace))
                        }

                        when (section) {
                            is TextSection -> {
                                section.heading?.let {
                                    Text(text = it, style = MaterialTheme.typography.titleMedium)
                                }

                                // BODY
                                section.body?.let { body ->
                                    Spacer(Modifier.height(4.dp))
                                    val isThis = sameSection(section, index, activeHighlight?.sectionId)
                                    val part   = partOf(activeHighlight?.sectionId)
                                    val bodyMatches: List<IntRange> =
                                        if (isThis && (part == null || part == "body"))
                                            activeHighlight?.matchRanges ?: emptyList()
                                        else emptyList()
                                    val focus: IntRange? = bodyMatches.getOrNull(currentMatchIndex)

                                    Text(
                                        text = if (bodyMatches.isEmpty()) AnnotatedString(body)
                                        else buildHighlighted(body, bodyMatches, focus),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                // FOOTNOTE
                                section.footnote?.let { note ->
                                    val isThis = sameSection(section, index, activeHighlight?.sectionId)
                                    val part   = partOf(activeHighlight?.sectionId)
                                    val footMatches: List<IntRange> =
                                        if (isThis && part == "footnote")
                                            activeHighlight?.matchRanges ?: emptyList()
                                        else emptyList()
                                    val footFocus = footMatches.getOrNull(currentMatchIndex)

                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = if (footMatches.isEmpty()) AnnotatedString(note)
                                        else buildHighlighted(note, footMatches, footFocus),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            is TableSection -> {
                                val footMatches = tableFootnoteMatches[index].orEmpty()
                                val isThis = sameSection(section, index, activeHighlight?.sectionId)
                                val part = partOf(activeHighlight?.sectionId)
                                val footFocus =
                                    if (isThis && part == "footnote")
                                        activeHighlight?.matchRanges?.getOrNull(currentMatchIndex)
                                    else null

                                TableSectionView(
                                    section = section,
                                    footnoteMatches = footMatches,
                                    footnoteFocus = footFocus
                                )
                            }

                            is ImageSection -> ImageSectionView(section)
                        }
                    }
                }

                // Overlay navegación (↓)
                if (totalHits > 0 && activeHighlight != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 56.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        SearchNavigatorOverlay(
                            total = totalHits,
                            current = currentHitIndex,
                            onPrev = onPrev,
                            onNext = onNext,
                            onExit = onExit
                        )
                    }
                }
            }
        }

        is com.gio.guiasclinicas.ui.state.ChapterUiState.Loading ->
            Text("Cargando contenido…",
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))

        is com.gio.guiasclinicas.ui.state.ChapterUiState.Error   ->
            Text("Error: ${state.message}",
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))

        com.gio.guiasclinicas.ui.state.ChapterUiState.Idle       ->
            Text("Seleccione un capítulo",
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
    }
}

/* --- Overlay compacto para navegar entre coincidencias --- */
@Composable
private fun SearchNavigatorOverlay(
    total: Int,
    current: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.padding(12.dp).wrapContentSize()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("${current + 1}/$total", style = MaterialTheme.typography.labelMedium)
            FilledTonalIconButton(onClick = onPrev) {
                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Anterior")
            }
            FilledTonalIconButton(onClick = onNext) {
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Siguiente")
            }
            Spacer(Modifier.width(4.dp))
            TextButton(onClick = onExit) { Text("✕ Cerrar") }
        }
    }
}
