package app.simple.positional.viewmodels.viewmodel

import app.simple.positional.ui.compose.SettingsLabels
import app.simple.positional.util.NightMode
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsViewModelTest {
    @Test
    fun autoThemeWinsOverStoredTheme() {
        assertEquals(SettingsLabels.THEME_AUTO, SettingsViewModel.themeMode(true, NightMode.YES))
    }

    @Test
    fun storedThemeShownWhenAutoIsOff() {
        assertEquals(NightMode.YES, SettingsViewModel.themeMode(false, NightMode.YES))
        assertEquals(NightMode.FOLLOW_SYSTEM, SettingsViewModel.themeMode(false, NightMode.FOLLOW_SYSTEM))
    }

    @Test
    fun cornerRadiusIsStoredInFifths() {
        assertEquals(SettingsLabels.CORNER_DEFAULT, SettingsViewModel.cornerRadius(30))
        assertEquals(SettingsLabels.CORNER_MIN, SettingsViewModel.cornerRadius(5))
        assertEquals(SettingsLabels.CORNER_MAX, SettingsViewModel.cornerRadius(80))
    }
}
