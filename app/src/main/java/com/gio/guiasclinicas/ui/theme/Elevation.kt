// ui/theme/Elevation.kt
package com.gio.guiasclinicas.ui.theme

import androidx.compose.ui.unit.dp

object AppElevation {
    val none = 0.dp
    val xs   = 1.dp   // separaciones sutiles
    val sm   = 2.dp   // cards/menus base
    val md   = 4.dp   // hover/pressed
    val lg   = 8.dp   // sheets/modals notorios
    val xl   = 12.dp  // diálogos/menús principales
}

object AppBorderAlpha {
    const val subtle  = 0.25f // contorno suave
    const val medium  = 0.35f // tablas/bloques
    const val strong  = 0.50f // énfasis fuerte/control
}
