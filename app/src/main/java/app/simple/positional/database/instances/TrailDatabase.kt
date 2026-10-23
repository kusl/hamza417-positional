package app.simple.positional.database.instances

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import app.simple.positional.database.dao.TrailDao
import app.simple.positional.model.TrailEntry

@Database(entities = [TrailEntry::class], exportSchema = false, version = 1)
abstract class TrailDatabase : RoomDatabase() {
    abstract fun trailDao(): TrailDao

    companion object {
        const val NAME = "%%_trails"

        @Volatile
        private var instance: TrailDatabase? = null

        fun getInstance(context: Context): TrailDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, TrailDatabase::class.java, "$NAME.db")
                .build()
                .also { instance = it }
        }
    }
}
