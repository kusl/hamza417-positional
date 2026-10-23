package app.simple.positional.math

import app.simple.positional.math.UnitConverter.toFeet
import app.simple.positional.math.UnitConverter.toKiloMetersPerHour
import app.simple.positional.math.UnitConverter.toKilometers
import app.simple.positional.math.UnitConverter.toMiles
import app.simple.positional.math.UnitConverter.toMilesPerHour
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConverterTest {
    @Test
    fun convertsSpeeds() {
        assertEquals(36.0, 10.0.toKiloMetersPerHour(), 1e-9)
        assertEquals(36f, 10f.toKiloMetersPerHour(), 1e-4f)
        assertEquals(22.369, 10f.toMilesPerHour().toDouble(), 1e-2)
        assertEquals(62.1371, 100.0.toMilesPerHour(), 1e-9)
    }

    @Test
    fun convertsDistances() {
        assertEquals(1.5, 1500.0.toKilometers(), 1e-9)
        assertEquals(1.5f, 1500f.toKilometers(), 1e-6f)
        assertEquals(1f, 1609f.toMiles(), 1e-6f)
        assertEquals(6.21371, 10.0.toMiles(), 1e-9)
        assertEquals(328.084, 100.0.toFeet(), 1e-9)
        assertEquals(3.28084f, 1f.toFeet(), 1e-6f)
    }
}
