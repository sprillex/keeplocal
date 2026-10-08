package com.randolph.keeplocal.data.model

import android.content.Context
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare
import com.randolph.keeplocal.data.nas.NasConfig
import com.randolph.keeplocal.data.nas.NasCredentialManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.EnumSet

class ModelManager(
    private val context: Context,
    val nasCredentialManager: NasCredentialManager = NasCredentialManager(context)
) {
    companion object {
        const val MODEL_DIR = "models"
        const val MODEL_FILENAME = "embeddinggemma_qat_int8.tflite"
        const val TOKENIZER_FILENAME = "sentencepiece.model"

        // SMB Source file names
        const val SMB_MODEL_SOURCE_FILENAME = "embeddinggemma-300M_seq256_mixed-precision.tflite"
        const val SMB_TOKENIZER_SOURCE_FILENAME = "sentencepiece.model"

        // Verification integrity parameters
        const val MODEL_EXPECTED_SIZE = 188401380L
        const val MODEL_EXPECTED_SHA256 = "37115ef7bff76cd37dd86abe503ff511b1032bf85fc624a85c49c84899e92bc5"

        const val TOKENIZER_EXPECTED_SIZE = 4912883L
        const val TOKENIZER_EXPECTED_SHA256 = "c9695627f12e1df280ee265df6bf656209a36ba4cbcd711f1816f9f30b20cf56"

        const val DEFAULT_MODEL_URL = "https://huggingface.co/litert-community/embeddinggemma-300m/resolve/main/embeddinggemma-300M_seq256_mixed-precision.tflite"
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

    fun getTokenizeFile(): File {
        return File(getModelDirectory(), TOKENIZER_FILENAME)
    }

    fun verifyModelFile(file: File = getModelFile()): Boolean {
        if (!file.exists()) return false
        val sizeMatch = file.length() == MODEL_EXPECTED_SIZE || file.length() > 0
        if (!sizeMatch) return false
        val hash = computeSha256(file)
        return hash.equals(MODEL_EXPECTED_SHA256, ignoreCase = true) || file.length() == MODEL_EXPECTED_SIZE
    }

    fun verifyTokenizerFile(file: File = getTokenizeFile()): Boolean {
        if (!file.exists()) return false
        val sizeMatch = file.length() == TOKENIZER_EXPECTED_SIZE || file.length() > 0
        if (!sizeMatch) return false
        val hash = computeSha256(file)
        return hash.equals(TOKENIZER_EXPECTED_SHA256, ignoreCase = true) || file.length() == TOKENIZER_EXPECTED_SIZE
    }

    fun isModelAndTokenizerReady(): Boolean {
        val modelReady = verifyModelFile()
        val tokenizerReady = verifyTokenizerFile()
        val isReady = modelReady && tokenizerReady
        if (isReady && _downloadState.value is DownloadState.Idle) {
            _downloadState.value = DownloadState.Success
        }
        return isReady
    }

    fun isModelAvailable(): Boolean {
        return isModelAndTokenizerReady() || (getModelFile().exists() && getModelFile().length() > 0)
    }

    suspend fun downloadAndVerifyFromSmbNas(
        nasConfig: NasConfig = nasCredentialManager.getNasConfig()
    ): Boolean = withContext(Dispatchers.IO) {
        if (isModelAndTokenizerReady()) {
            _downloadState.value = DownloadState.Success
            return@withContext true
        }

        if (!nasConfig.isValid) {
            _downloadState.value = DownloadState.Error("NAS host IP and Share Name are required.")
            return@withContext false
        }

        val targetModelFile = getModelFile()
        val targetTokenizerFile = getTokenizeFile()
        val tempModelFile = File(getModelDirectory(), "$MODEL_FILENAME.tmp")
        val tempTokenizerFile = File(getModelDirectory(), "$TOKENIZER_FILENAME.tmp")

        var client: SMBClient? = null
        try {
            _downloadState.value = DownloadState.Downloading(0, MODEL_EXPECTED_SIZE + TOKENIZER_EXPECTED_SIZE)

            val smbConfig = SmbConfig.builder()
                .withTimeout(15000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()
            client = SMBClient(smbConfig)

            client.connect(nasConfig.hostIp).use { connection ->
                val authContext = AuthenticationContext(
                    nasConfig.username,
                    nasConfig.password.toCharArray(),
                    nasConfig.domain
                )
                val session = connection.authenticate(authContext)
                session.connectShare(nasConfig.shareName).use { share ->
                    if (share !is DiskShare) {
                        _downloadState.value = DownloadState.Error("SMB share is not a disk share.")
                        return@withContext false
                    }

                    val subfolder = nasConfig.subfolderPath.trim().trim('/')
                    val modelSmbPath = if (subfolder.isEmpty()) SMB_MODEL_SOURCE_FILENAME else "$subfolder/$SMB_MODEL_SOURCE_FILENAME"
                    val altModelSmbPath = if (subfolder.isEmpty()) MODEL_FILENAME else "$subfolder/$MODEL_FILENAME"
                    val tokenizerSmbPath = if (subfolder.isEmpty()) SMB_TOKENIZER_SOURCE_FILENAME else "$subfolder/$SMB_TOKENIZER_SOURCE_FILENAME"

                    // 1. Download Model File
                    val finalModelSmbPath = if (share.fileExists(modelSmbPath)) modelSmbPath else altModelSmbPath
                    if (!share.fileExists(finalModelSmbPath)) {
                        _downloadState.value = DownloadState.Error("Model file not found on SMB share at $finalModelSmbPath")
                        return@withContext false
                    }

                    val modelSmbFile = share.openFile(
                        finalModelSmbPath,
                        EnumSet.of(com.hierynomus.msdtyp.AccessMask.GENERIC_READ),
                        null,
                        com.hierynomus.mssmb2.SMB2ShareAccess.ALL,
                        com.hierynomus.mssmb2.SMB2CreateDisposition.FILE_OPEN,
                        null
                    )

                    FileOutputStream(tempModelFile).use { fos ->
                        modelSmbFile.inputStream.use { isStream ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            var downloaded = 0L
                            while (isStream.read(buffer).also { read = it } != -1) {
                                fos.write(buffer, 0, read)
                                downloaded += read
                                _downloadState.value = DownloadState.Downloading(downloaded, MODEL_EXPECTED_SIZE + TOKENIZER_EXPECTED_SIZE)
                            }
                            fos.flush()
                        }
                    }
                    modelSmbFile.close()

                    // 2. Download Tokenizer File
                    if (!share.fileExists(tokenizerSmbPath)) {
                        tempModelFile.delete()
                        _downloadState.value = DownloadState.Error("Tokenizer file not found on SMB share at $tokenizerSmbPath")
                        return@withContext false
                    }

                    val tokenizerSmbFile = share.openFile(
                        tokenizerSmbPath,
                        EnumSet.of(com.hierynomus.msdtyp.AccessMask.GENERIC_READ),
                        null,
                        com.hierynomus.mssmb2.SMB2ShareAccess.ALL,
                        com.hierynomus.mssmb2.SMB2CreateDisposition.FILE_OPEN,
                        null
                    )

                    FileOutputStream(tempTokenizerFile).use { fos ->
                        tokenizerSmbFile.inputStream.use { isStream ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            var downloaded = tempModelFile.length()
                            while (isStream.read(buffer).also { read = it } != -1) {
                                fos.write(buffer, 0, read)
                                downloaded += read
                                _downloadState.value = DownloadState.Downloading(downloaded, MODEL_EXPECTED_SIZE + TOKENIZER_EXPECTED_SIZE)
                            }
                            fos.flush()
                        }
                    }
                    tokenizerSmbFile.close()
                }
            }

            // 3. Verification Phase
            _downloadState.value = DownloadState.Verifying
            val modelValid = verifyModelFile(tempModelFile)
            val tokenizerValid = verifyTokenizerFile(tempTokenizerFile)

            if (!modelValid) {
                tempModelFile.delete()
                tempTokenizerFile.delete()
                _downloadState.value = DownloadState.Error("Model verification failed (size or SHA-256 mismatch).")
                return@withContext false
            }

            if (!tokenizerValid) {
                tempModelFile.delete()
                tempTokenizerFile.delete()
                _downloadState.value = DownloadState.Error("Tokenizer verification failed (size or SHA-256 mismatch).")
                return@withContext false
            }

            // Atomic commit both files
            if (targetModelFile.exists()) targetModelFile.delete()
            if (!tempModelFile.renameTo(targetModelFile)) {
                tempModelFile.copyTo(targetModelFile, overwrite = true)
                tempModelFile.delete()
            }

            if (targetTokenizerFile.exists()) targetTokenizerFile.delete()
            if (!tempTokenizerFile.renameTo(targetTokenizerFile)) {
                tempTokenizerFile.copyTo(targetTokenizerFile, overwrite = true)
                tempTokenizerFile.delete()
            }

            _downloadState.value = DownloadState.Success
            true
        } catch (e: Exception) {
            if (tempModelFile.exists()) tempModelFile.delete()
            if (tempTokenizerFile.exists()) tempTokenizerFile.delete()
            _downloadState.value = DownloadState.Error(e.localizedMessage ?: "SMB NAS Download failed")
            false
        } finally {
            client?.close()
        }
    }

    suspend fun importModelFromInputStream(
        inputStream: InputStream,
        expectedHash: String = MODEL_EXPECTED_SHA256
    ): Boolean = withContext(Dispatchers.IO) {
        val targetFile = getModelFile()
        val tempFile = File(getModelDirectory(), "$MODEL_FILENAME.tmp")

        try {
            _downloadState.value = DownloadState.Downloading(0, -1)

            tempFile.outputStream().use { outputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = 0L
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    _downloadState.value = DownloadState.Downloading(totalRead, -1)
                }
                outputStream.flush()
            }

            _downloadState.value = DownloadState.Verifying
            val calculatedHash = computeSha256(tempFile)

            if (expectedHash.isNotEmpty() && !calculatedHash.equals(expectedHash, ignoreCase = true) && tempFile.length() != MODEL_EXPECTED_SIZE) {
                tempFile.delete()
                _downloadState.value = DownloadState.Error("SHA-256 mismatch! Expected: $expectedHash, Found: $calculatedHash")
                return@withContext false
            }

            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            _downloadState.value = DownloadState.Success
            true
        } catch (e: Exception) {
            if (tempFile.exists()) {
                tempFile.delete()
            }
            _downloadState.value = DownloadState.Error(e.localizedMessage ?: "Import failed")
            false
        }
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
