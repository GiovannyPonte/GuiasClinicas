package com.gio.guiasclinicas.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreHoriz
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
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gio.guiasclinicas.data.model.TableSection
import com.gio.guiasclinicas.ui.components.table.SmartBreaks
import com.gio.guiasclinicas.ui.theme.LocalTableTheme
import com.gio.guiasclinicas.ui.search.SearchResult
import com.gio.guiasclinicas.ui.search.highlightText
import com.gio.guiasclinicas.ui.search.SearchPart
import kotlin.math.max
import kotlin.math.roundToInt

/* ===================== PARÁMETROS AJUSTABLES ===================== */
/** Líneas colapsadas por celda (todas las columnas) antes de elipsis. */
private const val ROW_MAX_LINES: Int = 4
/** Repetir encabezado cada N filas (mejora de legibilidad barata en memoria). */
private const val REPEAT_HEADER_EVERY: Int = 12
/** Factor para altura de línea (en relación al tamaño de texto del tema). */
private const val LINE_HEIGHT_FACTOR: Float = 1.25f
/* ================================================================= */

/* -----------------------------------------------------------------------
 *  HEURÍSTICA: usar el renderer Pro solo si hay >3 columnas y celdas densas
 * ---------------------------------------------------------------------- */
@Composable
fun ShouldUseBigTable(section: TableSection): Boolean {
    val cols = section.columns.size
    if (cols <= 3) return false
    // Densidad: miramos hasta 40 filas; consideramos "abundante" si len >= 120
    // o si hay >= 24 espacios (muchas palabras).
    val dense = section.rows.asSequence().take(40).any { r ->
        r.cells.values.any { cell ->
            val len = cell.length
            val spaces = cell.count { it.isWhitespace() }
            len >= 120 || spaces >= 24
        }
    }
    return dense
}

/* -----------------------------------------------------------------------
 *  RENDERER PRO: 1ª columna fija con ancho limitado, resto scrolleable,
 *  celdas con altura uniforme, elipsis + diálogo, paginación y fade derecho.
 * ---------------------------------------------------------------------- */
