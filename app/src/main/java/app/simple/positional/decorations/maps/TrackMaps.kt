package app.simple.positional.decorations.maps

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.SensorManager
import android.location.Location
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import app.simple.positional.R
import app.simple.positional.constants.TrailIcons
import app.simple.positional.decorations.utils.DelayedTask
import app.simple.positional.model.Measure
import app.simple.positional.model.MeasurePoint
import app.simple.positional.model.TrailPoint
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.preferences.MeasurePreferences
import app.simple.positional.preferences.TrailPreferences
import app.simple.positional.util.BitmapHelper.toBitmap
import app.simple.positional.util.BitmapHelper.toBitmapKeepingSize
import app.simple.positional.util.MapSource
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline

interface TrailMap : PanelMap {
    fun showPoints(points: List<TrailPoint>)
    fun removeLast()
    fun toggleWrap()
    fun moveTo(latitude: Double, longitude: Double, zoom: Float)

    companion object {
        fun create(context: Context): TrailMap = if (MapSource.isOsm()) OsmTrailMap(context) else GoogleTrailMap(context)
    }
}

interface MeasureMap : PanelMap {
    val points: List<MeasurePoint>
    fun showMeasure(measure: Measure)
    fun addPoint()
    fun removeLast()
    fun moveTo(latitude: Double, longitude: Double, zoom: Float)
    fun clear()

    companion object {
        fun create(context: Context): MeasureMap = if (MapSource.isOsm()) OsmMeasureMap(context) else GoogleMeasureMap(context)
    }
}

object TrackPin {
    private const val SCALE = 2
    private const val HISTORICAL_SIZE = 60

    @DrawableRes
    fun drawable(hasLocation: Boolean, compass: Boolean, accuracy: Int, moving: Boolean): Int = when {
        !hasLocation -> R.drawable.ic_place_historical
        compass -> when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> R.drawable.ic_pin_low
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> R.drawable.ic_pin_medium
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> R.drawable.ic_pin_high
            else -> R.drawable.ic_pin_unreliable
        }
        moving -> R.drawable.ic_pin_bearing
        else -> R.drawable.ic_pin_no_speed
    }

    fun bitmap(context: Context, @DrawableRes drawable: Int): Bitmap =
        if (drawable == R.drawable.ic_place_historical) drawable.toBitmap(context, HISTORICAL_SIZE) else drawable.toBitmapKeepingSize(context, SCALE)
}

@SuppressLint("ViewConstructor")
abstract class GoogleTrackMap(context: Context) : GooglePanelMap(context) {
    protected fun drawPuck() {
        val compass = compassRotation
        val fix = location
        val point = fix?.let { LatLng(it.latitude, it.longitude) } ?: MainPreferences.getLastCoordinates().let { LatLng(it[0].toDouble(), it[1].toDouble()) }
        val pin = TrackPin.drawable(fix != null, compass, headingAccuracy, (fix?.speed ?: 0f) > 0f)
        drawPin(pin, point, centered = true, rotation = if (compass) null else fix?.bearing ?: 0f) { TrackPin.bitmap(context, pin) }
        if (fix == null) return
        drawAccuracy(
            point,
            fix.accuracy.toDouble(),
            ContextCompat.getColor(context, R.color.map_circle_color),
            ContextCompat.getColor(context, R.color.compass_pin_color),
        )
    }

    override fun onHeadingChanged() {
        if (compassRotation) rotatePin(heading)
    }

    override fun onHeadingAccuracyChanged() = drawPuck()

    override fun onCompassRotation(enabled: Boolean) = drawPuck()
}

@SuppressLint("ViewConstructor")
class GoogleTrailMap(context: Context) : GoogleTrackMap(context), TrailMap {
    override val compassRotation get() = TrailPreferences.isCompassRotation()
    override val compassKey get() = TrailPreferences.compass
    override val lookKeys: Set<String> = setOf(
        TrailPreferences.trailLabelMode,
        TrailPreferences.trailSatellite,
        TrailPreferences.trailHighContrastMap,
        TrailPreferences.trailShowBuilding,
    )
    private var points: List<TrailPoint> = emptyList()
    private var path: Polyline? = null
    private val flags = mutableListOf<Pair<Long, Marker>>()
    private val icons = mutableMapOf<Int, BitmapDescriptor>()
    private var restore: CameraPosition? = null
    private val autoCenter = DelayedTask(AUTO_CENTER_DELAY, ::recenter)

