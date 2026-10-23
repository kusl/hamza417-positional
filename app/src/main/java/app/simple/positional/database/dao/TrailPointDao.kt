package app.simple.positional.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.simple.positional.model.TrailPoint
import kotlinx.coroutines.flow.Flow

@Dao
interface TrailPointDao {
    @Query("SELECT * FROM trail_data ORDER BY time_added")
    fun observeAll(): Flow<List<TrailPoint>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrailData(trailPoint: TrailPoint)

    @Delete
    suspend fun deleteTrailData(trailPoint: TrailPoint)
}
