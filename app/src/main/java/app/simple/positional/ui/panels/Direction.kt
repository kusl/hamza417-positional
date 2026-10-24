package app.simple.positional.ui.panels

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.simple.positional.extensions.panel.SensorPanel
import app.simple.positional.sensors.HeadingSensor
import app.simple.positional.ui.compose.DirectionPanel
import app.simple.positional.ui.compose.DirectionState
import app.simple.positional.ui.compose.PanelMenus
import app.simple.positional.ui.navigation.Routes
import app.simple.positional.viewmodels.viewmodel.DirectionViewModel

class Direction(activity: ComponentActivity) : SensorPanel(activity), HeadingSensor.Listener {
    private val state = DirectionState()
    private val sensor = HeadingSensor(activity, this)
    private val viewModel = panelModel(DirectionViewModel::class, ::DirectionViewModel)

    init {
        bind(viewModel.gimbalLock) { sensor.gimbalLock = it }
        lifecycle.addObserver(sensor)
        if (!sensor.isAvailable) showSensorError()
    }

    @Composable
    override fun Content(topInset: Dp) {
        val info by viewModel.info.collectAsStateWithLifecycle()
        SideEffect { state.bearing = info.bearing }
        DirectionPanel(
            state,
            info,
            topInset,
            onCalibrate = { showCalibration() },
            onTarget = ::openTargets,
            onMenu = { menuSheet.show { PanelMenus.direction(::openTargets) } },
        )
    }

    private fun openTargets() = navigate(Routes.DIRECTIONS)

    override fun onHeading(azimuth: Float) = state.onHeading(azimuth)

    override fun onAccuracy(type: Int, accuracy: Int) = checkCalibration(type, accuracy)
}