    override fun look() = MapLook(
        TrailPreferences.isLabelOn(),
        TrailPreferences.isSatelliteOn(),
        TrailPreferences.getHighContrastMap(),
        TrailPreferences.getShowBuildingsOnMap(),
        traffic = false,
    )

    override fun startCamera(): CameraPosition {
        val start = location?.let { LatLng(it.latitude, it.longitude) } ?: MainPreferences.getLastCoordinates().let { LatLng(it[0].toDouble(), it[1].toDouble()) }
        return camera(start, TrailPreferences.getMapZoom(), TrailPreferences.getMapTilt(), TrailPreferences.getMapBearing())
    }

    override fun onCameraIdle(position: CameraPosition) {
        TrailPreferences.setMapZoom(position.zoom)
        TrailPreferences.setMapTilt(position.tilt)
        TrailPreferences.setMapBearing(position.bearing)
        scheduleAutoCenter()
    }

    override fun onCameraMoved() = autoCenter.cancel()

    override fun onReady() {
        drawPoints()
        drawPuck()
        if (TrailPreferences.arePolylinesWrapped()) wrap(animate = false)
    }

    override fun onResumed() = scheduleAutoCenter()

    override fun updateLocation(location: Location) {
        val first = this.location == null
        this.location = location
        drawPuck()
        if (first && !TrailPreferences.arePolylinesWrapped()) {
            moveCamera(camera(LatLng(location.latitude, location.longitude), TrailPreferences.getMapZoom(), TrailPreferences.getMapTilt(), TrailPreferences.getMapBearing()))
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
        animateTo(camera(LatLng(latitude, longitude), zoom, TrailPreferences.getMapTilt(), TrailPreferences.getMapBearing()))
    }

    private fun wrap(animate: Boolean) {
        if (!TrailPreferences.arePolylinesWrapped()) restore = map?.cameraPosition
        if (fit(points.map { LatLng(it.latitude, it.longitude) }, animate)) TrailPreferences.setWrapStatus(true)
    }

    private fun scheduleAutoCenter() {
        if (TrailPreferences.getMapAutoCenter()) autoCenter.schedule(resumedScope) else autoCenter.cancel()
    }

    private fun recenter() {
        val current = location ?: return
        if (TrailPreferences.arePolylinesWrapped()) return
        moveTo(current.latitude, current.longitude, TrailPreferences.getMapZoom())
    }

    private fun drawPoints() {
        val map = map ?: return
        val positions = points.map { LatLng(it.latitude, it.longitude) }
        path?.let { it.points = positions } ?: run { path = line(positions, geodesic = TrailPreferences.isTrailGeodesic()) }
        var kept = 0
        while (kept < flags.size && kept < points.size && flags[kept].first == points[kept].timeAdded) kept++
        while (flags.size > kept) flags.removeAt(flags.lastIndex).second.remove()
        points.drop(kept).forEach { point ->
            val marker = map.addMarker(MarkerOptions().position(LatLng(point.latitude, point.longitude)).icon(icon(point.iconPosition)))
            if (marker != null) flags += point.timeAdded to marker
        }
    }

    private fun icon(position: Int): BitmapDescriptor = icons.getOrPut(position) {
        BitmapDescriptorFactory.fromBitmap(TrailIcons.icons.getOrElse(position) { TrailIcons.icons[0] }.toBitmap(context, ICON_SIZE))
    }

    override fun bindPreferences(preferences: MapPreferences) {
        super.bindPreferences(preferences)
        preferences.watch(TrailPreferences.mapAutoCenter, read = TrailPreferences::getMapAutoCenter) { scheduleAutoCenter() }
        preferences.watch(TrailPreferences.geodesic, read = TrailPreferences::isTrailGeodesic) { path?.isGeodesic = it }
    }

    private companion object {
        const val AUTO_CENTER_DELAY = 6000L
        const val ICON_SIZE = 50
    }
}

@SuppressLint("ViewConstructor")
class GoogleMeasureMap(context: Context) : GoogleTrackMap(context), MeasureMap {
    override val compassRotation get() = MeasurePreferences.isCompassRotation()
    override val compassKey get() = MeasurePreferences.COMPASS_ROTATION
    override val lookKeys: Set<String> = setOf(
        MeasurePreferences.LABEL_MODE,
        MeasurePreferences.SATELLITE_MAP,
        MeasurePreferences.HIGH_CONTRAST_MAP,
        MeasurePreferences.SHOW_BUILDINGS,
    )
    private val measurePoints = mutableListOf<MeasurePoint>()
    override val points: List<MeasurePoint> get() = measurePoints
    private var restore: CameraPosition? = null
    private var path: Polyline? = null
    private var guide: Polyline? = null

