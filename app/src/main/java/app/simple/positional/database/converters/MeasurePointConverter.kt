package app.simple.positional.database.converters

import androidx.room.TypeConverter
import app.simple.positional.model.MeasurePoint

class MeasurePointConverter {
    @TypeConverter
    fun fromMeasurePoints(measurePoints: ArrayList<MeasurePoint?>): String =
        measurePoints.filterNotNull().joinToString("") { it.convertForDatabase() + ";" }

    @TypeConverter
    fun toMeasurePoints(data: String?): ArrayList<MeasurePoint?> =
        data.orEmpty().split(';').filter { it.isNotEmpty() }.mapTo(ArrayList<MeasurePoint?>()) { MeasurePoint.convertFromDatabase(it) }
}
