package app.simple.positional.viewmodels.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.viewModelScope
import app.simple.positional.R
import app.simple.positional.extensions.viewmodel.WrappedViewModel
import app.simple.positional.preferences.DirectionPreferences
import app.simple.positional.preferences.GPSPreferences
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.singleton.SharedPreferences.changes
import app.simple.positional.ui.compose.DirectionInfo
import app.simple.positional.ui.compose.InfoRow
import app.simple.positional.util.DMSConverter
import app.simple.positional.util.DistanceText
import app.simple.positional.util.LocationExtension
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

class DirectionViewModel(application: Application, location: LocationViewModel) : WrappedViewModel(application) {
    val info: StateFlow<DirectionInfo> = combine(changes(*KEYS), location.location) { _, fix -> info(fix) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), DirectionInfo())

    private fun info(fix: Location?): DirectionInfo {
        val context = getContext()
        val maps = DirectionPreferences.isUsingMapsTarget() && GPSPreferences.isTargetMarkerSet()
        val stored = if (maps) GPSPreferences.getTargetMarkerCoordinates() else DirectionPreferences.getTargetCoordinates()
        val target = LatLng(stored[0].toDouble(), stored[1].toDouble())
        val formatted = DMSConverter.getFormattedCoordinates(target, context)
        val info = DirectionInfo(
            target = if (maps) context.getString(R.string.using_maps_target) else DirectionPreferences.getTargetLabel() ?: context.getString(R.string.not_available),
            coordinates = listOf(
                InfoRow(context.getString(R.string.gps_latitude), formatted[0]),
                InfoRow(context.getString(R.string.gps_longitude), formatted[1]),
            ),
        )
        fix ?: return info
        val meters = LocationExtension.measureDisplacement(arrayOf(LatLng(fix.latitude, fix.longitude), target)).toDouble()
        return info.copy(
            displacement = DistanceText.format(context, meters, MainPreferences.isMetric()),
            bearing = LocationExtension.calculateBearingAngle(fix.latitude, fix.longitude, target.latitude, target.longitude).toFloat(),
        )
    }

    private companion object {
        const val STOP_TIMEOUT = 5_000L
        val KEYS = arrayOf(
            DirectionPreferences.directionLatitude,
            DirectionPreferences.directionLongitude,
            DirectionPreferences.directionLabel,
            DirectionPreferences.useMapsTarget,
            GPSPreferences.mapTargetMarker,
            GPSPreferences.mapTargetMarkerLatitude,
            GPSPreferences.mapTargetMarkerLongitude,
            MainPreferences.unit,
            MainPreferences.coordinatesFormat,
        )
    }
}
