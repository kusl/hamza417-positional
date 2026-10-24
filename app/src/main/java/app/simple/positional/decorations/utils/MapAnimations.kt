package app.simple.positional.decorations.utils

import android.animation.ValueAnimator
import android.location.Location
import android.view.animation.PathInterpolator
import androidx.core.graphics.ColorUtils
import app.simple.positional.math.MapInterpolation
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline

private const val DURATION = 1000L

private fun animate(update: (Float) -> Unit): ValueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
    duration = DURATION
    interpolator = PathInterpolator(0f, 0f, 0.2f, 1f)
    addUpdateListener { animator -> runCatching { update(animator.animatedFraction) } }
    start()
}

private fun LatLng.toward(end: LatLng, fraction: Float): LatLng {
    val (latitude, longitude) = MapInterpolation.position(fraction, latitude, longitude, end.latitude, end.longitude)
    return LatLng(latitude, longitude)
}

private fun Location.latLng() = LatLng(latitude, longitude)

fun Marker.animateTo(location: Location, rotation: Float?): ValueAnimator {
    val start = position
    val end = location.latLng()
    val startRotation = this.rotation
    return animate { fraction ->
        position = start.toward(end, fraction)
        if (rotation != null) this.rotation = MapInterpolation.angle(fraction, startRotation, rotation)
    }
}

fun Circle.animateTo(location: Location): ValueAnimator {
    val start = center
    val end = location.latLng()
    val startRadius = radius
    val endRadius = location.accuracy.toDouble()
    return animate { fraction ->
        center = start.toward(end, fraction)
        radius = MapInterpolation.linear(fraction, startRadius, endRadius)
    }
}

fun Circle.animateColors(fill: Int, stroke: Int): ValueAnimator {
    val startFill = fillColor
    val startStroke = strokeColor
    return animate { fraction ->
        fillColor = ColorUtils.blendARGB(startFill, fill, fraction)
        strokeColor = ColorUtils.blendARGB(startStroke, stroke, fraction)
    }
}

fun Polyline.animateSegment(start: LatLng, end: LatLng): ValueAnimator = animate { fraction ->
    points = listOf(start, start.toward(end, fraction))
}
