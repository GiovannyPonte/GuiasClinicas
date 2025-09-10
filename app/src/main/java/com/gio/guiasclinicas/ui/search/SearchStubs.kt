package com.gio.guiasclinicas.ui.search

import androidx.compose.ui.text.AnnotatedString

enum class SearchPart { HEADING, BODY, FOOTNOTE, CAPTION, ALT, CELL }

data class SearchResult(
    val sectionKey: String = "",
    val part: SearchPart = SearchPart.BODY,
    val start: Int = 0,
    val length: Int = 0,
    val index: Int = 0,
    val preview: String = "",
    val previewStart: Int = 0,
    val row: Int? = null,
    val cellKey: String? = null
)

data class ScopedSearchResult(val placeholder: Int = 0)

// Sin implementación real (para desbloquear compilación)
fun searchAllGuides(vararg any: Any?): List<ScopedSearchResult> = emptyList()

fun highlightText(text: String, matches: List<SearchResult>, currentIndex: Int): AnnotatedString =
    AnnotatedString(text)
