package com.gio.guiasclinicas.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gio.guiasclinicas.data.model.*
import com.gio.guiasclinicas.ui.state.ChapterUiState

// --- Espaciados consistentes para toda la pantalla ---
private val DefaultSectionSpacing = 12.dp
private val ImageAfterTableSpacing = 20.dp     // regla especial: más aire Tabla -> Imagen
private val ScreenHorizontalPadding = 16.dp
private val ScreenVerticalPadding = 8.dp
private val ScreenBottomSafePadding = 24.dp    // para que no choque con el bottom bar

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
    val vScroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(vScroll) // <<--- RECUPERA SCROLL VERTICAL
            .padding(horizontal = ScreenHorizontalPadding, vertical = ScreenVerticalPadding)
    ) {
        sections.forEachIndexed { index, section ->
            // Espaciado superior entre secciones
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

        Spacer(Modifier.height(ScreenBottomSafePadding))
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
            // Sin Spacer final: lo gestiona ChapterBodyView para controlar el ritmo visual
        }

        is TableSection -> {
            TableSectionView(section)  // tu renderer horizontal-scroll
        }

        is ImageSection -> {
            ImageSectionView(section)  // el caption ya se maneja dentro del componente
        }
    }
}
