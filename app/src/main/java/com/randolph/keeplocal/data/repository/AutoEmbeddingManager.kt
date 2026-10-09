package com.randolph.keeplocal.data.repository

import com.randolph.keeplocal.data.local.dao.NoteDao
import com.randolph.keeplocal.data.local.dao.NoteEmbeddingDao
import com.randolph.keeplocal.data.local.entity.NoteEmbeddingEntity
import com.randolph.keeplocal.data.local.entity.NoteEntity
import com.randolph.keeplocal.util.VectorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class AutoEmbeddingManager(
    private val noteDao: NoteDao,
    private val noteEmbeddingDao: NoteEmbeddingDao,
    private val embeddingRepository: EmbeddingRepository
) {
    /**
     * Scans notes that do not have vector embeddings or whose embeddings are outdated,
     * computes their embeddings, and saves them to the Room note_embeddings table.
     */
    suspend fun runEmbeddingPass(): Int = withContext(Dispatchers.IO) {
        val allNotes = noteDao.getAllActiveNotes().first()
        var updatedCount = 0

        for (note in allNotes) {
            val existingEmbedding = noteEmbeddingDao.getEmbeddingForNote(note.id)
            if (existingEmbedding == null || existingEmbedding.updatedAt < note.updatedAt) {
                embedAndSaveNote(note)
                updatedCount++
            }
        }
        updatedCount
    }

    suspend fun embedAndSaveNote(note: NoteEntity) = withContext(Dispatchers.IO) {
        try {
            val formattedInput = embeddingRepository.formatDocumentInput(note.title, note.content)
            val vectorFloats = embeddingRepository.generateEmbedding(formattedInput)
            val vectorBytes = VectorUtils.floatArrayToByteArray(vectorFloats)

            val embeddingEntity = NoteEmbeddingEntity(
                noteId = note.id,
                vector = vectorBytes,
                dimensions = VectorUtils.DEFAULT_EMBEDDING_DIM,
                updatedAt = System.currentTimeMillis()
            )
            noteEmbeddingDao.insertEmbedding(embeddingEntity)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
}
