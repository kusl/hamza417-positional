package app.simple.positional.database.instances

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.simple.positional.database.converters.MeasurePointConverter
import app.simple.positional.database.dao.MeasureDao
import app.simple.positional.model.Measure

@Database(entities = [Measure::class], exportSchema = true, version = 1)
@TypeConverters(MeasurePointConverter::class)
abstract class MeasureDatabase : RoomDatabase() {
    abstract fun measureDao(): MeasureDao

    companion object {
        @Volatile
        private var instance: MeasureDatabase? = null

        fun getInstance(context: Context): MeasureDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, MeasureDatabase::class.java, "measure_db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                .also { instance = it }
        }
    }
}
