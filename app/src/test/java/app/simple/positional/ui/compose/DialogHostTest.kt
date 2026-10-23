package app.simple.positional.ui.compose

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DialogHostTest {
    @Test
    fun showAndDismiss() {
        val host = DialogHost()
        assertNull(host.content)
        host.show {}
        assertNotNull(host.content)
        host.dismiss()
        assertNull(host.content)
    }
}
