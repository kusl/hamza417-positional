package app.simple.positional.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.simple.positional.model.DirectionModel
import kotlinx.coroutines.flow.Flow

@Dao
interface DirectionDao {
    @Query("SELECT * FROM directions ORDER BY date_added DESC LIMIT 999")
    fun observeAll(): Flow<List<DirectionModel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDirection(direction: DirectionModel)

    @Delete
    suspend fun deleteDirection(direction: DirectionModel)
}
