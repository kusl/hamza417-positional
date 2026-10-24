package app.simple.positional.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measures")
class Measure(
    @PrimaryKey @ColumnInfo(name = "date_added") var dateCreated: Long,
    @ColumnInfo(name = "measure_name") var name: String?,
    @ColumnInfo(name = "measure_note") var note: String?,
    @ColumnInfo(name = "measure_points") var measurePoints: ArrayList<MeasurePoint>? = null,
) {
    val points: ArrayList<MeasurePoint>
        get() = measurePoints ?: ArrayList<MeasurePoint>().also { measurePoints = it }
}
