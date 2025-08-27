package com.gio.guiasclinicas

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import com.gio.guiasclinicas.ui.components.buildHighlighted

class HighlightedTextTest {
    @Test
    fun overlapping_ranges_do_not_duplicate_characters() {
        val result = buildHighlighted("aaa", listOf(0..1, 1..2), null)
        // texto completo sin duplicados
        assertEquals("aaa", result.text)
        assertEquals(3, result.text.length)

        // dos zonas amarillas
        assertEquals(2, result.spanStyles.size)
        val yellow = Color(0xFFFFF59D)
        for (span in result.spanStyles) {
            assertEquals(yellow, span.item.background)
        }
        assertEquals(0, result.spanStyles[0].start)
        assertEquals(2, result.spanStyles[0].end)
        assertEquals(2, result.spanStyles[1].start)
        assertEquals(3, result.spanStyles[1].end)
    }
}
