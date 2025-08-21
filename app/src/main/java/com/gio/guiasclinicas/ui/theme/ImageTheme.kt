package com.gio.guiasclinicas.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Blanco "hueso" elegante
val BoneWhite = Color(0xFFF7F3ED)

enum class FigureCaptionPlacement { Top, Bottom }

data class ImageThemeSpec(
    val containerBg: Color = BoneWhite,              // fondo visible tras PNGs con transparencia
    val cornerRadiusDp: Int = 12,                    // radios del contenedor
    val captionPlacement: FigureCaptionPlacement = FigureCaptionPlacement.Bottom, // << NUEVO
    val captionSpacingDp: Int = 6                    // separación entre imagen y caption
)

val LocalImageTheme = staticCompositionLocalOf { ImageThemeSpec() }

@Composable
fun ImageContainerTheme(
    spec: ImageThemeSpec = ImageThemeSpec(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalImageTheme provides spec) {
        content()
    }
}
