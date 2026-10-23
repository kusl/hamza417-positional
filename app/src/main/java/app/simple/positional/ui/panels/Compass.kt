package app.simple.positional.ui.panels

import android.content.SharedPreferences
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.location.Location
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import app.simple.positional.R
import app.simple.positional.constants.CompassBloom.compassBloomRes
import app.simple.positional.dialogs.compass.CompassBloom
import app.simple.positional.dialogs.compass.CompassPhysicalProperties
import app.simple.positional.extensions.fragment.SensorPanel
import app.simple.positional.math.MathExtensions.round
import app.simple.positional.preferences.CompassPreferences
import app.simple.positional.sensors.HeadingSensor
import app.simple.positional.ui.compose.CompassScreen
import app.simple.positional.ui.compose.CompassState
import app.simple.positional.ui.compose.InfoRow
import app.simple.positional.ui.compose.axisRows
import app.simple.positional.ui.compose.positionalComposeView
import app.simple.positional.ui.compose.MenuSheetState
import app.simple.positional.ui.compose.PanelMenus
import app.simple.positional.viewmodels.viewmodel.LocationViewModel

class Compass : SensorPanel(), HeadingSensor.Listener {
    private val state = CompassState()
    private val menuSheet = MenuSheetState()
    private lateinit var sensor: HeadingSensor
    private var lastReadings = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensor = HeadingSensor(requireActivity(), this)
        lifecycle.addObserver(sensor)
        applyPreferences()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        positionalComposeView(menuSheet, dialogs) { top ->
            CompassScreen(state, top, onCalibrate = { showCalibration() }, onMenu = ::showMenu)
        }

    private fun showMenu() = menuSheet.show {
        PanelMenus.compass(
            onBloomSkins = { CompassBloom().show(parentFragmentManager, "compass_bloom") },
            onPhysicalProperties = { CompassPhysicalProperties().show(parentFragmentManager, "compass_properties") },
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!sensor.isAvailable) showSensorError()
        ViewModelProvider(requireActivity())[LocationViewModel::class.java].getLocation().observe(viewLifecycleOwner) { showField(it) }
    }

    override fun onHeading(azimuth: Float) = state.onHeading(azimuth)

    override fun onAccuracy(type: Int, accuracy: Int) {
        when (type) {
            Sensor.TYPE_ACCELEROMETER -> state.accelerometerAccuracy = accuracy
            Sensor.TYPE_MAGNETIC_FIELD -> state.magnetometerAccuracy = accuracy
        }
        checkCalibration(accuracy)
    }

    override fun onReadings(accelerometer: FloatArray, magnetometer: FloatArray) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastReadings < READINGS_INTERVAL) return
        lastReadings = now
        state.accelerometer = axisRows(accelerometer)
        state.magnetometer = axisRows(magnetometer)
    }

    private fun showField(location: Location) {
        val field = GeomagneticField(location.latitude.toFloat(), location.longitude.toFloat(), location.altitude.toFloat(), location.time)
        state.field = listOf(
            InfoRow(getString(R.string.compass_inclination), "${round(field.inclination.toDouble(), 2)}°"),
            InfoRow(getString(R.string.compass_declination), "${round(field.declination.toDouble(), 2)}°"),
            InfoRow(getString(R.string.compass_field_strength), "${round(field.fieldStrength.toDouble(), 2)} nT"),
        )
    }

    private fun applyPreferences() {
        sensor.gimbalLock = CompassPreferences.isUsingGimbalLock()
        state.physics = CompassPreferences.isUsingPhysicalProperties()
        state.directionCode = CompassPreferences.getDirectionCode()
        state.bloom = if (CompassPreferences.isFlowerBloomOn()) compassBloomRes.getOrNull(CompassPreferences.getFlowerBloomTheme()) else null
        state.configure(CompassPreferences.getRotationalInertia(), CompassPreferences.getDampingCoefficient(), CompassPreferences.getMagneticCoefficient())
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key in compassKeys) applyPreferences()
    }

    companion object {
        private const val READINGS_INTERVAL = 250L
        private val compassKeys = setOf(
            CompassPreferences.direction_code,
            CompassPreferences.flowerBloom,
            CompassPreferences.flowerBloomTheme,
            CompassPreferences.dampingCoefficient,
            CompassPreferences.magneticCoefficient,
            CompassPreferences.rotationalInertia,
            CompassPreferences.usePhysicalProperties,
            CompassPreferences.useGimbalLock,
        )

        fun newInstance() = Compass()
    }
}