@Composable
fun BigTableSectionView(
    section: TableSection,
    matches: List<SearchResult>,
    currentIndex: Int
) {
    val theme = LocalTableTheme.current
    val cols = section.columns
    val allRows = section.rows

    // ---------- Paginación ----------
    val pageSizes = listOf(10, 20, 50)
    var rowsPerPage by remember { mutableStateOf(20) }
    var page by remember { mutableStateOf(0) }

    val pageCount = remember(allRows.size, rowsPerPage) {
        max(1, (allRows.size + rowsPerPage - 1) / rowsPerPage)
    }
    if (page >= pageCount) page = pageCount - 1

    val firstRowIndex = page * rowsPerPage
    val pageRows = allRows.drop(firstRowIndex).take(rowsPerPage)

    // ---------- Medidas / Cálculo de anchos y alturas ----------
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val screenWidthPx = with(density) { screenWidthDp.toPx() }
    val contentPadPx = with(density) { (theme.cellPaddingH.dp * 2).toPx() }
    val verticalPadPx = with(density) { (theme.cellPaddingV.dp * 2).toPx() }

    val headerStyle = MaterialTheme.typography.labelLarge.copy(fontSize = theme.textMaxSp.sp)
    val cellStyle   = MaterialTheme.typography.bodyMedium.copy(fontSize = theme.textMaxSp.sp)

    // Altura de línea fija para TODAS las celdas (uniformidad visual)
    val rowLineHeightSp = max(theme.textMaxSp * LINE_HEIGHT_FACTOR, theme.textMaxSp + 2f)
    val lineHeightPx = with(density) { rowLineHeightSp.sp.toPx() }
    val rowHeightDp = with(density) { (lineHeightPx * ROW_MAX_LINES + verticalPadPx).toDp() }
    val headerHeightDp = with(density) { (lineHeightPx * 2 + verticalPadPx).toDp() } // 2 líneas para header

    // Límites de ancho (móvil vertical)
    val firstMaxPx = (screenWidthPx * 0.42f) // 1ª columna tope superior (~42%)
    val firstMinPx = max(screenWidthPx * 0.34f, with(density) { 120.dp.toPx() }) // 1ª col mínimo
    val restMinPx  = with(density) { 96.dp.toPx() } // otras columnas mínimo

    // Tope máximo dinámico para columnas 2..N según nº de columnas
    val colCount = cols.size
    val restMaxFraction = when {
        colCount <= 4 -> 0.46f
        colCount <= 6 -> 0.40f
        else          -> 0.36f
    }
    val restMaxPx = max(screenWidthPx * restMaxFraction, with(density) { 180.dp.toPx() })

    // Muestra global estable para evitar "saltos" al cambiar de página
    val sampleRows = remember(allRows) { allRows.take(60) }

    // Medición "natural" por columna (header + muestra), en línea única
    val naturalPx = cols.mapIndexed { i, col ->
        var maxText = measurer.measure(AnnotatedString(col.label), headerStyle).size.width.toFloat()
        sampleRows.forEach { r ->
            val t = r.cells[col.key].orEmpty()
            val w = measurer.measure(AnnotatedString(t), cellStyle).size.width.toFloat()
            if (w > maxText) maxText = w
        }
        maxText + contentPadPx
    }.toMutableList()

    // Clamp 1ª columna
    if (naturalPx.isNotEmpty()) {
        naturalPx[0] = naturalPx[0].coerceIn(firstMinPx, firstMaxPx)
    }
    // Clamp resto de columnas
    for (i in 1 until naturalPx.size) {
        naturalPx[i] = naturalPx[i].coerceIn(restMinPx, restMaxPx)
    }

    // A Dp y ancho utilizable (sin padding) para detectar overflow
    val colWidthsDp = naturalPx.map { with(density) { it.toDp() } }
    val colContentWidthPx = colWidthsDp.map {
        (with(density) { it.toPx() } - contentPadPx).toInt().coerceAtLeast(1)
    }

    // Scroll horizontal compartido (2..N)
    val hScroll = rememberScrollState()
    // Al cambiar de página, resetea el scroll horizontal
    LaunchedEffect(page, rowsPerPage) { hScroll.scrollTo(0) }

    // ---------- UI ----------
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
            Spacer(Modifier.height(8.dp))
        }

        Card(
            shape = RoundedCornerShape(theme.cornerRadiusDp.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // ----------------------------- HEADER INICIAL -----------------------------
            TableHeaderRow(
                cols = cols,
                colWidthsDp = colWidthsDp,
                colContentWidthPx = colContentWidthPx,
                hScroll = hScroll,
                headerHeightDp = headerHeightDp
            )

            // ----------------------------- FILAS -----------------------------
            var lastGroup: String? = null
            pageRows.forEachIndexed { index, r ->

                // Repetimos encabezado cada N filas (barato, sin segundo scroll)
                if (index > 0 && index % REPEAT_HEADER_EVERY == 0) {
                    TableHeaderRow(
                        cols = cols,
                        colWidthsDp = colWidthsDp,
                        colContentWidthPx = colContentWidthPx,
                        hScroll = hScroll,
                        headerHeightDp = headerHeightDp
                    )
                }

                // Separador de grupo si corresponde
                if (!r.group.isNullOrBlank() && r.group != lastGroup) {
                    lastGroup = r.group
                    Row(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                        Text(
                            text = r.group!!.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = theme.groupLabelColor
                        )
                    }
                }

                // Fila de datos
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 1ª columna fija (misma altura para TODA la fila)
                    ExpandableCell(
                        text = r.cells[cols.first().key].orEmpty(),
                        isHeader = false,
                        contentWidthPx = colContentWidthPx[0],
                        collapsedMaxLines = ROW_MAX_LINES,
                        lineHeightSp = rowLineHeightSp,
                        modifier = Modifier
                            .width(colWidthsDp[0])
                            .height(rowHeightDp)
                    )

                    // Resto de celdas con scroll horizontal (misma altura)
                    Box(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(hScroll)
                        ) {
                            cols.drop(1).forEachIndexed { i, col ->
                                ExpandableCell(
                                    text = r.cells[col.key].orEmpty(),
                                    isHeader = false,
                                    contentWidthPx = colContentWidthPx[i + 1],
                                    collapsedMaxLines = ROW_MAX_LINES,
                                    lineHeightSp = rowLineHeightSp,
                                    modifier = Modifier
                                        .width(colWidthsDp[i + 1])
                                        .height(rowHeightDp)
                                )
                            }
                        }
                        RightEdgeFade()
                    }
                }
            }

            // ----------------------------- FOOTNOTE -----------------------------
            section.footnote?.let { note ->
                Spacer(Modifier.height(8.dp))
                val footMatches = matches.filter { it.part == SearchPart.FOOTNOTE }
                Text(
                    text = highlightText(note, footMatches, currentIndex),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            // ----------------------------- PAGINACIÓN -----------------------------
            if (pageCount > 1) {
                PagerBar(
                    page = page,
                    pageCount = pageCount,
                    rowsPerPage = rowsPerPage,
                    totalRows = allRows.size,
                    onPrev = { if (page > 0) page-- },
                    onNext = { if (page < pageCount - 1) page++ },
                    onChangeRowsPerPage = {
                        rowsPerPage = it
                        page = 0
                    },
                    pageSizes = pageSizes,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/* ------------------------- Header reutilizable ------------------------- */
@Composable
private fun TableHeaderRow(
    cols: List<com.gio.guiasclinicas.data.model.TableColumn>,
    colWidthsDp: List<Dp>,
    colContentWidthPx: List<Int>,
    hScroll: androidx.compose.foundation.ScrollState,
    headerHeightDp: Dp
) {
    val theme = LocalTableTheme.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1ª columna fija (HEADER) — expandible si desborda
        ExpandableCell(
            text = cols.firstOrNull()?.label.orEmpty(),
            isHeader = true,
            contentWidthPx = colContentWidthPx[0],
            collapsedMaxLines = 2,
            lineHeightSp = (theme.textMaxSp * LINE_HEIGHT_FACTOR),
            modifier = Modifier
                .width(colWidthsDp[0])
                .height(headerHeightDp)
        )

        // Resto del header, scrolleable en bloque, con fade
        Box(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(hScroll)
            ) {
                cols.drop(1).forEachIndexed { i, col ->
                    ExpandableCell(
                        text = col.label,
                        isHeader = true,
                        contentWidthPx = colContentWidthPx[i + 1],
                        collapsedMaxLines = 2,
                        lineHeightSp = (theme.textMaxSp * LINE_HEIGHT_FACTOR),
                        modifier = Modifier
                            .width(colWidthsDp[i + 1])
                            .height(headerHeightDp)
                    )
                }
            }
            RightEdgeFade() // overlay sutil para indicar más columnas
        }
    }
}

/* --------------------- Fade derecho (overlay robusto) --------------------- */
@Composable
private fun RightEdgeFade(width: Dp = 24.dp) {
    val bg = MaterialTheme.colorScheme.surface
    val wPx = with(LocalDensity.current) { width.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize() // cubre el Box padre (no intercepta gestos)
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

/* ---------------- Celda expandible con elipsis + diálogo ---------------- */
@Composable
fun ExpandableCell(
    text: String,
    isHeader: Boolean,
    contentWidthPx: Int,
    collapsedMaxLines: Int,
    lineHeightSp: Float,
    modifier: Modifier = Modifier,
) {
    val theme = LocalTableTheme.current
    val shape = RoundedCornerShape(6.dp)
    var showDialog by remember { mutableStateOf(false) }
    var overflowing by remember { mutableStateOf(false) }

    // Preprocesado: cortes "inteligentes" y ZWSP en tokens largos
    val prepared = remember(text) { prepareDenseCellText(text) }

    Box(
        modifier = modifier
            .border(BorderStroke(theme.borderWidthDp.dp, theme.cellBorder), shape)
            .background(color = if (isHeader) theme.headerBg else theme.cellBg, shape = shape)
            .padding(horizontal = theme.cellPaddingH.dp, vertical = theme.cellPaddingV.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = prepared,
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
                        text = prepared,
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
    var menuOpen by remember { mutableStateOf(false) }
    val from = if (totalRows == 0) 0 else page * rowsPerPage + 1
    val to = ((page + 1) * rowsPerPage).coerceAtMost(totalRows)

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev, enabled = page > 0) { Icon(Icons.Filled.ChevronLeft, null) }
        Text("$from–$to de $totalRows filas", style = MaterialTheme.typography.bodySmall)
        IconButton(onClick = onNext, enabled = page < pageCount - 1) { Icon(Icons.Filled.ChevronRight, null) }
        Spacer(Modifier.weight(1f))
        Box {
            TextButton(onClick = { menuOpen = true }) { Text("Filas/página: $rowsPerPage") }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                pageSizes.forEach {
                    DropdownMenuItem(
                        text = { Text(it.toString()) },
                        onClick = {
                            onChangeRowsPerPage(it)
                            menuOpen = false
                        }
                    )
                }
            }
        }
    }
}

/* -------------------- Utilidades para partir el texto -------------------- */

// Inserta ZWSP cada N chars en tokens largos (evita columnas "gigantes")
private fun insertZwspEvery(token: String, chunk: Int = 10): String {
    if (token.length < chunk) return token
    val sb = StringBuilder(token.length + token.length / chunk)
    var i = 0
    while (i < token.length) {
        val end = (i + chunk).coerceAtMost(token.length)
        sb.append(token.substring(i, end))
        if (end < token.length) sb.append('\u200B') // ZWSP
        i = end
    }
    return sb.toString()
}

// Tokens de ≥24 chars (letras, números o _) se consideran "muy largos"
private val LONG_TOKEN = Regex("[\\p{L}\\p{N}_]{24,}")

// Prepara el texto de celda:
// 1) Soft breaks que ya usas (SmartBreaks)
// 2) ZWSP tras separadores frecuentes
// 3) ZWSP dentro de tokens muy largos
private fun prepareDenseCellText(raw: String): String {
    val base = SmartBreaks.prepareEs(raw)
    val withDelims = base
        .replace("/", "/\u200B")
        .replace("\\", "\\\u200B")
        .replace("_", "_\u200B")
        .replace("·", "·\u200B")
        .replace("(", "(\u200B")

    return LONG_TOKEN.replace(withDelims) { m -> insertZwspEvery(m.value, chunk = 10) }
}
