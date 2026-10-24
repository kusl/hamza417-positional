package app.simple.positional.math

import kotlin.math.abs
import kotlin.math.sign

object MapInterpolation {
    fun position(fraction: Float, startLatitude: Double, startLongitude: Double, endLatitude: Double, endLongitude: Double): Pair<Double, Double> {
        var delta = endLongitude - startLongitude
        if (abs(delta) > 180) delta -= sign(delta) * 360
        return (endLatitude - startLatitude) * fraction + startLatitude to delta * fraction + startLongitude
    }

    fun angle(fraction: Float, start: Float, end: Float): Float {
        var delta = ((end - start) % 360 + 360) % 360
        if (delta > 180) delta -= 360
        return ((start + delta * fraction) % 360 + 360) % 360
    }

    fun linear(fraction: Float, start: Double, end: Double) = start + (end - start) * fraction
}
