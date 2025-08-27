package com.gio.guiasclinicas.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.rememberCoroutineScope

import androidx.compose.ui.Modifier
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
    val expandedMap = rememberSaveable(
        saver = mapSaver(


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
                Button(onClick = {
                    sections.forEachIndexed { index, section ->
                        val key = section.id ?: "sec-$index-${section::class.simpleName}"
                        expandedMap[key] = true
                    }
                }) {
                    Text("Desplegar todos")
                }
                Button(onClick = {
                    sections.forEachIndexed { index, section ->
                        val key = section.id ?: "sec-$index-${section::class.simpleName}"
                        expandedMap[key] = false
                    }
                }) {
                    Text("Contraer todos")
                }
            }

            Spacer(Modifier.height(DefaultSectionSpacing))

            Row(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .width(160.dp)
                        .padding(end = 8.dp)
                ) {
                    itemsIndexed(sections) { index, section ->
                        val key = section.id ?: "sec-$index-${section::class.simpleName}"
                        val title = section.title
                            ?: (section as? TextSection)?.heading
                            ?: "Sección ${index + 1}"
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val current = expandedMap[key] ?: false
                                    expandedMap[key] = !current
                                }
                                .padding(vertical = 8.dp)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
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

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                val title = section.title
                                    ?: (section as? TextSection)?.heading
                                    ?: "Sección ${index + 1}"
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expandedMap[key] = !expanded }
                                        .padding(16.dp)
                                )
                                AnimatedVisibility(visible = expanded) {
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
}

@Composable
private fun RenderSection(section: ChapterSection) {
    when (section) {
        is TextSection -> {
            section.heading?.let {
                Text(text = it, style = MaterialTheme.typography.titleMedium)
            }
            section.body?.let {
                Spacer(Modifier.height(4.dp))
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
