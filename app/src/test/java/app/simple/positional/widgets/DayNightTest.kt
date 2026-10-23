package app.simple.positional.widgets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class DayNightTest {
    private val zone = ZoneId.of("America/New_York")
    private fun at(day: Int, hour: Int, minute: Int = 0) = ZonedDateTime.of(2026, 10, day, hour, minute, 0, 0, zone)

    @Test
    fun dayRunsFromSixToSevenPm() {
        assertFalse(DayNight.isDay(5))
        assertTrue(DayNight.isDay(6))
        assertTrue(DayNight.isDay(18))
        assertFalse(DayNight.isDay(19))
    }

    @Test
    fun nextChangeIsTheNextBoundary() {
        assertEquals(at(23, 6), DayNight.nextChange(at(23, 2, 30)))
        assertEquals(at(23, 19), DayNight.nextChange(at(23, 6)))
        assertEquals(at(23, 19), DayNight.nextChange(at(23, 18, 59)))
        assertEquals(at(24, 6), DayNight.nextChange(at(23, 19)))
        assertEquals(at(24, 6), DayNight.nextChange(at(23, 23, 59)))
    }

    @Test
    fun nextChangeKeepsTheZone() {
        assertEquals(zone, DayNight.nextChange(at(23, 12)).zone)
    }
}
