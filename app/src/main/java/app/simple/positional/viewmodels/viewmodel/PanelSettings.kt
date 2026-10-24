package app.simple.positional.viewmodels.viewmodel

import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.simple.positional.R
import app.simple.positional.constants.ClockSkinsConstants.clockNeedleSkins
import app.simple.positional.constants.CompassBloom.compassBloomRes
import app.simple.positional.constants.LocationPins
import app.simple.positional.math.ClockMotion
import app.simple.positional.singleton.SharedPreferences
import app.simple.positional.ui.compose.MapRotation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

data class MapTools(val toolsLeft: Boolean, val compass: Boolean, val wrapped: Boolean)

data class GpsSettings(
    @param:DrawableRes val pin: Int,
    val targetMode: Boolean,
    val targetSet: Boolean,
    val toolsLeft: Boolean,
    val volumeKeys: Boolean,
    val rotation: MapRotation,
) {
    companion object {
        fun rotation(compass: Boolean, bearing: Boolean): MapRotation = when {
            compass -> MapRotation.Compass
            bearing -> MapRotation.Bearing
            else -> MapRotation.North
        }
    }
}

data class CompassSettings(
    val gimbalLock: Boolean,
    val physics: Boolean,
    val directionCode: Boolean,
    val bloom: Int?,
    val inertia: Float,
    val damping: Float,
    val magnetic: Float,
) {
    companion object {
        fun bloom(on: Boolean, theme: Int): Int? = if (on) compassBloomRes.getOrNull(theme) else null
    }
}

data class ClockSettings(val motion: ClockMotion, val face24: Boolean, val needles: List<Int>, @param:DrawableRes val pin: Int, val customLocation: Boolean) {
    companion object {
        fun needles(theme: Int): List<Int> = clockNeedleSkins.getOrElse(theme) { clockNeedleSkins[0] }.toList()

        fun pin(skin: Int): Int = LocationPins.locationsPins.getOrElse(skin) { R.drawable.ic_pin_01 }
    }
}

private const val STOP_TIMEOUT = 5_000L

fun <T> ViewModel.preference(vararg keys: String, read: () -> T): StateFlow<T> =
    SharedPreferences.watch(*keys, read = read).stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), read())
