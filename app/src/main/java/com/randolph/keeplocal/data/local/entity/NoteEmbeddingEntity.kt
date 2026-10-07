package com.randolph.keeplocal.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "note_embeddings",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["noteId"], unique = true)]
)
data class NoteEmbeddingEntity(
    @PrimaryKey val noteId: Long,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val vector: ByteArray,
    val dimensions: Int = 128,
    val updatedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NoteEmbeddingEntity

        if (noteId != other.noteId) return false
        if (!vector.contentEquals(other.vector)) return false
        if (dimensions != other.dimensions) return false
        if (updatedAt != other.updatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = noteId.hashCode()
        result = 31 * result + vector.contentHashCode()
        result = 31 * result + dimensions
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}
