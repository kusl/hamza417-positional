package app.simple.positional.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NightModeTest {
    @Test
    fun explicitModesIgnoreSystem() {
        assertTrue(NightMode.resolve(NightMode.YES, false, systemNight = false, powerSave = false, hour = 12))
        assertFalse(NightMode.resolve(NightMode.NO, false, systemNight = true, powerSave = true, hour = 23))
    }

    @Test
    fun followSystem() {
        assertTrue(NightMode.resolve(NightMode.FOLLOW_SYSTEM, false, systemNight = true, powerSave = false, hour = 12))
        assertFalse(NightMode.resolve(NightMode.FOLLOW_SYSTEM, false, systemNight = false, powerSave = true, hour = 23))
    }

    @Test
    fun batterySaverTurnsNightOn() {
        assertTrue(NightMode.resolve(NightMode.AUTO_BATTERY, false, systemNight = false, powerSave = true, hour = 12))
        assertFalse(NightMode.resolve(NightMode.AUTO_BATTERY, false, systemNight = false, powerSave = false, hour = 12))
    }

    @Test
    fun byTimeUsesSixToEighteenAsDay() {
        assertFalse(NightMode.resolve(NightMode.NO, true, systemNight = true, powerSave = false, hour = 6))
        assertFalse(NightMode.resolve(NightMode.AUTO_TIME, false, systemNight = true, powerSave = false, hour = 17))
        assertTrue(NightMode.resolve(NightMode.YES, true, systemNight = false, powerSave = false, hour = 18))
        assertTrue(NightMode.resolve(NightMode.AUTO_TIME, false, systemNight = false, powerSave = false, hour = 5))
    }
}
