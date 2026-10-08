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
    private var relatedContextJob: Job? = null

    init {
        loadNotes()
    }

    private fun loadNotes() {
        viewModelScope.launch {
            val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000L
            val cutoff = System.currentTimeMillis() - thirtyDaysMs
            noteDao.purgeOldTrashedNotes(cutoff)
        }
        viewModelScope.launch {
            noteDao.getAllActiveNotes().collectLatest { activeNotes ->
                _uiState.update { it.copy(notes = activeNotes) }
            }
        }
        viewModelScope.launch {
            noteDao.getTrashedNotes().collectLatest { trash ->
                _uiState.update { it.copy(trashedNotes = trash) }
            }
        }
    }

    fun toggleLayoutType() {
        _uiState.update {
            val newLayout = if (it.layoutType == LayoutType.GRID) LayoutType.LIST else LayoutType.GRID
            it.copy(layoutType = newLayout)
        }
    }

    fun setTrashView(isTrash: Boolean) {
        _uiState.update { it.copy(isTrashView = isTrash, searchQuery = "", searchResults = emptyList()) }
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
        isPinned: Boolean = false,
        colorHex: String? = null
    ) {
        viewModelScope.launch {
            val existing = if (id != 0L) noteDao.getNoteById(id) else null
            val note = NoteEntity(
                id = id,
                title = title,
                content = content,
                noteType = noteType,
                isPinned = isPinned,
                isArchived = existing?.isArchived ?: false,
                isDeleted = false,
                deletedAtEpochMs = null,
                colorHex = colorHex,
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
            val updated = note.copy(isArchived = !note.isArchived, updatedAt = System.currentTimeMillis())
            noteDao.updateNote(updated)
        }
    }

    fun softDeleteNote(note: NoteEntity) {
        viewModelScope.launch {
            val updated = note.copy(
                isDeleted = true,
                deletedAtEpochMs = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            noteDao.updateNote(updated)
        }
    }

    fun restoreNote(note: NoteEntity) {
        viewModelScope.launch {
            val updated = note.copy(
                isDeleted = false,
                deletedAtEpochMs = null,
                updatedAt = System.currentTimeMillis()
            )
            noteDao.updateNote(updated)
        }
    }

    fun permanentlyDeleteNote(note: NoteEntity) {
        viewModelScope.launch {
            noteDao.deleteNote(note)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            noteDao.emptyTrash()
        }
    }

    fun openNoteDialog(note: NoteEntity) {
        _uiState.update { it.copy(selectedNoteForDialog = note) }
    }

    fun dismissNoteDialog() {
        _uiState.update { it.copy(selectedNoteForDialog = null) }
    }

    fun promptPermanentDelete(note: NoteEntity) {
        _uiState.update {
            it.copy(
                noteToPermanentlyDelete = note,
                showPermanentDeleteConfirmDialog = true
            )
        }
    }

    fun dismissPermanentDeletePrompt() {
        _uiState.update {
            it.copy(
                noteToPermanentlyDelete = null,
                showPermanentDeleteConfirmDialog = false
            )
        }
    }

    fun promptEmptyTrash() {
        _uiState.update { it.copy(showEmptyTrashConfirmDialog = true) }
    }

    fun dismissEmptyTrashPrompt() {
        _uiState.update { it.copy(showEmptyTrashConfirmDialog = false) }
    }

    fun selectNoteForEditing(note: NoteEntity?) {
        _uiState.update { it.copy(selectedNoteForEditing = note) }
        if (note != null) {
            loadRelatedContextForDraft(note.title, note.content)
        } else {
            _uiState.update { it.copy(relatedContextNotes = emptyList()) }
        }
    }

    fun loadRelatedContextForDraft(title: String, content: String) {
        relatedContextJob?.cancel()
        val queryText = if (title.isNotBlank()) "$title $content" else content
        if (queryText.isBlank()) {
            _uiState.update { it.copy(relatedContextNotes = emptyList()) }
            return
        }

        relatedContextJob = viewModelScope.launch {
            delay(200) // Debounce related context query
            val currentId = _uiState.value.selectedNoteForEditing?.id ?: 0L
            val matches = searchRepository.executeSemanticSearch(queryText)
                .filter { it.note.id != currentId }
                .take(3)
            _uiState.update { it.copy(relatedContextNotes = matches) }
        }
    }
}
