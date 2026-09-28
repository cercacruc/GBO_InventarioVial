package com.tuempresa.inventariovial.scap.export

import android.content.Context
import android.util.Base64
import android.util.Base64OutputStream
import androidx.room.withTransaction
import androidx.work.*
import com.tuempresa.inventariovial.DriveConfig
import com.tuempresa.inventariovial.DriveUploadPolicy
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.scap.data.ScapFieldValueEntity
import com.tuempresa.inventariovial.scap.data.ScapInspectionSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okio.BufferedSink
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object ScapDriveSync {
    suspend fun enqueue(context: Context, snapshot: ScapInspectionSnapshot) {
        DriveUploadPolicy.requireConfiguration()
        val db = InventoryDatabase.getInstance(context)
        val inspection = snapshot.inspection
        val request = OneTimeWorkRequestBuilder<ScapDriveWorker>()
            .setInputData(workDataOf("inspectionId" to inspection.id))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
        // APPEND_OR_REPLACE retains edits queued while an earlier revision is being sent.
        // A second job is harmless: it exits if the current revision has a verified ACK.
        db.scapDao().syncState(inspection.id, inspection.updatedAt, "QUEUED")
        try {
            withContext(Dispatchers.IO) {
                WorkManager.getInstance(context).enqueueUniqueWork("scap-drive-${inspection.id}",
                    ExistingWorkPolicy.APPEND_OR_REPLACE, request).result.get()
            }
        } catch (e: Exception) {
            db.scapDao().syncState(inspection.id, inspection.updatedAt, "ERROR")
            throw e
        }
    }

    fun validAck(json: JSONObject, s: ScapInspectionSnapshot, hash: String): Boolean {
        val files = json.optJSONArray("files") ?: return false
        val paths = (0 until files.length()).map {files.optJSONObject(it) ?: return false}
        val expected = setOf("SCAP.xlsx", "manifest.json") + s.photos.map {"fotos/${it.id}.png"} + s.sketches.map {"croquis/${it.id}.png"}
        return paths.size == expected.size && paths.map {it.optString("path")}.toSet() == expected &&
            paths.all {it.optString("fileId").isNotBlank() && it.optString("folderId").isNotBlank()} &&
            json.optBoolean("ok") && json.optInt("protocol") == 1 && json.optString("recordKind") == "SCAP" &&
            json.optString("scapInspectionId") == s.inspection.id && json.optLong("revision") == s.inspection.updatedAt &&
            json.optString("sha256") == hash && json.optString("fileId").isNotBlank() &&
            json.optString("folderId").isNotBlank()
    }

    suspend fun acknowledge(db: InventoryDatabase, s: ScapInspectionSnapshot, ack: JSONObject, hash: String): Boolean {
        require(validAck(ack, s, hash)) {"El servidor no confirmó la entrega SCAP."}
        return db.withTransaction {
            val i = s.inspection
            if (db.scapDao().syncState(i.id, i.updatedAt, "SYNCED") != 1) false else {
                for ((key, value) in mapOf("revision" to i.updatedAt.toString(), "sha256" to hash,
                    "fileId" to ack.getString("fileId"), "folderId" to ack.getString("folderId"))) {
                    db.scapDao().putValue(ScapFieldValueEntity(i.id, "drive", key, value, "SERVER_ACK"))
                }
                val files=ack.getJSONArray("files")
                for(photo in s.photos) {
                    val file=(0 until files.length()).map {files.getJSONObject(it)}.single {it.getString("path")=="fotos/${photo.id}.png"}
                    db.scapDao().photoSynced(i.id,photo.id,file.getString("folderId"),file.getString("fileId"))
                }
                true
            }
        }
    }
}

