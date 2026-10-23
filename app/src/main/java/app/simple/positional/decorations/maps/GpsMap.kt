package app.simple.positional.decorations.maps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.location.Location
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import app.simple.positional.R
import app.simple.positional.constants.LocationPins
import app.simple.positional.extensions.maps.MapLifecycle
import app.simple.positional.extensions.maps.MapsCallbacks
import app.simple.positional.preferences.GPSPreferences
import app.simple.positional.util.BitmapHelper.addLinearGradient
import app.simple.positional.util.BitmapHelper.toBitmapKeepingSize
import app.simple.positional.util.MapSource
import com.google.android.gms.maps.model.LatLng

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
        fun create(context: Context): GpsMap = if (MapSource.isOsm()) OsmGpsMap(context) else GoogleGpsMap(Maps(context))
    }
}

object GpsPin {
    private val customGradient = intArrayOf(Color.parseColor("#FF1B50"), Color.parseColor("#e11677"))

    fun bitmap(context: Context, custom: Boolean, moving: Boolean): Bitmap {
        val icon = if (moving && !custom) R.drawable.ic_bearing_marker else LocationPins.getLocationPin()
        val bitmap = icon.toBitmapKeepingSize(context, GPSPreferences.getPinSize(), GPSPreferences.getPinOpacity())
        return if (custom) bitmap.addLinearGradient(customGradient) else bitmap
    }
}

class GoogleGpsMap(private val maps: Maps) : GpsMap {
    init {
        maps.alpha = 0f
        maps.onCreate(null)
    }

    override val view: View get() = maps
    override val lifecycleObserver = MapLifecycle(maps)
    override var isMapMovementEnabled by maps::isMapMovementEnabled
    override var onTouch: ((MotionEvent) -> Unit)? = null
        set(value) {
            field = value
            maps.onTouch = value?.let { touch -> { event: MotionEvent, _: Boolean -> touch(event) } }
        }

    override fun setCallbacks(callbacks: MapsCallbacks) = maps.setOnMapsCallbackListener(callbacks)

    override fun updateLocation(location: Location) {
        maps.setFirstLocation(location)
        maps.location = location
        maps.addMarker(LatLng(location.latitude, location.longitude))
    }

    override fun screenPoint(latLng: LatLng): Point? = maps.screenPoint(latLng)
    override fun resetCamera(zoom: Float) = maps.resetCamera(zoom)
    override fun setTargetMarker(latLng: LatLng?) = maps.setTargetMarker(latLng)
    override fun moveCameraToTarget() = maps.moveCameraToTarget()
    override fun zoomIn() = maps.zoomIn()
    override fun zoomOut() = maps.zoomOut()
    override fun registerWithRunnable() = maps.registerWithRunnable()
    override fun unregister() = maps.unregister()
}
