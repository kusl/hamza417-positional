package app.simple.positional.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class TwilightTest {
    private val zone = "America/New_York"
    private val midsummer = ZonedDateTime.of(2026, 6, 21, 0, 0, 0, 0, ZoneId.of(zone)).toInstant()
    private val day = Twilight.compute(37.0, -76.4, zone, midsummer)

    @Test
    fun eveningMirrorsMorning() {
        assertEquals(day.morning.map { it.phase }.reversed(), day.evening.map { it.phase })
        assertEquals(TwilightPhase.entries.size, day.morning.size)
    }

    @Test
    fun rangesStartBeforeTheyEnd() {
        (day.morning + day.evening).forEach {
            assertNotNull(it.start)
            assertNotNull(it.end)
            if (it.range) assertTrue(it.phase.name, it.start!!.isBefore(it.end!!)) else assertEquals(it.start, it.end)
        }
    }

    @Test
    fun morningGoldenHourPrecedesEvening() {
        val morning = day.morning.last { it.phase == TwilightPhase.GOLDEN_HOUR }
        val evening = day.evening.first { it.phase == TwilightPhase.GOLDEN_HOUR }
        assertTrue(morning.end!!.isBefore(evening.start!!))
    }

    @Test
    fun horizonFollowsAstronomicalDawn() {
        val dawn = day.morning.first { it.phase == TwilightPhase.ASTRONOMICAL }.start!!
        val sunrise = day.morning.first { it.phase == TwilightPhase.HORIZON }.start!!
        assertTrue(dawn.isBefore(sunrise))
    }

    @Test
    fun polarDayDoesNotThrow() {
        val polar = Twilight.compute(78.2, 15.6, "Arctic/Longyearbyen", midsummer)
        assertEquals(polar.morning.size, polar.evening.size)
    }

    @Test
    fun phasesHaveDistinctColors() {
        assertEquals(TwilightPhase.entries.size, TwilightPhase.entries.map { it.color }.toSet().size)
    }
}
