package com.gio.guiasclinicas.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gio.guiasclinicas.data.model.TableColumn
import com.gio.guiasclinicas.data.model.TableRow
import com.gio.guiasclinicas.data.model.TableSection
import com.gio.guiasclinicas.ui.theme.LocalTableTheme
import kotlin.math.max

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton


/* ===================== PARÁMETROS ===================== */
private const val ROW_MAX_LINES = 4
private const val REPEAT_HEADER_EVERY = 12
private const val LINE_HEIGHT_FACTOR = 1.25f
/* ===================================================== */

/** Heurística: decide si usar el renderer “Pro”. */
@Composable
fun ShouldUseBigTable(section: TableSection): Boolean {
    val cols = section.columns.size
    if (cols <= 3) return false

    val dense = section.rows.asSequence().take(40).any { r ->
        r.cells.values.any { cell ->
            val len = cell.length
            val spaces = cell.count { it.isWhitespace() }
            len >= 120 || spaces >= 24
        }
    }

    // Heurística extra por ancho estimado del header
    val conf = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWpx = with(density) { conf.screenWidthDp.dp.toPx() }
    val fontSizeSp = 14.sp
    val avgCharPx = with(density) { fontSizeSp.toPx() } * 0.55f
    val gapPx = with(density) { 16.dp.toPx() }

    val headerChars = section.columns.sumOf { c -> max(c.label.trim().length, 3) }
    val estimated = headerChars * avgCharPx + (cols - 1) * gapPx
    val overflows = estimated > screenWpx * 1.20f

    return dense || overflows
}

