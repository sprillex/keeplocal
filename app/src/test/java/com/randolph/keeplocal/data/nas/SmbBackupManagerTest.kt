package com.randolph.keeplocal.data.nas

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.randolph.keeplocal.data.local.KeepLocalDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SmbBackupManagerTest {

    private lateinit var context: Context
    private lateinit var db: KeepLocalDatabase
    private lateinit var nasCredentialManager: NasCredentialManager
    private lateinit var smbBackupManager: SmbBackupManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, KeepLocalDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        nasCredentialManager = NasCredentialManager(context)
        nasCredentialManager.clearNasConfig()

        smbBackupManager = SmbBackupManager(db.noteDao(), nasCredentialManager)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testUnconfiguredNasReturnsError() = runBlocking {
        val testResult = smbBackupManager.testConnection()
        assertTrue(testResult is SyncResult.Error)
        assertEquals("NAS host IP and share name must be configured.", (testResult as SyncResult.Error).message)

        val backupResult = smbBackupManager.performBackup(context.cacheDir)
        assertTrue(backupResult is SyncResult.Error)

        val restoreResult = smbBackupManager.restoreBackup(context.cacheDir)
        assertTrue(restoreResult is SyncResult.Error)
    }

    @Test
    fun testInvalidHostReturnsConnectionError() = runBlocking {
        val config = NasConfig(
            hostIp = "192.0.2.254", // Invalid/unreachable IP in test range
            shareName = "NonExistentShare",
            username = "user",
            password = "pass"
        )
        nasCredentialManager.saveNasConfig(config)

        val testResult = smbBackupManager.testConnection(config)
        assertTrue(testResult is SyncResult.Error)

        val backupResult = smbBackupManager.performBackup(context.cacheDir)
        assertTrue(backupResult is SyncResult.Error)
    }
}
