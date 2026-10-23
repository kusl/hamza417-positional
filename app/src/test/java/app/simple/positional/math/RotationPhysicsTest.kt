package app.simple.positional.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RotationPhysicsTest {
    private fun settle(physics: RotationPhysics): Int {
        var frame = 0L
        var steps = 0
        while (physics.step(frame) && steps < 10_000) {
            frame += 16L
            steps++
        }
        return steps
    }

    @Test
    fun settlesOnTarget() {
        val physics = RotationPhysics()
        physics.snap(0f)
        physics.aim(90f)
        assertTrue(settle(physics) < 10_000)
        assertEquals(0.0, RotationPhysics.shortestDelta(physics.angle, 90.0), 0.2)
        assertTrue(physics.isSettled)
    }

    @Test
    fun takesShortestPathAcrossNorth() {
        val physics = RotationPhysics()
        physics.snap(350f)
        physics.aim(10f)
        physics.step(0L)
        assertTrue(physics.angle > 350.0)
        settle(physics)
        assertEquals(0.0, RotationPhysics.shortestDelta(physics.angle, 10.0), 0.2)
    }

    @Test
    fun snapStopsMotion() {
        val physics = RotationPhysics()
        physics.aim(120f)
        physics.snap(45f)
        assertEquals(45.0, physics.angle, 0.0)
        assertFalse(physics.step(0L))
    }

    @Test
    fun negativeCoefficientsFallBackToDefaults() {
        val physics = RotationPhysics(-1f, -1f, -1f)
        assertEquals(RotationPhysics.INERTIA, physics.inertia, 0f)
        assertEquals(RotationPhysics.DAMPING, physics.damping, 0f)
        assertEquals(RotationPhysics.MAGNETIC, physics.magnetic, 0f)
    }

    @Test
    fun shortestDeltaWraps() {
        assertEquals(20.0, RotationPhysics.shortestDelta(350.0, 10.0), 1e-9)
        assertEquals(-20.0, RotationPhysics.shortestDelta(10.0, 350.0), 1e-9)
        assertEquals(0.0, RotationPhysics.shortestDelta(-360.0, 720.0), 1e-9)
    }
}
