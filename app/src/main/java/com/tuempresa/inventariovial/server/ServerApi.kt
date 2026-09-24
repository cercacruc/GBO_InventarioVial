package com.tuempresa.inventariovial.server

import com.tuempresa.inventariovial.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class ServerSyncStatus { PENDING, SYNCING, SYNCED, ERROR }
data class UpsertResult(val accepted: Boolean,val retryable: Boolean,val message: String? = null)
interface ServerCredentialProvider { fun bearerToken(): String? }
interface ServerApi { suspend fun upsert(payload: JSONObject): UpsertResult }

object ServerConfiguration {
    fun validBaseUrl(value: String): Boolean {
        val url=value.toHttpUrlOrNull() ?: return false
        return url.isHttps && url.username.isEmpty() && url.password.isEmpty() && url.query==null && url.fragment==null
    }
    val enabled get() = validBaseUrl(BuildConfig.SERVER_BASE_URL)
}

class HttpsServerApi(baseUrl: String,private val credentials: ServerCredentialProvider? = null) : ServerApi {
    private val base=baseUrl.trimEnd('/')
    private val client=OkHttpClient.Builder().connectTimeout(20,TimeUnit.SECONDS).readTimeout(60,TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).build()
    init { require(ServerConfiguration.validBaseUrl(baseUrl)) { "El servidor requiere una URL HTTPS sin credenciales." } }
    override suspend fun upsert(payload: JSONObject): UpsertResult {
        val id=payload.getString("id");val version=payload.getLong("updatedAt")
        val builder=Request.Builder().url("$base/api/v1/inventory/records/upsert")
            .header("Idempotency-Key",id).post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
        credentials?.bearerToken()?.takeIf { it.isNotBlank() }?.let { builder.header("Authorization","Bearer $it") }
        client.newCall(builder.build()).execute().use { response ->
            if(!response.isSuccessful) return UpsertResult(false,response.code==408 || response.code==429 || response.code>=500,"HTTP ${response.code}")
            val acknowledgement=runCatching { JSONObject(response.body?.string().orEmpty()) }.getOrNull()
            val accepted=acknowledgement?.optBoolean("accepted")==true && acknowledgement.optString("id")==id &&
                acknowledgement.optLong("updatedAt",-1)==version
            return UpsertResult(accepted,false,if(accepted) null else "Acuse incompatible; revisar el contrato de la API.")
        }
    }
}
