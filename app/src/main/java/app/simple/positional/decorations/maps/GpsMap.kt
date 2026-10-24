package app.simple.positional.decorations.maps

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.location.Location
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import app.simple.positional.R
import app.simple.positional.constants.LocationPins
import app.simple.positional.extensions.maps.MapsCallbacks
import app.simple.positional.preferences.GPSPreferences
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.util.BitmapHelper.addLinearGradient
import app.simple.positional.util.BitmapHelper.toBitmapKeepingSize
import app.simple.positional.util.MapSource
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline

interface MapHost {
    val view: View
    val lifecycleObserver: DefaultLifecycleObserver
}

interface PanelMap : MapHost {
    fun setCallbacks(callbacks: MapsCallbacks)
    fun updateLocation(location: Location)
    fun resetCamera(zoom: Float)
}

interface GpsMap : PanelMap {
    var isMapMovementEnabled: Boolean
    var onTouch: ((MotionEvent) -> Unit)?
    fun screenPoint(latLng: LatLng): Point?
    fun setTargetMarker(latLng: LatLng?)
    fun moveCameraToTarget()
    fun zoomIn()
    fun zoomOut()
    fun registerWithRunnable()
    fun unregister()

    companion object {
        fun create(context: Context): GpsMap = if (MapSource.isOsm()) OsmGpsMap(context) else GoogleGpsMap(context)
    }
}

object GpsPin {
    private val customGradient = intArrayOf(0xFFFF1B50.toInt(), 0xFFE11677.toInt())

    fun bitmap(context: Context, custom: Boolean, moving: Boolean): Bitmap {
        val icon = if (moving && !custom) R.drawable.ic_bearing_marker else LocationPins.getLocationPin()
        val bitmap = icon.toBitmapKeepingSize(context, GPSPreferences.getPinSize(), GPSPreferences.getPinOpacity())
        return if (custom) bitmap.addLinearGradient(customGradient) else bitmap
    }
}

object GpsMapPreferences {
    private val targetKeys = arrayOf(
        GPSPreferences.mapTargetMarker,
        GPSPreferences.mapTargetMarkerLatitude,
        GPSPreferences.mapTargetMarkerLongitude,
        GPSPreferences.mapTargetMarkerStartLatitude,
        GPSPreferences.mapTargetMarkerStartLongitude,
    )

    fun bind(preferences: MapPreferences, onPin: () -> Unit, onBearing: () -> Unit, onTarget: () -> Unit) {
        preferences.watch(GPSPreferences.pinSkin, GPSPreferences.pinSize, GPSPreferences.pinOpacity, read = ::pinStyle) { onPin() }
        preferences.watch(GPSPreferences.useBearingRotation, read = GPSPreferences::isBearingRotation) { onBearing() }
        preferences.watch(*targetKeys, read = ::targetState) { onTarget() }
    }

    private fun pinStyle() = Triple(GPSPreferences.getPinSkin(), GPSPreferences.getPinSize(), GPSPreferences.getPinOpacity())

    private fun targetState() = Triple(
        GPSPreferences.isTargetMarkerSet(),
        GPSPreferences.getTargetMarkerCoordinates().toList(),
        GPSPreferences.getTargetMarkerStartCoordinates().toList(),
    )
}

@SuppressLint("ViewConstructor")
class GoogleGpsMap(context: Context) : GooglePanelMap(context), GpsMap {
    override var isMapMovementEnabled = true
    override var onTouch: ((MotionEvent) -> Unit)? = null
    override val compassRotation get() = GPSPreferences.isCompassRotation()
    override val compassKey get() = GPSPreferences.compassRotation
    override val lookKeys: Set<String> = setOf(
        GPSPreferences.GPSLabelMode,
        GPSPreferences.GPSSatellite,
        GPSPreferences.highContrastMap,
        GPSPreferences.showBuilding,
        GPSPreferences.trafficMode,
    )
    private val custom = MainPreferences.isCustomCoordinate()
    private val customPoint = MainPreferences.getCoordinates().let { LatLng(it[0].toDouble(), it[1].toDouble()) }
    private var targetLine: Polyline? = null

    override fun look() = MapLook(
        GPSPreferences.isLabelOn(),
        GPSPreferences.isSatelliteOn(),
        GPSPreferences.getHighContrastMap(),
        GPSPreferences.getShowBuildingsOnMap(),
        GPSPreferences.isTrafficShown(),
    )

