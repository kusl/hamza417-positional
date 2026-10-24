package app.simple.positional.ui.panels

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import android.content.ClipData
import android.content.ClipboardManager
import android.content.SharedPreferences
import android.location.Location
import android.view.MotionEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.getSystemService
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import app.simple.positional.R
import app.simple.positional.callbacks.BottomBarHost
import app.simple.positional.constants.LocationPins
import app.simple.positional.database.instances.LocationDatabase
import app.simple.positional.decorations.maps.GpsMap
import app.simple.positional.extensions.panel.PanelController
import app.simple.positional.extensions.maps.MapsCallbacks
import app.simple.positional.math.MathExtensions.round
import app.simple.positional.math.MovementThreshold
import app.simple.positional.math.UnitConverter.toFeet
import app.simple.positional.math.UnitConverter.toKiloMetersPerHour
import app.simple.positional.math.UnitConverter.toMilesPerHour
import app.simple.positional.model.Locations
import app.simple.positional.preferences.GPSPreferences
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.ui.compose.GpsActions
import app.simple.positional.ui.compose.GpsScreen
import app.simple.positional.ui.compose.GpsState
import app.simple.positional.ui.compose.InfoRow
import app.simple.positional.ui.compose.MapRotation
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
import app.simple.positional.ui.compose.toAnnotatedString
import app.simple.positional.util.DMSConverter
import app.simple.positional.util.Direction.getDirectionNameFromAzimuth
import app.simple.positional.util.LocaleHelper
import app.simple.positional.util.LocationExtension.getLocationStatus
import app.simple.positional.util.LocationPrompt
import app.simple.positional.util.MapIntents
import app.simple.positional.util.MapSource
import app.simple.positional.util.NetworkCheck.isNetworkAvailable
import app.simple.positional.util.ReverseGeocoder
import app.simple.positional.viewmodels.viewmodel.LocationViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GPS(activity: ComponentActivity) : PanelController(activity), GpsActions, MapsCallbacks {
    private val state: GpsState
    private val targetForm = CustomLocationForm()
    private var accuracySeries by mutableStateOf(emptyList<Float>())
    private var altitudeSeries by mutableStateOf(emptyList<Float>())
    private val maps: GpsMap
    private val viewModel: LocationViewModel
    private val addressThreshold = MovementThreshold(ADDRESS_DISTANCE)
    private var addressJob: Job? = null
    private var location: Location? = null
    private var pressed: LatLng? = null
    private var accuracyRow = InfoRow("")
    private var altitudeRow = InfoRow("")
    private var latencyRow = InfoRow("")

    init {
        viewModel = ViewModelProvider(activity)[LocationViewModel::class.java]
        state = GpsState(MainPreferences.isCustomCoordinate())
        state.fullScreen = !isLandscapeOrientation && consumeFullScreenShortcut()
        readPreferences()
        accuracyRow = InfoRow(getString(R.string.gps_accuracy), PENDING)
        altitudeRow = InfoRow(getString(R.string.gps_altitude), PENDING)
        latencyRow = InfoRow(getString(R.string.gps_latency), PENDING)
        publishLocationRows()
        state.provider = providerRows(PENDING)
        state.movement = listOf(R.string.gps_bearing, R.string.gps_direction, R.string.gps_speed).map { InfoRow(getString(it), PENDING) }
        state.coordinates = listOf(R.string.gps_latitude, R.string.gps_longitude).map { InfoRow(getString(it), PENDING) }
        state.address = PENDING
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
        GpsScreen(state, maps.view, topInset, isLandscapeOrientation, this)
    }

    init {
        owner.lifecycle.addObserver(maps.lifecycleObserver)
        if (state.fullScreen) bottomBar(visible = false)

        val needsPlayServices = !MapSource.isOsm() || MainPreferences.getLocationProvider() == SettingsLabels.PROVIDER_FUSED
        if (needsPlayServices && !LocationPrompt.checkGooglePlayServices(activity)) {
            showError(getString(R.string.play_services_error))
        }

        if (state.custom) {
            val coordinates = MainPreferences.getCoordinates()
            showCoordinates(coordinates[0].toDouble(), coordinates[1].toDouble())
        }

        observe(viewModel.location.filterNotNull(), ::onLocation)
        observe(viewModel.provider.filterNotNull()) {
            state.enabled = getLocationStatus(activity)
            state.provider = providerRows(it)
        }
        observe(viewModel.latency.filterNotNull()) {
            val unit = getString(if (it.inSeconds) R.string.seconds else R.string.milliseconds)
            latencyRow = InfoRow(getString(R.string.gps_latency), "${it.value} $unit", it.isLate)
            publishLocationRows()
        }
        observe(viewModel.target.filterNotNull()) { state.target = it.toAnnotatedString() }
        observe(viewModel.accuracySeries) { accuracySeries = it }
        observe(viewModel.altitudeSeries) { altitudeSeries = it }
    }

    override fun onCleared() {
        addressJob?.cancel()
        addressThreshold.reset()
    }

    private fun onLocation(location: Location) {
        this.location = location

        val metric = MainPreferences.isMetric()
        val length = getString(if (metric) R.string.meter else R.string.feet)
        val accuracy = if (metric) location.accuracy.toDouble() else location.accuracy.toDouble().toFeet()
        val altitude = if (metric) location.altitude else location.altitude.toFeet()
        val kmh = location.speed.toDouble().toKiloMetersPerHour()
        val speed = if (metric) kmh else kmh.toMilesPerHour()
        val direction = if (location.speed > 0f) getDirectionNameFromAzimuth(activity, location.bearing.toDouble()) else getString(R.string.not_available)

        state.fixed = true
        state.enabled = getLocationStatus(activity)
        state.provider = providerRows(location.provider?.uppercase(LocaleHelper.getAppLocale()) ?: getString(R.string.not_available))
        accuracyRow = InfoRow(getString(R.string.gps_accuracy), "${round(accuracy, 2)} $length")
        altitudeRow = InfoRow(getString(R.string.gps_altitude), "${round(altitude, 2)} $length")
        publishLocationRows()
        state.movement = listOf(
            InfoRow(getString(R.string.gps_bearing), "${location.bearing}°"),
            InfoRow(getString(R.string.gps_direction), direction),
            InfoRow(getString(R.string.gps_speed), "${round(speed, 2)} ${getString(if (metric) R.string.kilometer_hour else R.string.miles_hour)}"),
        )
        state.speed = if (metric) location.speed.toKiloMetersPerHour() else location.speed.toMilesPerHour()

        if (!state.custom) {
            maps.updateLocation(location)
            showCoordinates(location.latitude, location.longitude)
        }
    }

    private fun showCoordinates(latitude: Double, longitude: Double) {
        state.coordinates = listOf(
            InfoRow(getString(R.string.gps_latitude), DMSConverter.getFormattedLatitude(latitude, activity)),
            InfoRow(getString(R.string.gps_longitude), DMSConverter.getFormattedLongitude(longitude, activity)),
        )
        if (!addressThreshold.accept(latitude, longitude)) return
        addressJob?.cancel()
        addressJob = owner.lifecycleScope.launch {
            state.address = if (isNetworkAvailable(activity)) {
                ReverseGeocoder.addressLine(activity, latitude, longitude) ?: getString(R.string.no_address_found)
            } else {
                addressThreshold.reset()
                getString(R.string.internet_connection_alert)
            }
        }
    }

    private fun providerRows(source: String) = listOf(
        InfoRow(getString(R.string.gps_status), getString(if (getLocationStatus(activity)) R.string.gps_enabled else R.string.gps_disabled)),
        InfoRow(getString(R.string.gps_source), source),
    )

    private fun publishLocationRows() {
        state.location = listOf(accuracyRow, altitudeRow, latencyRow)
    }

    private fun readPreferences() {
        state.pin = LocationPins.getLocationPin()
        state.targetMode = GPSPreferences.isTargetMarkerMode()
        state.targetSet = GPSPreferences.isTargetMarkerSet()
        state.toolsLeft = GPSPreferences.isToolsGravityLeft()
        state.volumeKeys = GPSPreferences.isUsingVolumeKeys()
        state.rotation = when {
            GPSPreferences.isCompassRotation() -> MapRotation.Compass
            GPSPreferences.isBearingRotation() -> MapRotation.Bearing
            else -> MapRotation.North
        }
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
        val current = location
        if (!state.custom && current != null) return current.latitude to current.longitude
        val stored = if (state.custom) MainPreferences.getCoordinates() else MainPreferences.getLastCoordinates()
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
            address = state.address
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
        val text = buildString {
            section(getString(R.string.gps_provider), state.provider)
            section(getString(R.string.gps_location), state.location)
            section(getString(R.string.gps_movement), state.movement)
            if (state.custom) append(getString(R.string.specified_location_notice)).append("\n\n")
            section(getString(R.string.gps_coordinates), state.coordinates + InfoRow(getString(R.string.gps_address) + ":", state.address))
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
        LocationShare.share(activity, latitude, longitude, state.address)
    }

    override fun locationDetails() = dialogs.show(GpsDialog.Details)

    override fun coordinateDetails() {
        val (latitude, longitude) = coordinates()
        dialogs.show(GpsDialog.Formats(latitude, longitude))
    }

    @Composable
    override fun DialogContent(dialog: PanelDialog) {
        when (dialog) {
            is GpsDialog.Summary -> LocationSummaryDialog(dialog.latitude, dialog.longitude, state.custom, dialogs::dismiss)
            is GpsDialog.Formats -> CoordinateDetailsDialog(dialog.latitude, dialog.longitude, dialogs::dismiss)
            GpsDialog.Pin -> PinCustomizationDialog(dialogs::dismiss)
            GpsDialog.Details -> LocationDetailsDialog(state.location, accuracySeries, altitudeSeries, MainPreferences.isMetric(), dialogs::dismiss)
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

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key in gpsKeys) readPreferences()
    }

    companion object {
        private const val PENDING = "-"
        private const val FULL_SCREEN_ACTION = "action_map_panel_full"
        private const val CLOSE_ZOOM = 18F
        private const val ADDRESS_DISTANCE = 25.0
        private val gpsKeys = setOf(
            GPSPreferences.pinSkin,
            GPSPreferences.toolsGravity,
            GPSPreferences.isTargetMarkerMode,
            GPSPreferences.mapTargetMarker,
            GPSPreferences.compassRotation,
            GPSPreferences.useBearingRotation,
            GPSPreferences.isNorthOnly,
            GPSPreferences.useVolumeKeys,
        )
    }
}
