package com.gio.guiasclinicas.ui.components.table

import androidx.compose.runtime.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.Text
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.AnnotatedString

@Composable
fun AutoResizeText(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle,
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
) {
    val measurer = rememberTextMeasurer()
    var fontSize by remember(text) { mutableStateOf(maxFontSize) }

    BoxWithConstraints(modifier = modifier) {
        val widthPx = constraints.maxWidth
        val heightPx = constraints.maxHeight

        // Ajuste de tamaño trabajando con floats (sp) para evitar -= y max() con TextUnit
        LaunchedEffect(text, widthPx, heightPx, maxFontSize, minFontSize, style, maxLines) {
            val maxSp = maxFontSize.value
            val minSp = minFontSize.value
            var currentSp = maxSp
            val stepSp = 1f

            while (currentSp >= minSp) {
                val result = measurer.measure(
                    text = AnnotatedString(text),
                    style = style.copy(fontSize = currentSp.sp),
                    maxLines = maxLines,
                    overflow = TextOverflow.Clip
                )
                if (result.size.width <= widthPx && result.size.height <= heightPx) {
                    break
                }
                currentSp -= stepSp
            }

            if (currentSp < minSp) currentSp = minSp
            fontSize = currentSp.sp
        }

        Text(
            text = text,
            style = style.copy(fontSize = fontSize),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}
