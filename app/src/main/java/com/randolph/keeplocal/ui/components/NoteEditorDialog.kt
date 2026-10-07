package com.randolph.keeplocal.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.randolph.keeplocal.data.local.entity.NoteEntity
import com.randolph.keeplocal.data.local.entity.NoteType
import com.randolph.keeplocal.data.repository.SearchResult

data class ChecklistItem(
    val isChecked: Boolean = false,
    val text: String = ""
)

object ChecklistParser {
    fun parseContentToChecklist(content: String): List<ChecklistItem> {
        if (content.isBlank()) return emptyList()
        val lines = content.lines()
        val items = mutableListOf<ChecklistItem>()
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("[x]") -> items.add(ChecklistItem(isChecked = true, text = trimmed.removePrefix("[x]").trim()))
                trimmed.startsWith("[X]") -> items.add(ChecklistItem(isChecked = true, text = trimmed.removePrefix("[X]").trim()))
                trimmed.startsWith("[ ]") -> items.add(ChecklistItem(isChecked = false, text = trimmed.removePrefix("[ ]").trim()))
                trimmed.isNotEmpty() -> items.add(ChecklistItem(isChecked = false, text = trimmed))
            }
        }
        return items
    }

    fun formatChecklistToContent(items: List<ChecklistItem>): String {
        return items.filter { it.text.isNotBlank() }
            .joinToString("\n") { item ->
                if (item.isChecked) "[x] ${item.text}" else "[ ] ${item.text}"
            }
    }
}

@Composable
fun NoteEditorDialog(
    note: NoteEntity?,
    relatedContextMatches: List<SearchResult>,
    onDismiss: () -> Unit,
    onSave: (id: Long, title: String, content: String, noteType: NoteType) -> Unit,
    onDelete: ((NoteEntity) -> Unit)? = null
) {
    var title by remember(note) { mutableStateOf(note?.title ?: "") }
    var selectedNoteType by remember(note) { mutableStateOf(note?.noteType ?: NoteType.TEXT) }
    var rawContent by remember(note) { mutableStateOf(note?.content ?: "") }

    val checklistItems = remember(note, selectedNoteType) {
        mutableStateListOf<ChecklistItem>().apply {
            if (selectedNoteType == NoteType.CHECKLIST || selectedNoteType == NoteType.PROJECT_TASK) {
                addAll(ChecklistParser.parseContentToChecklist(rawContent))
                if (isEmpty()) {
                    add(ChecklistItem(isChecked = false, text = ""))
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val finalContent = if (selectedNoteType == NoteType.CHECKLIST || selectedNoteType == NoteType.PROJECT_TASK) {
                        ChecklistParser.formatChecklistToContent(checklistItems)
                    } else {
                        rawContent
                    }

                    if (title.isNotBlank() || finalContent.isNotBlank()) {
                        onSave(note?.id ?: 0, title, finalContent, selectedNoteType)
                    }
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = {
            Text(if (note == null) "New Note / Task" else "Edit Note")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Note Type selector chips
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = selectedNoteType == NoteType.TEXT,
                        onClick = { selectedNoteType = NoteType.TEXT },
                        label = { Text("Text Note") }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    FilterChip(
                        selected = selectedNoteType == NoteType.CHECKLIST,
                        onClick = { selectedNoteType = NoteType.CHECKLIST },
                        label = { Text("Checklist") }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    FilterChip(
                        selected = selectedNoteType == NoteType.PROJECT_TASK,
                        onClick = { selectedNoteType = NoteType.PROJECT_TASK },
                        label = { Text("Task") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedNoteType == NoteType.CHECKLIST || selectedNoteType == NoteType.PROJECT_TASK) {
                    Text(
                        text = "Interactive Checklist Items:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        checklistItems.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.isChecked,
                                    onCheckedChange = { checked ->
                                        checklistItems[index] = item.copy(isChecked = checked)
                                    }
                                )
                                OutlinedTextField(
                                    value = item.text,
                                    onValueChange = { newText ->
                                        checklistItems[index] = item.copy(text = newText)
                                    },
                                    placeholder = { Text("List item ${index + 1}") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                IconButton(
                                    onClick = {
                                        if (checklistItems.size > 1) {
                                            checklistItems.removeAt(index)
                                        } else {
                                            checklistItems[0] = ChecklistItem()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Remove item",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { checklistItems.add(ChecklistItem()) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Item")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Item")
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = rawContent,
                        onValueChange = { rawContent = it },
                        label = { Text("Note Content") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }

                if (relatedContextMatches.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    RelatedContextDrawer(
                        relatedMatches = relatedContextMatches,
                        onMatchClick = { match ->
                            title = match.note.title
                            rawContent = match.note.content
                        }
                    )
                }
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
