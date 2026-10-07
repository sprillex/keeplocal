package com.randolph.keeplocal.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.randolph.keeplocal.data.local.dao.NoteDao
import com.randolph.keeplocal.data.local.entity.NoteEntity
import com.randolph.keeplocal.data.local.entity.NoteType
import com.randolph.keeplocal.data.repository.AutoEmbeddingManager
import com.randolph.keeplocal.data.repository.SearchMode
import com.randolph.keeplocal.data.repository.SearchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotesViewModel(
    private val noteDao: NoteDao,
    private val searchRepository: SearchRepository,
    private val autoEmbeddingManager: AutoEmbeddingManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadNotes()
    }

    private fun loadNotes() {
        viewModelScope.launch {
            noteDao.getAllActiveNotes().collectLatest { activeNotes ->
                _uiState.update { it.copy(notes = activeNotes) }
            }
        }
    }

    fun toggleLayoutType() {
        _uiState.update {
            val newLayout = if (it.layoutType == LayoutType.GRID) LayoutType.LIST else LayoutType.GRID
            it.copy(layoutType = newLayout)
        }
    }

    fun setSearchQuery(query: String, debounceMs: Long = 300L): Job {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        val job = viewModelScope.launch {
            if (debounceMs > 0) delay(debounceMs)
            executeSearch(query, _uiState.value.searchMode)
        }
        searchJob = job
        return job
    }

    fun setSearchMode(mode: SearchMode): Job {
        _uiState.update { it.copy(searchMode = mode) }
        searchJob?.cancel()
        val job = viewModelScope.launch {
            executeSearch(_uiState.value.searchQuery, mode)
        }
        searchJob = job
        return job
    }

    private suspend fun executeSearch(query: String, mode: SearchMode) {
        if (query.isBlank()) {
            _uiState.update { it.copy(isSearching = false, searchResults = emptyList()) }
            return
        }

        _uiState.update { it.copy(isSearching = true) }
        val results = searchRepository.searchNotes(query, mode)
        _uiState.update { it.copy(searchResults = results, isSearching = false) }
    }

    fun saveNote(
        id: Long = 0,
        title: String,
        content: String,
        noteType: NoteType = NoteType.TEXT,
        isPinned: Boolean = false
    ) {
        viewModelScope.launch {
            val note = NoteEntity(
                id = id,
                title = title,
                content = content,
                noteType = noteType,
                isPinned = isPinned,
                updatedAt = System.currentTimeMillis()
            )
            val savedId = noteDao.insertNote(note)
            val updatedNote = note.copy(id = savedId)

            // Auto-embed note in background
            autoEmbeddingManager.embedAndSaveNote(updatedNote)
        }
    }

    fun togglePinNote(note: NoteEntity) {
        viewModelScope.launch {
            val updated = note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis())
            noteDao.updateNote(updated)
        }
    }

    fun archiveNote(note: NoteEntity) {
        viewModelScope.launch {
            val updated = note.copy(isArchived = true, updatedAt = System.currentTimeMillis())
            noteDao.updateNote(updated)
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            noteDao.deleteNote(note)
        }
    }

    fun selectNoteForEditing(note: NoteEntity?) {
        _uiState.update { it.copy(selectedNoteForEditing = note) }
        if (note != null) {
            loadRelatedContext(note)
        } else {
            _uiState.update { it.copy(relatedContextNotes = emptyList()) }
        }
    }

    private fun loadRelatedContext(note: NoteEntity) {
        viewModelScope.launch {
            val queryText = if (note.title.isNotBlank()) "${note.title} ${note.content}" else note.content
            if (queryText.isBlank()) return@launch

            val matches = searchRepository.executeSemanticSearch(queryText)
                .filter { it.note.id != note.id }
                .take(3)
            _uiState.update { it.copy(relatedContextNotes = matches) }
        }
    }
}
