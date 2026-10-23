package app.simple.positional.ui.compose

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.simple.positional.R
import app.simple.positional.util.LocaleHelper

@Immutable
data class SettingsState(
    val hasLocationPermission: Boolean = true,
    val themeMode: Int = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
    val isMetric: Boolean = true,
    val coordinatesFormat: Int = 0,
    val locationProvider: String = SettingsLabels.PROVIDER_ANDROID,
    val language: String = SettingsLabels.LANGUAGE_DEFAULT,
    val isCustomLocation: Boolean = false,
    val keepScreenOn: Boolean = false,
    val skipSplashScreen: Boolean = false,
    val iconIndex: Int = 0,
    val cornerRadius: Int = SettingsLabels.CORNER_DEFAULT,
    val versionName: String = "",
)

interface SettingsActions {
    fun requestPermission()
    fun setThemeMode(mode: Int)
    fun openAccent()
    fun setIcon(index: Int)
    fun setCornerRadius(value: Int)
    fun setSkipSplashScreen(enabled: Boolean)
    fun openArts()
    fun setMetric(metric: Boolean)
    fun setCoordinatesFormat(format: Int)
    fun setLocationProvider(provider: String)
    fun openCustomLocations()
    fun setCustomLocation(enabled: Boolean)
    fun setKeepScreenOn(enabled: Boolean)
    fun setLanguage(code: String)
    fun openLegal(@StringRes page: Int)
    fun openReleases()
    fun openRepository()
    fun openIssues()
}

internal enum class SettingsDialog { THEME, ICON, CORNERS, UNITS, COORDINATES, PROVIDER, LANGUAGE }

object SettingsLabels {
    const val THEME_AUTO = 4
    const val PROVIDER_ANDROID = "android"
    const val PROVIDER_FUSED = "fused"
    const val LANGUAGE_DEFAULT = "default"
    const val CORNER_MIN = 25
    const val CORNER_MAX = 400
    const val CORNER_DEFAULT = 150

    val legalPages = listOf(
        R.string.disclaimer,
        R.string.privacy_policy,
        R.string.terms_of_use,
        R.string.permissions,
        R.string.credits,
        R.string.internet_uses,
    )

    val themeModes = listOf(
        AppCompatDelegate.MODE_NIGHT_NO,
        AppCompatDelegate.MODE_NIGHT_YES,
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        THEME_AUTO,
    )

    val coordinateFormats = listOf(0, 1, 2)

    val providers = listOf(PROVIDER_ANDROID, PROVIDER_FUSED)

    @StringRes
    fun theme(mode: Int): Int? = when (mode) {
        AppCompatDelegate.MODE_NIGHT_NO -> R.string.theme_day
        AppCompatDelegate.MODE_NIGHT_YES -> R.string.theme_night
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> R.string.theme_follow_system
        THEME_AUTO -> R.string.theme_auto
        else -> null
    }

    @StringRes
    fun unit(isMetric: Boolean): Int = if (isMetric) R.string.unit_metric else R.string.unit_imperial

    @StringRes
    fun coordinatesFormat(format: Int): Int? = when (format) {
        0 -> R.string.dd_dddddd
        1 -> R.string.dd_mm_mmm
        2 -> R.string.dd_mm_ss_sss
        else -> null
    }

    @StringRes
    fun locationProvider(provider: String): Int? = when (provider) {
        PROVIDER_ANDROID -> R.string.android_location_provider
        PROVIDER_FUSED -> R.string.fused_location_provider
        else -> null
    }

    @StringRes
    fun locationProviderDescription(provider: String): Int? = when (provider) {
        PROVIDER_ANDROID -> R.string.desc_android_location_provider
        PROVIDER_FUSED -> R.string.desc_fused_location_provider
        else -> null
    }

    fun languages(): List<Pair<String, String?>> {
        val (defaults, named) = LocaleHelper.localeList.partition { it.localeCode == LANGUAGE_DEFAULT }
        return defaults.map { it.localeCode to null } + named.sortedBy { it.language }.map { it.localeCode to it.language }
    }

    fun languageName(code: String): String? =
        if (code == LANGUAGE_DEFAULT) null else LocaleHelper.localeList.firstOrNull { it.localeCode == code }?.language
}

@Composable
fun SettingsScreen(state: SettingsState, actions: SettingsActions, topInset: Int) {
    val top = with(LocalDensity.current) { topInset.toDp() }
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(top = top, bottom = dimensionResource(R.dimen.bottom_nav_height)),
        ) {
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 26.dp, vertical = 16.dp),
            )
            if (!state.hasLocationPermission) PermissionBanner(actions::requestPermission)
            AppearanceSection(state, actions) { dialog = it }
            ConfigurationSection(state, actions) { dialog = it }
            AboutSection(state, actions)
        }
    }
    dialog?.let { current -> SettingsDialogHost(current, state, actions) { dialog = null } }
}

