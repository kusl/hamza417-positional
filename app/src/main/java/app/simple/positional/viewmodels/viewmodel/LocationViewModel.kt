package app.simple.positional.viewmodels.viewmodel

import android.app.Application
import android.content.Context
import android.location.Location
import android.os.SystemClock
import android.text.Spanned
import android.text.TextUtils
import androidx.lifecycle.viewModelScope
import app.simple.positional.R
import app.simple.positional.extensions.viewmodel.WrappedViewModel
import app.simple.positional.location.LocationEvent
import app.simple.positional.location.LocationSource
import app.simple.positional.math.Latency
import app.simple.positional.math.MathExtensions.round
import app.simple.positional.math.MovementThreshold
import app.simple.positional.math.SeriesStats
import app.simple.positional.preferences.GPSPreferences
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.util.Direction
import app.simple.positional.util.DistanceText
import app.simple.positional.util.HtmlHelper
import app.simple.positional.util.LocationExtension
import app.simple.positional.util.ReverseGeocoder
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class LocationViewModel(application: Application) : WrappedViewModel(application) {
    private val providerSetting = preference(MainPreferences.locationProvider) { MainPreferences.getLocationProvider() }
    private val restarts = MutableStateFlow(0)
    private val saved = MovementThreshold(SAVE_DISTANCE)
    private val mutableLatency = MutableStateFlow<Latency?>(null)
    private val mutableAccuracy = MutableStateFlow(emptyList<Float>())
    private val mutableAltitude = MutableStateFlow(emptyList<Float>())
    private val targetPoint = preference(GPSPreferences.mapTargetMarker, GPSPreferences.mapTargetMarkerLatitude, GPSPreferences.mapTargetMarkerLongitude, read = ::currentTarget)
    private var lastFix = SystemClock.elapsedRealtime()

    private val events = combine(providerSetting, restarts) { provider, _ -> provider }
        .flatMapLatest { provider -> LocationSource.of(application, provider).events().onStart { lastFix = SystemClock.elapsedRealtime() } }
        .onEach(::record)
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT))

    val location: StateFlow<Location?> = events.filterIsInstance<LocationEvent.Fix>()
        .map { it.location }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
    val provider: StateFlow<String?> = events.filterIsInstance<LocationEvent.Provider>()
        .map { it.name?.uppercase(Locale.getDefault()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
    val latency: StateFlow<Latency?> = mutableLatency.asStateFlow()
    val accuracySeries: StateFlow<List<Float>> = mutableAccuracy.asStateFlow()
    val altitudeSeries: StateFlow<List<Float>> = mutableAltitude.asStateFlow()

    private val targetAddress = targetPoint.transformLatest<LatLng?, String?> { point ->
        emit(null)
        if (point != null) emit(ReverseGeocoder.addressLine(getApplication<Application>(), point.latitude, point.longitude))
    }

    val target: StateFlow<Spanned?> = combine(location.filterNotNull(), targetPoint, targetAddress) { fix, point, address -> targetText(fix, point, address) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), null)

    fun restart() = restarts.update { it + 1 }

    private fun record(event: LocationEvent) {
        if (event !is LocationEvent.Fix) return
        val fix = event.location
        val now = SystemClock.elapsedRealtime()
        mutableLatency.value = Latency(now - lastFix)
        lastFix = now
        mutableAccuracy.update { SeriesStats.append(it, fix.accuracy) }
        mutableAltitude.update { SeriesStats.append(it, fix.altitude.toFloat()) }
        if (saved.accept(fix.latitude, fix.longitude)) {
            MainPreferences.setLastLocation(fix.latitude.toFloat(), fix.longitude.toFloat(), fix.altitude.toFloat())
        }
    }

    private fun currentTarget(): LatLng? =
        if (GPSPreferences.isTargetMarkerSet()) GPSPreferences.getTargetMarkerCoordinates().let { LatLng(it[0].toDouble(), it[1].toDouble()) } else null

    private fun targetText(fix: Location, target: LatLng?, address: String?): Spanned {
        val context = getContext()
        val missing = context.getString(R.string.not_available)
        val values = if (target == null) List(ROWS.size) { missing } else {
            val origin = if (MainPreferences.isCustomCoordinate()) {
                MainPreferences.getCoordinates().let { LatLng(it[0].toDouble(), it[1].toDouble()) }
            } else {
                LatLng(fix.latitude, fix.longitude)
            }
            val start = GPSPreferences.getTargetMarkerStartCoordinates().let { LatLng(it[0].toDouble(), it[1].toDouble()) }
            val metric = MainPreferences.isMetric()
            val distance = LocationExtension.measureDisplacement(arrayOf(target, origin)).toDouble()
            val bearing = (LocationExtension.calculateBearingAngle(origin.latitude, origin.longitude, target.latitude, target.longitude) % 360 + 360) % 360
            listOf(
                DistanceText.format(context, distance, metric),
                DistanceText.format(context, LocationExtension.measureDisplacement(arrayOf(start, origin)).toDouble(), metric),
                "${round(bearing, 2)}°",
                Direction.getDirectionNameFromAzimuth(context, bearing),
                if (fix.speed > 0f) eta(context, (distance / fix.speed).toLong()) else missing,
            )
        }
        val rows = ROWS.zip(values) { label, value -> "<b>${context.getString(label)} </b>$value" } +
            "<b>${context.getString(R.string.gps_address)}: </b>${address?.let(TextUtils::htmlEncode) ?: missing}"
        return HtmlHelper.fromHtml(rows.joinToString("<br>"))
    }

    private fun eta(context: Context, seconds: Long): String = when {
        seconds < MINUTE -> context.getString(R.string.eta_seconds, seconds.toString())
        seconds < HOUR -> context.getString(R.string.eta_minutes, (seconds / MINUTE).toString())
        seconds < DAY -> context.getString(R.string.eta_hours, (seconds / HOUR).toString(), (seconds / MINUTE % 60).toString())
        else -> context.getString(
            R.string.eta_days,
            (seconds / DAY).toString(),
            (seconds / HOUR % 24).toString(),
            (seconds / MINUTE % 60).toString(),
        )
    }

    private companion object {
        const val STOP_TIMEOUT = 5_000L
        const val SAVE_DISTANCE = 50.0
        const val MINUTE = 60L
        const val HOUR = 3_600L
        const val DAY = 86_400L
        val ROWS = listOf(
            R.string.gps_displacement_from_dest,
            R.string.gps_displacement_from_origin,
            R.string.gps_bearing,
            R.string.gps_direction,
            R.string.eta,
        )
    }
}
