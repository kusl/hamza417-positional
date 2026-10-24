package app.simple.positional.activities.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import app.simple.positional.extensions.panel.PanelController
import app.simple.positional.R
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.simple.positional.ui.navigation.AccentRoute
import app.simple.positional.ui.navigation.AppNavigator
import app.simple.positional.ui.navigation.ArtsRoute
import app.simple.positional.ui.navigation.CustomLocationsRoute
import app.simple.positional.ui.navigation.DirectionsRoute
import app.simple.positional.ui.navigation.MapSearchRoute
import app.simple.positional.ui.navigation.MeasuresRoute
import app.simple.positional.ui.navigation.Routes
import app.simple.positional.ui.navigation.SearchResult
import app.simple.positional.ui.navigation.TimeZoneRoute
import app.simple.positional.ui.navigation.TrailsRoute
import app.simple.positional.ui.navigation.WebRoute
import app.simple.positional.callbacks.BottomBarHost
import app.simple.positional.extensions.activity.BaseActivity
import app.simple.positional.preferences.FragmentPreferences
import app.simple.positional.preferences.MainPreferences
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
import app.simple.positional.viewmodels.viewmodel.LocationViewModel
import com.google.android.gms.maps.MapsInitializer

class MainActivity : BaseActivity(), BottomBarHost, AppNavigator {
    private var panel by mutableStateOf(PanelCatalog.LOCATION)
    private var barVisible by mutableStateOf(true)
    private var navController: NavHostController? = null
    private var searchResult: ((SearchResult) -> Unit)? = null
    private val location: LocationViewModel by viewModels()

    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) {
            showPrompt()
            location.restart()
        } else {
            Toast.makeText(this, R.string.no_location_permission_alert, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LATEST) { }
        panel = PanelCatalog.resolve(FragmentPreferences.getCurrentTag())
        setPositionalContent { topInset -> AppNavHost(topInset) }
        checkLocationPermission()
        MainPreferences.setLaunchCount(MainPreferences.getLaunchCount() + 1)
    }

    private fun select(tag: String) {
        panel = tag
        barVisible = true
        FragmentPreferences.setCurrentTag(tag)
    }

    @Composable
    private fun AppNavHost(topInset: Dp) {
        val nav = rememberNavController()
        navController = nav
        val controller = remember(panel) { createPanel(panel, this) }
        DisposableEffect(controller) { onDispose { controller.destroy() } }
        NavHost(nav, startDestination = Routes.PANELS, modifier = Modifier.fillMaxSize().background(colorResource(R.color.mainBackground))) {
            composable(Routes.PANELS) {
                Box(Modifier.fillMaxSize()) {
                    controller.Host(topInset)
                    PanelSwitcher(panel, barVisible, ::select, Modifier.align(Alignment.BottomEnd))
                }
            }
            composable(Routes.ACCENT) { AccentRoute(this@MainActivity, topInset) }
            composable(Routes.ARTS) { ArtsRoute() }
            composable(Routes.TIMEZONE) { TimeZoneRoute(this@MainActivity, topInset) }
            composable(Routes.MEASURES) { MeasuresRoute(this@MainActivity, topInset) }
            composable(Routes.TRAILS) { TrailsRoute(this@MainActivity, topInset) }
            composable(Routes.DIRECTIONS) { DirectionsRoute(this@MainActivity, topInset) }
            composable(Routes.CUSTOM_LOCATIONS) { CustomLocationsRoute(this@MainActivity, topInset) }
            composable(Routes.MAP_SEARCH) { MapSearchRoute(this@MainActivity) }
            composable(Routes.WEB, arguments = listOf(navArgument(Routes.PAGE_ARGUMENT) { type = NavType.StringType })) { entry ->
                WebRoute(entry.arguments?.getString(Routes.PAGE_ARGUMENT), this@MainActivity)
            }
        }
    }

    override fun navigate(route: String) {
        navController?.navigate(route) { launchSingleTop = true }
    }

    override fun back() {
        navController?.popBackStack()
    }

    override fun search(onResult: (SearchResult) -> Unit) {
        searchResult = onResult
        navigate(Routes.MAP_SEARCH)
    }

    override fun deliver(result: SearchResult) {
        searchResult?.invoke(result)
        searchResult = null
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

    private companion object {
        val locationPermissions = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    }
}

private fun createPanel(tag: String, activity: ComponentActivity): PanelController = when (tag) {
    PanelCatalog.CLOCK -> Time(activity)
    PanelCatalog.COMPASS -> Compass(activity)
    PanelCatalog.DIRECTION -> Direction(activity)
    PanelCatalog.TRAIL -> Trail(activity)
    PanelCatalog.MEASURE -> Measure(activity)
    PanelCatalog.LEVEL -> Level(activity)
    PanelCatalog.SETTINGS -> Settings(activity)
    else -> GPS(activity)
}
