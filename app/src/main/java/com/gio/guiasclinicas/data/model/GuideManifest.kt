package com.gio.guiasclinicas.data.model

data class GuideManifest(
    val schemaVersion: String,
    val guide: GuideMeta,
    val chapters: List<ChapterEntry>,
    val assets: GuideAssets = GuideAssets()
)

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

data class ChapterEntry(
    val slug: String,
    val title: String,
    val order: Int,
    val folder: String,
    val manifestPath: String,
    val hash: String
)

data class GuideAssets(
    val images: List<String> = emptyList(),
    val documents: List<String> = emptyList()
)
