package app.simple.positional.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapSourceTest {
    @Test
    fun automaticFallsBackToOsmWithoutKey() {
        assertTrue(MapSource.useOsm(MapSource.AUTO, hasGoogleKey = false))
        assertFalse(MapSource.useOsm(MapSource.AUTO, hasGoogleKey = true))
    }

    @Test
    fun explicitChoiceWins() {
        assertTrue(MapSource.useOsm(MapSource.OSM, hasGoogleKey = true))
        assertFalse(MapSource.useOsm(MapSource.GOOGLE, hasGoogleKey = false))
    }

    @Test
    fun choicesAreDistinct() {
        assertEquals(3, MapSource.choices.toSet().size)
    }
}
