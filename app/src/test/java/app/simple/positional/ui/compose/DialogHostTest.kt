package app.simple.positional.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DialogHostTest {
    @Test
    fun showAndDismiss() {
        val host = DialogHost()
        assertNull(host.current)
        host.show(ErrorDialog("a"))
        assertEquals(ErrorDialog("a"), host.current)
        host.dismiss()
        assertNull(host.current)
    }

    @Test
    fun showReplacesCurrent() {
        val host = DialogHost()
        host.show(ErrorDialog("a"))
        host.show(ErrorDialog("b"))
        assertEquals(ErrorDialog("b"), host.current)
    }
}
