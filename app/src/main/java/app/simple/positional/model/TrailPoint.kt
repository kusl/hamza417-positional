package app.simple.positional.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trail_data")
class TrailPoint(
    @ColumnInfo(name = "lat") var latitude: Double,
    @ColumnInfo(name = "lng") var longitude: Double,
    @PrimaryKey @ColumnInfo(name = "time_added") var timeAdded: Long,
    @ColumnInfo(name = "icon_position") var iconPosition: Int,
    @ColumnInfo(name = "note") var note: String?,
    @ColumnInfo(name = "name") var name: String?,
    @ColumnInfo(name = "accuracy") var accuracy: Float,
)
