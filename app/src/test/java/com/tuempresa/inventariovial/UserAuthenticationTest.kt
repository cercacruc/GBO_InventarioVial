package com.tuempresa.inventariovial

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuempresa.inventariovial.auth.*
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.field.FieldController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class UserAuthenticationTest {
    private lateinit var db: InventoryDatabase
    private lateinit var auth: AuthRepository
    private lateinit var sessions: AuthSessionManager
    private lateinit var sync: UserSyncRepository
    private lateinit var service: FakeUsers
    private lateinit var protector: CredentialProtector
    private var time = 1_800_000_000_000L
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val ownerId = "00000000-0000-4000-8000-000000000001"
    private val operatorId = "00000000-0000-4000-8000-000000000002"

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, InventoryDatabase::class.java).build()
        sessions = AuthSessionManager()
        protector = TestProtector()
        service = FakeUsers(JSONArray().put(row(ownerId, "owner", "ADMIN")).put(row(operatorId, "field", "OPERATOR")))
        sync = UserSyncRepository(db, service, protector, sessions, { time })
        auth = AuthRepository(db, protector, sessions, { time })
    }
    @After fun close() { db.close() }
    private fun row(id: String, username: String, role: String): JSONObject = verifier.json()
        .put("id", id).put("username", username).put("displayName", "Nombre $username").put("role", role)
        .put("active", true).put("createdAt", 1000L).put("updatedAt", 1000L)
    private suspend fun seed() { assertTrue(sync.sync().isSuccess) }
    private suspend fun login(name: String = "owner", pin: String = "783294") = auth.login(name, pin.toCharArray())

    @Test fun pbkdf2MatchesKnownSha256Vector() {
        val actual = PasswordHasher.derive("password".toCharArray(), "salt".toByteArray(), 1).joinToString("") { "%02x".format(it.toInt() and 255) }
        assertEquals("120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b", actual)
        assertTrue(PasswordHasher.verify("783294".toCharArray(), verifier))
        assertFalse(PasswordHasher.verify("000000".toCharArray(), verifier))
    }
    @Test fun usernameNormalizationAndOfflineLoginNeverCallsService() = runBlocking {
        seed(); service.fail = true
        val calls = service.calls
        assertNull(login("  OwNeR  ")); assertEquals(ownerId, sessions.current.value!!.id)
        assertEquals(calls, service.calls)
        assertEquals("owner", normalizeUsername(" OWNER "))
    }
    @Test fun wrongPinAndInactiveUserUseGenericError() = runBlocking {
        seed(); val wrong = login(pin = "000000")
        service.rows.getJSONObject(0).put("active", false)
        // A second active admin is required in every valid central snapshot.
        service.rows.getJSONObject(1).put("role", "ADMIN"); service.version++
        seed(); assertEquals(wrong, login()); assertNull(sessions.current.value)
    }
    @Test fun fiveFailuresPersistAcrossRepositoryRecreationAndExpire() = runBlocking {
        seed()
        repeat(5) { assertNotNull(login(pin = "000000")) }
        val restarted = AuthRepository(db, protector, sessions, { time })
        assertTrue(restarted.login("owner", "783294".toCharArray())!!.contains("cinco minutos"))
        time += 300001
        assertNull(restarted.login("owner", "783294".toCharArray()))
        assertEquals(0, db.userDao().loginState()!!.failures)
    }
    @Test fun noCacheRequiresFirstSyncAndDoesNotCreateAdmin() = runBlocking {
        assertEquals(AuthRepository.INITIAL_SYNC, login()); assertEquals(0, db.userDao().countUsers())
    }
    @Test fun failedSyncPreservesUsersVersionAndTimestamp() = runBlocking {
        seed(); val before = db.userDao().getAllUsers(); val state = db.userDao().syncState()
        time += 1000; service.fail = true
        assertTrue(sync.sync().isFailure); assertEquals(before, db.userDao().getAllUsers()); assertEquals(state, db.userDao().syncState())
    }
    @Test fun equalVersionOnlyRefreshesLeaseWithoutReplacingCredentials() = runBlocking {
        seed(); val ciphertext = db.userDao().getAllUsers().map { it.credentialCiphertext }
        time += 1000; seed()
        assertEquals(1, service.downloads); assertEquals(ciphertext, db.userDao().getAllUsers().map { it.credentialCiphertext })
        assertEquals(time, db.userDao().syncState()!!.lastSuccessfulSyncAt)
    }
    @Test fun newerVersionDisablesUserAndUpdatesRole() = runBlocking {
        seed(); assertNull(login("field"))
        service.rows.getJSONObject(1).put("active", false); service.version++; seed()
        assertTrue(sessions.current.value!!.closing)
        assertTrue(runCatching { sessions.requireUser() }.isFailure)
        auth.logout(); assertNotNull(login("field"))
        service.rows.getJSONObject(1).put("active", true).put("role", "ADMIN"); service.version++; seed()
        assertNull(login("field")); assertEquals(UserRole.ADMIN, sessions.requireAdmin().role)
    }
    @Test fun roleChangesTakeEffectInActiveSession() = runBlocking {
        seed(); assertNull(login("field"))
        service.rows.getJSONObject(1).put("role", "ADMIN"); service.version++; seed()
        assertEquals(UserRole.ADMIN, sessions.requireAdmin().role)
        service.rows.getJSONObject(1).put("role", "OPERATOR"); service.version++; seed()
        assertTrue(runCatching { sessions.requireAdmin() }.isFailure)
    }
    @Test fun malformedSnapshotDoesNotPartiallyReplaceCache() = runBlocking {
        seed(); val before = db.userDao().getAllUsers()
        service.rows.getJSONObject(1).put("pinHash", "broken"); service.version++
        assertTrue(sync.sync().isFailure); assertEquals(before, db.userDao().getAllUsers()); assertEquals(1L, db.userDao().syncState()!!.usersVersion)
    }
    @Test fun databaseWriteFailureRollsBackSnapshotAndLease() = runBlocking {
        seed(); val before = db.userDao().getAllUsers(); val state = db.userDao().syncState()
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_user_snapshot BEFORE INSERT ON user_cache BEGIN SELECT RAISE(ABORT, 'test rollback'); END")
        service.version++; time += 1000
        assertTrue(sync.sync().isFailure)
        assertEquals(before, db.userDao().getAllUsers()); assertEquals(state, db.userDao().syncState())
    }
    @Test fun olderVersionAndDuplicateUsernameAreRejected() = runBlocking {
        seed(); service.version = 0; assertTrue(sync.sync().isFailure)
        service.version = 2; service.rows.getJSONObject(1).put("username", "owner")
        assertTrue(sync.sync().isFailure); assertEquals(2, db.userDao().countUsers())
    }
    @Test fun cacheOlderThan72HoursRequiresSyncButDoesNotEndExistingSession() = runBlocking {
        seed(); assertNull(login()); time += 72 * 3_600_000L + 1
        assertEquals(ownerId, sessions.requireUser().id)
        auth.logout(); assertEquals(AuthRepository.EXPIRED_CACHE, login())
        seed(); assertNull(login())
    }
    @Test fun exactly72HoursIsAllowedAndClockRollbackIsRejected() = runBlocking {
        seed(); time += 72 * 3_600_000L; assertNull(login())
        auth.logout(); time -= 73 * 3_600_000L; assertEquals(AuthRepository.EXPIRED_CACHE, login())
    }
    @Test fun operatorCannotRunAdminLogicOrReadAudit() = runBlocking {
        seed(); assertNull(login("field"))
        val repo = admin()
        assertTrue(runCatching { repo.authenticate("783294".toCharArray()) }.isFailure)
        assertTrue(runCatching { repo.mutate("updateUser", JSONObject().put("id", ownerId).put("displayName", "test")) }.isFailure)
        assertTrue(runCatching { repo.audit() }.isFailure)
        assertEquals(1, service.calls)
    }
    @Test fun offlineAdminActionIsRejectedWithoutLocalChanges() = runBlocking {
        seed(); assertNull(login()); val before = db.userDao().getAllUsers()
        val error = runCatching { admin(false).mutate("setUserActive", JSONObject().put("id", operatorId).put("active", false)) }.exceptionOrNull()
        assertEquals("OFFLINE", (error as UserServiceException).code); assertEquals(before, db.userDao().getAllUsers())
    }
    @Test fun clientPreventsDisablingAndDemotingLastAdmin() = runBlocking {
        seed(); assertNull(login()); val repo = admin(); repo.authenticate("783294".toCharArray())
        for ((action, payload) in listOf("setUserActive" to JSONObject().put("active", false), "changeUserRole" to JSONObject().put("role", "OPERATOR"))) {
            val error = runCatching { repo.mutate(action, payload.put("id", ownerId)) }.exceptionOrNull()
            assertEquals("LAST_ADMIN", (error as UserServiceException).code)
        }
        assertEquals(2, service.calls)
    }
    @Test fun adminSessionExpiresLocally() = runBlocking {
        seed(); assertNull(login()); val repo = admin(); repo.authenticate("783294".toCharArray()); time += 300001
        val error = runCatching { repo.mutate("setUserActive", JSONObject().put("id", operatorId).put("active", false)) }.exceptionOrNull()
        assertEquals("SESSION_EXPIRED", (error as UserServiceException).code)
    }
    @Test fun lockRequiresSameUserAndRetainsSessionIdentity() = runBlocking {
        seed(); assertNull(login()); sessions.lock()
        assertTrue(sessions.current.value!!.locked); assertNotNull(login("field")); assertNull(login())
        assertFalse(sessions.current.value!!.locked); assertEquals(ownerId, sessions.current.value!!.id)
    }
    @Test fun storedVerifierIsProtectedAndBoundToUser() = runBlocking {
        seed(); val user = db.userDao().getUserById(ownerId)!!
        assertFalse(user.credentialCiphertext.contains(verifier.pinHash))
        assertTrue(runCatching { protector.decrypt(user.credentialCiphertext, "user:$operatorId") }.isFailure)
        assertFalse(db.auditDao().recent().toString().contains(verifier.pinHash))
    }
    @Test fun fieldSessionOperatorComesFromAuthenticatedUser() = runBlocking {
        seed(); assertNull(login("field"))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val controller = FieldController(context, db, scope, sessions)
            val done = CompletableDeferred<String?>()
            controller.startSession("Proyecto", "R", "", "CD", "INCREASING", { done.complete(null) }, { done.complete(it) })
            assertNull(withTimeout(10000) { done.await() })
            assertEquals("Nombre field", db.inventoryDao().observeSession().first()!!.operator)
            assertTrue(db.auditDao().recent().any { it.action == "FIELD_SESSION_STARTED" && it.userId == operatorId })
        } finally { scope.cancel() }
    }
    @Test fun revokedTabletPreventsNewLoginAndSafelyMarksSession() = runBlocking {
        seed(); assertNull(login()); service.revoked = true
        assertTrue(sync.sync().isFailure); assertTrue(sessions.current.value!!.closing)
        assertEquals(2, db.userDao().countUsers()); auth.logout(); assertTrue(login()!!.contains("revocado"))
    }
    @Test fun changingUserAfterRestartClosesPreviousFieldWithoutRewritingOperator() = runBlocking {
        seed(); assertNull(login("owner"))
        db.inventoryDao().insertSession(com.tuempresa.inventariovial.data.entity.FieldSession("old-field", "Project", "Nombre owner", "device", null, null, null, startTime = 1))
        sessions.logout() // Simulate an in-memory session lost on process restart.
        assertNull(login("field"))
        assertNull(db.inventoryDao().currentSession())
        assertEquals("Nombre owner", db.inventoryDao().sessionById("old-field")!!.operator)
        assertNotNull(db.inventoryDao().sessionById("old-field")!!.endTime)
    }
    @Test fun migration7to8PreservesEveryExistingTable() = runBlocking {
        val schema = JSONObject(File("schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/7.json").readText()).getJSONObject("database")
        val entities = schema.getJSONArray("entities")
        val name = "auth-v7.db"; context.deleteDatabase(name)
        val file = context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i); val table = entity.getString("tableName")
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indexes = entity.optJSONArray("indices") ?: JSONArray()
                for (j in 0 until indexes.length()) old.execSQL(indexes.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                val fields = entity.getJSONArray("fields")
                val columns = (0 until fields.length()).map { fields.getJSONObject(it).getString("columnName") }
                val values = (0 until fields.length()).map { when(fields.getJSONObject(it).getString("affinity")) { "INTEGER" -> "1"; "REAL" -> "1.5"; "BLOB" -> "X'00'"; else -> "'legacy'" } }
                old.execSQL("INSERT INTO `$table` (${columns.joinToString { "`$it`" }}) VALUES (${values.joinToString()})")
            }
            old.version = 7
        }
        val migrated = Room.databaseBuilder(context, InventoryDatabase::class.java, name).addMigrations(InventoryDatabase.MIGRATION_7_8).build()
        try {
            assertEquals(0, migrated.userDao().countUsers()) // Forces complete Room schema validation.
            for (i in 0 until entities.length()) {
                val table = entities.getJSONObject(i).getString("tableName")
                migrated.openHelper.readableDatabase.query("SELECT COUNT(*) FROM `$table`").use { assertTrue(it.moveToFirst()); assertEquals(table, 1, it.getInt(0)) }
            }
            migrated.openHelper.readableDatabase.query("SELECT operator FROM field_sessions").use { assertTrue(it.moveToFirst()); assertEquals("legacy", it.getString(0)) }
            migrated.openHelper.readableDatabase.query("SELECT localPath FROM photos").use { assertTrue(it.moveToFirst()); assertEquals("legacy", it.getString(0)) }
        } finally { migrated.close(); context.deleteDatabase(name) }
    }
    private fun admin(online: Boolean = true) = AdminUsersRepository(db, service, sessions, sync, { online }, { time })
    private class FakeUsers(val rows: JSONArray) : UserService {
        var version = 1L; var calls = 0; var downloads = 0; var fail = false; var revoked = false
        override suspend fun call(action: String, fields: JSONObject): JSONObject {
            calls++
            if (revoked) throw UserServiceException("DEVICE_REVOKED")
            if (fail) throw java.io.IOException("offline fixture")
            if (action == "adminAuthenticate") return JSONObject().put("adminToken", "test-session").put("expiresInSeconds", 300)
            check(action == "syncUsers")
            val changed = fields.getLong("clientVersion") != version
            val response = JSONObject().put("success", true).put("changed", changed).put("version", version)
            if (changed) { downloads++; response.put("users", rows) }
            return response
        }
    }
    private class TestProtector : CredentialProtector {
        private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        override fun encrypt(plaintext: String, purpose: String): String {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key); updateAAD(purpose.toByteArray()) }
            return Base64.getEncoder().encodeToString(cipher.iv + cipher.doFinal(plaintext.toByteArray()))
        }
        override fun decrypt(ciphertext: String, purpose: String): String {
            val bytes = Base64.getDecoder().decode(ciphertext)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(0, 12))); updateAAD(purpose.toByteArray()) }
            return String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)))
        }
    }
    companion object { private val verifier by lazy { PasswordHasher.create("783294".toCharArray()) } }
}
