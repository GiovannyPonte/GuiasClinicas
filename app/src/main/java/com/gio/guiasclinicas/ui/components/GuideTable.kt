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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gio.guiasclinicas.data.model.ChapterSection
import com.gio.guiasclinicas.ui.components.table.AutoResizeText
import com.gio.guiasclinicas.ui.theme.LocalTableTheme

/**
 * Renderer NO-LAZY para tablas.
 * - Sin espacio entre columnas (gutter = 0.dp).
 * - Columnas con ancho adaptativo al texto más largo (induciendo multilínea razonable).
 * - Columna "op" (y / o) centrada y con padding ultra-compacto para que no aparezca "…".
 */
@Composable
fun TableSectionView(section: ChapterSection) {
    val cols = section.columns.orEmpty()
    val rows = section.rows.orEmpty()
    val hScroll = rememberScrollState()
    val theme = LocalTableTheme.current

    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    // 0) Separación mínima entre columnas -> 0
    val gutter = 0.dp

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
            // Contenedor con ancho finito para scroll horizontal
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                val maxW = this@BoxWithConstraints.maxWidth
                val containerWidth = if (maxW.value.isFinite()) maxW else screenWidth

                // 1) Cálculo de anchos por columna (natural + padding real por columna) con límites
                val headerStyle = MaterialTheme.typography.labelLarge.copy(fontSize = theme.textMaxSp.sp)
                val cellStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = theme.textMaxSp.sp)
                val defaultPadPx = with(density) { (theme.cellPaddingH.dp * 2).toPx() }
                val opPadPx = with(density) { (2.dp * 2).toPx() } // padding ultra-compacto para "op"
                val defaultMinPx = with(density) { 48.dp.toPx() }
                val opMinPx = with(density) { 24.dp.toPx() }
                val containerPx = with(density) { containerWidth.toPx() }

                val capsFraction = when (cols.size) {
                    4 -> listOf(0.42f, 0.26f, 0.06f, 0.26f) // categoría / PAS / op / PAD
                    3 -> listOf(0.40f, 0.30f, 0.30f)
                    2 -> listOf(0.50f, 0.50f)
                    else -> List(cols.size) { 1f / cols.size }
                }
                val capsPx = capsFraction.map { it * containerPx }

                val colWidthsDp: List<Dp> = cols.mapIndexed { idx, col ->
                    val isOp = col.key.equals("op", ignoreCase = true) || col.label.isBlank()
                    val padPx = if (isOp) opPadPx else defaultPadPx

                    var maxTextPx = measurer.measure(
                        text = AnnotatedString(col.label),
                        style = headerStyle
                    ).size.width.toFloat()

                    rows.forEach { r ->
                        val t = r.cells[col.key].orEmpty()
                        val res = measurer.measure(
                            text = AnnotatedString(t),
                            style = cellStyle
                        ).size.width.toFloat()
                        if (res > maxTextPx) maxTextPx = res
                    }

                    val naturalPx = maxTextPx + padPx
                    val cappedPx = kotlin.math.min(naturalPx, capsPx.getOrElse(idx) { containerPx / cols.size })
                    val minPx = if (isOp) opMinPx else defaultMinPx

                    val finalPx = kotlin.math.max(cappedPx, minPx)
                    with(density) { finalPx.toDp() }
                }

                val minTableWidth =
                    colWidthsDp.fold(0.dp) { acc, w -> acc + w } + gutter * (cols.size * 2)

                Box(
                    modifier = Modifier
                        .width(containerWidth) // ancho finito del contenedor scrolleable
                        .horizontalScroll(hScroll)
                        .padding(12.dp)
                ) {
                    Column(modifier = Modifier.widthIn(min = minTableWidth)) {
                        // === Encabezado ===
                        Row {
                            cols.forEachIndexed { index, col ->
                                val cw = colWidthsDp[index]
                                val isOp = col.key.equals("op", ignoreCase = true) || col.label.isBlank()
                                TableCell(
                                    text = col.label,
                                    isHeader = true,
                                    centerContent = isOp,                       // header de "op" centrado
                                    paddingHorizontalOverride = if (isOp) 2.dp else null,
                                    modifier = Modifier
                                        .width(cw)
                                        .heightIn(min = theme.cellMinHeightDp.dp)
                                        .padding(horizontal = gutter)
                                )
                            }
                        }

                        // === Filas ===
                        var lastGroup: String? = null
                        rows.forEach { r ->
                            if (!r.group.isNullOrBlank() && r.group != lastGroup) {
                                lastGroup = r.group
                                Row(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                                    Text(
                                        text = r.group.uppercase(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = theme.groupLabelColor
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                cols.forEachIndexed { idx, col ->
                                    val cw = colWidthsDp[idx]
                                    val isOp = col.key.equals("op", ignoreCase = true) || col.label.isBlank()
                                    val text = r.cells[col.key].orEmpty()
                                    TableCell(
                                        text = text,
                                        isHeader = false,
                                        centerContent = isOp,                   // "y" / "o" centrado
                                        paddingHorizontalOverride = if (isOp) 2.dp else null,
                                        modifier = Modifier
                                            .width(cw)
                                            .heightIn(min = theme.cellMinHeightDp.dp)
                                            .padding(horizontal = gutter)
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
    modifier: Modifier = Modifier,
    centerContent: Boolean = false,
    paddingHorizontalOverride: Dp? = null
) {
    val theme = LocalTableTheme.current
    val shape = RoundedCornerShape(6.dp)
    val hPad = paddingHorizontalOverride ?: theme.cellPaddingH.dp

    Box(
        modifier = modifier
            .border(
                BorderStroke(theme.borderWidthDp.dp, theme.cellBorder),
                shape
            )
            .background(
                color = if (isHeader) theme.headerBg else theme.cellBg,
                shape = shape
            )
            .padding(horizontal = hPad, vertical = theme.cellPaddingV.dp),
        contentAlignment = when {
            isHeader -> Alignment.Center
            centerContent -> Alignment.Center
            else -> Alignment.CenterStart
        }
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
