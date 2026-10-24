package app.simple.positional.ui.panels

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
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
import app.simple.positional.ui.compose.MeasureActions
import app.simple.positional.ui.compose.MeasureScreen
import app.simple.positional.ui.compose.MeasureState
import app.simple.positional.ui.compose.NameNoteDialog
import app.simple.positional.ui.compose.PanelDialog
import app.simple.positional.ui.compose.PanelMenus
import app.simple.positional.util.DistanceText
import app.simple.positional.util.LocationExtension.getLocationStatus
import app.simple.positional.util.LocationPrompt
import app.simple.positional.viewmodels.viewmodel.MapTools
import app.simple.positional.viewmodels.viewmodel.MeasureViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.filterNotNull
import app.simple.positional.model.Measure as MeasureModel

class Measure(activity: ComponentActivity) : PanelController(activity), MeasureActions, MapsCallbacks {
    private val state = MeasureState()
    private val measureViewModel = ViewModelProvider(activity)[MeasureViewModel::class.java]
    private val maps: MeasureMap
    private var location: Location? = null
    private var shown: Long? = null

    init {
        bind(measureViewModel.tools, ::applySettings)
        maps = MeasureMap.create(activity).apply { setCallbacks(this@Measure) }
    }

    @Composable
    override fun Content(topInset: Dp) {
        val summary by measureViewModel.summary.collectAsStateWithLifecycle()
        MeasureScreen(state, summary, maps.view, topInset, this)
    }

    init {
        owner.lifecycle.addObserver(maps.lifecycleObserver)
        if (state.fullScreen) bottomBar(visible = false)
        observe(locationModel.provider) { state.enabled = getLocationStatus(activity) }
        observe(locationModel.location.filterNotNull()) {
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
        state.hasPoints = measure.points.isNotEmpty()
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

    private fun distance(meters: Double) = DistanceText.format(activity, meters, MainPreferences.isMetric())

    private fun clearMeasure() {
        shown = null
        maps.clear()
        state.selected = false
        state.hasPoints = false
        state.current = ""
    }

    private fun applySettings(tools: MapTools) {
        state.toolsLeft = tools.toolsLeft
        state.compass = tools.compass
        state.wrapped = tools.wrapped
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

    private companion object {
        const val CLOSE_ZOOM = 18F
    }
}
