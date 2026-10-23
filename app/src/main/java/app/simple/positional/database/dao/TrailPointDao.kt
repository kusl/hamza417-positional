package app.simple.positional.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.simple.positional.model.TrailPoint

@Dao
interface TrailPointDao {
    @Query("SELECT * FROM trail_data ORDER BY time_added COLLATE nocase")
    fun getAllTrailData(): List<TrailPoint>

    @Query("SELECT * FROM trail_data ORDER BY time_added COLLATE nocase DESC")
    fun getAllTrailDataDesc(): List<TrailPoint>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrailData(trailPoint: TrailPoint)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrailData(trailData: List<TrailPoint>)

    @Update
    suspend fun updateTrailData(trailPoint: TrailPoint)

    @Delete
    suspend fun deleteTrailData(trailPoint: TrailPoint)

    @Query("DELETE FROM trail_data")
    fun nukeTable()
}
