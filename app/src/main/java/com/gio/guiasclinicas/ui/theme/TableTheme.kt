package com.gio.guiasclinicas.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Tema base para tablas
data class TableTheme(
    val cornerRadiusDp: Float = 14f,
    val borderWidthDp: Float = 0.6f,
    val headerBg: Color = Color(0xFFF3F4F6),
    val cellBg: Color = Color.White,
    val cellBorder: Color = Color(0xFFE5E7EB),
    val groupLabelColor: Color = Color(0xFF1D4ED8),
    val cellPaddingH: Float = 10f,
    val cellPaddingV: Float = 8f,
    val cellMinHeightDp: Float = 40f,
    val textMaxSp: Float = 14f,
    val textMinSp: Float = 10f,
    val operatorTextSp: Float = 12f,

    // Colores especiales para tablas "Recomendacion" (col 0: COR, col 1: LOE)
    val recFirstColBg: Color = Color(0xFF4CAF50),   // COR: verde muy suave
    val recSecondColBg: Color = Color(0xFF1141D2),  // LOE: azul muy suave
    val recHeaderFirstBg: Color = Color(0xFFDBEFE0),
    val recHeaderSecondBg: Color = Color(0xFFD7EAFB)
)

// CompositionLocal + Provider
val LocalTableTheme = staticCompositionLocalOf { TableTheme() }

@Composable
fun TableThemeProvider(
    theme: TableTheme = TableTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalTableTheme provides theme) { content() }
}
