package app.simple.positional.decorations.maps

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import app.simple.positional.R
import app.simple.positional.decorations.utils.animateColors
import app.simple.positional.decorations.utils.animateTo
import app.simple.positional.extensions.maps.MapsCallbacks
import app.simple.positional.sensors.HeadingSensor
import app.simple.positional.singleton.SharedPreferences.getSharedPreferences
import app.simple.positional.util.BitmapHelper.toBitmap
import app.simple.positional.util.ColorUtils.resolveAttrColor
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.CustomCap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PatternItem
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import kotlin.math.roundToInt

data class MapLook(val label: Boolean, val satellite: Boolean, val highContrast: Boolean, val buildings: Boolean, val traffic: Boolean)

object GoogleMapStyle {
    fun type(satellite: Boolean, label: Boolean): Int = when {
        !satellite -> GoogleMap.MAP_TYPE_NORMAL
        label -> GoogleMap.MAP_TYPE_HYBRID
        else -> GoogleMap.MAP_TYPE_SATELLITE
    }

    @RawRes
    fun raw(label: Boolean, highContrast: Boolean, night: Boolean): Int = when {
        highContrast -> if (label) R.raw.map_high_contrast_labelled else R.raw.map_high_contrast_non_labelled
        night -> if (label) R.raw.maps_dark_labelled else R.raw.maps_dark_no_label
        else -> if (label) R.raw.maps_light_labelled else R.raw.maps_light_no_label
    }
}

