package com.randolph.keeplocal.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.randolph.keeplocal.data.local.dao.NoteDao
import com.randolph.keeplocal.data.local.dao.NoteEmbeddingDao
import com.randolph.keeplocal.data.local.entity.NoteEmbeddingEntity
import com.randolph.keeplocal.data.local.entity.NoteEntity
import com.randolph.keeplocal.data.local.entity.NoteType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KeepLocalDatabaseTest {

    private lateinit var db: KeepLocalDatabase
    private lateinit var noteDao: NoteDao
    private lateinit var noteEmbeddingDao: NoteEmbeddingDao

    @Before
    fun createDb() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, KeepLocalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        noteDao = db.noteDao()
        noteEmbeddingDao = db.noteEmbeddingDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetNote() = runBlocking {
        val note = NoteEntity(
            title = "Meeting Notes",
            content = "Discuss architecture and offline search.",
            noteType = NoteType.TEXT,
            isPinned = true
        )
        val noteId = noteDao.insertNote(note)

        val retrieved = noteDao.getNoteById(noteId)
        assertNotNull(retrieved)
        assertEquals("Meeting Notes", retrieved?.title)
        assertEquals("Discuss architecture and offline search.", retrieved?.content)
        assertEquals(NoteType.TEXT, retrieved?.noteType)
        assertTrue(retrieved?.isPinned == true)
    }

    @Test
    fun testUpdateNote() = runBlocking {
        val note = NoteEntity(
            title = "Original Title",
            content = "Original Content",
            noteType = NoteType.TEXT
        )
        val noteId = noteDao.insertNote(note)
        val savedNote = noteDao.getNoteById(noteId)!!

        val updatedNote = savedNote.copy(
            title = "Updated Title",
            isPinned = true,
            isArchived = true
        )
        noteDao.updateNote(updatedNote)

        val retrieved = noteDao.getNoteById(noteId)
        assertNotNull(retrieved)
        assertEquals("Updated Title", retrieved?.title)
        assertTrue(retrieved?.isPinned == true)
        assertTrue(retrieved?.isArchived == true)
    }

    @Test
    fun testFtsSearch() = runBlocking {
        val note1 = NoteEntity(
            title = "Shopping List",
            content = "Buy apples, bananas, and almond milk.",
            noteType = NoteType.CHECKLIST
        )
        val note2 = NoteEntity(
            title = "Project Plan",
            content = "Develop Android offline note taking application.",
            noteType = NoteType.PROJECT_TASK
        )

        val id1 = noteDao.insertNote(note1)
        val id2 = noteDao.insertNote(note2)

        // Query FTS table for keyword 'almond'
        val searchResults = noteDao.searchNotesFts("almond*").first()
        assertEquals(1, searchResults.size)
        assertEquals(id1, searchResults[0].id)
        assertEquals("Shopping List", searchResults[0].title)

        // Query FTS table for keyword 'Android'
        val androidResults = noteDao.searchNotesFts("Android*").first()
        assertEquals(1, androidResults.size)
        assertEquals(id2, androidResults[0].id)
    }

    @Test
    fun testNoteEmbeddingsForeignKeysAndOperations() = runBlocking {
        val note = NoteEntity(
            title = "Vector Note",
            content = "Embedding test note content",
            noteType = NoteType.TEXT
        )
        val noteId = noteDao.insertNote(note)

        val dummyVector = ByteArray(512) { (it % 128).toByte() }
        val embedding = NoteEmbeddingEntity(
            noteId = noteId,
            vector = dummyVector,
            dimensions = 128
        )
        noteEmbeddingDao.insertEmbedding(embedding)

        val retrievedEmbedding = noteEmbeddingDao.getEmbeddingForNote(noteId)
        assertNotNull(retrievedEmbedding)
        assertEquals(noteId, retrievedEmbedding?.noteId)
        assertEquals(128, retrievedEmbedding?.dimensions)
        assertTrue(dummyVector.contentEquals(retrievedEmbedding!!.vector))

        // Test cascade deletion
        noteDao.deleteNote(noteDao.getNoteById(noteId)!!)
        val deletedEmbedding = noteEmbeddingDao.getEmbeddingForNote(noteId)
        assertNull(deletedEmbedding)
    }
}
