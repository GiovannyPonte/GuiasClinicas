package com.gio.guiasclinicas.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

@Composable
fun AutoResizeTitleText(
    text: String,
    maxLines: Int = 2,
    maxTextSizeSp: Float = 22f,
    minTextSizeSp: Float = 14f,
    stepFactor: Float = 0.92f,
    modifier: Modifier = Modifier
) {
    val baseStyle = MaterialTheme.typography.titleLarge
    var textStyle by remember(maxTextSizeSp, baseStyle) {
        mutableStateOf(baseStyle.copy(fontSize = maxTextSizeSp.sp))
    }
    var ready by remember { mutableStateOf(false) }

    Text(
        text = text,
        style = textStyle,
        maxLines = maxLines,
        softWrap = true,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { result ->
            if (!ready) {
                if (result.didOverflowHeight || result.didOverflowWidth) {
                    val next = (textStyle.fontSize.value * stepFactor).coerceAtLeast(minTextSizeSp)
                    if (next == textStyle.fontSize.value) ready = true
                    else textStyle = textStyle.copy(fontSize = next.sp)
                } else {
                    ready = true
                }
            }
        },
        modifier = modifier
    )
}
