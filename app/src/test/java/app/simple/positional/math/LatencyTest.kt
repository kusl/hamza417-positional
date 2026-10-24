package app.simple.positional.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LatencyTest {
    @Test
    fun shortGapsStayInMilliseconds() {
        val latency = Latency(850)
        assertFalse(latency.inSeconds)
        assertEquals(850L, latency.value)
        assertFalse(latency.isLate)
    }

    @Test
    fun longGapsSwitchToSeconds() {
        val latency = Latency(1234)
        assertTrue(latency.inSeconds)
        assertEquals(1.234, latency.value.toDouble(), 1e-9)
        assertFalse(latency.isLate)
    }

    @Test
    fun gapsOverFiveSecondsAreLate() {
        assertTrue(Latency(5001).isLate)
        assertFalse(Latency(5000).isLate)
    }
}
