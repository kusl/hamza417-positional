package app.simple.positional.database.dao

import androidx.room.*
import app.simple.positional.model.DirectionModel

@Dao
interface DirectionDao {
    @Query("SELECT * FROM directions ORDER BY date_added COLLATE nocase DESC LIMIT 999")
    fun getAllDirections(): MutableList<DirectionModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDirection(direction: DirectionModel)

    @Update
    suspend fun updateDirection(direction: DirectionModel)

    @Delete
    suspend fun deleteDirection(direction: DirectionModel)

    @Query("DELETE FROM directions")
    fun nukeTable()
}
