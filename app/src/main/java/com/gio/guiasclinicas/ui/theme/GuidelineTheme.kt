
// package com.gio.guideline.render

package com.gio.guiasclinicas.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class DiagramTheme(
    val background: Color = Color(0xFFF8F8FB),
    val nodeFill: Color = Color.White,
    val nodeStroke: Color = Color(0xFFCBD5E1),
    val decisionFill: Color = Color(0xFFEFF6FF),
    val formFill: Color = Color.White,
    val scoreFill: Color = Color(0xFFF5F3FF),
    val outputFill: Color = Color(0xFFE6FFFA),
    val text: Color = Color(0xFF0F172A),
    val edgeStroke: Color = Color(0xFF64748B),
    val edgeActive: Color = Color(0xFF2563EB),
    val arrowHead: Color = Color(0xFF475569),
    val cornerRadius: Dp = 12.dp,
    val lineWidth: Dp = 2.dp
)

val DefaultDiagramTheme = DiagramTheme()
