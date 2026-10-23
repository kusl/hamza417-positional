package app.simple.positional.math

import android.view.Surface
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

data class Tilt(val x: Float, val y: Float, val z: Float) {
    val horizontal: Float get() = abs(Math.toDegrees(atan2(x, sqrt(y * y + z * z)).toDouble()).toFloat())
    val vertical: Float get() = abs(Math.toDegrees(atan2(y, sqrt(x * x + z * z)).toDouble()).toFloat())
    val isLevel: Boolean get() = abs(x) <= LEVEL_TOLERANCE && abs(y) <= LEVEL_TOLERANCE

    companion object {
        private const val LEVEL_TOLERANCE = 0.2f
        val zero = Tilt(0f, 0f, 0f)

        fun fromDevice(values: FloatArray, rotation: Int): Tilt = when (rotation) {
            Surface.ROTATION_90 -> Tilt(-values[1], values[0], values[2])
            Surface.ROTATION_180 -> Tilt(-values[0], -values[1], values[2])
            Surface.ROTATION_270 -> Tilt(values[1], -values[0], values[2])
            else -> Tilt(values[0], values[1], values[2])
        }
    }
}
