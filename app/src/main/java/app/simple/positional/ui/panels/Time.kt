package app.simple.positional.ui.panels

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import android.content.SharedPreferences
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.simple.positional.R
import app.simple.positional.ui.navigation.Routes
import app.simple.positional.constants.ClockSkinsConstants.clockNeedleSkins
import app.simple.positional.constants.LocationPins
import app.simple.positional.extensions.panel.PanelController
import app.simple.positional.math.ClockMotion
import app.simple.positional.preferences.ClockPreferences
import app.simple.positional.preferences.GPSPreferences
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.ui.compose.ClockScreen
import app.simple.positional.ui.compose.ClockState
import app.simple.positional.ui.compose.Choice
import app.simple.positional.ui.compose.ChoiceDialog
import app.simple.positional.ui.compose.ClockNeedleDialog
import app.simple.positional.ui.compose.LocationSummaryDialog
import app.simple.positional.ui.compose.PanelMenus
import app.simple.positional.viewmodels.viewmodel.LocationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId

class Time(activity: ComponentActivity) : PanelController(activity) {
    private val state = ClockState()
    private lateinit var report: ClockReport
    private var latitude = 0.0
    private var longitude = 0.0
    private var altitude = 0.0
    private var lastSky = 0L

    init {
        report = ClockReport(activity)
        applyPreferences()
    }

    @Composable
    override fun Content(topInset: Dp) {
        ClockScreen(
            state,
            topInset,
            onTimezone = { navigate(Routes.TIMEZONE) },
            onMenu = ::showMenu,
            onLocation = { dialogs.show { LocationSummaryDialog(latitude, longitude, state.customLocation, dialogs::dismiss) } },
        )
    }

    private fun showMenu() = menuSheet.show {
        PanelMenus.clock(
            onNeedles = {
                dialogs.show { ClockNeedleDialog(ClockPreferences.getClockNeedleTheme(), dialogs::dismiss) { ClockPreferences.setClockNeedleTheme(it) } }
            },
            onMotionType = ::showMotionTypes,
        )
    }

    private fun showMotionTypes() {
        val title = getString(R.string.motion_type)
        val choices = motionTypes.map { (value, label) -> Choice(value, getString(label)) }
        dialogs.show {
            ChoiceDialog(title, choices, ClockPreferences.getMovementType(), onDismiss = dialogs::dismiss) {
                ClockPreferences.setMovementType(it)
                dialogs.dismiss()
            }
        }
    }

    init {
        ViewModelProvider(activity)[LocationViewModel::class.java].getLocation().observe(owner) {
            if (state.customLocation) return@observe
            latitude = it.latitude
            longitude = it.longitude
            altitude = it.altitude
            if (SystemClock.elapsedRealtime() - lastSky > SKY_MIN_INTERVAL) refreshSkyAsync()
        }
        owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    while (true) {
                        val zone = state.zone
                        state.time = withContext(Dispatchers.Default) { report.time(zone) }
                        delay(1000L - System.currentTimeMillis() % 1000L)
                    }
                }
                while (true) {
                    refreshSky()
                    delay(SKY_INTERVAL)
                }
            }
        }
    }

    private suspend fun refreshSky() {
        lastSky = SystemClock.elapsedRealtime()
        val zone = state.zone
        state.sky = withContext(Dispatchers.Default) { report.sky(latitude, longitude, altitude, zone) }
    }

    private fun refreshSkyAsync() {
        owner.lifecycleScope.launch { refreshSky() }
    }

    private fun applyPreferences() {
        state.zone = runCatching { ZoneId.of(ClockPreferences.getTimeZone()) }.getOrDefault(ZoneId.systemDefault())
        state.motion = ClockMotion.of(ClockPreferences.getMovementType())
        state.face24 = ClockPreferences.isClockFace24Hour()
        state.needles = clockNeedleSkins.getOrElse(ClockPreferences.getClockNeedleTheme()) { clockNeedleSkins[0] }.toList()
        state.pin = LocationPins.locationsPins.getOrElse(GPSPreferences.getPinSkin()) { R.drawable.ic_pin_01 }
        state.customLocation = MainPreferences.isCustomCoordinate()
        if (state.customLocation) {
            latitude = MainPreferences.getCoordinates()[0].toDouble()
            longitude = MainPreferences.getCoordinates()[1].toDouble()
            altitude = 0.0
        } else {
            latitude = MainPreferences.getLastCoordinates()[0].toDouble()
            longitude = MainPreferences.getLastCoordinates()[1].toDouble()
            altitude = MainPreferences.getLastAltitude().toDouble()
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key !in clockKeys) return
        applyPreferences()
        refreshSkyAsync()
    }

    companion object {
        private const val SKY_INTERVAL = 60_000L
        private val motionTypes = listOf(
            "smooth" to R.string.motion_type_smooth,
            "tick" to R.string.motion_type_tick,
            "oscillate" to R.string.tick_oscillate,
            "tick_smooth" to R.string.tick_smooth,
            "mechanical" to R.string.mechanical,
        )
        private const val SKY_MIN_INTERVAL = 10_000L
        private val clockKeys = setOf(
            ClockPreferences.clockNeedleMovementType,
            ClockPreferences.clockNeedle,
            ClockPreferences.timezone,
            ClockPreferences.is24HourFace,
            MainPreferences.isCustomCoordinate,
            MainPreferences.latitude,
            MainPreferences.longitude,
            MainPreferences.unit,
            GPSPreferences.pinSkin,
        )
    }
}
