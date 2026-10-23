package app.simple.positional.ui.compose

import app.simple.positional.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MenuSheetStateTest {
    @Test
    fun refreshRebuildsFromBuilder() {
        var checked = false
        val state = MenuSheetState()
        state.show { listOf(MenuItem.Toggle(R.string.labels, checked) { checked = it }) }
        val toggle = state.items!!.single() as MenuItem.Toggle
        toggle.onChange(!toggle.checked)
        state.refresh()
        assertEquals(true, (state.items!!.single() as MenuItem.Toggle).checked)
    }

    @Test
    fun dismissClearsItems() {
        val state = MenuSheetState()
        state.show { listOf(MenuItem.Action(R.string.help) {}) }
        state.dismiss()
        assertNull(state.items)
        state.refresh()
        assertNull(state.items)
    }
}
