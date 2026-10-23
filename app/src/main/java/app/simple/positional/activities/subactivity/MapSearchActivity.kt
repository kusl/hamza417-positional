package app.simple.positional.activities.subactivity

import android.content.Intent
import android.content.res.Configuration
import android.location.Address
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import app.simple.positional.R
import app.simple.positional.extensions.activity.BaseActivity
import app.simple.positional.ui.compose.CoordinateInput
import app.simple.positional.ui.compose.MapSearchActions
import app.simple.positional.ui.compose.MapSearchScreen
import app.simple.positional.ui.compose.PositionalTheme
import app.simple.positional.viewmodels.viewmodel.MapSearchViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions

class MapSearchActivity : BaseActivity(), MapSearchActions {
    private val viewModel: MapSearchViewModel by viewModels()
    private lateinit var mapView: MapView
    private var map: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapView = MapView(this)
        mapView.onCreate(savedInstanceState?.getBundle(MAP_STATE))
        lifecycle.addObserver(MapLifecycle(mapView))
        mapView.getMapAsync { onMapReady(it, savedInstanceState == null) }
        setContentView(ComposeView(this).apply {
            setContent {
                PositionalTheme {
                    val suggestions by viewModel.suggestions.collectAsState()
                    MapSearchScreen(viewModel.form, suggestions, mapView, this@MapSearchActivity)
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle(MAP_STATE, Bundle().also(mapView::onSaveInstanceState))
    }

    private fun onMapReady(googleMap: GoogleMap, moveToLast: Boolean) {
        map = googleMap
        googleMap.uiSettings.isMapToolbarEnabled = false
        googleMap.uiSettings.isCompassEnabled = false
        val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        runCatching {
            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, if (night) R.raw.maps_dark_labelled else R.raw.maps_light_labelled))
        }
        if (moveToLast) {
            CoordinateInput.parse(viewModel.form.latitude, viewModel.form.longitude)?.let { (lat, lon) ->
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), ZOOM))
            }
        }
        googleMap.setOnCameraIdleListener {
            googleMap.cameraPosition.target.let { viewModel.onCameraIdle(it.latitude, it.longitude) }
        }
        googleMap.setOnMapClickListener { googleMap.animateCamera(CameraUpdateFactory.newLatLng(it)) }
    }

    override fun addressChanged(text: String) = viewModel.onAddressChange(text)

    override fun pick(address: Address) {
        viewModel.pick(address)
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(address.latitude, address.longitude), ZOOM))
    }

    override fun cancel() {
        setResult(RESULT_CANCELED)
        finish()
    }

    override fun confirm() {
        val form = viewModel.form
        val coordinates = CoordinateInput.parse(form.latitude, form.longitude)
        if (coordinates == null) {
            Toast.makeText(this, R.string.failed, Toast.LENGTH_SHORT).show()
            return
        }
        setResult(RESULT_OK, Intent()
            .putExtra("latitude", coordinates.first)
            .putExtra("longitude", coordinates.second)
            .putExtra("address", form.address.trim()))
        finish()
    }

    private class MapLifecycle(private val mapView: MapView) : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) = mapView.onStart()
        override fun onResume(owner: LifecycleOwner) = mapView.onResume()
        override fun onPause(owner: LifecycleOwner) = mapView.onPause()
        override fun onStop(owner: LifecycleOwner) = mapView.onStop()
        override fun onDestroy(owner: LifecycleOwner) = mapView.onDestroy()
    }

    companion object {
        private const val MAP_STATE = "map_state"
        private const val ZOOM = 15F
    }
}
