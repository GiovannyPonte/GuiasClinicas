package com.gio.guiasclinicas.ui.components.table

private const val SHY = '\u00AD'   // soft hyphen: sólo aparece si hay salto
private const val ZWSP = '\u200B'  // zero width space: punto de ruptura sin guion

private val VOWELS = setOf(
    'a','e','i','o','u','á','é','í','ó','ú',
    'A','E','I','O','U','Á','É','Í','Ó','Ú'
)

/**
 * Inserta puntos de ruptura "agradables" para español:
 * - Añade ZWSP después de separadores comunes para facilitar saltos.
 * - Para palabras largas (> threshold) inserta SHY en bloques preferidos (≈8),
 *   idealmente dejando vocal al final del renglón; nunca trozos < minChunk.
 */
object SmartBreaks {
    fun prepareEs(text: String, threshold: Int = 9, prefer: Int = 8, minChunk: Int = 4): String {
        if (text.isEmpty()) return text

        val sb = StringBuilder(text.length + 8)
        val separators = setOf('/', '\\', '-', '·', '—', '–', '—', '·', '.', ',', ';', ':', '|', '+', '=')

        var i = 0
        while (i < text.length) {
            // separadores → agregamos ZWSP tras el separador
            val ch = text[i]
            if (separators.contains(ch)) {
                sb.append(ch).append(ZWSP)
                i++
                continue
            }

            // espacios → copiar tal cual
            if (ch.isWhitespace()) {
                sb.append(ch)
                i++
                continue
            }

            // consumir palabra (letras/dígitos)
            val start = i
            while (i < text.length && !text[i].isWhitespace() && !separators.contains(text[i])) {
                i++
            }
            val word = text.substring(start, i)
            sb.append(hyphenateWordEs(word, threshold, prefer, minChunk))
        }
        return sb.toString()
    }

    private fun hyphenateWordEs(word: String, threshold: Int, prefer: Int, minChunk: Int): String {
        if (word.length <= threshold) return word

        val out = StringBuilder(word.length + 4)
        var pos = 0
        val end = word.length

        while (pos + threshold < end) {
            // candidato de corte: prefer, pero deja al menos minChunk al final
            var cut = (pos + prefer).coerceAtMost(end - minChunk)

            // intenta dejar vocal al final del renglón
            while (cut > pos + minChunk && !VOWELS.contains(word[cut - 1])) {
                cut--
            }
            if (cut <= pos + minChunk) {
                // no encontramos vocal "bien", usa prefer/redondeo
                cut = (pos + prefer).coerceAtMost(end - minChunk)
            }

            out.append(word, pos, cut).append(SHY)
            pos = cut
        }
        out.append(word.substring(pos))
        return out.toString()
    }
}
