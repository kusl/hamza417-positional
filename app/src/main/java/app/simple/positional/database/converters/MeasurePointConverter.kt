package app.simple.positional.database.converters

import androidx.room.TypeConverter
import app.simple.positional.model.MeasurePoint

class MeasurePointConverter {
    @TypeConverter
    fun fromMeasurePoints(measurePoints: ArrayList<MeasurePoint>): String =
        measurePoints.joinToString("") { it.convertForDatabase() + ";" }

    @TypeConverter
    fun toMeasurePoints(data: String?): ArrayList<MeasurePoint> =
        data.orEmpty().split(';').filter { it.isNotEmpty() }.mapTo(ArrayList()) { MeasurePoint.convertFromDatabase(it) }
}
