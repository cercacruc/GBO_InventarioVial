package com.tuempresa.inventariovial.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AuthHomePanel(graph: AuthGraph) {
    val session by graph.sessions.current.collectAsState()
    val scope = rememberCoroutineScope()
    var showAdmin by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("${session?.displayName.orEmpty()} · ${session?.role?.name.orEmpty()}")
            if (session?.closing == true) Text("Tu acceso fue desactivado. Guarda y finaliza el recorrido abierto para salir.")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (session?.role == UserRole.ADMIN && session?.closing == false) OutlinedButton(onClick = { showAdmin = true }) { Text("Administración") }
                OutlinedButton(onClick = { scope.launch {
                    try { graph.logoutAtHome() } catch (_: Exception) { message = "Finaliza el recorrido antes de salir." }
                } }) { Text("Cerrar sesión") }
            }
            message?.let { Text(it) }
        }
    }
    if (showAdmin && session?.role == UserRole.ADMIN && session?.closing == false) Dialog(
        onDismissRequest = { showAdmin = false; graph.admin.forgetAuthorization() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) { AdminUsersScreen(graph) { showAdmin = false; graph.admin.forgetAuthorization() } }
    }
}

@Composable
fun AdminUsersScreen(graph: AuthGraph, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val users by graph.database.userDao().observeUsers().collectAsState(initial = emptyList())
    val syncState by graph.database.userDao().observeSyncState().collectAsState(initial = null)
    var adminPin by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<UserCacheEntity?>(null) }
    var action by remember { mutableStateOf<String?>(null) }
    var audit by remember { mutableStateOf<List<AuditLogEntity>?>(null) }
    fun runTask(task: suspend () -> String) {
        busy = true
        scope.launch {
            try { message = task() }
            catch (cancel: CancellationException) { throw cancel }
            catch (e: Exception) { message = if (e is UserServiceException) e.message else "No se pudo completar la acción. Revisa los datos y la conexión." }
            finally { busy = false }
        }
    }
    LazyColumn(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TextButton(onClick = onClose) { Text("Volver") }
            Text("Administración · Usuarios", style = MaterialTheme.typography.headlineMedium)
            Text(syncState?.let { "Usuarios actualizados: " + SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(it.lastSuccessfulSyncAt)) } ?: "Pendiente de sincronización")
            Text("Sin conexión puedes consultar los usuarios sincronizados. Los cambios requieren Internet.")
            PinField(adminPin, { adminPin = it }, "Tu PIN de administrador")
            Button(enabled = !busy, onClick = {
                val pin = adminPin.toCharArray(); adminPin = ""
                runTask { try { graph.admin.authenticate(pin); "Administración autorizada durante cinco minutos." } finally { pin.fill('\u0000') } }
            }) { Text("Autorizar cambios online") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = !busy, onClick = { editing = null; action = "createUser" }) { Text("Nuevo usuario") }
                OutlinedButton(enabled = !busy, onClick = { runTask { graph.syncNow(); graph.message.value.orEmpty() } }) { Text("Sincronizar usuarios") }
                OutlinedButton(enabled = !busy, onClick = { runTask { audit = graph.admin.audit(); "Auditoría local: últimos 200 eventos." } }) { Text("Auditoría") }
            }
            message?.let { Text(it) }
        }
        items(users, key = { it.id }) { user ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("${user.displayName} · ${user.username}", style = MaterialTheme.typography.titleMedium)
                    Text("${user.role} · ${if(user.active) "ACTIVO" else "INACTIVO"}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(enabled = !busy, onClick = { editing = user; action = "updateUser" }) { Text("Editar nombre") }
                        TextButton(enabled = !busy, onClick = { editing = user; action = "resetUserPin" }) { Text("Cambiar PIN") }
                        TextButton(enabled = !busy, onClick = { editing = user; action = "changeUserRole" }) { Text("Cambiar rol") }
                        TextButton(enabled = !busy, onClick = { editing = user; action = "setUserActive" }) { Text(if(user.active) "Desactivar" else "Activar") }
                    }
                }
            }
        }
        audit?.let { rows -> items(rows, key = { "audit:${it.id}" }) { event ->
            Text("${SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(event.timestamp))} · ${event.username.orEmpty()} · ${event.action}")
        } }
    }
    action?.let { selectedAction -> UserEditDialog(selectedAction, editing, onClose = { action = null }) { fields ->
        action = null
        runTask { graph.admin.mutate(selectedAction, fields) }
    } }
}

@Composable
private fun UserEditDialog(action: String, user: UserCacheEntity?, onClose: () -> Unit, onSubmit: (JSONObject) -> Unit) {
    var username by remember { mutableStateOf("") }
    var name by remember { mutableStateOf(user?.displayName.orEmpty()) }
    var role by remember { mutableStateOf(user?.role ?: "OPERATOR") }
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = { if (!busy) onClose() }, title = { Text(if (user == null) "Nuevo usuario" else user.displayName) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (action == "createUser") OutlinedTextField(username, { username = it }, label = { Text("Usuario: letras, números, punto, guion") })
            if (action in setOf("createUser", "updateUser")) OutlinedTextField(name, { name = it }, label = { Text("Nombre completo") })
            if (action in setOf("createUser", "changeUserRole")) Row {
                UserRole.entries.forEach { candidate -> FilterChip(selected = role == candidate.name, onClick = { role = candidate.name }, label = { Text(candidate.name) }) }
            }
            if (action in setOf("createUser", "resetUserPin")) {
                PinField(pin, { pin = it }, "Nuevo PIN: 6 a 12 dígitos")
                PinField(confirmation, { confirmation = it }, "Repetir PIN")
            }
            if (action == "setUserActive") Text(if (user!!.active) "¿Desactivar este usuario en todas las tablets después de sincronizar?" else "¿Activar este usuario?")
            error?.let { Text(it) }
        }
    }, confirmButton = { Button(enabled = !busy, onClick = {
        val entered = pin.toCharArray()
        if (action in setOf("createUser", "resetUserPin") && (!PasswordHasher.validPin(entered) || pin != confirmation)) {
            entered.fill('\u0000'); error = "Ingresa y repite un PIN de 6 a 12 dígitos."
        } else if (action in setOf("createUser", "updateUser") && (name.isBlank() || name.length > 120)) {
            entered.fill('\u0000'); error = "Ingresa un nombre de hasta 120 caracteres."
        } else if (action == "createUser" && !normalizeUsername(username).matches(Regex("[a-z0-9._-]{3,64}"))) {
            entered.fill('\u0000'); error = "El usuario debe tener entre 3 y 64 caracteres válidos."
        } else {
            pin = ""; confirmation = ""; busy = true
            scope.launch {
                try {
                    val fields = JSONObject()
                    user?.let { fields.put("id", it.id) }
                    if (action == "createUser") fields.put("username", normalizeUsername(username))
                    if (action in setOf("createUser", "updateUser")) fields.put("displayName", name.trim())
                    if (action in setOf("createUser", "changeUserRole")) fields.put("role", role)
                    if (action == "setUserActive") fields.put("active", !user!!.active)
                    if (action in setOf("createUser", "resetUserPin")) fields.put("credential", withContext(Dispatchers.Default) { PasswordHasher.create(entered).json() })
                    onSubmit(fields)
                } finally { entered.fill('\u0000'); busy = false }
            }
        }
    }) { Text("Guardar en el servicio central") } }, dismissButton = { TextButton(enabled = !busy, onClick = onClose) { Text("Cancelar") } })
}
