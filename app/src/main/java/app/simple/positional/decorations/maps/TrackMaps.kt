package app.simple.positional.decorations.maps

import android.content.Context
import android.graphics.Bitmap
import android.hardware.SensorManager
import android.location.Location
import android.view.View
import androidx.annotation.DrawableRes
import app.simple.positional.R
import app.simple.positional.decorations.measure.MeasureMaps
import app.simple.positional.decorations.trails.TrailMaps
import app.simple.positional.extensions.maps.MapLifecycle
import app.simple.positional.extensions.maps.MapsCallbacks
import app.simple.positional.model.Measure
import app.simple.positional.model.MeasurePoint
import app.simple.positional.model.TrailPoint
import app.simple.positional.preferences.MeasurePreferences
import app.simple.positional.preferences.TrailPreferences
import app.simple.positional.util.BitmapHelper.toBitmap
import app.simple.positional.util.BitmapHelper.toBitmapKeepingSize
import app.simple.positional.util.MapSource
import com.google.android.gms.maps.model.LatLng

interface TrailMap : PanelMap {
    fun showPoints(points: List<TrailPoint>)
    fun removeLast()
    fun toggleWrap()
    fun moveTo(latitude: Double, longitude: Double, zoom: Float)
    fun clear()

    companion object {
        fun create(context: Context): TrailMap = if (MapSource.isOsm()) OsmTrailMap(context) else GoogleTrailMap(TrailMaps(context))
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
        fun create(context: Context): MeasureMap = if (MapSource.isOsm()) OsmMeasureMap(context) else GoogleMeasureMap(MeasureMaps(context))
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

class GoogleTrailMap(private val maps: TrailMaps) : TrailMap {
    init {
        maps.alpha = 0f
        maps.onCreate(null)
    }

    override val view: View get() = maps
    override val lifecycleObserver = MapLifecycle(maps)
    override fun setCallbacks(callbacks: MapsCallbacks) = maps.setOnMapsCallbackListener(callbacks)

    override fun updateLocation(location: Location) {
        maps.setFirstLocation(location)
        maps.location = location
        maps.addMarker(LatLng(location.latitude, location.longitude))
    }

    override fun resetCamera(zoom: Float) = maps.resetCamera(zoom)
    override fun showPoints(points: List<TrailPoint>) = maps.addPolylines(ArrayList(points))
    override fun removeLast() = maps.removePolyline()
    override fun toggleWrap() = maps.wrapUnwrap()
    override fun moveTo(latitude: Double, longitude: Double, zoom: Float) =
        maps.moveMapCamera(LatLng(latitude, longitude), zoom, TrailPreferences.getMapTilt())
    override fun clear() = maps.clear()
}

class GoogleMeasureMap(private val maps: MeasureMaps) : MeasureMap {
    init {
        maps.alpha = 0f
        maps.onCreate(null)
    }

    override val view: View get() = maps
    override val lifecycleObserver = MapLifecycle(maps)
    override val points: List<MeasurePoint> get() = maps.getMeasurePoints()
    override fun setCallbacks(callbacks: MapsCallbacks) = maps.setOnMapsCallbackListener(callbacks)

    override fun updateLocation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        maps.setFirstLocation(latLng)
        maps.location = location
        maps.addMarker(latLng)
    }

    override fun resetCamera(zoom: Float) = maps.resetCamera(zoom)
    override fun showMeasure(measure: Measure) = maps.createMeasurePolylines(measure)
    override fun addPoint() = maps.addPolyline()
    override fun removeLast() = maps.removeRecentPolyline()
    override fun moveTo(latitude: Double, longitude: Double, zoom: Float) =
        maps.moveMapCamera(LatLng(latitude, longitude), zoom, MeasurePreferences.getMapTilt(), MeasurePreferences.getMapBearing())
    override fun clear() = maps.clear()
}
