package com.randolph.keeplocal.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.randolph.keeplocal.data.local.KeepLocalDatabase
import com.randolph.keeplocal.data.local.entity.NoteEntity
import com.randolph.keeplocal.data.local.entity.NoteType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SearchRepositoryTest {

    private lateinit var db: KeepLocalDatabase
    private lateinit var searchRepository: SearchRepository
    private lateinit var autoEmbeddingManager: AutoEmbeddingManager
    private lateinit var embeddingRepository: EmbeddingRepository

    @Before
    fun setup() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, KeepLocalDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        embeddingRepository = EmbeddingRepository(context)
        autoEmbeddingManager = AutoEmbeddingManager(db.noteDao(), db.noteEmbeddingDao(), embeddingRepository)
        searchRepository = SearchRepository(db.noteDao(), db.noteEmbeddingDao(), embeddingRepository)
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testHybridRrfSearch() = runBlocking {
        val note1 = NoteEntity(
            title = "Groceries",
            content = "Buy organic apples and bananas",
            noteType = NoteType.CHECKLIST
        )
        val note2 = NoteEntity(
            title = "Apples Project",
            content = "Research fruit distribution network",
            noteType = NoteType.PROJECT_TASK
        )

        val id1 = db.noteDao().insertNote(note1)
        val id2 = db.noteDao().insertNote(note2)

        // Run auto-embedding pass
        val updated = autoEmbeddingManager.runEmbeddingPass()
        assertEquals(2, updated)

        // Perform Hybrid RRF Search
        val results = searchRepository.searchNotes("apples", SearchMode.HYBRID)
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.note.id == id1 })
        assertTrue(results.any { it.note.id == id2 })
    }
}
