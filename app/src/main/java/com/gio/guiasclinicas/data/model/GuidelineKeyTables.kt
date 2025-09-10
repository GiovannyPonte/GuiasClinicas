package com.gio.guiasclinicas.data.model

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gio.guiasclinicas.ui.theme.DiagramTheme

/**
 * Renderiza "tablas clave" derivadas del flujo:
 * - Toma todos los nodos con exposeInKey = true (por ej., PERC, Wells, Ginebra…)
 * - Muestra su título y la lista de campos (labels).
 *
 * @param embedded Si es true, se usa en una celda de LazyColumn (sin scroll interno).
 */
@Composable
fun KeyTables(
    flow: GuidelineFlow,
    theme: DiagramTheme,
    embedded: Boolean = false
) {
    // Nodos a exponer como "Key": forms/sections marcadas en el JSON
    val keyNodes = remember(flow) {
        flow.nodes.filter { it.exposeInKey == true }
    }
    if (keyNodes.isEmpty()) return

    val container = if (embedded) Modifier.fillMaxWidth() else Modifier.fillMaxSize()

    Column(
        modifier = container,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        keyNodes.forEach { node ->
            val heading =
                node.title?.best()
                    ?: node.prompt?.best()
                    ?: node.id

            // Título de la sección clave
            Text(
                text = heading,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(4.dp))

            // Lista de ítems (labels de los fields, si existen)
            node.fields.orEmpty().forEach { field ->
                val label = field.label?.best().orEmpty().ifBlank { field.id }
                Text(
                    text = "• $label",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
