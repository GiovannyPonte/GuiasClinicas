package com.gio.guiasclinicas.ui.components.table

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Versión optimizada:
 *  - Preprocesa texto con puntos de corte suaves («soft hyphen», ZWSP) para evitar cortes feos.
 *  - Máx. 3 mediciones: max, mid, min. (nada de búsqueda binaria larga).
 *  - Respeta lineBreak y Hyphens para mejor envoltura.
 */
@Composable
fun AutoResizeText(
    text: String,
    maxWidthPx: Int,
    style: TextStyle,
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    maxLines: Int = Int.MAX_VALUE,
    // mantenemos parámetros para compatibilidad (no usados)
    stepGranularitySp: Float = 0.5f,
    modifier: Modifier = Modifier
) {
    val measurer = rememberTextMeasurer()

    // Prepara cortes suaves: smart soft-hyphen + ZWSP en separadores
    val prepared = remember(text) { SmartBreaks.prepareEs(text) }

    // Style con reglas de corte más legibles
    val baseStyle = style.copy(
        lineBreak = LineBreak.Paragraph, // permite cortes de palabra más naturales
        hyphens = Hyphens.Auto           // usa \u00AD si es necesario
    )

    fun fits(sizeSp: Float): Boolean {
        val result = measurer.measure(
            text = AnnotatedString(prepared),
            style = baseStyle.copy(fontSize = sizeSp.sp),
            maxLines = maxLines,
            overflow = TextOverflow.Clip,
            softWrap = true,
            constraints = Constraints(maxWidth = maxWidthPx.coerceAtLeast(1))
        )
        return !result.hasVisualOverflow
    }

    val targetSp = remember(
        prepared, maxWidthPx, maxLines, maxFontSize, minFontSize,
        baseStyle.fontFamily, baseStyle.fontWeight, baseStyle.letterSpacing, baseStyle.lineHeight
    ) {
        val maxSp = maxFontSize.value
        val minSp = minFontSize.value
        val midSp = ((maxSp + minSp) / 2f).coerceAtMost(maxSp - 1f)

        when {
            fits(maxSp) -> maxSp         // cabe con fuente normal
            fits(midSp) -> midSp         // una sola reducción
            else        -> minSp         // último recurso
        }.sp
    }

    Text(
        modifier = modifier,
        text = prepared,
        style = baseStyle.copy(fontSize = targetSp),
        maxLines = maxLines,
        overflow = TextOverflow.Clip,
        softWrap = true
    )
}
