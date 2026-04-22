package com.example.tiorico.ui.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiorico.data.models.ChatDocument
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

    private var lastJoinAttemptCode: String = ""
    private var hasJoinedOnce: Boolean = false

    //cargar username desde Firebase
    fun loadUserName() {
        viewModelScope.launch {
            try {
                val user = FirebaseAuth.getInstance().currentUser ?: return@launch

                val snapshot = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.uid)
                    .get()
                    .await()

                val username = snapshot.getString("username")
                    ?: user.email ?: "Jugador"

                _uiState.value = _uiState.value.copy(playerName = username)

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

    fun createGame() {
        val name = _uiState.value.playerName

        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Ingresa tu nombre")
            return
        }
        _uiState.value = _uiState.value.copy(errorMessage = null)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            leaveCurrentRoom() // Esperamos a salir de cualquier sala previa

            val result = repository.createGame(name)
            // ... rest of the logic

            if (result.isSuccess) {
                val (gameId, roomCode, playerId) = result.getOrNull()!!

                currentRoomCode = gameId

                _uiState.value = _uiState.value.copy(
                    isHost = true,
                    isLoading = false,
                    roomCode = roomCode,
                    playerId = playerId,
                    gameId = gameId
                )
                
                listenRoom(gameId, playerId)
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

        if (hasJoinedOnce && roomCode == lastJoinAttemptCode) {
            return
        }

        _uiState.value = _uiState.value.copy(errorMessage = null)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            leaveCurrentRoom() // Esperamos a salir de cualquier sala previa

            val result = repository.joinGame(roomCode, name)
            // ... rest of the logic

            if (result.isSuccess) {
                val (gameId, playerId) = result.getOrNull()!!

                currentRoomCode = gameId

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    playerId = playerId,
                    roomCode = roomCode,
                    gameId = gameId,
                )
                
                listenRoom(gameId, playerId)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    private var hasSeenHost = false

    fun listenRoom(gameId: String, currentPlayerId: String) {
        hasSeenHost = false
        viewModelScope.launch {
            repository.listenPlayers(gameId).collect { players ->
                
                // Primero actualizamos la lista de jugadores y mi estado de Host actual
                val amIHost = players.find { it.id == currentPlayerId }?.isHost ?: false
                
                if (players.any { it.isHost }) {
                    hasSeenHost = true
                }

                _uiState.value = _uiState.value.copy(
                    players = players,
                    isHost = amIHost
                )

                // Lógica de recalcular Host:
                // Solo si NO hay ningún host en la sala Y hay al menos un jugador
                if (players.isNotEmpty() && players.none { it.isHost }) {
                    // Solo promovemos si ya vimos un host antes (indicando que el host salió)
                    // o si hay más de un jugador (indicando que no es solo el snapshot inicial de carga)
                    if (hasSeenHost || players.size > 1) {
                        val nextHostCandidate = players.minByOrNull { it.id } 
                        
                        if (nextHostCandidate?.id == currentPlayerId) {
                            repository.promoteToHost(gameId, currentPlayerId)
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            repository.listenGameStart(gameId).collect { started ->
                if (started) {
                    _uiState.value = _uiState.value.copy(
                        navigateToGame = true
                    )
                }
            }
        }
        
        observeChat(gameId)
    }

    private fun observeChat(gameId: String) {
        viewModelScope.launch {
            repository.observeChat(gameId).collect { messages ->
                _uiState.value = _uiState.value.copy(chat = messages)
            }
        }
    }

    fun sendMessage(text: String) {
        val gameId = _uiState.value.gameId
        val playerId = _uiState.value.playerId
        val name = _uiState.value.playerName
        if (text.isBlank() || gameId.isBlank()) return

        viewModelScope.launch {
            val message = ChatDocument(
                senderId = playerId,
                senderName = name,
                message = text
            )
            repository.sendMessage(gameId, message)
        }
    }

    fun startGame() {
        viewModelScope.launch {

            val gameId = currentRoomCode
            if (_uiState.value.players.size < 2) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Se necesitan al menos 2 jugadores"
                )
                return@launch
            }

            if (gameId.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error al iniciar la partida"
                )
                return@launch
            }

            val result = repository.startGame(gameId)

            if (!result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun onNavigatedToGame() {
        _uiState.value = _uiState.value.copy(navigateToGame = false)
    }

    fun resetState() {
        currentRoomCode = ""
        hasJoinedOnce = false
        lastJoinAttemptCode = ""
        _uiState.value = LobbyUiState() //limpia todo
    }
    fun onRoomCodeChanged(code: String) {
        val formatted = code.uppercase()

        // si cambia el código → permitir join otra vez
        if (formatted != _uiState.value.roomCode) {
            lastJoinAttemptCode = ""
            hasJoinedOnce = false
        }

        _uiState.value = _uiState.value.copy(
            roomCode = formatted
        )
    }
    private suspend fun leaveCurrentRoom() {
        val gameId = currentRoomCode
        val playerId = _uiState.value.playerId

        if (gameId.isBlank() || playerId.isBlank()) return

        repository.leaveRoom(gameId, playerId)
    }

    fun onLogout() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            leaveCurrentRoom() // Ahora esperamos a que se borre de Firestore antes de desloguear

            FirebaseAuth.getInstance().signOut()
            resetState()
        }
    }

    fun leaveRoom() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            leaveCurrentRoom()
            resetState()
        }
    }
}
