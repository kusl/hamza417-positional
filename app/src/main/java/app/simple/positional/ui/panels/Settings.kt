package app.simple.positional.ui.panels

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.Settings as AndroidSettings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.simple.positional.constants.HtmlPage
import app.simple.positional.extensions.panel.PanelController
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.ui.compose.SettingsActions
import app.simple.positional.ui.compose.SettingsScreen
import app.simple.positional.ui.navigation.Routes
import app.simple.positional.viewmodels.viewmodel.SettingsViewModel

class Settings(activity: ComponentActivity) : PanelController(activity), SettingsActions {
    private val viewModel = ViewModelProvider(activity)[SettingsViewModel::class.java]
    private var appSettingsOpened = false
    private val permissionLauncher = register(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { !it } && !appSettingsOpened) {
            appSettingsOpened = true
            startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            })
        }
        viewModel.refresh()
    }

    @Composable
    override fun Content(topInset: Dp) {
        val state by viewModel.state.collectAsStateWithLifecycle()
        SettingsScreen(state, this, topInset)
    }

    override fun onResume() {
        appSettingsOpened = false
        viewModel.refresh()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == MainPreferences.accentColor) activity.recreate()
    }

    override fun requestPermission() {
        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
    }

    override fun setThemeMode(mode: Int) {
        viewModel.setThemeMode(mode)
        activity.recreate()
    }

    override fun openAccent() = navigate(Routes.ACCENT)

    override fun setIcon(index: Int) = viewModel.setIcon(index)

    override fun setCornerRadius(value: Int) {
        if (viewModel.setCornerRadius(value)) activity.recreate()
    }

    override fun setSkipSplashScreen(enabled: Boolean) = viewModel.setSkipSplashScreen(enabled)

    override fun openArts() = navigate(Routes.ARTS)

    override fun setMetric(metric: Boolean) = viewModel.setMetric(metric)

    override fun setCoordinatesFormat(format: Int) = viewModel.setCoordinatesFormat(format)

    override fun setLocationProvider(provider: String) = viewModel.setLocationProvider(provider)

    override fun setMapSource(source: Int) = viewModel.setMapSource(source)

    override fun openCustomLocations() = navigate(Routes.CUSTOM_LOCATIONS)

    override fun setCustomLocation(enabled: Boolean) {
        if (enabled) openCustomLocations() else viewModel.disableCustomLocation()
    }

    override fun setKeepScreenOn(enabled: Boolean) {
        viewModel.setKeepScreenOn(enabled)
        if (enabled) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun setLanguage(code: String) {
        if (viewModel.setLanguage(code)) activity.recreate()
    }

    override fun openLegal(page: Int) {
        HtmlPage.forTitle(page)?.let(::openPage)
    }

    override fun openReleases() = openUrl("$REPOSITORY_URL/releases")

    override fun openRepository() = openUrl(REPOSITORY_URL)

    override fun openIssues() = openUrl("$REPOSITORY_URL/issues")

    private fun openUrl(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
    }

    companion object {
        private const val REPOSITORY_URL = "https://github.com/kusl/hamza417-positional"
    }
}
