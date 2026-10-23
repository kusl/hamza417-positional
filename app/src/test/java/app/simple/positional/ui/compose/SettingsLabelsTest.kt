package app.simple.positional.ui.compose

import androidx.appcompat.app.AppCompatDelegate
import app.simple.positional.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsLabelsTest {
    @Test
    fun themeLabels() {
        assertEquals(R.string.theme_day, SettingsLabels.theme(AppCompatDelegate.MODE_NIGHT_NO))
        assertEquals(R.string.theme_night, SettingsLabels.theme(AppCompatDelegate.MODE_NIGHT_YES))
        assertEquals(R.string.theme_follow_system, SettingsLabels.theme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM))
        assertEquals(R.string.theme_auto, SettingsLabels.theme(SettingsLabels.THEME_AUTO))
        assertNull(SettingsLabels.theme(99))
    }

    @Test
    fun unitLabels() {
        assertEquals(R.string.unit_metric, SettingsLabels.unit(true))
        assertEquals(R.string.unit_imperial, SettingsLabels.unit(false))
    }

    @Test
    fun coordinatesFormatLabels() {
        assertEquals(R.string.dd_dddddd, SettingsLabels.coordinatesFormat(0))
        assertEquals(R.string.dd_mm_mmm, SettingsLabels.coordinatesFormat(1))
        assertEquals(R.string.dd_mm_ss_sss, SettingsLabels.coordinatesFormat(2))
        assertNull(SettingsLabels.coordinatesFormat(3))
    }

    @Test
    fun locationProviderLabels() {
        assertEquals("Android Location Provider", SettingsLabels.locationProvider(SettingsLabels.PROVIDER_ANDROID))
        assertEquals("Fused Location Provider", SettingsLabels.locationProvider(SettingsLabels.PROVIDER_FUSED))
        assertEquals("", SettingsLabels.locationProvider("other"))
    }

    @Test
    fun languageNames() {
        assertNull(SettingsLabels.languageName(SettingsLabels.LANGUAGE_DEFAULT))
        assertEquals("Français", SettingsLabels.languageName("fr"))
        assertNull(SettingsLabels.languageName("xx"))
    }

    @Test
    fun legalPagesAreDistinct() {
        assertEquals(6, SettingsLabels.legalPages.toSet().size)
    }
}