// package com.gio.guiasclinicas.ui.components

package com.gio.guiasclinicas.ui.components

import android.R
import android.R.attr.theme
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.gio.guiasclinicas.data.model.*
import com.gio.guiasclinicas.ui.theme.DefaultDiagramTheme
import com.gio.guiasclinicas.ui.theme.DiagramTheme
import kotlin.math.max
import kotlin.math.min
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.DpSize
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color as ComposeColor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip

import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.graphics.drawscope.scale

import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.platform.LocalDensity

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity

import androidx.compose.ui.unit.toSize






@Composable
fun GuidelineDiagramFromAssets(
    relativePath: String,
    modifier: Modifier = Modifier,
    theme: DiagramTheme = DefaultDiagramTheme,
    locale: String = "es",
    showKeyTablesBelow: Boolean = true,
    embedded: Boolean = false                       // 👈 NUEVO
) {
    val context = LocalContext.current
    var flow by remember { mutableStateOf<GuidelineFlow?>(null) }
    var error by remember { mutableStateOf<Throwable?>(null) }

    LaunchedEffect(relativePath) {
        val result = runCatching { GuidelineParser.fromAssets(context, relativePath) }
        error = result.exceptionOrNull()
        flow  = result.getOrNull()
        error?.let { Log.e("GuidelineDiagram", "Error cargando asset: $relativePath", it) }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        when {
            flow != null -> GuidelineDiagram(
                flow = flow!!,
                theme = theme,
                locale = locale,
                showKeyTablesBelow = showKeyTablesBelow,
                embedded = embedded                 // 👈 pasa flag
            )
            error != null -> Text(
                "No se pudo cargar el diagrama:\n$relativePath",
                modifier = Modifier.align(Alignment.Center)
            )
            else -> Text("Cargando diagrama…", modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun GuidelineDiagram(
    flow: GuidelineFlow,
    theme: DiagramTheme,
    locale: String,
    showKeyTablesBelow: Boolean,
    embedded: Boolean
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    // Config de layout
    val cfg = remember(flow.ui_hints?.layout) {
        LayoutConfig(
            spacingX = (flow.ui_hints?.layout?.spacing_x ?: 64).toFloat(),
            spacingY = (flow.ui_hints?.layout?.spacing_y ?: 56).toFloat(),
            minNodeW = 240f,
            maxNodeW = 360f
        )
    }

    // 1) Construcción + medición + layout
    val graph = remember(flow) { buildGraph(flow, locale) }
    LaidOutGraphWrapped(graph, cfg, textMeasurer, theme)
    layoutGraphImproved(graph, cfg)

    // 2) BBox del contenido
    val bbox = remember(graph.nodes.size) { computeBoundingBox(graph) }

    // 3) Estado del visor
    var viewSizePx by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    var baseScale by remember { mutableFloatStateOf(1f) }   // fit-to-view
    var zoom by remember { mutableFloatStateOf(1f) }        // relativo a baseScale
    val hScroll = rememberScrollState()
    val vScroll = rememberScrollState()
    val minZoom = 0.75f
    val maxZoom = 6f

    val nodeTextStyle = TextStyle(
        color = theme.text,
        fontSize = MaterialTheme.typography.bodyMedium.fontSize
    )

    // Fit-to-view cuando cambia tamaño o contenido
    LaunchedEffect(viewSizePx, flow.id, bbox.left, bbox.top, bbox.width, bbox.height) {
        if (viewSizePx.width > 0 && viewSizePx.height > 0 && bbox.width > 0f && bbox.height > 0f) {
            val fit = min(
                viewSizePx.width.toFloat() / bbox.width,
                viewSizePx.height.toFloat() / bbox.height
            )
            baseScale = fit * 0.95f
            zoom = 1f
            hScroll.scrollTo(0)
            vScroll.scrollTo(0)
        }
    }

    fun totalScale() = baseScale * zoom
    val scope = rememberCoroutineScope()

    fun zoomTo(newZoom: Float, focus: Offset? = null) {
        val clamped = newZoom.coerceIn(minZoom, maxZoom)
        val sc0 = totalScale()
        val sc1 = baseScale * clamped
        val f = focus ?: Offset(viewSizePx.width / 2f, viewSizePx.height / 2f)

        val contentW1 = bbox.width * sc1
        val contentH1 = bbox.height * sc1

        val newHX = ((hScroll.value + f.x) * sc1 / sc0 - f.x)
            .coerceIn(0f, max(0f, contentW1 - viewSizePx.width))
        val newVY = ((vScroll.value + f.y) * sc1 / sc0 - f.y)
            .coerceIn(0f, max(0f, contentH1 - viewSizePx.height))

        zoom = clamped
        scope.launch {
            hScroll.scrollTo(newHX.toInt())
            vScroll.scrollTo(newVY.toInt())
        }
    }

    val canvasBoxModifier =
        if (embedded) Modifier.fillMaxWidth().heightIn(min = 360.dp, max = 720.dp)
        else Modifier.fillMaxSize()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.background)
    ) {
        Box(modifier = canvasBoxModifier) {
            val sc = totalScale()
            val contentPxW = max(1f, bbox.width * sc)
            val contentPxH = max(1f, bbox.height * sc)
            val contentDpW = with(density) { contentPxW.toDp() }
            val contentDpH = with(density) { contentPxH.toDp() }

            // Scroll horizontal EXTERIOR + scroll vertical INTERIOR → no hay colisión
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { viewSizePx = it }
                    .horizontalScroll(hScroll)
            ) {
                Box(
                    modifier = Modifier
                        .verticalScroll(vScroll)
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(DpSize(contentDpW, contentDpH))
                            .pinchToZoomOnly(viewSizePx, zoom) { newZoom, focus ->
                                zoomTo(newZoom, focus)
                            }
                    ) {
                        withTransform({
                            translate(-bbox.left, -bbox.top)
                            scale(sc)
                        }) {
                            drawEdges(graph, theme)
                            drawNodes(graph, theme, textMeasurer, nodeTextStyle)
                        }
                    }
                }
            }

            // Controles flotantes (+ / − / reset)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ComposeColor(0x4D000000))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(onClick = { zoomTo(zoom * 1.15f) }) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Acercar", tint = ComposeColor.White)
                }
                IconButton(onClick = { zoomTo(zoom / 1.15f) }) {
                    Icon(imageVector = Icons.Filled.Remove, contentDescription = "Alejar", tint = ComposeColor.White)
                }
                IconButton(onClick = {
                    zoom = 1f
                    scope.launch { hScroll.scrollTo(0); vScroll.scrollTo(0) }
                }) {
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Ajustar a pantalla", tint = ComposeColor.White)
                }
            }
        }

        if (showKeyTablesBelow) {
            Divider()
            KeyTables(flow = flow, theme = theme, embedded = embedded)
        }
    }
}



