package app.simple.positional.math

import app.simple.positional.math.MathExtensions.toDegrees
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MathExtensionsTest {
    @Test
    fun normalizeAngleWrapsIntoRange() {
        assertEquals(10f, MathExtensions.normalizeAngle(370f), 1e-4f)
        assertEquals(350f, MathExtensions.normalizeAngle(-10f), 1e-4f)
        assertEquals(0f, MathExtensions.normalizeAngle(720f), 1e-4f)
        assertEquals(90f, MathExtensions.normalizeAngle(-270f), 1e-4f)
        assertEquals(180f, MathExtensions.normalizeAngle(180f), 1e-4f)
    }

    @Test
    fun roundsToPlaces() {
        assertEquals(3.14, MathExtensions.round(3.14159, 2), 1e-12)
        assertEquals(2.72, MathExtensions.round(2.7182, 2), 1e-12)
        assertEquals(-1.5, MathExtensions.round(-1.54, 1), 1e-12)
        assertEquals(90.123457, MathExtensions.round(90.1234567, 6), 1e-12)
        assertEquals(12345678.9, MathExtensions.round(12345678.94, 1), 1e-6)
        assertTrue(MathExtensions.round(1.0, -1).isNaN())
    }

    @Test
    fun convertsRadiansToDegrees() {
        assertEquals(90f, (Math.PI / 2).toFloat().toDegrees(), 1e-4f)
    }
}
