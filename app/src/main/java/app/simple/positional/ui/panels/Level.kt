package app.simple.positional.ui.panels

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.simple.positional.extensions.panel.SensorPanel
import app.simple.positional.math.Tilt
import app.simple.positional.preferences.LevelPreferences
import app.simple.positional.sensors.GravitySensor
import app.simple.positional.ui.compose.LevelScreen

class Level(activity: ComponentActivity) : SensorPanel(activity) {
    private var tilt by mutableStateOf(Tilt.zero)
    private var square by mutableStateOf(false)
    private val sensor: GravitySensor

    init {
        sensor = GravitySensor(activity) { tilt = it }
        lifecycle.addObserver(sensor)
        square = LevelPreferences.isSquareStyle()
    }

    @Composable
    override fun Content(topInset: Dp) {
        LevelScreen({ tilt }, square, topInset) { LevelPreferences.setSquareStyle(!square) }
    }

    init {
        if (!sensor.isAvailable) showSensorError()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == LevelPreferences.isSquareStyle) square = LevelPreferences.isSquareStyle()
    }

}