@SuppressLint("ViewConstructor")
abstract class GoogleMapView(context: Context) : MapView(context), MapHost, HeadingSensor.Listener,
    SharedPreferences.OnSharedPreferenceChangeListener {
    override val view: View get() = this
    protected var map: GoogleMap? = null
    protected var mapCallbacks: MapsCallbacks? = null
    protected var location: Location? = null
    protected var heading = 0f
    protected var headingAccuracy = -1
    protected var animating = false
    protected val mainHandler = Handler(Looper.getMainLooper())
    protected val accent = context.resolveAttrColor(R.attr.colorAppAccent)
    private var owner: LifecycleOwner? = null
    private var sensorOn = false
    private val headingSensor = HeadingSensor(context, this)
    private val registerTask = Runnable { registerHeading() }
    private var pin: Marker? = null
    private var pinKey: Any? = null
    private var circle: Circle? = null
    private var pinAnimator: ValueAnimator? = null
    private var circleAnimator: ValueAnimator? = null
    private var colorAnimator: ValueAnimator? = null
    private val startCap by lazy { cap(R.drawable.ic_trail_start) }
    private val endCap by lazy { cap(R.drawable.seekbar_thumb) }

    protected abstract val compassRotation: Boolean
    protected abstract val lookKeys: Set<String>
    protected abstract fun look(): MapLook
    protected abstract fun startCamera(): CameraPosition
    protected abstract fun onCameraIdle(position: CameraPosition)
    protected open fun onReady() {}
    protected open fun onClick(latLng: LatLng) {
        mapCallbacks?.onMapClicked()
    }
    protected open fun onGesture() {}
    protected open fun onCameraMoved() {}
    protected open fun onHeadingChanged() {}
    protected open fun onHeadingAccuracyChanged() {}
    protected open fun onResumed() {}
    protected open fun onPaused() {}
    protected open fun onPreference(key: String?) {}

    override val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) = this@GoogleMapView.onStart()

        override fun onResume(owner: LifecycleOwner) {
            this@GoogleMapView.owner = owner
            this@GoogleMapView.onResume()
            getSharedPreferences().registerOnSharedPreferenceChangeListener(this@GoogleMapView)
            registerHeadingLater()
            onResumed()
        }

        override fun onPause(owner: LifecycleOwner) {
            onPaused()
            unregisterHeading()
            getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this@GoogleMapView)
            this@GoogleMapView.onPause()
        }

        override fun onStop(owner: LifecycleOwner) = this@GoogleMapView.onStop()

        override fun onDestroy(owner: LifecycleOwner) {
            mainHandler.removeCallbacksAndMessages(null)
            listOf(pinAnimator, circleAnimator, colorAnimator).forEach { it?.cancel() }
            this@GoogleMapView.owner = null
            this@GoogleMapView.onDestroy()
        }
    }

    init {
        alpha = 0f
        onCreate(null)
        getMapAsync { ready(it) }
    }

    private fun ready(map: GoogleMap) {
        this.map = map
        map.uiSettings.isCompassEnabled = false
        map.uiSettings.isMapToolbarEnabled = false
        map.uiSettings.isMyLocationButtonEnabled = false
        applyLook()
        map.moveCamera(CameraUpdateFactory.newCameraPosition(startCamera()))
        map.setOnMapClickListener { onClick(it) }
        map.setOnMapLongClickListener { mapCallbacks?.onMapLongClicked(it) }
        map.setOnCameraMoveStartedListener { reason ->
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                animating = false
                onGesture()
            }
        }
        map.setOnCameraMoveListener { onCameraMoved() }
        map.setOnCameraIdleListener { onCameraIdle(map.cameraPosition) }
        onReady()
        animate().alpha(1f).setDuration(FADE).start()
    }

    final override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key in lookKeys) applyLook() else onPreference(key)
    }

    private fun applyLook() {
        val map = map ?: return
        val look = look()
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        map.mapType = GoogleMapStyle.type(look.satellite, look.label)
        runCatching { map.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, GoogleMapStyle.raw(look.label, look.highContrast, night))) }
        map.isBuildingsEnabled = look.buildings
        map.isTrafficEnabled = look.traffic
    }

    override fun onHeading(azimuth: Float) {
        heading = azimuth
        onHeadingChanged()
    }

    override fun onAccuracy(type: Int, accuracy: Int) {
        if (headingAccuracy == accuracy) return
        headingAccuracy = accuracy
        onHeadingAccuracyChanged()
    }

    protected fun registerHeadingLater() {
        mainHandler.removeCallbacks(registerTask)
        mainHandler.postDelayed(registerTask, SENSOR_DELAY)
    }

    protected fun unregisterHeading() {
        mainHandler.removeCallbacks(registerTask)
        if (sensorOn) owner?.let(headingSensor::onPause)
        sensorOn = false
    }

    private fun registerHeading() {
        val current = owner ?: return
        if (sensorOn || !compassRotation || !headingSensor.isAvailable) return
        headingSensor.onResume(current)
        sensorOn = true
    }

    protected fun drawPin(key: Any, position: LatLng, centered: Boolean, rotation: Float?, bitmap: () -> Bitmap) {
        val map = map ?: return
        val anchor = if (centered) 0.5f else 1f
        val marker = pin
        if (marker == null) {
            pin = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .anchor(0.5f, anchor)
                    .flat(centered)
                    .rotation(rotation ?: heading)
                    .icon(BitmapDescriptorFactory.fromBitmap(bitmap()))
            )
            pinKey = key
            return
        }
        if (pinKey != key) {
            marker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap()))
            pinKey = key
        }
        marker.setAnchor(0.5f, anchor)
        marker.isFlat = centered
        pinAnimator?.cancel()
        pinAnimator = marker.animateTo(position, rotation)
    }

    protected fun rotatePin(rotation: Float) {
        pin?.rotation = rotation
    }

    protected fun invalidatePin() {
        pinKey = null
    }

    protected fun drawAccuracy(position: LatLng, radius: Double, @ColorInt fill: Int, @ColorInt stroke: Int) {
        val map = map ?: return
        val current = circle
        if (current == null) {
            circle = map.addCircle(
                CircleOptions().center(position).radius(radius).fillColor(fill).strokeColor(stroke).strokeWidth(CIRCLE_STROKE).clickable(false)
            )
            return
        }
        circleAnimator?.cancel()
        circleAnimator = current.animateTo(position, radius)
        if (current.fillColor != fill || current.strokeColor != stroke) {
            colorAnimator?.cancel()
            colorAnimator = current.animateColors(fill, stroke)
        }
    }

    protected fun line(
        points: List<LatLng>,
        @ColorInt color: Int = accent,
        width: Float = LINE_WIDTH,
        geodesic: Boolean = false,
        pattern: List<PatternItem>? = null,
    ): Polyline? {
        val map = map ?: return null
        val options = PolylineOptions()
            .addAll(points)
            .width(width)
            .color(color)
            .jointType(JointType.ROUND)
            .geodesic(geodesic)
            .startCap(startCap)
            .endCap(endCap)
        if (pattern != null) options.pattern(pattern)
        return map.addPolyline(options)
    }

    private fun cap(@DrawableRes drawable: Int) = CustomCap(BitmapDescriptorFactory.fromBitmap(drawable.toBitmap(context, CAP_SIZE)))

    protected fun target(): LatLng? = map?.cameraPosition?.target

    protected fun isVisible(position: LatLng): Boolean = map?.projection?.visibleRegion?.latLngBounds?.contains(position) ?: true

    protected fun camera(target: LatLng, zoom: Float, tilt: Float, bearing: Float) = CameraPosition(target, zoom, tilt, bearing)

    protected fun moveCamera(position: CameraPosition) {
        map?.moveCamera(CameraUpdateFactory.newCameraPosition(position))
    }

    protected fun animateTo(position: CameraPosition) {
        val map = map ?: return
        animating = true
        map.animateCamera(CameraUpdateFactory.newCameraPosition(position), CAMERA_SPEED, object : GoogleMap.CancelableCallback {
            override fun onCancel() {
                animating = false
            }

            override fun onFinish() {
                animating = false
            }
        })
    }

    protected fun fit(points: List<LatLng>, animate: Boolean): Boolean {
        val map = map ?: return false
        if (points.size < 2) return false
        val bounds = LatLngBounds.builder().apply { points.forEach { include(it) } }.build()
        val update = CameraUpdateFactory.newLatLngBounds(bounds, (FIT_PADDING * resources.displayMetrics.density).roundToInt())
        return runCatching { if (animate) map.animateCamera(update) else map.moveCamera(update) }.isSuccess
    }

    protected fun zoom(zoomIn: Boolean) {
        map?.animateCamera(if (zoomIn) CameraUpdateFactory.zoomIn() else CameraUpdateFactory.zoomOut())
    }

    protected fun rotate(bearing: Float, animate: Boolean) {
        val map = map ?: return
        val update = CameraUpdateFactory.newCameraPosition(CameraPosition.Builder(map.cameraPosition).bearing(bearing).build())
        if (animate) map.animateCamera(update) else map.moveCamera(update)
    }

    protected companion object {
        const val CAMERA_SPEED = 1000
        const val SENSOR_DELAY = 500L
        const val FIT_PADDING = 64
        const val FADE = 300L
        const val LINE_WIDTH = 10f
        const val CIRCLE_STROKE = 3f
        const val CAP_SIZE = 30
    }
}

@SuppressLint("ViewConstructor")
abstract class GooglePanelMap(context: Context) : GoogleMapView(context), PanelMap {
    override fun setCallbacks(callbacks: MapsCallbacks) {
        mapCallbacks = callbacks
    }
}
