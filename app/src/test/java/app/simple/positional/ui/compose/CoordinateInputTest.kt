package app.simple.positional.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoordinateInputTest {
    @Test
    fun parsesValidCoordinates() {
        assertEquals(12.5 to -45.25, CoordinateInput.parse("12.5", "-45.25"))
        assertEquals(90.0 to 180.0, CoordinateInput.parse(" 90 ", "180"))
        assertEquals(-90.0 to -180.0, CoordinateInput.parse("-90", "-180"))
    }

    @Test
    fun acceptsDecimalComma() {
        assertEquals(48.85 to 2.35, CoordinateInput.parse("48,85", "2,35"))
    }

    @Test
    fun rejectsOutOfRange() {
        assertNull(CoordinateInput.parse("90.01", "0"))
        assertNull(CoordinateInput.parse("0", "-180.5"))
    }

    @Test
    fun rejectsInvalidText() {
        assertNull(CoordinateInput.parse("", "10"))
        assertNull(CoordinateInput.parse("10", "abc"))
        assertNull(CoordinateInput.parse("NaN", "0"))
    }

    @Test
    fun formFillReplacesAllFields() {
        val form = CustomLocationForm()
        form.fill("Home", "1.0", "2.0")
        assertEquals("Home", form.address)
        assertEquals("1.0", form.latitude)
        assertEquals("2.0", form.longitude)
    }

    @Test
    fun formatsToSixDecimals() {
        assertEquals("48.858372", CoordinateInput.format(48.8583721))
        assertEquals("-2.5", CoordinateInput.format(-2.5))
        assertEquals("0.0", CoordinateInput.format(0.0))
    }
}
