package app.simple.positional.ui.panels

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.simple.positional.R
import app.simple.positional.activities.subactivity.TimezonePickerActivity
import app.simple.positional.constants.ClockSkinsConstants.clockNeedleSkins
import app.simple.positional.constants.LocationPins
import app.simple.positional.dialogs.app.LocationParameters
import app.simple.positional.dialogs.clock.ClockMenu
import app.simple.positional.extensions.fragment.ScopedFragment
import app.simple.positional.math.ClockMotion
import app.simple.positional.preferences.ClockPreferences
import app.simple.positional.preferences.GPSPreferences
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.ui.compose.ClockScreen
import app.simple.positional.ui.compose.ClockState
import app.simple.positional.ui.compose.positionalComposeView
import app.simple.positional.viewmodels.viewmodel.LocationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId

class Time : ScopedFragment() {
    private val state = ClockState()
    private lateinit var report: ClockReport
    private var latitude = 0.0
    private var longitude = 0.0
    private var altitude = 0.0
    private var lastSky = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        report = ClockReport(requireContext())
        applyPreferences()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        positionalComposeView { top ->
            ClockScreen(
                state,
                top,
                onTimezone = { startActivity(Intent(requireActivity(), TimezonePickerActivity::class.java)) },
                onMenu = { ClockMenu.newInstance().show(parentFragmentManager, ClockMenu.TAG) },
                onLocation = { LocationParameters.newInstance().show(parentFragmentManager, LocationParameters.TAG) },
            )
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewModelProvider(requireActivity())[LocationViewModel::class.java].getLocation().observe(viewLifecycleOwner) {
            if (state.customLocation) return@observe
            latitude = it.latitude
            longitude = it.longitude
            altitude = it.altitude
            if (SystemClock.elapsedRealtime() - lastSky > SKY_MIN_INTERVAL) refreshSkyAsync()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
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
        if (view != null) viewLifecycleOwner.lifecycleScope.launch { refreshSky() }
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

        fun newInstance() = Time()
    }
}
