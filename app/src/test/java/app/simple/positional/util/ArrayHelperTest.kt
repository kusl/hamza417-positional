package app.simple.positional.util

import app.simple.positional.util.ArrayHelper.isLastValueSame
import app.simple.positional.util.ArrayHelper.secondLastOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrayHelperTest {
    @Test
    fun lastValueComparison() {
        assertFalse(arrayListOf<Float>().isLastValueSame(1f))
        assertTrue(arrayListOf(1f, 2f).isLastValueSame(2f))
        assertFalse(arrayListOf(1f, 2f).isLastValueSame(1f))
    }

    @Test
    fun secondLast() {
        assertNull(listOf(1).secondLastOrNull())
        assertEquals(2, listOf(1, 2, 3).secondLastOrNull())
    }
}
