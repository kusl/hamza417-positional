package app.simple.positional.viewmodels.viewmodel

import android.app.Application
import android.content.Context
import android.location.Location
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.viewModelScope
import app.simple.positional.R
import app.simple.positional.constants.LocationPins
import app.simple.positional.extensions.viewmodel.WrappedViewModel
import app.simple.positional.math.Latency
import app.simple.positional.math.MathExtensions.round
import app.simple.positional.math.MovementThreshold
import app.simple.positional.math.UnitConverter.toFeet
import app.simple.positional.math.UnitConverter.toKiloMetersPerHour
import app.simple.positional.math.UnitConverter.toMilesPerHour
import app.simple.positional.preferences.GPSPreferences
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.singleton.SharedPreferences.changes
import app.simple.positional.ui.compose.GpsInfo
import app.simple.positional.ui.compose.InfoRow
import app.simple.positional.ui.compose.toAnnotatedString
import app.simple.positional.util.DMSConverter
import app.simple.positional.util.Direction.getDirectionNameFromAzimuth
import app.simple.positional.util.LocaleHelper
import app.simple.positional.util.LocationExtension.getLocationStatus
import app.simple.positional.util.NetworkCheck.isNetworkAvailable
import app.simple.positional.util.Geocoding
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class GpsViewModel(application: Application, source: LocationViewModel) : WrappedViewModel(application) {
    private val addressThreshold = MovementThreshold(ADDRESS_DISTANCE)

    private val point: Flow<Pair<Boolean, LatLng?>> = combine(
        changes(MainPreferences.isCustomCoordinate, MainPreferences.latitude, MainPreferences.longitude),
        source.location,
    ) { _, fix -> customPoint()?.let { true to it } ?: (false to fix?.let { LatLng(it.latitude, it.longitude) }) }
        .distinctUntilChanged()

    private val address: Flow<String> = point
        .onStart { addressThreshold.reset() }
        .map { it.second }
        .filterNotNull()
        .filter { addressThreshold.accept(it.latitude, it.longitude) }
        .mapLatest(::lookup)
        .onStart { emit(PENDING) }

    private val fix: Flow<FixRows> = combine(source.location, source.provider, source.latency, changes(MainPreferences.unit)) { fix, provider, latency, _ ->
        fixRows(getContext(), fix, provider, latency)
    }

    private val coordinates: Flow<Pair<Boolean, List<InfoRow>>> = combine(point, changes(MainPreferences.coordinatesFormat)) { (custom, latLng), _ ->
        custom to coordinateRows(getContext(), latLng)
    }

    val info: StateFlow<GpsInfo> = combine(fix, coordinates, address, source.target) { rows, (custom, coords), addressLine, target ->
        GpsInfo(
            provider = rows.provider,
            location = rows.location,
            movement = rows.movement,
            coordinates = coords,
            address = addressLine,
            target = target?.toAnnotatedString() ?: AnnotatedString(""),
            speed = rows.speed,
            fixed = rows.fixed,
            enabled = rows.enabled,
            custom = custom,
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), GpsInfo(custom = MainPreferences.isCustomCoordinate()))

    val settings: StateFlow<GpsSettings> = preference(
        GPSPreferences.pinSkin,
        GPSPreferences.toolsGravity,
        GPSPreferences.isTargetMarkerMode,
        GPSPreferences.mapTargetMarker,
        GPSPreferences.compassRotation,
        GPSPreferences.useBearingRotation,
        GPSPreferences.useVolumeKeys,
    ) {
        GpsSettings(
            pin = LocationPins.getLocationPin(),
            targetMode = GPSPreferences.isTargetMarkerMode(),
            targetSet = GPSPreferences.isTargetMarkerSet(),
            toolsLeft = GPSPreferences.isToolsGravityLeft(),
            volumeKeys = GPSPreferences.isUsingVolumeKeys(),
            rotation = GpsSettings.rotation(GPSPreferences.isCompassRotation(), GPSPreferences.isBearingRotation()),
        )
    }

    private suspend fun lookup(point: LatLng): String {
        val context = getContext()
        if (!isNetworkAvailable(context)) {
            addressThreshold.reset()
            return context.getString(R.string.internet_connection_alert)
        }
        return Geocoding.addressLine(context, point.latitude, point.longitude) ?: context.getString(R.string.no_address_found)
    }

    private fun customPoint(): LatLng? {
        if (!MainPreferences.isCustomCoordinate()) return null
        val coordinates = MainPreferences.getCoordinates()
        return LatLng(coordinates[0].toDouble(), coordinates[1].toDouble())
    }

    private class FixRows(
        val provider: List<InfoRow>,
        val location: List<InfoRow>,
        val movement: List<InfoRow>,
        val speed: Float,
        val fixed: Boolean,
        val enabled: Boolean,
    )

    private fun fixRows(context: Context, fix: Location?, provider: String?, latency: Latency?): FixRows {
        val enabled = getLocationStatus(context)
        val metric = MainPreferences.isMetric()
        val length = context.getString(if (metric) R.string.meter else R.string.feet)
        val source = fix?.provider?.uppercase(LocaleHelper.getAppLocale()) ?: provider ?: PENDING
        val latencyRow = if (latency == null) {
            InfoRow(context.getString(R.string.gps_latency), PENDING)
        } else {
            val unit = context.getString(if (latency.inSeconds) R.string.seconds else R.string.milliseconds)
            InfoRow(context.getString(R.string.gps_latency), "${latency.value} $unit", latency.isLate)
        }
        val providerRows = listOf(
            InfoRow(context.getString(R.string.gps_status), context.getString(if (enabled) R.string.gps_enabled else R.string.gps_disabled)),
            InfoRow(context.getString(R.string.gps_source), source),
        )
        if (fix == null) {
            return FixRows(
                providerRows,
                listOf(InfoRow(context.getString(R.string.gps_accuracy), PENDING), InfoRow(context.getString(R.string.gps_altitude), PENDING), latencyRow),
                listOf(R.string.gps_bearing, R.string.gps_direction, R.string.gps_speed).map { InfoRow(context.getString(it), PENDING) },
                0f,
                false,
                enabled,
            )
        }
        val accuracy = if (metric) fix.accuracy.toDouble() else fix.accuracy.toDouble().toFeet()
        val altitude = if (metric) fix.altitude else fix.altitude.toFeet()
        val kmh = fix.speed.toDouble().toKiloMetersPerHour()
        val speed = if (metric) kmh else kmh.toMilesPerHour()
        val direction = if (fix.speed > 0f) getDirectionNameFromAzimuth(context, fix.bearing.toDouble()) else context.getString(R.string.not_available)
        return FixRows(
            providerRows,
            listOf(
                InfoRow(context.getString(R.string.gps_accuracy), "${round(accuracy, 2)} $length"),
                InfoRow(context.getString(R.string.gps_altitude), "${round(altitude, 2)} $length"),
                latencyRow,
            ),
            listOf(
                InfoRow(context.getString(R.string.gps_bearing), "${fix.bearing}°"),
                InfoRow(context.getString(R.string.gps_direction), direction),
                InfoRow(context.getString(R.string.gps_speed), "${round(speed, 2)} ${context.getString(if (metric) R.string.kilometer_hour else R.string.miles_hour)}"),
            ),
            if (metric) fix.speed.toKiloMetersPerHour() else fix.speed.toMilesPerHour(),
            true,
            enabled,
        )
    }

    private fun coordinateRows(context: Context, point: LatLng?): List<InfoRow> = listOf(
        InfoRow(context.getString(R.string.gps_latitude), point?.let { DMSConverter.getFormattedLatitude(it.latitude, context) } ?: PENDING),
        InfoRow(context.getString(R.string.gps_longitude), point?.let { DMSConverter.getFormattedLongitude(it.longitude, context) } ?: PENDING),
    )

    private companion object {
        const val STOP_TIMEOUT = 5_000L
        const val ADDRESS_DISTANCE = 25.0
        const val PENDING = "-"
    }
}
