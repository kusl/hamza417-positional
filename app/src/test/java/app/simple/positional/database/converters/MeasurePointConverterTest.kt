package app.simple.positional.database.converters

import app.simple.positional.model.Measure
import app.simple.positional.model.MeasurePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurePointConverterTest {
    private val converter = MeasurePointConverter()

    @Test
    fun roundTrip() {
        val points = arrayListOf(MeasurePoint(1.5, -2.25, 1), MeasurePoint(-45.0, 170.125, 2))
        val data = converter.fromMeasurePoints(points)
        assertEquals("1.5,-2.25,1;-45.0,170.125,2;", data)
        assertEquals(points, converter.toMeasurePoints(data))
    }

    @Test
    fun emptyAndNull() {
        assertEquals("", converter.fromMeasurePoints(arrayListOf()))
        assertNull(converter.fromMeasurePoints(null))
        assertNull(converter.toMeasurePoints(null))
        assertTrue(converter.toMeasurePoints("")!!.isEmpty())
    }

    @Test
    fun pointsCompareByValue() {
        val list = arrayListOf(MeasurePoint(3.0, 4.0, 1))
        list.remove(MeasurePoint(3.0, 4.0, 1))
        assertTrue(list.isEmpty())
    }

    @Test
    fun measureCreatesPointsOnce() {
        val measure = Measure(1L, "a", null)
        val points = measure.points
        points.add(MeasurePoint(1.0, 2.0, 1))
        assertSame(points, measure.measurePoints)
        assertEquals(1, measure.points.size)
    }
}