    override fun startCamera(): CameraPosition {
        val start = current() ?: MainPreferences.getLastCoordinates().let { LatLng(it[0].toDouble(), it[1].toDouble()) }
        return camera(start, GPSPreferences.getMapZoom(), GPSPreferences.getMapTilt(), GPSPreferences.getMapBearing())
    }

    override fun onCameraIdle(position: CameraPosition) {
        GPSPreferences.setMapZoom(position.zoom)
        GPSPreferences.setMapTilt(position.tilt)
        if (!compassRotation) GPSPreferences.setMapBearing(position.bearing)
    }

    override fun onGesture() {
        isMapMovementEnabled = false
    }

    override fun onReady() {
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
        val point = current() ?: return
        if (first) {
            moveCamera(camera(point, GPSPreferences.getMapZoom(), GPSPreferences.getMapTilt(), bearing()))
        } else if (isMapMovementEnabled && !animating && !GPSPreferences.isTargetMarkerMode() && (!compassRotation || !isVisible(point))) {
            follow(GPSPreferences.getMapZoom())
        }
    }

    override fun screenPoint(latLng: LatLng): Point? = map?.projection?.toScreenLocation(latLng)

    override fun resetCamera(zoom: Float) = follow(zoom)

    override fun setTargetMarker(latLng: LatLng?) {
        val target = latLng ?: target() ?: return
        GPSPreferences.setTargetMarkerLatitude(target.latitude.toFloat())
        GPSPreferences.setTargetMarkerLongitude(target.longitude.toFloat())
        GPSPreferences.setTargetMarker(true)
    }

    override fun moveCameraToTarget() {
        val target = GPSPreferences.getTargetMarkerCoordinates()
        animateTo(camera(LatLng(target[0].toDouble(), target[1].toDouble()), GPSPreferences.getMapZoom(), GPSPreferences.getMapTilt(), bearing()))
    }

    override fun zoomIn() = zoom(zoomIn = true)

    override fun zoomOut() = zoom(zoomIn = false)

    override fun registerWithRunnable() = registerHeadingLater()

    override fun unregister() = unregisterHeading()

    private fun current(): LatLng? = if (custom) customPoint else location?.let { LatLng(it.latitude, it.longitude) }

    private fun bearing(): Float = when {
        GPSPreferences.isCompassRotation() -> heading
        GPSPreferences.isBearingRotation() -> location?.bearing ?: 0f
        else -> 0f
    }

    private fun follow(zoom: Float) {
        val point = current() ?: target() ?: return
        animateTo(camera(point, zoom, GPSPreferences.getMapTilt(), bearing()))
    }

    private fun drawPin() {
        val point = current() ?: return
        val fix = location
        val moving = !custom && (fix?.speed ?: 0f) > 0f
        drawPin(moving, point, centered = moving, rotation = if (moving) fix?.bearing ?: 0f else 0f) {
            GpsPin.bitmap(context, custom, moving)
        }
        if (custom || fix == null) return
        drawAccuracy(
            point,
            if (moving) fix.speed.toDouble() else fix.accuracy.toDouble(),
            if (moving) ContextCompat.getColor(context, R.color.bearing_circle_color) else LocationPins.getCircleFillColor(),
            if (moving) ContextCompat.getColor(context, R.color.bearing_circle_stroke) else LocationPins.getCircleStrokeColor(),
        )
    }

    private fun drawTarget() {
        val current = current()
        if (!GPSPreferences.isTargetMarkerSet() || current == null) {
            targetLine?.remove()
            targetLine = null
            return
        }
        val target = GPSPreferences.getTargetMarkerCoordinates().let { LatLng(it[0].toDouble(), it[1].toDouble()) }
        val start = GPSPreferences.getTargetMarkerStartCoordinates().let { LatLng(it[0].toDouble(), it[1].toDouble()) }
        val points = if (custom) listOf(current, target) else listOf(start, current, target)
        targetLine?.let { it.points = points } ?: run { targetLine = line(points) }
    }

    override fun onCompassRotation(enabled: Boolean) = rotate(bearing(), animate = true)

    override fun bindPreferences(preferences: MapPreferences) {
        super.bindPreferences(preferences)
        GpsMapPreferences.bind(preferences, onPin = ::redrawPin, onBearing = { rotate(bearing(), animate = true) }, onTarget = ::drawTarget)
    }

    private fun redrawPin() {
        invalidatePin()
        drawPin()
    }
}
