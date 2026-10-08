package com.randolph.keeplocal.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.randolph.keeplocal.data.local.KeepLocalDatabase
import com.randolph.keeplocal.data.repository.AutoEmbeddingManager
import com.randolph.keeplocal.data.repository.EmbeddingRepository

class AutoEmbeddingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = KeepLocalDatabase.getDatabase(applicationContext)
            val embeddingRepo = EmbeddingRepository(applicationContext)
            val autoEmbeddingManager = AutoEmbeddingManager(db.noteDao(), db.noteEmbeddingDao(), embeddingRepo)

            autoEmbeddingManager.runEmbeddingPass()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
