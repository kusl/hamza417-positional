package app.simple.positional.decorations.maps

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Point
import android.location.Location
import android.view.View
import androidx.annotation.ColorInt
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import app.simple.positional.R
import app.simple.positional.decorations.utils.DelayedTask
import app.simple.positional.decorations.utils.child
import app.simple.positional.extensions.maps.MapsCallbacks
import app.simple.positional.sensors.HeadingSensor
import app.simple.positional.util.ColorUtils.resolveAttrColor
import com.google.android.gms.maps.model.LatLng
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.PropertyValue
import org.maplibre.android.style.sources.GeoJsonSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlin.math.roundToInt
import org.maplibre.android.geometry.LatLng as LibreLatLng

@SuppressLint("ViewConstructor")
abstract class OsmMapView(context: Context) : MapView(OsmTiles.init(context)), MapHost, HeadingSensor.Listener {
    override val view: View get() = this
    protected var map: MapLibreMap? = null
    protected var mapCallbacks: MapsCallbacks? = null
    protected var location: Location? = null
    protected var heading = 0f
    protected var headingAccuracy = -1
    protected var animating = false
    protected var resumedScope: CoroutineScope? = null
        private set
    private var style: Style? = null
    private var pinKey: Any? = null
    private var owner: LifecycleOwner? = null
    private var sensorOn = false
    private val headingSensor = HeadingSensor(context, this)
    private val registerTask = DelayedTask(SENSOR_DELAY, ::registerHeading)
    private val accent = context.resolveAttrColor(R.attr.colorAppAccent)
    private val preferences by lazy { MapPreferences().also(::bindPreferences) }

    protected abstract val compassRotation: Boolean
    protected open val compassKey: String? = null
    protected abstract fun startCamera(): CameraPosition
    protected abstract fun onCameraIdle(position: CameraPosition)
    protected open fun onStyleReady() {}
    protected open fun onClick(latitude: Double, longitude: Double) {
        mapCallbacks?.onMapClicked()
    }
    protected open fun onGesture() {}
    protected open fun onCameraMoved() {}
    protected open fun onHeadingChanged() {}
    protected open fun onResumed() {}
    protected open fun onCompassRotation(enabled: Boolean) {}

    protected open fun bindPreferences(preferences: MapPreferences) {
        compassKey?.let { key ->
            preferences.watch(key, read = { compassRotation }) { enabled ->
                if (enabled) registerHeadingLater() else unregisterHeading()
                onCompassRotation(enabled)
            }
        }
    }

