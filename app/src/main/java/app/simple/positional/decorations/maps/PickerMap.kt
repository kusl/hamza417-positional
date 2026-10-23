package app.simple.positional.decorations.maps

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.view.View
import app.simple.positional.R
import app.simple.positional.extensions.maps.MapLifecycle
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.util.MapSource
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import org.maplibre.android.camera.CameraPosition

interface PickerMap : MapHost {
    var onIdle: ((Double, Double) -> Unit)?
    fun moveTo(latitude: Double, longitude: Double, animate: Boolean)

    companion object {
        const val ZOOM = 15F

        fun create(context: Context): PickerMap = if (MapSource.isOsm()) OsmPickerMap(context) else GooglePickerMap(context)
    }
}

class GooglePickerMap(context: Context) : PickerMap {
    private val mapView = MapView(context).apply { onCreate(null) }
    private var map: GoogleMap? = null
    private var pending: Pair<Double, Double>? = null
    override val view: View get() = mapView
    override val lifecycleObserver = MapLifecycle(mapView)
    override var onIdle: ((Double, Double) -> Unit)? = null

    init {
        mapView.getMapAsync { googleMap ->
            map = googleMap
            googleMap.uiSettings.isMapToolbarEnabled = false
            googleMap.uiSettings.isCompassEnabled = false
            val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            runCatching {
                googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, if (night) R.raw.maps_dark_labelled else R.raw.maps_light_labelled))
            }
            pending?.let { (latitude, longitude) -> moveTo(latitude, longitude, animate = false) }
            googleMap.setOnCameraIdleListener { googleMap.cameraPosition.target.let { onIdle?.invoke(it.latitude, it.longitude) } }
            googleMap.setOnMapClickListener { googleMap.animateCamera(CameraUpdateFactory.newLatLng(it)) }
        }
    }

    override fun moveTo(latitude: Double, longitude: Double, animate: Boolean) {
        val map = map ?: run {
            pending = latitude to longitude
            return
        }
        val update = CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), PickerMap.ZOOM)
        if (animate) map.animateCamera(update) else map.moveCamera(update)
    }
}

@SuppressLint("ViewConstructor")
class OsmPickerMap(context: Context) : OsmMapView(context), PickerMap {
    override val compassRotation = false
    override var onIdle: ((Double, Double) -> Unit)? = null
    private var pending: Pair<Double, Double>? = null

    override fun startCamera(): CameraPosition {
        val (latitude, longitude) = pending ?: MainPreferences.getLastCoordinates().let { it[0].toDouble() to it[1].toDouble() }
        return camera(latitude, longitude, PickerMap.ZOOM, 0f, 0f)
    }

    override fun onCameraIdle(position: CameraPosition) {
        position.target?.let { onIdle?.invoke(it.latitude, it.longitude) }
    }

    override fun onClick(latitude: Double, longitude: Double) {
        val zoom = map?.cameraPosition?.zoom?.toFloat() ?: PickerMap.ZOOM
        animateTo(camera(latitude, longitude, zoom, 0f, 0f))
    }

    override fun moveTo(latitude: Double, longitude: Double, animate: Boolean) {
        pending = latitude to longitude
        val position = camera(latitude, longitude, PickerMap.ZOOM, 0f, 0f)
        if (animate) animateTo(position) else moveCamera(position)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) = Unit
}
