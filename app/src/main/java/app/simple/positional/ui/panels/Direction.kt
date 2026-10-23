package app.simple.positional.ui.panels

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import android.content.SharedPreferences
import android.location.Location
import androidx.lifecycle.ViewModelProvider
import app.simple.positional.R
import app.simple.positional.ui.navigation.Routes
import app.simple.positional.extensions.panel.SensorPanel
import app.simple.positional.preferences.DirectionPreferences
import app.simple.positional.preferences.GPSPreferences
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.sensors.HeadingSensor
import app.simple.positional.ui.compose.DirectionPanel
import app.simple.positional.ui.compose.DirectionState
import app.simple.positional.ui.compose.InfoRow
import app.simple.positional.ui.compose.PanelMenus
import app.simple.positional.util.DMSConverter
import app.simple.positional.util.DistanceText
import app.simple.positional.util.LocationExtension
import app.simple.positional.viewmodels.viewmodel.LocationViewModel
import com.google.android.gms.maps.model.LatLng

class Direction(activity: ComponentActivity) : SensorPanel(activity), HeadingSensor.Listener {
    private val state = DirectionState()
    private val sensor: HeadingSensor
    private var target = LatLng(0.0, 0.0)
    private var location: Location? = null

    init {
        sensor = HeadingSensor(activity, this)
        sensor.gimbalLock = DirectionPreferences.isGimbalLock()
        lifecycle.addObserver(sensor)
    }

    @Composable
    override fun Content(topInset: Dp) {
        DirectionPanel(
            state,
            topInset,
            onCalibrate = { showCalibration() },
            onTarget = ::openTargets,
            onMenu = { menuSheet.show { PanelMenus.direction(::openTargets) } },
        )
    }

    private fun openTargets() = navigate(Routes.DIRECTIONS)

    init {
        if (!sensor.isAvailable) showSensorError()
        loadTarget()
        ViewModelProvider(activity)[LocationViewModel::class.java].getLocation().observe(owner) {
            location = it
            updateTarget()
        }
    }

    override fun onHeading(azimuth: Float) = state.onHeading(azimuth)

    override fun onAccuracy(type: Int, accuracy: Int) = checkCalibration(type, accuracy)

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            DirectionPreferences.directionLatitude,
            DirectionPreferences.directionLongitude,
            DirectionPreferences.directionLabel,
            DirectionPreferences.useMapsTarget -> loadTarget()
            DirectionPreferences.directionGimbalLock -> sensor.gimbalLock = DirectionPreferences.isGimbalLock()
        }
    }

    private fun usesMapsTarget() = DirectionPreferences.isUsingMapsTarget() && GPSPreferences.isTargetMarkerSet()

    private fun loadTarget() {
        val coordinates = if (usesMapsTarget()) GPSPreferences.getTargetMarkerCoordinates() else DirectionPreferences.getTargetCoordinates()
        target = LatLng(coordinates[0].toDouble(), coordinates[1].toDouble())
        state.target = if (usesMapsTarget()) getString(R.string.using_maps_target) else DirectionPreferences.getTargetLabel() ?: getString(R.string.not_available)
        val formatted = DMSConverter.getFormattedCoordinates(target, activity)
        state.coordinates = listOf(
            InfoRow(getString(R.string.gps_latitude), formatted[0]),
            InfoRow(getString(R.string.gps_longitude), formatted[1]),
        )
        updateTarget()
    }

    private fun updateTarget() {
        val current = location ?: return
        state.bearing = LocationExtension.calculateBearingAngle(current.latitude, current.longitude, target.latitude, target.longitude).toFloat()
        state.displacement = DistanceText.format(activity, LocationExtension.measureDisplacement(arrayOf(LatLng(current.latitude, current.longitude), target)).toDouble(), MainPreferences.isMetric())
    }

}
