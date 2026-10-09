package com.randolph.keeplocal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val noteType: NoteType,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAtEpochMs: Long? = null,
    val reminderEpochMs: Long? = null,
    val colorHex: String? = null,
    val imageUris: List<String> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
)
