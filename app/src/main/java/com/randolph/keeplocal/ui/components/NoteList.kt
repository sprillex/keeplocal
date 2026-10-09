package com.randolph.keeplocal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.randolph.keeplocal.data.local.entity.NoteEntity
import com.randolph.keeplocal.ui.notes.LayoutType

@Composable
fun NoteList(
    notes: List<NoteEntity>,
    layoutType: LayoutType,
    isArchiveView: Boolean = false,
    onNoteClick: (NoteEntity) -> Unit,
    onNoteLongClick: (NoteEntity) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp)
) {
    val pinnedNotes = notes.filter { it.isPinned }
    val unpinnedNotes = notes.filter { !it.isPinned }
    val hasPinned = pinnedNotes.isNotEmpty() && !isArchiveView

    if (layoutType == LayoutType.GRID) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalItemSpacing = 12.dp
        ) {
            if (hasPinned) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        text = "PINNED",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp, top = 4.dp)
                    )
                }
                items(pinnedNotes, key = { "pinned_${it.id}_${it.updatedAt}" }) { note ->
                    NoteCard(
                        note = note,
                        onClick = { onNoteClick(note) },
                        onLongClick = { onNoteLongClick(note) }
                    )
                }
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        text = "OTHERS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
                    )
                }
                items(unpinnedNotes, key = { "unpinned_${it.id}_${it.updatedAt}" }) { note ->
                    NoteCard(
                        note = note,
                        onClick = { onNoteClick(note) },
                        onLongClick = { onNoteLongClick(note) }
                    )
                }
            } else {
                items(notes, key = { "${it.id}_${it.updatedAt}" }) { note ->
                    NoteCard(
                        note = note,
                        onClick = { onNoteClick(note) },
                        onLongClick = { onNoteLongClick(note) }
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (hasPinned) {
                item {
                    Text(
                        text = "PINNED",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp, top = 4.dp)
                    )
                }
                items(pinnedNotes, key = { "pinned_${it.id}_${it.updatedAt}" }) { note ->
                    NoteCard(
                        note = note,
                        onClick = { onNoteClick(note) },
                        onLongClick = { onNoteLongClick(note) }
                    )
                }
                item {
                    Text(
                        text = "OTHERS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
                    )
                }
                items(unpinnedNotes, key = { "unpinned_${it.id}_${it.updatedAt}" }) { note ->
                    NoteCard(
                        note = note,
                        onClick = { onNoteClick(note) },
                        onLongClick = { onNoteLongClick(note) }
                    )
                }
            } else {
                items(notes, key = { "${it.id}_${it.updatedAt}" }) { note ->
                    NoteCard(
                        note = note,
                        onClick = { onNoteClick(note) },
                        onLongClick = { onNoteLongClick(note) }
                    )
                }
            }
        }
    }
}
