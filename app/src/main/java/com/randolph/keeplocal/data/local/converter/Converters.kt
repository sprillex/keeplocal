package com.randolph.keeplocal.data.local.converter

import androidx.room.TypeConverter
import com.randolph.keeplocal.data.local.entity.NoteType

class Converters {
    @TypeConverter
    fun fromNoteType(type: NoteType): String {
        return type.name
    }

    @TypeConverter
    fun toNoteType(value: String): NoteType {
        return try {
            NoteType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            NoteType.TEXT
        }
    }
}