/** Renderer Pro: 1ª columna fija + resto scrolleable, altura uniforme, paginación. */
@Composable
fun BigTableSectionView(section: TableSection) {
    val theme = LocalTableTheme.current
    val cols = section.columns
    val rows = section.rows

    // Paginación
    val pageSizes = listOf(10, 20, 50)
    var rowsPerPage by remember { mutableStateOf(20) }
    var page by remember { mutableStateOf(0) }
    val pageCount = remember(rows.size, rowsPerPage) { max(1, (rows.size + rowsPerPage - 1) / rowsPerPage) }
    if (page >= pageCount) page = pageCount - 1
    val firstRowIdx = page * rowsPerPage
    val pageRows = rows.drop(firstRowIdx).take(rowsPerPage)

    // Medidas
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val screenWpx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val contentPadPx = with(density) { (theme.cellPaddingH.dp * 2).toPx() }
    val vPadPx = with(density) { (theme.cellPaddingV.dp * 2).toPx() }

    val headerStyle = MaterialTheme.typography.labelLarge.copy(fontSize = theme.textMaxSp.sp)
    val cellStyle   = MaterialTheme.typography.bodyMedium.copy(fontSize = theme.textMaxSp.sp)

    val rowLineSp = max(theme.textMaxSp * LINE_HEIGHT_FACTOR, theme.textMaxSp + 2f)
    val linePx = with(density) { rowLineSp.sp.toPx() }
    val rowHdp = with(density) { (linePx * ROW_MAX_LINES + vPadPx).toDp() }
    val headerHdp = with(density) { (linePx * 2 + vPadPx).toDp() }

    // Límites
    val firstMaxPx = screenWpx * 0.42f
    val firstMinPx = max(screenWpx * 0.34f, with(density) { 120.dp.toPx() })
    val restMinPx  = with(density) { 96.dp.toPx() }
    val restMaxPx  = with(density) { 180.dp.toPx() }

    // Muestra estable
    val sample = remember(rows) { rows.take(60) }

    // Medición natural
    val natPx = cols.map { col ->
        var maxW = measurer.measure(AnnotatedString(col.label), headerStyle).size.width.toFloat()
        sample.forEach { r ->
            val t = r.cells[col.key].orEmpty()
            val w = measurer.measure(AnnotatedString(t), cellStyle).size.width.toFloat()
            if (w > maxW) maxW = w
        }
        maxW + contentPadPx
    }.toMutableList()

    if (natPx.isNotEmpty()) natPx[0] = natPx[0].coerceIn(firstMinPx, firstMaxPx)
    for (i in 1 until natPx.size) natPx[i] = natPx[i].coerceIn(restMinPx, restMaxPx)

    val colWdp = natPx.map { with(density) { it.toDp() } }
    val colContentPx = colWdp.map { (with(density) { it.toPx() } - contentPadPx).toInt().coerceAtLeast(1) }

    val hScroll = rememberScrollState()
    LaunchedEffect(page, rowsPerPage) { hScroll.scrollTo(0) }

    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        section.title?.let {
            Text(it, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
        }
        Card(
            shape = RoundedCornerShape(theme.cornerRadiusDp.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header
            TableHeaderRow(cols, colWdp, colContentPx, hScroll, headerHdp)

            // Filas
            var lastGroup: String? = null
            pageRows.forEachIndexed { idx, r ->
                if (idx > 0 && idx % REPEAT_HEADER_EVERY == 0) {
                    TableHeaderRow(cols, colWdp, colContentPx, hScroll, headerHdp)
                }
                if (!r.group.isNullOrBlank() && r.group != lastGroup) {
                    lastGroup = r.group
                    Row(Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                        Text(r.group!!.uppercase(), style = MaterialTheme.typography.labelMedium, color = LocalTableTheme.current.groupLabelColor)
                    }
                }
                DataRow(r, cols, colWdp, colContentPx, rowHdp, hScroll, rowLineSp)
            }

            // Nota
            section.footnote?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            // Pager
            if (pageCount > 1) {
                PagerBar(
                    page = page,
                    pageCount = pageCount,
                    rowsPerPage = rowsPerPage,
                    totalRows = rows.size,
                    onPrev = { if (page > 0) page-- },
                    onNext = { if (page < pageCount - 1) page++ },
                    onChangeRowsPerPage = { rowsPerPage = it; page = 0 },
                    pageSizes = pageSizes,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun DataRow(
    r: TableRow,
    cols: List<TableColumn>,
    colWdp: List<Dp>,
    colContentPx: List<Int>,
    rowHdp: Dp,
    hScroll: androidx.compose.foundation.ScrollState,
    rowLineSp: Float
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        // 1ª columna fija
        ExpandableCell(
            text = r.cells[cols.first().key].orEmpty(),
            isHeader = false,
            contentWidthPx = colContentPx[0],
            collapsedMaxLines = ROW_MAX_LINES,
            lineHeightSp = rowLineSp,
            modifier = Modifier.width(colWdp[0]).height(rowHdp)
        )
        // Resto scrolleable
        Box(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth().horizontalScroll(hScroll)) {
                cols.drop(1).forEachIndexed { i, col ->
                    ExpandableCell(
                        text = r.cells[col.key].orEmpty(),
                        isHeader = false,
                        contentWidthPx = colContentPx[i + 1],
                        collapsedMaxLines = ROW_MAX_LINES,
                        lineHeightSp = rowLineSp,
                        modifier = Modifier.width(colWdp[i + 1]).height(rowHdp)
                    )
                }
            }
            RightEdgeFade()
        }
    }
}

/* ------------------------- Header reutilizable ------------------------- */
@Composable
private fun TableHeaderRow(
    cols: List<TableColumn>,
    colWdp: List<Dp>,
    colContentPx: List<Int>,
    hScroll: androidx.compose.foundation.ScrollState,
    headerHdp: Dp
) {
    val theme = LocalTableTheme.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ExpandableCell(
            text = cols.firstOrNull()?.label.orEmpty(),
            isHeader = true,
            contentWidthPx = colContentPx[0],
            collapsedMaxLines = 2,
            lineHeightSp = (theme.textMaxSp * LINE_HEIGHT_FACTOR),
            modifier = Modifier.width(colWdp[0]).height(headerHdp)
        )
        Box(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth().horizontalScroll(hScroll)) {
                cols.drop(1).forEachIndexed { i, col ->
                    ExpandableCell(
                        text = col.label,
                        isHeader = true,
                        contentWidthPx = colContentPx[i + 1],
                        collapsedMaxLines = 2,
                        lineHeightSp = (theme.textMaxSp * LINE_HEIGHT_FACTOR),
                        modifier = Modifier.width(colWdp[i + 1]).height(headerHdp)
                    )
                }
            }
            RightEdgeFade()
        }
    }
}

/* --------------------- Fade derecho (overlay) --------------------- */
@Composable
private fun RightEdgeFade(width: Dp = 24.dp) {
    val bg = MaterialTheme.colorScheme.surface
    val wPx = with(LocalDensity.current) { width.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                val start = (size.width - wPx).coerceAtLeast(0f)
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, bg),
                        startX = start,
                        endX = size.width
                    ),
                    topLeft = Offset(start, 0f),
                    size = androidx.compose.ui.geometry.Size(
                        (size.width - start).coerceAtLeast(0f),
                        size.height
                    )
                )
            }
    )
}

