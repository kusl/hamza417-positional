package app.simple.positional.ui.panels

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import android.content.ClipData
import android.content.ClipboardManager
import android.content.SharedPreferences
import android.location.Location
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.getSystemService
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.simple.positional.R
import app.simple.positional.ui.navigation.Routes
import app.simple.positional.callbacks.BottomBarHost
import app.simple.positional.constants.TrailIcons
import app.simple.positional.decorations.maps.TrailMap
import app.simple.positional.extensions.panel.PanelController
import app.simple.positional.extensions.maps.MapsCallbacks
import app.simple.positional.math.GeoDistance
import app.simple.positional.math.MathExtensions.round
import app.simple.positional.model.TrailEntry
import app.simple.positional.model.TrailPoint
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.preferences.TrailPreferences
import app.simple.positional.ui.compose.InfoRow
import app.simple.positional.ui.compose.TrailActions
import app.simple.positional.ui.compose.TrailPointItem
import app.simple.positional.ui.compose.TrailScreen
import app.simple.positional.ui.compose.TrailState
import app.simple.positional.ui.compose.MarkerIconRow
import app.simple.positional.ui.compose.NameNoteDialog
import app.simple.positional.ui.compose.PanelDialog
import app.simple.positional.ui.compose.PanelMenus
import app.simple.positional.constants.HtmlPage
import app.simple.positional.util.DMSConverter
import app.simple.positional.util.DistanceText
import app.simple.positional.util.LocationExtension.getLocationStatus
import app.simple.positional.util.LocationPrompt
import app.simple.positional.util.MapIntents
import app.simple.positional.util.TimeFormatter.formatDate
import app.simple.positional.util.TrailNames
import app.simple.positional.viewmodels.viewmodel.LocationViewModel
import app.simple.positional.viewmodels.viewmodel.TrailDataViewModel
import app.simple.positional.viewmodels.viewmodel.TrailsViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.filterNotNull

class Trail(activity: ComponentActivity) : PanelController(activity), TrailActions, MapsCallbacks {
    private val state = TrailState()
    private val trailData: TrailDataViewModel by activity.viewModels()
    private val trails: TrailsViewModel by activity.viewModels()
    private val locationViewModel: LocationViewModel
    private val maps: TrailMap
    private var location: Location? = null
    private var pending: Triple<Double, Double, Float>? = null

    init {
        locationViewModel = ViewModelProvider(activity)[LocationViewModel::class.java]
        readPreferences()
    }

    init {
        maps = TrailMap.create(activity).apply { setCallbacks(this@Trail) }
    }

    @Composable
    override fun Content(topInset: Dp) {
        TrailScreen(state, maps.view, topInset, isLandscapeOrientation, this)
    }

    init {
        owner.lifecycle.addObserver(maps.lifecycleObserver)
        if (state.fullScreen) bottomBar(visible = false)

        observe(locationViewModel.location.filterNotNull()) {
            location = it
            state.fixed = true
            state.enabled = getLocationStatus(activity)
            maps.updateLocation(it)
        }
        observe(locationViewModel.provider) { state.enabled = getLocationStatus(activity) }
        observe(trailData.trail) { state.trail = it }
        observe(trailData.points, ::showPoints)
    }

    private fun showPoints(points: List<TrailPoint>) {
        maps.showPoints(points)
        val metric = MainPreferences.isMetric()
        val distance = GeoDistance.path(points.map { it.latitude to it.longitude })
        state.summary = listOf(
            InfoRow(getString(R.string.total), points.size.toString()),
            InfoRow(getString(R.string.gps_displacement), DistanceText.format(activity, distance, metric)),
        )
        state.points = points.asReversed().map { item(it, metric) }
    }

    private fun item(point: TrailPoint, metric: Boolean): TrailPointItem {
        val (accuracy, unit) = DistanceText.parts(point.accuracy.toDouble(), metric)
        return TrailPointItem(
            point = point,
            icon = TrailIcons.icons.getOrElse(point.iconPosition) { TrailIcons.icons[0] },
            name = point.name.orEmpty().ifEmpty { "--" },
            note = point.note.orEmpty().ifEmpty { getString(R.string.not_available) },
            coordinates = getString(
                R.string.coordinates_format,
                DMSConverter.getFormattedLatitude(point.latitude, activity),
                DMSConverter.getFormattedLongitude(point.longitude, activity),
            ),
            accuracy = getString(R.string.trail_data_accuracy, round(accuracy, 1).toString(), getString(unit)),
            date = point.timeAdded.formatDate(),
        )
    }

    private fun readPreferences() {
        state.toolsLeft = TrailPreferences.isToolsGravityToLeft()
        state.compass = TrailPreferences.isCompassRotation()
        state.wrapped = TrailPreferences.arePolylinesWrapped()
    }

    private fun bottomBar(visible: Boolean) {
        (activity as? BottomBarHost)?.showBottomBar(visible)
    }

    private fun requestMarker(latitude: Double, longitude: Double, accuracy: Float) {
        if (TrailPreferences.getCurrentTrail().isEmpty()) {
            addTrail()
            return
        }
        pending = Triple(latitude, longitude, accuracy)
        state.pickingMarker = true
    }

    override fun onMapClicked() {
        if (isLandscapeOrientation) return
        state.fullScreen = !state.fullScreen
        bottomBar(!state.fullScreen)
    }

    override fun onMapLongClicked(latLng: LatLng) = requestMarker(latLng.latitude, latLng.longitude, 0f)

    override fun onLineDeleted(trailPoint: TrailPoint?) = trailData.delete(trailPoint)

