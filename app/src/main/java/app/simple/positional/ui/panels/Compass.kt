package app.simple.positional.ui.panels

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import android.content.SharedPreferences
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.location.Location
import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import app.simple.positional.R
import app.simple.positional.constants.CompassBloom.compassBloomRes
import app.simple.positional.constants.HtmlPage
import app.simple.positional.ui.compose.BloomSkinDialog
import app.simple.positional.ui.compose.PhysicalPropertiesDialog
import app.simple.positional.extensions.panel.SensorPanel
import app.simple.positional.math.MathExtensions.round
import app.simple.positional.preferences.CompassPreferences
import app.simple.positional.sensors.HeadingSensor
import app.simple.positional.ui.compose.CompassScreen
import app.simple.positional.ui.compose.CompassState
import app.simple.positional.ui.compose.InfoRow
import app.simple.positional.ui.compose.axisRows
import app.simple.positional.ui.compose.PanelMenus
import app.simple.positional.viewmodels.viewmodel.LocationViewModel
import kotlinx.coroutines.flow.filterNotNull

class Compass(activity: ComponentActivity) : SensorPanel(activity), HeadingSensor.Listener {
    private val state = CompassState()
    private val sensor: HeadingSensor
    private var lastReadings = 0L

    init {
        sensor = HeadingSensor(activity, this)
        lifecycle.addObserver(sensor)
        applyPreferences()
    }

    @Composable
    override fun Content(topInset: Dp) {
        CompassScreen(state, topInset, onCalibrate = { showCalibration() }, onMenu = ::showMenu)
    }

    private fun showMenu() = menuSheet.show {
        PanelMenus.compass(
            onBloomSkins = {
                dialogs.show { BloomSkinDialog(CompassPreferences.getFlowerBloomTheme(), dialogs::dismiss) { CompassPreferences.setFlowerBloom(it) } }
            },
            onPhysicalProperties = {
                dialogs.show { PhysicalPropertiesDialog(dialogs::dismiss) { openPage(HtmlPage.PHYSICAL_PROPERTIES) } }
            },
        )
    }

    init {
        if (!sensor.isAvailable) showSensorError()
        observe(ViewModelProvider(activity)[LocationViewModel::class.java].location.filterNotNull(), ::showField)
    }

    override fun onHeading(azimuth: Float) = state.onHeading(azimuth)

    override fun onAccuracy(type: Int, accuracy: Int) {
        when (type) {
            Sensor.TYPE_ACCELEROMETER -> state.accelerometerAccuracy = accuracy
            Sensor.TYPE_MAGNETIC_FIELD -> state.magnetometerAccuracy = accuracy
        }
        checkCalibration(type, accuracy)
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
    }
}
