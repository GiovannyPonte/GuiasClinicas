package com.gio.guiasclinicas.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonNames

@Serializable
@JsonClassDiscriminator("type")
sealed interface ChapterSection {
    val id: String?
    val title: String?
    val footnote: String?
}

@Serializable
@SerialName("text")
data class TextSection(
    override val id: String? = null,
    override val title: String? = null,
    val heading: String? = null,
    val body: String? = null,
    override val footnote: String? = null
) : ChapterSection

@Serializable
@SerialName("table")
data class TableSection(
    override val id: String? = null,
    override val title: String? = null,
    @SerialName("nCols") val nCols: Int? = null,
    @SerialName("nRows") val nRows: Int? = null,
    val columns: List<TableColumn> = emptyList(),
    val rows: List<TableRow> = emptyList(),
    override val footnote: String? = null,
    // Tablas “especiales” (p.ej. "Recomendacion")
    val variant: String? = null
) : ChapterSection

@Serializable
@SerialName("image")
data class ImageSection(
    override val id: String? = null,
    override val title: String? = null,
    val path: String,
    val caption: String? = null,
    val alt: String? = null,
    override val footnote: String? = null
) : ChapterSection

@Serializable
@SerialName("organigrama")
data class OrganigramaSection(
    override val id: String? = null,
    override val title: String? = null,
    /** Ruta relativa al guideDir, por ejemplo: "organigramas/organigrama_diagnostico_TEP.json" */
    val path: String,
    val locale: String? = null,
    val showKeyTables: Boolean? = true,
    override val footnote: String? = null
) : ChapterSection

@Serializable
data class TableColumn(
    val key: String,
    // Valor por defecto para soportar encabezados vacíos o ausentes
    val label: String = ""
)

@Serializable
data class TableRow(
    val group: String? = null,

    /**
     * Operador de agrupación de celdas.
     * Soporta JSON que venga como "operator" o como "op".
     */
    @SerialName("operator")
    @JsonNames("op")
    val operator: String? = null,

    val cells: Map<String, String> = emptyMap()
)

// --- NUEVO: tipo de sección "workflow" ---
@Serializable
@SerialName("workflow")
data class WorkflowSection(
    override val id: String? = null,
    override val title: String? = null,
    /** Ruta relativa al guideDir, p.ej.: "capitulos/workflows/workflow_diagnostico_TEP.json" */
    val path: String,
    val startButtonLabel: String? = null,
    val locale: String? = "es",
    override val footnote: String? = null
) : ChapterSection
