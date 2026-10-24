package app.simple.positional.ui.panels

import android.hardware.Sensor
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.simple.positional.constants.HtmlPage
import app.simple.positional.extensions.panel.SensorPanel
import app.simple.positional.preferences.CompassPreferences
import app.simple.positional.sensors.HeadingSensor
import app.simple.positional.ui.compose.BloomSkinDialog
import app.simple.positional.ui.compose.CompassScreen
import app.simple.positional.ui.compose.CompassState
import app.simple.positional.ui.compose.PanelDialog
import app.simple.positional.ui.compose.PanelMenus
import app.simple.positional.ui.compose.PhysicalPropertiesDialog
import app.simple.positional.ui.compose.axisRows
import app.simple.positional.viewmodels.viewmodel.CompassSettings
import app.simple.positional.viewmodels.viewmodel.CompassViewModel

class Compass(activity: ComponentActivity) : SensorPanel(activity), HeadingSensor.Listener {
    private val state = CompassState()
    private val sensor = HeadingSensor(activity, this)
    private val viewModel = panelModel(CompassViewModel::class, ::CompassViewModel)
    private var lastReadings = 0L

    init {
        lifecycle.addObserver(sensor)
        bind(viewModel.settings, ::applySettings)
        if (!sensor.isAvailable) showSensorError()
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

    private fun applySettings(settings: CompassSettings) {
        sensor.gimbalLock = settings.gimbalLock
        state.physics = settings.physics
        state.directionCode = settings.directionCode
        state.bloom = settings.bloom
        state.configure(settings.inertia, settings.damping, settings.magnetic)
    }

    private companion object {
        const val READINGS_INTERVAL = 250L
    }
}
