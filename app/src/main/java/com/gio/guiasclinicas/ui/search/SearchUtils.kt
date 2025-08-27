package com.gio.guiasclinicas.ui.search

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.gio.guiasclinicas.data.model.ChapterSection
import com.gio.guiasclinicas.data.model.TextSection
import java.text.Normalizer

/** Indicates which part of a section matched the query */
enum class SearchPart { BODY, FOOTNOTE }

/** Represents one occurrence of the search query */
data class SearchResult(
    val sectionKey: String,
    val part: SearchPart,
    val start: Int,
    val length: Int,
    val index: Int
)

private fun normalize(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        .lowercase()

/** Finds all occurrences of [query] in [sections] ignoring case and accents */
fun searchSections(sections: List<ChapterSection>, query: String): List<SearchResult> {
    val normQuery = normalize(query)
    if (normQuery.isBlank()) return emptyList()
    val results = mutableListOf<SearchResult>()
    sections.forEachIndexed { index, section ->
        val key = section.id ?: "sec-$index-${section::class.simpleName}"
        when (section) {
            is TextSection -> {
                section.body?.let { body ->
                    val normBody = normalize(body)
                    var start = 0
                    while (true) {
                        val idx = normBody.indexOf(normQuery, start)
                        if (idx < 0) break
                        results.add(SearchResult(key, SearchPart.BODY, idx, normQuery.length, results.size))
                        start = idx + normQuery.length
                    }
                }
                section.footnote?.let { foot ->
                    val normFoot = normalize(foot)
                    var start = 0
                    while (true) {
                        val idx = normFoot.indexOf(normQuery, start)
                        if (idx < 0) break
                        results.add(SearchResult(key, SearchPart.FOOTNOTE, idx, normQuery.length, results.size))
                        start = idx + normQuery.length
                    }
                }
            }
            else -> Unit
        }
    }
    return results
}

/** Builds an [AnnotatedString] highlighting [matches]. */
fun highlightText(text: String, matches: List<SearchResult>, currentIndex: Int): AnnotatedString {
    if (matches.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text)
        matches.forEach { m ->
            val color = if (m.index == currentIndex) Color.Green else Color.Yellow
            addStyle(SpanStyle(color = color), m.start, m.start + m.length)
        }
    }
}
