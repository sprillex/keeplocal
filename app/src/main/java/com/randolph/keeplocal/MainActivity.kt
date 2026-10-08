package com.randolph.keeplocal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.randolph.keeplocal.data.local.KeepLocalDatabase
import com.randolph.keeplocal.data.model.ModelManager
import com.randolph.keeplocal.data.nas.NasCredentialManager
import com.randolph.keeplocal.data.nas.SmbBackupManager
import com.randolph.keeplocal.data.repository.AutoEmbeddingManager
import com.randolph.keeplocal.data.repository.EmbeddingRepository
import com.randolph.keeplocal.data.repository.SearchRepository
import com.randolph.keeplocal.ui.components.KeepLocalSearchBar
import com.randolph.keeplocal.ui.components.NoteList
import com.randolph.keeplocal.ui.notes.NoteEditorScreen
import com.randolph.keeplocal.ui.notes.NotesViewModel
import com.randolph.keeplocal.ui.settings.ModelSettingsScreen
import com.randolph.keeplocal.ui.settings.NasBackupSettingsScreen
import com.randolph.keeplocal.ui.theme.KeepLocalTheme
import com.randolph.keeplocal.worker.NasBackupWorker

enum class Screen {
    MAIN_NOTES,
    NOTE_EDITOR,
    MODEL_SETTINGS,
    NAS_SETTINGS
}

class MainActivity : ComponentActivity() {

    private lateinit var database: KeepLocalDatabase
    private lateinit var modelManager: ModelManager
    private lateinit var nasCredentialManager: NasCredentialManager
    private lateinit var smbBackupManager: SmbBackupManager
    private lateinit var viewModel: NotesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = KeepLocalDatabase.getDatabase(applicationContext)
        modelManager = ModelManager(applicationContext)
        val embeddingRepository = EmbeddingRepository(applicationContext, modelManager)
        val autoEmbeddingManager = AutoEmbeddingManager(database.noteDao(), database.noteEmbeddingDao(), embeddingRepository)
        val searchRepository = SearchRepository(database.noteDao(), database.noteEmbeddingDao(), embeddingRepository)
        nasCredentialManager = NasCredentialManager(applicationContext)
        smbBackupManager = SmbBackupManager(database.noteDao(), nasCredentialManager, autoEmbeddingManager = autoEmbeddingManager)

        viewModel = NotesViewModel(database.noteDao(), searchRepository, autoEmbeddingManager)

        // Schedule automated overnight NAS backup with UNMETERED + CHARGING constraints
        NasBackupWorker.schedulePeriodicBackup(applicationContext)

