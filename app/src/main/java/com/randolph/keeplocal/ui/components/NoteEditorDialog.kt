package com.randolph.keeplocal.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.randolph.keeplocal.data.local.entity.NoteEntity
import com.randolph.keeplocal.data.local.entity.NoteType
import com.randolph.keeplocal.data.repository.SearchResult

@Composable
fun NoteEditorDialog(
    note: NoteEntity?,
    relatedContextMatches: List<SearchResult>,
    onDismiss: () -> Unit,
    onSave: (id: Long, title: String, content: String, noteType: NoteType) -> Unit,
    onDelete: ((NoteEntity) -> Unit)? = null
) {
    var title by remember(note) { mutableStateOf(note?.title ?: "") }
    var content by remember(note) { mutableStateOf(note?.content ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank() || content.isNotBlank()) {
                        onSave(note?.id ?: 0, title, content, note?.noteType ?: NoteType.TEXT)
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
            Text(if (note == null) "New Note" else "Edit Note")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Note Content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )

                if (relatedContextMatches.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    RelatedContextDrawer(
                        relatedMatches = relatedContextMatches,
                        onMatchClick = { match ->
                            title = match.note.title
                            content = match.note.content
                        }
                    )
                }
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
