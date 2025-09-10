package com.gio.guiasclinicas.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

enum class Brand { AzulProfesional, VerdeSalud, NeutroElegante, Dinamico,CardioAtria,AtriaMed  }
enum class Typo { Inter, Roboto }
enum class ShapeStyle { Mixto, Recto, Redondeado }
// + añade "CardioAtria" al enum Brand


data class DesignConfig(
    val brand: Brand = Brand.AzulProfesional,
    val typo: Typo = Typo.Inter,
    val shapes: ShapeStyle = ShapeStyle.Mixto,
    val useDynamicColor: Boolean = false
)

val LocalDesignConfig = compositionLocalOf { DesignConfig() }

@Composable
fun AppDesignSystem(
    config: DesignConfig = DesignConfig(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalDesignConfig provides config) {
        GuiasClinicasTheme(
            brand = config.brand,
            typo = config.typo,
            shapeStyle = config.shapes,
            dynamicColor = config.useDynamicColor,
            content = content
        )
    }
}
