package com.randolph.keeplocal.data.nas

import com.randolph.keeplocal.data.local.entity.NoteEntity
import com.randolph.keeplocal.data.local.entity.NoteType
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

data class BackupManifest(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val noteCount: Int,
    val sha256: String
)

class BackupArchiver {

    fun serializeNotesToJson(notes: List<NoteEntity>): String {
        val jsonArray = JSONArray()
        for (note in notes) {
            val jsonObject = JSONObject().apply {
                put("id", note.id)
                put("title", note.title)
                put("content", note.content)
                put("noteType", note.noteType.name)
                put("isPinned", note.isPinned)
                put("isArchived", note.isArchived)
                put("colorHex", note.colorHex ?: JSONObject.NULL)
                val imagesArray = JSONArray()
                note.imageUris.forEach { imagesArray.put(it) }
                put("imageUris", imagesArray)
                put("reminderEpochMs", note.reminderEpochMs ?: JSONObject.NULL)
                put("updatedAt", note.updatedAt)
            }
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString(2)
    }

    fun deserializeNotesFromJson(jsonString: String): List<NoteEntity> {
        val notes = mutableListOf<NoteEntity>()
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val imagesList = mutableListOf<String>()
            if (obj.has("imageUris") && !obj.isNull("imageUris")) {
                val imgsArr = obj.getJSONArray("imageUris")
                for (j in 0 until imgsArr.length()) {
                    imagesList.add(imgsArr.getString(j))
                }
            }
            val note = NoteEntity(
                id = obj.optLong("id", 0),
                title = obj.getString("title"),
                content = obj.getString("content"),
                noteType = NoteType.valueOf(obj.getString("noteType")),
                isPinned = obj.optBoolean("isPinned", false),
                isArchived = obj.optBoolean("isArchived", false),
                colorHex = if (obj.isNull("colorHex")) null else obj.optString("colorHex"),
                imageUris = imagesList,
                reminderEpochMs = if (obj.isNull("reminderEpochMs")) null else obj.optLong("reminderEpochMs"),
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
            )
            notes.add(note)
        }
        return notes
    }

    fun createBackupArchive(notes: List<NoteEntity>, outputFile: File): BackupManifest {
        val notesJson = serializeNotesToJson(notes)
        val jsonBytes = notesJson.toByteArray(Charsets.UTF_8)

        val digest = MessageDigest.getInstance("SHA-256")
        val sha256 = digest.digest(jsonBytes).joinToString("") { "%02x".format(it) }

        val manifest = BackupManifest(
            noteCount = notes.size,
            sha256 = sha256
        )

        // Write compressed archive payload
        FileOutputStream(outputFile).use { fos ->
            BufferedOutputStream(fos).use { bos ->
                GZIPOutputStream(bos).use { gzos ->
                    gzos.write(jsonBytes)
                    gzos.flush()
                }
            }
        }

        return manifest
    }

    fun unpackBackupArchive(archiveFile: File): Pair<BackupManifest, List<NoteEntity>> {
        val decompressedBytes = FileInputStream(archiveFile).use { fis ->
            BufferedInputStream(fis).use { bis ->
                GZIPInputStream(bis).use { gzis ->
                    gzis.readBytes()
                }
            }
        }

        val jsonString = String(decompressedBytes, Charsets.UTF_8)
        val notes = deserializeNotesFromJson(jsonString)

        val digest = MessageDigest.getInstance("SHA-256")
        val sha256 = digest.digest(decompressedBytes).joinToString("") { "%02x".format(it) }

        val manifest = BackupManifest(
            noteCount = notes.size,
            sha256 = sha256
        )

        return Pair(manifest, notes)
    }
}
