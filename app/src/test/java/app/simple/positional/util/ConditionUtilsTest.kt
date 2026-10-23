package app.simple.positional.util

import app.simple.positional.util.ConditionUtils.invert
import app.simple.positional.util.ConditionUtils.isNotNull
import app.simple.positional.util.ConditionUtils.isNull
import app.simple.positional.util.ConditionUtils.isZero
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConditionUtilsTest {
    @Test
    fun conditions() {
        val nothing: Any? = null
        assertTrue(nothing.isNull())
        assertFalse(nothing.isNotNull())
        assertTrue("x".isNotNull())
        assertTrue(0.isZero())
        assertFalse(1.isZero())
        assertFalse(true.invert())
    }
}
