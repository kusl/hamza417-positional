package app.simple.positional.math

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoDistance {
    private const val EARTH_RADIUS = 6_371_008.8

    fun meters(fromLatitude: Double, fromLongitude: Double, toLatitude: Double, toLongitude: Double): Double {
        val dLat = Math.toRadians(toLatitude - fromLatitude)
        val dLon = Math.toRadians(toLongitude - fromLongitude)
        val a = sin(dLat / 2).let { it * it } +
                cos(Math.toRadians(fromLatitude)) * cos(Math.toRadians(toLatitude)) * sin(dLon / 2).let { it * it }
        return 2 * EARTH_RADIUS * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }
}

class MovementThreshold(private val meters: Double) {
    private var last: Pair<Double, Double>? = null

    fun accept(latitude: Double, longitude: Double): Boolean {
        val previous = last
        if (previous != null && GeoDistance.meters(previous.first, previous.second, latitude, longitude) < meters) return false
        last = latitude to longitude
        return true
    }

    fun reset() {
        last = null
    }
}
