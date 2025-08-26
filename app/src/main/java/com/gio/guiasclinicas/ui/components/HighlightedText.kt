// HighlightedText.kt
package com.gio.guiasclinicas.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

/**
 * Pinta todas las coincidencias en amarillo y, si [focus] no es null,
 * pinta esa coincidencia específica en verde.
 *
 * Los rangos se esperan con fin inclusivo (ej. 10..14).
 */
// === Reemplaza la función completa ===
fun buildHighlighted(
    text: String,
    matches: List<IntRange>,
    focus: IntRange?
): AnnotatedString {
    if (text.isEmpty() || matches.isEmpty()) return AnnotatedString(text)

    // Normaliza rangos a [start..end] inclusivo dentro de bounds
    fun IntRange.toClosedBounded(): IntRange {
        if (isEmpty()) return 0..-1
        val s = first.coerceIn(0, text.lastIndex.coerceAtLeast(0))
        val e = last.coerceIn(s, text.lastIndex.coerceAtLeast(0))
        return s..e
    }

    val closed = matches.map { it.toClosedBounded() }.filter { it.first <= it.last }.sortedBy { it.first }
    val focusClosed = focus?.toClosedBounded()

    val yellowBg = SpanStyle(background = Color(0xFFFFF59D))
    val greenBg  = SpanStyle(background = Color(0xFFA5D6A7))

    return buildAnnotatedString {
        var cursor = 0
        for (r in closed) {
            // Texto previo sin resaltar
            if (cursor < r.first) append(text.substring(cursor, r.first))

            // Substring del rango r (ojo: .last es inclusivo → endExcl = last + 1)
            val endExcl = r.last + 1
            val style   = if (focusClosed != null && r == focusClosed) greenBg else yellowBg

            pushStyle(style)
            append(text.substring(r.first, endExcl))
            pop()

            cursor = endExcl
        }
        if (cursor < text.length) append(text.substring(cursor))
    }
}
