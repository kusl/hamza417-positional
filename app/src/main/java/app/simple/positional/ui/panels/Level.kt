package app.simple.positional.ui.panels

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.simple.positional.extensions.fragment.SensorPanel
import app.simple.positional.math.Tilt
import app.simple.positional.preferences.LevelPreferences
import app.simple.positional.sensors.GravitySensor
import app.simple.positional.ui.compose.LevelScreen
import app.simple.positional.ui.compose.positionalComposeView

class Level : SensorPanel() {
    private var tilt by mutableStateOf(Tilt.zero)
    private var square by mutableStateOf(false)
    private lateinit var sensor: GravitySensor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensor = GravitySensor(requireActivity()) { tilt = it }
        lifecycle.addObserver(sensor)
        square = LevelPreferences.isSquareStyle()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        positionalComposeView(dialog = dialogs) { top ->
            LevelScreen({ tilt }, square, top) { LevelPreferences.setSquareStyle(!square) }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!sensor.isAvailable) showSensorError()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == LevelPreferences.isSquareStyle) square = LevelPreferences.isSquareStyle()
    }

    companion object {
        fun newInstance() = Level()
    }
}
