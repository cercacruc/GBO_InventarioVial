package com.tuempresa.inventariovial.auth

import android.content.Context
import androidx.work.*
import com.tuempresa.inventariovial.access.DeviceAccessManager
import java.util.concurrent.TimeUnit

class UserSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!DeviceAccessManager(applicationContext).check().authorized) return Result.failure()
        val graph = AuthGraph.get(applicationContext)
        if (!graph.settings.configured) return Result.failure()
        val result = graph.sync.sync()
        return when {
            result.isSuccess -> Result.success()
            (result.exceptionOrNull() as? UserServiceException)?.code == "DEVICE_REVOKED" -> Result.failure()
            else -> Result.retry()
        }
    }
    companion object {
        private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork("gbo-user-sync-now", ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<UserSyncWorker>().setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build())
        }
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("gbo-user-sync-periodic", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<UserSyncWorker>(6, TimeUnit.HOURS).setConstraints(constraints).build())
        }
    }
}
