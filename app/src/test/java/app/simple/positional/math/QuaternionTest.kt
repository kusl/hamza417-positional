package app.simple.positional.math

import org.junit.Assert.assertEquals
import org.junit.Test

class QuaternionTest {
    private fun assertVector(expected: Vector3, actual: Vector3) {
        assertEquals(expected.x, actual.x, 1e-4f)
        assertEquals(expected.y, actual.y, 1e-4f)
        assertEquals(expected.z, actual.z, 1e-4f)
    }

    @Test
    fun identityDoesNotRotate() {
        val v = Vector3(1f, 2f, 3f)
        assertVector(v, Quaternion.zero.rotate(v))
    }

    @Test
    fun normalizeProducesUnitQuaternion() {
        assertEquals(1f, Quaternion(1f, 2f, 3f, 4f).normalize().magnitude(), 1e-5f)
    }

    @Test
    fun inverseUndoesRotation() {
        val q = Quaternion.from(Euler(10f, 20f, 30f))
        val v = Vector3(1f, 0f, 0f)
        assertVector(v, q.inverse().rotate(q.rotate(v)))
        assertVector(v, (q * q.inverse()).rotate(v))
    }

    @Test
    fun eulerRoundTrip() {
        val euler = Quaternion.from(Euler(10f, 20f, 30f)).toEuler()
        assertEquals(10f, euler.roll, 1e-2f)
        assertEquals(20f, euler.pitch, 1e-2f)
        assertEquals(30f, euler.yaw, 1e-2f)
    }
}
