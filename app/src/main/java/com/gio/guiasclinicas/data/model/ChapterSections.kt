@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.gio.guiasclinicas.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("type")
sealed interface ChapterSection {
    val id: String?
    val title: String?
    val footnote: String?
}

/** Bloque de TEXTO */
@Serializable
@SerialName("text")
data class TextSection(
    override val id: String? = null,
    override val title: String? = null,
    val heading: String? = null,
    val body: String? = null,
    override val footnote: String? = null
) : ChapterSection

/** Bloque de TABLA */
@Serializable
@SerialName("table")
data class TableSection(
    override val id: String? = null,
    override val title: String? = null,
    @SerialName("nCols") val nCols: Int? = null,
    @SerialName("nRows") val nRows: Int? = null,
    val columns: List<TableColumn> = emptyList(),
    val rows: List<TableRow> = emptyList(),
    override val footnote: String? = null
) : ChapterSection

@Serializable
data class TableColumn(
    val key: String,
    val label: String
)

@Serializable
data class TableRow(
    val group: String? = null,
    val cells: Map<String, String> = emptyMap(),
    val operator: String? = null
)

/** Bloque de IMAGEN */
@Serializable
@SerialName("image")
data class ImageSection(
    override val id: String? = null,
    override val title: String? = null,
    val path: String,                 // ruta relativa dentro de assets/
    val caption: String? = null,
    val alt: String? = null,
    override val footnote: String? = null
) : ChapterSection
