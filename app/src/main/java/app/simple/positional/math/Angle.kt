@file:Suppress("unused")

package app.simple.positional.math

import kotlin.math.acos
import kotlin.math.sqrt

object Angle {
    fun getAngle(xTouch: Double, yTouch: Double, dialerWidth: Float, dialerHeight: Float): Float {
        val centerX = dialerWidth / 2.0F
        val centerY = dialerHeight / 2.0F
        val x2 = xTouch - centerX
        val y2 = yTouch - centerY
        val d1 = sqrt((centerY * centerY).toDouble())
        val d2 = sqrt(x2 * x2 + y2 * y2)
        return if (xTouch >= centerX) {
            Math.toDegrees(acos(-centerY * y2 / (d1 * d2))).toFloat()
        } else
            (360 - Math.toDegrees(acos(-centerY * y2 / (d1 * d2)))).toFloat()
    }

    fun Float.normalizeEulerAngle(inverseResult: Boolean): Float {
        var normalized = this % 360
        if (normalized < 0) normalized += 360
        return if (inverseResult) normalized - 360F else normalized
    }

    private fun getQuadrant(x: Double, y: Double): Int {
        return if (x >= 0) {
            if (y >= 0) 1 else 4
        } else {
            if (y >= 0) 2 else 3
        }
    }
}