class ScapDriveWorker(context: Context, parameters: WorkerParameters): CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val id = inputData.getString("inspectionId") ?: return@withContext Result.failure()
        val db = InventoryDatabase.getInstance(applicationContext)
        val dao = db.scapDao()
        val snapshot = dao.snapshot(id) ?: return@withContext Result.failure()
        val revision = snapshot.inspection.updatedAt
        if (snapshot.inspection.syncStatus == "SYNCED") return@withContext Result.success()
        suspend fun state(value: String, message: String = "") {
            db.withTransaction {
                if (dao.syncState(id, revision, value) == 1)
                    dao.putValue(ScapFieldValueEntity(id, "drive", "message", message, "SYNC"))
            }
        }
        if (DriveUploadPolicy.configurationError() != null) {
            state("ERROR", "Drive no configurado."); return@withContext Result.failure()
        }
        try {
            state("UPLOADING")
            val file = ScapDelivery.bundle(applicationContext, snapshot)
            val hash = ScapDelivery.sha256(file)
            val response = ScapDriveClient.upload(snapshot, file, hash)
            if (!ScapDriveSync.validAck(response, snapshot, hash)) {
                val retry = response.optBoolean("retryable", false)
                state(if (retry) "QUEUED" else "ERROR", if (retry) "Drive ocupado; se reintentará automáticamente." else
                    "El servidor no confirmó SCAP. Comprueba que publicaste el script actualizado y su configuración.")
                return@withContext if (retry) Result.retry() else Result.failure()
            }
            val current = ScapDriveSync.acknowledge(db, snapshot, response, hash)
            // Also remove an acknowledged old revision after concurrent local edits.
            file.delete()
            if (current) {
                state("SYNCED")
                // The remote immutable package is complete. Capture originals remain untouched.
                Result.success()
            } else Result.retry() // A newer saved revision still needs delivery.
        } catch (e: CancellationException) { throw e }
        catch (e: IOException) {
            state("QUEUED", "Conexión interrumpida; se reintentará automáticamente."); Result.retry()
        } catch (e: Exception) {
            state("ERROR", "No se pudo preparar SCAP. Revisa los archivos locales y los pendientes de exportación.")
            Result.failure()
        }
    }
}

internal object ScapDriveClient {
    private val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS).writeTimeout(180, TimeUnit.SECONDS).callTimeout(240, TimeUnit.SECONDS).build()
    suspend fun upload(snapshot: ScapInspectionSnapshot, file: File, hash: String): JSONObject = withContext(Dispatchers.IO) {
        DriveUploadPolicy.requireConfiguration()
        val body = JSONObject().put("token", DriveConfig.API_TOKEN).put("action", "uploadScap")
            .put("recordKind", "SCAP").put("protocol", 1).put("scapInspectionId", snapshot.inspection.id)
            .put("revision", snapshot.inspection.updatedAt).put("bridgeCode", snapshot.inspection.bridgeCode)
            .put("bridgeName", snapshot.inspection.bridgeName).put("sha256", hash)
        val request = Request.Builder().url(DriveConfig.WEB_APP_URL).post(ScapUploadBody(body, file)).build()
        client.newCall(request).execute().use {r ->
            if (!r.isSuccessful) throw IOException("Respuesta no disponible")
            runCatching {JSONObject(r.body?.string().orEmpty())}.getOrElse {throw IOException("Respuesta inválida")}
        }
    }
}

/** Stream base64 from disk instead of holding the complete package twice in memory. Never logged. */
internal class ScapUploadBody(metadata: JSONObject, private val file: File): RequestBody() {
    private val prefix = metadata.toString().dropLast(1) + ",\"base64\":\""
    override fun contentType() = "application/json; charset=utf-8".toMediaType()
    override fun contentLength() = prefix.toByteArray(Charsets.UTF_8).size + ((file.length() + 2) / 3) * 4 + 2
    override fun writeTo(sink: BufferedSink) {
        sink.writeUtf8(prefix)
        Base64OutputStream(sink.outputStream(), Base64.NO_WRAP or Base64.NO_CLOSE).use {out -> file.inputStream().use {it.copyTo(out)}}
        sink.writeUtf8("\"}")
    }
}
