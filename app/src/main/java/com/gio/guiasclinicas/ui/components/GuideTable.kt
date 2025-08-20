package com.gio.guiasclinicas.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gio.guiasclinicas.data.model.ChapterSection
import com.gio.guiasclinicas.ui.components.table.AutoResizeText
import com.gio.guiasclinicas.ui.theme.LocalTableTheme

/**
 * Renderer NO-LAZY para tablas.
 * - Scroll horizontal solo si la tabla es más ancha que el viewport.
 * - Columnas alineadas con pesos fijos por índice.
 * - Texto con wrap y auto-shrink dentro de la celda.
 */
@Composable
fun TableSectionView(section: ChapterSection) {
    val cols = section.columns.orEmpty()
    val rows = section.rows.orEmpty()
    val hScroll = rememberScrollState()
    val theme = LocalTableTheme.current

    // Pesos por columna (ajusta si cambias nº de columnas)
    val colWeights: List<Float> = remember(cols.size) {
        when (cols.size) {
            3 -> listOf(0.40f, 0.30f, 0.30f) // Categoría / PAS / PAD
            2 -> listOf(0.50f, 0.50f)
            else -> List(cols.size) { 1f / cols.size.toFloat() }
        }
    }

    // Anchos mínimos por columna para habilitar scroll cuando no quepan
    val colMinDp: List<Int> = remember(cols.size) {
        when (cols.size) {
            3 -> listOf(140, 120, 120)
            2 -> listOf(140, 140)
            else -> List(cols.size) { 120 }
        }
    }
    val tableMinWidthDp = (colMinDp.sum()).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        // Título
        section.title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Forzar ancho FINITO al contenedor con scroll (usa explícitamente el scope)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val screenWidth: Dp = LocalConfiguration.current.screenWidthDp.dp
                // Usamos el scope de BoxWithConstraints explícitamente --> sin warning
                val maxW: Dp = this@BoxWithConstraints.maxWidth
                val containerWidth: Dp =
                    if (maxW.value.isFinite()) maxW else screenWidth

                Box(
                    modifier = Modifier
                        .width(containerWidth)       // aquí garantizamos ancho FINITO
                        .horizontalScroll(hScroll)
                        .padding(12.dp)
                ) {
                    // Si la tabla es más ancha que containerWidth, aparece el scroll horizontal.
                    Column(
                        modifier = Modifier.widthIn(min = tableMinWidthDp)
                    ) {
                        // === Encabezado ===
                        Row(modifier = Modifier.fillMaxWidth()) {
                            cols.forEachIndexed { index, col ->
                                val weight = colWeights.getOrElse(index) { 1f / cols.size }
                                TableCell(
                                    text = col.label,
                                    isHeader = true,
                                    modifier = Modifier
                                        .weight(weight)
                                        .heightIn(min = theme.cellMinHeightDp.dp)
                                        .padding(horizontal = 4.dp)
                                )
                            }
                        }

                        // === Filas ===
                        var lastGroup: String? = null
                        rows.forEach { r ->
                            if (!r.group.isNullOrBlank() && r.group != lastGroup) {
                                lastGroup = r.group
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp, bottom = 4.dp)
                                ) {
                                    Text(
                                        text = r.group.uppercase(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = theme.groupLabelColor
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                cols.forEachIndexed { idx, col ->
                                    val base = r.cells[col.key].orEmpty()
                                    val show = if (idx == 1 && !r.operator.isNullOrBlank()) {
                                        "$base  ${r.operator}"
                                    } else base

                                    val weight = colWeights.getOrElse(idx) { 1f / cols.size }

                                    TableCell(
                                        text = show,
                                        isHeader = false,
                                        modifier = Modifier
                                            .weight(weight)
                                            .heightIn(min = theme.cellMinHeightDp.dp)
                                            .padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Nota al pie
        section.footnote?.let { note ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TableCell(
    text: String,
    isHeader: Boolean,
    modifier: Modifier = Modifier
) {
    val theme = LocalTableTheme.current
    val border = BorderStroke(theme.borderWidthDp.dp, MaterialTheme.colorScheme.outlineVariant)
    val shape = RoundedCornerShape(6.dp)

    Box(
        modifier = modifier
            .border(border, shape)
            .background(
                color = if (isHeader) theme.headerBg else theme.cellBg,
                shape = shape
            )
            .padding(horizontal = theme.cellPaddingH.dp, vertical = theme.cellPaddingV.dp),
        contentAlignment = if (isHeader) Alignment.Center else Alignment.CenterStart
    ) {
        AutoResizeText(
            text = text,
            style = if (isHeader) MaterialTheme.typography.labelLarge
            else MaterialTheme.typography.bodyMedium,
            maxFontSize = theme.textMaxSp.sp,
            minFontSize = theme.textMinSp.sp,
            maxLines = Int.MAX_VALUE
        )
    }
}
