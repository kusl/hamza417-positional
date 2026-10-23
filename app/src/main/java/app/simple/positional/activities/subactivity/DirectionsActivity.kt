package app.simple.positional.activities.subactivity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import app.simple.positional.R
import app.simple.positional.extensions.activity.BaseActivity
import app.simple.positional.model.DirectionModel
import app.simple.positional.preferences.DirectionPreferences
import app.simple.positional.preferences.GPSPreferences
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.ui.compose.DirectionsActions
import app.simple.positional.ui.compose.DirectionsScreen
import app.simple.positional.ui.compose.setPositionalContent
import app.simple.positional.viewmodels.viewmodel.DirectionsViewModel

class DirectionsActivity : BaseActivity(), DirectionsActions {
    private val viewModel: DirectionsViewModel by viewModels()

    private val searchLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode == RESULT_OK && data != null) {
            val form = viewModel.form
            form.fill(
                address = data.getStringExtra("address").orEmpty().ifEmpty { form.address },
                latitude = data.getDoubleExtra("latitude", 0.0).toString(),
                longitude = data.getDoubleExtra("longitude", 0.0).toString(),
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setPositionalContent { topInset ->
            val directions by viewModel.directions.collectAsState()
            DirectionsScreen(directions, viewModel.editing != null, viewModel.form, topInset, this)
        }
    }

    override fun create() {
        val target = DirectionPreferences.getTargetCoordinates()
        viewModel.open(DirectionPreferences.getTargetLabel(), target[0].toDouble(), target[1].toDouble())
    }

    override fun createFromTarget() {
        val target = GPSPreferences.getTargetMarkerCoordinates()
        viewModel.open(null, target[0].toDouble(), target[1].toDouble())
    }

    override fun set(direction: DirectionModel) = select(direction, useMapsTarget = false)

    override fun edit(direction: DirectionModel) =
        viewModel.open(direction.name, direction.latitude, direction.longitude, direction.dateAdded)

    override fun useAsTarget(direction: DirectionModel) {
        val last = MainPreferences.getLastCoordinates()
        GPSPreferences.setTargetMarker(true)
        GPSPreferences.setTargetMarkerLatitude(direction.latitude.toFloat())
        GPSPreferences.setTargetMarkerLongitude(direction.longitude.toFloat())
        GPSPreferences.setTargetMarkerStartLatitude(last[0])
        GPSPreferences.setTargetMarkerStartLongitude(last[1])
        select(direction, useMapsTarget = true)
    }

    override fun navigate(direction: DirectionModel) {
        val coordinates = "${direction.latitude},${direction.longitude}"
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, "google.navigation:q=$coordinates&mode=d".toUri()).setPackage(GOOGLE_MAPS))
        }.recoverCatching {
            startActivity(Intent(Intent.ACTION_VIEW, "geo:$coordinates?q=$coordinates".toUri()))
        }.onFailure {
            Toast.makeText(this, R.string.failed, Toast.LENGTH_SHORT).show()
        }
    }

    override fun delete(direction: DirectionModel) = viewModel.delete(direction)

    override fun search() = searchLauncher.launch(Intent(this, MapSearchActivity::class.java))

    override fun save(latitude: Double, longitude: Double) = viewModel.save(latitude, longitude)

    override fun dismissEditor() = viewModel.close()

    private fun select(direction: DirectionModel, useMapsTarget: Boolean) {
        DirectionPreferences.setTargetLatitude(direction.latitude.toFloat())
        DirectionPreferences.setTargetLongitude(direction.longitude.toFloat())
        DirectionPreferences.setTargetLabel(direction.name.orEmpty())
        DirectionPreferences.setUseMapsTarget(useMapsTarget)
        finish()
    }

    private companion object {
        const val GOOGLE_MAPS = "com.google.android.apps.maps"
    }
}
