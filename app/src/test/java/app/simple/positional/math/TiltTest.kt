package app.simple.positional.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TiltTest {
    private val gravity = floatArrayOf(1f, 2f, 9f)

    @Test
    fun remapsToScreenAxes() {
        assertEquals(Tilt(1f, 2f, 9f), Tilt.fromDevice(gravity, 0))
        assertEquals(Tilt(-2f, 1f, 9f), Tilt.fromDevice(gravity, 1))
        assertEquals(Tilt(-1f, -2f, 9f), Tilt.fromDevice(gravity, 2))
        assertEquals(Tilt(2f, -1f, 9f), Tilt.fromDevice(gravity, 3))
    }

    @Test
    fun flatDeviceIsLevel() {
        val tilt = Tilt(0.1f, -0.1f, 9.8f)
        assertTrue(tilt.isLevel)
        assertEquals(0f, Tilt.zero.horizontal, 0f)
    }

    @Test
    fun uprightDeviceIsTilted() {
        val tilt = Tilt(0f, 9.8f, 0f)
        assertFalse(tilt.isLevel)
        assertEquals(90f, tilt.vertical, 1e-3f)
        assertEquals(0f, tilt.horizontal, 1e-3f)
    }
}