@Composable
private fun PermissionBanner(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFF4444))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.no_location_permission_alert),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(16.dp))
        Icon(painterResource(R.drawable.ic_bad_location), contentDescription = null, tint = Color.White)
    }
}

@Composable
private fun AppearanceSection(state: SettingsState, actions: SettingsActions, show: (SettingsDialog) -> Unit) {
    PreferenceCategory(stringResource(R.string.appearance)) {
        PreferenceItem(
            title = stringResource(R.string.theme),
            summary = SettingsLabels.theme(state.themeMode)?.let { stringResource(it) },
            onClick = { show(SettingsDialog.THEME) },
        )
        PreferenceItem(title = stringResource(R.string.icon), onClick = { show(SettingsDialog.ICON) })
        PreferenceItem(
            title = stringResource(R.string.accent_colors),
            trailing = {
                Box(
                    Modifier
                        .size(dimensionResource(R.dimen.app_button_size))
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            },
            onClick = actions::openAccent,
        )
        PreferenceItem(title = stringResource(R.string.corner_radius), onClick = { show(SettingsDialog.CORNERS) })
        SwitchPreference(
            title = stringResource(R.string.skip_splash_screen),
            summary = stringResource(R.string.desc_skip_splash_Screen),
            checked = state.skipSplashScreen,
            onLongClick = actions::openArts,
            onCheckedChange = actions::setSkipSplashScreen,
        )
    }
}

@Composable
private fun ConfigurationSection(state: SettingsState, actions: SettingsActions, show: (SettingsDialog) -> Unit) {
    PreferenceCategory(stringResource(R.string.configuration)) {
        PreferenceItem(
            title = stringResource(R.string.units),
            summary = stringResource(SettingsLabels.unit(state.isMetric)),
            onClick = { show(SettingsDialog.UNITS) },
        )
        PreferenceItem(
            title = stringResource(R.string.app_coordinates_format),
            summary = SettingsLabels.coordinatesFormat(state.coordinatesFormat)?.let { stringResource(it) },
            onClick = { show(SettingsDialog.COORDINATES) },
        )
        PreferenceItem(
            title = stringResource(R.string.location_provider),
            summary = SettingsLabels.locationProvider(state.locationProvider)?.let { stringResource(it) },
            onClick = { show(SettingsDialog.PROVIDER) },
        )
        PreferenceItem(
            title = stringResource(R.string.specified_location),
            summary = stringResource(R.string.desc_specified_location),
            trailing = { Switch(checked = state.isCustomLocation, onCheckedChange = actions::setCustomLocation) },
            onClick = actions::openCustomLocations,
        )
        SwitchPreference(
            title = stringResource(R.string.keep_screen_on),
            summary = stringResource(R.string.desc_screen_on),
            checked = state.keepScreenOn,
            onCheckedChange = actions::setKeepScreenOn,
        )
        PreferenceItem(
            title = stringResource(R.string.language),
            summary = SettingsLabels.languageName(state.language) ?: stringResource(R.string.auto_system_default_language),
            onClick = { show(SettingsDialog.LANGUAGE) },
        )
    }
}

@Composable
private fun AboutSection(state: SettingsState, actions: SettingsActions) {
    var legalExpanded by remember { mutableStateOf(false) }
    PreferenceCategory(stringResource(R.string.about)) {
        Box {
            PreferenceItem(
                title = stringResource(R.string.legal_notes),
                summary = stringResource(R.string.desc_legal),
                onClick = { legalExpanded = true },
            )
            DropdownMenu(expanded = legalExpanded, onDismissRequest = { legalExpanded = false }) {
                SettingsLabels.legalPages.forEach { page ->
                    DropdownMenuItem(
                        text = { Text(stringResource(page), style = MaterialTheme.typography.titleMedium) },
                        onClick = {
                            legalExpanded = false
                            actions.openLegal(page)
                        },
                    )
                }
            }
        }
        PreferenceItem(title = stringResource(R.string.app_version), summary = state.versionName, onClick = actions::openReleases)
        PreferenceItem(title = "GitHub", summary = stringResource(R.string.desc_git), onClick = actions::openRepository)
        PreferenceItem(title = stringResource(R.string.found_an_issue), onClick = actions::openIssues)
    }
}