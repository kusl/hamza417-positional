package app.simple.positional.math

import kotlin.math.pow
import kotlin.math.roundToLong

object MathExtensions {
    fun normalizeAngle(angle: Float): Float = (angle % 360f + 360f) % 360f

    fun Float.toDegrees(): Float = Math.toDegrees(toDouble()).toFloat()

    fun round(value: Double, places: Int): Double {
        if (places < 0) return Double.NaN
        val factor = 10.0.pow(places)
        return (value * factor).roundToLong() / factor
    }
}
