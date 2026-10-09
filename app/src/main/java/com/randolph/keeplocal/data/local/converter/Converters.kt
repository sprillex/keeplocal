package com.randolph.keeplocal.data.local.converter

import androidx.room.TypeConverter
import com.randolph.keeplocal.data.local.entity.NoteType
import org.json.JSONArray

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

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        if (list.isNullOrEmpty()) return ""
        val jsonArray = JSONArray()
        list.forEach { jsonArray.put(it) }
        return jsonArray.toString()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            val jsonArray = JSONArray(value)
            List(jsonArray.length()) { jsonArray.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
