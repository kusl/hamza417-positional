package app.simple.positional.math

import android.view.Surface
import android.view.WindowManager
import app.simple.positional.math.MathExtensions.normalizeAngle
import app.simple.positional.math.MathExtensions.toDegrees
import kotlin.math.atan2

@Suppress("unused")
object CompassAzimuth {
    @Suppress("deprecation")
    fun adjustAzimuthForDisplayRotation(azimuth: Float, windowManager: WindowManager): Float {
        return when (val displayRotation = windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_0 -> azimuth
            Surface.ROTATION_90 -> azimuth - 270f
            Surface.ROTATION_180 -> azimuth - 180f
            Surface.ROTATION_270 -> azimuth - 90f
            else -> throw IllegalArgumentException("Unexpected display rotation: $displayRotation")
        }
    }

    fun calculate(gravity: Vector3, magneticField: Vector3): Float {
        val normGravity = gravity.normalize()
        val normMagField = magneticField.normalize()

        val east = normMagField.cross(normGravity)
        val normEast = east.normalize()

        val eastMagnitude = east.magnitude()
        val gravityMagnitude = gravity.magnitude()
        val magneticMagnitude = magneticField.magnitude()
        if (gravityMagnitude * magneticMagnitude * eastMagnitude < 0.1f) {
            return 0F
        }

        val dotProduct = normGravity.dot(normMagField)
        val north = normMagField.minus(normGravity * dotProduct)
        val normNorth = north.normalize()

        val sin = normEast.y - normNorth.x
        val cos = normEast.x + normNorth.y
        val azimuth = if (!(sin == 0f && sin == cos)) atan2(sin, cos) else 0f

        if (azimuth.isNaN()) {
            return 0F
        }

        return if (azimuth.isNaN() || !azimuth.isFinite()) 0f else normalizeAngle(azimuth.toDegrees())
    }

    fun calculate(gravity: Vector3, magneticField: Vector3, windowManager: WindowManager): Float {
        return adjustAzimuthForDisplayRotation(calculate(gravity, magneticField), windowManager)
    }

    fun inverse(azimuth: Float): Float {
        return azimuth + 180F
    }
}
