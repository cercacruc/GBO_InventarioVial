package com.tuempresa.inventariovial.auth

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT COUNT(*) FROM user_cache") suspend fun countUsers(): Int
    @Query("SELECT * FROM user_cache WHERE username = :username") suspend fun getUserByUsername(username: String): UserCacheEntity?
    @Query("SELECT * FROM user_cache WHERE id = :id") suspend fun getUserById(id: String): UserCacheEntity?
    @Query("SELECT * FROM user_cache ORDER BY username") suspend fun getAllUsers(): List<UserCacheEntity>
    @Query("SELECT * FROM user_cache ORDER BY username") fun observeUsers(): Flow<List<UserCacheEntity>>
    @Query("SELECT COUNT(*) FROM user_cache WHERE active = 1 AND role = 'ADMIN'") suspend fun countActiveAdmins(): Int
    @Upsert suspend fun upsertUsers(users: List<UserCacheEntity>)
    @Query("DELETE FROM user_cache") suspend fun clearForSnapshot()
    @Query("SELECT * FROM user_sync_state WHERE id = 1") suspend fun syncState(): UserSyncStateEntity?
    @Query("SELECT * FROM user_sync_state WHERE id = 1") fun observeSyncState(): Flow<UserSyncStateEntity?>
    @Upsert suspend fun saveSyncState(state: UserSyncStateEntity)
    @Query("SELECT * FROM local_login_state WHERE id = 1") suspend fun loginState(): LocalLoginStateEntity?
    @Upsert suspend fun saveLoginState(state: LocalLoginStateEntity)
}

@Dao
interface AuditDao {
    @Insert suspend fun insert(event: AuditLogEntity)
    @Query("SELECT * FROM auth_audit ORDER BY timestamp DESC LIMIT 200") suspend fun recent(): List<AuditLogEntity>
}
