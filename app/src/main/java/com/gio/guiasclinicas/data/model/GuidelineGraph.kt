
// package com.gio.guideline.render

package com.gio.guiasclinicas.data.model

import kotlin.math.max

// Estructuras para layout/render
data class GNode(
    val id: String,
    val type: String,
    val titleOrPrompt: String,
    val data: Node,
    var width: Float = 240f,
    var height: Float = 72f,
    var layer: Int = 0,
    var x: Float = 0f,
    var y: Float = 0f
)

data class GEdge(
    val from: String,
    val to: String,
    val label: String? = null
)

data class GGraph(
    val nodes: MutableMap<String, GNode> = LinkedHashMap(),
    val edges: MutableList<GEdge> = ArrayList()
)

// Construye grafo a partir de GuidelineFlow
fun buildGraph(flow: GuidelineFlow, locale: String = "es"): GGraph {
    val g = GGraph()
    fun txt(n: Node): String = when {
        n.title?.best()?.isNotBlank() == true -> n.title.best()
        n.prompt?.best()?.isNotBlank() == true -> n.prompt.best()
        n.recommendation?.best()?.isNotBlank() == true -> n.recommendation.best()
        else -> n.id
    }
    flow.nodes.forEach { n ->
        g.nodes[n.id] = GNode(
            id = n.id,
            type = n.type,
            titleOrPrompt = txt(n),
            data = n
        )
        // aristas por transitions
        n.transitions?.forEach { t ->
            val to = t.goto ?: return@forEach
            g.edges += GEdge(n.id, to, t.label ?: t.`when`?.toString())
        }
        // arista por next
        n.next?.let { g.edges += GEdge(n.id, it, null) }
    }
    // Asignar capas topológicas (BFS desde start)
    val startId = flow.nodes.firstOrNull { it.type == "start" }?.id ?: flow.nodes.first().id
    val adj = g.edges.groupBy { it.from }.mapValues { it.value.map { e -> e.to } }
    val visited = HashSet<String>()
    val queue: ArrayDeque<Pair<String, Int>> = ArrayDeque()
    queue.add(startId to 0)
    visited.add(startId)
    while (queue.isNotEmpty()) {
        val (nid, lvl) = queue.removeFirst()
        g.nodes[nid]?.layer = lvl
        adj[nid]?.forEach { to ->
            if (visited.add(to)) queue.add(to to (lvl + 1))
        }
    }
    // Nodos no alcanzados (por seguridad)
    g.nodes.values.filter { it.id !in visited }.forEach { it.layer = g.nodes.values.maxOf { n -> n.layer } + 1 }
    return g
}

// Disposición jerárquica: columnas por capa, x distribuidas, y por capas
data class LayoutConfig(
    val spacingX: Float = 64f,
    val spacingY: Float = 56f,
    val minNodeW: Float = 200f,
    val maxNodeW: Float = 360f,
    val nodeHPad: Float = 16f,
    val nodeVPad: Float = 12f
)

// Calcula posición (x,y) sin solapes (aprox)
fun layoutGraph(graph: GGraph, cfg: LayoutConfig): Pair<Float, Float> {
    // Agrupa por layer
    val byLayer = graph.nodes.values.groupBy { it.layer }.toSortedMap()
    // Anchuras/alturas uniformes por simplicidad (se actualizarán tras medir)
    var y = 0f
    var totalW = 0f
    byLayer.forEach { (layer, nodes) ->
        var x = 0f
        var layerMaxH = 0f
        nodes.forEach { n ->
            n.x = x
            n.y = y
            x += (n.width + cfg.spacingX)
            layerMaxH = max(layerMaxH, n.height)
        }
        totalW = max(totalW, x - cfg.spacingX) // quita último espacio
        y += (layerMaxH + cfg.spacingY)
    }
    val totalH = y - cfg.spacingY
    // Centra columnas más cortas respecto al ancho total
    byLayer.forEach { (_, nodes) ->
        val rowW = nodes.sumOf { it.width.toDouble() }.toFloat() + cfg.spacingX * (nodes.size - 1)
        val offsetX = (totalW - rowW) / 2f
        nodes.forEach { it.x += offsetX }
    }
    return totalW to totalH
}
