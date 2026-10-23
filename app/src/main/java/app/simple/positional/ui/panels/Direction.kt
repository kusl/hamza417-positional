package app.simple.positional.ui.panels

import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import app.simple.positional.R
import app.simple.positional.activities.subactivity.DirectionsActivity
import app.simple.positional.extensions.fragment.SensorPanel
import app.simple.positional.preferences.DirectionPreferences
import app.simple.positional.preferences.GPSPreferences
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.sensors.HeadingSensor
import app.simple.positional.ui.compose.DirectionPanel
import app.simple.positional.ui.compose.DirectionState
import app.simple.positional.ui.compose.InfoRow
import app.simple.positional.ui.compose.positionalComposeView
import app.simple.positional.ui.compose.MenuSheetState
import app.simple.positional.ui.compose.PanelMenus
import app.simple.positional.util.DMSConverter
import app.simple.positional.util.DistanceText
import app.simple.positional.util.LocationExtension
import app.simple.positional.viewmodels.viewmodel.LocationViewModel
import com.google.android.gms.maps.model.LatLng

class Direction : SensorPanel(), HeadingSensor.Listener {
    private val state = DirectionState()
    private val menuSheet = MenuSheetState()
    private lateinit var sensor: HeadingSensor
    private var target = LatLng(0.0, 0.0)
    private var location: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensor = HeadingSensor(requireActivity(), this)
        sensor.gimbalLock = DirectionPreferences.isGimbalLock()
        lifecycle.addObserver(sensor)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        positionalComposeView(menuSheet, dialogs) { top ->
            DirectionPanel(
                state,
                top,
                onCalibrate = { showCalibration() },
                onTarget = ::openTargets,
                onMenu = { menuSheet.show { PanelMenus.direction(::openTargets) } },
            )
        }

    private fun openTargets() = startActivity(Intent(requireActivity(), DirectionsActivity::class.java))

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!sensor.isAvailable) showSensorError()
        loadTarget()
        ViewModelProvider(requireActivity())[LocationViewModel::class.java].getLocation().observe(viewLifecycleOwner) {
            location = it
            updateTarget()
        }
    }

    override fun onHeading(azimuth: Float) = state.onHeading(azimuth)

    override fun onAccuracy(type: Int, accuracy: Int) = checkCalibration(accuracy)

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
        val formatted = DMSConverter.getFormattedCoordinates(target, requireContext())
        state.coordinates = listOf(
            InfoRow(getString(R.string.gps_latitude), formatted[0]),
            InfoRow(getString(R.string.gps_longitude), formatted[1]),
        )
        updateTarget()
    }

    private fun updateTarget() {
        val current = location ?: return
        state.bearing = LocationExtension.calculateBearingAngle(current.latitude, current.longitude, target.latitude, target.longitude).toFloat()
        state.displacement = DistanceText.format(requireContext(), LocationExtension.measureDisplacement(arrayOf(LatLng(current.latitude, current.longitude), target)).toDouble(), MainPreferences.isMetric())
    }

    companion object {
        fun newInstance() = Direction()
    }
}
