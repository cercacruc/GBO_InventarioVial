package com.tuempresa.inventariovial.auth

import com.tuempresa.inventariovial.data.database.InventoryDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class AdminUsersRepository(private val db: InventoryDatabase, private val service: UserService,
    private val sessions: AuthSessionManager, private val sync: UserSyncRepository,
    private val online: () -> Boolean, private val now: () -> Long = System::currentTimeMillis) {
    private var token: String? = null
    private var tokenUser: String? = null
    private var expiresAt = 0L
    fun forgetAuthorization() { token = null; tokenUser = null; expiresAt = 0 }
    suspend fun authenticate(pin: CharArray) = withContext(Dispatchers.IO) {
        try {
            val user = sessions.requireAdmin()
            if (!online()) throw UserServiceException("OFFLINE")
            val result = service.call("adminAuthenticate", JSONObject().put("username", user.username).put("pin", String(pin)))
            sessions.requireAdmin().also { check(it.id == user.id) }
            token = result.getString("adminToken")
            tokenUser = user.id
            expiresAt = now() + result.getLong("expiresInSeconds").coerceIn(1, 300) * 1000
        } finally { pin.fill('\u0000') }
    }
    suspend fun users(): List<UserCacheEntity> { sessions.requireAdmin(); return db.userDao().getAllUsers() }
    suspend fun audit(): List<AuditLogEntity> { sessions.requireAdmin(); return db.auditDao().recent() }
    suspend fun mutate(action: String, fields: JSONObject): String = withContext(Dispatchers.IO) {
        val admin = sessions.requireAdmin()
        if (!online()) throw UserServiceException("OFFLINE")
        check(action in setOf("createUser", "updateUser", "setUserActive", "changeUserRole", "resetUserPin"))
        if (token == null || tokenUser != admin.id || now() >= expiresAt) throw UserServiceException("SESSION_EXPIRED")
        val target = fields.optString("id").takeIf { it.isNotEmpty() }?.let { db.userDao().getUserById(it) }
        if (target?.active == true && target.role == "ADMIN" &&
            ((action == "setUserActive" && !fields.getBoolean("active")) || (action == "changeUserRole" && fields.getString("role") != "ADMIN"))) {
            if (db.userDao().countActiveAdmins() <= 1) throw UserServiceException("LAST_ADMIN")
        }
        val event = when(action) {
            "createUser" -> "USER_CREATED"
            "setUserActive" -> if (fields.getBoolean("active")) "USER_ENABLED" else "USER_DISABLED"
            "changeUserRole" -> "USER_ROLE_CHANGED"
            "resetUserPin" -> "USER_PIN_RESET"
            else -> "USER_UPDATED"
        }
        val response = service.call(action, JSONObject(fields.toString()).put("adminToken", token))
        db.auditDao().insert(AuditLogEntity(userId = admin.id, username = admin.username, action = event,
            details = response.getString("id")))
        val result = sync.sync()
        if (result.isSuccess) "Cambio guardado y usuarios actualizados."
        else "Cambio guardado en el servicio central. Falta actualizar la caché; pulsa Sincronizar usuarios."
    }
}
