package app.simple.positional.sensors

import app.simple.positional.R
import org.junit.Assert.assertEquals
import org.junit.Test

class SensorAccuracyTest {
    @Test
    fun mapsStatusToLabel() {
        assertEquals(R.string.sensor_accuracy_high, SensorAccuracy.label(3))
        assertEquals(R.string.sensor_accuracy_medium, SensorAccuracy.label(2))
        assertEquals(R.string.sensor_accuracy_low, SensorAccuracy.label(1))
        assertEquals(R.string.sensor_accuracy_unreliable, SensorAccuracy.label(0))
        assertEquals(R.string.not_available, SensorAccuracy.label(-1))
    }
}
