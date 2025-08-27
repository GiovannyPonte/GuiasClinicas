package com.gio.guiasclinicas.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.runtime.snapshots.SnapshotStateMap

import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.gio.guiasclinicas.data.model.*
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
fun ChapterContentView(state: ChapterUiState) {
    when (state) {
        is ChapterUiState.Ready ->
            ChapterBodyView(sections = state.content.content.sections)

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
private fun ChapterBodyView(sections: List<ChapterSection>) {
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
                            val title = section.title
                                ?: (section as? TextSection)?.heading
                                ?: (section as? ImageSection)?.caption
                                ?: "Sección ${index + 1}"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
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
                                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    RenderSection(section)
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
private fun RenderSection(section: ChapterSection) {
    when (section) {
        is TextSection -> {
            section.body?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }
            section.footnote?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Sin Spacer final: lo gestiona ChapterBodyView para el ritmo visual
        }

        is TableSection -> {
            TableSectionView(section)  // renderer universal de tablas
        }

        is ImageSection -> {
            ImageSectionView(section)  // usa el contenedor zoom/pan y el tema de imagen
        }
    }
}
