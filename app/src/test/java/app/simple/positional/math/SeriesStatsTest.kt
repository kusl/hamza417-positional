package app.simple.positional.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
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

    @Test
    fun appendSkipsRepeatedValue() {
        val values = listOf(1f, 2f)
        assertSame(values, SeriesStats.append(values, 2f))
        assertEquals(listOf(1f, 2f, 1f), SeriesStats.append(values, 1f))
    }

    @Test
    fun appendKeepsNewestWithinLimit() {
        val values = (1..SeriesStats.LIMIT).map { it.toFloat() }
        val appended = SeriesStats.append(values, 0f)
        assertEquals(SeriesStats.LIMIT, appended.size)
        assertEquals(2f, appended.first())
        assertEquals(0f, appended.last())
    }
}
