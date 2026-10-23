package app.simple.positional.ui.panels

import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import app.simple.positional.R
import app.simple.positional.activities.subactivity.MeasuresActivity
import app.simple.positional.callbacks.BottomBarHost
import app.simple.positional.decorations.measure.MeasureMaps
import app.simple.positional.dialogs.measure.MeasureAdd
import app.simple.positional.dialogs.measure.MeasureAdd.Companion.showMeasureAdd
import app.simple.positional.dialogs.measure.MeasureMenu.Companion.showMeasureMenu
import app.simple.positional.extensions.fragment.ScopedFragment
import app.simple.positional.extensions.maps.MapLifecycle
import app.simple.positional.extensions.maps.MapsCallbacks
import app.simple.positional.math.GeoDistance
import app.simple.positional.model.MeasurePoint
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.preferences.MeasurePreferences
import app.simple.positional.ui.compose.InfoRow
import app.simple.positional.ui.compose.MeasureActions
import app.simple.positional.ui.compose.MeasureScreen
import app.simple.positional.ui.compose.MeasureState
import app.simple.positional.ui.compose.positionalComposeView
import app.simple.positional.util.DistanceText
import app.simple.positional.util.LocationExtension.getLocationStatus
import app.simple.positional.util.LocationPrompt
import app.simple.positional.util.ParcelUtils.parcelable
import app.simple.positional.viewmodels.viewmodel.LocationViewModel
import app.simple.positional.viewmodels.viewmodel.MeasureViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import app.simple.positional.model.Measure as MeasureModel

class Measure : ScopedFragment(), MeasureActions, MapsCallbacks {
    private val state = MeasureState()
    private lateinit var locationViewModel: LocationViewModel
    private lateinit var measureViewModel: MeasureViewModel
    private lateinit var maps: MeasureMaps
    private var location: Location? = null
    private var camera: CameraPosition? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationViewModel = ViewModelProvider(requireActivity())[LocationViewModel::class.java]
        measureViewModel = ViewModelProvider(requireActivity())[MeasureViewModel::class.java]
        camera = savedInstanceState?.parcelable(CAMERA)
        state.fullScreen = !isLandscapeOrientation && savedInstanceState?.getBoolean(FULL_SCREEN) == true
        readPreferences()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        maps = MeasureMaps(requireContext()).apply {
            alpha = 0f
            onCreate(null)
            setOnMapsCallbackListener(this@Measure)
        }
        return positionalComposeView { top -> MeasureScreen(state, maps, top, this) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycle.addObserver(MapLifecycle(maps))
        if (state.fullScreen) bottomBar(visible = false)
        locationViewModel.getProvider().observe(viewLifecycleOwner) { state.enabled = getLocationStatus(requireContext()) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(FULL_SCREEN, state.fullScreen)
        if (::maps.isInitialized) outState.putParcelable(CAMERA, maps.getCamera() ?: camera)
    }

    override fun onMapInitialized() {
        camera?.let(maps::setCamera)
        locationViewModel.getLocation().observe(viewLifecycleOwner) {
            location = it
            state.fixed = true
            state.enabled = getLocationStatus(requireContext())
            val latLng = LatLng(it.latitude, it.longitude)
            maps.setFirstLocation(latLng)
            maps.location = it
            maps.addMarker(latLng)
        }
        measureViewModel.getMeasure().observe(viewLifecycleOwner) { measure ->
            if (MeasurePreferences.isMeasureSelected() && measure != null) {
                maps.createMeasurePolylines(measure)
                state.selected = true
                state.name = measure.name.orEmpty()
                showSummary(measure)
                state.current = ""
            } else {
                clearMeasure()
            }
        }
    }

    override fun onMapClicked() {
        if (isLandscapeOrientation) return
        state.fullScreen = !state.fullScreen
        bottomBar(!state.fullScreen)
    }

    override fun onLineAdded(measurePoint: MeasurePoint) {
        measureViewModel.addMeasurePoint(measurePoint) { measure -> view?.post { showSummary(measure) } }
    }

    override fun onLineDeleted(measurePoint: MeasurePoint?) {
        measureViewModel.removeMeasurePoint(measurePoint) { measure -> view?.post { showSummary(measure) } }
    }

    override fun onCameraDistance(latLng: LatLng?) {
        val last = maps.getMeasurePoints().lastOrNull()
        state.current = if (latLng == null || last == null) "" else {
            distance(GeoDistance.meters(last.latitude, last.longitude, latLng.latitude, latLng.longitude))
        }
    }

    private fun showSummary(measure: MeasureModel) {
        val points = measure.measurePoints.orEmpty()
        state.hasPoints = points.isNotEmpty()
        state.summary = listOf(
            InfoRow(getString(R.string.total), points.size.toString()),
            InfoRow(getString(R.string.gps_displacement), distance(GeoDistance.path(points.map { it.latitude to it.longitude }))),
        )
    }

    private fun distance(meters: Double) = DistanceText.format(requireContext(), meters, MainPreferences.isMetric())

    private fun clearMeasure() {
        maps.clear()
        state.selected = false
        state.hasPoints = false
        state.current = ""
    }

    private fun readPreferences() {
        state.toolsLeft = MeasurePreferences.isToolsGravityToLeft()
        state.compass = MeasurePreferences.isCompassRotation()
        state.wrapped = MeasurePreferences.arePolylinesWrapped()
    }

    private fun bottomBar(visible: Boolean) {
        (requireActivity() as? BottomBarHost)?.showBottomBar(visible)
    }

    override fun addMeasure() {
        showMeasureAdd().setOnMeasureAddCallbacks(object : MeasureAdd.Companion.MeasureAddCallbacks {
            override fun onSave(name: String, note: String) = measureViewModel.addMeasure(name, note)
        })
    }

    override fun measures() = startActivity(Intent(requireContext(), MeasuresActivity::class.java))

    override fun menu() {
        showMeasureMenu()
    }

    override fun locate(close: Boolean) {
        if (!getLocationStatus(requireContext())) {
            LocationPrompt.displayLocationSettingsRequest(requireActivity())
            return
        }
        val current = location ?: return
        if (close) {
            maps.resetCamera(CLOSE_ZOOM)
        } else {
            maps.moveMapCamera(
                LatLng(current.latitude, current.longitude),
                MeasurePreferences.getMapZoom(),
                MeasurePreferences.getMapTilt(),
                MeasurePreferences.getMapBearing(),
            )
        }
    }

    override fun addPoint() = maps.addPolyline()

    override fun removeLast() = maps.removeRecentPolyline()

    override fun wrap() = MeasurePreferences.invertPolylinesWrapped()

    override fun toggleCompass() = MeasurePreferences.invertCompassRotation()

    override fun toggleToolsSide() = MeasurePreferences.invertToolsGravity()

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            MeasurePreferences.LAST_SELECTED_MEASURE -> if (!MeasurePreferences.isMeasureSelected()) clearMeasure()
            MeasurePreferences.TOOLS_GRAVITY, MeasurePreferences.COMPASS_ROTATION, MeasurePreferences.POLYLINES_WRAPPED -> readPreferences()
        }
    }

    companion object {
        private const val CAMERA = "camera"
        private const val FULL_SCREEN = "fullscreen"
        private const val CLOSE_ZOOM = 18F

        fun newInstance() = Measure()
    }
}
