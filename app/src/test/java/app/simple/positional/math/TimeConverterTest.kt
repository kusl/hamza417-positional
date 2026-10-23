package app.simple.positional.math

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

class TimeConverterTest {
    private val time = ZonedDateTime.of(2026, 1, 1, 15, 30, 15, 500_000_000, ZoneOffset.UTC)

    @Test
    fun convertsClockHandsToDegrees() {
        assertEquals(90f, TimeConverter.getSecondsInDegrees(time, false), 1e-4f)
        assertEquals(93f, TimeConverter.getSecondsInDegrees(time, true), 1e-3f)
        assertEquals(181.5f, TimeConverter.getMinutesInDegrees(time), 1e-3f)
        assertEquals(465f, TimeConverter.getHoursInDegrees(time), 1e-3f)
        assertEquals(232.5f, TimeConverter.getHoursInDegreesFor24(time), 1e-3f)
    }
}
