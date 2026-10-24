package app.simple.positional.database.converters

import androidx.room.TypeConverter
import app.simple.positional.model.MeasurePoint

class MeasurePointConverter {
    @TypeConverter
    fun fromMeasurePoints(points: ArrayList<MeasurePoint>?): String? =
        points?.joinToString("") { it.toDatabase() + ";" }

    @TypeConverter
    fun toMeasurePoints(data: String?): ArrayList<MeasurePoint>? =
        data?.split(';')?.filter { it.isNotEmpty() }?.mapTo(ArrayList()) { MeasurePoint.fromDatabase(it) }
}