/* -------- Celda expandible (común a Pro y Estándar) -------- */
@Composable
fun ExpandableCell(
    text: String,
    isHeader: Boolean,
    contentWidthPx: Int,
    collapsedMaxLines: Int,
    lineHeightSp: Float,
    modifier: Modifier = Modifier
) {
    val theme = LocalTableTheme.current
    val shape = RoundedCornerShape(6.dp)
    var showDialog by remember { mutableStateOf(false) }
    var overflowing by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .border(BorderStroke(theme.borderWidthDp.dp, theme.cellBorder), shape)
            .background(color = if (isHeader) theme.headerBg else theme.cellBg, shape = shape)
            .padding(horizontal = theme.cellPaddingH.dp, vertical = theme.cellPaddingV.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = (if (isHeader) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium).copy(
                fontSize = theme.textMaxSp.sp,
                lineBreak = LineBreak.Paragraph,
                hyphens = Hyphens.Auto,
                lineHeight = lineHeightSp.sp
            ),
            maxLines = collapsedMaxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { overflowing = it.hasVisualOverflow },
            modifier = Modifier.fillMaxWidth()
        )
        if (overflowing) {
            IconButton(onClick = { showDialog = true }, modifier = Modifier.align(Alignment.BottomEnd)) {
                Icon(Icons.Filled.MoreHoriz, contentDescription = "Ver más")
            }
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(min = 280.dp, max = 600.dp)
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (isHeader) "Título de columna" else "Detalle",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = lineHeightSp.sp),
                        modifier = Modifier
                            .heightIn(min = 120.dp, max = 420.dp)
                            .verticalScroll(rememberScrollState())
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showDialog = false }) { Text("Cerrar") }
                    }
                }
            }
        }
    }
}

/* --------------------------- Barra de paginación -------------------------- */
@Composable
private fun PagerBar(
    page: Int,
    pageCount: Int,
    rowsPerPage: Int,
    totalRows: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onChangeRowsPerPage: (Int) -> Unit,
    pageSizes: List<Int>,
    modifier: Modifier = Modifier
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onPrev, enabled = page > 0) { Text("◀") }
        Text(" ${page + 1} / $pageCount  •  $totalRows filas", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onNext, enabled = page < pageCount - 1) { Text("▶") }
        Spacer(Modifier.weight(1f))
        var open by remember { mutableStateOf(false) }
        Box {
            TextButton(onClick = { open = true }) { Text("Filas/página: $rowsPerPage") }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                pageSizes.forEach {
                    DropdownMenuItem(
                        text = { Text(it.toString()) },
                        onClick = { onChangeRowsPerPage(it); open = false }
                    )
                }
            }
        }
    }
}
