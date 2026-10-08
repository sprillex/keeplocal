package com.randolph.keeplocal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.randolph.keeplocal.data.local.entity.NoteEmbeddingEntity

@Dao
interface NoteEmbeddingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbedding(embedding: NoteEmbeddingEntity)

    @Query("SELECT * FROM note_embeddings WHERE noteId = :noteId")
    suspend fun getEmbeddingForNote(noteId: Long): NoteEmbeddingEntity?

    @Query("SELECT * FROM note_embeddings")
    suspend fun getAllEmbeddings(): List<NoteEmbeddingEntity>

    @Query("DELETE FROM note_embeddings WHERE noteId = :noteId")
    suspend fun deleteEmbeddingForNote(noteId: Long)
}
