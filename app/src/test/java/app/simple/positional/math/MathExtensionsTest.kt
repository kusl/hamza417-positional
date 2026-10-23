package app.simple.positional.math

import app.simple.positional.math.MathExtensions.round
import app.simple.positional.math.MathExtensions.toDegrees
import app.simple.positional.math.MathExtensions.toNegative
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MathExtensionsTest {
    @Test
    fun normalizeAngleWrapsIntoRange() {
        assertEquals(10f, MathExtensions.normalizeAngle(370f), 1e-4f)
        assertEquals(350f, MathExtensions.normalizeAngle(-10f), 1e-4f)
        assertEquals(0f, MathExtensions.normalizeAngle(720f), 1e-4f)
        assertEquals(90.0, MathExtensions.normalizeAngle(-270.0), 1e-9)
    }

    @Test
    fun trigonometryUsesDegrees() {
        assertEquals(1.0, MathExtensions.sinDegrees(90.0), 1e-9)
        assertEquals(-1.0, MathExtensions.cosDegrees(180.0), 1e-9)
        assertEquals(1.0, MathExtensions.tanDegrees(45.0), 1e-9)
        assertEquals(1f, MathExtensions.tanDegrees(45f), 1e-5f)
    }

    @Test
    fun clampLimitsValue() {
        assertEquals(5f, MathExtensions.clamp(10f, 0f, 5f), 0f)
        assertEquals(0f, MathExtensions.clamp(-3f, 0f, 5f), 0f)
        assertEquals(2.5f, MathExtensions.clamp(2.5f, 0f, 5f), 0f)
    }

    @Test
    fun roundingAndConversions() {
        assertEquals(3.14, MathExtensions.round(3.14159, 2), 1e-12)
        assertTrue(MathExtensions.round(1.0, -1).isNaN())
        assertEquals(2.7f, 2.7182f.round(1), 1e-6f)
        assertEquals(180.0, Math.PI.toDegrees(), 1e-9)
        assertEquals(90f, (Math.PI / 2).toFloat().toDegrees(), 1e-4f)
        assertEquals(-4f, 4f.toNegative(), 0f)
        assertEquals(-4f, 4.toNegative(), 0f)
    }
}
