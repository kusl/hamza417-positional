package app.simple.positional.constants

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationPinsTest {
    @Test
    fun unknownSkinFallsBackToFirst() {
        assertEquals(LocationPins.locationsPins[0], LocationPins.pin(-1))
        assertEquals(LocationPins.locationsPins[0], LocationPins.pin(LocationPins.locationsPins.size))
        assertEquals(LocationPins.locationsPins[3], LocationPins.pin(3))
    }

    @Test
    fun fillIsTranslucentStroke() {
        LocationPins.locationsPins.indices.forEach { skin ->
            val fill = LocationPins.fillColor(skin)
            val stroke = LocationPins.strokeColor(skin)
            assertEquals(0x4D, fill ushr 24)
            assertEquals(0xFF, stroke ushr 24)
            assertEquals(stroke and 0xFFFFFF, fill and 0xFFFFFF)
        }
    }

    @Test
    fun firstSkinKeepsOriginalColors() {
        assertEquals(0xFF1B9CFF.toInt(), LocationPins.strokeColor(0))
        assertEquals(0x4D1B9CFF, LocationPins.fillColor(0))
        assertEquals(LocationPins.strokeColor(0), LocationPins.strokeColor(99))
    }
}
