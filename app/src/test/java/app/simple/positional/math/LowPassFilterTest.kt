package app.simple.positional.math

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class LowPassFilterTest {
    @Test
    fun blendsReadingsByAlpha() {
        val readings = floatArrayOf(0f, 10f, 20f)
        LowPassFilter.smoothAndSetReadings(readings, floatArrayOf(10f, 20f, 40f), 0.5f)
        assertArrayEquals(floatArrayOf(5f, 15f, 30f), readings, 1e-6f)
    }

    @Test
    fun alphaOneReplacesReadings() {
        val readings = floatArrayOf(1f, 2f, 3f)
        LowPassFilter.smoothAndSetReadings(readings, floatArrayOf(7f, 8f, 9f), 1f)
        assertArrayEquals(floatArrayOf(7f, 8f, 9f), readings, 0f)
    }
}