@Composable
private fun LaidOutGraph(
    graph: GGraph,
    cfg: LayoutConfig,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    theme: DiagramTheme
) {
    val style = TextStyle(
        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
        color = theme.text
    )
    graph.nodes.values.forEach { n ->
        val text = n.titleOrPrompt.ifBlank { n.id }
        val measured = textMeasurer.measure(AnnotatedString(text), style)
        val padW = 28f
        val padH = 28f
        val minW = if (n.type == "decision") 220f else cfg.minNodeW
        val w = kotlin.math.max(minW, measured.size.width + padW)
        val hBase = if (n.type == "decision") 84f else 72f
        val h = kotlin.math.max(hBase, measured.size.height + padH)
        n.width  = kotlin.math.min(cfg.maxNodeW, w)
        n.height = h
    }
}

// Resto de helpers geométricos
private fun computeBoundingBox(graph: GGraph): Rect {
    var left = Float.POSITIVE_INFINITY
    var top = Float.POSITIVE_INFINITY
    var right = Float.NEGATIVE_INFINITY
    var bottom = Float.NEGATIVE_INFINITY
    graph.nodes.values.forEach { n ->
        left   = min(left,   n.x)
        top    = min(top,    n.y)
        right  = max(right,  n.x + n.width)
        bottom = max(bottom, n.y + n.height)
    }
    return if (left == Float.POSITIVE_INFINITY) Rect(0f,0f,1f,1f) else Rect(left, top, right, bottom)
}

