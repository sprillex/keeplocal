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
        val tmpFile = File(modelManager.getModelDirectory(), "${ModelManager.MODEL_FILENAME}.tmp")
        if (tmpFile.exists()) {
            tmpFile.delete()
        }
    }

    @Test
    fun testModelFileLocationAndDirectoryCreation() {
        val dir = modelManager.getModelDirectory()
        assertTrue(dir.exists())
        assertTrue(dir.isDirectory)

        val file = modelManager.getModelFile()
        assertEquals("embeddinggemma-300M_seq256_mixed-precision.tflite", file.name)
        assertFalse(modelManager.isModelAvailable())
    }

    @Test
    fun testSha256Computation() {
        val testFile = File(modelManager.getModelDirectory(), "test_data.bin")
        val content = "KeepLocal Privacy First Notes".toByteArray(Charsets.UTF_8)
        testFile.writeBytes(content)

        val expectedDigest = MessageDigest.getInstance("SHA-256").digest(content)
            .joinToString("") { "%02x".format(it) }

        val calculatedDigest = modelManager.computeSha256(testFile)
        assertEquals(expectedDigest, calculatedDigest)

        testFile.delete()
    }

    @Test
    fun testImportModelFromInputStreamSuccess() = runBlocking {
        val mockWeights = "Mock EmbeddingGemma Model Weights 300M".toByteArray(Charsets.UTF_8)
        val expectedSha256 = MessageDigest.getInstance("SHA-256").digest(mockWeights)
            .joinToString("") { "%02x".format(it) }

        val inputStream = ByteArrayInputStream(mockWeights)
        val success = modelManager.importModelFromInputStream(inputStream, expectedSha256)

        assertTrue(success)
        assertTrue(modelManager.isModelAvailable())
        assertTrue(modelManager.verifyExistingModelHash(expectedSha256))
    }

    @Test
    fun testImportModelFromInputStreamSha256Mismatch() = runBlocking {
        val mockWeights = "Corrupted Model File Data".toByteArray(Charsets.UTF_8)
        val wrongSha256 = "0000000000000000000000000000000000000000000000000000000000000000"

        val inputStream = ByteArrayInputStream(mockWeights)
        val success = modelManager.importModelFromInputStream(inputStream, wrongSha256)

        assertFalse(success)
        assertFalse(modelManager.isModelAvailable())
    }
}
