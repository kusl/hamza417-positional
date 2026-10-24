package app.simple.positional.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trails")
class TrailEntry(
    @ColumnInfo(name = "date_added") var dateCreated: Long,
    @ColumnInfo(name = "trail_name") var trailName: String?,
    @ColumnInfo(name = "trail_note") var trailNote: String?,
) {
    @PrimaryKey(autoGenerate = true)
    var keyTag: Int = 0
}
