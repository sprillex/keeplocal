package com.randolph.keeplocal.ui.notes

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.randolph.keeplocal.data.local.KeepLocalDatabase
import com.randolph.keeplocal.data.local.entity.NoteEntity
import com.randolph.keeplocal.data.local.entity.NoteType
import com.randolph.keeplocal.data.repository.AutoEmbeddingManager
import com.randolph.keeplocal.data.repository.EmbeddingRepository
import com.randolph.keeplocal.data.repository.SearchMode
import com.randolph.keeplocal.data.repository.SearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class NotesViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var db: KeepLocalDatabase
    private lateinit var autoEmbeddingManager: AutoEmbeddingManager
    private lateinit var searchRepository: SearchRepository
    private lateinit var viewModel: NotesViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, KeepLocalDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val embeddingRepository = EmbeddingRepository(context)
        autoEmbeddingManager = AutoEmbeddingManager(db.noteDao(), db.noteEmbeddingDao(), embeddingRepository)
        searchRepository = SearchRepository(db.noteDao(), db.noteEmbeddingDao(), embeddingRepository)

        viewModel = NotesViewModel(db.noteDao(), searchRepository, autoEmbeddingManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun testToggleLayoutType() {
        assertEquals(LayoutType.GRID, viewModel.uiState.value.layoutType)
        viewModel.toggleLayoutType()
        assertEquals(LayoutType.LIST, viewModel.uiState.value.layoutType)
        viewModel.toggleLayoutType()
        assertEquals(LayoutType.GRID, viewModel.uiState.value.layoutType)
    }

    @Test
    fun testSaveAndSearchNotes() = runTest {
        val note = NoteEntity(
            title = "Local AI Search",
            content = "Test note with vector search capabilities.",
            noteType = NoteType.TEXT
        )
        val savedId = db.noteDao().insertNote(note)
        val savedNote = note.copy(id = savedId)
        autoEmbeddingManager.embedAndSaveNote(savedNote)

        viewModel.setSearchMode(SearchMode.HYBRID)
        viewModel.setSearchQuery("Search", debounceMs = 0L).join()

        val searchResults = viewModel.uiState.value.searchResults
        assertTrue("Search results should not be empty", searchResults.isNotEmpty())
        assertEquals("Local AI Search", searchResults[0].note.title)
    }

    @Test
    fun testSoftDeleteAndRestoreNote() = runTest {
        val note = NoteEntity(
            title = "Note to Soft Delete",
            content = "Content to move to trash.",
            noteType = NoteType.TEXT
        )
        val savedId = db.noteDao().insertNote(note)
        val savedNote = note.copy(id = savedId)

        val active1 = db.noteDao().getAllActiveNotes().first()
        assertEquals(1, active1.size)

        // Soft delete
        db.noteDao().updateNote(savedNote.copy(isDeleted = true, deletedAtEpochMs = System.currentTimeMillis()))

        val active2 = db.noteDao().getAllActiveNotes().first()
        val trashed2 = db.noteDao().getTrashedNotes().first()

        assertEquals(0, active2.size)
        assertEquals(1, trashed2.size)
        assertTrue(trashed2[0].isDeleted)

        // Restore
        db.noteDao().updateNote(trashed2[0].copy(isDeleted = false, deletedAtEpochMs = null))

        val active3 = db.noteDao().getAllActiveNotes().first()
        val trashed3 = db.noteDao().getTrashedNotes().first()

        assertEquals(1, active3.size)
        assertEquals(0, trashed3.size)
        assertFalse(active3[0].isDeleted)
    }

    @Test
    fun testPermanentDeleteAndEmptyTrash() = runTest {
        val note1 = NoteEntity(title = "Note 1", content = "Test 1", noteType = NoteType.TEXT, isDeleted = true, deletedAtEpochMs = System.currentTimeMillis())
        val note2 = NoteEntity(title = "Note 2", content = "Test 2", noteType = NoteType.TEXT, isDeleted = true, deletedAtEpochMs = System.currentTimeMillis())
        val id1 = db.noteDao().insertNote(note1)
        val id2 = db.noteDao().insertNote(note2)

        val savedNote1 = note1.copy(id = id1)

        val trashed1 = db.noteDao().getTrashedNotes().first()
        assertEquals(2, trashed1.size)

        // Permanently delete note 1
        db.noteDao().deleteNote(savedNote1)

        val trashed2 = db.noteDao().getTrashedNotes().first()
        assertEquals(1, trashed2.size)
        assertEquals(id2, trashed2[0].id)

        // Empty trash
        db.noteDao().emptyTrash()

        val trashed3 = db.noteDao().getTrashedNotes().first()
        assertEquals(0, trashed3.size)
    }

    @Test
    fun testThirtyDayPurgeOldTrashOnStartup() = runTest {
        val thirtyOneDaysAgo = System.currentTimeMillis() - (31L * 24 * 60 * 60 * 1000L)
        val oldNote = NoteEntity(
            title = "Old Note",
            content = "Expired in trash",
            noteType = NoteType.TEXT,
            isDeleted = true,
            deletedAtEpochMs = thirtyOneDaysAgo
        )
        db.noteDao().insertNote(oldNote)

        val trashedBefore = db.noteDao().getTrashedNotes().first()
        assertEquals(1, trashedBefore.size)

        db.noteDao().purgeOldTrashedNotes(System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000L))

        val trashedAfter = db.noteDao().getTrashedNotes().first()
        assertEquals(0, trashedAfter.size)
    }

    @Test
    fun testNoteDialogStateHandling() {
        val note = NoteEntity(title = "Dialog Note", content = "Test", noteType = NoteType.TEXT, id = 123)

        viewModel.openNoteDialog(note)
        assertEquals(note, viewModel.uiState.value.selectedNoteForDialog)

        viewModel.dismissNoteDialog()
        assertNull(viewModel.uiState.value.selectedNoteForDialog)

        viewModel.promptPermanentDelete(note)
        assertTrue(viewModel.uiState.value.showPermanentDeleteConfirmDialog)
        assertEquals(note, viewModel.uiState.value.noteToPermanentlyDelete)

        viewModel.dismissPermanentDeletePrompt()
        assertFalse(viewModel.uiState.value.showPermanentDeleteConfirmDialog)
        assertNull(viewModel.uiState.value.noteToPermanentlyDelete)
    }

    @Test
    fun testPinUnpinAndEditExistingNote() = runTest {
        val note = NoteEntity(title = "Original Title", content = "Original Content", noteType = NoteType.TEXT)
        val id = db.noteDao().insertNote(note)
        val saved = note.copy(id = id)

        // Toggle pin and wait for completion
        viewModel.togglePinNote(saved).join()

        val updated = db.noteDao().getNoteById(id)
        assertTrue("Note should be pinned", updated?.isPinned == true)

        // Save edit to existing note without generating new ID or losing vector
        viewModel.saveNote(
            id = id,
            title = "Edited Title",
            content = "Edited Content",
            noteType = NoteType.TEXT,
            isPinned = true
        ).join()

        val edited = db.noteDao().getNoteById(id)
        assertEquals("Edited Title", edited?.title)
        assertEquals("Edited Content", edited?.content)
        assertTrue("Edited note should maintain pin state", edited?.isPinned == true)
    }

    @Test
    fun testArchiveAndUnarchiveNote() = runTest {
        val note = NoteEntity(title = "Archive Test", content = "Content", noteType = NoteType.TEXT)
        val id = db.noteDao().insertNote(note)
        val saved = note.copy(id = id)

        // Archive note
        viewModel.archiveNote(saved).join()

        val active = db.noteDao().getAllActiveNotes().first()
        val archived = db.noteDao().getArchivedNotes().first()

        assertEquals(0, active.size)
        assertEquals(1, archived.size)
        assertTrue(archived[0].isArchived)

        // Unarchive note
        viewModel.archiveNote(archived[0]).join()

        val active2 = db.noteDao().getAllActiveNotes().first()
        val archived2 = db.noteDao().getArchivedNotes().first()

        assertEquals(1, active2.size)
        assertEquals(0, archived2.size)
        assertFalse(active2[0].isArchived)
    }
}
