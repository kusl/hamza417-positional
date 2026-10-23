package app.simple.positional.math

import org.junit.Assert.assertEquals
import org.junit.Test

class CompassAzimuthTest {
    private val gravity = Vector3(0f, 0f, 9.81f)

    @Test
    fun deviceFacingNorth() {
        assertEquals(0f, CompassAzimuth.calculate(gravity, Vector3(0f, 20f, -40f)), 1e-3f)
    }

    @Test
    fun deviceFacingWest() {
        assertEquals(270f, CompassAzimuth.calculate(gravity, Vector3(20f, 0f, -40f)), 1e-3f)
    }

    @Test
    fun degenerateInputReturnsZero() {
        assertEquals(0f, CompassAzimuth.calculate(Vector3.zero, Vector3.zero), 0f)
    }

    @Test
    fun inverseAddsHalfTurn() {
        assertEquals(190f, CompassAzimuth.inverse(10f), 0f)
    }

    @Test
    fun adjustsForDisplayRotation() {
        assertEquals(100f, CompassAzimuth.adjustForRotation(100f, 0), 0f)
        assertEquals(-170f, CompassAzimuth.adjustForRotation(100f, 1), 0f)
        assertEquals(-80f, CompassAzimuth.adjustForRotation(100f, 2), 0f)
        assertEquals(10f, CompassAzimuth.adjustForRotation(100f, 3), 0f)
    }
}
