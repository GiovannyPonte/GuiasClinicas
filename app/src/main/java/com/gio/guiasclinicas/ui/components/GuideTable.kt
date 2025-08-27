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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gio.guiasclinicas.data.model.TableSection
import com.gio.guiasclinicas.ui.components.table.AutoResizeText
import com.gio.guiasclinicas.ui.theme.LocalRecTableTheme
import com.gio.guiasclinicas.ui.theme.LocalTableTheme
import com.gio.guiasclinicas.ui.theme.RecommendationTableTheme
import java.text.Normalizer
import com.gio.guiasclinicas.ui.components.BigTableSectionView
import com.gio.guiasclinicas.ui.components.ShouldUseBigTable
import com.gio.guiasclinicas.ui.search.SearchResult
import com.gio.guiasclinicas.ui.search.highlightText
import com.gio.guiasclinicas.ui.search.SearchPart
import kotlin.math.max


// ======================================================
// Selector de renderer según variante
// ======================================================
@Composable
fun TableSectionView(
    section: TableSection,
    matches: List<SearchResult> = emptyList(),
    currentIndex: Int = -1
) {
    if (section.variant.isRecommendationVariant()) {
        RecommendationTableTheme {
            RecommendationTableView(section)
        }
    } else {
        if (ShouldUseBigTable(section)) {
            BigTableSectionView(section, matches, currentIndex)
        } else {
            StandardTableSectionView(section, matches, currentIndex)
        }
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
// Renderer ESTÁNDAR (universal)
// ======================================================
@Suppress("BoxWithConstraintsScope")
@Composable
private fun StandardTableSectionView(
    section: TableSection,
    matches: List<SearchResult>,
    currentIndex: Int
) {
    val cols = section.columns
    val rows = section.rows
    val theme = LocalTableTheme.current
    val hScroll = rememberScrollState()

    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val gutter = 0.dp

    // Límites razonables (igual filosofía que BigTable)
    val headerMaxLines = 2
    val smallMaxLines = 2
    val collapsedLines = 4   // ⟵ líneas colapsadas para TODAS las celdas (altura de fila)

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
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                val maxW = this@BoxWithConstraints.maxWidth
                val containerWidth = if (maxW.value.isFinite()) maxW else screenWidth
                val containerPx = with(density) { containerWidth.toPx() }

                // 1) Columnas “pequeñas” típicas (códigos, indicadores)
                fun isSmallColumn(label: String, key: String): Boolean {
                    val k = key.lowercase(); val l = label.lowercase()
                    return k == "cor" || k == "loe" || k == "op" || l == "cor" || l == "loe" || l == "op"
                }

                // 2) Medición “natural” por columna + mínimos
                val headerStyle = MaterialTheme.typography.labelLarge.copy(fontSize = theme.textMaxSp.sp)
                val cellStyle   = MaterialTheme.typography.bodyMedium.copy(fontSize = theme.textMaxSp.sp)
                val measurePadPx = with(density) { (theme.cellPaddingH.dp * 2).toPx() }
                val verticalPadPx = with(density) { (theme.cellPaddingV.dp * 2).toPx() }  // ⟵ nuevo (para altura fija)
                val smallMinPx  = with(density) { 40.dp.toPx() }
                val regMinPx    = with(density) { 80.dp.toPx() }

                data class ColMeasure(val naturalPx: Float, val isSmall: Boolean)

                val measures = cols.map { col ->
                    val small = isSmallColumn(col.label, col.key)
                    var maxTextPx = measurer.measure(AnnotatedString(col.label), headerStyle).size.width.toFloat()
                    rows.forEach { r ->
                        val t = r.cells[col.key].orEmpty()
                        val w = measurer.measure(AnnotatedString(t), cellStyle).size.width.toFloat()
                        if (w > maxTextPx) maxTextPx = w
                    }
                    val minPx = if (small) smallMinPx else regMinPx
                    ColMeasure(maxOf(minPx, maxTextPx + measurePadPx), small)
                }

                // 3) Reparto de anchos (igual que tenías)
                val smallIdxs = measures.indices.filter { measures[it].isSmall }
                val largeIdxs = measures.indices.filter { !measures[it].isSmall }
                val widthsPx = FloatArray(cols.size)

                smallIdxs.forEach { i -> widthsPx[i] = measures[i].naturalPx }
                val sumSmall = smallIdxs.sumOf { widthsPx[it].toDouble() }.toFloat()
                val naturalSum = measures.sumOf { it.naturalPx.toDouble() }.toFloat()

                if (naturalSum <= containerPx) {
                    largeIdxs.forEach { i -> widthsPx[i] = measures[i].naturalPx }
                    val extra = containerPx - naturalSum
                    val denom = largeIdxs.sumOf { widthsPx[it].toDouble() }.toFloat()
                        .takeIf { it > 0f } ?: cols.size.toFloat()
                    largeIdxs.forEach { i ->
                        val share = extra * (widthsPx[i] / denom)
                        widthsPx[i] += share
                    }
                } else {
                    val availableForLarge = (containerPx - sumSmall).coerceAtLeast(0f)
                    val baseSum = largeIdxs.sumOf { measures[it].naturalPx.toDouble() }.toFloat().coerceAtLeast(1f)
                    if (availableForLarge > 0f && largeIdxs.isNotEmpty()) {
                        var assigned = 0f
                        largeIdxs.forEach { i ->
                            val target = maxOf(regMinPx, availableForLarge * (measures[i].naturalPx / baseSum))
                            widthsPx[i] = target
                            assigned += target
                        }
                        if (assigned != availableForLarge && assigned > 0f) {
                            val scale = availableForLarge / assigned
                            largeIdxs.forEach { i -> widthsPx[i] *= scale }
                        }
                    } else {
                        largeIdxs.forEach { i -> widthsPx[i] = regMinPx }
                    }
                }

                val colWidthsDp: List<Dp> = widthsPx.map { with(density) { it.toDp() } }
                val contentPadPx = with(density) { (theme.cellPaddingH.dp * 2).toPx() }
                val colContentWidthPx: List<Int> = colWidthsDp.map { wDp ->
                    (with(density) { wDp.toPx() } - contentPadPx).toInt().coerceAtLeast(1)
                }

                // 4) **Alturas UNIFORMES** (misma lógica que BigTable)
                val rowLineHeightSp = max(theme.textMaxSp * 1.25f, theme.textMaxSp + 2f)
                val lineHeightPx = with(density) { rowLineHeightSp.sp.toPx() }
                val rowHeightDp = with(density) { (lineHeightPx * collapsedLines + verticalPadPx).toDp() }
                val headerHeightDp = with(density) { (lineHeightPx * headerMaxLines + verticalPadPx).toDp() }

                val minTableWidth = colWidthsDp.fold(0.dp) { acc, w -> acc + w } + gutter * (cols.size * 2)

                Box(
                    modifier = Modifier
                        .width(containerWidth)
                        .horizontalScroll(hScroll)
                        .padding(12.dp)
                ) {
                    Column(modifier = Modifier.widthIn(min = minTableWidth)) {

                        // HEADER (2 líneas máx., altura fija del header)
                        Row {
                            cols.forEachIndexed { i, col ->
                                ExpandableCell(
                                    text = col.label,
                                    isHeader = true,
                                    contentWidthPx = colContentWidthPx[i],
                                    collapsedMaxLines = headerMaxLines,
                                    lineHeightSp = rowLineHeightSp,
                                    modifier = Modifier
                                        .width(colWidthsDp[i])
                                        .height(headerHeightDp)
                                        .padding(horizontal = gutter)
                                )
                            }
                        }

                        // FILAS (altura fija por fila)
                        var lastGroup: String? = null
                        rows.forEachIndexed { rIndex, r ->
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
                                val rowMatches = matches.filter { it.part == SearchPart.CELL && it.row == rIndex }
                                cols.forEachIndexed { i, col ->
                                    val collapsed = if (isSmallColumn(col.label, col.key)) smallMaxLines else collapsedLines
                                    val cellMatches = rowMatches.filter { it.cellKey == col.key }
                                    ExpandableCell(
                                        text = r.cells[col.key].orEmpty(),
                                        isHeader = false,
                                        contentWidthPx = colContentWidthPx[i],
                                        collapsedMaxLines = collapsed,
                                        lineHeightSp = rowLineHeightSp,
                                        matches = cellMatches,
                                        currentIndex = currentIndex,
                                        modifier = Modifier
                                            .width(colWidthsDp[i])
                                            .height(rowHeightDp)      // ⟵ altura UNIFORME de la fila
                                            .padding(horizontal = gutter)
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
            val footMatches = matches.filter { it.part == SearchPart.FOOTNOTE }
            Text(
                text = highlightText(note, footMatches, currentIndex),
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
    contentWidthPx: Int,
    maxLines: Int,
    modifier: Modifier = Modifier
) {
    val theme = LocalTableTheme.current
    val shape = RoundedCornerShape(6.dp)

    Box(
        modifier = modifier
            .border(BorderStroke(theme.borderWidthDp.dp, theme.cellBorder), shape)
            .background(color = if (isHeader) theme.headerBg else theme.cellBg, shape = shape)
            .padding(horizontal = theme.cellPaddingH.dp, vertical = theme.cellPaddingV.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        AutoResizeText(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            style = if (isHeader) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
            maxFontSize = theme.textMaxSp.sp,
            minFontSize = theme.textMinSp.sp,
            maxLines = maxLines,
            maxWidthPx = contentWidthPx
        )
    }
}

// ======================================================
// Renderer RECOMENDACIONES (con colores + centrado 1/2)
// ======================================================

@Suppress("BoxWithConstraintsScope")
@Composable
private fun RecommendationTableView(section: TableSection) {
    val cols = section.columns
    val rows = section.rows
    val spec = LocalRecTableTheme.current
    val hScroll = rememberScrollState()

    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val gutter = 0.dp

    // Límites razonables
    val headerMaxLines = 2
    val corLoeMaxLines = 2
    val contentMaxLines = 8

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        // Etiqueta y título
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(20.dp)
                    .background(spec.stripeColor, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = section.title ?: "Recomendaciones",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Franja interior a la izquierda
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(spec.stripeColor)
                )

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                    val maxW = this@BoxWithConstraints.maxWidth
                    val containerWidth = if (maxW.value.isFinite()) maxW else screenWidth
                    val containerPx = with(density) { containerWidth.toPx() }

                    // Medición "natural" por columna
                    val headerStyle = MaterialTheme.typography.labelLarge.copy(fontSize = spec.textMaxSp.sp)
                    val cellStyle   = MaterialTheme.typography.bodyMedium.copy(fontSize = spec.textMaxSp.sp)
                    val measurePadPx = with(density) { (spec.cellPaddingH.dp * 2).toPx() }
                    val smallMinPx   = with(density) { 40.dp.toPx() }   // col 0/1
                    val contentMinPx = with(density) { 80.dp.toPx() }   // resto
                    val smallExtraPx = with(density) { 6.dp.toPx() }

                    val natural = cols.mapIndexed { i, col ->
                        var maxTextPx = measurer.measure(AnnotatedString(col.label), headerStyle).size.width.toFloat()
                        rows.forEach { r ->
                            val t = r.cells[col.key].orEmpty()
                            val w = measurer.measure(AnnotatedString(t), cellStyle).size.width.toFloat()
                            if (w > maxTextPx) maxTextPx = w
                        }
                        val minPx = if (i <= 1) smallMinPx else contentMinPx
                        maxOf(minPx, maxTextPx + measurePadPx)
                    }.toMutableList()

                    if (cols.isNotEmpty()) natural[0] = maxOf(smallMinPx, natural[0] + smallExtraPx)
                    if (cols.size > 1)     natural[1] = maxOf(smallMinPx, natural[1] + smallExtraPx)

                    // Resto ocupa TODO el espacio restante (wrap prioritario)
                    val widthsPx = natural.toMutableList()
                    val restIdxs = (2 until cols.size).toList()
                    if (restIdxs.isNotEmpty()) {
                        val fixedSum = (if (cols.isNotEmpty()) widthsPx[0] else 0f) +
                                (if (cols.size > 1) widthsPx[1] else 0f)
                        val available = (containerPx - fixedSum).coerceAtLeast(contentMinPx * restIdxs.size)
                        val restBaseSum = restIdxs.sumOf { widthsPx[it].toDouble() }.toFloat().coerceAtLeast(1f)
                        var assignedSum = 0f
                        restIdxs.forEach { i ->
                            val target = maxOf(contentMinPx, available * (widthsPx[i] / restBaseSum))
                            widthsPx[i] = target
                            assignedSum += target
                        }
                        if (assignedSum != available && assignedSum > 0f) {
                            val scale = available / assignedSum
                            restIdxs.forEach { i -> widthsPx[i] *= scale }
                        }
                    } else {
                        // Solo COR/LOE: llenar el contenedor
                        val naturalSum = widthsPx.sum()
                        if (naturalSum < containerPx) {
                            val extra = containerPx - naturalSum
                            val denom = widthsPx.sum().takeIf { it > 0f } ?: cols.size.toFloat()
                            for (i in widthsPx.indices) {
                                val share = extra * (widthsPx[i] / denom)
                                widthsPx[i] += share
                            }
                        }
                    }

                    val colWidthsDp: List<Dp> = widthsPx.map { with(density) { it.toDp() } }

                    // Ancho interno de contenido para AutoResizeText
                    val contentPadPx = with(density) { (spec.cellPaddingH.dp * 2).toPx() }
                    val colContentWidthPx: List<Int> = colWidthsDp.map { wDp ->
                        (with(density) { wDp.toPx() } - contentPadPx).toInt().coerceAtLeast(1)
                    }

                    val minTableWidth = colWidthsDp.fold(0.dp) { acc, w -> acc + w } + gutter * (cols.size * 2)

                    Box(
                        modifier = Modifier
                            .width(containerWidth)
                            .horizontalScroll(hScroll)
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.widthIn(min = minTableWidth)) {

                            // Header (neutro; centrado 1/2)
                            Row {
                                cols.forEachIndexed { i, col ->
                                    RecCell(
                                        text = col.label,
                                        isHeader = true,
                                        bg = spec.headerBg,
                                        fg = spec.headerOnBg,
                                        contentWidthPx = colContentWidthPx[i],
                                        centerContent = (i == 0 || i == 1),
                                        modifier = Modifier
                                            .width(colWidthsDp[i])
                                            .heightIn(min = spec.cellMinHeightDp.dp)
                                            .padding(horizontal = gutter),
                                        maxLines = headerMaxLines
                                    )
                                }
                            }

                            // Filas
                            rows.forEach { r ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    cols.forEachIndexed { i, col ->
                                        val textCell = r.cells[col.key].orEmpty()
                                        val colored = textCell.isNotBlank() && (i == 0 || i == 1)

                                        val bg = when {
                                            !colored -> spec.cellBg
                                            i == 0 -> spec.corBg
                                            else   -> spec.loeBg
                                        }
                                        val fg = when {
                                            !colored -> spec.cellOnBg
                                            i == 0 -> spec.corOnBg
                                            else   -> spec.loeOnBg
                                        }

                                        RecCell(
                                            text = textCell,
                                            isHeader = false,
                                            bg = bg,
                                            fg = fg,
                                            contentWidthPx = colContentWidthPx[i],
                                            centerContent = (i == 0 || i == 1),
                                            modifier = Modifier
                                                .width(colWidthsDp[i])
                                                .heightIn(min = spec.cellMinHeightDp.dp)
                                                .padding(horizontal = gutter),
                                            maxLines = if (i <= 1) corLoeMaxLines else contentMaxLines
                                        )
                                    }
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
    fg: Color,
    contentWidthPx: Int,
    modifier: Modifier = Modifier,
    centerContent: Boolean = false,
    maxLines: Int = Int.MAX_VALUE
) {
    val spec = LocalRecTableTheme.current
    val shape = RoundedCornerShape(8.dp)

    // Si está vacío, no colorear
    val finalBg = if (text.isBlank()) spec.cellBg else bg
    val finalFg = if (text.isBlank()) spec.cellOnBg else fg

    val baseStyle = if (isHeader)
        MaterialTheme.typography.labelLarge.copy(color = finalFg)
    else
        MaterialTheme.typography.bodyMedium.copy(color = finalFg)

    val style = if (centerContent) baseStyle.copy(textAlign = TextAlign.Center) else baseStyle

    Box(
        modifier = modifier
            .border(BorderStroke(spec.borderWidthDp.dp, spec.cellBorder), shape)
            .background(finalBg, shape)
            .padding(horizontal = spec.cellPaddingH.dp, vertical = spec.cellPaddingV.dp),
        contentAlignment = when {
            centerContent -> Alignment.Center
            isHeader      -> Alignment.Center
            else          -> Alignment.CenterStart
        }
    ) {
        AutoResizeText(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            style = style,
            maxFontSize = spec.textMaxSp.sp,
            minFontSize = spec.textMinSp.sp,
            maxLines = maxLines,
            maxWidthPx = contentWidthPx
        )
    }
}
