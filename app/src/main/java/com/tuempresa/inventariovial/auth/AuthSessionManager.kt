package com.tuempresa.inventariovial.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex

data class AuthSession(val id: String, val username: String, val displayName: String, val role: UserRole,
    val closing: Boolean = false, val locked: Boolean = false)

class AuthSessionManager {
    internal val cacheMutex = Mutex()
    private val mutable = MutableStateFlow<AuthSession?>(null)
    val current = mutable.asStateFlow()
    @Synchronized fun login(user: UserCacheEntity) {
        val old = mutable.value
        check(old == null || old.id == user.id) { "Vuelve a autenticar al usuario de la ficha abierta." }
        mutable.value = AuthSession(user.id, user.username, user.displayName, UserRole.valueOf(user.role))
    }
    @Synchronized fun refresh(users: List<UserCacheEntity>) {
        val old = mutable.value ?: return
        val user = users.find { it.id == old.id }
        mutable.value = if (user == null || !user.active) old.copy(closing = true, locked = false)
        else old.copy(displayName = user.displayName, role = UserRole.valueOf(user.role))
    }
    @Synchronized fun lock() { mutable.value = mutable.value?.let { if (it.closing) it else it.copy(locked = true) } }
    @Synchronized fun logout() { mutable.value = null }
    fun requireUser(): AuthSession = checkNotNull(current.value?.takeUnless { it.closing || it.locked }) {
        "Inicia sesión con un usuario autorizado."
    }
    fun requireAdmin(): AuthSession = requireUser().also { check(it.role == UserRole.ADMIN) { "Se requiere un administrador." } }
}
