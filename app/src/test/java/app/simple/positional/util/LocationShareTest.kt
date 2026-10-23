package app.simple.positional.util

import app.simple.positional.ui.compose.InfoRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationShareTest {
    @Test
    fun shareTextRoundsAndLinks() {
        val text = LocationShare.text("Coordinates", 37.12345678, -76.98765432, "Somewhere")
        assertTrue(text.startsWith("Coordinates: 37.123457, -76.987654"))
        assertTrue(text.contains("query=37.123457,-76.987654"))
        assertTrue(text.contains("geo:37.123457,-76.987654?q=37.123457,-76.987654"))
        assertTrue(text.contains("Somewhere"))
    }

    @Test
    fun coordinateTextJoinsSections() {
        val text = CoordinateFormats.text(listOf("A" to listOf(InfoRow("x:", "1")), "B" to listOf(InfoRow("y"))))
        assertEquals("A\nx: 1\n\nB\ny", text)
    }
}
