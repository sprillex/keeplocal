package com.randolph.keeplocal.data.nas

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NasCredentialManagerTest {

    private lateinit var context: Context
    private lateinit var credentialManager: NasCredentialManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        credentialManager = NasCredentialManager(context)
        credentialManager.clearNasConfig()
    }

    @Test
    fun testCredentialStorageAndRetrieval() {
        val initialConfig = credentialManager.getNasConfig()
        assertFalse(initialConfig.isValid)

        val newConfig = NasConfig(
            hostIp = "192.168.1.150",
            shareName = "NotesBackup",
            domain = "WORKGROUP",
            username = "nasuser",
            password = "secretpassword123"
        )
        credentialManager.saveNasConfig(newConfig)

        val retrieved = credentialManager.getNasConfig()
        assertTrue(retrieved.isValid)
        assertEquals("192.168.1.150", retrieved.hostIp)
        assertEquals("NotesBackup", retrieved.shareName)
        assertEquals("WORKGROUP", retrieved.domain)
        assertEquals("nasuser", retrieved.username)
        assertEquals("secretpassword123", retrieved.password)
    }
}
