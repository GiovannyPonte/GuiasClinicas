package com.gio.guiasclinicas.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

data class RecTableThemeSpec(
    val stripeColor: Color,
    val corBg: Color,
    val corOnBg: Color,
    val loeBg: Color,
    val loeOnBg: Color,
    val headerBg: Color,
    val headerOnBg: Color,
    val cellBg: Color,
    val cellOnBg: Color,
    val cellBorder: Color,
    // medidas (int -> dp/sp en el renderer)
    val borderWidthDp: Int = 1,
    val cellPaddingH: Int = 8,
    val cellPaddingV: Int = 8,
    val cellMinHeightDp: Int = 40,
    val textMaxSp: Int = 14,
    val textMinSp: Int = 11,
)

val LocalRecTableTheme = staticCompositionLocalOf {
    RecTableThemeSpec(
        stripeColor = Color(0xFF0B57D0),      // azul acento (fallback)
        corBg = Color(0xFF185ABC),            // píldora COR (fallback)
        corOnBg = Color.White,
        loeBg = Color(0xFF006D3C),            // píldora LOE (fallback)
        loeOnBg = Color.White,
        headerBg = Color(0xFFE0E3E7),
        headerOnBg = Color(0xFF1F1F1F),
        cellBg = Color.White,
        cellOnBg = Color(0xFF1F1F1F),
        cellBorder = Color(0x33000000),
    )
}
@Composable
fun RecommendationTableTheme(
    spec: RecTableThemeSpec = RecTableThemeSpec(
        stripeColor = MaterialTheme.colorScheme.primary,
        corBg = Color(0xFF0B57D0),            // Azul fuerte (Columna 1)
        corOnBg = Color.White,                // Texto blanco
        loeBg = Color(0xFF2E7D32),            // Verde fuerte (Columna 2)
        loeOnBg = Color.White,                // Texto blanco
        headerBg = MaterialTheme.colorScheme.surfaceVariant,
        headerOnBg = MaterialTheme.colorScheme.onSurfaceVariant,
        cellBg = MaterialTheme.colorScheme.surface,
        cellOnBg = MaterialTheme.colorScheme.onSurface,
        cellBorder = MaterialTheme.colorScheme.outlineVariant
    ),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalRecTableTheme provides spec) {
        content()
    }
}

