package app.simple.positional.database.instances

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import app.simple.positional.database.dao.DirectionDao
import app.simple.positional.model.DirectionModel

@Database(entities = [DirectionModel::class], exportSchema = true, version = 1)
abstract class DirectionDatabase : RoomDatabase() {
    abstract fun directionDao(): DirectionDao

    companion object {
        @Volatile
        private var instance: DirectionDatabase? = null

        fun getInstance(context: Context): DirectionDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, DirectionDatabase::class.java, "directions")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                .also { instance = it }
        }
    }
}
