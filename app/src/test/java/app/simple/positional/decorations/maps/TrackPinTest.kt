package app.simple.positional.decorations.maps

import android.hardware.SensorManager
import app.simple.positional.R
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackPinTest {
    @Test
    fun noLocationShowsHistoricalPin() {
        assertEquals(R.drawable.ic_place_historical, TrackPin.drawable(false, compass = true, accuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH, moving = true))
    }

    @Test
    fun compassPinFollowsSensorAccuracy() {
        assertEquals(R.drawable.ic_pin_low, TrackPin.drawable(true, true, SensorManager.SENSOR_STATUS_ACCURACY_LOW, false))
        assertEquals(R.drawable.ic_pin_medium, TrackPin.drawable(true, true, SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM, false))
        assertEquals(R.drawable.ic_pin_high, TrackPin.drawable(true, true, SensorManager.SENSOR_STATUS_ACCURACY_HIGH, false))
        assertEquals(R.drawable.ic_pin_unreliable, TrackPin.drawable(true, true, SensorManager.SENSOR_STATUS_UNRELIABLE, false))
        assertEquals(R.drawable.ic_pin_unreliable, TrackPin.drawable(true, true, -1, false))
    }

    @Test
    fun withoutCompassPinShowsMovement() {
        assertEquals(R.drawable.ic_pin_bearing, TrackPin.drawable(true, false, -1, true))
        assertEquals(R.drawable.ic_pin_no_speed, TrackPin.drawable(true, false, -1, false))
    }
}
