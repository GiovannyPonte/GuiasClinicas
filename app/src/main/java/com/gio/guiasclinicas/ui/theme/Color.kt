// ui/theme/Color.kt
package com.gio.guiasclinicas.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ===== Paletas clásicas =====
private object Azul {
    val primary = Color(0xFF1E40AF)
    val onPrimary = Color.White
    val secondary = Color(0xFF0EA5E9)
    val tertiary = Color(0xFF10B981)
    val surface = Color(0xFFF7F8FC)
    val onSurface = Color(0xFF1A1C1E)
    val surfaceVariant = Color(0xFFE6E9F2)
    val onSurfaceVariant = Color(0xFF3F4757)
    val outline = Color(0xFFB7C0D0)
}

private object Verde {
    val primary = Color(0xFF059669)
    val onPrimary = Color.White
    val secondary = Color(0xFF0EA5E9)
    val tertiary = Color(0xFF10B981)
    val surface = Color(0xFFF6FBF8)
    val onSurface = Color(0xFF16201D)
    val surfaceVariant = Color(0xFFE3F2EC)
    val onSurfaceVariant = Color(0xFF2E3E38)
    val outline = Color(0xFFA2D1C3)
}

private object Neutro {
    val primary = Color(0xFF374151)
    val onPrimary = Color.White
    val secondary = Color(0xFF0EA5E9)
    val tertiary = Color(0xFF9CA3AF)
    val surface = Color(0xFFF6F7FA)
    val onSurface = Color(0xFF14171C)
    val surfaceVariant = Color(0xFFE8EBF1)
    val onSurfaceVariant = Color(0xFF404651)
    val outline = Color(0xFFC6CBD4)
}

// ===== Brand moderno: CARDIOATRIA (light/dark) =====
private object CardioAtriaLight {
    val primary            = Color(0xFF143B8C)
    val onPrimary          = Color(0xFFFFFFFF)
    val primaryContainer   = Color(0xFFD8E1FF)
    val onPrimaryContainer = Color(0xFF0A1B52)

    val secondary            = Color(0xFF00A0C6)
    val onSecondary          = Color(0xFF001F27)
    val secondaryContainer   = Color(0xFFC6F2FF)
    val onSecondaryContainer = Color(0xFF002B36)

    val tertiary            = Color(0xFF12BFAE)
    val onTertiary          = Color(0xFF072018)
    val tertiaryContainer   = Color(0xFFBFF3E6)
    val onTertiaryContainer = Color(0xFF072018)

    val surface          = Color(0xFFF7F9FC)
    val onSurface        = Color(0xFF111827)
    val surfaceVariant   = Color(0xFFE7EDF7)
    val onSurfaceVariant = Color(0xFF3E4A5E)
    val outline          = Color(0xFFC6D2E6)
    val outlineVariant   = Color(0xFFB5C3DB)

    val error   = Color(0xFFBA1A1A)
    val onError = Color(0xFFFFFFFF)
}

private object CardioAtriaDark {
    val primary            = Color(0xFF93B1F2)
    val onPrimary          = Color(0xFF0A1226)
    val primaryContainer   = Color(0xFF223061)
    val onPrimaryContainer = Color(0xFFD8E1FF)

    val secondary            = Color(0xFF60D2EB)
    val onSecondary          = Color(0xFF003642)
    val secondaryContainer   = Color(0xFF174B56)
    val onSecondaryContainer = Color(0xFFC6F2FF)

    val tertiary            = Color(0xFF66E6D8)
    val onTertiary          = Color(0xFF0A2A21)
    val tertiaryContainer   = Color(0xFF184C3E)
    val onTertiaryContainer = Color(0xFFBFF3E6)

    val surface          = Color(0xFF0B1220)
    val onSurface        = Color(0xFFE6E9F2)
    val surfaceVariant   = Color(0xFF162032)
    val onSurfaceVariant = Color(0xFFBAC3D6)
    val outline          = Color(0xFF2F3A50)
    val outlineVariant   = Color(0xFF405172)

    val error   = Color(0xFFFFB4AB)
    val onError = Color(0xFF690005)
}
// ========= AtriaMed 2.0 – LIGHT =========
private object AtriaMed2Light {
    // Azules clínicos y cian técnico con verdes bienestar
    val primary            = Color(0xFF1843D2) // cobalto moderno (ligeramente más vivo)
    val onPrimary          = Color(0xFFFFFFFF)
    val primaryContainer   = Color(0xFFDCE4FF)
    val onPrimaryContainer = Color(0xFF0A1C55)

    val secondary            = Color(0xFF009EC8) // cian técnico
    val onSecondary          = Color(0xFF00222B)
    val secondaryContainer   = Color(0xFFC5F2FF)
    val onSecondaryContainer = Color(0xFF002F3A)

    val tertiary            = Color(0xFF10B589) // teal bienestar (ligeramente más sobrio)
    val onTertiary          = Color(0xFF052019)
    val tertiaryContainer   = Color(0xFFBCEFE0)
    val onTertiaryContainer = Color(0xFF092E24)

    val surface          = Color(0xFFF7F9FC)   // superficie de lectura
    val onSurface        = Color(0xFF111827)
    val surfaceVariant   = Color(0xFFEBF0FA)   // contenedores secundarios/encabezados
    val onSurfaceVariant = Color(0xFF3D485B)

    val outline          = Color(0xFFC6D2E6)   // bordes
    val outlineVariant   = Color(0xFFB3C2DA)   // bordes más notorios
    val scrim            = Color(0x66000000)

    val error            = Color(0xFFB3261E)
    val onError          = Color(0xFFFFFFFF)
    val errorContainer   = Color(0xFFF9DEDC)
    val onErrorContainer = Color(0xFF410E0B)
}

