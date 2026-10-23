package app.simple.positional.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Test

class PhysicsValueTest {
    @Test
    fun zeroIsRaisedToMinimum() {
        assertEquals(0.1f, physicsValue(0f), 0f)
    }

    @Test
    fun positiveValuesPassThrough() {
        assertEquals(25f, physicsValue(25f), 0f)
    }
}
