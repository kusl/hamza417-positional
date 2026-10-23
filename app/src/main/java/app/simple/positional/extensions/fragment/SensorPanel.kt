package app.simple.positional.extensions.fragment

import android.hardware.SensorManager
import app.simple.positional.R
import app.simple.positional.dialogs.compass.CompassCalibration
import app.simple.positional.ui.compose.DialogHost
import app.simple.positional.ui.compose.MessageDialog

abstract class SensorPanel : ScopedFragment() {
    protected val dialogs = DialogHost()
    private var calibrationShown = false

    protected fun showSensorError() {
        val message = getString(R.string.sensor_error)
        dialogs.show { MessageDialog(message, dialogs::dismiss) }
    }

    protected fun showCalibration() {
        if (!isStateSaved && parentFragmentManager.findFragmentByTag(CALIBRATION_TAG) == null) {
            CompassCalibration.newInstance().show(parentFragmentManager, CALIBRATION_TAG)
        }
    }

    protected fun checkCalibration(accuracy: Int) {
        if (!calibrationShown && accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
            calibrationShown = true
            showCalibration()
        }
    }

    private companion object {
        const val CALIBRATION_TAG = "calibration_dialog"
    }
}