// 4) Dibujo de nodos con wrapping y bordes
private fun DrawScope.drawNodes(
    graph: GGraph,
    theme: DiagramTheme,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    textStyle: TextStyle                      // ← el estilo viene calculado desde el @Composable
) {
    graph.nodes.values.forEach { n ->
        val r = Rect(n.x, n.y, n.x + n.width, n.y + n.height)
        val fill = when (n.type) {
            "decision" -> theme.decisionFill
            "form"     -> theme.formFill
            "score"    -> theme.scoreFill
            "output"   -> theme.outputFill
            "start", "end" -> theme.scoreFill.copy(alpha = 0.7f)
            else       -> theme.nodeFill
        }

        // fondo por forma
        when (n.type) {
            "decision" -> drawPath(diamondPath(r), fill)
            "start","end" -> drawRoundRect(
                color = fill,
                topLeft = Offset(r.left, r.top),
                size = r.size,
                cornerRadius = CornerRadius(999f, 999f)
            )
            else -> drawRoundRect(
                color = fill,
                topLeft = Offset(r.left, r.top),
                size = r.size,
                cornerRadius = CornerRadius(16f, 16f)
            )
        }
        // borde
        val outline = when (n.type) {
            "decision"   -> diamondPath(r)
            "start","end"-> roundPath(r, 16f)
            else         -> roundPath(r, 16f)
        }
        drawPath(outline, color = theme.nodeStroke, style = Stroke(width = theme.lineWidth.value))

        // texto con wrap dentro del padding
        drawText(
            textMeasurer,
            AnnotatedString(n.titleOrPrompt),
            topLeft = Offset(r.left + 16f, r.top + 12f),
            size = androidx.compose.ui.geometry.Size(n.width - 32f, n.height - 24f),
            style = textStyle
        )
    }
}


// 5) Dibujo de aristas con márgenes de salida/entrada
private fun DrawScope.drawEdges(graph: GGraph, theme: DiagramTheme) {
    val stroke = Stroke(width = theme.lineWidth.value, cap = StrokeCap.Round)
    val vGap = 6f // margen para separar de los nodos

    graph.edges.forEach { e ->
        val from = graph.nodes[e.from] ?: return@forEach
        val to   = graph.nodes[e.to]   ?: return@forEach

        val start = Offset(from.x + from.width / 2f, from.y + from.height + vGap)
        val end   = Offset(to.x   + to.width   / 2f, to.y   - vGap)

        // Ruta ortogonal con codo horizontal central
        val midY = (start.y + end.y) / 2f
        val p = Path().apply {
            moveTo(start.x, start.y)
            lineTo(start.x, midY)
            lineTo(end.x,   midY)
            lineTo(end.x,   end.y)
        }
        drawPath(p, color = theme.edgeStroke, style = stroke)
        drawArrowHead(end, up = false, color = theme.arrowHead)
    }
}

private fun diamondPath(r: Rect): Path = Path().apply {
    moveTo((r.left + r.right)/2f, r.top)
    lineTo(r.right, (r.top + r.bottom)/2f)
    lineTo((r.left + r.right)/2f, r.bottom)
    lineTo(r.left, (r.top + r.bottom)/2f)
    close()
}

private fun roundPath(r: Rect, radius: Float): Path =
    Path().apply { addRoundRect(RoundRect(r, CornerRadius(radius, radius))) }

