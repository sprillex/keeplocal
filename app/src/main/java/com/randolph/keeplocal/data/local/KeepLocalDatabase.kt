package com.randolph.keeplocal.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KeepLocalDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun noteEmbeddingDao(): NoteEmbeddingDao

    companion object {
        @Volatile
        private var INSTANCE: KeepLocalDatabase? = null

        private fun createTriggers(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TRIGGER IF EXISTS notes_fts_ai;")
            db.execSQL("DROP TRIGGER IF EXISTS notes_fts_ad;")
            db.execSQL("DROP TRIGGER IF EXISTS notes_fts_au;")

            db.execSQL("""
                CREATE TRIGGER IF NOT EXISTS notes_fts_ai AFTER INSERT ON notes BEGIN
                    INSERT INTO notes_fts(rowid, title, content) VALUES (new.id, new.title, new.content);
                END;
            """.trimIndent())
            db.execSQL("""
                CREATE TRIGGER IF NOT EXISTS notes_fts_ad AFTER DELETE ON notes BEGIN
                    DELETE FROM notes_fts WHERE rowid = old.id;
                END;
            """.trimIndent())
            db.execSQL("""
                CREATE TRIGGER IF NOT EXISTS notes_fts_au AFTER UPDATE ON notes BEGIN
                    DELETE FROM notes_fts WHERE rowid = old.id;
                    INSERT INTO notes_fts(rowid, title, content) VALUES (new.id, new.title, new.content);
                END;
            """.trimIndent())
        }

        fun getDatabase(context: Context): KeepLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KeepLocalDatabase::class.java,
                    "keeplocal_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        createTriggers(db)
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        createTriggers(db)
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
