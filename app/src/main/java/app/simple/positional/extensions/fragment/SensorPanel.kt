package app.simple.positional.extensions.fragment

import android.hardware.SensorManager
import app.simple.positional.R
import app.simple.positional.dialogs.app.ErrorDialog
import app.simple.positional.dialogs.compass.CompassCalibration

abstract class SensorPanel : ScopedFragment() {
    private var calibrationShown = false

    protected fun showSensorError() {
        if (childFragmentManager.findFragmentByTag(ERROR_TAG) == null) {
            ErrorDialog.newInstance(getString(R.string.sensor_error)).show(childFragmentManager, ERROR_TAG)
        }
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
        const val ERROR_TAG = "error_dialog"
        const val CALIBRATION_TAG = "calibration_dialog"
    }
}
