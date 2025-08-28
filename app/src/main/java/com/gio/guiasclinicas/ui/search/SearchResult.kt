package com.gio.guiasclinicas.search

/**
 * Result returned from an FTS query.
 */
data class SearchResult(
    val rowid: Int,
    val guideSlug: String,
    val guideTitle: String,
    val chapterPath: String,
    val chapterTitle: String,
    val sectionId: String?,
    val preview: String
)
