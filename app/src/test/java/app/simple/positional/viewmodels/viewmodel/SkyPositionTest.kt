package app.simple.positional.viewmodels.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SkyPositionTest {
    private val origin = SkyPosition(37.0, -76.0, 10.0)

    @Test
    fun smallMovesAreNear() {
        assertTrue(origin.isNear(SkyPosition(37.005, -76.0, 500.0), 1_000.0))
        assertTrue(origin.isNear(origin, 1_000.0))
    }

    @Test
    fun largeMovesAreNotNear() {
        assertFalse(origin.isNear(SkyPosition(37.01, -76.0, 10.0), 1_000.0))
        assertFalse(origin.isNear(SkyPosition(-37.0, 104.0, 10.0), 1_000.0))
    }
}
