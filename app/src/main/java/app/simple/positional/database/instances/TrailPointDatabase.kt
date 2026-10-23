package app.simple.positional.database.instances

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import app.simple.positional.database.dao.TrailPointDao
import app.simple.positional.model.TrailPoint

@Database(entities = [TrailPoint::class], exportSchema = true, version = 3)
abstract class TrailPointDatabase : RoomDatabase() {
    abstract fun trailDataDao(): TrailPointDao

    companion object {
        private var name: String? = null
        private var instance: TrailPointDatabase? = null

        @Synchronized
        fun getInstance(context: Context, trail: String): TrailPointDatabase {
            instance?.takeIf { name == trail }?.let { return it }
            instance?.close()
            return Room.databaseBuilder(context.applicationContext, TrailPointDatabase::class.java, "$trail.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                .also {
                    instance = it
                    name = trail
                }
        }

        @Synchronized
        fun close(trail: String) {
            if (name != trail) return
            instance?.close()
            instance = null
            name = null
        }
    }
}
