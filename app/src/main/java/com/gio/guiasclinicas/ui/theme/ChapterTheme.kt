package com.gio.guiasclinicas.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class ChapterThemeSpec(
    // Colores
    val bodyCardBg: Color,
    val bodyCardOnBg: Color,
    val footnoteText: Color,
    // Medidas / tipografía
    val bodyCornerRadiusDp: Int = 12,
    val bodyPaddingH: Int = 12,
    val bodyPaddingV: Int = 10,
    val bodyTextMaxSp: Int = 16,
    val bodyLineHeightSp: Int = 22
)

val LocalChapterTheme = staticCompositionLocalOf {
    // Fallback neutro (no depende del esquema)
    ChapterThemeSpec(
        bodyCardBg = Color(0xFFF6F8FB),
        bodyCardOnBg = Color(0xFF1F2937),
        footnoteText = Color(0xFF475569)
    )
}

@Composable
fun ChapterTheme(
    spec: ChapterThemeSpec = ChapterThemeSpec(
        bodyCardBg      = MaterialTheme.colorScheme.surfaceVariant,
        bodyCardOnBg    = MaterialTheme.colorScheme.onSurface,
        footnoteText    = MaterialTheme.colorScheme.onSurfaceVariant
    ),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalChapterTheme provides spec) {
        content()
    }
}

