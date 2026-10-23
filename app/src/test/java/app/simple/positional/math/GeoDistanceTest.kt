package app.simple.positional.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoDistanceTest {
    @Test
    fun samePointIsZero() {
        assertEquals(0.0, GeoDistance.meters(37.0, -76.4, 37.0, -76.4), 1e-9)
    }

    @Test
    fun oneDegreeOfLatitude() {
        assertEquals(111_195.0, GeoDistance.meters(0.0, 0.0, 1.0, 0.0), 5.0)
    }

    @Test
    fun antipodesAreHalfCircumference() {
        assertEquals(20_015_115.0, GeoDistance.meters(0.0, 0.0, 0.0, 180.0), 50.0)
    }

    @Test
    fun distanceIsSymmetric() {
        val forward = GeoDistance.meters(51.5, -0.12, 48.85, 2.35)
        val backward = GeoDistance.meters(48.85, 2.35, 51.5, -0.12)
        assertEquals(forward, backward, 1e-6)
        assertEquals(343_500.0, forward, 1_500.0)
    }

    @Test
    fun thresholdIgnoresSmallMoves() {
        val threshold = MovementThreshold(25.0)
        assertTrue(threshold.accept(10.0, 10.0))
        assertFalse(threshold.accept(10.0001, 10.0))
        assertTrue(threshold.accept(10.001, 10.0))
        assertFalse(threshold.accept(10.001, 10.0))
    }

    @Test
    fun resetAcceptsAgain() {
        val threshold = MovementThreshold(25.0)
        assertTrue(threshold.accept(1.0, 1.0))
        threshold.reset()
        assertTrue(threshold.accept(1.0, 1.0))
    }

    @Test
    fun pathSumsSegments() {
        val a = 0.0 to 0.0
        val b = 1.0 to 0.0
        val c = 2.0 to 0.0
        assertEquals(GeoDistance.meters(0.0, 0.0, 2.0, 0.0), GeoDistance.path(listOf(a, b, c)), 1e-6)
        assertEquals(0.0, GeoDistance.path(listOf(a)), 0.0)
        assertEquals(0.0, GeoDistance.path(emptyList()), 0.0)
    }
}
