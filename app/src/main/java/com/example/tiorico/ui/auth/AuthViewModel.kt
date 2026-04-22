package com.example.tiorico.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiorico.data.models.AuthUiState
import com.example.tiorico.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            try {
                val user = repository.getCurrentUser()

                if (user != null) {

                    //no bloquear UI con DB sync pesado
                    val userId = user.uid

                    _uiState.value = AuthUiState.Success(
                        userId = userId,
                        username = ""
                    )

                    _eventFlow.emit(UiEvent.NavigateToHome)
                }

            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Error cargando sesión")
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Campos vacíos")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            val result = repository.login(email, password)

            if (result.isSuccess) {
                try {
                    val (userId, username) = repository.getUserData()
                    _uiState.value = AuthUiState.Success(userId, username)
                    _eventFlow.emit(UiEvent.NavigateToHome)

                } catch (e: Exception) {
                    _uiState.value = AuthUiState.Error("Error obteniendo usuario")
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: ""

                val cleanMessage = when {
                    errorMsg.contains("badly formatted") ->
                        "Correo inválido"

                    errorMsg.contains("no user record") ->
                        "Usuario no existe"

                    errorMsg.contains("password is invalid") ->
                        "Contraseña incorrecta"

                    else -> "Error al iniciar sesión"
                }

                _uiState.value = AuthUiState.Error(cleanMessage)
            }
        }
    }
    sealed class UiEvent {
        object NavigateToHome : UiEvent()
        data class ShowSnackbar(val message: String) : UiEvent()
    }
}