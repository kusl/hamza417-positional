package app.simple.positional.ui.panels

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import android.content.SharedPreferences
import android.location.Location
import androidx.lifecycle.ViewModelProvider
import app.simple.positional.R
import app.simple.positional.ui.navigation.Routes
import app.simple.positional.callbacks.BottomBarHost
import app.simple.positional.decorations.maps.MeasureMap
import app.simple.positional.extensions.panel.PanelController
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
import app.simple.positional.ui.compose.PanelDialog
import app.simple.positional.ui.compose.PanelMenus
import app.simple.positional.util.DistanceText
import app.simple.positional.util.LocationExtension.getLocationStatus
import app.simple.positional.util.LocationPrompt
import app.simple.positional.viewmodels.viewmodel.LocationViewModel
import app.simple.positional.viewmodels.viewmodel.MeasureViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.filterNotNull
import app.simple.positional.model.Measure as MeasureModel

class Measure(activity: ComponentActivity) : PanelController(activity), MeasureActions, MapsCallbacks {
    private val state = MeasureState()
    private val locationViewModel: LocationViewModel
    private val measureViewModel: MeasureViewModel
    private val maps: MeasureMap
    private var location: Location? = null
    private var shown: Long? = null

    init {
        locationViewModel = ViewModelProvider(activity)[LocationViewModel::class.java]
        measureViewModel = ViewModelProvider(activity)[MeasureViewModel::class.java]
        readPreferences()
    }

    init {
        maps = MeasureMap.create(activity).apply { setCallbacks(this@Measure) }
    }

    @Composable
    override fun Content(topInset: Dp) {
        MeasureScreen(state, maps.view, topInset, this)
    }

    init {
        owner.lifecycle.addObserver(maps.lifecycleObserver)
        if (state.fullScreen) bottomBar(visible = false)
        observe(locationViewModel.provider) { state.enabled = getLocationStatus(activity) }
        observe(locationViewModel.location.filterNotNull()) {
            location = it
            state.fixed = true
            state.enabled = getLocationStatus(activity)
            maps.updateLocation(it)
        }
        observe(measureViewModel.measure, ::showMeasure)
    }

    private fun showMeasure(measure: MeasureModel?) {
        if (measure == null) {
            clearMeasure()
            return
        }
        if (measure.dateCreated != shown || maps.points != measure.points) {
            maps.showMeasure(measure)
            state.current = ""
        }
        shown = measure.dateCreated
        state.selected = true
        state.name = measure.name.orEmpty()
        showSummary(measure)
    }

    override fun onMapClicked() {
        if (isLandscapeOrientation) return
        state.fullScreen = !state.fullScreen
        bottomBar(!state.fullScreen)
    }

    override fun onLineAdded(measurePoint: MeasurePoint) {
        measureViewModel.addMeasurePoint(measurePoint)
    }

    override fun onLineDeleted(measurePoint: MeasurePoint?) {
        measureViewModel.removeMeasurePoint(measurePoint)
    }

    override fun onCameraDistance(latLng: LatLng?) {
        val last = maps.points.lastOrNull()
        state.current = if (latLng == null || last == null) "" else {
            distance(GeoDistance.meters(last.latitude, last.longitude, latLng.latitude, latLng.longitude))
        }
    }

    private fun showSummary(measure: MeasureModel) {
        val points = measure.points
        state.hasPoints = points.isNotEmpty()
        state.summary = listOf(
            InfoRow(getString(R.string.total), points.size.toString()),
            InfoRow(getString(R.string.gps_displacement), distance(GeoDistance.path(points.map { it.latitude to it.longitude }))),
        )
    }

    private fun distance(meters: Double) = DistanceText.format(activity, meters, MainPreferences.isMetric())

    private fun clearMeasure() {
        shown = null
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

    override fun addMeasure() = dialogs.show(NewMeasure)

    @Composable
    override fun DialogContent(dialog: PanelDialog) {
        if (dialog == NewMeasure) {
            NameNoteDialog(stringResource(R.string.create_new), isValid = { it.isNotEmpty() }, onDismiss = dialogs::dismiss) { name, note ->
                measureViewModel.addMeasure(name, note)
                dialogs.dismiss()
            }
        } else {
            super.DialogContent(dialog)
        }
    }

    private data object NewMeasure : PanelDialog

    override fun measures() = navigate(Routes.MEASURES)

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
            maps.moveTo(current.latitude, current.longitude, MeasurePreferences.getMapZoom())
        }
    }

    override fun addPoint() = maps.addPoint()

    override fun removeLast() = maps.removeLast()

    override fun wrap() = MeasurePreferences.invertPolylinesWrapped()

    override fun toggleCompass() = MeasurePreferences.invertCompassRotation()

    override fun toggleToolsSide() = MeasurePreferences.invertToolsGravity()

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            MeasurePreferences.TOOLS_GRAVITY, MeasurePreferences.COMPASS_ROTATION, MeasurePreferences.POLYLINES_WRAPPED -> readPreferences()
        }
    }

    companion object {
        private const val CLOSE_ZOOM = 18F
    }
}
