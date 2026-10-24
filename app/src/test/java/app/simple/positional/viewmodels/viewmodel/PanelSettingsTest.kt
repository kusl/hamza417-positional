package app.simple.positional.viewmodels.viewmodel

import app.simple.positional.R
import app.simple.positional.constants.ClockSkinsConstants.clockNeedleSkins
import app.simple.positional.constants.CompassBloom.compassBloomRes
import app.simple.positional.constants.LocationPins
import app.simple.positional.ui.compose.MapRotation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PanelSettingsTest {
    @Test
    fun compassRotationWinsOverBearing() {
        assertEquals(MapRotation.Compass, GpsSettings.rotation(compass = true, bearing = true))
        assertEquals(MapRotation.Bearing, GpsSettings.rotation(compass = false, bearing = true))
        assertEquals(MapRotation.North, GpsSettings.rotation(compass = false, bearing = false))
    }

    @Test
    fun bloomIsHiddenWhenOffOrUnknown() {
        assertNull(CompassSettings.bloom(false, 0))
        assertNull(CompassSettings.bloom(true, compassBloomRes.size))
        assertEquals(compassBloomRes[1], CompassSettings.bloom(true, 1))
    }

    @Test
    fun unknownNeedleSkinFallsBackToFirst() {
        assertEquals(clockNeedleSkins[0].toList(), ClockSettings.needles(-1))
        assertEquals(clockNeedleSkins.last().toList(), ClockSettings.needles(clockNeedleSkins.lastIndex))
    }

    @Test
    fun unknownPinSkinFallsBackToDefault() {
        assertEquals(R.drawable.ic_pin_01, ClockSettings.pin(LocationPins.locationsPins.size))
        assertEquals(LocationPins.locationsPins[2], ClockSettings.pin(2))
    }

    @Test
    fun settingsCompareByValue() {
        assertEquals(MapTools(toolsLeft = true, compass = false, wrapped = true), MapTools(toolsLeft = true, compass = false, wrapped = true))
        assertEquals(ClockSettings.needles(1), ClockSettings.needles(1))
    }
}
