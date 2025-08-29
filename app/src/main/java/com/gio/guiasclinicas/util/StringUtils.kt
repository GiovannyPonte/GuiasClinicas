package com.gio.guiasclinicas.util

import java.text.Normalizer

fun normalizeText(input: String, caseSensitive: Boolean, accentSensitive: Boolean): String {
    var result = input
    if (!accentSensitive) {
        result = Normalizer.normalize(result, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
    }
    if (!caseSensitive) {
        result = result.lowercase()
    }
    return result
}

fun equalsNormalized(a: String, b: String, caseSensitive: Boolean, accentSensitive: Boolean): Boolean {
    return normalizeText(a, caseSensitive, accentSensitive) == normalizeText(b, caseSensitive, accentSensitive)
}

fun containsNormalized(text: String, query: String, caseSensitive: Boolean, accentSensitive: Boolean): Boolean {
    return normalizeText(text, caseSensitive, accentSensitive)
        .contains(normalizeText(query, caseSensitive, accentSensitive))
}