        setContent {
            KeepLocalTheme {
                var currentScreen by remember { mutableStateOf(Screen.MAIN_NOTES) }
                val uiState by viewModel.uiState.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentScreen) {
                        Screen.MAIN_NOTES -> MainNotesScreen(
                            viewModel = viewModel,
                            onOpenEditor = { note ->
                                viewModel.selectNoteForEditing(note)
                                currentScreen = Screen.NOTE_EDITOR
                            },
                            onNavigateToModelSettings = { currentScreen = Screen.MODEL_SETTINGS },
                            onNavigateToNasSettings = { currentScreen = Screen.NAS_SETTINGS }
                        )
                        Screen.NOTE_EDITOR -> NoteEditorScreen(
                            note = uiState.selectedNoteForEditing,
                            relatedContextMatches = uiState.relatedContextNotes,
                            onBackAndSave = { id, title, content, type, isPinned, colorHex ->
                                if (title.isNotBlank() || content.isNotBlank()) {
                                    viewModel.saveNote(id, title, content, type, isPinned, colorHex)
                                }
                                currentScreen = Screen.MAIN_NOTES
                            },
                            onDelete = { note ->
                                viewModel.softDeleteNote(note)
                                currentScreen = Screen.MAIN_NOTES
                            },
                            onArchive = { note ->
                                viewModel.saveAndArchiveNote(
                                    id = note.id,
                                    title = note.title,
                                    content = note.content,
                                    noteType = note.noteType,
                                    isPinned = note.isPinned,
                                    colorHex = note.colorHex
                                )
                                currentScreen = Screen.MAIN_NOTES
                            },
                            onQueryRelatedContext = { title, content ->
                                viewModel.loadRelatedContextForDraft(title, content)
                            }
                        )
                        Screen.MODEL_SETTINGS -> ModelSettingsScreen(
                            modelManager = modelManager,
                            onBackClick = { currentScreen = Screen.MAIN_NOTES }
                        )
                        Screen.NAS_SETTINGS -> NasBackupSettingsScreen(
                            nasCredentialManager = nasCredentialManager,
                            smbBackupManager = smbBackupManager,
                            onBackClick = { currentScreen = Screen.MAIN_NOTES }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainNotesScreen(
    viewModel: NotesViewModel,
    onOpenEditor: (note: com.randolph.keeplocal.data.local.entity.NoteEntity?) -> Unit,
    onNavigateToModelSettings: () -> Unit,
    onNavigateToNasSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val displayedNotes = when {
        uiState.isTrashView -> uiState.trashedNotes
        uiState.isArchiveView -> uiState.archivedNotes
        uiState.searchQuery.isNotBlank() -> uiState.searchResults.map { it.note }
        else -> uiState.notes
    }

    Scaffold(
        floatingActionButton = {
            if (uiState.isTrashView) {
                FloatingActionButton(
                    onClick = { viewModel.promptEmptyTrash() },
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = "Empty Trash")
                }
            } else {
                FloatingActionButton(
                    onClick = { onOpenEditor(null) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "New Note")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.isTrashView || uiState.isArchiveView) {
                    IconButton(onClick = {
                        viewModel.setTrashView(false)
                        viewModel.setArchiveView(false)
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Main Notes",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = if (uiState.isTrashView) "Trash" else "Archive",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Text(
                        text = "KeepLocal",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = { viewModel.setArchiveView(true) }) {
                        Icon(
                            imageVector = Icons.Outlined.Archive,
                            contentDescription = "Archive",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { viewModel.setTrashView(true) }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Trash",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { onNavigateToModelSettings() }) {
                        Icon(
                            imageVector = Icons.Filled.Memory,
                            contentDescription = "AI Model Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { onNavigateToNasSettings() }) {
                        Icon(
                            imageVector = Icons.Filled.Storage,
                            contentDescription = "NAS Backup Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!uiState.isTrashView && !uiState.isArchiveView) {
                KeepLocalSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    searchMode = uiState.searchMode,
                    onSearchModeChange = { viewModel.setSearchMode(it) },
                    layoutType = uiState.layoutType,
                    onToggleLayout = { viewModel.toggleLayoutType() }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (displayedNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val emptyMessage = when {
                        uiState.isTrashView -> "Trash is empty."
                        uiState.isArchiveView -> "Archive is empty."
                        uiState.searchQuery.isNotBlank() -> "No matching notes found."
                        else -> "No notes yet. Tap + to add one!"
                    }
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                NoteList(
                    notes = displayedNotes,
                    layoutType = uiState.layoutType,
                    isArchiveView = uiState.isArchiveView,
                    onNoteClick = { note ->
                        if (uiState.isTrashView) {
                            viewModel.openNoteDialog(note)
                        } else {
                            onOpenEditor(note)
                        }
                    },
                    onNoteLongClick = { note ->
                        viewModel.openNoteDialog(note)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Dialog for Long-Pressed Note Options
    uiState.selectedNoteForDialog?.let { selectedNote ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissNoteDialog() },
            title = {
                Text(text = selectedNote.title.ifBlank { "Untitled Note" })
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!uiState.isTrashView) {
                        TextButton(
                            onClick = {
                                viewModel.togglePinNote(selectedNote)
                                viewModel.dismissNoteDialog()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (selectedNote.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Text(if (selectedNote.isPinned) "Unpin Note" else "Pin Note")
                            }
                        }

                        TextButton(
                            onClick = {
                                viewModel.archiveNote(selectedNote)
                                viewModel.dismissNoteDialog()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Archive,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Text(if (selectedNote.isArchived) "Unarchive Note" else "Archive Note")
                            }
                        }

                        TextButton(
                            onClick = {
                                viewModel.softDeleteNote(selectedNote)
                                viewModel.dismissNoteDialog()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Text("Delete (Move to Trash)", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else {
                        TextButton(
                            onClick = {
                                viewModel.restoreNote(selectedNote)
                                viewModel.dismissNoteDialog()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Restore,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Text("Restore Note")
                            }
                        }

                        TextButton(
                            onClick = {
                                viewModel.dismissNoteDialog()
                                viewModel.promptPermanentDelete(selectedNote)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteForever,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Text("Delete permanently", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.dismissNoteDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation Dialog for Permanent Delete
    if (uiState.showPermanentDeleteConfirmDialog && uiState.noteToPermanentlyDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPermanentDeletePrompt() },
            title = { Text("Delete permanently?") },
            text = { Text("This note will be permanently deleted and cannot be recovered.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        uiState.noteToPermanentlyDelete?.let { viewModel.permanentlyDeleteNote(it) }
                        viewModel.dismissPermanentDeletePrompt()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPermanentDeletePrompt() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation Dialog for Empty Trash
    if (uiState.showEmptyTrashConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissEmptyTrashPrompt() },
            title = { Text("Empty Trash?") },
            text = { Text("All notes in the trash will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.emptyTrash()
                        viewModel.dismissEmptyTrashPrompt()
                    }
                ) {
                    Text("Empty Trash", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissEmptyTrashPrompt() }) {
                    Text("Cancel")
                }
            }
        )
    }
}
