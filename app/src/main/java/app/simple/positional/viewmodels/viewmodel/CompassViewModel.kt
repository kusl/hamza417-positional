package app.simple.positional.viewmodels.viewmodel

import android.app.Application
import android.hardware.GeomagneticField
import android.location.Location
import androidx.lifecycle.viewModelScope
import app.simple.positional.R
import app.simple.positional.extensions.viewmodel.WrappedViewModel
import app.simple.positional.math.MathExtensions.round
import app.simple.positional.ui.compose.InfoRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class CompassViewModel(application: Application, location: LocationViewModel) : WrappedViewModel(application) {
    val field: StateFlow<List<InfoRow>> = location.location.filterNotNull()
        .map(::fieldRows)
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    private fun fieldRows(location: Location): List<InfoRow> {
        val field = GeomagneticField(location.latitude.toFloat(), location.longitude.toFloat(), location.altitude.toFloat(), location.time)
        val context = getContext()
        return listOf(
            InfoRow(context.getString(R.string.compass_inclination), "${round(field.inclination.toDouble(), 2)}°"),
            InfoRow(context.getString(R.string.compass_declination), "${round(field.declination.toDouble(), 2)}°"),
            InfoRow(context.getString(R.string.compass_field_strength), "${round(field.fieldStrength.toDouble(), 2)} nT"),
        )
    }

    private companion object {
        const val STOP_TIMEOUT = 5_000L
    }
}
