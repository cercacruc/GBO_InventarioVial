package com.tuempresa.inventariovial.auth

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.SystemClock
import androidx.room.withTransaction
import com.tuempresa.inventariovial.access.DeviceAccessManager
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.tracking.TrackCaptureService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One graph for the UI and Worker: serializes syncs and holds sessions only in memory. */
class AuthGraph private constructor(private val context: Context) {
    val database = InventoryDatabase.getInstance(context)
    val sessions = AuthSessionManager()
    private val protector = KeystoreCredentialProtector()
    val settings = UserServiceSettings(context, protector)
    private val service = HttpsUserService(context, settings)
    val auth = AuthRepository(database, protector, sessions,
        canSwitchFieldUser = { TrackCaptureService.activeRecordId.value == null })
    val sync = UserSyncRepository(database, service, protector, sessions)
    val admin = AdminUsersRepository(database, service, sessions, sync, { hasUserServiceNetwork(context) })
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var backgroundAt: Long? = null
    private var monitoring = false
    private val mutableMessage = MutableStateFlow<String?>(null)
    val message = mutableMessage.asStateFlow()
    private val mutableBusy = MutableStateFlow(false)
    val busy = mutableBusy.asStateFlow()
    var safeAtHome = true
    fun foreground() {
        val elapsed = backgroundAt?.let { SystemClock.elapsedRealtime() - it } ?: 0L
        backgroundAt = null
        if (elapsed >= 15 * 60_000) { admin.forgetAuthorization(); sessions.lock() }
        start()
    }
    fun background() { backgroundAt = SystemClock.elapsedRealtime(); admin.forgetAuthorization() }
    fun start() { scope.launch {
        try {
            if (!DeviceAccessManager(context).check().authorized) return@launch
        if (!monitoring) {
            monitoring = true
            var connected = false
            context.getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    val available = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    if (available && !connected && settings.configured) UserSyncWorker.enqueue(context)
                    connected = available
                }
                override fun onLost(network: Network) { connected = false }
            })
        }
        if (settings.configured) {
            UserSyncWorker.schedule(context)
            UserSyncWorker.enqueue(context)
        }
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { mutableMessage.value = "No se pudo preparar la sincronización de usuarios. Reintenta al abrir la app." }
    } }
    suspend fun syncNow() {
        if (mutableBusy.value) return
        mutableBusy.value = true
        try {
            val result = sync.sync()
            mutableMessage.value = result.fold({ "Usuarios actualizados." }, { it.message })
        } finally { mutableBusy.value = false }
    }
    suspend fun logoutAtHome() {
        check(safeAtHome && TrackCaptureService.activeRecordId.value == null) { "Guarda la ficha y finaliza el recorrido antes de salir." }
        val user = sessions.current.value
        database.withTransaction {
            val hasField = database.inventoryDao().currentSession() != null
            database.inventoryDao().closeSessions(System.currentTimeMillis())
            if (hasField) database.auditDao().insert(AuditLogEntity(userId = user?.id, username = user?.username, action = "FIELD_SESSION_ENDED"))
        }
        admin.forgetAuthorization()
        runCatching { com.tuempresa.inventariovial.server.scheduleServerSync(context) }
        auth.logout()
    }
    companion object {
        @Volatile private var instance: AuthGraph? = null
        fun get(context: Context): AuthGraph = instance ?: synchronized(this) {
            instance ?: AuthGraph(context.applicationContext).also { instance = it }
        }
    }
}
