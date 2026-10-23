package app.simple.positional.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.simple.positional.model.Measure
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasureDao {
    @Query("SELECT * FROM measures ORDER BY date_added DESC")
    fun observeAll(): Flow<List<Measure>>

    @Query("SELECT * FROM measures WHERE date_added = :date")
    fun getMeasureById(date: Long): Measure?

    @Delete
    suspend fun deleteMeasure(measure: Measure)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasure(measure: Measure)

    @Update
    suspend fun updateMeasure(measure: Measure)
}
