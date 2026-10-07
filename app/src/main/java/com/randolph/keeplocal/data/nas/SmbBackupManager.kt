package com.randolph.keeplocal.data.nas

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare
import com.randolph.keeplocal.data.local.dao.NoteDao
import com.randolph.keeplocal.data.repository.AutoEmbeddingManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.EnumSet

sealed interface SyncResult {
    data object Success : SyncResult
    data class Error(val message: String) : SyncResult
}

class SmbBackupManager(
    private val noteDao: NoteDao,
    private val nasCredentialManager: NasCredentialManager,
    private val backupArchiver: BackupArchiver = BackupArchiver(),
    private val autoEmbeddingManager: AutoEmbeddingManager? = null
) {
    suspend fun testConnection(config: NasConfig = nasCredentialManager.getNasConfig()): SyncResult = withContext(Dispatchers.IO) {
        if (!config.isValid) {
            return@withContext SyncResult.Error("NAS host IP and share name must be configured.")
        }

        var client: SMBClient? = null
        try {
            val smbConfig = SmbConfig.builder()
                .withTimeout(10000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()
            client = SMBClient(smbConfig)
            client.connect(config.hostIp).use { connection ->
                val authContext = AuthenticationContext(
                    config.username,
                    config.password.toCharArray(),
                    config.domain
                )
                val session = connection.authenticate(authContext)
                session.connectShare(config.shareName).use { share ->
                    if (share is DiskShare) {
                        val subfolder = config.subfolderPath.trim().trim('/')
                        if (subfolder.isNotEmpty() && !share.folderExists(subfolder)) {
                            try {
                                share.mkdir(subfolder)
                            } catch (e: Exception) {
                                // Folder creation attempt
                            }
                        }
                        return@withContext SyncResult.Success
                    } else {
                        return@withContext SyncResult.Error("Specified SMB share is not a disk share.")
                    }
                }
            }
        } catch (e: Exception) {
            return@withContext SyncResult.Error(e.localizedMessage ?: "SMB connection failed")
        } finally {
            client?.close()
        }
    }

    suspend fun performBackup(cacheDir: File): SyncResult = withContext(Dispatchers.IO) {
        val config = nasCredentialManager.getNasConfig()
        if (!config.isValid) {
            return@withContext SyncResult.Error("NAS host IP and share name must be configured.")
        }

        var backupFile: File? = null
        var client: SMBClient? = null
        try {
            val allNotes = noteDao.getAllActiveNotes().first()
            backupFile = File(cacheDir, "keeplocal_backup_${System.currentTimeMillis()}.tar.gz")
            backupArchiver.createBackupArchive(allNotes, backupFile)

            val smbConfig = SmbConfig.builder()
                .withTimeout(15000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()
            client = SMBClient(smbConfig)

            client.connect(config.hostIp).use { connection ->
                val authContext = AuthenticationContext(
                    config.username,
                    config.password.toCharArray(),
                    config.domain
                )
                val session = connection.authenticate(authContext)
                session.connectShare(config.shareName).use { share ->
                    if (share is DiskShare) {
                        val subfolder = config.subfolderPath.trim().trim('/')
                        if (subfolder.isNotEmpty() && !share.folderExists(subfolder)) {
                            try {
                                share.mkdir(subfolder)
                            } catch (e: Exception) {
                                // Folder creation attempt
                            }
                        }

                        val smbFileName = if (subfolder.isEmpty()) "keeplocal_backup.tar.gz" else "$subfolder/keeplocal_backup.tar.gz"
                        val smbFile = share.openFile(
                            smbFileName,
                            EnumSet.of(AccessMask.GENERIC_WRITE, AccessMask.GENERIC_READ),
                            null,
                            SMB2ShareAccess.ALL,
                            SMB2CreateDisposition.FILE_OVERWRITE_IF,
                            null
                        )

                        FileInputStream(backupFile).use { fis ->
                            smbFile.outputStream.use { os ->
                                fis.copyTo(os)
                                os.flush()
                            }
                        }
                        smbFile.close()
                        backupFile.delete()
                        return@withContext SyncResult.Success
                    } else {
                        return@withContext SyncResult.Error("SMB share is not a disk share")
                    }
                }
            }
        } catch (e: Exception) {
            backupFile?.delete()
            return@withContext SyncResult.Error(e.localizedMessage ?: "Backup upload failed")
        } finally {
            client?.close()
        }
    }

    suspend fun restoreBackup(cacheDir: File): SyncResult = withContext(Dispatchers.IO) {
        val config = nasCredentialManager.getNasConfig()
        if (!config.isValid) {
            return@withContext SyncResult.Error("NAS host IP and share name must be configured.")
        }

        try {
            val localRestoreFile = File(cacheDir, "restore_download.tar.gz")
            var client: SMBClient? = null
            try {
                val smbConfig = SmbConfig.builder()
                    .withTimeout(15000, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .build()
                client = SMBClient(smbConfig)
                client.connect(config.hostIp).use { connection ->
                    val authContext = AuthenticationContext(
                        config.username,
                        config.password.toCharArray(),
                        config.domain
                    )
                    val session = connection.authenticate(authContext)
                    session.connectShare(config.shareName).use { share ->
                        if (share is DiskShare) {
                            val subfolder = config.subfolderPath.trim().trim('/')
                            val smbFileName = if (subfolder.isEmpty()) "keeplocal_backup.tar.gz" else "$subfolder/keeplocal_backup.tar.gz"

                            if (!share.fileExists(smbFileName)) {
                                return@withContext SyncResult.Error("No backup archive found on NAS share at $smbFileName.")
                            }

                            val smbFile = share.openFile(
                                smbFileName,
                                EnumSet.of(AccessMask.GENERIC_READ),
                                null,
                                SMB2ShareAccess.ALL,
                                SMB2CreateDisposition.FILE_OPEN,
                                null
                            )

                            FileOutputStream(localRestoreFile).use { fos ->
                                smbFile.inputStream.use { isStream ->
                                    isStream.copyTo(fos)
                                    fos.flush()
                                }
                            }
                            smbFile.close()
                        }
                    }
                }
            } finally {
                client?.close()
            }

            if (!localRestoreFile.exists() || localRestoreFile.length() == 0L) {
                return@withContext SyncResult.Error("Failed to download backup archive from NAS.")
            }

            // Unpack notes from restored archive
            val (_, restoredNotes) = backupArchiver.unpackBackupArchive(localRestoreFile)
            localRestoreFile.delete()

            // Save restored notes into Room DB
            for (note in restoredNotes) {
                noteDao.insertNote(note)
            }

            // Trigger background auto-embedding pass to rebuild vector index
            autoEmbeddingManager?.runEmbeddingPass()

            SyncResult.Success
        } catch (e: Exception) {
            SyncResult.Error(e.localizedMessage ?: "Restore failed")
        }
    }
}
