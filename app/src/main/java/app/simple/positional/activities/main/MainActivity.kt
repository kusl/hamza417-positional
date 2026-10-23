package app.simple.positional.activities.main

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.core.content.ContextCompat
import androidx.fragment.compose.AndroidFragment
import app.simple.positional.R
import app.simple.positional.callbacks.BottomBarHost
import app.simple.positional.extensions.activity.BaseActivity
import app.simple.positional.preferences.FragmentPreferences
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.services.FusedLocationService
import app.simple.positional.services.LocationService
import app.simple.positional.singleton.SharedPreferences.getSharedPreferences
import app.simple.positional.ui.compose.PanelCatalog
import app.simple.positional.ui.compose.PanelSwitcher
import app.simple.positional.ui.compose.setPositionalContent
import app.simple.positional.ui.panels.Compass
import app.simple.positional.ui.panels.Direction
import app.simple.positional.ui.panels.GPS
import app.simple.positional.ui.panels.Level
import app.simple.positional.ui.panels.Measure
import app.simple.positional.ui.panels.Settings
import app.simple.positional.ui.panels.Time
import app.simple.positional.ui.panels.Trail
import app.simple.positional.util.LocationExtension.getLocationStatus
import app.simple.positional.util.LocationPrompt.displayLocationSettingsRequest
import com.google.android.gms.maps.MapsInitializer

class MainActivity : BaseActivity(), BottomBarHost, SharedPreferences.OnSharedPreferenceChangeListener {
    private var panel by mutableStateOf(PanelCatalog.LOCATION)
    private var barVisible by mutableStateOf(true)

    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) {
            showPrompt()
            startLocation()
        } else {
            Toast.makeText(this, R.string.no_location_permission_alert, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LATEST) { }
        panel = PanelCatalog.resolve(FragmentPreferences.getCurrentTag())
        setPositionalContent {
            Box(Modifier.fillMaxSize().background(colorResource(R.color.mainBackground))) {
                key(panel) { PanelFragment(panel) }
                PanelSwitcher(panel, barVisible, ::select, Modifier.align(Alignment.BottomEnd))
            }
        }
        checkLocationPermission()
        MainPreferences.setLaunchCount(MainPreferences.getLaunchCount() + 1)
    }

    override fun onResume() {
        super.onResume()
        getSharedPreferences().registerOnSharedPreferenceChangeListener(this)
        startLocation()
    }

    override fun onPause() {
        super.onPause()
        getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this)
        stopLocation()
    }

    private fun select(tag: String) {
        panel = tag
        barVisible = true
        FragmentPreferences.setCurrentTag(tag)
    }

    override fun showBottomBar(visible: Boolean) {
        barVisible = visible
    }

    private fun hasLocationPermission() = locationPermissions.any {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkLocationPermission() {
        when {
            hasLocationPermission() -> showPrompt()
            MainPreferences.getShowPermissionDialog() -> permissions.launch(locationPermissions)
            else -> Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_LONG).show()
        }
    }

    private fun showPrompt() {
        if (!getLocationStatus(this)) displayLocationSettingsRequest(this)
    }

    private fun locationService(provider: String = MainPreferences.getLocationProvider()) =
        if (provider == FUSED) FusedLocationService::class.java else LocationService::class.java

    private fun startLocation() {
        runCatching { startService(Intent(applicationContext, locationService())) }
    }

    private fun stopLocation() {
        runCatching { stopService(Intent(applicationContext, locationService())) }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key != MainPreferences.locationProvider) return
        val current = MainPreferences.getLocationProvider()
        runCatching {
            stopService(Intent(applicationContext, locationService(if (current == FUSED) ANDROID else FUSED)))
            startService(Intent(applicationContext, locationService(current)))
        }.onFailure { Toast.makeText(applicationContext, it.message, Toast.LENGTH_SHORT).show() }
    }

    private companion object {
        const val FUSED = "fused"
        const val ANDROID = "android"
        val locationPermissions = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    }
}

@Composable
private fun PanelFragment(tag: String) {
    val modifier = Modifier.fillMaxSize()
    when (tag) {
        PanelCatalog.CLOCK -> AndroidFragment<Time>(modifier)
        PanelCatalog.COMPASS -> AndroidFragment<Compass>(modifier)
        PanelCatalog.DIRECTION -> AndroidFragment<Direction>(modifier)
        PanelCatalog.TRAIL -> AndroidFragment<Trail>(modifier)
        PanelCatalog.MEASURE -> AndroidFragment<Measure>(modifier)
        PanelCatalog.LEVEL -> AndroidFragment<Level>(modifier)
        PanelCatalog.SETTINGS -> AndroidFragment<Settings>(modifier)
        else -> AndroidFragment<GPS>(modifier)
    }
}
