package com.gio.guiasclinicas.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GuideManifest(
    val schemaVersion: String,
    val guide: GuideMeta,
    val chapters: List<ChapterEntry>,
    val assets: GuideAssets = GuideAssets()
)

@Serializable
data class GuideMeta(
    val slug: String,
    val title: String,
    val version: String,
    val publishedAt: String,
    val organizations: List<String>,
    val status: String,
    val locale: String,
    val changelog: String,
    val features: List<String>
)

/**
 * He dejado tus campos y añadí opciones opcionales para compatibilidad con distintas fuentes:
 * - path / chapterPath / file: por si tu JSON apunta directamente al capítulo
 * - manifestPath: ya lo tienes; lo consideraremos como fallback en la UI
 */
@Serializable
data class ChapterEntry(
    val slug: String,
    val title: String,
    val order: Int,
    val folder: String? = null,
    val manifestPath: String? = null,
    val hash: String? = null,

    // compatibilidad con otros esquemas
    val path: String? = null,
    @SerialName("chapterPath") val chapterPath: String? = null,
    val file: String? = null
)

@Serializable
data class GuideAssets(
    val images: List<String> = emptyList(),
    val documents: List<String> = emptyList()
)
