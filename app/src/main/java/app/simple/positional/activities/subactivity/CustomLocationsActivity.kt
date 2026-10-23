package app.simple.positional.activities.subactivity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import app.simple.positional.R
import app.simple.positional.constants.HtmlPage
import app.simple.positional.extensions.activity.BaseActivity
import app.simple.positional.model.Locations
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.ui.compose.CustomLocationActions
import app.simple.positional.ui.compose.CustomLocationScreen
import app.simple.positional.ui.compose.PositionalTheme
import app.simple.positional.util.StatusBarHeight
import app.simple.positional.util.TextViewUtils.capitalizeText
import app.simple.positional.viewmodels.viewmodel.CustomLocationViewModel

class CustomLocationsActivity : BaseActivity(), CustomLocationActions {
    private val viewModel: CustomLocationViewModel by viewModels()

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
        if (savedInstanceState == null && MainPreferences.isCustomCoordinate()) {
            val coordinates = MainPreferences.getCoordinates()
            viewModel.form.fill(MainPreferences.getAddress(), coordinates[0].toString(), coordinates[1].toString())
        }
        val topInset = StatusBarHeight.getStatusBarHeight(resources)
        setContentView(ComposeView(this).apply {
            setContent {
                PositionalTheme {
                    val locations by viewModel.locations.collectAsState()
                    CustomLocationScreen(
                        form = viewModel.form,
                        locations = locations,
                        topInset = with(LocalDensity.current) { topInset.toDp() },
                        actions = this@CustomLocationsActivity,
                    )
                }
            }
        })
    }

    override fun search() = searchLauncher.launch(Intent(this, MapSearchActivity::class.java))

    override fun save(latitude: Double, longitude: Double) = viewModel.save(newLocation(latitude, longitude))

    override fun setLocation(latitude: Double, longitude: Double, save: Boolean) {
        if (save) viewModel.save(newLocation(latitude, longitude))
        use(viewModel.form.address, latitude, longitude)
    }

    override fun select(location: Locations) =
        viewModel.form.fill(location.address, location.latitude.toString(), location.longitude.toString())

    override fun useLocation(location: Locations) = use(location.address, location.latitude, location.longitude)

    override fun delete(location: Locations) = viewModel.delete(location)

    override fun deleteAll() = viewModel.deleteAll()

    override fun help() = WebPageViewerActivity.open(this, HtmlPage.CUSTOM_COORDINATES_HELP)

    override fun failed() = Toast.makeText(this, R.string.failed, Toast.LENGTH_SHORT).show()

    private fun newLocation(latitude: Double, longitude: Double) = Locations().apply {
        address = viewModel.form.address.trim().ifEmpty { "----" }.capitalizeText()
        this.latitude = latitude
        this.longitude = longitude
        date = System.currentTimeMillis()
    }

    private fun use(address: String, latitude: Double, longitude: Double) {
        MainPreferences.setCustomCoordinates(true)
        MainPreferences.setLatitude(latitude.toFloat())
        MainPreferences.setLongitude(longitude.toFloat())
        MainPreferences.setAddress(address)
        finishAfterTransition()
    }
}
