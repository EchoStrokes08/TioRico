package com.example.tiorico.ui.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiorico.data.models.LobbyUiState
import com.example.tiorico.data.repository.LobbyRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LobbyViewModel : ViewModel() {

    private val repository = LobbyRepository()

    private val _uiState = MutableStateFlow(LobbyUiState())
    val uiState = _uiState.asStateFlow()

    private var currentRoomCode: String = ""

    // 🔥 NUEVO: cargar username desde Firebase
    fun loadUserName() {
        viewModelScope.launch {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid

                if (userId.isNullOrEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Usuario no autenticado"
                    )
                    return@launch
                }

                val db = FirebaseDatabase.getInstance().reference

                val snapshot = db.child("users")
                    .child(userId)
                    .get()
                    .await()

                val username = snapshot.child("username")
                    .getValue(String::class.java) ?: ""

                _uiState.value = _uiState.value.copy(
                    playerName = username
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Error cargando usuario"
                )
            }
        }
    }

    fun onNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(playerName = name)
    }

    fun onRoomCodeChanged(code: String) {
        _uiState.value = _uiState.value.copy(roomCode = code)
    }

    fun createGame() {
        val name = _uiState.value.playerName

        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Ingresa tu nombre")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = repository.createGame(name)

            if (result.isSuccess) {
                currentRoomCode = result.getOrNull() ?: ""

                listenRoom()

                _uiState.value = _uiState.value.copy(
                    isHost = true,
                    isLoading = false,
                    roomCode = currentRoomCode
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun joinGame() {
        val name = _uiState.value.playerName
        val roomCode = _uiState.value.roomCode

        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Ingresa tu nombre")
            return
        }

        if (roomCode.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Ingresa el código de sala")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = repository.joinGame(roomCode, name)

            if (result.isSuccess) {
                currentRoomCode = roomCode

                listenRoom()

                _uiState.value = _uiState.value.copy(
                    players = result.getOrNull() ?: emptyList(),
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    private fun listenRoom() {
        repository.listenPlayers(currentRoomCode) { players ->
            _uiState.value = _uiState.value.copy(players = players)
        }

        repository.listenGameStart(currentRoomCode) {
            _uiState.value = _uiState.value.copy(navigateToGame = true)
        }
    }

    fun startGame() {
        viewModelScope.launch {
            val result = repository.startGame(currentRoomCode)

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(navigateToGame = true)
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun onNavigatedToGame() {
        _uiState.value = _uiState.value.copy(navigateToGame = false)
    }
}