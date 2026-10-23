package app.simple.positional.ui.panels

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.getSystemService
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.simple.positional.R
import app.simple.positional.activities.subactivity.TrailsViewerActivity
import app.simple.positional.callbacks.BottomBarHost
import app.simple.positional.constants.TrailIcons
import app.simple.positional.decorations.trails.TrailMaps
import app.simple.positional.extensions.fragment.ScopedFragment
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
import app.simple.positional.ui.compose.positionalComposeView
import app.simple.positional.ui.compose.DialogHost
import app.simple.positional.ui.compose.MarkerIconRow
import app.simple.positional.ui.compose.MenuSheetState
import app.simple.positional.ui.compose.NameNoteDialog
import app.simple.positional.ui.compose.PanelMenus
import app.simple.positional.activities.subactivity.WebPageViewerActivity
import app.simple.positional.constants.HtmlPage
import app.simple.positional.util.DMSConverter
import app.simple.positional.util.DistanceText
import app.simple.positional.util.LocationExtension.getLocationStatus
import app.simple.positional.util.LocationPrompt
import app.simple.positional.util.MapIntents
import app.simple.positional.util.ParcelUtils.parcelable
import app.simple.positional.util.TimeFormatter.formatDate
import app.simple.positional.util.TrailNames
import app.simple.positional.viewmodels.viewmodel.LocationViewModel
import app.simple.positional.viewmodels.viewmodel.TrailDataViewModel
import app.simple.positional.viewmodels.viewmodel.TrailsViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch

class Trail : ScopedFragment(), TrailActions, MapsCallbacks {
    private val state = TrailState()
    private val menuSheet = MenuSheetState()
    private val dialogs = DialogHost()
    private val trailData: TrailDataViewModel by viewModels()
    private val trails: TrailsViewModel by viewModels()
    private lateinit var locationViewModel: LocationViewModel
    private lateinit var maps: TrailMaps
    private var location: Location? = null
    private var camera: CameraPosition? = null
    private var pending: Triple<Double, Double, Float>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationViewModel = ViewModelProvider(requireActivity())[LocationViewModel::class.java]
        camera = savedInstanceState?.parcelable(CAMERA)
        state.fullScreen = !isLandscapeOrientation && savedInstanceState?.getBoolean(FULL_SCREEN) == true
        readPreferences()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        maps = TrailMaps(requireContext()).apply {
            alpha = 0f
            onCreate(null)
            setOnMapsCallbackListener(this@Trail)
        }
        return positionalComposeView(menuSheet, dialogs) { top -> TrailScreen(state, maps, top, isLandscapeOrientation, this) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycle.addObserver(MapLifecycle(maps))
        if (state.fullScreen) bottomBar(visible = false)

        locationViewModel.getLocation().observe(viewLifecycleOwner) {
            location = it
            MainPreferences.setLastLatitude(it.latitude.toFloat())
            MainPreferences.setLastLongitude(it.longitude.toFloat())
            state.fixed = true
            state.enabled = getLocationStatus(requireContext())
            maps.setFirstLocation(it)
            maps.location = it
            maps.addMarker(LatLng(it.latitude, it.longitude))
        }
        locationViewModel.getProvider().observe(viewLifecycleOwner) { state.enabled = getLocationStatus(requireContext()) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { trailData.trail.collect { state.trail = it } }
                trailData.points.collect(::showPoints)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(FULL_SCREEN, state.fullScreen)
        if (::maps.isInitialized) outState.putParcelable(CAMERA, maps.getCamera() ?: camera)
    }

    private fun showPoints(points: List<TrailPoint>) {
        maps.addPolylines(ArrayList(points))
        val metric = MainPreferences.isMetric()
        val distance = GeoDistance.path(points.map { it.latitude to it.longitude })
        state.summary = listOf(
            InfoRow(getString(R.string.total), points.size.toString()),
            InfoRow(getString(R.string.gps_displacement), DistanceText.format(requireContext(), distance, metric)),
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
                DMSConverter.getFormattedLatitude(point.latitude, requireContext()),
                DMSConverter.getFormattedLongitude(point.longitude, requireContext()),
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
        (requireActivity() as? BottomBarHost)?.showBottomBar(visible)
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
        camera?.let(maps::setCamera)
        maps.addPolylines(ArrayList(trailData.points.value))
    }

    override fun onMapClicked() {
        if (isLandscapeOrientation) return
        state.fullScreen = !state.fullScreen
        bottomBar(!state.fullScreen)
    }

    override fun onMapLongClicked(latLng: LatLng) = requestMarker(latLng.latitude, latLng.longitude, 0f)

    override fun onLineDeleted(trailPoint: TrailPoint?) = trailData.delete(trailPoint)

    override fun trails() = startActivity(Intent(requireContext(), TrailsViewerActivity::class.java))

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

    override fun menu() = menuSheet.show { PanelMenus.trail { WebPageViewerActivity.open(requireContext(), HtmlPage.TRAIL_HELP) } }

    override fun locate(close: Boolean) {
        if (!getLocationStatus(requireContext())) {
            LocationPrompt.displayLocationSettingsRequest(requireActivity())
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
            Toast.makeText(requireContext(), R.string.location_not_available, Toast.LENGTH_SHORT).show()
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
        requireContext().getSystemService<ClipboardManager>()?.setPrimaryClip(ClipData.newPlainText(item.name, text))
    }

    override fun navigate(item: TrailPointItem) = MapIntents.navigate(requireContext(), item.point.latitude, item.point.longitude)

    override fun share(item: TrailPointItem) = MapIntents.open(requireContext(), item.point.latitude, item.point.longitude)

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
        private const val CAMERA = "camera"
        private const val FULL_SCREEN = "fullscreen"
        private const val CLOSE_ZOOM = 18F
        private const val FOCUS_ZOOM = 15F

        fun newInstance() = Trail()
    }
}
