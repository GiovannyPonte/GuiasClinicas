package com.gio.guiasclinicas.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

fun typographyFor(@Suppress("UNUSED_PARAMETER") typo: Typo): Typography {
    val fam = FontFamily.SansSerif // cambia por tu FontFamily Inter si ya la añadiste
    return Typography(
        titleLarge  = TextStyle(
            fontFamily = fam,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 22.sp,
            lineHeight = 28.sp
        ),
        titleMedium = TextStyle(
            fontFamily = fam,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 18.sp,
            lineHeight = 24.sp
        ),
        bodyLarge   = TextStyle(
            fontFamily = fam,
            fontWeight = FontWeight.Normal,
            fontSize   = 16.sp,
            lineHeight = 22.sp
        ),
        bodyMedium  = TextStyle(
            fontFamily = fam,
            fontWeight = FontWeight.Normal,
            fontSize   = 14.sp,
            lineHeight = 20.sp
        ),
        labelLarge  = TextStyle(
            fontFamily = fam,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 13.sp,
            lineHeight = 18.sp
        ),
    )
}