    override fun trails() = navigate(Routes.TRAILS)

    override fun addTrail() = dialogs.show(NewTrail)

    override fun menu() = menuSheet.show { PanelMenus.trail { openPage(HtmlPage.TRAIL_HELP) } }

    override fun locate(close: Boolean) {
        if (!getLocationStatus(activity)) {
            LocationPrompt.displayLocationSettingsRequest(activity)
            return
        }
        val current = location ?: return
        if (close) {
            maps.resetCamera(CLOSE_ZOOM)
        } else {
            maps.moveTo(current.latitude, current.longitude, TrailPreferences.getMapZoom())
        }
    }

    override fun addPoint() {
        val current = location
        if (current == null && TrailPreferences.getCurrentTrail().isNotEmpty()) {
            Toast.makeText(activity, R.string.location_not_available, Toast.LENGTH_SHORT).show()
            return
        }
        requestMarker(current?.latitude ?: 0.0, current?.longitude ?: 0.0, current?.accuracy ?: 0f)
    }

    override fun removeLast() = maps.removeLast()

    override fun wrap() = maps.toggleWrap()

    override fun toggleCompass() = TrailPreferences.setCompassRotation(!TrailPreferences.isCompassRotation())

    override fun toggleToolsSide() = TrailPreferences.setToolsGravityToLeft(!TrailPreferences.isToolsGravityToLeft())

    override fun sheet(open: Boolean) {
        state.sheet = open
    }

    override fun markerPicked(icon: Int, quick: Boolean) {
        val (latitude, longitude, accuracy) = pending ?: return
        pending = null
        if (quick) {
            save(TrailPoint(latitude, longitude, System.currentTimeMillis(), icon, null, null, accuracy))
        } else {
            dialogs.show(NewMarker(icon, latitude, longitude, accuracy, System.currentTimeMillis()))
        }
    }

    @Composable
    override fun DialogContent(dialog: PanelDialog) {
        when (dialog) {
            NewTrail -> NewTrailDialog()
            is NewMarker -> NewMarkerDialog(dialog)
            else -> super.DialogContent(dialog)
        }
    }

    @Composable
    private fun NewTrailDialog() {
        val existing by trails.trails.collectAsStateWithLifecycle()
        NameNoteDialog(
            stringResource(R.string.enter_trail_name),
            isValid = { name -> TrailNames.isValid(name, existing.orEmpty().mapNotNull { it.trailName }) },
            onDismiss = dialogs::dismiss,
            initial = TrailPreferences.getLastTrailName() to TrailPreferences.getLastTrailNote(),
            onDraft = { name, note ->
                TrailPreferences.setLastTrailName(name)
                TrailPreferences.setLastTrailNote(note)
            },
        ) { name, note ->
            trails.addTrail(TrailEntry(System.currentTimeMillis(), name, note))
            TrailPreferences.setLastTrailName("")
            TrailPreferences.setLastTrailNote("")
            dialogs.dismiss()
        }
    }

    @Composable
    private fun NewMarkerDialog(marker: NewMarker) {
        var selected by rememberSaveable(marker) { mutableIntStateOf(marker.icon) }
        NameNoteDialog(
            stringResource(R.string.enter_marker_name),
            isValid = { true },
            onDismiss = dialogs::dismiss,
            initial = TrailPreferences.getLastMarkerName() to TrailPreferences.getLastMarkerNote(),
            onDraft = { name, note ->
                TrailPreferences.setLastMarkerName(name)
                TrailPreferences.setLastMarkerNote(note)
            },
            extra = { MarkerIconRow(selected) { selected = it } },
        ) { name, note ->
            save(TrailPoint(marker.latitude, marker.longitude, marker.time, selected, note.ifEmpty { null }, name.ifEmpty { null }, marker.accuracy))
            TrailPreferences.setLastMarkerName("")
            TrailPreferences.setLastMarkerNote("")
            dialogs.dismiss()
        }
    }

    private data object NewTrail : PanelDialog

    private data class NewMarker(val icon: Int, val latitude: Double, val longitude: Double, val accuracy: Float, val time: Long) : PanelDialog

    private fun save(point: TrailPoint) {
        trailData.save(point)
        if (!TrailPreferences.arePolylinesWrapped()) {
            maps.moveTo(point.latitude, point.longitude, TrailPreferences.getMapZoom())
        }
    }

    override fun focus(item: TrailPointItem) {
        state.sheet = false
        maps.moveTo(item.point.latitude, item.point.longitude, FOCUS_ZOOM)
    }

    override fun delete(item: TrailPointItem) = trailData.delete(item.point)

    override fun copy(item: TrailPointItem) {
        val text = listOf(item.name, item.note, "geo:${item.point.latitude},${item.point.longitude}", item.date).joinToString("\n\n")
        activity.getSystemService<ClipboardManager>()?.setPrimaryClip(ClipData.newPlainText(item.name, text))
    }

    override fun navigate(item: TrailPointItem) = MapIntents.navigate(activity, item.point.latitude, item.point.longitude)

    override fun share(item: TrailPointItem) = MapIntents.open(activity, item.point.latitude, item.point.longitude)

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            TrailPreferences.selectedTrail -> {
                maps.clear()
                trailData.select(TrailPreferences.getCurrentTrail())
            }
            TrailPreferences.toolsMenuGravity, TrailPreferences.compass, TrailPreferences.wrapped -> readPreferences()
        }
    }

    companion object {
        private const val CLOSE_ZOOM = 18F
        private const val FOCUS_ZOOM = 15F
    }
}
