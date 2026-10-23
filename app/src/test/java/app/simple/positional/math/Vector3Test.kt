package app.simple.positional.math

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class Vector3Test {
    private val x = Vector3(1f, 0f, 0f)
    private val y = Vector3(0f, 1f, 0f)

    @Test
    fun arithmetic() {
        assertEquals(Vector3(0f, 0f, 1f), x.cross(y))
        assertEquals(Vector3(1f, 1f, 0f), x + y)
        assertEquals(Vector3(1f, -1f, 0f), x - y)
        assertEquals(Vector3(3f, 0f, 0f), x * 3f)
        assertEquals(0f, x.dot(y), 0f)
    }

    @Test
    fun magnitudeAndNormalize() {
        val v = Vector3(3f, 4f, 0f)
        assertEquals(5f, v.magnitude(), 1e-6f)
        assertEquals(1f, v.normalize().magnitude(), 1e-6f)
        assertArrayEquals(floatArrayOf(3f, 4f, 0f), Vector3.from(v.toFloatArray()).toFloatArray(), 0f)
    }

    @Test
    fun arrayHelpersMatchVectorOperations() {
        val a = floatArrayOf(1f, 2f, 3f)
        val b = floatArrayOf(4f, 5f, 6f)
        assertArrayEquals(floatArrayOf(-3f, 6f, -3f), Vector3Utils.cross(a, b), 1e-6f)
        assertEquals(32f, Vector3Utils.dot(a, b), 1e-6f)
        assertEquals(1f, Vector3Utils.magnitude(Vector3Utils.normalize(b)), 1e-6f)
    }
}
