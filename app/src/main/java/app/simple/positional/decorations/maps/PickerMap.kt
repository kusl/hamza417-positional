package app.simple.positional.decorations.maps

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.util.MapSource
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.CameraPosition as GoogleCameraPosition
import org.maplibre.android.camera.CameraPosition

interface PickerMap : MapHost {
    var onIdle: ((Double, Double) -> Unit)?
    fun moveTo(latitude: Double, longitude: Double, animate: Boolean)

    companion object {
        const val ZOOM = 15F

        fun create(context: Context): PickerMap = if (MapSource.isOsm()) OsmPickerMap(context) else GooglePickerMap(context)
    }
}

@SuppressLint("ViewConstructor")
class GooglePickerMap(context: Context) : GoogleMapView(context), PickerMap {
    override val compassRotation = false
    override val lookKeys: Set<String> = emptySet()
    override var onIdle: ((Double, Double) -> Unit)? = null
    private var pending: LatLng? = null

    override fun look() = MapLook(label = true, satellite = false, highContrast = false, buildings = true, traffic = false)

    override fun startCamera(): GoogleCameraPosition =
        camera(pending ?: MainPreferences.getLastCoordinates().let { LatLng(it[0].toDouble(), it[1].toDouble()) }, PickerMap.ZOOM, 0f, 0f)

    override fun onCameraIdle(position: GoogleCameraPosition) {
        onIdle?.invoke(position.target.latitude, position.target.longitude)
    }

    override fun onClick(latLng: LatLng) {
        animateTo(camera(latLng, map?.cameraPosition?.zoom ?: PickerMap.ZOOM, 0f, 0f))
    }

    override fun moveTo(latitude: Double, longitude: Double, animate: Boolean) {
        val target = LatLng(latitude, longitude)
        pending = target
        val position = camera(target, PickerMap.ZOOM, 0f, 0f)
        if (animate) animateTo(position) else moveCamera(position)
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
