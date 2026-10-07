package com.randolph.keeplocal.data.repository

import android.content.Context
import android.os.ParcelFileDescriptor
import com.randolph.keeplocal.data.model.ModelManager
import com.randolph.keeplocal.util.VectorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.channels.FileChannel

class EmbeddingRepository(
    private val context: Context,
    private val modelManager: ModelManager = ModelManager(context)
) {
    companion object {
        const val DOCUMENT_PREFIX = "task: search document | text: "
        const val QUERY_PREFIX = "task: search query | query: "
        const val TARGET_DIMENSIONS = 128
    }

    private var interpreter: Interpreter? = null

    /**
     * Initializes the LiteRT / TFLite interpreter from internal storage.
     */
    @Synchronized
    fun initializeFromInternalStorage(): Boolean {
        val modelFile = modelManager.getModelFile()
        if (!modelFile.exists()) return false

        return try {
            close()
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(modelFile, options)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Initializes the LiteRT / TFLite interpreter from an external ParcelFileDescriptor
     * supplied via signature-protected FileProvider.
     */
    @Synchronized
    fun initializeFromFileDescriptor(pfd: ParcelFileDescriptor): Boolean {
        return try {
            close()
            val fileInputStream = FileInputStream(pfd.fileDescriptor)
            val fileChannel = fileInputStream.channel
            val mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size())

            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(mappedByteBuffer, options)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Formats text into asymmetric note indexing task representation:
     * "task: search document | text: $title. $content"
     */
    fun formatDocumentInput(title: String, content: String): String {
        val body = if (title.isNotBlank()) "$title. $content" else content
        return "$DOCUMENT_PREFIX$body"
    }

    /**
     * Formats query into asymmetric query task representation:
     * "task: search query | query: $query"
     */
    fun formatQueryInput(query: String): String {
        return "$QUERY_PREFIX$query"
    }

    /**
     * Executes inference on the text string and produces a 128-dim normalized embedding float array.
     * Falls back to pseudo-semantic embedding if the model file is not downloaded yet.
     */
    suspend fun generateEmbedding(text: String): FloatArray = withContext(Dispatchers.Default) {
        val currentInterpreter = interpreter
        if (currentInterpreter != null) {
            try {
                // Prepare input and output tensors
                // EmbeddingGemma output tensor shape is typically [1, 768] or [1, 128]
                val outputBuffer = Array(1) { FloatArray(768) }
                currentInterpreter.run(arrayOf(text), outputBuffer)
                return@withContext VectorUtils.sliceAndNormalize(outputBuffer[0], TARGET_DIMENSIONS)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback pseudo-embedding generation (deterministic hash vector) when model is not loaded
        return@withContext generateFallbackEmbedding(text)
    }

    /**
     * Generates a deterministic 128-dim normalized pseudo-embedding for testing or offline pre-download state.
     */
    fun generateFallbackEmbedding(text: String): FloatArray {
        val raw = FloatArray(768)
        val hashCode = text.hashCode()
        for (i in raw.indices) {
            val seed = hashCode + i * 31
            raw[i] = ((seed % 1000) / 1000.0f)
        }
        return VectorUtils.sliceAndNormalize(raw, TARGET_DIMENSIONS)
    }

    @Synchronized
    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
