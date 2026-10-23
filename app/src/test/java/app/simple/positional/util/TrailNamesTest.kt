package app.simple.positional.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrailNamesTest {
    @Test
    fun acceptsNewNames() {
        assertTrue(TrailNames.isValid("Morning walk", listOf("Evening run")))
        assertTrue(TrailNames.isValid("  Hike  ", emptyList()))
    }

    @Test
    fun rejectsBlankReservedAndPathNames() {
        assertFalse(TrailNames.isValid("   ", emptyList()))
        assertFalse(TrailNames.isValid("%%_TRAILS", emptyList()))
        assertFalse(TrailNames.isValid("a/b", emptyList()))
        assertFalse(TrailNames.isValid("a\\b", emptyList()))
    }

    @Test
    fun rejectsDuplicatesIgnoringCase() {
        assertFalse(TrailNames.isValid(" evening RUN ", listOf("Evening run")))
    }
}
