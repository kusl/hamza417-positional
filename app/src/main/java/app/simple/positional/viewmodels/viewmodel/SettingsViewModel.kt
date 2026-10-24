package app.simple.positional.viewmodels.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.simple.positional.BuildConfig
import app.simple.positional.preferences.MainPreferences
import app.simple.positional.singleton.SharedPreferences
import app.simple.positional.ui.compose.SettingsLabels
import app.simple.positional.ui.compose.SettingsState
import app.simple.positional.util.AppIcons
import app.simple.positional.util.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val refreshes = MutableStateFlow(0)

    val state: StateFlow<SettingsState> = combine(SharedPreferences.changes(*keys), refreshes) { _, _ -> read() }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), read())

    fun refresh() {
        refreshes.value++
    }

    fun setThemeMode(mode: Int) {
        MainPreferences.setDayNight(mode == SettingsLabels.THEME_AUTO)
        if (mode != SettingsLabels.THEME_AUTO) MainPreferences.setTheme(mode)
    }

    fun setIcon(index: Int) {
        AppIcons.select(getApplication(), index)
        refresh()
    }

    fun setCornerRadius(value: Int): Boolean {
        if (value == cornerRadius(MainPreferences.getCornerRadius())) return false
        MainPreferences.setCornerRadius(value)
        return true
    }

    fun setLanguage(code: String): Boolean {
        if (code == MainPreferences.getAppLanguage()) return false
        MainPreferences.setAppLanguage(code)
        return true
    }

    fun setSkipSplashScreen(enabled: Boolean) = MainPreferences.setSkipSplashScreen(enabled)

    fun setMetric(metric: Boolean) = MainPreferences.setUnit(metric)

    fun setCoordinatesFormat(format: Int) = MainPreferences.setCoordinatesFormat(format)

    fun setLocationProvider(provider: String) = MainPreferences.setLocationProvider(provider)

    fun setMapSource(source: Int) = MainPreferences.setMapSource(source)

    fun disableCustomLocation() = MainPreferences.setCustomCoordinates(false)

    fun setKeepScreenOn(enabled: Boolean) = MainPreferences.setScreenOn(enabled)

    private fun read(): SettingsState {
        val context = getApplication<Application>()
        return SettingsState(
            hasLocationPermission = PermissionUtils.checkPermission(context),
            themeMode = themeMode(MainPreferences.isDayNightOn(), MainPreferences.getTheme()),
            isMetric = MainPreferences.isMetric(),
            coordinatesFormat = MainPreferences.getCoordinatesFormat(),
            locationProvider = MainPreferences.getLocationProvider(),
            mapSource = MainPreferences.getMapSource(),
            language = MainPreferences.getAppLanguage() ?: SettingsLabels.LANGUAGE_DEFAULT,
            isCustomLocation = MainPreferences.isCustomCoordinate(),
            keepScreenOn = MainPreferences.isScreenOn(),
            skipSplashScreen = MainPreferences.getSkipSplashScreen(),
            iconIndex = AppIcons.current(context),
            cornerRadius = cornerRadius(MainPreferences.getCornerRadius()),
            versionName = BuildConfig.VERSION_NAME,
        )
    }

    companion object {
        private const val STOP_TIMEOUT = 5_000L
        private const val CORNER_STEP = 5

        private val keys = arrayOf(
            MainPreferences.dayNightMode,
            MainPreferences.theme,
            MainPreferences.unit,
            MainPreferences.coordinatesFormat,
            MainPreferences.locationProvider,
            MainPreferences.mapSource,
            MainPreferences.appLanguage,
            MainPreferences.isCustomCoordinate,
            MainPreferences.screenOn,
            MainPreferences.skipSplashScreen,
            MainPreferences.appCornerRadius,
        )

        fun themeMode(dayNight: Boolean, theme: Int): Int = if (dayNight) SettingsLabels.THEME_AUTO else theme

        fun cornerRadius(stored: Int): Int = stored * CORNER_STEP
    }
}
