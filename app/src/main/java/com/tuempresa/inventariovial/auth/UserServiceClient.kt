package com.tuempresa.inventariovial.auth

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.tuempresa.inventariovial.access.DeviceAccessManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

interface UserService {
    suspend fun call(action: String, fields: JSONObject = JSONObject()): JSONObject
}
class UserServiceException(val code: String) : Exception(when(code) {
    "DEVICE_REVOKED" -> "El acceso de esta tablet fue revocado. Contacta con GBO."
    "ADMIN_REQUIRED", "SESSION_EXPIRED" -> "Vuelve a autorizar la administración con tu PIN."
    "LAST_ADMIN" -> "No puedes quitar el último administrador activo."
    "DUPLICATE_USERNAME" -> "El nombre de usuario ya está registrado."
    "RATE_LIMITED" -> "Demasiados intentos. Espera cinco minutos."
    "INVALID_CREDENTIALS" -> "Usuario o PIN incorrecto."
    "OFFLINE" -> "Esta acción requiere conexión a Internet."
    "INVALID_REQUEST" -> "Revisa los datos ingresados."
    else -> "No se pudo contactar con el servicio de usuarios. Inténtalo nuevamente."
})

class UserServiceSettings(context: Context, private val protector: CredentialProtector) {
    private val preferences = context.getSharedPreferences("user_service", Context.MODE_PRIVATE)
    val configured get() = preferences.contains("provisioning")
    fun read(): JSONObject = JSONObject(protector.decrypt(checkNotNull(preferences.getString("provisioning", null)), "tablet-provisioning"))
    fun provision(url: String, token: String) {
        check(!configured) { "La tablet ya está configurada." }
        validateUrl(url.trim())
        require(token.matches(Regex("[A-Za-z0-9_-]{43}"))) { "Token de tablet inválido." }
        val blob = protector.encrypt(JSONObject().put("url", url.trim()).put("token", token).toString(), "tablet-provisioning")
        check(preferences.edit().putString("provisioning", blob).commit())
    }
    internal fun clearInitialProvisioning() { check(preferences.edit().remove("provisioning").commit()) }
    companion object {
        fun validateUrl(value: String) {
            val url = value.toHttpUrl()
            require(url.scheme == "https" && url.host == "script.google.com" && url.port == 443 &&
                url.username.isEmpty() && url.password.isEmpty() && url.query == null && url.fragment == null &&
                url.encodedPath.matches(Regex("/macros/s/[A-Za-z0-9_-]+/exec"))) {
                "Ingresa la URL HTTPS /exec del Apps Script de usuarios."
            }
        }
    }
}

fun hasUserServiceNetwork(context: Context): Boolean {
    val manager = context.getSystemService(ConnectivityManager::class.java)
    val caps = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

class HttpsUserService(private val context: Context, private val settings: UserServiceSettings) : UserService {
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(75, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build()
    override suspend fun call(action: String, fields: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        if (!hasUserServiceNetwork(context)) throw UserServiceException("OFFLINE")
        check(DeviceAccessManager(context).check().authorized) { "Dispositivo no autorizado." }
        val config = settings.read()
        UserServiceSettings.validateUrl(config.getString("url"))
        val body = JSONObject(fields.toString()).put("action", action).put("deviceId", DeviceAccessManager(context).fingerprint())
            .put("tabletToken", config.getString("token"))
        var request = Request.Builder().url(config.getString("url"))
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType())).build()
        // Apps Script returns a one-time ContentService URL. Never resend the secret POST to a redirect.
        var json: JSONObject? = null
        for (attempt in 0..1) {
            client.newCall(request).execute().use { response ->
                if (attempt == 0 && response.code in listOf(301, 302, 303)) {
                    val target = response.header("Location")?.toHttpUrl() ?: throw UserServiceException("SERVICE_ERROR")
                    if (target.scheme != "https" || target.host != "script.googleusercontent.com" || target.port != 443 ||
                        target.username.isNotEmpty() || target.password.isNotEmpty()) throw UserServiceException("SERVICE_ERROR")
                    request = Request.Builder().url(target).get().build()
                } else {
                    if (!response.isSuccessful) throw UserServiceException("SERVICE_ERROR")
                    val source = response.body?.source() ?: throw UserServiceException("SERVICE_ERROR")
                    val buffer = okio.Buffer()
                    while (buffer.size <= 2_000_000 && source.read(buffer, 8192) != -1L) { /* bounded response */ }
                    if (buffer.size > 2_000_000) throw UserServiceException("SERVICE_ERROR")
                    json = JSONObject(buffer.readUtf8())
                }
            }
            if (json != null) break
        }
        val result = json ?: throw UserServiceException("SERVICE_ERROR")
        if (!result.getBoolean("success")) throw UserServiceException(result.optString("error", "SERVICE_ERROR"))
        result
    }
}
