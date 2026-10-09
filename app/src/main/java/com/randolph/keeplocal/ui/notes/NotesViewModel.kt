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
            try {
                val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000L
                val cutoff = System.currentTimeMillis() - thirtyDaysMs
                noteDao.purgeOldTrashedNotes(cutoff)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        viewModelScope.launch {
            try {
                noteDao.getAllActiveNotes().collectLatest { activeNotes ->
                    _uiState.update { it.copy(notes = activeNotes) }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        viewModelScope.launch {
            try {
                noteDao.getArchivedNotes().collectLatest { archived ->
                    _uiState.update { it.copy(archivedNotes = archived) }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        viewModelScope.launch {
            try {
                noteDao.getTrashedNotes().collectLatest { trash ->
                    _uiState.update { it.copy(trashedNotes = trash) }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun toggleLayoutType() {
        _uiState.update {
            val newLayout = if (it.layoutType == LayoutType.GRID) LayoutType.LIST else LayoutType.GRID
            it.copy(layoutType = newLayout)
        }
    }

    fun setArchiveView(isArchive: Boolean) {
        _uiState.update { it.copy(isArchiveView = isArchive, isTrashView = false, searchQuery = "", searchResults = emptyList()) }
    }

    fun setTrashView(isTrash: Boolean) {
        _uiState.update { it.copy(isTrashView = isTrash, isArchiveView = false, searchQuery = "", searchResults = emptyList()) }
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
        try {
            val results = searchRepository.searchNotes(query, mode)
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        } catch (e: Throwable) {
            e.printStackTrace()
            _uiState.update { it.copy(isSearching = false) }
        }
    }

    private fun refreshSearchIfSearching() {
        if (_uiState.value.searchQuery.isNotBlank()) {
            viewModelScope.launch {
                executeSearch(_uiState.value.searchQuery, _uiState.value.searchMode)
            }
        }
    }

    fun saveNote(
        id: Long = 0,
        title: String,
        content: String,
        noteType: NoteType = NoteType.TEXT,
        isPinned: Boolean = false,
        colorHex: String? = null
    ): Job = viewModelScope.launch {
        try {
            val existing = if (id != 0L) noteDao.getNoteById(id) else null
            val note = NoteEntity(
                id = id,
                title = title,
                content = content,
                noteType = noteType,
                isPinned = isPinned,
                isArchived = existing?.isArchived ?: false,
                isDeleted = existing?.isDeleted ?: false,
                deletedAtEpochMs = existing?.deletedAtEpochMs,
                colorHex = colorHex,
                updatedAt = System.currentTimeMillis()
            )
            val finalNote = if (id != 0L) {
                noteDao.updateNote(note)
                note
            } else {
                val savedId = noteDao.insertNote(note)
                note.copy(id = savedId)
            }

            // Auto-embed note in background
            autoEmbeddingManager.embedAndSaveNote(finalNote)
            refreshSearchIfSearching()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun saveAndArchiveNote(
        id: Long = 0,
        title: String,
        content: String,
        noteType: NoteType = NoteType.TEXT,
        isPinned: Boolean = false,
        colorHex: String? = null
    ): Job = viewModelScope.launch {
        try {
            val existing = if (id != 0L) noteDao.getNoteById(id) else null
            val newArchivedState = !(existing?.isArchived ?: false)
            val note = NoteEntity(
                id = id,
                title = title,
                content = content,
                noteType = noteType,
                isPinned = isPinned,
                isArchived = if (existing != null) newArchivedState else true,
                isDeleted = existing?.isDeleted ?: false,
                deletedAtEpochMs = existing?.deletedAtEpochMs,
                colorHex = colorHex,
                updatedAt = System.currentTimeMillis()
            )
            val finalNote = if (id != 0L) {
                noteDao.updateNote(note)
                note
            } else {
                val savedId = noteDao.insertNote(note)
                note.copy(id = savedId)
            }

            autoEmbeddingManager.embedAndSaveNote(finalNote)
            refreshSearchIfSearching()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun togglePinNote(note: NoteEntity): Job = viewModelScope.launch {
        try {
            val updated = note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis())
            noteDao.updateNote(updated)
            refreshSearchIfSearching()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun archiveNote(note: NoteEntity): Job = viewModelScope.launch {
        try {
            val updated = note.copy(isArchived = !note.isArchived, updatedAt = System.currentTimeMillis())
            noteDao.updateNote(updated)
            refreshSearchIfSearching()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun softDeleteNote(note: NoteEntity): Job = viewModelScope.launch {
        try {
            val updated = note.copy(
                isDeleted = true,
                deletedAtEpochMs = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            noteDao.updateNote(updated)
            refreshSearchIfSearching()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun restoreNote(note: NoteEntity) {
        viewModelScope.launch {
            try {
                val updated = note.copy(
                    isDeleted = false,
                    deletedAtEpochMs = null,
                    updatedAt = System.currentTimeMillis()
                )
                noteDao.updateNote(updated)
                refreshSearchIfSearching()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun permanentlyDeleteNote(note: NoteEntity) {
        viewModelScope.launch {
            try {
                noteDao.deleteNote(note)
                refreshSearchIfSearching()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            try {
                noteDao.emptyTrash()
                refreshSearchIfSearching()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
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
            try {
                val currentId = _uiState.value.selectedNoteForEditing?.id ?: 0L
                val matches = searchRepository.executeSemanticSearch(queryText)
                    .filter { it.note.id != currentId }
                    .take(3)
                _uiState.update { it.copy(relatedContextNotes = matches) }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}
