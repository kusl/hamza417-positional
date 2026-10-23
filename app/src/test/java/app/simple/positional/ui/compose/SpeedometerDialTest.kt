package app.simple.positional.ui.compose

import app.simple.positional.R
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedometerDialTest {
    @Test
    fun rangesSwitchEveryThreeHundredDegrees() {
        assertEquals(0, SpeedometerDial.range(0f))
        assertEquals(0, SpeedometerDial.range(300f))
        assertEquals(1, SpeedometerDial.range(300.5f))
        assertEquals(1, SpeedometerDial.range(600f))
        assertEquals(2, SpeedometerDial.range(900f))
        assertEquals(3, SpeedometerDial.range(901f))
    }

    @Test
    fun needleIsCompensatedPerRange() {
        assertEquals(120f, SpeedometerDial.needle(120f), 0f)
        assertEquals(340f, SpeedometerDial.needle(400f), 0f)
        assertEquals(580f, SpeedometerDial.needle(700f), 0f)
        assertEquals(820f, SpeedometerDial.needle(1000f), 0f)
    }

    @Test
    fun dialMatchesRange() {
        assertEquals(R.drawable.speedometer_dial_normal_range, SpeedometerDial.dial(10f))
        assertEquals(R.drawable.speedometer_dial_medium_range, SpeedometerDial.dial(450f))
        assertEquals(R.drawable.speedometer_dial_high_range, SpeedometerDial.dial(750f))
        assertEquals(R.drawable.speedometer_dial_max_range, SpeedometerDial.dial(2000f))
    }
}
