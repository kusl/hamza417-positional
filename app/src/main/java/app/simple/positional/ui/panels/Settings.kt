package app.simple.positional.ui.panels

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.net.toUri
import app.simple.positional.BuildConfig
import app.simple.positional.activities.subactivity.AccentColorsActivity
import app.simple.positional.activities.subactivity.ArtsActivity
import app.simple.positional.activities.subactivity.CustomLocationsActivity
import app.simple.positional.activities.subactivity.WebPageViewerActivity
import app.simple.positional.dialogs.settings.CoordinatesFormat
import app.simple.positional.dialogs.settings.Icons
import app.simple.positional.dialogs.settings.Locales
import app.simple.positional.dialogs.settings.LocationProvider
import app.simple.positional.dialogs.settings.RoundedCorners
import app.simple.positional.dialogs.settings.Theme
import app.simple.positional.dialogs.settings.Units
import app.simple.positional.extensions.fragment.ScopedFragment
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.ui.compose.PositionalTheme
import app.simple.positional.ui.compose.SettingsActions
import app.simple.positional.ui.compose.SettingsLabels
import app.simple.positional.ui.compose.SettingsScreen
import app.simple.positional.ui.compose.SettingsState
import app.simple.positional.util.PermissionUtils
import app.simple.positional.util.StatusBarHeight

class Settings : ScopedFragment(), SettingsActions {
    private var state by mutableStateOf(SettingsState())
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var appSettingsOpened = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.any { !it } && !appSettingsOpened) {
                appSettingsOpened = true
                startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                })
            }
            refresh()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val topInset = StatusBarHeight.getStatusBarHeight(resources)
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                PositionalTheme {
                    SettingsScreen(state, this@Settings, topInset)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appSettingsOpened = false
        refresh()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == MainPreferences.accentColor) {
            requireActivity().recreate()
        } else {
            refresh()
        }
    }

    private fun refresh() {
        state = SettingsState(
            hasLocationPermission = PermissionUtils.checkPermission(requireContext()),
            themeMode = if (MainPreferences.isDayNightOn()) SettingsLabels.THEME_AUTO else AppCompatDelegate.getDefaultNightMode(),
            isMetric = MainPreferences.isMetric(),
            coordinatesFormat = MainPreferences.getCoordinatesFormat(),
            locationProvider = MainPreferences.getLocationProvider(),
            language = MainPreferences.getAppLanguage() ?: SettingsLabels.LANGUAGE_DEFAULT,
            isCustomLocation = MainPreferences.isCustomCoordinate(),
            keepScreenOn = MainPreferences.isScreenOn(),
            skipSplashScreen = MainPreferences.getSkipSplashScreen(),
            versionName = BuildConfig.VERSION_NAME,
        )
    }

    override fun requestPermission() {
        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
    }

    override fun openTheme() = Theme().show(parentFragmentManager, "theme")

    override fun openAccent() = startActivity(Intent(requireContext(), AccentColorsActivity::class.java))

    override fun openIcons() = Icons.newInstance().show(childFragmentManager, "app_icons")

    override fun openCorners() = RoundedCorners.newInstance().show(parentFragmentManager, "rounded_corners")

    override fun setSkipSplashScreen(enabled: Boolean) = MainPreferences.setSkipSplashScreen(enabled)

    override fun openArts() = startActivity(Intent(requireContext(), ArtsActivity::class.java))

    override fun openUnits() = Units().show(parentFragmentManager, "units")

    override fun openCoordinatesFormat() = CoordinatesFormat.newInstance().show(childFragmentManager, "coordinates_format")

    override fun openLocationProvider() = LocationProvider.newInstance().show(childFragmentManager, "location_provider")

    override fun openCustomLocations() = startActivity(Intent(requireContext(), CustomLocationsActivity::class.java))

    override fun setCustomLocation(enabled: Boolean) {
        if (enabled) openCustomLocations() else MainPreferences.setCustomCoordinates(false)
    }

    override fun setKeepScreenOn(enabled: Boolean) {
        MainPreferences.setScreenOn(enabled)
        if (enabled) {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun openLanguages() = Locales().newInstance().show(childFragmentManager, "locales")

    override fun openLegal(page: Int) {
        startActivity(Intent(requireContext(), WebPageViewerActivity::class.java).putExtra("source", getString(page)))
    }

    override fun openReleases() = openUrl("$REPOSITORY_URL/releases")

    override fun openRepository() = openUrl(REPOSITORY_URL)

    override fun openIssues() = openUrl("$REPOSITORY_URL/issues")

    private fun openUrl(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
    }

    companion object {
        private const val REPOSITORY_URL = "https://github.com/kusl/hamza417-positional"

        fun newInstance() = Settings()
    }
}