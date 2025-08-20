// app/src/main/java/com/gio/guiasclinicas/ui/components/table/AutoResizeText.kt
package com.gio.guiasclinicas.ui.components.table

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Ajusta el tamaño de fuente para que el texto quepa dentro de un ancho máximo (px),
 * priorizando multilínea y sin elipsis. Usa búsqueda binaria entre min y max font size.
 */
@Composable
fun AutoResizeText(
    text: String,
    maxWidthPx: Int,
    style: TextStyle,
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    maxLines: Int = Int.MAX_VALUE,
    stepGranularitySp: Float = 0.5f,
    modifier: Modifier = Modifier
) {
    val measurer = rememberTextMeasurer()

    val fittedSize = remember(
        text, maxWidthPx, maxLines, maxFontSize, minFontSize,
        style.fontFamily, style.fontWeight, style.letterSpacing, style.lineHeight
    ) {
        fun fits(sizeSp: Float): Boolean {
            val result = measurer.measure(
                text = AnnotatedString(text),
                style = style.copy(fontSize = sizeSp.sp),
                maxLines = maxLines,
                overflow = TextOverflow.Clip,   // sin elipsis
                softWrap = true,
                constraints = Constraints(maxWidth = maxWidthPx.coerceAtLeast(1))
            )
            return !result.hasVisualOverflow   // <-- return agregado
        }

        // Búsqueda binaria del mayor tamaño que cabe
        var low = minFontSize.value
        var high = maxFontSize.value
        var best = low
        var guard = 0

        while (high - low > stepGranularitySp && guard < 40) {
            guard++
            val mid = (low + high) / 2f
            if (fits(mid)) {
                best = mid
                low = mid
            } else {
                high = mid
            }
        }
        best.sp
    }

    Text(
        modifier = modifier,
        text = text,
        style = style.copy(fontSize = fittedSize),
        maxLines = maxLines,
        overflow = TextOverflow.Clip,
        softWrap = true
    )
}
