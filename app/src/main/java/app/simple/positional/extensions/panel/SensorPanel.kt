package app.simple.positional.extensions.panel

import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import app.simple.positional.R
import app.simple.positional.ui.compose.CalibrationDialog
import app.simple.positional.ui.compose.MessageDialog

abstract class SensorPanel(activity: ComponentActivity) : PanelController(activity) {
    private var calibrationShown = false
    private var accelerometerAccuracy by mutableIntStateOf(-1)
    private var magnetometerAccuracy by mutableIntStateOf(-1)

    protected fun showSensorError() {
        val message = getString(R.string.sensor_error)
        dialogs.show { MessageDialog(message, dialogs::dismiss) }
    }

    protected fun showCalibration() {
        dialogs.show { CalibrationDialog(accelerometerAccuracy, magnetometerAccuracy, dialogs::dismiss) }
    }

    protected fun checkCalibration(type: Int, accuracy: Int) {
        when (type) {
            Sensor.TYPE_ACCELEROMETER -> accelerometerAccuracy = accuracy
            Sensor.TYPE_MAGNETIC_FIELD -> magnetometerAccuracy = accuracy
        }
        if (!calibrationShown && accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
            calibrationShown = true
            showCalibration()
        }
    }
}
