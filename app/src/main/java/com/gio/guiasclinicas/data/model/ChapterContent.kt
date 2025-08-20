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
