package app.simple.positional.util

import app.simple.positional.R
import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceTextTest {
    @Test
    fun metricSwitchesToKilometers() {
        assertEquals(999.5 to R.string.meter, DistanceText.parts(999.5, true))
        assertEquals(1.5 to R.string.kilometer, DistanceText.parts(1500.0, true))
    }

    @Test
    fun imperialSwitchesToMilesAtOneMile() {
        assertEquals(3280.84 to R.string.feet, DistanceText.parts(1000.0, false))
        assertEquals(2.0 to R.string.miles, DistanceText.parts(3218.688, false))
    }

    @Test
    fun zeroIsMeters() {
        assertEquals(0.0 to R.string.meter, DistanceText.parts(0.0, true))
    }
}
