package app.simple.positional.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.simple.positional.model.Locations
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Query("SELECT * FROM location ORDER BY date_added DESC LIMIT 99")
    fun observeAll(): Flow<List<Locations>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: Locations)

    @Delete
    suspend fun deleteLocation(location: Locations)

    @Query("DELETE FROM location")
    suspend fun deleteAll()
}
