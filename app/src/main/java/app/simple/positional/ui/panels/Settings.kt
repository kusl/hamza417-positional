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
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import app.simple.positional.BuildConfig
import app.simple.positional.ui.navigation.Routes
import app.simple.positional.constants.HtmlPage
import app.simple.positional.extensions.panel.PanelController
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.ui.compose.SettingsActions
import app.simple.positional.ui.compose.SettingsLabels
import app.simple.positional.ui.compose.SettingsScreen
import app.simple.positional.ui.compose.SettingsState
import app.simple.positional.util.AppIcons
import app.simple.positional.util.PermissionUtils

class Settings(activity: ComponentActivity) : PanelController(activity), SettingsActions {
    private var state by mutableStateOf(SettingsState())
    private var appSettingsOpened = false
    private val permissionLauncher = register(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { !it } && !appSettingsOpened) {
            appSettingsOpened = true
            startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            })
        }
        refresh()
    }

    @Composable
    override fun Content(topInset: Dp) {
        SettingsScreen(state, this, topInset)
    }

    override fun onResume() {
        appSettingsOpened = false
        refresh()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == MainPreferences.accentColor) {
            activity.recreate()
        } else {
            refresh()
        }
    }

    private fun refresh() {
        state = SettingsState(
            hasLocationPermission = PermissionUtils.checkPermission(activity),
            themeMode = if (MainPreferences.isDayNightOn()) SettingsLabels.THEME_AUTO else MainPreferences.getTheme(),
            isMetric = MainPreferences.isMetric(),
            coordinatesFormat = MainPreferences.getCoordinatesFormat(),
            locationProvider = MainPreferences.getLocationProvider(),
            language = MainPreferences.getAppLanguage() ?: SettingsLabels.LANGUAGE_DEFAULT,
            isCustomLocation = MainPreferences.isCustomCoordinate(),
            keepScreenOn = MainPreferences.isScreenOn(),
            skipSplashScreen = MainPreferences.getSkipSplashScreen(),
            iconIndex = AppIcons.current(activity),
            cornerRadius = MainPreferences.getCornerRadius() * 5,
            versionName = BuildConfig.VERSION_NAME,
        )
    }

    override fun requestPermission() {
        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
    }

    override fun setThemeMode(mode: Int) {
        MainPreferences.setDayNight(mode == SettingsLabels.THEME_AUTO)
        if (mode != SettingsLabels.THEME_AUTO) MainPreferences.setTheme(mode)
        activity.recreate()
    }

    override fun openAccent() = navigate(Routes.ACCENT)

    override fun setIcon(index: Int) {
        AppIcons.select(activity, index)
        refresh()
    }

    override fun setCornerRadius(value: Int) {
        if (value == MainPreferences.getCornerRadius() * 5) return
        MainPreferences.setCornerRadius(value)
        activity.recreate()
    }

    override fun setSkipSplashScreen(enabled: Boolean) = MainPreferences.setSkipSplashScreen(enabled)

    override fun openArts() = navigate(Routes.ARTS)

    override fun setMetric(metric: Boolean) = MainPreferences.setUnit(metric)

    override fun setCoordinatesFormat(format: Int) = MainPreferences.setCoordinatesFormat(format)

    override fun setLocationProvider(provider: String) = MainPreferences.setLocationProvider(provider)

    override fun openCustomLocations() = navigate(Routes.CUSTOM_LOCATIONS)

    override fun setCustomLocation(enabled: Boolean) {
        if (enabled) openCustomLocations() else MainPreferences.setCustomCoordinates(false)
    }

    override fun setKeepScreenOn(enabled: Boolean) {
        MainPreferences.setScreenOn(enabled)
        if (enabled) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun setLanguage(code: String) {
        if (code == MainPreferences.getAppLanguage()) return
        MainPreferences.setAppLanguage(code)
        activity.recreate()
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