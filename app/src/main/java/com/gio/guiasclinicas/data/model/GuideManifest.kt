package com.gio.guiasclinicas.data.model

data class GuideManifest(
    val title: String,
    val version: String,
    val chapters: List<GuideChapter>
)

data class GuideChapter(
    val id: String,
    val title: String,
    /** Ruta (relativa o absoluta a clinical_guidelines_db/) del JSON del capítulo */
    val contentPath: String
)

