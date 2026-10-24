package app.simple.positional.math

import android.view.Surface
import app.simple.positional.math.MathExtensions.normalizeAngle
import app.simple.positional.math.MathExtensions.toDegrees
import kotlin.math.atan2

object CompassAzimuth {
    fun adjustForRotation(azimuth: Float, rotation: Int): Float = when (rotation) {
        Surface.ROTATION_90 -> azimuth - 270f
        Surface.ROTATION_180 -> azimuth - 180f
        Surface.ROTATION_270 -> azimuth - 90f
        else -> azimuth
    }

    fun calculate(gravity: Vector3, magneticField: Vector3): Float {
        val normGravity = gravity.normalize()
        val normMagField = magneticField.normalize()
        val east = normMagField.cross(normGravity)
        val normEast = east.normalize()

        if (gravity.magnitude() * magneticField.magnitude() * east.magnitude() < 0.1f) {
            return 0F
        }

        val north = normMagField.minus(normGravity * normGravity.dot(normMagField))
        val normNorth = north.normalize()

        val sin = normEast.y - normNorth.x
        val cos = normEast.x + normNorth.y
        val azimuth = if (!(sin == 0f && sin == cos)) atan2(sin, cos) else 0f

        return if (azimuth.isNaN() || !azimuth.isFinite()) 0f else normalizeAngle(azimuth.toDegrees())
    }
}
