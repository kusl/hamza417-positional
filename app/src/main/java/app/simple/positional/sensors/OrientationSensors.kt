package app.simple.positional.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Display
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import app.simple.positional.math.Angle.normalizeEulerAngle
import app.simple.positional.math.CompassAzimuth
import app.simple.positional.math.LowPassFilter.smoothAndSetReadings
import app.simple.positional.math.Tilt
import app.simple.positional.math.Vector3

abstract class OrientationSensor(context: Context) : SensorEventListener, DefaultLifecycleObserver {
    protected val sensorManager: SensorManager? = context.getSystemService(SensorManager::class.java)
    private val display: Display = ContextCompat.getDisplayOrDefault(context)
    protected abstract val sensors: List<Sensor?>

    protected val rotation: Int get() = display.rotation

    val isAvailable: Boolean get() = sensors.isNotEmpty() && sensors.all { it != null }

    override fun onResume(owner: LifecycleOwner) {
        if (isAvailable) sensors.forEach { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    override fun onPause(owner: LifecycleOwner) {
        sensorManager?.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}

class GravitySensor(context: Context, private val onTilt: (Tilt) -> Unit) : OrientationSensor(context) {
    override val sensors = listOf(sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY))
    private val readings = FloatArray(3)

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_GRAVITY) return
        smoothAndSetReadings(readings, event.values, ALPHA)
        onTilt(Tilt.fromDevice(readings, rotation))
    }

    private companion object {
        const val ALPHA = 0.04f
    }
}

class HeadingSensor(context: Context, private val listener: Listener) : OrientationSensor(context) {
    interface Listener {
        fun onHeading(azimuth: Float)
        fun onAccuracy(type: Int, accuracy: Int) {}
        fun onReadings(accelerometer: FloatArray, magnetometer: FloatArray) {}
    }

    override val sensors = listOf(
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
        sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
    )
    private val accelerometer = FloatArray(3)
    private val magnetometer = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)
    var gimbalLock = false

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> smoothAndSetReadings(accelerometer, event.values, ALPHA)
            Sensor.TYPE_MAGNETIC_FIELD -> smoothAndSetReadings(magnetometer, event.values, ALPHA)
            else -> return
        }
        listener.onReadings(accelerometer, magnetometer)
        listener.onHeading(azimuth().normalizeEulerAngle(false))
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        listener.onAccuracy(sensor.type, accuracy)
    }

    private fun azimuth(): Float {
        val raw = if (gimbalLock) {
            if (!SensorManager.getRotationMatrix(rotationMatrix, null, accelerometer, magnetometer)) return 0f
            SensorManager.getOrientation(rotationMatrix, orientation)
            Math.toDegrees(orientation[0].toDouble()).toFloat()
        } else {
            CompassAzimuth.calculate(Vector3.from(accelerometer), Vector3.from(magnetometer))
        }
        return CompassAzimuth.adjustForRotation(raw, rotation)
    }

    private companion object {
        const val ALPHA = 0.03f
    }
}
