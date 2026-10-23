package app.simple.positional.ui.compose

import app.simple.positional.util.NightMode
import app.simple.positional.R
import app.simple.positional.util.LocaleHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsLabelsTest {
    @Test
    fun themeLabels() {
        assertEquals(R.string.theme_day, SettingsLabels.theme(NightMode.NO))
        assertEquals(R.string.theme_night, SettingsLabels.theme(NightMode.YES))
        assertEquals(R.string.theme_follow_system, SettingsLabels.theme(NightMode.FOLLOW_SYSTEM))
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
        assertEquals(R.string.android_location_provider, SettingsLabels.locationProvider(SettingsLabels.PROVIDER_ANDROID))
        assertEquals(R.string.fused_location_provider, SettingsLabels.locationProvider(SettingsLabels.PROVIDER_FUSED))
        assertNull(SettingsLabels.locationProvider("other"))
        assertEquals(R.string.desc_fused_location_provider, SettingsLabels.locationProviderDescription(SettingsLabels.PROVIDER_FUSED))
    }

    @Test
    fun everyChoiceHasALabel() {
        SettingsLabels.themeModes.forEach { assertNotNull(SettingsLabels.theme(it)) }
        SettingsLabels.coordinateFormats.forEach { assertNotNull(SettingsLabels.coordinatesFormat(it)) }
        SettingsLabels.providers.forEach {
            assertNotNull(SettingsLabels.locationProvider(it))
            assertNotNull(SettingsLabels.locationProviderDescription(it))
        }
    }

    @Test
    fun languagesStartWithDefaultThenSortByName() {
        val languages = SettingsLabels.languages()
        assertEquals(SettingsLabels.LANGUAGE_DEFAULT to null, languages.first())
        val names = languages.drop(1).map { it.second!! }
        assertEquals(names.sorted(), names)
        assertEquals(LocaleHelper.localeList.size, languages.size)
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