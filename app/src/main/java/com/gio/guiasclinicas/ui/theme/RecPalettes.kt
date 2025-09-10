// ui/theme/RecPalettes.kt
package com.gio.guiasclinicas.ui.theme

import androidx.compose.ui.graphics.Color
import java.text.Normalizer
import kotlin.math.roundToInt

/** Sistemas de clasificación soportados. */
enum class RecSystem { ACC_AHA_2024, ESC_ERS_2019, UNKNOWN }

/** Paleta de colores por código (COR y LOE). */
data class RecCodeColors(
    val system: RecSystem,
    val corMap: Map<String, Color>,
    val loeMap: Map<String, Color>,
    val fallbackCellBg: Color,
    val fallbackCellOn: Color
) {
    fun colorForCor(raw: String): Pair<Color, Color> {
        val key = normalizeCor(raw)
        val bg = corMap[key] ?: fallbackCellBg
        return bg to onFor(bg, fallbackCellOn)
    }
    fun colorForLoe(raw: String): Pair<Color, Color> {
        val key = normalizeLoe(system, raw)
        val bg = loeMap[key] ?: fallbackCellBg
        return bg to onFor(bg, fallbackCellOn)
    }
}

/** Paletas listas para usar. Colores aproximados a los esquemas de las figuras. */
object RecPalettes {

    // ---------- ACC/AHA (2024) ----------
    private val ACC_COR_1  = Color(0xFF2E7D32) // verde fuerte
    private val ACC_COR_2A = Color(0xFF43A047) // verde medio
    private val ACC_COR_2B = Color(0xFFF9A825) // ámbar
    private val ACC_COR_3N = Color(0xFFEF6C00) // naranja (no beneficio)
    private val ACC_COR_3H = Color(0xFFD32F2F) // rojo (daño) — no siempre presente en tablas

    private val ACC_LOE_A   = Color(0xFF294F75) // azul oscuro
    private val ACC_LOE_BR  = Color(0xFF3E5F8B) // azul medio (B-R)
    private val ACC_LOE_BNR = Color(0xFF5B708B) // azul grisáceo (B-NR)
    private val ACC_LOE_CLD = Color(0xFFA3B1C7) // azul claro (C-LD)
    private val ACC_LOE_CEO = Color(0xFFD7DEE9) // gris azulado pálido (C-EO)

    // ---------- ESC/ERS (2019/2020) ----------
    private val ESC_COR_I   = Color(0xFF2E7D32) // verde
    private val ESC_COR_IIA = Color(0xFFF9B233) // amarillo mostaza
    private val ESC_COR_IIB = Color(0xFFD9822B) // naranja
    private val ESC_COR_III = Color(0xFFD32F2F) // rojo

    private val ESC_LOE_A = Color(0xFF007C91)   // teal oscuro
    private val ESC_LOE_B = Color(0xFF5DA5B3)   // teal medio
    private val ESC_LOE_C = Color(0xFFA9C7D0)   // teal claro

    /** Devuelve la paleta para el sistema dado. */
    fun paletteFor(
        system: RecSystem,
        fallbackCellBg: Color,
        fallbackCellOn: Color
    ): RecCodeColors = when (system) {
        RecSystem.ACC_AHA_2024 -> RecCodeColors(
            system = system,
            corMap = mapOf(
                "i" to ACC_COR_1, "1" to ACC_COR_1, "class1" to ACC_COR_1,
                "iia" to ACC_COR_2A, "2a" to ACC_COR_2A, "class2a" to ACC_COR_2A,
                "iib" to ACC_COR_2B, "2b" to ACC_COR_2B, "class2b" to ACC_COR_2B,
                "iii" to ACC_COR_3N, "3" to ACC_COR_3N, "class3" to ACC_COR_3N,
                "iii-harm" to ACC_COR_3H, "3-harm" to ACC_COR_3H
            ),
            loeMap = mapOf(
                "a" to ACC_LOE_A,
                "b-r" to ACC_LOE_BR, "br" to ACC_LOE_BR, "b_randomized" to ACC_LOE_BR,
                "b-nr" to ACC_LOE_BNR, "bnr" to ACC_LOE_BNR, "b_nonrandomized" to ACC_LOE_BNR,
                "c-ld" to ACC_LOE_CLD, "cld" to ACC_LOE_CLD, "c_limiteddata" to ACC_LOE_CLD,
                "c-eo" to ACC_LOE_CEO, "ceo" to ACC_LOE_CEO, "c_expertopinion" to ACC_LOE_CEO
            ),
            fallbackCellBg = fallbackCellBg,
            fallbackCellOn = fallbackCellOn
        )

        RecSystem.ESC_ERS_2019 -> RecCodeColors(
            system = system,
            corMap = mapOf(
                "i" to ESC_COR_I, "1" to ESC_COR_I, "clasei" to ESC_COR_I, "classi" to ESC_COR_I,
                "iia" to ESC_COR_IIA, "2a" to ESC_COR_IIA, "claseiia" to ESC_COR_IIA,
                "iib" to ESC_COR_IIB, "2b" to ESC_COR_IIB, "claseiib" to ESC_COR_IIB,
                "iii" to ESC_COR_III, "3" to ESC_COR_III, "claseiii" to ESC_COR_III
            ),
            loeMap = mapOf(
                "a" to ESC_LOE_A,
                "b" to ESC_LOE_B,
                "c" to ESC_LOE_C
            ),
            fallbackCellBg = fallbackCellBg,
            fallbackCellOn = fallbackCellOn
        )

        else -> RecCodeColors(
            system = RecSystem.UNKNOWN,
            corMap = emptyMap(),
            loeMap = emptyMap(),
            fallbackCellBg = fallbackCellBg,
            fallbackCellOn = fallbackCellOn
        )
    }
}

