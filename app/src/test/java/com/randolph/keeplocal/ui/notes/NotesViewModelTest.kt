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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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

        var retries = 0
        while (viewModel.uiState.value.notes.isEmpty() && retries < 20) {
            delay(50)
            retries++
        }

        viewModel.setSearchMode(SearchMode.HYBRID)
        viewModel.setSearchQuery("Search", debounceMs = 0L).join()

        val searchResults = viewModel.uiState.value.searchResults
        assertTrue("Search results should not be empty", searchResults.isNotEmpty())
        assertEquals("Local AI Search", searchResults[0].note.title)
    }
}
