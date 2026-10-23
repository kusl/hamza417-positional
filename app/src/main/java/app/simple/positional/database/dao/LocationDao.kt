package app.simple.positional.database.dao

import androidx.room.*
import app.simple.positional.model.Locations

@Dao
interface LocationDao {
    @Query("SELECT * FROM location ORDER BY date_added COLLATE nocase DESC LIMIT 99")
    fun getAllLocations(): MutableList<Locations>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: Locations)

    @Update
    suspend fun updateSong(location: Locations)

    @Delete
    suspend fun deleteLocation(location: Locations)

    @Query("DELETE FROM location")
    fun nukeTable()
}
