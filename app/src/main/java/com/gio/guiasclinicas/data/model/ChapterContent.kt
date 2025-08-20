package com.gio.guiasclinicas.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChapterContent(
    @SerialName("schemaVersion") val schemaVersion: String,
    @SerialName("chapter") val chapter: ChapterMeta,
    @SerialName("content") val content: ChapterBody
)

@Serializable
data class ChapterMeta(
    val slug: String,
    val title: String,
    val order: Int
)

@Serializable
data class ChapterBody(
    val summary: String,
    val sections: List<ChapterSection>
)

/**
 * Sección polivalente:
 * - Texto: usa heading/body y deja type = null
 * - Tabla: usa type = "table" y completa los campos de tabla
 */
@Serializable
data class ChapterSection(
    // Texto
    val heading: String? = null,
    val body: String? = null,

    // Identificador de tipo
    val type: String? = null, // "table" para tablas

    // Tabla
    val id: String? = null,
    val title: String? = null,
    val nCols: Int? = null,
    val nRows: Int? = null,
    val columns: List<TableColumn>? = null,
    val rows: List<TableRow>? = null,
    val footnote: String? = null
)

@Serializable
data class TableColumn(
    val key: String,
    val label: String
)

@Serializable
data class TableRow(
    val group: String? = null,
    val operator: String? = null, // "and"/"or"
    val cells: Map<String, String> = emptyMap()
)
