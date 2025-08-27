package com.gio.guiasclinicas.ui.search

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.gio.guiasclinicas.data.model.ChapterSection
import com.gio.guiasclinicas.data.model.ImageSection
import com.gio.guiasclinicas.data.model.TableSection
import com.gio.guiasclinicas.data.model.TextSection
import java.text.Normalizer
import kotlin.math.max
import kotlin.math.min

/** Indicates which part of a section matched the query */
enum class SearchPart { HEADING, BODY, FOOTNOTE, CAPTION, ALT, CELL }

/** Represents one occurrence of the search query */
data class SearchResult(
    val sectionKey: String,
    val part: SearchPart,
    val start: Int,
    val length: Int,
    val index: Int,
    val preview: String,
    val row: Int? = null,
    val cellKey: String? = null
)

private fun normalize(text: String, ignoreCase: Boolean, ignoreAccents: Boolean): String {
    var result = text
    if (ignoreAccents) {
        result = Normalizer.normalize(result, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }
    return if (ignoreCase) result.lowercase() else result
}

private fun String.preview(start: Int, len: Int, radius: Int = 20): String {
    val from = max(0, start - radius)
    val to = min(this.length, start + len + radius)
    return this.substring(from, to)
}

/** Finds all occurrences of [query] in [sections] with configurable rules */
fun searchSections(
    sections: List<ChapterSection>,
    query: String,
    ignoreCase: Boolean = true,
    ignoreAccents: Boolean = true
): List<SearchResult> {
    val normQuery = normalize(query, ignoreCase, ignoreAccents)
    if (normQuery.isBlank()) return emptyList()
    val results = mutableListOf<SearchResult>()
    sections.forEachIndexed { index, section ->
        val key = section.id ?: "sec-$index-${section::class.simpleName}"
        fun scan(text: String?, part: SearchPart, row: Int? = null, cellKey: String? = null) {
            if (text.isNullOrBlank()) return
            val normText = normalize(text, ignoreCase, ignoreAccents)
            var start = 0
            while (true) {
                val idx = normText.indexOf(normQuery, start)
                if (idx < 0) break
                val preview = text.preview(idx, normQuery.length)
                results.add(
                    SearchResult(
                        sectionKey = key,
                        part = part,
                        start = idx,
                        length = normQuery.length,
                        index = results.size,
                        preview = preview,
                        row = row,
                        cellKey = cellKey
                    )
                )
                start = idx + normQuery.length
            }
        }

        when (section) {
            is TextSection -> {
                scan(section.heading, SearchPart.HEADING)
                scan(section.body, SearchPart.BODY)
                scan(section.footnote, SearchPart.FOOTNOTE)
            }

            is ImageSection -> {
                scan(section.caption, SearchPart.CAPTION)
                scan(section.alt, SearchPart.ALT)
                scan(section.footnote, SearchPart.FOOTNOTE)
            }

            is TableSection -> {
                section.footnote?.let { scan(it, SearchPart.FOOTNOTE) }
                section.rows.forEachIndexed { rIdx, row ->
                    row.cells.forEach { (cellK, value) ->
                        scan(value, SearchPart.CELL, rIdx, cellK)
                    }
                }
            }
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
