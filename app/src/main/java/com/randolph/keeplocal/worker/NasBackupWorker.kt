package com.randolph.keeplocal.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.randolph.keeplocal.data.local.KeepLocalDatabase
import com.randolph.keeplocal.data.nas.NasCredentialManager
import com.randolph.keeplocal.data.nas.SmbBackupManager
import com.randolph.keeplocal.data.nas.SyncResult
import java.util.concurrent.TimeUnit

class NasBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "NasBackupPeriodicWorker"

        fun schedulePeriodicBackup(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED) // LAN Wi-Fi
                .setRequiresCharging(true)                     // Device charging constraint
                .build()

            val backupRequest = PeriodicWorkRequestBuilder<NasBackupWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                backupRequest
            )
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val db = KeepLocalDatabase.getDatabase(applicationContext)
            val nasCredentialManager = NasCredentialManager(applicationContext)
            val smbBackupManager = SmbBackupManager(db.noteDao(), nasCredentialManager)

            val syncResult = smbBackupManager.performBackup(applicationContext.cacheDir)
            if (syncResult is SyncResult.Success) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