private fun DrawScope.drawArrowHead(tip: Offset, up: Boolean, color: ComposeColor) {
    val s = 8f
    val sign = if (up) -1f else 1f
    val p = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(tip.x - s, tip.y - sign * s)
        lineTo(tip.x + s, tip.y - sign * s)
        close()
    }
    drawPath(p, color = color)
}
// 1) Medición “wrapped”: calcula ancho objetivo y re-mide para obtener altura real
@Composable
private fun LaidOutGraphWrapped(
    graph: GGraph,
    cfg: LayoutConfig,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    theme: DiagramTheme
) {
    val baseStyle = TextStyle(
        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
        color = theme.text
    )
    graph.nodes.values.forEach { n ->
        val text = n.titleOrPrompt.ifBlank { n.id }
        val padW = 28f
        val padH = 28f
        val minW = if (n.type == "decision") max(260f, cfg.minNodeW) else cfg.minNodeW
        val hBase = if (n.type == "decision") 90f else 72f

        // Medición sin constraints → estimación de ancho
        val loose = textMeasurer.measure(AnnotatedString(text), baseStyle)
        val guessW = min(cfg.maxNodeW, max(minW, loose.size.width + padW))

        // Re-medición con constraints de ancho (wrapping)
        val para = textMeasurer.measure(
            text = AnnotatedString(text),
            style = baseStyle,
            constraints = Constraints(maxWidth = (guessW - padW).toInt())
        )
        val w = min(cfg.maxNodeW, max(minW, para.size.width + padW))
        val h = max(hBase, para.size.height + padH)

        n.width = w
        n.height = h
    }
}
// 2) Layout jerárquico (BFS) + orden por barycenter (evita cruces) + centrado por fila
private fun layoutGraphImproved(graph: GGraph, cfg: LayoutConfig): Pair<Float, Float> {
    // Recalcular niveles desde 'start'
    val start = graph.nodes.values.firstOrNull { it.type == "start" }?.id
        ?: graph.nodes.keys.firstOrNull().orEmpty()
    assignLayersBFS(graph, start)

    // Padres por nodo
    val parents = graph.edges.groupBy { it.to }.mapValues { it.value.map { e -> e.from } }
    // Orden por capa, usando barycenter con respecto a la capa previa
    val layers = graph.nodes.values.groupBy { it.layer }.toSortedMap()

    val orderInLayer = mutableMapOf<String, Int>()
    layers.forEach { (layer, nodes) ->
        val ordered = if (layer == 0) {
            nodes.sortedBy { it.id }
        } else {
            nodes.sortedBy { n ->
                val ps = parents[n.id].orEmpty()
                if (ps.isEmpty()) Int.MAX_VALUE.toDouble()
                else ps.mapNotNull { p -> orderInLayer[p]?.toDouble() }.average()
            }
        }
        ordered.forEachIndexed { idx, n -> orderInLayer[n.id] = idx }
    }

    // Posicionamiento x/y por filas
    var y = 0f
    var totalW = 0f
    layers.forEach { (_, row) ->
        val nodes = row.sortedBy { orderInLayer[it.id] ?: 0 }
        var x = 0f
        var rowH = 0f
        nodes.forEach { n ->
            n.x = x
            n.y = y
            x += n.width + cfg.spacingX
            rowH = max(rowH, n.height)
        }
        val rowW = if (nodes.isEmpty()) 0f else x - cfg.spacingX
        totalW = max(totalW, rowW)
        // próxima fila
        y += rowH + cfg.spacingY
    }

    // Centrar cada fila dentro del ancho total
    layers.forEach { (_, row) ->
        val rowW = row.sumOf { it.width.toDouble() }.toFloat() + cfg.spacingX * (row.size - 1)
        val dx = (totalW - rowW) / 2f
        row.forEach { it.x += dx }
    }

    val totalH = y - cfg.spacingY
    return totalW to totalH
}