/* -------------------- Utilidades -------------------- */

private fun onFor(bg: Color, defaultOn: Color): Color {
    // luminancia simple; si es oscuro -> texto blanco
    val lum = 0.299f * bg.red + 0.587f * bg.green + 0.114f * bg.blue
    return if (lum < 0.55f) Color.White else defaultOn
}

private fun String.stripAccentsLower(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        .lowercase()

private fun normalizeCor(raw: String): String {
    val s = raw.stripAccentsLower().replace(Regex("[^a-z0-9]+"), "")
    return when {
        s.startsWith("class1") || s == "1" || s == "i" -> "i"
        s.startsWith("class2a") || s == "2a" || s == "iia" -> "iia"
        s.startsWith("class2b") || s == "2b" || s == "iib" -> "iib"
        s.startsWith("class3harm") || s == "3harm" -> "iii-harm"
        s.startsWith("class3") || s == "3" || s == "iii" -> "iii"
        s.startsWith("clasei") -> "i"      // compat ESC en español
        s.startsWith("claseiia") -> "iia"
        s.startsWith("claseiib") -> "iib"
        s.startsWith("claseiii") -> "iii"
        else -> s
    }
}

private fun normalizeLoe(sys: RecSystem, raw: String): String {
    val s = raw.stripAccentsLower()
        .replace('‑','-').replace('–','-').replace('—','-') // guiones raros
        .replace(Regex("[^a-z0-9-]+"), "")
    return when (sys) {
        RecSystem.ACC_AHA_2024 -> when {
            s == "a" -> "a"
            s == "b" || s == "br" || s == "b-r" || s == "b_randomized" -> "b-r"
            s == "bnr" || s == "b-nr" || s == "b_nonrandomized" -> "b-nr"
            s == "cld" || s == "c-ld" -> "c-ld"
            s == "ceo" || s == "c-eo" -> "c-eo"
            else -> s
        }
        RecSystem.ESC_ERS_2019 -> when (s) {
            "a","b","c" -> s
            else -> s
        }
        else -> s
    }
}

/** Heurística simple para detectar columnas COR/LOE por etiqueta. */
fun detectCorLoeIndices(headers: List<String>): Pair<Int, Int> {
    var cor = -1; var loe = -1
    headers.forEachIndexed { idx, h ->
        val x = h.stripAccentsLower().replace(" ", "")
        if (cor == -1 && (x == "cor" || x.startsWith("clase") || x.startsWith("class"))) cor = idx
        if (loe == -1 && (x == "loe" || x.startsWith("nivel") || x.startsWith("level"))) loe = idx
    }
    return cor to loe
}

/** Detección del sistema por etiquetas y/o patrones de LOE. */
fun detectRecSystem(
    headers: List<String>,
    loeColumnSamples: List<String>
): RecSystem {
    val labels = headers.map { it.stripAccentsLower() }
    val hasAccLabels = labels.any { it == "cor" } && labels.any { it == "loe" }
    val hasEscLabels = labels.any { it.startsWith("clase") || it.startsWith("class") } &&
            labels.any { it.startsWith("nivel") || it.startsWith("level") }

    if (hasAccLabels) return RecSystem.ACC_AHA_2024
    if (hasEscLabels) return RecSystem.ESC_ERS_2019

    // Mirar el contenido de LOE
    val samples = loeColumnSamples.map { it.stripAccentsLower() }
    if (samples.any { it.contains("b-r") || it.contains("bnr") || it.contains("c-ld") || it.contains("c-eo") })
        return RecSystem.ACC_AHA_2024
    if (samples.all { it.isEmpty() || it == "a" || it == "b" || it == "c" })
        return RecSystem.ESC_ERS_2019

    return RecSystem.UNKNOWN
}
