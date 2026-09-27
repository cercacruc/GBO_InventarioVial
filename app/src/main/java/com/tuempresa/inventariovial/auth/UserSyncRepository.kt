package com.tuempresa.inventariovial.auth

import androidx.room.withTransaction
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

class UserSyncRepository(private val db: InventoryDatabase, private val service: UserService,
    private val protector: CredentialProtector, private val sessions: AuthSessionManager,
    private val now: () -> Long = System::currentTimeMillis) {
    private val mutex = Mutex()
    suspend fun resetInitialProvisioning(settings: UserServiceSettings) = mutex.withLock {
        check(db.userDao().countUsers() == 0 && (db.userDao().syncState()?.usersVersion ?: -1) < 1)
        settings.clearInitialProvisioning()
    }
    suspend fun sync(): Result<Unit> = withContext(Dispatchers.IO) { mutex.withLock {
        try {
            val old = db.userDao().syncState()
            val clientVersion = old?.usersVersion ?: -1
            val response = service.call("syncUsers", JSONObject().put("clientVersion", clientVersion))
            require(response.get("version") is Int || response.get("version") is Long)
            require(response.get("changed") is Boolean)
            val version = response.getLong("version")
            require(version >= 1 && version >= clientVersion)
            val changed = response.getBoolean("changed")
            val time = now()
            val users = if (changed) {
                require(version > clientVersion)
                val rows = response.getJSONArray("users")
                require(rows.length() in 1..1000)
                val parsed = (0 until rows.length()).map { index ->
                    val row = rows.getJSONObject(index)
                    val id = row.getString("id").also { require(UUID.fromString(it).toString() == it) }
                    val username = row.getString("username")
                    require(username == normalizeUsername(username) && username.matches(Regex("[a-z0-9._-]{3,64}")))
                    val name = row.getString("displayName").also { require(it.isNotBlank() && it.length <= 120) }
                    val role = UserRole.valueOf(row.getString("role")).name
                    val created = row.getLong("createdAt")
                    val updated = row.getLong("updatedAt")
                    require(row.get("active") is Boolean)
                    require(row.get("createdAt") is Long || row.get("createdAt") is Int)
                    require(row.get("updatedAt") is Long || row.get("updatedAt") is Int)
                    require(created > 0 && updated >= created)
                    val verifier = PinVerifier.parse(row)
                    UserCacheEntity(id, username, name, role, row.getBoolean("active"),
                        protector.encrypt(verifier.json().toString(), "user:$id"), created, updated, time)
                }
                require(parsed.map { it.id }.distinct().size == parsed.size && parsed.map { it.username }.distinct().size == parsed.size)
                require(parsed.any { it.active && it.role == "ADMIN" })
                parsed
            } else {
                require(old != null && version == clientVersion && db.userDao().countUsers() > 0)
                null
            }
            sessions.cacheMutex.withLock {
                db.withTransaction {
                    if (users != null) {
                        db.userDao().clearForSnapshot()
                        db.userDao().upsertUsers(users)
                    }
                    db.userDao().saveSyncState(UserSyncStateEntity(usersVersion = version, lastSuccessfulSyncAt = time))
                    db.auditDao().insert(AuditLogEntity(timestamp = time, action = "USER_SYNC_SUCCESS"))
                }
                sessions.refresh(db.userDao().getAllUsers())
            }
            Result.success(Unit)
        } catch (cancel: CancellationException) { throw cancel }
        catch (error: Exception) {
            if ((error as? UserServiceException)?.code == "DEVICE_REVOKED") {
                sessions.cacheMutex.withLock {
                    db.withTransaction {
                        val old = db.userDao().syncState() ?: UserSyncStateEntity(usersVersion = -1, lastSuccessfulSyncAt = 0)
                        db.userDao().saveSyncState(old.copy(accessRevoked = true))
                    }
                    sessions.refresh(emptyList())
                }
            }
            db.auditDao().insert(AuditLogEntity(action = "USER_SYNC_FAILED"))
            Result.failure(if (error is UserServiceException) error else UserServiceException("SERVICE_ERROR"))
        }
    } }
}