    override fun look() = MapLook(
        MeasurePreferences.isLabelOn(),
        MeasurePreferences.isSatelliteOn(),
        MeasurePreferences.getHighContrastMap(),
        MeasurePreferences.getShowBuildingsOnMap(),
        traffic = false,
    )

    override fun startCamera(): CameraPosition = camera(
        LatLng(MeasurePreferences.getLastLatitude(), MeasurePreferences.getLastLongitude()),
        MeasurePreferences.getMapZoom(),
        MeasurePreferences.getMapTilt(),
        MeasurePreferences.getMapBearing(),
    )

    override fun onCameraIdle(position: CameraPosition) {
        MeasurePreferences.setMapZoom(position.zoom)
        MeasurePreferences.setMapTilt(position.tilt)
        MeasurePreferences.setMapBearing(position.bearing)
        MeasurePreferences.setLastLatitude(position.target.latitude)
        MeasurePreferences.setLastLongitude(position.target.longitude)
    }

    override fun onCameraMoved() {
        drawGuide()
        reportDistance()
    }

    override fun onReady() {
        drawLines()
        drawPuck()
        reportDistance()
        if (MeasurePreferences.arePolylinesWrapped()) wrap(animate = false)
    }

    override fun updateLocation(location: Location) {
        this.location = location
        drawPuck()
    }

    override fun resetCamera(zoom: Float) {
        location?.let { moveTo(it.latitude, it.longitude, zoom) }
    }

    override fun showMeasure(measure: Measure) {
        measurePoints.clear()
        measurePoints.addAll(measure.points)
        drawLines()
        if (MeasurePreferences.arePolylinesWrapped()) wrap(animate = false)
    }

    override fun addPoint() {
        val target = target() ?: return
        val point = MeasurePoint(target.latitude, target.longitude, measurePoints.size + 1)
        measurePoints.add(point)
        drawLines()
        mapCallbacks?.onLineAdded(point)
        reportDistance()
    }

    override fun removeLast() {
        val last = measurePoints.removeLastOrNull() ?: return
        drawLines()
        mapCallbacks?.onLineDeleted(last)
        reportDistance()
    }

    override fun moveTo(latitude: Double, longitude: Double, zoom: Float) {
        restore = null
        MeasurePreferences.setPolylinesWrapped(false)
        animateTo(camera(LatLng(latitude, longitude), zoom, MeasurePreferences.getMapTilt(), MeasurePreferences.getMapBearing()))
    }

    override fun clear() {
        measurePoints.clear()
        drawLines()
    }

    private fun wrap(animate: Boolean) {
        if (restore == null) restore = map?.cameraPosition
        fit(measurePoints.map { LatLng(it.latitude, it.longitude) }, animate)
    }

    private fun drawLines() {
        val positions = measurePoints.map { LatLng(it.latitude, it.longitude) }
        path?.let { it.points = positions } ?: run { path = line(positions) }
        drawGuide()
    }

    private fun drawGuide() {
        val last = measurePoints.lastOrNull()
        val target = target()
        val positions = if (last == null || target == null) emptyList() else listOf(LatLng(last.latitude, last.longitude), target)
        guide?.let { it.points = positions } ?: run { guide = line(positions, Color.BLACK, GUIDE_WIDTH, pattern = GUIDE_PATTERN) }
    }

    private fun reportDistance() {
        target()?.let { mapCallbacks?.onCameraDistance(it) }
    }

    override fun bindPreferences(preferences: MapPreferences) {
        super.bindPreferences(preferences)
        preferences.watch(MeasurePreferences.POLYLINES_WRAPPED, read = MeasurePreferences::arePolylinesWrapped) { wrapped ->
            if (wrapped) {
                wrap(animate = true)
            } else {
                restore?.let(::animateTo)
                restore = null
            }
        }
    }

    private companion object {
        const val GUIDE_WIDTH = 7f
        val GUIDE_PATTERN = listOf(Dash(20f), Gap(10f), Dash(30f), Gap(20f), Dash(40f), Gap(30f))
    }
}
