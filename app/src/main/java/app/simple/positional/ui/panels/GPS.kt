package app.simple.positional.ui.panels

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import android.content.ClipData
import android.content.ClipboardManager
import android.location.Location
import android.view.MotionEvent
import androidx.compose.runtime.getValue
import androidx.core.content.getSystemService
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.simple.positional.R
import app.simple.positional.callbacks.BottomBarHost
import app.simple.positional.database.instances.LocationDatabase
import app.simple.positional.decorations.maps.GpsMap
import app.simple.positional.extensions.panel.PanelController
import app.simple.positional.extensions.maps.MapsCallbacks
import app.simple.positional.model.Locations
import app.simple.positional.preferences.GPSPreferences
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.ui.compose.GpsActions
import app.simple.positional.ui.compose.GpsScreen
import app.simple.positional.ui.compose.GpsState
import app.simple.positional.ui.compose.InfoRow
import app.simple.positional.ui.compose.MapRotation
import app.simple.positional.viewmodels.viewmodel.GpsSettings
import app.simple.positional.ui.compose.PinCustomizationDialog
import app.simple.positional.ui.compose.CoordinateInput
import app.simple.positional.ui.compose.CustomLocationForm
import app.simple.positional.ui.compose.TargetCoordinatesDialog
import app.simple.positional.ui.compose.CoordinateDetailsDialog
import app.simple.positional.ui.compose.LocationDetailsDialog
import app.simple.positional.ui.compose.LocationSummaryDialog
import app.simple.positional.util.LocationShare
import app.simple.positional.ui.compose.PanelDialog
import app.simple.positional.ui.compose.PanelMenus
import app.simple.positional.ui.navigation.SearchResult
import app.simple.positional.ui.compose.SettingsLabels
import app.simple.positional.constants.HtmlPage
import app.simple.positional.util.LocationExtension.getLocationStatus
import app.simple.positional.util.LocationPrompt
import app.simple.positional.util.MapIntents
import app.simple.positional.util.MapSource
import app.simple.positional.viewmodels.viewmodel.GpsViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GPS(activity: ComponentActivity) : PanelController(activity), GpsActions, MapsCallbacks {
    private val state = GpsState()
    private val targetForm = CustomLocationForm()
    private val maps: GpsMap
    private val viewModel = panelModel(GpsViewModel::class, ::GpsViewModel)
    private var location: Location? = null
    private var pressed: LatLng? = null

    init {
        state.fullScreen = !isLandscapeOrientation && consumeFullScreenShortcut()
        bind(viewModel.settings, ::applySettings)
    }

    init {
        maps = GpsMap.create(activity).apply {
            setCallbacks(this@GPS)
            onTouch = { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> unregister()
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> registerWithRunnable()
                }
            }
        }
    }

    @Composable
    override fun Content(topInset: Dp) {
        val info by viewModel.info.collectAsStateWithLifecycle()
        GpsScreen(state, info, maps.view, topInset, isLandscapeOrientation, this)
    }

    init {
        owner.lifecycle.addObserver(maps.lifecycleObserver)
        if (state.fullScreen) bottomBar(visible = false)

        val needsPlayServices = !MapSource.isOsm() || MainPreferences.getLocationProvider() == SettingsLabels.PROVIDER_FUSED
        if (needsPlayServices && !LocationPrompt.checkGooglePlayServices(activity)) {
            showError(getString(R.string.play_services_error))
        }

        observe(locationModel.location.filterNotNull()) {
            location = it
            if (!MainPreferences.isCustomCoordinate()) maps.updateLocation(it)
        }
    }

    private fun applySettings(settings: GpsSettings) {
        state.pin = settings.pin
        state.targetMode = settings.targetMode
        state.targetSet = settings.targetSet
        state.toolsLeft = settings.toolsLeft
        state.volumeKeys = settings.volumeKeys
        state.rotation = settings.rotation
    }

    private fun consumeFullScreenShortcut(): Boolean {
        val intent = activity.intent ?: return false
        if (intent.action != FULL_SCREEN_ACTION) return false
        intent.action = null
        return true
    }

    private fun bottomBar(visible: Boolean) {
        (activity as? BottomBarHost)?.showBottomBar(visible)
    }

    override fun onMapClicked() {
        if (isLandscapeOrientation) return
        state.fullScreen = !state.fullScreen
        bottomBar(!state.fullScreen)
    }

    override fun onMapLongClicked(latLng: LatLng) {
        val point = maps.screenPoint(latLng) ?: return
        pressed = latLng
        state.pressed = IntOffset(point.x, point.y)
    }

    override fun menu() = menuSheet.show {
        PanelMenus.gps(onPin = ::pinCustomization) {
            openPage(HtmlPage.MEDIA_KEYS)
        }
    }

    override fun customLocation() {
        val (latitude, longitude) = coordinates()
        dialogs.show(GpsDialog.Summary(latitude, longitude))
    }

    private fun coordinates(): Pair<Double, Double> {
        val custom = MainPreferences.isCustomCoordinate()
        val current = location
        if (!custom && current != null) return current.latitude to current.longitude
        val stored = if (custom) MainPreferences.getCoordinates() else MainPreferences.getLastCoordinates()
        return stored[0].toDouble() to stored[1].toDouble()
    }

    override fun pinCustomization() = dialogs.show(GpsDialog.Pin)

    override fun locate(close: Boolean) {
        if (getLocationStatus(activity)) {
            maps.isMapMovementEnabled = true
            maps.resetCamera(if (close) CLOSE_ZOOM else GPSPreferences.getMapZoom())
        } else {
            LocationPrompt.displayLocationSettingsRequest(activity)
        }
    }

    override fun toggleTargetMode() = GPSPreferences.setTargetMarkerMode(!GPSPreferences.isTargetMarkerMode())

    override fun confirmTarget() {
        GPSPreferences.setTargetMarkerMode(false)
        maps.setTargetMarker(null)
    }

    override fun targetCoordinates() {
        val target = GPSPreferences.getTargetMarkerCoordinates()
        targetForm.fill("", CoordinateInput.format(target[0].toDouble()), CoordinateInput.format(target[1].toDouble()))
        dialogs.show(GpsDialog.Target)
    }

    override fun onSearchResult(result: SearchResult) {
        targetForm.fill(result.address, CoordinateInput.format(result.latitude), CoordinateInput.format(result.longitude))
        dialogs.show(GpsDialog.Target)
    }

    override fun removeTarget() = GPSPreferences.setTargetMarker(false)

    override fun toggleToolsSide() = GPSPreferences.setToolsGravity(!GPSPreferences.isToolsGravityLeft())

    override fun rotate(rotation: MapRotation) {
        GPSPreferences.setCompassRotation(rotation == MapRotation.Compass)
        GPSPreferences.setBearingRotation(rotation == MapRotation.Bearing)
        GPSPreferences.setNorthOnly(rotation == MapRotation.North)
    }

    override fun sheet(open: Boolean) {
        state.sheet = open
        if (open) maps.unregister() else maps.registerWithRunnable()
    }

    override fun save() {
        val current = location
        if (current == null) {
            state.message = R.string.location_not_saved
            return
        }
        val entry = Locations().apply {
            latitude = current.latitude
            longitude = current.longitude
            address = viewModel.info.value.address
            date = System.currentTimeMillis()
        }
        val dao = LocationDatabase.getInstance(activity).locationDao()
        owner.lifecycleScope.launch {
            withContext(NonCancellable) { dao.insertLocation(entry) }
            state.message = R.string.location_saved
        }
    }

    override fun copy() {
        val clipboard = activity.getSystemService<ClipboardManager>() ?: return
        val info = viewModel.info.value
        val text = buildString {
            section(getString(R.string.gps_provider), info.provider)
            section(getString(R.string.gps_location), info.location)
            section(getString(R.string.gps_movement), info.movement)
            if (info.custom) append(getString(R.string.specified_location_notice)).append("\n\n")
            section(getString(R.string.gps_coordinates), info.coordinates + InfoRow(getString(R.string.gps_address) + ":", info.address))
        }.trim()
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.gps_info), text))
        state.message = R.string.info_copied
    }

    private fun StringBuilder.section(title: String, rows: List<InfoRow>) {
        append(title).append('\n')
        rows.forEach { append(it.label).append(' ').append(it.value).append('\n') }
        append('\n')
    }

    override fun share() {
        val (latitude, longitude) = coordinates()
        LocationShare.share(activity, latitude, longitude, viewModel.info.value.address)
    }

    override fun locationDetails() = dialogs.show(GpsDialog.Details)

    override fun coordinateDetails() {
        val (latitude, longitude) = coordinates()
        dialogs.show(GpsDialog.Formats(latitude, longitude))
    }

    @Composable
    override fun DialogContent(dialog: PanelDialog) {
        when (dialog) {
            is GpsDialog.Summary -> LocationSummaryDialog(dialog.latitude, dialog.longitude, MainPreferences.isCustomCoordinate(), dialogs::dismiss)
            is GpsDialog.Formats -> CoordinateDetailsDialog(dialog.latitude, dialog.longitude, dialogs::dismiss)
            GpsDialog.Pin -> PinCustomizationDialog(dialogs::dismiss)
            GpsDialog.Details -> {
                val info by viewModel.info.collectAsStateWithLifecycle()
                val accuracy by locationModel.accuracySeries.collectAsStateWithLifecycle()
                val altitude by locationModel.altitudeSeries.collectAsStateWithLifecycle()
                LocationDetailsDialog(info.location, accuracy, altitude, MainPreferences.isMetric(), dialogs::dismiss)
            }
            GpsDialog.Target -> TargetCoordinatesDialog(targetForm, onSearch = ::search, onDismiss = dialogs::dismiss) { latitude, longitude ->
                GPSPreferences.setTargetMarkerLatitude(latitude.toFloat())
                GPSPreferences.setTargetMarkerLongitude(longitude.toFloat())
                GPSPreferences.setTargetMarker(true)
                dialogs.dismiss()
            }
            else -> super.DialogContent(dialog)
        }
    }

    private sealed interface GpsDialog : PanelDialog {
        data class Summary(val latitude: Double, val longitude: Double) : GpsDialog
        data class Formats(val latitude: Double, val longitude: Double) : GpsDialog
        data object Pin : GpsDialog
        data object Details : GpsDialog
        data object Target : GpsDialog
    }

    override fun showTarget() {
        sheet(false)
        if (GPSPreferences.isTargetMarkerSet()) maps.moveCameraToTarget() else GPSPreferences.setTargetMarkerMode(true)
    }

    override fun setPressedTarget() {
        pressed?.let(maps::setTargetMarker)
        state.pressed = null
    }

    override fun navigatePressed() {
        state.pressed = null
        val latLng = pressed ?: return
        MapIntents.navigate(activity, latLng.latitude, latLng.longitude)
    }

    override fun zoom(zoomIn: Boolean) = if (zoomIn) maps.zoomIn() else maps.zoomOut()

    override fun recenter() = maps.resetCamera(GPSPreferences.getMapZoom())

    private companion object {
        const val FULL_SCREEN_ACTION = "action_map_panel_full"
        const val CLOSE_ZOOM = 18F
    }
}
