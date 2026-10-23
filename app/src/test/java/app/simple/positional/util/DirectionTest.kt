package app.simple.positional.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DirectionTest {
    @Test
    fun mapsAzimuthToSector() {
        val cases = mapOf(
            0.0 to 0, 10.0 to 0, 355.0 to 0, -5.0 to 0, 370.0 to 0,
            45.0 to 1, 90.0 to 2, 135.0 to 3, 180.0 to 4,
            225.0 to 5, 270.0 to 6, 315.0 to 7, 349.0 to 7, Double.NaN to -1
        )
        cases.forEach { (azimuth, sector) -> assertEquals("azimuth $azimuth", sector, Direction.sector(azimuth)) }
    }
}
