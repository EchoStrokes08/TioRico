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
    private var lastCreatedGameId: String = ""
    private val repository = LobbyRepository()

    private val _uiState = MutableStateFlow(LobbyUiState())
    val uiState = _uiState.asStateFlow()

    private var currentRoomCode: String = ""

    // 🔥 NUEVO: cargar username desde Firebase
    fun loadUserName() {
        viewModelScope.launch {
            try {
                val auth = FirebaseAuth.getInstance()
                val user = auth.currentUser ?: return@launch

                val db = FirebaseDatabase.getInstance().reference

                val snapshot = db.child("users")
                    .child(user.uid)
                    .get()
                    .await()

                val username = snapshot.child("username")
                    .getValue(String::class.java) ?: user.email ?: "Jugador"

                _uiState.value = _uiState.value.copy(
                    playerName = username
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    playerName = FirebaseAuth.getInstance().currentUser?.email ?: "Jugador"
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
                val (roomCode, playerId) = result.getOrNull()!!

                currentRoomCode = roomCode

                listenRoom(roomCode)

                _uiState.value = _uiState.value.copy(
                    isHost = true,
                    isLoading = false,
                    roomCode = roomCode,
                    playerId = playerId   // 🔥 NUEVO
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
                val playerId = result.getOrNull()!!

                currentRoomCode = roomCode

                listenRoom(roomCode)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    playerId = playerId,  // 🔥 NUEVO
                    roomCode = roomCode
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun listenRoom(roomCode: String) {
        repository.listenPlayers(roomCode) { players ->
            _uiState.value = _uiState.value.copy(players = players)
        }

        repository.listenGameStart(roomCode) {
            if (!_uiState.value.navigateToGame) {
                _uiState.value = _uiState.value.copy(
                    navigateToGame = true
                )
            }
        }
    }

    fun startGame() {
        viewModelScope.launch {
            val roomCode = _uiState.value.roomCode

            println("🔥 START GAME CLICKED")
            println("ROOM CODE UI: $roomCode")

            if (roomCode.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "RoomCode vacío"
                )
                return@launch
            }

            val result = repository.startGame(roomCode)

            if (result.isSuccess) {
                println("✅ START GAME OK")
            } else {
                println("❌ ERROR: ${result.exceptionOrNull()}")
            }
        }
    }

    fun onNavigatedToGame() {
        _uiState.value = _uiState.value.copy(navigateToGame = false)
    }
}