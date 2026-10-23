package app.simple.positional.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapSearchQueryTest {
    @Test
    fun shortQueriesAreIgnored() {
        assertNull(MapSearchQuery.normalize(""))
        assertNull(MapSearchQuery.normalize("  ab  "))
    }

    @Test
    fun queriesAreTrimmed() {
        assertEquals("Paris", MapSearchQuery.normalize("  Paris "))
        assertEquals("abc", MapSearchQuery.normalize("abc"))
    }
}
