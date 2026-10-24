package app.simple.positional.math

import org.junit.Assert.assertEquals
import org.junit.Test

class MapInterpolationTest {
    @Test
    fun positionMovesLinearly() {
        val (latitude, longitude) = MapInterpolation.position(0.5f, 0.0, 0.0, 10.0, 20.0)
        assertEquals(5.0, latitude, 1e-9)
        assertEquals(10.0, longitude, 1e-9)
    }

    @Test
    fun positionCrossesAntimeridianTheShortWay() {
        assertEquals(180.0, MapInterpolation.position(0.5f, 0.0, 179.0, 0.0, -179.0).second, 1e-9)
        assertEquals(-180.0, MapInterpolation.position(0.5f, 0.0, -179.0, 0.0, 179.0).second, 1e-9)
    }

    @Test
    fun angleTurnsTheShortWay() {
        assertEquals(0f, MapInterpolation.angle(0.5f, 350f, 10f), 1e-4f)
        assertEquals(0f, MapInterpolation.angle(0.5f, 10f, 350f), 1e-4f)
        assertEquals(10f, MapInterpolation.angle(1f, 350f, 10f), 1e-4f)
        assertEquals(350f, MapInterpolation.angle(0f, 350f, 10f), 1e-4f)
        assertEquals(90f, MapInterpolation.angle(0.5f, 0f, 180f), 1e-4f)
    }

    @Test
    fun radiusIsNotWrappedLikeAnAngle() {
        assertEquals(205.0, MapInterpolation.linear(0.5f, 10.0, 400.0), 1e-9)
    }
}
