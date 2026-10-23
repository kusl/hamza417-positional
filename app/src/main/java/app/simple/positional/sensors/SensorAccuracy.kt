package app.simple.positional.sensors

import android.hardware.SensorManager
import app.simple.positional.R

object SensorAccuracy {
    fun label(accuracy: Int): Int = when (accuracy) {
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> R.string.sensor_accuracy_high
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> R.string.sensor_accuracy_medium
        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> R.string.sensor_accuracy_low
        SensorManager.SENSOR_STATUS_UNRELIABLE -> R.string.sensor_accuracy_unreliable
        else -> R.string.not_available
    }
}
