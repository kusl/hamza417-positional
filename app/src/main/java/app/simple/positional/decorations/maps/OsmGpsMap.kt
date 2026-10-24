package app.simple.positional.decorations.maps

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Point
import android.location.Location
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import app.simple.positional.R
import app.simple.positional.constants.LocationPins
import app.simple.positional.preferences.GPSPreferences
import app.simple.positional.preferences.MainPreferences
import com.google.android.gms.maps.model.LatLng
import org.maplibre.android.camera.CameraPosition

@SuppressLint("ViewConstructor")
class OsmGpsMap(context: Context) : OsmPanelMap(context), GpsMap {
    override var isMapMovementEnabled = true
    override var onTouch: ((MotionEvent) -> Unit)? = null
    override val compassRotation get() = GPSPreferences.isCompassRotation()
    private val custom = MainPreferences.isCustomCoordinate()
    private val customPoint = MainPreferences.getCoordinates().let { it[0].toDouble() to it[1].toDouble() }

    override fun startCamera(): CameraPosition {
        val start = if (custom) customPoint else MainPreferences.getLastCoordinates().let { it[0].toDouble() to it[1].toDouble() }
        return camera(start.first, start.second, GPSPreferences.getMapZoom(), GPSPreferences.getMapTilt(), GPSPreferences.getMapBearing())
    }

    override fun onCameraIdle(position: CameraPosition) {
        GPSPreferences.setMapZoom(position.zoom.toFloat())
        GPSPreferences.setMapTilt(position.tilt.toFloat())
        if (!compassRotation) GPSPreferences.setMapBearing(position.bearing.toFloat())
    }

    override fun onGesture() {
        isMapMovementEnabled = false
    }

    override fun onStyleReady() {
        drawPin()
        drawTarget()
    }

    override fun onHeadingChanged() {
        if (!animating) rotate(heading, animate = false)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        onTouch?.invoke(ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun updateLocation(location: Location) {
        val first = this.location == null
        this.location = location
        drawPin()
        drawTarget()
        if (first) {
            moveCamera(camera(location.latitude, location.longitude, GPSPreferences.getMapZoom(), GPSPreferences.getMapTilt(), bearing()))
        } else if (isMapMovementEnabled && !animating && !GPSPreferences.isTargetMarkerMode() && (!compassRotation || current()?.let { !isVisible(it.first, it.second) } == true)) {
            follow(GPSPreferences.getMapZoom())
        }
    }

    override fun screenPoint(latLng: LatLng): Point? = screenPointOf(latLng.latitude, latLng.longitude)

    override fun resetCamera(zoom: Float) = follow(zoom)

    override fun setTargetMarker(latLng: LatLng?) {
        val target = latLng?.let { it.latitude to it.longitude } ?: target() ?: return
        GPSPreferences.setTargetMarkerLatitude(target.first.toFloat())
        GPSPreferences.setTargetMarkerLongitude(target.second.toFloat())
        GPSPreferences.setTargetMarker(true)
    }

    override fun moveCameraToTarget() {
        val target = GPSPreferences.getTargetMarkerCoordinates()
        animateTo(camera(target[0].toDouble(), target[1].toDouble(), GPSPreferences.getMapZoom(), GPSPreferences.getMapTilt(), bearing()))
    }

    override fun zoomIn() = zoom(zoomIn = true)

    override fun zoomOut() = zoom(zoomIn = false)

    override fun registerWithRunnable() = registerHeadingLater()

    override fun unregister() = unregisterHeading()

    private fun current(): Pair<Double, Double>? = if (custom) customPoint else location?.let { it.latitude to it.longitude }

    private fun bearing(): Float = when {
        GPSPreferences.isCompassRotation() -> heading
        GPSPreferences.isBearingRotation() -> location?.bearing ?: 0f
        else -> 0f
    }

    private fun follow(zoom: Float) {
        val point = current() ?: target() ?: return
        animateTo(camera(point.first, point.second, zoom, GPSPreferences.getMapTilt(), bearing()))
    }

    private fun drawPin() {
        val point = current() ?: return
        val fix = location
        val moving = !custom && (fix?.speed ?: 0f) > 0f
        drawPin(moving, point.first, point.second, centered = moving, rotation = if (moving) fix?.bearing ?: 0f else 0f) {
            GpsPin.bitmap(context, custom, moving)
        }
        if (custom || fix == null) return
        drawAccuracy(
            point.first,
            point.second,
            if (moving) fix.speed.toDouble() else fix.accuracy.toDouble(),
            if (moving) ContextCompat.getColor(context, R.color.bearing_circle_color) else LocationPins.getCircleFillColor(),
            if (moving) ContextCompat.getColor(context, R.color.bearing_circle_stroke) else LocationPins.getCircleStrokeColor(),
        )
    }

    private fun drawTarget() {
        val current = current()
        if (!GPSPreferences.isTargetMarkerSet() || current == null) {
            source(OsmStyle.LINE, OsmStyle.EMPTY_FEATURES)
            return
        }
        val target = GPSPreferences.getTargetMarkerCoordinates().let { it[0].toDouble() to it[1].toDouble() }
        val start = GPSPreferences.getTargetMarkerStartCoordinates().let { it[0].toDouble() to it[1].toDouble() }
        source(OsmStyle.LINE, OsmStyle.line(if (custom) listOf(current, target) else listOf(start, current, target)))
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            GPSPreferences.pinSkin, GPSPreferences.pinSize, GPSPreferences.pinOpacity -> {
                invalidatePin()
                drawPin()
            }
            GPSPreferences.compassRotation -> if (GPSPreferences.isCompassRotation()) registerHeadingLater() else unregisterHeading()
            GPSPreferences.useBearingRotation, GPSPreferences.isNorthOnly -> rotate(bearing(), animate = true)
            else -> drawTarget()
        }
    }
}
