package com.gio.guiasclinicas.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun shapesFor(style: ShapeStyle): Shapes = when (style) {
    ShapeStyle.Mixto -> Shapes(
        small = RoundedCornerShape(8),
        medium = RoundedCornerShape(16),
        large = RoundedCornerShape(24),
    )
    ShapeStyle.Recto -> Shapes(
        small = RoundedCornerShape(4),
        medium = RoundedCornerShape(6),
        large = RoundedCornerShape(8),
    )
    ShapeStyle.Redondeado -> Shapes(
        small = RoundedCornerShape(12),
        medium = RoundedCornerShape(16),
        large = RoundedCornerShape(28),
    )
}

@Composable
fun GuiasClinicasTheme(
    brand: Brand,
    typo: Typo,
    shapeStyle: ShapeStyle,
    dynamicColor: Boolean,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme =
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && brand == Brand.Dinamico) {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        } else {
            colorSchemeFor(brand,darkTheme)
        }

    val typography = typographyFor(typo)
    val shapes = shapesFor(shapeStyle)

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = typography, shapes = shapes) {
        content()
    }
}

// Sobrecarga compatible si en algún sitio llamas sin DesignSystem
@Composable
fun GuiasClinicasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    GuiasClinicasTheme(
        brand = if (dynamicColor) Brand.Dinamico else Brand.AzulProfesional,
        typo = Typo.Inter,
        shapeStyle = ShapeStyle.Mixto,
        dynamicColor = dynamicColor,
        darkTheme = darkTheme,
        content = content
    )
}
