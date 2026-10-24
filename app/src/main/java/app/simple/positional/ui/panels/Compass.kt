package app.simple.positional.ui.panels

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.simple.positional.viewmodels.viewmodel.CompassViewModel
import androidx.compose.ui.unit.Dp
import android.content.SharedPreferences
import android.hardware.Sensor
import android.os.SystemClock
import app.simple.positional.constants.CompassBloom.compassBloomRes
import app.simple.positional.constants.HtmlPage
import app.simple.positional.ui.compose.BloomSkinDialog
import app.simple.positional.ui.compose.PhysicalPropertiesDialog
import app.simple.positional.extensions.panel.SensorPanel
import app.simple.positional.preferences.CompassPreferences
import app.simple.positional.sensors.HeadingSensor
import app.simple.positional.ui.compose.CompassScreen
import app.simple.positional.ui.compose.CompassState
import app.simple.positional.ui.compose.axisRows
import app.simple.positional.ui.compose.PanelDialog
import app.simple.positional.ui.compose.PanelMenus

class Compass(activity: ComponentActivity) : SensorPanel(activity), HeadingSensor.Listener {
    private val state = CompassState()
    private val sensor: HeadingSensor
    private val viewModel = panelModel(CompassViewModel::class, ::CompassViewModel)
    private var lastReadings = 0L

    init {
        sensor = HeadingSensor(activity, this)
        lifecycle.addObserver(sensor)
        applyPreferences()
    }

    @Composable
    override fun Content(topInset: Dp) {
        val field by viewModel.field.collectAsStateWithLifecycle()
        CompassScreen(state, field, topInset, onCalibrate = { showCalibration() }, onMenu = ::showMenu)
    }

    private fun showMenu() = menuSheet.show {
        PanelMenus.compass(onBloomSkins = { dialogs.show(CompassDialog.Bloom) }, onPhysicalProperties = { dialogs.show(CompassDialog.Physics) })
    }

    @Composable
    override fun DialogContent(dialog: PanelDialog) {
        when (dialog) {
            CompassDialog.Bloom -> BloomSkinDialog(CompassPreferences.getFlowerBloomTheme(), dialogs::dismiss) { CompassPreferences.setFlowerBloom(it) }
            CompassDialog.Physics -> PhysicalPropertiesDialog(dialogs::dismiss) { openPage(HtmlPage.PHYSICAL_PROPERTIES) }
            else -> super.DialogContent(dialog)
        }
    }

    private enum class CompassDialog : PanelDialog { Bloom, Physics }

    init {
        if (!sensor.isAvailable) showSensorError()
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
