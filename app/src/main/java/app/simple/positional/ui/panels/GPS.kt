package app.simple.positional.ui.panels

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import app.simple.positional.R
import app.simple.positional.callbacks.BottomSheetSlide
import app.simple.positional.constants.LocationPins
import app.simple.positional.database.instances.LocationDatabase
import app.simple.positional.decorations.maps.Maps
import app.simple.positional.dialogs.app.ErrorDialog
import app.simple.positional.dialogs.app.LocationParameters
import app.simple.positional.dialogs.gps.CoordinatesExpansion
import app.simple.positional.dialogs.gps.GPSMenu
import app.simple.positional.dialogs.gps.LocationExpansion
import app.simple.positional.dialogs.gps.PinCustomization
import app.simple.positional.dialogs.gps.TargetCoordinates
import app.simple.positional.extensions.fragment.ScopedFragment
import app.simple.positional.extensions.maps.MapLifecycle
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
import app.simple.positional.ui.compose.positionalComposeView
import app.simple.positional.ui.compose.toAnnotatedString
import app.simple.positional.util.DMSConverter
import app.simple.positional.util.Direction.getDirectionNameFromAzimuth
import app.simple.positional.util.LocaleHelper
import app.simple.positional.util.LocationExtension.getLocationStatus
import app.simple.positional.util.LocationPrompt
import app.simple.positional.util.NetworkCheck.isNetworkAvailable
import app.simple.positional.util.ParcelUtils.parcelable
import app.simple.positional.util.ReverseGeocoder
import app.simple.positional.viewmodels.viewmodel.LocationViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GPS : ScopedFragment(), GpsActions, MapsCallbacks {
    private lateinit var state: GpsState
    private lateinit var maps: Maps
    private lateinit var viewModel: LocationViewModel
    private val addressThreshold = MovementThreshold(ADDRESS_DISTANCE)
    private var addressJob: Job? = null
    private var location: Location? = null
    private var camera: CameraPosition? = null
    private var pressed: LatLng? = null
    private var accuracyRow = InfoRow("")
    private var altitudeRow = InfoRow("")
    private var latencyRow = InfoRow("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[LocationViewModel::class.java]
        state = GpsState(MainPreferences.isCustomCoordinate())
        camera = savedInstanceState?.parcelable(CAMERA)
        state.fullScreen = !isLandscapeOrientation && (savedInstanceState?.getBoolean(FULL_SCREEN) ?: consumeFullScreenShortcut())
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        maps = Maps(requireContext()).apply {
            alpha = 0f
            onCreate(null)
            setOnMapsCallbackListener(this@GPS)
            onTouch = { event, _ ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> unregister()
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> registerWithRunnable()
                }
            }
        }
        return positionalComposeView { top -> GpsScreen(state, maps, top, isLandscapeOrientation, this) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycle.addObserver(MapLifecycle(maps))
        if (state.fullScreen) bottomBar(visible = false)

        if (!LocationPrompt.checkGooglePlayServices(requireContext())) {
            ErrorDialog.newInstance(getString(R.string.play_services_error)).show(childFragmentManager, "error_dialog")
        }

        if (state.custom) {
            val coordinates = MainPreferences.getCoordinates()
            showCoordinates(coordinates[0].toDouble(), coordinates[1].toDouble())
        }

        viewModel.getLocation().observe(viewLifecycleOwner, ::onLocation)
        viewModel.getProvider().observe(viewLifecycleOwner) {
            state.enabled = getLocationStatus(requireContext())
            state.provider = providerRows(it)
        }
        viewModel.latency.observe(viewLifecycleOwner) {
            val unit = getString(if (it.second) R.string.seconds else R.string.milliseconds)
            latencyRow = InfoRow(getString(R.string.gps_latency), "${it.first} $unit", it.second && it.first.toDouble() > LATE)
            publishLocationRows()
        }
        viewModel.targetData.observe(viewLifecycleOwner) { state.target = it.toAnnotatedString() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        addressJob?.cancel()
        addressThreshold.reset()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(FULL_SCREEN, state.fullScreen)
        if (::maps.isInitialized) outState.putParcelable(CAMERA, maps.getCamera() ?: camera)
    }

    private fun onLocation(location: Location) {
        this.location = location
        MainPreferences.setLastLatitude(location.latitude.toFloat())
        MainPreferences.setLastLongitude(location.longitude.toFloat())
        MainPreferences.setLastAltitude(location.altitude.toFloat())

        val metric = MainPreferences.isMetric()
        val length = getString(if (metric) R.string.meter else R.string.feet)
        val accuracy = if (metric) location.accuracy.toDouble() else location.accuracy.toDouble().toFeet()
        val altitude = if (metric) location.altitude else location.altitude.toFeet()
        val kmh = location.speed.toDouble().toKiloMetersPerHour()
        val speed = if (metric) kmh else kmh.toMilesPerHour()
        val direction = if (location.speed > 0f) getDirectionNameFromAzimuth(requireContext(), location.bearing.toDouble()) else getString(R.string.not_available)

        state.fixed = true
        state.enabled = getLocationStatus(requireContext())
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
            maps.setFirstLocation(location)
            maps.location = location
            maps.addMarker(LatLng(location.latitude, location.longitude))
            showCoordinates(location.latitude, location.longitude)
        }
    }

    private fun showCoordinates(latitude: Double, longitude: Double) {
        state.coordinates = listOf(
            InfoRow(getString(R.string.gps_latitude), DMSConverter.getFormattedLatitude(latitude, requireContext())),
            InfoRow(getString(R.string.gps_longitude), DMSConverter.getFormattedLongitude(longitude, requireContext())),
        )
        if (!addressThreshold.accept(latitude, longitude)) return
        addressJob?.cancel()
        addressJob = viewLifecycleOwner.lifecycleScope.launch {
            state.address = if (isNetworkAvailable(requireContext())) {
                ReverseGeocoder.addressLine(requireContext(), latitude, longitude) ?: getString(R.string.no_address_found)
            } else {
                addressThreshold.reset()
                getString(R.string.internet_connection_alert)
            }
        }
    }

    private fun providerRows(source: String) = listOf(
        InfoRow(getString(R.string.gps_status), getString(if (getLocationStatus(requireContext())) R.string.gps_enabled else R.string.gps_disabled)),
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
        val intent = requireActivity().intent ?: return false
        if (intent.action != FULL_SCREEN_ACTION) return false
        intent.action = null
        return true
    }

    private fun bottomBar(visible: Boolean) {
        (requireActivity() as? BottomSheetSlide)?.onMapClicked(visible)
    }

    override fun onMapInitialized() {
        camera?.let(maps::setCamera)
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

    override fun menu() = GPSMenu().show(parentFragmentManager, "gps_menu")

    override fun customLocation() = LocationParameters.newInstance().show(parentFragmentManager, "location_params")

    override fun pinCustomization() = PinCustomization.newInstance(true).show(parentFragmentManager, "pin_customization")

    override fun locate(close: Boolean) {
        if (getLocationStatus(requireContext())) {
            maps.isMapMovementEnabled = true
            maps.resetCamera(if (close) CLOSE_ZOOM else GPSPreferences.getMapZoom())
        } else {
            LocationPrompt.displayLocationSettingsRequest(requireActivity())
        }
    }

    override fun toggleTargetMode() = GPSPreferences.setTargetMarkerMode(!GPSPreferences.isTargetMarkerMode())

    override fun confirmTarget() {
        GPSPreferences.setTargetMarkerMode(false)
        maps.setTargetMarker(null)
    }

    override fun targetCoordinates() = TargetCoordinates.newInstance().show(childFragmentManager, "target_coordinates")

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
        val dao = LocationDatabase.getInstance(requireContext()).locationDao()
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(NonCancellable) { dao.insertLocation(entry) }
            state.message = R.string.location_saved
        }
    }

    override fun copy() {
        val clipboard = requireContext().getSystemService<ClipboardManager>() ?: return
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
        val target = location?.let { it.latitude to it.longitude }
            ?: MainPreferences.getCoordinates().takeIf { state.custom }?.let { it[0].toDouble() to it[1].toDouble() }
            ?: return
        val latitude = round(target.first, 6)
        val longitude = round(target.second, 6)
        val text = "${getString(R.string.gps_coordinates)}: $latitude, $longitude\n\n${state.address}\n\n" +
                "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude\n\ngeo:$latitude,$longitude?q=$latitude,$longitude"
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), getString(R.string.share)))
    }

    override fun locationDetails() = LocationExpansion.newInstance().show(childFragmentManager, "location_expansion")

    override fun coordinateDetails() = CoordinatesExpansion.newInstance().show(childFragmentManager, "coordinates_expansion")

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
        val navigation = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${latLng.latitude},${latLng.longitude}&mode=d"))
            .setPackage(GOOGLE_MAPS)
        val generic = Intent(Intent.ACTION_VIEW, Uri.parse("geo:${latLng.latitude},${latLng.longitude}?q=${latLng.latitude},${latLng.longitude}"))
        runCatching { startActivity(navigation) }
            .recoverCatching { startActivity(generic) }
            .onFailure { Toast.makeText(requireContext(), R.string.error, Toast.LENGTH_SHORT).show() }
    }

    override fun zoom(zoomIn: Boolean) = if (zoomIn) maps.zoomIn() else maps.zoomOut()

    override fun recenter() = maps.resetCamera(GPSPreferences.getMapZoom())

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key in gpsKeys) readPreferences()
    }

    companion object {
        private const val PENDING = "-"
        private const val CAMERA = "camera"
        private const val FULL_SCREEN = "fullscreen"
        private const val FULL_SCREEN_ACTION = "action_map_panel_full"
        private const val GOOGLE_MAPS = "com.google.android.apps.maps"
        private const val CLOSE_ZOOM = 18F
        private const val LATE = 5.0
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

        fun newInstance() = GPS()
    }
}
