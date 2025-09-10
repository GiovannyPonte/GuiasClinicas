package com.gio.guiasclinicas.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tokens de estilo SOLO para el acordeón (header, cuerpo y espaciados).
 * No toca los JSON ni el tema global Material.
 */
data class AccordionTheme(
    // Geometría / espaciados
    val cornerRadius: Dp = 16.dp,
    val itemSpacing: Dp = 6.dp,
    val headerPadH: Dp = 14.dp,
    val headerPadTop: Dp = 12.dp,
    val headerPadBottom: Dp = 10.dp,

    // Colores del header (tapa)
    val headerBg: Color = Color.Unspecified,
    val headerFg: Color = Color.Unspecified,
    val headerIconTint: Color = Color.Unspecified,

    // Colores del cuerpo (contenido)
    val bodyBg: Color = Color.Unspecified,
    val bodyFg: Color = Color.Unspecified,

    // Elevación visual
    val cardElevation: Dp = 1.dp
)

val LocalAccordionTheme = staticCompositionLocalOf { AccordionTheme() }

/** Tema por defecto derivado de MaterialTheme (coherente con light/dark). */
@Composable
fun accordionThemeFromMaterial(): AccordionTheme {
    val cs = MaterialTheme.colorScheme
    return AccordionTheme(
        cornerRadius = 16.dp,
        itemSpacing = 6.dp,
        headerPadH = 14.dp,
        headerPadTop = 12.dp,
        headerPadBottom = 10.dp,
        headerBg = cs.surfaceVariant.copy(alpha = 0.72f),
        headerFg = cs.onSurface,
        headerIconTint = cs.onSurfaceVariant,
        bodyBg = cs.surface,
        bodyFg = cs.onSurface,
        cardElevation = 1.dp
    )
}

@Composable
fun AccordionThemeProvider(
    theme: AccordionTheme = accordionThemeFromMaterial(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAccordionTheme provides theme) {
        content()
    }
}
