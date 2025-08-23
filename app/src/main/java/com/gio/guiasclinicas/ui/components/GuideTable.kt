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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gio.guiasclinicas.data.model.TableSection
import com.gio.guiasclinicas.ui.components.table.AutoResizeText
import com.gio.guiasclinicas.ui.theme.LocalTableTheme
import java.text.Normalizer

// ======================================================
// Selector de renderer según variante
// ======================================================

@Composable
fun TableSectionView(section: TableSection) {
    if (section.variant.isRecommendationVariant()) {
        RecommendationTableView(section)
    } else {
        StandardTableSectionView(section)
    }
}

private fun String?.isRecommendationVariant(): Boolean {
    if (this == null) return false
    val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        .lowercase()
    return normalized == "recomendacion"
}

// ======================================================
// Renderer estándar (neutro)
// ======================================================

@Suppress("BoxWithConstraintsScope")
@Composable
private fun StandardTableSectionView(section: TableSection) {
    val cols = section.columns
    val rows = section.rows
    val hScroll = rememberScrollState()
    val theme = LocalTableTheme.current
    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        section.title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(theme.cornerRadiusDp.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                val maxW = this@BoxWithConstraints.maxWidth
                val containerWidth = if (maxW.value.isFinite()) maxW else screenWidth

                // Ancho simple y estable por columna
                val colWidthsDp: List<Dp> = cols.map { 120.dp }
                val colContentWidthPx = colWidthsDp.map { with(density) { it.toPx() }.toInt() }

                Box(
                    modifier = Modifier
                        .width(containerWidth)
                        .horizontalScroll(hScroll)
                        .padding(12.dp)
                ) {
                    Column {
                        // Header
                        Row {
                            cols.forEachIndexed { i, col ->
                                StandardCell(
                                    text = col.label,
                                    isHeader = true,
                                    modifier = Modifier
                                        .width(colWidthsDp[i])
                                        .heightIn(min = theme.cellMinHeightDp.dp),
                                    maxWidthPx = colContentWidthPx[i]
                                )
                            }
                        }
                        // Filas
                        rows.forEach { r ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                cols.forEachIndexed { i, col ->
                                    StandardCell(
                                        text = r.cells[col.key].orEmpty(),
                                        isHeader = false,
                                        modifier = Modifier
                                            .width(colWidthsDp[i])
                                            .heightIn(min = theme.cellMinHeightDp.dp),
                                        maxWidthPx = colContentWidthPx[i]
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

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
private fun StandardCell(
    text: String,
    isHeader: Boolean,
    modifier: Modifier,
    maxWidthPx: Int
) {
    val theme = LocalTableTheme.current
    val shape = RoundedCornerShape(6.dp)
    val bg = if (isHeader) theme.headerBg else theme.cellBg

    Box(
        modifier = modifier
            .border(BorderStroke(theme.borderWidthDp.dp, theme.cellBorder), shape)
            .background(bg, shape)
            .padding(horizontal = theme.cellPaddingH.dp, vertical = theme.cellPaddingV.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        AutoResizeText(
            text = text,
            maxWidthPx = maxWidthPx,
            style = if (isHeader) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
            maxFontSize = theme.textMaxSp.sp,
            minFontSize = theme.textMinSp.sp
        )
    }
}

// ======================================================
// Renderer Recomendaciones (col 0 y 1 coloreadas)
// ======================================================

@Suppress("BoxWithConstraintsScope")
@Composable
private fun RecommendationTableView(section: TableSection) {
    val cols = section.columns
    val rows = section.rows
    val theme = LocalTableTheme.current
    val hScroll = rememberScrollState()
    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        section.title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(theme.cornerRadiusDp.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                val maxW = this@BoxWithConstraints.maxWidth
                val containerWidth = if (maxW.value.isFinite()) maxW else screenWidth

                val colWidthsDp: List<Dp> = cols.map { 120.dp }
                val colContentWidthPx = colWidthsDp.map { with(density) { it.toPx() }.toInt() }

                Box(
                    modifier = Modifier
                        .width(containerWidth)
                        .horizontalScroll(hScroll)
                        .padding(12.dp)
                ) {
                    Column {
                        // Header (sin colores especiales: usa el headerBg neutro)
                        Row {
                            cols.forEachIndexed { i, col ->
                                RecCell(
                                    text = col.label,
                                    isHeader = true,
                                    bg = theme.headerBg,                   // <--- ahora SIEMPRE el neutro
                                    modifier = Modifier
                                        .width(colWidthsDp[i])
                                        .heightIn(min = theme.cellMinHeightDp.dp),
                                    maxWidthPx = colContentWidthPx[i],
                                    centerContent = (i == 0 || i == 1)    // seguimos centrando COR/LOE
                                )
                            }
                        }

                        // Filas con colores para COR/LOE
                        rows.forEach { r ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                cols.forEachIndexed { i, col ->
                                    val bgColor = when (i) {
                                        0 -> theme.recFirstColBg
                                        1 -> theme.recSecondColBg
                                        else -> theme.cellBg
                                    }
                                    RecCell(
                                        text = r.cells[col.key].orEmpty(),
                                        isHeader = false,
                                        bg = bgColor,
                                        modifier = Modifier
                                            .width(colWidthsDp[i])
                                            .heightIn(min = theme.cellMinHeightDp.dp),
                                        maxWidthPx = colContentWidthPx[i],
                                        centerContent = (i == 0 || i == 1)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

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
private fun RecCell(
    text: String,
    isHeader: Boolean,
    bg: Color,
    modifier: Modifier,
    maxWidthPx: Int,
    centerContent: Boolean = false
) {
    val theme = LocalTableTheme.current
    val shape = RoundedCornerShape(6.dp)

    Box(
        modifier = modifier
            .border(BorderStroke(theme.borderWidthDp.dp, theme.cellBorder), shape)
            .background(bg, shape)
            .padding(horizontal = theme.cellPaddingH.dp, vertical = theme.cellPaddingV.dp),
        contentAlignment = if (centerContent) Alignment.Center else Alignment.CenterStart
    ) {
        AutoResizeText(
            text = text,
            maxWidthPx = maxWidthPx,
            style = if (isHeader) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
            maxFontSize = theme.textMaxSp.sp,
            minFontSize = theme.textMinSp.sp
        )
    }
}
