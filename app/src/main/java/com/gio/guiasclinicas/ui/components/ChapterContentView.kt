package com.gio.guiasclinicas.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gio.guiasclinicas.data.model.ChapterSection
import com.gio.guiasclinicas.ui.state.ChapterUiState

@Composable
fun ChapterContentView(state: ChapterUiState) {
    when (state) {
        ChapterUiState.Idle -> Text("Selecciona una guía y luego un capítulo", modifier = Modifier.padding(16.dp))
        ChapterUiState.Loading -> Text("Cargando contenido...", modifier = Modifier.padding(16.dp))
        is ChapterUiState.Error -> Text("Error: ${state.message}", modifier = Modifier.padding(16.dp))
        is ChapterUiState.Ready -> {
            val c = state.content
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                item {
                    Text(
                        text = c.chapter.title,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.height(8.dp))
                    Divider()
                    Spacer(Modifier.height(12.dp))
                }

                if (c.content.summary.isNotBlank()) {
                    item {
                        Text("Resumen", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        SelectionContainer {
                            Text(text = c.content.summary, style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }

                items(c.content.sections) { sec ->
                    SectionBlock(sec)
                }
            }
        }
    }
}
@Composable
private fun SectionBlock(section: ChapterSection) {
    if (section.type == "table" && section.columns != null && section.rows != null) {
        // ---- Tabla ----
        TableSectionView(section = section) // <- usa el renderer nuevo
        Spacer(Modifier.height(16.dp))
    } else {
        // ---- Texto ----
        section.heading?.takeIf { it.isNotBlank() }?.let {
            Text(text = it, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
        }
        section.body?.takeIf { it.isNotBlank() }?.let {
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
