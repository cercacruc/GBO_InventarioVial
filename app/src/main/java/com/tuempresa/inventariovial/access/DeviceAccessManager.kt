package com.tuempresa.inventariovial.access

import android.content.Context
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.tuempresa.inventariovial.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.KeyStore
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

sealed interface AuthorizationDecision {
    data class Allowed(val evidence: String) : AuthorizationDecision
    data object Denied : AuthorizationDecision
    data object Unavailable : AuthorizationDecision
}
interface DeviceAuthorizationProvider { suspend fun authorize(fingerprint: String): AuthorizationDecision }

class LocalDeviceAuthorizationProvider(private val context: Context) : DeviceAuthorizationProvider {
    override suspend fun authorize(fingerprint: String): AuthorizationDecision = withContext(Dispatchers.IO) {
        val root=context.assets.open("allowed_devices.json").bufferedReader().use { JSONObject(it.readText()) }
        val array=root.getJSONArray("fingerprints")
        require(array.length()<=20) { "La demostración admite hasta 20 dispositivos." }
        if((0 until array.length()).any { array.getString(it)==fingerprint }) AuthorizationDecision.Allowed("LOCAL_DEMO")
        else AuthorizationDecision.Denied
    }
}

/** Integrate an HTTPS service and server signature verification after the authentication contract is agreed. */
class ServerDeviceAuthorizationProvider : DeviceAuthorizationProvider {
    override suspend fun authorize(fingerprint: String) = AuthorizationDecision.Unavailable
}

data class DeviceAccessState(val authorized: Boolean,val fingerprint: String,val message: String = "") {
    val displayCode get() = fingerprint.take(12).uppercase()
}

class DeviceAccessManager(private val context: Context,
    private val provider: DeviceAuthorizationProvider = LocalDeviceAuthorizationProvider(context)) {
    private val preferences=context.getSharedPreferences("device_access",Context.MODE_PRIVATE)
    fun fingerprint(): String = synchronized(fingerprintLock) {
        val installation=preferences.getString("installation_uuid",null) ?: UUID.randomUUID().toString().also {
            preferences.edit().putString("installation_uuid",it).commit()
        }
        val androidId=Settings.Secure.getString(context.contentResolver,Settings.Secure.ANDROID_ID).orEmpty()
        val hash=MessageDigest.getInstance("SHA-256").digest("$androidId:$installation".toByteArray())
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        preferences.edit().putString("fingerprint_hash",hash).apply()
        hash
    }
    suspend fun check(enabled: Boolean = BuildConfig.ACCESS_CONTROL_ENABLED): DeviceAccessState = withContext(Dispatchers.IO) {
        val id=fingerprint()
        if(!enabled) return@withContext DeviceAccessState(true,id,"Control de acceso deshabilitado para desarrollo")
        val cached=hasValidCache(id)
        val decision=try { provider.authorize(id) } catch(_:Exception) { AuthorizationDecision.Unavailable }
        when(decision) {
            is AuthorizationDecision.Allowed -> {
                val payload="$id|${decision.evidence}|${System.currentTimeMillis()}"
                preferences.edit().putString("grant",payload).putString("signature",sign(payload)).commit()
                DeviceAccessState(true,id)
            }
            AuthorizationDecision.Denied -> {
                preferences.edit().remove("grant").remove("signature").commit()
                DeviceAccessState(false,id,"DISPOSITIVO NO ACTIVADO")
            }
            AuthorizationDecision.Unavailable -> DeviceAccessState(cached,id,
                if(cached) "Autorización local válida; trabajo offline habilitado" else "DISPOSITIVO NO ACTIVADO")
        }
    }
    private fun hasValidCache(id: String): Boolean = runCatching {
        val payload=preferences.getString("grant",null) ?: return false
        val signature=preferences.getString("signature",null) ?: return false
        payload.startsWith("$id|") && MessageDigest.isEqual(Base64.decode(signature,Base64.NO_WRAP),Base64.decode(sign(payload),Base64.NO_WRAP))
    }.getOrDefault(false)
    private fun sign(payload: String): String {
        val store=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val alias="inventory_access_cache"
        val key=(store.getKey(alias,null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY).build())
        }.generateKey()
        val mac=Mac.getInstance("HmacSHA256").apply { init(key) }
        return Base64.encodeToString(mac.doFinal(payload.toByteArray()),Base64.NO_WRAP)
    }
    companion object { private val fingerprintLock=Any() }
}
