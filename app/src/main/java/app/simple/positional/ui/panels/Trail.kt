package app.simple.positional.ui.panels

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.getSystemService
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.simple.positional.R
import app.simple.positional.activities.subactivity.TrailsViewerActivity
import app.simple.positional.callbacks.BottomBarHost
import app.simple.positional.constants.TrailIcons
import app.simple.positional.decorations.trails.TrailMaps
import app.simple.positional.extensions.panel.PanelController
import app.simple.positional.extensions.maps.MapLifecycle
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
import app.simple.positional.ui.compose.PanelMenus
import app.simple.positional.activities.subactivity.WebPageViewerActivity
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
import kotlinx.coroutines.launch

class Trail(activity: ComponentActivity) : PanelController(activity), TrailActions, MapsCallbacks {
    private val state = TrailState()
    private val trailData: TrailDataViewModel by activity.viewModels()
    private val trails: TrailsViewModel by activity.viewModels()
    private lateinit var locationViewModel: LocationViewModel
    private lateinit var maps: TrailMaps
    private var location: Location? = null
    private var pending: Triple<Double, Double, Float>? = null

    init {
        locationViewModel = ViewModelProvider(activity)[LocationViewModel::class.java]
        readPreferences()
    }

    init {
        maps = TrailMaps(activity).apply {
            alpha = 0f
            onCreate(null)
            setOnMapsCallbackListener(this@Trail)
        }
    }

    @Composable
    override fun Content(topInset: Dp) {
        TrailScreen(state, maps, topInset, isLandscapeOrientation, this)
    }

    init {
        owner.lifecycle.addObserver(MapLifecycle(maps))
        if (state.fullScreen) bottomBar(visible = false)

        locationViewModel.getLocation().observe(owner) {
            location = it
            MainPreferences.setLastLatitude(it.latitude.toFloat())
            MainPreferences.setLastLongitude(it.longitude.toFloat())
            state.fixed = true
            state.enabled = getLocationStatus(activity)
            maps.setFirstLocation(it)
            maps.location = it
            maps.addMarker(LatLng(it.latitude, it.longitude))
        }
        locationViewModel.getProvider().observe(owner) { state.enabled = getLocationStatus(activity) }

        owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { trailData.trail.collect { state.trail = it } }
                trailData.points.collect(::showPoints)
            }
        }
    }


    private fun showPoints(points: List<TrailPoint>) {
        maps.addPolylines(ArrayList(points))
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
            name = point.name.ifEmpty { "--" },
            note = point.note.ifEmpty { getString(R.string.not_available) },
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

    override fun onMapInitialized() {
        maps.addPolylines(ArrayList(trailData.points.value))
    }

    override fun onMapClicked() {
        if (isLandscapeOrientation) return
        state.fullScreen = !state.fullScreen
        bottomBar(!state.fullScreen)
    }

    override fun onMapLongClicked(latLng: LatLng) = requestMarker(latLng.latitude, latLng.longitude, 0f)

    override fun onLineDeleted(trailPoint: TrailPoint?) = trailData.delete(trailPoint)

    override fun trails() = startActivity(Intent(activity, TrailsViewerActivity::class.java))

    override fun addTrail() {
        val title = getString(R.string.enter_trail_name)
        dialogs.show {
            val existing by trails.trails.collectAsState()
            NameNoteDialog(
                title,
                isValid = { name -> TrailNames.isValid(name, existing.orEmpty().map { it.trailName }) },
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
    }

    override fun menu() = menuSheet.show { PanelMenus.trail { WebPageViewerActivity.open(activity, HtmlPage.TRAIL_HELP) } }

    override fun locate(close: Boolean) {
        if (!getLocationStatus(activity)) {
            LocationPrompt.displayLocationSettingsRequest(activity)
            return
        }
        val current = location ?: return
        if (close) {
            maps.resetCamera(CLOSE_ZOOM)
        } else {
            maps.moveMapCamera(LatLng(current.latitude, current.longitude), TrailPreferences.getMapZoom(), TrailPreferences.getMapTilt())
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

    override fun removeLast() = maps.removePolyline()

    override fun wrap() = maps.wrapUnwrap()

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
            showMarkerDialog(icon, latitude, longitude, accuracy)
        }
    }

    private fun showMarkerDialog(icon: Int, latitude: Double, longitude: Double, accuracy: Float) {
        val title = getString(R.string.enter_marker_name)
        val time = System.currentTimeMillis()
        dialogs.show {
            var selected by rememberSaveable { mutableIntStateOf(icon) }
            NameNoteDialog(
                title,
                isValid = { true },
                onDismiss = dialogs::dismiss,
                initial = TrailPreferences.getLastMarkerName() to TrailPreferences.getLastMarkerNote(),
                onDraft = { name, note ->
                    TrailPreferences.setLastMarkerName(name)
                    TrailPreferences.setLastMarkerNote(note)
                },
                extra = { MarkerIconRow(selected) { selected = it } },
            ) { name, note ->
                save(TrailPoint(latitude, longitude, time, selected, note.ifEmpty { null }, name.ifEmpty { null }, accuracy))
                TrailPreferences.setLastMarkerName("")
                TrailPreferences.setLastMarkerNote("")
                dialogs.dismiss()
            }
        }
    }

    private fun save(point: TrailPoint) {
        trailData.save(point)
        if (!TrailPreferences.arePolylinesWrapped()) {
            maps.moveMapCamera(LatLng(point.latitude, point.longitude), TrailPreferences.getMapZoom(), TrailPreferences.getMapTilt())
        }
    }

    override fun focus(item: TrailPointItem) {
        state.sheet = false
        maps.moveMapCamera(LatLng(item.point.latitude, item.point.longitude), FOCUS_ZOOM, TrailPreferences.getMapTilt())
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