    override val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) = this@OsmMapView.onStart()

        override fun onResume(owner: LifecycleOwner) {
            this@OsmMapView.owner = owner
            this@OsmMapView.onResume()
            resumedScope = owner.lifecycleScope.child().also { preferences.launchIn(it) }
            registerHeadingLater()
            onResumed()
        }

        override fun onPause(owner: LifecycleOwner) {
            unregisterHeading()
            resumedScope?.cancel()
            resumedScope = null
            this@OsmMapView.onPause()
        }

        override fun onStop(owner: LifecycleOwner) = this@OsmMapView.onStop()

        override fun onDestroy(owner: LifecycleOwner) {
            this@OsmMapView.owner = null
            this@OsmMapView.onDestroy()
        }
    }

    init {
        onCreate(null)
        getMapAsync(::ready)
    }

    private fun ready(map: MapLibreMap) {
        this.map = map
        map.uiSettings.isCompassEnabled = false
        map.uiSettings.isLogoEnabled = false
        map.moveCamera(CameraUpdateFactory.newCameraPosition(startCamera()))
        map.addOnMapClickListener {
            onClick(it.latitude, it.longitude)
            true
        }
        map.addOnMapLongClickListener {
            mapCallbacks?.onMapLongClicked(LatLng(it.latitude, it.longitude))
            true
        }
        map.addOnCameraMoveStartedListener { reason ->
            if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                animating = false
                onGesture()
            }
        }
        map.addOnCameraMoveListener { onCameraMoved() }
        map.addOnCameraIdleListener { onCameraIdle(map.cameraPosition) }
        val dark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        map.setStyle(Style.Builder().fromJson(OsmStyle.json(dark))) {
            style = it
            pinKey = null
            layer(OsmStyle.LINE, PropertyFactory.lineColor(accent))
            layer(OsmStyle.GUIDE, PropertyFactory.lineColor(accent))
            onStyleReady()
        }
    }

    override fun onHeading(azimuth: Float) {
        heading = azimuth
        onHeadingChanged()
    }

    override fun onAccuracy(type: Int, accuracy: Int) {
        headingAccuracy = accuracy
    }

    protected fun registerHeadingLater() {
        registerTask.schedule(resumedScope)
    }

    protected fun unregisterHeading() {
        registerTask.cancel()
        if (sensorOn) owner?.let(headingSensor::onPause)
        sensorOn = false
    }

    private fun registerHeading() {
        val current = owner ?: return
        if (sensorOn || !compassRotation || !headingSensor.isAvailable) return
        headingSensor.onResume(current)
        sensorOn = true
    }

    protected fun layer(id: String, vararg properties: PropertyValue<*>) {
        style?.getLayer(id)?.setProperties(*properties)
    }

    protected fun source(id: String, json: String) {
        style?.getSourceAs<GeoJsonSource>(id)?.setGeoJson(json)
    }

    protected fun image(name: String, bitmap: Bitmap) {
        style?.addImage(name, bitmap)
    }

    protected fun hasStyle() = style != null

    protected fun drawPin(key: Any, latitude: Double, longitude: Double, centered: Boolean, rotation: Float, bitmap: () -> Bitmap) {
        if (style == null) return
        if (pinKey != key) {
            image(OsmStyle.PIN, bitmap())
            pinKey = key
        }
        source(OsmStyle.PIN, OsmStyle.point(latitude, longitude))
        layer(
            OsmStyle.PIN,
            PropertyFactory.iconAnchor(if (centered) Property.ICON_ANCHOR_CENTER else Property.ICON_ANCHOR_BOTTOM),
            PropertyFactory.iconRotate(rotation),
        )
    }

    protected fun invalidatePin() {
        pinKey = null
    }

    protected fun drawAccuracy(latitude: Double, longitude: Double, radius: Double, @ColorInt fill: Int, @ColorInt stroke: Int) {
        source(OsmStyle.ACCURACY, OsmStyle.circle(latitude, longitude, radius))
        layer(OsmStyle.ACCURACY, PropertyFactory.fillColor(fill), PropertyFactory.fillOutlineColor(stroke))
    }

    protected fun target(): Pair<Double, Double>? = map?.cameraPosition?.target?.let { it.latitude to it.longitude }

    protected fun isVisible(latitude: Double, longitude: Double): Boolean =
        map?.projection?.visibleRegion?.latLngBounds?.contains(LibreLatLng(latitude, longitude)) ?: true

    protected fun screenPointOf(latitude: Double, longitude: Double): Point? =
        map?.projection?.toScreenLocation(LibreLatLng(latitude, longitude))?.let { Point(it.x.roundToInt(), it.y.roundToInt()) }

    protected fun camera(latitude: Double, longitude: Double, zoom: Float, tilt: Float, bearing: Float): CameraPosition = CameraPosition.Builder()
        .target(LibreLatLng(latitude, longitude))
        .zoom(zoom.toDouble())
        .tilt(tilt.toDouble())
        .bearing(bearing.toDouble())
        .build()

    protected fun moveCamera(position: CameraPosition) {
        map?.moveCamera(CameraUpdateFactory.newCameraPosition(position))
    }

    protected fun animateTo(position: CameraPosition) {
        val map = map ?: return
        animating = true
        map.animateCamera(CameraUpdateFactory.newCameraPosition(position), CAMERA_SPEED, object : MapLibreMap.CancelableCallback {
            override fun onCancel() {
                animating = false
            }

            override fun onFinish() {
                animating = false
            }
        })
    }

    protected fun fit(points: List<Pair<Double, Double>>, animate: Boolean): Boolean {
        val map = map ?: return false
        if (points.size < 2) return false
        val bounds = LatLngBounds.Builder().includes(points.map { LibreLatLng(it.first, it.second) }).build()
        val update = CameraUpdateFactory.newLatLngBounds(bounds, (FIT_PADDING * resources.displayMetrics.density).roundToInt())
        if (animate) map.animateCamera(update) else map.moveCamera(update)
        return true
    }

    protected fun zoom(zoomIn: Boolean) {
        map?.animateCamera(if (zoomIn) CameraUpdateFactory.zoomIn() else CameraUpdateFactory.zoomOut())
    }

    protected fun rotate(bearing: Float, animate: Boolean) {
        val update = CameraUpdateFactory.bearingTo(bearing.toDouble())
        if (animate) map?.animateCamera(update) else map?.moveCamera(update)
    }

    private companion object {
        const val CAMERA_SPEED = 1000
        const val SENSOR_DELAY = 500L
        const val FIT_PADDING = 64
    }
}

@SuppressLint("ViewConstructor")
abstract class OsmPanelMap(context: Context) : OsmMapView(context), PanelMap {
    override fun setCallbacks(callbacks: MapsCallbacks) {
        mapCallbacks = callbacks
    }
}

object OsmTiles {
    private const val CACHE_BYTES = 128L * 1024 * 1024
    private var ready = false

    fun init(context: Context): Context {
        if (!ready) {
            MapLibre.getInstance(context.applicationContext)
            OfflineManager.getInstance(context.applicationContext).setMaximumAmbientCacheSize(CACHE_BYTES, null)
            ready = true
        }
        return context
    }
}
