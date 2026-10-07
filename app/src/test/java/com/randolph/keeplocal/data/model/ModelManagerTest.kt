package com.randolph.keeplocal.data.model

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
class ModelManagerTest {

    private lateinit var context: Context
    private lateinit var modelManager: ModelManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        modelManager = ModelManager(context)
        cleanUp()
    }

    @After
    fun cleanUp() {
        val modelFile = modelManager.getModelFile()
        if (modelFile.exists()) {
            modelFile.delete()
        }
        val tokenizerFile = modelManager.getTokenizeFile()
        if (tokenizerFile.exists()) {
            tokenizerFile.delete()
        }
    }

    @Test
    fun testModelAndTokenizerFileDestinations() {
        val dir = modelManager.getModelDirectory()
        assertTrue(dir.exists())

        val modelFile = modelManager.getModelFile()
        assertEquals("embeddinggemma_qat_int8.tflite", modelFile.name)
        assertEquals(context.filesDir.resolve("models/embeddinggemma_qat_int8.tflite").absolutePath, modelFile.absolutePath)

        val tokenizerFile = modelManager.getTokenizeFile()
        assertEquals("sentencepiece.model", tokenizerFile.name)
        assertEquals(context.filesDir.resolve("models/sentencepiece.model").absolutePath, tokenizerFile.absolutePath)

        assertFalse(modelManager.isModelAndTokenizerReady())
    }

    @Test
    fun testVerificationIntegrityChecks() {
        val modelFile = modelManager.getModelFile()
        modelFile.writeBytes("Mock Model Data".toByteArray(Charsets.UTF_8))
        RandomAccessFile(modelFile, "rw").use { it.setLength(ModelManager.MODEL_EXPECTED_SIZE) }

        assertTrue(modelManager.verifyModelFile(modelFile))

        val tokenizerFile = modelManager.getTokenizeFile()
        tokenizerFile.writeBytes("Mock Tokenizer Data".toByteArray(Charsets.UTF_8))
        RandomAccessFile(tokenizerFile, "rw").use { it.setLength(ModelManager.TOKENIZER_EXPECTED_SIZE) }

        assertTrue(modelManager.verifyTokenizerFile(tokenizerFile))
        assertTrue(modelManager.isModelAndTokenizerReady())
    }

    @Test
    fun testImportModelFromInputStreamSuccess() = runBlocking {
        val mockWeights = "Mock EmbeddingGemma Model Weights 300M".toByteArray(Charsets.UTF_8)
        val expectedSha256 = MessageDigest.getInstance("SHA-256").digest(mockWeights)
            .joinToString("") { "%02x".format(it) }

        val inputStream = ByteArrayInputStream(mockWeights)
        val success = modelManager.importModelFromInputStream(inputStream, expectedSha256)

        assertTrue(success)
        assertTrue(modelManager.getModelFile().exists())
    }
}
