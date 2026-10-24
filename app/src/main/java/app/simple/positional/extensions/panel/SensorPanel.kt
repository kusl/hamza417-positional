package app.simple.positional.extensions.panel

import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import app.simple.positional.R
import app.simple.positional.ui.compose.CalibrationDialog
import app.simple.positional.ui.compose.PanelDialog

abstract class SensorPanel(activity: ComponentActivity) : PanelController(activity) {
    private var calibrationShown = false
    private var accelerometerAccuracy by mutableIntStateOf(-1)
    private var magnetometerAccuracy by mutableIntStateOf(-1)

    private data object Calibration : PanelDialog

    protected fun showSensorError() = showError(getString(R.string.sensor_error))

    protected fun showCalibration() = dialogs.show(Calibration)

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

    @Composable
    override fun DialogContent(dialog: PanelDialog) {
        if (dialog == Calibration) CalibrationDialog(accelerometerAccuracy, magnetometerAccuracy, dialogs::dismiss) else super.DialogContent(dialog)
    }
}
