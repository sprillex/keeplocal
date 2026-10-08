package com.randolph.keeplocal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.randolph.keeplocal.data.local.entity.NoteEntity
import com.randolph.keeplocal.ui.notes.LayoutType

@Composable
fun NoteList(
    notes: List<NoteEntity>,
    layoutType: LayoutType,
    onNoteClick: (NoteEntity) -> Unit,
    onNoteLongClick: (NoteEntity) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp)
) {
    if (layoutType == LayoutType.GRID) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalItemSpacing = 12.dp
        ) {
            items(notes, key = { "${it.id}_${it.updatedAt}" }) { note ->
                NoteCard(
                    note = note,
                    onClick = { onNoteClick(note) },
                    onLongClick = { onNoteLongClick(note) }
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
