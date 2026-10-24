package app.simple.positional.ui.panels

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.simple.positional.extensions.panel.SensorPanel
import app.simple.positional.math.Tilt
import app.simple.positional.sensors.GravitySensor
import app.simple.positional.ui.compose.LevelScreen
import app.simple.positional.viewmodels.viewmodel.LevelViewModel

class Level(activity: ComponentActivity) : SensorPanel(activity) {
    private var tilt by mutableStateOf(Tilt.zero)
    private val viewModel = ViewModelProvider(activity)[LevelViewModel::class.java]
    private val sensor = GravitySensor(activity) { tilt = it }

    init {
        lifecycle.addObserver(sensor)
        if (!sensor.isAvailable) showSensorError()
    }

    @Composable
    override fun Content(topInset: Dp) {
        val square by viewModel.square.collectAsStateWithLifecycle()
        LevelScreen({ tilt }, square, topInset, viewModel::toggleStyle)
    }
}
