package com.tuempresa.inventariovial.auth

import androidx.room.withTransaction
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

class AuthRepository(private val db: InventoryDatabase, private val protector: CredentialProtector,
    val sessions: AuthSessionManager, private val now: () -> Long = System::currentTimeMillis,
    private val canSwitchFieldUser: () -> Boolean = { true }) {
    private val loginMutex = Mutex()
    suspend fun login(username: String, pin: CharArray): String? = withContext(Dispatchers.Default) {
        try { loginMutex.withLock { sessions.cacheMutex.withLock {
            var authenticated: UserCacheEntity? = null
            val error = db.withTransaction {
                val dao = db.userDao()
                val state = dao.syncState()
                if (state?.accessRevoked == true) return@withTransaction "El acceso de esta tablet fue revocado. Contacta con GBO."
                if (state == null || dao.countUsers() == 0) return@withTransaction INITIAL_SYNC
                val age = now() - state.lastSuccessfulSyncAt
                if (age < 0 || age > USER_CACHE_MAX_AGE_HOURS * 3_600_000L) return@withTransaction EXPIRED_CACHE
                val throttle = dao.loginState() ?: LocalLoginStateEntity()
                if (throttle.blockedUntil > now()) return@withTransaction "Demasiados intentos. Espera cinco minutos e inténtalo de nuevo."
                val user = dao.getUserByUsername(normalizeUsername(username))
                val verifier = user?.let { runCatching { PinVerifier.parse(JSONObject(protector.decrypt(it.credentialCiphertext, "user:${it.id}"))) }.getOrNull() }
                val matches = if (verifier != null) PasswordHasher.verify(pin, verifier) else {
                    PasswordHasher.derive(pin, ByteArray(32), PasswordHasher.ITERATIONS).fill(0); false
                }
                if (!matches || user?.active != true || (sessions.current.value?.id?.let { it != user.id } == true)) {
                    val failures = if (throttle.blockedUntil > 0) 1 else throttle.failures + 1
                    dao.saveLoginState(throttle.copy(failures = failures, blockedUntil = if (failures >= 5) now() + 300_000 else 0))
                    db.auditDao().insert(AuditLogEntity(timestamp = now(), action = "LOGIN_FAILED"))
                    return@withTransaction "Usuario o PIN incorrecto."
                }
                val field = db.inventoryDao().currentSession()
                if (sessions.current.value == null && field != null &&
                    ((throttle.lastUserId != null && throttle.lastUserId != user.id) || field.operator != user.displayName)) {
                    if (!canSwitchFieldUser()) return@withTransaction "Inicia sesión con el operador anterior para finalizar el recorrido abierto."
                    db.inventoryDao().closeSessions(now())
                    db.auditDao().insert(AuditLogEntity(timestamp = now(), userId = throttle.lastUserId, action = "FIELD_SESSION_ENDED"))
                }
                dao.saveLoginState(LocalLoginStateEntity(lastUserId = user.id))
                db.auditDao().insert(AuditLogEntity(timestamp = now(), userId = user.id, username = user.username, action = "LOGIN_SUCCESS"))
                authenticated = user
                null
            }
            authenticated?.let(sessions::login)
            error
        } } } finally { pin.fill('\u0000') }
    }
    suspend fun logout() {
        sessions.current.value?.let { db.auditDao().insert(AuditLogEntity(userId = it.id, username = it.username, action = "LOGOUT")) }
        sessions.logout()
    }
    companion object {
        const val USER_CACHE_MAX_AGE_HOURS = 72L
        const val INITIAL_SYNC = "Esta tablet necesita conexión a Internet para realizar la configuración inicial de usuarios."
        const val EXPIRED_CACHE = "Se requiere conexión a Internet para actualizar los usuarios autorizados."
    }
}
