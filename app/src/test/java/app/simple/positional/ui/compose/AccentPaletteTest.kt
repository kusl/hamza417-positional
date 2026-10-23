package app.simple.positional.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentPaletteTest {
    private val red = AccentOption(0xFFFF0000.toInt(), "Red")
    private val dynamic = AccentOption(0xFFFF0000.toInt(), AccentPalette.MATERIAL_YOU, dynamic = true)

    @Test
    fun staticColorSelectedByValue() {
        assertTrue(AccentPalette.isSelected(red, red.color, materialYou = false))
        assertFalse(AccentPalette.isSelected(red, 0xFF00FF00.toInt(), materialYou = false))
    }

    @Test
    fun dynamicSelectedOnlyWhenMaterialYou() {
        assertTrue(AccentPalette.isSelected(dynamic, 0, materialYou = true))
        assertFalse(AccentPalette.isSelected(red, red.color, materialYou = true))
        assertFalse(AccentPalette.isSelected(dynamic, dynamic.color, materialYou = false))
    }

    @Test
    fun paletteNamesAndColorsAreUnique() {
        assertEquals(23, AccentPalette.colors.size)
        assertEquals(AccentPalette.colors.size, AccentPalette.colors.map { it.first }.toSet().size)
        assertEquals(AccentPalette.colors.size, AccentPalette.colors.map { it.second }.toSet().size)
    }
}
