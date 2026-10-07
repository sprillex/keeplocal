package com.randolph.keeplocal

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import androidx.core.content.FileProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @Test
    fun testMainActivityLaunch() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
            }
        }
    }

    @Test
    fun testFileProviderConfigurationNotExported() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val componentName = ComponentName(context, FileProvider::class.java)
        val providerInfo: ProviderInfo = context.packageManager.getProviderInfo(
            componentName,
            PackageManager.GET_META_DATA
        )

        assertNotNull(providerInfo)
        assertEquals("com.randolph.keeplocal.modelprovider", providerInfo.authority)
        // FileProvider MUST NOT be exported to avoid SecurityException crash on startup
        assertFalse(providerInfo.exported)
    }
}
