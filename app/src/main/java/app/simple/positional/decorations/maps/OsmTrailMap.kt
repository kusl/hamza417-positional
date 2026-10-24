package app.simple.positional.decorations.maps

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import androidx.core.content.ContextCompat
import app.simple.positional.R
import app.simple.positional.constants.TrailIcons
import app.simple.positional.model.TrailPoint
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.preferences.TrailPreferences
import app.simple.positional.util.BitmapHelper.toBitmap
import org.maplibre.android.camera.CameraPosition

@SuppressLint("ViewConstructor")
class OsmTrailMap(context: Context) : OsmPanelMap(context), TrailMap {
    override val compassRotation get() = TrailPreferences.isCompassRotation()
    private var points: List<TrailPoint> = emptyList()
    private val icons = mutableSetOf<Int>()
    private var restore: CameraPosition? = null
    private val autoCenter = Runnable { recenter() }

    override fun startCamera(): CameraPosition {
        val last = MainPreferences.getLastCoordinates()
        return camera(last[0].toDouble(), last[1].toDouble(), TrailPreferences.getMapZoom(), TrailPreferences.getMapTilt(), TrailPreferences.getMapBearing())
    }

    override fun onCameraIdle(position: CameraPosition) {
        TrailPreferences.setMapZoom(position.zoom.toFloat())
        TrailPreferences.setMapTilt(position.tilt.toFloat())
        TrailPreferences.setMapBearing(position.bearing.toFloat())
        scheduleAutoCenter()
    }

    override fun onCameraMoved() {
        mainHandler.removeCallbacks(autoCenter)
    }

    override fun onStyleReady() {
        icons.clear()
        drawPoints()
        drawPuck()
        if (TrailPreferences.arePolylinesWrapped()) wrap(animate = false)
    }

    override fun onHeadingChanged() = drawPuck()

    override fun onResumed() = scheduleAutoCenter()

    override fun onPaused() {
        mainHandler.removeCallbacks(autoCenter)
    }

    override fun updateLocation(location: Location) {
        val first = this.location == null
        this.location = location
        drawPuck()
        if (first && !TrailPreferences.arePolylinesWrapped()) {
            moveCamera(camera(location.latitude, location.longitude, TrailPreferences.getMapZoom(), TrailPreferences.getMapTilt(), TrailPreferences.getMapBearing()))
        }
    }

    override fun resetCamera(zoom: Float) {
        location?.let { moveTo(it.latitude, it.longitude, zoom) }
    }

    override fun showPoints(points: List<TrailPoint>) {
        this.points = points
        drawPoints()
        if (TrailPreferences.arePolylinesWrapped()) wrap(animate = false)
    }

    override fun removeLast() {
        val last = points.lastOrNull() ?: return
        mapCallbacks?.onLineDeleted(last)
        val previous = points.getOrNull(points.size - 2)
        if (TrailPreferences.arePolylinesWrapped() || previous == null) return
        moveTo(previous.latitude, previous.longitude, TrailPreferences.getMapZoom())
    }

    override fun toggleWrap() {
        if (!TrailPreferences.arePolylinesWrapped()) {
            wrap(animate = true)
            return
        }
        TrailPreferences.setWrapStatus(false)
        restore?.let(::animateTo)
    }

    override fun moveTo(latitude: Double, longitude: Double, zoom: Float) {
        TrailPreferences.setWrapStatus(false)
        animateTo(camera(latitude, longitude, zoom, TrailPreferences.getMapTilt(), TrailPreferences.getMapBearing()))
    }

    override fun clear() {
        points = emptyList()
        drawPoints()
    }

    private fun wrap(animate: Boolean) {
        if (!TrailPreferences.arePolylinesWrapped()) restore = map?.cameraPosition
        if (fit(points.map { it.latitude to it.longitude }, animate)) TrailPreferences.setWrapStatus(true)
    }

    private fun scheduleAutoCenter() {
        mainHandler.removeCallbacks(autoCenter)
        if (TrailPreferences.getMapAutoCenter()) mainHandler.postDelayed(autoCenter, AUTO_CENTER_DELAY)
    }

    private fun recenter() {
        val current = location ?: return
        if (TrailPreferences.arePolylinesWrapped()) return
        moveTo(current.latitude, current.longitude, TrailPreferences.getMapZoom())
    }

    private fun drawPoints() {
        if (!hasStyle()) return
        points.map { it.iconPosition }.filter { it !in icons }.toSet().forEach {
            image(iconName(it), TrailIcons.icons.getOrElse(it) { TrailIcons.icons[0] }.toBitmap(context, ICON_SIZE))
            icons.add(it)
        }
        source(OsmStyle.LINE, OsmStyle.line(points.map { it.latitude to it.longitude }))
        source(OsmStyle.MARKERS, OsmStyle.markers(points.map { Triple(it.latitude, it.longitude, iconName(it.iconPosition)) }))
    }

    private fun drawPuck() {
        val point = location?.let { it.latitude to it.longitude }
            ?: MainPreferences.getLastCoordinates().let { it[0].toDouble() to it[1].toDouble() }
        val fix = location
        val compass = TrailPreferences.isCompassRotation()
        val pin = TrackPin.drawable(fix != null, compass, headingAccuracy, (fix?.speed ?: 0f) > 0f)
        drawPin(pin, point.first, point.second, centered = true, rotation = if (compass) heading else fix?.bearing ?: 0f) {
            TrackPin.bitmap(context, pin)
        }
        if (fix == null) return
        drawAccuracy(
            point.first,
            point.second,
            fix.accuracy.toDouble(),
            ContextCompat.getColor(context, R.color.map_circle_color),
            ContextCompat.getColor(context, R.color.compass_pin_color),
        )
    }

    private fun iconName(position: Int) = "trail-$position"

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            TrailPreferences.compass -> {
                if (TrailPreferences.isCompassRotation()) registerHeadingLater() else unregisterHeading()
                drawPuck()
            }
            TrailPreferences.mapAutoCenter -> scheduleAutoCenter()
        }
    }

    private companion object {
        const val AUTO_CENTER_DELAY = 6000L
        const val ICON_SIZE = 50
    }
}
