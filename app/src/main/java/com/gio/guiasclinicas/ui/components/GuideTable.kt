package com.gio.guiasclinicas.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlin.math.max
import androidx.compose.ui.graphics.Color
import com.gio.guiasclinicas.ui.theme.RecPalettes
import com.gio.guiasclinicas.ui.theme.RecSystem
import com.gio.guiasclinicas.ui.theme.detectCorLoeIndices
import com.gio.guiasclinicas.ui.theme.detectRecSystem



/* ===================== PARÁMETROS ===================== */
private const val ROW_MAX_LINES = 4
/* ===================================================== */

/** Selector de renderer (usa ShouldUseBigTable/BigTableSectionView del archivo Pro). */
@Composable
fun TableSectionView(section: TableSection) {
    if (section.variant.isRecommendationVariant()) {
        RecommendationTableTheme { RecommendationTableView(section) }
    } else {
        if (ShouldUseBigTable(section)) BigTableSectionView(section)
        else StandardTableSectionView(section)
    }
}

/* ================== RENDER COMPACTO ================== */
@Composable
private fun StandardTableSectionView(section: TableSection) {
    val theme = LocalTableTheme.current
    val cols = section.columns
    val rows = section.rows
    val hScroll = rememberScrollState()

    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val headerMaxLines = 2
    val smallMaxLines = 2
    val collapsedLines = 4

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

                fun isSmallColumn(label: String, key: String): Boolean {
                    val k = key.lowercase(); val l = label.lowercase()
                    return k == "cor" || k == "loe" || k == "op" || l == "cor" || l == "loe" || l == "op"
                }

                val headerStyle = MaterialTheme.typography.labelLarge.copy(fontSize = theme.textMaxSp.sp)
                val cellStyle   = MaterialTheme.typography.bodyMedium.copy(fontSize = theme.textMaxSp.sp)
                val measurePadPx = with(density) { (theme.cellPaddingH.dp * 2).toPx() }
                val verticalPadPx = with(density) { (theme.cellPaddingV.dp * 2).toPx() }
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

                val colWdp: List<Dp> = widthsPx.map { with(density) { it.toDp() } }
                val contentPadPx = with(density) { (theme.cellPaddingH.dp * 2).toPx() }
                val colContentPx: List<Int> = colWdp.map { wDp ->
                    (with(density) { wDp.toPx() } - contentPadPx).toInt().coerceAtLeast(1)
                }

                val rowLineSp = max(theme.textMaxSp * 1.25f, theme.textMaxSp + 2f)
                val linePx = with(density) { rowLineSp.sp.toPx() }
                val rowHdp = with(density) { (linePx * collapsedLines + verticalPadPx).toDp() }
                val headerHdp = with(density) { (linePx * headerMaxLines + verticalPadPx).toDp() }

                val minTableWidth = colWdp.fold(0.dp) { acc, w -> acc + w }

                Box(
                    modifier = Modifier
                        .width(containerWidth)
                        .horizontalScroll(hScroll)
                        .padding(12.dp)
                ) {
                    Column(modifier = Modifier.widthIn(min = minTableWidth)) {
                        // Header
                        Row {
                            cols.forEachIndexed { i, col ->
                                ExpandableCell(
                                    text = col.label,
                                    isHeader = true,
                                    contentWidthPx = colContentPx[i],
                                    collapsedMaxLines = headerMaxLines,
                                    lineHeightSp = rowLineSp,
                                    modifier = Modifier
                                        .width(colWdp[i])
                                        .height(headerHdp)
                                )
                            }
                        }
                        // Filas
                        var lastGroup: String? = null
                        rows.forEach { r ->
                            if (!r.group.isNullOrBlank() && r.group != lastGroup) {
                                lastGroup = r.group
                                GroupDivider(
                                    label = r.group,
                                    totalWidth = minTableWidth,
                                    bg = MaterialTheme.colorScheme.surfaceVariant,
                                    fg = LocalTableTheme.current.groupLabelColor // ya lo usabas
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                cols.forEachIndexed { i, col ->
                                    val collapsed = if (isSmallColumn(col.label, col.key)) smallMaxLines else collapsedLines
                                    ExpandableCell(
                                        text = r.cells[col.key].orEmpty(),
                                        isHeader = false,
                                        contentWidthPx = colContentPx[i],
                                        collapsedMaxLines = collapsed,
                                        lineHeightSp = rowLineSp,
                                        modifier = Modifier
                                            .width(colWdp[i])
                                            .height(rowHdp)
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

/* ============== RECOMENDACIONES (COR/LOE) ============== */
@Composable
private fun RecommendationTableView(section: TableSection) {
    val cols = section.columns
    val rows = section.rows
    val spec = LocalRecTableTheme.current
    val hScroll = rememberScrollState()
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    // --- si ya tienes detectores y paletas, úsalos; si no, estos índices caerán en 0/1 ---
    val headers = remember(cols) { cols.map { it.label } }
    val keys    = remember(cols) { cols.map { it.key } }
    val idxCor  = remember(keys, headers) {
        val byKey = keys.indexOfFirst { it.equals("cor", ignoreCase = true) }.takeIf { it >= 0 }
        val byLbl = headers.indexOfFirst { it.equals("cor", ignoreCase = true) }.takeIf { it >= 0 }
        byKey ?: byLbl ?: 0
    }
    val idxLoe  = remember(keys, headers) {
        val byKey = keys.indexOfFirst { it.equals("loe", ignoreCase = true) }.takeIf { it >= 0 }
        val byLbl = headers.indexOfFirst { it.equals("loe", ignoreCase = true) }.takeIf { it >= 0 }
        byKey ?: byLbl ?: 1.coerceAtMost(cols.lastIndex)
    }

    // Si tienes utilidades avanzadas (detectRecSystem/RecPalettes), descomenta estas 3 líneas:
    // val loeSamples = remember(rows, idxLoe) { if (idxLoe in cols.indices) rows.take(6).map { it.cells[cols[idxLoe].key].orEmpty() } else emptyList() }
    // val system     = remember(headers, loeSamples) { detectRecSystem(headers, loeSamples) }
    // val codeColors = remember(system, spec) { RecPalettes.paletteFor(system, fallbackCellBg = spec.cellBg, fallbackCellOn = spec.cellOnBg) }

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

                    // Medición “natural” por columna
                    val headerStyle = MaterialTheme.typography.labelLarge.copy(fontSize = spec.textMaxSp.sp)
                    val cellStyle   = MaterialTheme.typography.bodyMedium.copy(fontSize = spec.textMaxSp.sp)
                    val measurePadPx = with(density) { (spec.cellPaddingH.dp * 2).toPx() }
                    val smallMinPx   = with(density) { 40.dp.toPx() }   // columnas COR/LOE
                    val contentMinPx = with(density) { 80.dp.toPx() }   // resto
                    val smallExtraPx = with(density) { 6.dp.toPx() }

                    val natural = cols.mapIndexed { i, col ->
                        var maxTextPx = measurer.measure(AnnotatedString(col.label), headerStyle).size.width.toFloat()
                        rows.forEach { r ->
                            val t = r.cells[col.key].orEmpty()
                            val w = measurer.measure(AnnotatedString(t), cellStyle).size.width.toFloat()
                            if (w > maxTextPx) maxTextPx = w
                        }
                        val isSmall = (i == idxCor || i == idxLoe)
                        val minPx = if (isSmall) smallMinPx else contentMinPx
                        maxOf(minPx, maxTextPx + measurePadPx)
                    }.toMutableList()

                    // un poco más de ancho para las cajas COR/LOE
                    if (idxCor in natural.indices) natural[idxCor] = maxOf(smallMinPx, natural[idxCor] + smallExtraPx)
                    if (idxLoe in natural.indices) natural[idxLoe] = maxOf(smallMinPx, natural[idxLoe] + smallExtraPx)

                    // Reparto resto
                    val widthsPx = natural.toMutableList()
                    val restIdxs = (0 until cols.size).filter { it != idxCor && it != idxLoe }
                    if (restIdxs.isNotEmpty()) {
                        val fixedSum: Float = listOf(idxCor, idxLoe)
                            .filter { it in widthsPx.indices }
                            .sumOf { widthsPx[it].toDouble() }
                            .toFloat()

                        val available = (containerPx - fixedSum)
                            .coerceAtLeast(contentMinPx * restIdxs.size)

                        val restBaseSum = restIdxs
                            .sumOf { widthsPx[it].toDouble() }
                            .toFloat()
                            .coerceAtLeast(1f)

                        var assignedSum = 0f
                        restIdxs.forEach { i ->
                            val target = maxOf(contentMinPx, available * (widthsPx[i] / restBaseSum))
                            widthsPx[i] = target
                            assignedSum += target
                        }
                        if (assignedSum > 0f && assignedSum != available) {
                            val scale = available / assignedSum
                            restIdxs.forEach { i -> widthsPx[i] *= scale }
                        }
                    } else {
                        val naturalSum = widthsPx.sum()
                        if (naturalSum < containerPx) {
                            val extra = containerPx - naturalSum
                            val denom = widthsPx.sum().takeIf { it > 0f } ?: cols.size.toFloat()
                            for (i in widthsPx.indices) widthsPx[i] += extra * (widthsPx[i] / denom)
                        }
                    }

                    val colWdp: List<Dp> = widthsPx.map { with(density) { it.toDp() } }

                    // Ancho interno para AutoResizeText
                    val contentPadPx2 = with(density) { (spec.cellPaddingH.dp * 2).toPx() }
                    val colContentPx: List<Int> = colWdp.map { wDp ->
                        (with(density) { wDp.toPx() } - contentPadPx2).toInt().coerceAtLeast(1)
                    }

                    val minTableWidth = colWdp.fold(0.dp) { acc, w -> acc + w }

                    Box(
                        modifier = Modifier
                            .width(containerWidth)
                            .horizontalScroll(hScroll)
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.widthIn(min = minTableWidth)) {

                            // Header
                            Row {
                                cols.forEachIndexed { i, col ->
                                    RecCell(
                                        text = col.label,
                                        isHeader = true,
                                        bg = spec.headerBg,
                                        fg = spec.headerOnBg,
                                        contentWidthPx = colContentPx[i],
                                        centerContent = (i == idxCor || i == idxLoe),
                                        modifier = Modifier
                                            .width(colWdp[i])
                                            .heightIn(min = spec.cellMinHeightDp.dp),
                                        maxLines = headerMaxLines
                                    )
                                }
                            }

                            // --- FILAS + SEPARADORES DE GRUPO ---
                            var lastGroup: String? = null
                            rows.forEach { r ->
                                // Cuando cambia el grupo, dibuja un separador ancho con el título
                                if (!r.group.isNullOrBlank() && r.group != lastGroup) {
                                    lastGroup = r.group
                                    GroupDivider(
                                        label = r.group!!,
                                        totalWidth = minTableWidth,
                                        bg = MaterialTheme.colorScheme.surfaceVariant,
                                        fg = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    cols.forEachIndexed { i, col ->
                                        val textCell = r.cells[col.key].orEmpty()
                                        val isCor = (i == idxCor) && textCell.isNotBlank()
                                        val isLoe = (i == idxLoe) && textCell.isNotBlank()

                                        // Si usas RecPalettes, descomenta para colorear por clase/nivel:
                                        // val (bg, fg) = when {
                                        //     isCor -> codeColors.colorForCor(textCell)
                                        //     isLoe -> codeColors.colorForLoe(textCell)
                                        //     else  -> spec.cellBg to spec.cellOnBg
                                        // }

                                        // Fallback: color único para columnas COR/LOE (tema actual)
                                        val (bg, fg) = when {
                                            isCor -> spec.corBg to spec.corOnBg
                                            isLoe -> spec.loeBg to spec.loeOnBg
                                            else  -> spec.cellBg to spec.cellOnBg
                                        }

                                        RecCell(
                                            text = textCell,
                                            isHeader = false,
                                            bg = bg,
                                            fg = fg,
                                            contentWidthPx = colContentPx[i],
                                            centerContent = (i == idxCor || i == idxLoe),
                                            modifier = Modifier
                                                .width(colWdp[i])
                                                .heightIn(min = spec.cellMinHeightDp.dp),
                                            maxLines = if (i == idxCor || i == idxLoe) corLoeMaxLines else contentMaxLines
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

@Composable
private fun GroupDivider(
    label: String,
    totalWidth: Dp,
    bg: Color = MaterialTheme.colorScheme.surfaceVariant,
    fg: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    // Un poco de aire antes del separador
    Spacer(Modifier.height(8.dp))
    Box(
        modifier = Modifier
            .width(totalWidth)
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = fg
        )
    }
    Spacer(Modifier.height(4.dp))
}



private fun String?.isRecommendationVariant(): Boolean {
    if (this == null) return false
    val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        .lowercase()
    return normalized == "recomendacion"
}
