package com.tuempresa.inventariovial.server

import android.content.Context
import androidx.work.*
import com.tuempresa.inventariovial.BuildConfig
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit

class ServerSyncWorker(context: Context,parameters: WorkerParameters) : CoroutineWorker(context,parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if(!ServerConfiguration.enabled) return@withContext Result.success()
        val dao=InventoryDatabase.getInstance(applicationContext).inventoryDao()
        val api=HttpsServerApi(BuildConfig.SERVER_BASE_URL)
        var retry=false
        for(id in dao.pendingServerIds()) {
            val snapshot=dao.snapshot(id) ?: continue
            val version=snapshot.record.updatedAt
            if(dao.setServerStatus(id,version,ServerSyncStatus.SYNCING.name,null)==0) { retry=true;continue }
            try {
                val session=snapshot.record.sessionId?.let { dao.sessionById(it) }
                val result=api.upsert(ServerPayload.encode(snapshot,session))
                val updated=dao.setServerStatus(id,version,if(result.accepted) "SYNCED" else "ERROR",result.message)
                retry=retry || result.retryable || updated==0
            } catch(cancelled: CancellationException) {
                // A later run also selects SYNCING rows, recovering an interrupted attempt.
                throw cancelled
            } catch(error: IOException) {
                dao.setServerStatus(id,version,"ERROR","No se pudo contactar al servidor HTTPS.")
                retry=true
            } catch(error: Exception) {
                dao.setServerStatus(id,version,"ERROR",error.message?.take(200) ?: "Error preparando el registro.")
            }
        }
        if(retry) Result.retry() else Result.success()
    }
}

fun scheduleServerSync(context: Context) {
    if(!ServerConfiguration.enabled) return
    val request=OneTimeWorkRequestBuilder<ServerSyncWorker>()
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build()
    WorkManager.getInstance(context).enqueueUniqueWork("enterprise-inventory-sync",ExistingWorkPolicy.APPEND_OR_REPLACE,request)
}
