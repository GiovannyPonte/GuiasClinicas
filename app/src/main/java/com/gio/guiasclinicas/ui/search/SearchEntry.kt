package com.gio.guiasclinicas.search

/**
 * Plain representation of a text fragment that will be indexed in the FTS
 * database. A chapter can generate multiple entries if it contains several
 * sections.
 */
data class SearchEntry(
    val guideSlug: String,
    val guideTitle: String,
    val chapterPath: String,
    val chapterTitle: String,
    val sectionId: String?,
    val plainText: String
)