package com.celato.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface AuthRepository {
    fun currentUser(): User?
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signUp(name: String, email: String, password: String): Result<User>
    fun signOut()
}

data class AuthUiState(val loading: Boolean = false, val error: String? = null)

class AuthViewModel : ViewModel() {

    private val repo: AuthRepository = FirebaseAuthRepository()
    private val users = UserRepository()

    // Pehle se logged in ho to seedha app khulega
    private val _user = MutableStateFlow(repo.currentUser())
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui.asStateFlow()

    init {
        // Pehle se logged-in account ka profile doc bhi ban jaye (search ke liye)
        repo.currentUser()?.let { u -> viewModelScope.launch { runCatching { users.ensureUserDoc(u) } } }
    }

    fun submit(isSignUp: Boolean, name: String, email: String, password: String) {
        val error = validate(isSignUp, name, email, password)
        if (error != null) {
            _ui.value = AuthUiState(error = error)
            return
        }
        viewModelScope.launch {
            _ui.value = AuthUiState(loading = true)
            val result = if (isSignUp) repo.signUp(name.trim(), email.trim(), password)
            else repo.signIn(email.trim(), password)
            result
                .onSuccess {
                    runCatching { users.ensureUserDoc(it) }
                    _user.value = it
                    _ui.value = AuthUiState()
                }
                .onFailure { _ui.value = AuthUiState(error = friendlyMessage(it)) }
        }
    }

    fun logout() {
        repo.signOut()
        _user.value = null
    }

    fun clearError() {
        _ui.value = _ui.value.copy(error = null)
    }

    private fun friendlyMessage(t: Throwable): String = when (t) {
        is FirebaseAuthInvalidCredentialsException,
        is FirebaseAuthInvalidUserException -> "Email ya password galat hai"
        is FirebaseAuthUserCollisionException -> "Ye email pehle se registered hai, Log in karo"
        is FirebaseNetworkException -> "Internet check karo"
        else -> t.message ?: "Kuch gadbad ho gayi, dobara try karo"
    }

    private fun validate(isSignUp: Boolean, name: String, email: String, password: String): String? = when {
        isSignUp && name.isBlank() -> "Name daalo"
        !email.contains("@") || !email.contains(".") -> "Valid email daalo"
        password.length < 6 -> "Password kam se kam 6 characters ka ho"
        else -> null
    }
}
