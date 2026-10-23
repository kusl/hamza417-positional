package app.simple.positional.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Test

class PanelCatalogTest {
    @Test
    fun unknownTagsFallBackToLocation() {
        assertEquals(PanelCatalog.LOCATION, PanelCatalog.resolve(null))
        assertEquals(PanelCatalog.LOCATION, PanelCatalog.resolve("missing"))
        assertEquals(PanelCatalog.MEASURE, PanelCatalog.resolve("Measure"))
    }

    @Test
    fun stepWrapsAround() {
        assertEquals(PanelCatalog.COMPASS, PanelCatalog.step(PanelCatalog.CLOCK, 1))
        assertEquals(PanelCatalog.SETTINGS, PanelCatalog.step(PanelCatalog.CLOCK, -1))
        assertEquals(PanelCatalog.CLOCK, PanelCatalog.step(PanelCatalog.SETTINGS, 1))
    }

    @Test
    fun tagsAreUnique() {
        assertEquals(PanelCatalog.tags.size, PanelCatalog.tags.toSet().size)
    }
}
