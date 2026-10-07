package com.randolph.keeplocal.data.model

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class ModelManager(
    private val context: Context,
    val modelUrl: String = DEFAULT_MODEL_URL,
    val expectedSha256: String = DEFAULT_EXPECTED_SHA256
) {
    companion object {
        const val MODEL_DIR = "models"
        const val MODEL_FILENAME = "embeddinggemma_qat_int8.tflite"

        // Default configuration values
        const val DEFAULT_MODEL_URL = "https://huggingface.co/google/embeddinggemma-qat-int8/resolve/main/embeddinggemma_qat_int8.tflite"
        const val DEFAULT_EXPECTED_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    }

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    fun getModelDirectory(): File {
        val dir = File(context.filesDir, MODEL_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getModelFile(): File {
        return File(getModelDirectory(), MODEL_FILENAME)
    }

    fun isModelAvailable(): Boolean {
        val file = getModelFile()
        return file.exists() && file.length() > 0
    }

    suspend fun downloadAndVerifyModel(
        customUrl: String = modelUrl,
        expectedHash: String = expectedSha256
    ): Boolean = withContext(Dispatchers.IO) {
        val targetFile = getModelFile()
        val tempFile = File(getModelDirectory(), "$MODEL_FILENAME.tmp")

        try {
            _downloadState.value = DownloadState.Downloading(0, -1)

            val url = URL(customUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                _downloadState.value = DownloadState.Error("HTTP Error: ${connection.responseCode} ${connection.responseMessage}")
                return@withContext false
            }

            val totalBytes = connection.contentLengthLong
            val inputStream: InputStream = connection.inputStream

            tempFile.outputStream().use { outputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var downloadedBytes = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    _downloadState.value = DownloadState.Downloading(downloadedBytes, totalBytes)
                }
                outputStream.flush()
            }

            // Verification phase
            _downloadState.value = DownloadState.Verifying
            val calculatedHash = computeSha256(tempFile)

            if (expectedHash.isNotEmpty() && !calculatedHash.equals(expectedHash, ignoreCase = true)) {
                tempFile.delete()
                _downloadState.value = DownloadState.Error("SHA-256 mismatch! Expected: $expectedHash, Found: $calculatedHash")
                return@withContext false
            }

            // Atomic file commit
            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (!tempFile.renameTo(targetFile)) {
                // Fallback copy if rename fails across file systems
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            _downloadState.value = DownloadState.Success
            true
        } catch (e: Exception) {
            if (tempFile.exists()) {
                tempFile.delete()
            }
            _downloadState.value = DownloadState.Error(e.localizedMessage ?: "Download failed")
            false
        }
    }

    fun verifyExistingModelHash(expectedHash: String = expectedSha256): Boolean {
        val file = getModelFile()
        if (!file.exists()) return false
        val hash = computeSha256(file)
        return hash.equals(expectedHash, ignoreCase = true)
    }

    fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { inputStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
