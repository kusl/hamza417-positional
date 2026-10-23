package app.simple.positional.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.Locale

class TimeZoneCatalogTest {
    private val now = Instant.parse("2026-01-15T12:30:00Z")
    private val entries = TimeZoneCatalog.entries(now, twelveHour = false, locale = Locale.US)

    @Test
    fun entriesAreSortedAndUnique() {
        val ids = entries.map { it.id }
        assertEquals(ids.sorted(), ids)
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun utcUsesExplicitZeroOffset() {
        val utc = entries.first { it.id == "UTC" }
        assertEquals("+00:00", utc.offset)
        assertEquals("12:30", utc.time)
    }

    @Test
    fun twelveHourFormatUsesMeridiem() {
        val utc = TimeZoneCatalog.entries(now, twelveHour = true, locale = Locale.US).first { it.id == "UTC" }
        assertEquals("12:30 PM", utc.time)
    }

    @Test
    fun filterMatchesIdIgnoringCaseAndOffset() {
        assertTrue(TimeZoneCatalog.filter(entries, "  tokyo ").any { it.id == "Asia/Tokyo" })
        assertTrue(TimeZoneCatalog.filter(entries, "+09:00").all { it.offset.contains("+09:00") })
        assertEquals(entries, TimeZoneCatalog.filter(entries, "   "))
        assertTrue(TimeZoneCatalog.filter(entries, "no such zone").isEmpty())
    }
}
