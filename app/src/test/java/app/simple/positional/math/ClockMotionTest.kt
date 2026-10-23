package app.simple.positional.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockMotionTest {
    @Test
    fun parsesStoredKeys() {
        ClockMotion.entries.forEach { assertEquals(it, ClockMotion.of(it.key)) }
        assertEquals(ClockMotion.TICK, ClockMotion.of("unknown"))
        assertEquals(ClockMotion.TICK, ClockMotion.of(null))
    }

    @Test
    fun onlyPhysicsModesAnimateSeconds() {
        assertTrue(ClockMotion.OSCILLATE.animated)
        assertTrue(ClockMotion.TICK_SMOOTH.animated)
        assertFalse(ClockMotion.TICK.fractional)
        assertEquals(0L, ClockMotion.SMOOTH.intervalMillis)
    }
}
