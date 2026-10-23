package app.simple.positional.decorations.maps

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import androidx.core.content.ContextCompat
import app.simple.positional.R
import app.simple.positional.model.Measure
import app.simple.positional.model.MeasurePoint
import app.simple.positional.preferences.MeasurePreferences
import com.google.android.gms.maps.model.LatLng
import org.maplibre.android.camera.CameraPosition

@SuppressLint("ViewConstructor")
class OsmMeasureMap(context: Context) : OsmPanelMap(context), MeasureMap {
    override val compassRotation get() = MeasurePreferences.isCompassRotation()
    private val measurePoints = mutableListOf<MeasurePoint>()
    override val points: List<MeasurePoint> get() = measurePoints
    private var restore: CameraPosition? = null

    override fun startCamera(): CameraPosition = camera(
        MeasurePreferences.getLastLatitude(),
        MeasurePreferences.getLastLongitude(),
        MeasurePreferences.getMapZoom(),
        MeasurePreferences.getMapTilt(),
        MeasurePreferences.getMapBearing(),
    )

    override fun onCameraIdle(position: CameraPosition) {
        MeasurePreferences.setMapZoom(position.zoom.toFloat())
        MeasurePreferences.setMapTilt(position.tilt.toFloat())
        MeasurePreferences.setMapBearing(position.bearing.toFloat())
        position.target?.let {
            MeasurePreferences.setLastLatitude(it.latitude)
            MeasurePreferences.setLastLongitude(it.longitude)
        }
    }

    override fun onCameraMoved() {
        drawGuide()
        reportDistance()
    }

    override fun onStyleReady() {
        drawLines()
        drawPuck()
        reportDistance()
        if (MeasurePreferences.arePolylinesWrapped()) wrap(animate = false)
    }

    override fun onHeadingChanged() = drawPuck()

    override fun updateLocation(location: Location) {
        this.location = location
        drawPuck()
    }

    override fun resetCamera(zoom: Float) {
        location?.let { moveTo(it.latitude, it.longitude, zoom) }
    }

    override fun showMeasure(measure: Measure) {
        measurePoints.clear()
        measurePoints.addAll(measure.measurePoints)
        drawLines()
        callbacks?.onLineCountChanged(measurePoints.size)
        if (MeasurePreferences.arePolylinesWrapped()) wrap(animate = false)
    }

    override fun addPoint() {
        val (latitude, longitude) = target() ?: return
        val point = MeasurePoint(latitude, longitude, measurePoints.size + 1)
        measurePoints.add(point)
        drawLines()
        callbacks?.onLineAdded(point)
        reportDistance()
    }

    override fun removeLast() {
        val last = measurePoints.removeLastOrNull() ?: return
        drawLines()
        callbacks?.onLineDeleted(last)
        callbacks?.onLineCountChanged(measurePoints.size)
        reportDistance()
    }

    override fun moveTo(latitude: Double, longitude: Double, zoom: Float) {
        restore = null
        MeasurePreferences.setPolylinesWrapped(false)
        animateTo(camera(latitude, longitude, zoom, MeasurePreferences.getMapTilt(), MeasurePreferences.getMapBearing()))
    }

    override fun clear() {
        measurePoints.clear()
        drawLines()
    }

    private fun wrap(animate: Boolean) {
        if (restore == null) restore = map?.cameraPosition
        fit(measurePoints.map { it.latitude to it.longitude }, animate)
    }

    private fun drawLines() {
        source(OsmStyle.LINE, OsmStyle.line(measurePoints.map { it.latitude to it.longitude }))
        drawGuide()
    }

    private fun drawGuide() {
        val last = measurePoints.lastOrNull()
        val target = target()
        source(OsmStyle.GUIDE, if (last == null || target == null) OsmStyle.EMPTY_FEATURES else OsmStyle.line(listOf(last.latitude to last.longitude, target)))
    }

    private fun reportDistance() {
        target()?.let { callbacks?.onCameraDistance(LatLng(it.first, it.second)) }
    }

    private fun drawPuck() {
        val fix = location ?: return
        val compass = MeasurePreferences.isCompassRotation()
        val pin = TrackPin.drawable(true, compass, headingAccuracy, fix.speed > 0f)
        drawPin(pin, fix.latitude, fix.longitude, centered = true, rotation = if (compass) heading else fix.bearing) {
            TrackPin.bitmap(context, pin)
        }
        drawAccuracy(
            fix.latitude,
            fix.longitude,
            fix.accuracy.toDouble(),
            ContextCompat.getColor(context, R.color.map_circle_color),
            ContextCompat.getColor(context, R.color.compass_pin_color),
        )
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            MeasurePreferences.COMPASS_ROTATION -> {
                if (MeasurePreferences.isCompassRotation()) registerHeadingLater() else unregisterHeading()
                drawPuck()
            }
            MeasurePreferences.POLYLINES_WRAPPED -> if (MeasurePreferences.arePolylinesWrapped()) {
                wrap(animate = true)
            } else {
                restore?.let(::animateTo)
                restore = null
            }
        }
    }
}
