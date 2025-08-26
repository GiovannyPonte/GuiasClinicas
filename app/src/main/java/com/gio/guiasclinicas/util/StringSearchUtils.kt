package com.gio.guiasclinicas.util

import java.text.Normalizer

/** Flags para búsqueda. Por defecto: NO distinguir mayúsculas ni acentos. */
data class SearchFlags(
    val caseSensitive: Boolean = false,
    val accentSensitive: Boolean = false
)

/**
 * Cadena normalizada + mapa de posiciones para re-mapear índices sobre
 * la cadena original (necesario para pintar highlights correctos).
 *
 * `map[i]` = índice en la cadena original del carácter normalizado en `normalized[i]`.
 */
data class NormalizedText(
    val normalized: String,
    val map: IntArray
)

/** Normaliza texto según flags y devuelve la cadena normalizada y su mapa de posiciones. */
fun String.normalizeForSearch(flags: SearchFlags = SearchFlags()): NormalizedText {
    val out = StringBuilder(length)
    val map = ArrayList<Int>(length)

    for (origIndex in indices) {
        var segment = this[origIndex].toString()
        // Descompone y elimina diacríticos si no es sensible a acentos
        if (!flags.accentSensitive) {
            val nfd = Normalizer.normalize(segment, Normalizer.Form.NFD)
            val sb = StringBuilder(nfd.length)
            for (ch in nfd) {
                val type = Character.getType(ch)
                if (type != Character.NON_SPACING_MARK.toInt() &&
                    type != Character.COMBINING_SPACING_MARK.toInt()
                ) {
                    sb.append(ch)
                }
            }
            segment = sb.toString()
        }
        // Minúsculas si no es sensible a mayúsculas
        if (!flags.caseSensitive) {
            segment = segment.lowercase()
        }
        out.append(segment)
        repeat(segment.length) { map += origIndex }
    }

    return NormalizedText(out.toString(), map.toIntArray())
}

/**
 * Devuelve todos los rangos (en índices de la CADENA ORIGINAL) donde aparece `needleNormalized`
 * dentro de `haystack.normalized`. El `needleNormalized` debe venir ya normalizado con las mismas flags.
 */
fun findAllMatches(haystack: NormalizedText, needleNormalized: String): List<IntRange> {
    val ranges = ArrayList<IntRange>()
    if (needleNormalized.isEmpty()) return ranges
    val h = haystack.normalized
    var idx = h.indexOf(needleNormalized)
    while (idx >= 0) {
        val startOriginal = if (idx < haystack.map.size) haystack.map[idx] else 0
        val endIdx = idx + needleNormalized.length - 1
        val endOriginal = if (endIdx < haystack.map.size) haystack.map[endIdx] else startOriginal
        ranges += IntRange(startOriginal, endOriginal)
        idx = h.indexOf(needleNormalized, idx + 1)
    }
    return ranges
}

/** Preview con contexto alrededor del primer match. */
fun buildPreview(original: String, firstMatch: IntRange, window: Int = 48): String {
    if (original.isEmpty()) return ""
    val start = (firstMatch.first - window).coerceAtLeast(0)
    val end = (firstMatch.last + 1 + window).coerceAtMost(original.length)
    val prefix = if (start > 0) "… " else ""
    val suffix = if (end < original.length) " …" else ""
    return buildString {
        append(prefix)
        append(original.substring(start, end))
        append(suffix)
    }
}
