package app.simple.positional.viewmodels.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import app.simple.positional.extensions.viewmodel.WrappedViewModel
import app.simple.positional.math.GeoDistance
import app.simple.positional.preferences.ClockPreferences
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.singleton.SharedPreferences.changes
import app.simple.positional.ui.compose.ClockTime
import app.simple.positional.ui.compose.SkyReport
import app.simple.positional.ui.panels.ClockReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId

data class SkyPosition(val latitude: Double, val longitude: Double, val altitude: Double) {
    fun isNear(other: SkyPosition, meters: Double) = GeoDistance.meters(latitude, longitude, other.latitude, other.longitude) < meters
}

@OptIn(ExperimentalCoroutinesApi::class)
class ClockViewModel(application: Application, location: LocationViewModel) : WrappedViewModel(application) {
    private val custom = MutableStateFlow(customPosition())
    private val mutableZone = MutableStateFlow(storedZone())

    val zone: StateFlow<ZoneId> = mutableZone.asStateFlow()

    val position: StateFlow<SkyPosition> = custom
        .flatMapLatest { fixed ->
            if (fixed != null) flowOf(fixed) else location.location.filterNotNull().map { SkyPosition(it.latitude, it.longitude, it.altitude) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), custom.value ?: storedPosition())

    val time: StateFlow<ClockTime> = combine(mutableZone, changes(ClockPreferences.isUsingAmPm)) { zone, _ -> zone }
        .flatMapLatest { zone -> ticker(SECOND) { report().time(zone) } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), ClockTime())

    val sky: StateFlow<SkyReport?> = combine(
        position.distinctUntilChanged { old, new -> old.isNear(new, SKY_DISTANCE) },
        mutableZone,
        changes(MainPreferences.unit, ClockPreferences.isUsingAmPm, ClockPreferences.isUsingSecondsPrecision),
    ) { position, zone, _ -> position to zone }
        .flatMapLatest { (position, zone) -> ticker(MINUTE) { report().sky(position.latitude, position.longitude, position.altitude, zone) } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), null)

    private fun report() = ClockReport(getContext())

    private fun <T> ticker(period: Long, value: () -> T): Flow<T> = flow {
        while (true) {
            emit(value())
            delay(period - System.currentTimeMillis() % period)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            ClockPreferences.timezone -> mutableZone.value = storedZone()
            MainPreferences.isCustomCoordinate, MainPreferences.latitude, MainPreferences.longitude -> custom.value = customPosition()
        }
    }

    private companion object {
        const val STOP_TIMEOUT = 5_000L
        const val SECOND = 1_000L
        const val MINUTE = 60_000L
        const val SKY_DISTANCE = 1_000.0

        fun storedZone(): ZoneId = runCatching { ZoneId.of(ClockPreferences.getTimeZone()) }.getOrDefault(ZoneId.systemDefault())

        fun customPosition(): SkyPosition? {
            if (!MainPreferences.isCustomCoordinate()) return null
            val coordinates = MainPreferences.getCoordinates()
            return SkyPosition(coordinates[0].toDouble(), coordinates[1].toDouble(), 0.0)
        }

        fun storedPosition(): SkyPosition {
            val coordinates = MainPreferences.getLastCoordinates()
            return SkyPosition(coordinates[0].toDouble(), coordinates[1].toDouble(), MainPreferences.getLastAltitude().toDouble())
        }
    }
}
