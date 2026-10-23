package app.simple.positional.ui.panels

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import androidx.lifecycle.ViewModelProvider
import app.simple.positional.R
import app.simple.positional.activities.subactivity.MeasuresActivity
import app.simple.positional.callbacks.BottomBarHost
import app.simple.positional.decorations.measure.MeasureMaps
import app.simple.positional.extensions.panel.PanelController
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
import app.simple.positional.ui.compose.NameNoteDialog
import app.simple.positional.ui.compose.PanelMenus
import app.simple.positional.util.DistanceText
import app.simple.positional.util.LocationExtension.getLocationStatus
import app.simple.positional.util.LocationPrompt
import app.simple.positional.viewmodels.viewmodel.LocationViewModel
import app.simple.positional.viewmodels.viewmodel.MeasureViewModel
import com.google.android.gms.maps.model.LatLng
import app.simple.positional.model.Measure as MeasureModel

class Measure(activity: ComponentActivity) : PanelController(activity), MeasureActions, MapsCallbacks {
    private val state = MeasureState()
    private lateinit var locationViewModel: LocationViewModel
    private lateinit var measureViewModel: MeasureViewModel
    private lateinit var maps: MeasureMaps
    private var location: Location? = null

    init {
        locationViewModel = ViewModelProvider(activity)[LocationViewModel::class.java]
        measureViewModel = ViewModelProvider(activity)[MeasureViewModel::class.java]
        readPreferences()
    }

    init {
        maps = MeasureMaps(activity).apply {
            alpha = 0f
            onCreate(null)
            setOnMapsCallbackListener(this@Measure)
        }
    }

    @Composable
    override fun Content(topInset: Dp) {
        MeasureScreen(state, maps, topInset, this)
    }

    init {
        owner.lifecycle.addObserver(MapLifecycle(maps))
        if (state.fullScreen) bottomBar(visible = false)
        locationViewModel.getProvider().observe(owner) { state.enabled = getLocationStatus(activity) }
    }


    override fun onMapInitialized() {
        locationViewModel.getLocation().observe(owner) {
            location = it
            state.fixed = true
            state.enabled = getLocationStatus(activity)
            val latLng = LatLng(it.latitude, it.longitude)
            maps.setFirstLocation(latLng)
            maps.location = it
            maps.addMarker(latLng)
        }
        measureViewModel.getMeasure().observe(owner) { measure ->
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
        measureViewModel.addMeasurePoint(measurePoint) { measure -> activity.runOnUiThread { showSummary(measure) } }
    }

    override fun onLineDeleted(measurePoint: MeasurePoint?) {
        measureViewModel.removeMeasurePoint(measurePoint) { measure -> activity.runOnUiThread { showSummary(measure) } }
    }

    override fun onCameraDistance(latLng: LatLng?) {
        val last = maps.getMeasurePoints().lastOrNull()
        state.current = if (latLng == null || last == null) "" else {
            distance(GeoDistance.meters(last.latitude, last.longitude, latLng.latitude, latLng.longitude))
        }
    }

    private fun showSummary(measure: MeasureModel) {
        val points = measure.measurePoints
        state.hasPoints = points.isNotEmpty()
        state.summary = listOf(
            InfoRow(getString(R.string.total), points.size.toString()),
            InfoRow(getString(R.string.gps_displacement), distance(GeoDistance.path(points.map { it.latitude to it.longitude }))),
        )
    }

    private fun distance(meters: Double) = DistanceText.format(activity, meters, MainPreferences.isMetric())

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
        (activity as? BottomBarHost)?.showBottomBar(visible)
    }

    override fun addMeasure() {
        val title = getString(R.string.create_new)
        dialogs.show {
            NameNoteDialog(title, isValid = { it.isNotEmpty() }, onDismiss = dialogs::dismiss) { name, note ->
                measureViewModel.addMeasure(name, note)
                dialogs.dismiss()
            }
        }
    }

    override fun measures() = startActivity(Intent(activity, MeasuresActivity::class.java))

    override fun menu() = menuSheet.show { PanelMenus.measure() }

    override fun locate(close: Boolean) {
        if (!getLocationStatus(activity)) {
            LocationPrompt.displayLocationSettingsRequest(activity)
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
        private const val CLOSE_ZOOM = 18F
    }
}
