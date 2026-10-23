package app.simple.positional.math

import org.junit.Assert.assertEquals
import org.junit.Test

class SeriesStatsTest {
    @Test
    fun emptySeriesIsZero() {
        assertEquals(SeriesStats(0.0, 0.0, 0.0), SeriesStats.of(emptyList()))
    }

    @Test
    fun computesMinMaxAverage() {
        val stats = SeriesStats.of(listOf(4f, 2f, 6f))
        assertEquals(2.0, stats.min, 0.0)
        assertEquals(6.0, stats.max, 0.0)
        assertEquals(4.0, stats.average, 1e-9)
    }
}
