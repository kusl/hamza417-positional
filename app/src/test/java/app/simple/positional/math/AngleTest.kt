package app.simple.positional.math

import app.simple.positional.math.Angle.normalizeEulerAngle
import org.junit.Assert.assertEquals
import org.junit.Test

class AngleTest {
    @Test
    fun normalizesEulerAngles() {
        assertEquals(10f, 370f.normalizeEulerAngle(false), 1e-4f)
        assertEquals(270f, (-90f).normalizeEulerAngle(false), 1e-4f)
        assertEquals(-90f, (-90f).normalizeEulerAngle(true), 1e-4f)
    }

    @Test
    fun touchAngleIsMeasuredClockwiseFromTop() {
        assertEquals(90f, Angle.getAngle(100.0, 50.0, 100f, 100f), 1e-3f)
        assertEquals(180f, Angle.getAngle(50.0, 100.0, 100f, 100f), 1e-3f)
        assertEquals(270f, Angle.getAngle(0.0, 50.0, 100f, 100f), 1e-3f)
    }
}
