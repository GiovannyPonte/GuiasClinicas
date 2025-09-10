package com.gio.guiasclinicas.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class ChapterSectionTheme(
    // Forma y espaciados del contenedor
    val containerShape: RoundedCornerShape = RoundedCornerShape(20.dp),
    val containerPadH: Int = 12,              // padding interno horizontal (dp)
    val containerPadV: Int = 10,              // padding interno vertical (dp)

    // Padding específico del header: separa el título de la curva
    val headerPadStart: Int = 16,
    val headerPadTop: Int = 14,               // ← clave para que no se corte
    val headerPadEnd: Int = 12,
    val headerPadBottom: Int = 10,

    // Bloque informativo (resumen, etc.)
    val infoBg: Color = Color.Unspecified,    // se resuelve desde MaterialTheme si queda Unspecified
    val infoFg: Color = Color.Unspecified,
    val infoShape: RoundedCornerShape = RoundedCornerShape(12.dp)
)

val LocalChapterSectionTheme = staticCompositionLocalOf { ChapterSectionTheme() }

@Composable
fun ChapterSectionThemeProvider(
    theme: ChapterSectionTheme = ChapterSectionTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalChapterSectionTheme provides theme, content = content)
}
