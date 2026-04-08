package com.example.tiorico.ui.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiorico.data.models.GameDocument
import com.example.tiorico.data.models.PlayerDocument
import com.example.tiorico.data.repository.DataResult
import com.example.tiorico.data.repository.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

// ═══════════════════════════════════════════════════════════════════════════════
// LobbyViewModel
//
// Responsabilidad: todo lo que pasa ANTES de que empiece el juego.
//   - El host crea la sala y obtiene un roomCode para compartir
//   - Los jugadores entran con el roomCode
//   - Todos esperan en la sala viendo quién se conectó
//   - El host arranca el juego cuando quiere
//
// Este ViewModel NO sabe nada de turnos ni acciones — eso es GameViewModel.
// ═══════════════════════════════════════════════════════════════════════════════

data class LobbyUiState(
    val playerName: String           = "",   // nombre que escribió el jugador
    val playerId: String             = "",   // UUID generado localmente
    val game: GameDocument?          = null, // la sala actual
    val players: List<PlayerDocument> = emptyList(),
    val isHost: Boolean              = false,// ¿este jugador creó la sala?
    val isLoading: Boolean           = false,
    val errorMessage: String?        = null,
    val navigateToGame: Boolean      = false // señal para navegar al juego
)

class LobbyViewModel(
    private val repository: GameRepository = GameRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LobbyUiState())
    val uiState: StateFlow<LobbyUiState> = _uiState.asStateFlow()


    // ════════════════════════════════════════════════════════════════════════
    // PASO 1 — El jugador escribe su nombre
    // Llamar cada vez que cambia el texto del campo nombre
    // ════════════════════════════════════════════════════════════════════════

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(playerName = name) }
    }


    // ════════════════════════════════════════════════════════════════════════
    // PASO 2A — Crear sala (el host)
    // Genera el roomCode, crea la partida en Firestore y se une como primer jugador
    // ════════════════════════════════════════════════════════════════════════

    fun createGame(maxTurns: Int = 10) {
        val name = _uiState.value.playerName.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Escribe tu nombre primero") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Genera un id local para este jugador (no necesitamos Auth)
            val playerId = UUID.randomUUID().toString()

            when (val result = repository.createGame(maxTurns)) {
                is DataResult.Success -> {
                    val gameId = result.data

                    // Una vez creada la sala, el host se une como jugador
                    repository.joinGame(
                        gameId = gameId,
                        player = PlayerDocument(
                            id     = playerId,
                            name   = name,
                            cash   = 1000.0,
                            active = true,
                            done   = false
                        )
                    )

                    // Guarda el playerId localmente para identificar al host
                    _uiState.update {
                        it.copy(
                            playerId  = playerId,
                            isHost    = true,
                            isLoading = false
                        )
                    }

                    // Empieza a observar la sala
                    startObserving(gameId)
                }
                is DataResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // PASO 2B — Unirse a sala (los demás jugadores)
    // Busca la sala por roomCode y agrega al jugador
    // ════════════════════════════════════════════════════════════════════════

    fun joinGame(roomCode: String) {
        val name = _uiState.value.playerName.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Escribe tu nombre primero") }
            return
        }
        if (roomCode.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Escribe el código de sala") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Busca la sala por el código de 6 letras
            when (val result = repository.findGameByCode(roomCode.uppercase())) {
                is DataResult.Success -> {
                    val game = result.data
                    if (game == null) {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = "Sala no encontrada")
                        }
                        return@launch
                    }

                    val playerId = UUID.randomUUID().toString()

                    repository.joinGame(
                        gameId = game.id,
                        player = PlayerDocument(
                            id     = playerId,
                            name   = name,
                            cash   = 1000.0,
                            active = true,
                            done   = false
                        )
                    )

                    _uiState.update {
                        it.copy(
                            playerId  = playerId,
                            isHost    = false,
                            isLoading = false
                        )
                    }

                    startObserving(game.id)
                }
                is DataResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // OBSERVADORES — escuchan Firestore en tiempo real
    // Se activan apenas el jugador entra a la sala (crea o se une)
    // ════════════════════════════════════════════════════════════════════════

    private fun startObserving(gameId: String) {
        observeGame(gameId)
        observePlayers(gameId)
    }

    // Observa el documento de la partida
    // Cuando el host cambia status a "JUGANDO" → todos navegan al juego
    private fun observeGame(gameId: String) {
        viewModelScope.launch {
            repository.observeGame(gameId).collect { game ->
                _uiState.update { it.copy(game = game) }

                // Si el host arrancó el juego, navegar a GameFragment
                if (game?.status == "JUGANDO") {
                    _uiState.update { it.copy(navigateToGame = true) }
                }
            }
        }
    }

    // Observa la lista de jugadores en tiempo real
    // Cada vez que alguien se une → la pantalla se actualiza automáticamente
    private fun observePlayers(gameId: String) {
        viewModelScope.launch {
            repository.observePlayers(gameId).collect { players ->
                _uiState.update { it.copy(players = players) }
            }
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // ARRANCAR EL JUEGO — solo el host puede hacer esto
    // ════════════════════════════════════════════════════════════════════════

    fun startGame() {
        val game = _uiState.value.game ?: return
        if (!_uiState.value.isHost) return
        if (_uiState.value.players.size < 2) {
            _uiState.update { it.copy(errorMessage = "Se necesitan al menos 2 jugadores") }
            return
        }

        viewModelScope.launch {
            // Crea el primer turno
            repository.createTurn(game.id, 1)
            // Cambia el estado → todos los observers detectan el cambio y navegan
            repository.updateGameStatus(game.id, "JUGANDO")
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // LIMPIEZA
    // ════════════════════════════════════════════════════════════════════════

    fun onNavigatedToGame() {
        _uiState.update { it.copy(navigateToGame = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}