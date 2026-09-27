package com.tuempresa.inventariovial.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.selection.SelectionContainer
import com.tuempresa.inventariovial.access.DeviceAccessManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun AuthShell(graph: AuthGraph, content: @Composable () -> Unit) {
    val session by graph.sessions.current.collectAsState()
    if (session == null) LoginScreen(graph) else {
        content()
        if (session!!.locked) Dialog(onDismissRequest = {}, properties = DialogProperties(
            dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize()) { LoginScreen(graph, session!!.username) }
        }
    }
}

@Composable
fun LoginScreen(graph: AuthGraph, lockedUsername: String? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by graph.database.userDao().observeSyncState().collectAsState(initial = null)
    val syncing by graph.busy.collectAsState()
    val syncMessage by graph.message.collectAsState()
    var username by remember { mutableStateOf(lockedUsername.orEmpty()) }
    var pin by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var configured by remember { mutableStateOf(graph.settings.configured) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    BackHandler(lockedUsername != null) { }
    Column(Modifier.fillMaxSize().padding(32.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Inventario Vial", style = MaterialTheme.typography.headlineLarge)
        if (lockedUsername != null) Text("Vuelve a ingresar tu PIN para continuar. La ficha abierta se conserva.")
        if (!configured) {
            Text("Configuración inicial del servicio de usuarios")
            Text("Solicita a GBO la URL del servicio y el token asignado a esta tablet.")
            Text("Huella de esta tablet:")
            SelectionContainer { Text(remember { DeviceAccessManager(context).fingerprint() }) }
            OutlinedTextField(url, { url = it }, label = { Text("URL HTTPS del servicio de usuarios") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(token, { token = it.trim() }, label = { Text("Token de esta tablet") },
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
            Button(enabled = !busy, onClick = {
                try { graph.settings.provision(url, token); token = ""; configured = true; graph.start(); scope.launch { graph.syncNow() } }
                catch (_: Exception) { message = "No se pudo guardar. Revisa la URL HTTPS /exec y el token asignado por GBO." }
            }) { Text("Guardar configuración y sincronizar") }
        } else {
            if (state == null) Text(AuthRepository.INITIAL_SYNC)
            OutlinedTextField(username, { username = it }, readOnly = lockedUsername != null,
                label = { Text("Usuario") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            PinField(pin, { pin = it }, "PIN")
            Button(enabled = !busy && state != null, onClick = {
                val entered = pin.toCharArray(); pin = ""; busy = true
                scope.launch {
                    try { message = graph.auth.login(username, entered) }
                    catch (cancel: CancellationException) { throw cancel }
                    catch (_: Exception) { message = "No se pudo iniciar sesión. Sincroniza usuarios e inténtalo nuevamente." }
                    finally { entered.fill('\u0000'); busy = false }
                }
            }) { Text(if (busy) "Verificando…" else "Iniciar sesión") }
            OutlinedButton(enabled = !syncing, onClick = { scope.launch { graph.syncNow() } }) {
                Text(if (syncing) "Actualizando…" else "Sincronizar usuarios")
            }
            if (state == null || state!!.usersVersion < 1) TextButton(enabled = !syncing, onClick = { scope.launch {
                try { graph.sync.resetInitialProvisioning(graph.settings); configured = false }
                catch (_: Exception) { message = "La configuración ya fue completada; contacta con GBO para cambiarla." }
            } }) { Text("Corregir configuración inicial") }
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        syncMessage?.let { Text(it) }
    }
}

@Composable
internal fun PinField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(value, { onChange(it.filter { c -> c in '0'..'9' }.take(12)) }, label = { Text(label) }, singleLine = true,
        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth())
}
