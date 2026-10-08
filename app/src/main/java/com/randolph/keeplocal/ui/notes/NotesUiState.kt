package com.randolph.keeplocal.ui.notes

import com.randolph.keeplocal.data.local.entity.NoteEntity
import com.randolph.keeplocal.data.repository.SearchMode
import com.randolph.keeplocal.data.repository.SearchResult

enum class LayoutType {
    GRID,
    LIST
}

data class NotesUiState(
    val notes: List<NoteEntity> = emptyList(),
    val searchResults: List<SearchResult> = emptyList(),
    val searchQuery: String = "",
    val searchMode: SearchMode = SearchMode.HYBRID,
    val isSearching: Boolean = false,
    val layoutType: LayoutType = LayoutType.GRID,
    val selectedNoteForEditing: NoteEntity? = null,
    val relatedContextNotes: List<SearchResult> = emptyList(),
    val isLoading: Boolean = false
)
