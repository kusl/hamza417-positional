package app.simple.positional.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.simple.positional.model.TrailEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface TrailDao {
    @Query("SELECT * FROM trails ORDER BY date_added DESC")
    fun observeAll(): Flow<List<TrailEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrail(trailEntry: TrailEntry)

    @Delete
    suspend fun deleteTrail(trailEntry: TrailEntry)
}
