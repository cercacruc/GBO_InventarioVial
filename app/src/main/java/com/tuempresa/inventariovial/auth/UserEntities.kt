package com.tuempresa.inventariovial.auth

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Locale
import java.util.UUID

enum class UserRole { ADMIN, OPERATOR }
fun normalizeUsername(value: String) = value.trim().lowercase(Locale.ROOT)

@Entity(tableName = "user_cache", indices = [Index(value = ["username"], unique = true)])
data class UserCacheEntity(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val role: String,
    val active: Boolean,
    val credentialCiphertext: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long
)

@Entity(tableName = "user_sync_state")
data class UserSyncStateEntity(
    @PrimaryKey val id: Int = 1,
    val usersVersion: Long,
    val lastSuccessfulSyncAt: Long,
    val accessRevoked: Boolean = false
)

// Device-wide throttle: changing the supplied username cannot bypass it.
@Entity(tableName = "local_login_state")
data class LocalLoginStateEntity(
    @PrimaryKey val id: Int = 1,
    val failures: Int = 0,
    val blockedUntil: Long = 0,
    val lastUserId: String? = null
)

@Entity(tableName = "auth_audit", indices = [Index("timestamp")])
data class AuditLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String? = null,
    val username: String? = null,
    val action: String,
    val details: String? = null
)
