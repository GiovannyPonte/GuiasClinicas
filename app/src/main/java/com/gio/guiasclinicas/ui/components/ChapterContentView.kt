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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.toMutableStateMap

import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.gio.guiasclinicas.data.model.*
import com.gio.guiasclinicas.ui.search.SearchResult
import com.gio.guiasclinicas.ui.search.SearchPart
import com.gio.guiasclinicas.ui.search.highlightText
import com.gio.guiasclinicas.ui.components.zoom.ZoomResetHost
import com.gio.guiasclinicas.ui.components.zoom.resetZoomOnParentVerticalScroll
import com.gio.guiasclinicas.ui.state.ChapterUiState

// --- Espaciados consistentes para toda la pantalla ---
private val DefaultSectionSpacing = 12.dp
private val ImageAfterTableSpacing = 20.dp     // más aire Tabla -> Imagen
private val ScreenHorizontalPadding = 16.dp
private val ScreenVerticalPadding = 8.dp
private val ScreenBottomSafePadding = 24.dp    // que no choque con el bottom bar

@Composable
fun ChapterContentView(
    state: ChapterUiState,
    searchResults: List<SearchResult> = emptyList(),
    currentResult: Int = -1
) {
    when (state) {
        is ChapterUiState.Ready ->
            ChapterBodyView(
                sections = state.content.content.sections,
                searchResults = searchResults,
                currentResult = currentResult
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
    searchResults: List<SearchResult>,
    currentResult: Int
) {
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
    val sectionIndexMap = remember(sections) {
        sections.mapIndexed { idx, sec ->
            (sec.id ?: "sec-$idx-${sec::class.simpleName}") to idx
        }.toMap()
    }
    val matchesBySection = remember(searchResults) { searchResults.groupBy { it.sectionKey } }

    LaunchedEffect(currentResult) {
        val target = searchResults.getOrNull(currentResult) ?: return@LaunchedEffect
        val index = sectionIndexMap[target.sectionKey] ?: return@LaunchedEffect
        expandedMap[target.sectionKey] = true
        listState.animateScrollToItem(index)
    }

    ZoomResetHost {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = ScreenHorizontalPadding, vertical = ScreenVerticalPadding)
        ) {
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
                    val matches = matchesBySection[key].orEmpty()

                    if (index > 0) {
                        val prev = sections[index - 1]
                        val topSpace = when {
                            prev is TableSection && section is ImageSection -> ImageAfterTableSpacing
                            else -> DefaultSectionSpacing
                        }
                        Spacer(Modifier.height(topSpace))
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                        onClick = { expandedMap[key] = !expanded }
                    ) {
                        Column {
                            val rawTitle = section.title
                                ?: (section as? TextSection)?.heading
                                ?: (section as? ImageSection)?.caption
                                ?: "Sección ${index + 1}"
                            val headingMatches = matches.filter { it.part == SearchPart.HEADING || it.part == SearchPart.CAPTION }
                            val titleText = highlightText(rawTitle, headingMatches, currentResult)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = titleText,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = if (expanded) "Contraer" else "Expandir"
                                )
                            }
                            if (expanded) {
                                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    RenderSection(section, matches, currentResult)
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
    matches: List<SearchResult>,
    currentIndex: Int
) {
    when (section) {
        is TextSection -> {
            section.body?.let {
                val bodyMatches = matches.filter { m -> m.part == SearchPart.BODY }
                Text(
                    text = highlightText(it, bodyMatches, currentIndex),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            section.footnote?.let {
                Spacer(Modifier.height(6.dp))
                val footMatches = matches.filter { m -> m.part == SearchPart.FOOTNOTE }
                Text(
                    text = highlightText(it, footMatches, currentIndex),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        is TableSection -> {
            TableSectionView(section = section, matches = matches, currentIndex = currentIndex)
        }

        is ImageSection -> {
            val captionMatches = matches.filter { it.part == SearchPart.CAPTION }
            val footMatches = matches.filter { it.part == SearchPart.FOOTNOTE }
            ImageSectionView(
                section = section,
                captionText = section.caption?.let { highlightText(it, captionMatches, currentIndex) },
                footnoteText = section.footnote?.let { highlightText(it, footMatches, currentIndex) }
            )
        }
    }
}
