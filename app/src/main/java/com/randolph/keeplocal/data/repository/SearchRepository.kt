package com.randolph.keeplocal.data.repository

import com.randolph.keeplocal.data.local.dao.NoteDao
import com.randolph.keeplocal.data.local.dao.NoteEmbeddingDao
import com.randolph.keeplocal.data.local.entity.NoteEntity
import com.randolph.keeplocal.util.VectorUtils
import kotlinx.coroutines.flow.first

enum class SearchMode {
    HYBRID,         // RRF (FTS + Vector)
    SEMANTIC_ONLY,  // Vector similarity
    EXACT_KEYWORD   // Room FTS5
}

data class SearchResult(
    val note: NoteEntity,
    val score: Float,
    val vectorSimilarity: Float? = null,
    val ftsRank: Int? = null
)

class SearchRepository(
    private val noteDao: NoteDao,
    private val noteEmbeddingDao: NoteEmbeddingDao,
    private val embeddingRepository: EmbeddingRepository
) {
    companion object {
        const val RRF_K = 60.0f
    }

    /**
     * Executes search based on the specified SearchMode.
     */
    suspend fun searchNotes(
        query: String,
        searchMode: SearchMode = SearchMode.HYBRID
    ): List<SearchResult> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            val allNotes = noteDao.getAllActiveNotes().first()
            return allNotes.map { SearchResult(note = it, score = 1.0f) }
        }

        return when (searchMode) {
            SearchMode.EXACT_KEYWORD -> executeFtsSearch(trimmedQuery)
            SearchMode.SEMANTIC_ONLY -> executeSemanticSearch(trimmedQuery)
            SearchMode.HYBRID -> executeHybridRrfSearch(trimmedQuery)
        }
    }

    suspend fun executeFtsSearch(query: String): List<SearchResult> {
        val ftsMatchQuery = sanitizeFtsQuery(query)
        val ftsNotes = noteDao.searchNotesFts(ftsMatchQuery).first()
        return ftsNotes.mapIndexed { index, note ->
            val score = 1.0f / (index + 1)
            SearchResult(
                note = note,
                score = score,
                ftsRank = index + 1
            )
        }
    }

    suspend fun executeSemanticSearch(query: String): List<SearchResult> {
        val formattedQuery = embeddingRepository.formatQueryInput(query)
        val queryVector = embeddingRepository.generateEmbedding(formattedQuery)

        val embeddings = noteEmbeddingDao.getAllEmbeddings()
        val noteScores = mutableListOf<Pair<Long, Float>>()

        for (item in embeddings) {
            val vector = VectorUtils.byteArrayToFloatArray(item.vector)
            val similarity = VectorUtils.dotProduct(queryVector, vector)
            noteScores.add(item.noteId to similarity)
        }

        // Sort descending by similarity score
        noteScores.sortByDescending { it.second }

        val results = mutableListOf<SearchResult>()
        for (pair in noteScores) {
            val note = noteDao.getNoteById(pair.first)
            if (note != null && !note.isArchived && !note.isDeleted) {
                results.add(
                    SearchResult(
                        note = note,
                        score = pair.second,
                        vectorSimilarity = pair.second
                    )
                )
            }
        }
        return results
    }

    suspend fun executeHybridRrfSearch(query: String): List<SearchResult> {
        val ftsResults = executeFtsSearch(query)
        val semanticResults = executeSemanticSearch(query)

        // Maps noteId -> cumulative RRF score
        val rrfScores = mutableMapOf<Long, Float>()
        val ftsRanks = mutableMapOf<Long, Int>()
        val vectorSimilarities = mutableMapOf<Long, Float>()
        val notesMap = mutableMapOf<Long, NoteEntity>()

        // Process FTS rankings
        ftsResults.forEachIndexed { rankIndex, item ->
            val noteId = item.note.id
            val rank = rankIndex + 1
            val rrfContribution = 1.0f / (RRF_K + rank)
            rrfScores[noteId] = (rrfScores[noteId] ?: 0.0f) + rrfContribution
            ftsRanks[noteId] = rank
            notesMap[noteId] = item.note
        }

        // Process Semantic rankings
        semanticResults.forEachIndexed { rankIndex, item ->
            val noteId = item.note.id
            val rank = rankIndex + 1
            val rrfContribution = 1.0f / (RRF_K + rank)
            rrfScores[noteId] = (rrfScores[noteId] ?: 0.0f) + rrfContribution
            vectorSimilarities[noteId] = item.vectorSimilarity ?: 0.0f
            notesMap[noteId] = item.note
        }

        val combinedList = rrfScores.mapNotNull { (noteId, rrfScore) ->
            val note = notesMap[noteId] ?: noteDao.getNoteById(noteId)
            if (note != null && !note.isArchived && !note.isDeleted) {
                SearchResult(
                    note = note,
                    score = rrfScore,
                    vectorSimilarity = vectorSimilarities[noteId],
                    ftsRank = ftsRanks[noteId]
                )
            } else null
        }

        return combinedList.sortedByDescending { it.score }
    }

    private fun sanitizeFtsQuery(query: String): String {
        val clean = query.replace("\"", "").trim()
        val terms = clean.split("\\s+".toRegex()).filter { it.isNotBlank() }
        return terms.joinToString(" ") { "$it*" }
    }
}