// ========= AtriaMed 2.0 – DARK =========
private object AtriaMed2Dark {
    val primary            = Color(0xFFAEC2FF)
    val onPrimary          = Color(0xFF0A1C55)
    val primaryContainer   = Color(0xFF223063)
    val onPrimaryContainer = Color(0xFFDCE4FF)

    val secondary            = Color(0xFF61D6EE)
    val onSecondary          = Color(0xFF003743)
    val secondaryContainer   = Color(0xFF1A4C57)
    val onSecondaryContainer = Color(0xFFC5F2FF)

    val tertiary            = Color(0xFF7FE3C8)
    val onTertiary          = Color(0xFF0B2A22)
    val tertiaryContainer   = Color(0xFF1A4C3D)
    val onTertiaryContainer = Color(0xFFBCEFE0)

    val surface          = Color(0xFF0B1220)
    val onSurface        = Color(0xFFE7EAF3)
    val surfaceVariant   = Color(0xFF161F33)
    val onSurfaceVariant = Color(0xFFBAC5DA)

    val outline          = Color(0xFF2F3A50)
    val outlineVariant   = Color(0xFF405172)
    val scrim            = Color(0x99000000)

    val error            = Color(0xFFFFB4A9)
    val onError          = Color(0xFF680003)
    val errorContainer   = Color(0xFF930006)
    val onErrorContainer = Color(0xFFFFDAD4)
}

/** ÚNICA función usada por Theme.kt */
fun colorSchemeFor(brand: Brand, darkTheme: Boolean = false): ColorScheme =
    when (brand) {
        // Si tu enum tiene ambos nombres, cúbrelos aquí; si no usas AtriaMed, puedes quitarlo.
        Brand.CardioAtria, Brand.AtriaMed ->
            if (!darkTheme) {
                lightColorScheme(
                    primary = CardioAtriaLight.primary, onPrimary = CardioAtriaLight.onPrimary,
                    primaryContainer = CardioAtriaLight.primaryContainer, onPrimaryContainer = CardioAtriaLight.onPrimaryContainer,
                    secondary = CardioAtriaLight.secondary, onSecondary = CardioAtriaLight.onSecondary,
                    secondaryContainer = CardioAtriaLight.secondaryContainer, onSecondaryContainer = CardioAtriaLight.onSecondaryContainer,
                    tertiary = CardioAtriaLight.tertiary, onTertiary = CardioAtriaLight.onTertiary,
                    tertiaryContainer = CardioAtriaLight.tertiaryContainer, onTertiaryContainer = CardioAtriaLight.onTertiaryContainer,
                    surface = CardioAtriaLight.surface, onSurface = CardioAtriaLight.onSurface,
                    surfaceVariant = CardioAtriaLight.surfaceVariant, onSurfaceVariant = CardioAtriaLight.onSurfaceVariant,
                    outline = CardioAtriaLight.outline, outlineVariant = CardioAtriaLight.outlineVariant,
                    error = CardioAtriaLight.error, onError = CardioAtriaLight.onError
                )
            } else {
                darkColorScheme(
                    primary = CardioAtriaDark.primary, onPrimary = CardioAtriaDark.onPrimary,
                    primaryContainer = CardioAtriaDark.primaryContainer, onPrimaryContainer = CardioAtriaDark.onPrimaryContainer,
                    secondary = CardioAtriaDark.secondary, onSecondary = CardioAtriaDark.onSecondary,
                    secondaryContainer = CardioAtriaDark.secondaryContainer, onSecondaryContainer = CardioAtriaDark.onSecondaryContainer,
                    tertiary = CardioAtriaDark.tertiary, onTertiary = CardioAtriaDark.onTertiary,
                    tertiaryContainer = CardioAtriaDark.tertiaryContainer, onTertiaryContainer = CardioAtriaDark.onTertiaryContainer,
                    surface = CardioAtriaDark.surface, onSurface = CardioAtriaDark.onSurface,
                    surfaceVariant = CardioAtriaDark.surfaceVariant, onSurfaceVariant = CardioAtriaDark.onSurfaceVariant,
                    outline = CardioAtriaDark.outline, outlineVariant = CardioAtriaDark.outlineVariant,
                    error = CardioAtriaDark.error, onError = CardioAtriaDark.onError
                )
            }

        Brand.AzulProfesional -> lightColorScheme(
            primary = Azul.primary, onPrimary = Azul.onPrimary,
            secondary = Azul.secondary, tertiary = Azul.tertiary,
            surface = Azul.surface, onSurface = Azul.onSurface,
            surfaceVariant = Azul.surfaceVariant, onSurfaceVariant = Azul.onSurfaceVariant,
            outline = Azul.outline
        )

        Brand.VerdeSalud -> lightColorScheme(
            primary = Verde.primary, onPrimary = Verde.onPrimary,
            secondary = Verde.secondary, tertiary = Verde.tertiary,
            surface = Verde.surface, onSurface = Verde.onSurface,
            surfaceVariant = Verde.surfaceVariant, onSurfaceVariant = Verde.onSurfaceVariant,
            outline = Verde.outline
        )

        Brand.NeutroElegante -> lightColorScheme(
            primary = Neutro.primary, onPrimary = Neutro.onPrimary,
            secondary = Neutro.secondary, tertiary = Neutro.tertiary,
            surface = Neutro.surface, onSurface = Neutro.onSurface,
            surfaceVariant = Neutro.surfaceVariant, onSurfaceVariant = Neutro.onSurfaceVariant,
            outline = Neutro.outline
        )

        Brand.Dinamico -> lightColorScheme() // el dynamic real se resuelve en Theme.kt
    }