// Asigna 'layer' vía BFS desde start
private fun assignLayersBFS(graph: GGraph, start: String) {
    graph.nodes.values.forEach { it.layer = Int.MAX_VALUE }
    val adj: Map<String, List<String>> =
        graph.edges.groupBy { it.from }.mapValues { it.value.map { e -> e.to } }
    val q: ArrayDeque<Pair<String, Int>> = ArrayDeque()
    q.add(start to 0)
    graph.nodes[start]?.layer = 0
    val seen = mutableSetOf(start)

    while (q.isNotEmpty()) {
        val (u, lvl) = q.removeFirst()
        graph.nodes[u]?.layer = min(graph.nodes[u]?.layer ?: lvl, lvl)
        // 🔧 aquí estaba el problema: adj[u] es List<String>?
        adj[u].orEmpty().forEach { v ->
            if (seen.add(v)) q.add(v to (lvl + 1))
        }
    }
}

// 3) Clamp del pan para mantener el contenido dentro del viewport (o centrado si cabe)
private fun clampOffset(
    offset: Offset,
    bbox: Rect,
    scale: Float,
    viewW: Float,
    viewH: Float
): Offset {
    val contentW = bbox.width * scale
    val contentH = bbox.height * scale
    val shiftX = -bbox.left * scale
    val shiftY = -bbox.top * scale

    val (minX, maxX) = if (contentW >= viewW) {
        (viewW - contentW + shiftX) to (0f + shiftX)
    } else {
        val center = (viewW - contentW) / 2f + shiftX
        center to center
    }
    val (minY, maxY) = if (contentH >= viewH) {
        (viewH - contentH + shiftY) to (0f + shiftY)
    } else {
        val center = (viewH - contentH) / 2f + shiftY
        center to center
    }
    return Offset(offset.x.coerceIn(minX, maxX), offset.y.coerceIn(minY, maxY))
}
// Utilidad para null safety en mapas
private fun <K,V> Map<K, List<V>>.orElseEmpty(): List<V> = this.values.firstOrNull() ?: emptyList()

private fun centerPan(
    viewSize: androidx.compose.ui.unit.IntSize,
    bbox: Rect,
    scale: Float
): Offset {
    val contentW = bbox.width * scale
    val contentH = bbox.height * scale
    val tx = (viewSize.width - contentW) / 2f - bbox.left * scale
    val ty = (viewSize.height - contentH) / 2f - bbox.top * scale
    return Offset(tx, ty)
}

private fun clampPan(
    pan: Offset,
    bbox: Rect,
    scale: Float,
    viewW: Float,
    viewH: Float
): Offset {
    val contentW = bbox.width * scale
    val contentH = bbox.height * scale
    val shiftX = -bbox.left * scale
    val shiftY = -bbox.top * scale

    val (minX, maxX) = if (contentW >= viewW) {
        (viewW - contentW + shiftX) to (0f + shiftX)
    } else {
        val c = (viewW - contentW) / 2f + shiftX
        c to c
    }
    val (minY, maxY) = if (contentH >= viewH) {
        (viewH - contentH + shiftY) to (0f + shiftY)
    } else {
        val c = (viewH - contentH) / 2f + shiftY
        c to c
    }
    return Offset(pan.x.coerceIn(minX, maxX), pan.y.coerceIn(minY, maxY))
}

private fun Modifier.pinchToZoomOnly(
    viewSizePx: androidx.compose.ui.unit.IntSize,
    zoom: Float,
    onZoom: (newZoom: Float, focus: Offset) -> Unit
): Modifier = pointerInput(viewSizePx, zoom) {
    awaitEachGesture {
        var active = true
        while (active) {
            val event = awaitPointerEvent()
            val pressed = event.changes.count { it.pressed }
            if (pressed >= 2) {
                val z = event.calculateZoom()
                val c = event.calculateCentroid(useCurrent = true)
                if (z != 1f) {
                    onZoom(zoom * z, c)
                    // consumimos el gesto pinch para que no llegue al scroll
                    event.changes.forEach { it.consume() }
                }
            } else {
                // 0–1 dedos: no consumimos → el scroll recibe el arrastre
            }
            active = event.changes.any { it.pressed }
        }
    }
}
