package com.randolph.keeplocal.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.randolph.keeplocal.data.local.converter.Converters
import com.randolph.keeplocal.data.local.dao.NoteDao
import com.randolph.keeplocal.data.local.dao.NoteEmbeddingDao
import com.randolph.keeplocal.data.local.entity.NoteEmbeddingEntity
import com.randolph.keeplocal.data.local.entity.NoteEntity
import com.randolph.keeplocal.data.local.entity.NoteFtsEntity

@Database(
    entities = [
        NoteEntity::class,
        NoteEmbeddingEntity::class,
        NoteFtsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KeepLocalDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun noteEmbeddingDao(): NoteEmbeddingDao

    companion object {
        @Volatile
        private var INSTANCE: KeepLocalDatabase? = null

        fun getDatabase(context: Context): KeepLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KeepLocalDatabase::class.java,
                    "keeplocal_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
