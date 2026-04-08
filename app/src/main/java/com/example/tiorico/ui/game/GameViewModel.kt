package com.example.tiorico.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiorico.data.models.*
import com.example.tiorico.data.repository.DataResult
import com.example.tiorico.data.repository.GameRepository
import com.example.tiorico.usecase.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════════════════
// GameViewModel
//
// Responsabilidad: todo lo que pasa DURANTE el juego.
//   - El jugador elige su acción (AHORRAR, INVERTIR, GASTAR)
//   - Cuando todos jugaron → resuelve el turno
//   - Aplica eventos aleatorios
//   - Verifica si el juego terminó
//   - Maneja el chat
//
// Recibe gameId y playerId desde LobbyViewModel via navigation arguments.
// ═══════════════════════════════════════════════════════════════════════════════

data class GameUiState(
    val game: GameDocument?              = null,
    val players: List<PlayerDocument>    = emptyList(),
    val chat: List<ChatDocument>         = emptyList(),
    val currentTurnId: String            = "",
    val currentPlayer: PlayerDocument?   = null,  // el jugador local
    val lastEvent: EventDocument?        = null,  // evento del turno actual
    val isLoading: Boolean               = true,
    val errorMessage: String?            = null,
    val navigateToResult: Boolean        = false  // señal para ir a resultados
)

class GameViewModel(
    private val repository: GameRepository = GameRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // UseCases
    private val everyoneIsReady  = EveryoneIsReady()
    private val registerAction   = RegisterAction()
    private val resolveTurn      = ResolveTurn()
    private val verifyGame       = VerifyGame()

    // IDs de sesión — se setean al inicializar
    private var gameId   = ""
    private var playerId = ""


    // ════════════════════════════════════════════════════════════════════════
    // INICIALIZAR
    // Llamar desde GameFragment.onViewCreated() con los args de navegación
    // ════════════════════════════════════════════════════════════════════════

    fun initialize(gameId: String, playerId: String) {
        this.gameId   = gameId
        this.playerId = playerId
        observeGame()
        observePlayers()
        observeChat()
    }


    // ════════════════════════════════════════════════════════════════════════
    // OBSERVADORES
    // ════════════════════════════════════════════════════════════════════════

    private fun observeGame() {
        viewModelScope.launch {
            repository.observeGame(gameId).collect { game ->
                _uiState.update { it.copy(game = game, isLoading = false) }
            }
        }
    }

    // Este es el observer más importante:
    // Cada vez que un jugador actualiza su "done", Firestore emite la lista nueva.
    // Aquí verificamos si todos terminaron para resolver el turno.
    private fun observePlayers() {
        viewModelScope.launch {
            repository.observePlayers(gameId).collect { players ->

                // Actualiza el jugador local
                val currentPlayer = players.find { it.id == playerId }

                _uiState.update {
                    it.copy(
                        players       = players,
                        currentPlayer = currentPlayer
                    )
                }

                // ► UseCase: ¿todos los jugadores activos ya jugaron?
                val todosListos = everyoneIsReady.execute(players)
                if (todosListos && players.isNotEmpty()) {
                    processTurnEnd()
                }
            }
        }
    }

    private fun observeChat() {
        viewModelScope.launch {
            repository.observeChat(gameId).collect { messages ->
                _uiState.update { it.copy(chat = messages) }
            }
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // ACCIÓN DEL JUGADOR
    // Llamar cuando el jugador toca AHORRAR, INVERTIR o GASTAR
    // ════════════════════════════════════════════════════════════════════════

    fun playAction(action: ActionDocument) {
        val player = _uiState.value.currentPlayer ?: return
        val game   = _uiState.value.game          ?: return

        // Guarda de seguridad: no puede jugar si está eliminado
        if (!player.active) return

        viewModelScope.launch {

            // ► UseCase: marca al jugador como "done" y guarda la acción
            val updatedPlayer = registerAction.execute(player, action)

            // ► Repository: actualiza el jugador en Firestore
            // Esto dispara el observer de todos los dispositivos
            repository.updatePlayer(
                gameId   = gameId,
                playerId = playerId,
                newCash  = updatedPlayer.cash,
                lastAction = updatedPlayer.lastAction ?: "",
                active   = updatedPlayer.active
            )

            // ► Repository: guarda la acción en el historial del turno
            repository.saveAction(
                gameId  = gameId,
                turnId  = _uiState.value.currentTurnId,
                action  = ActionDocument(
                    playerId   = playerId,
                    playerName = player.name,
                    type       = action.type,
                    cashBefore = player.cash,
                    cashAfter  = updatedPlayer.cash
                )
            )
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // FIN DE TURNO
    // Se llama automáticamente desde observePlayers() cuando todos jugaron
    // Solo el host lo ejecuta para evitar escrituras duplicadas en Firestore
    // ════════════════════════════════════════════════════════════════════════

    private fun processTurnEnd() {
        // Solo el host resuelve el turno para evitar que todos escriban a la vez
        val isHost = _uiState.value.players
            .minByOrNull { it.name }?.id == playerId  // el primero alfabéticamente es el "host"

        if (!isHost) return

        val game    = _uiState.value.game    ?: return
        val players = _uiState.value.players

        viewModelScope.launch {

            // ► UseCase: calcula el nuevo cash de cada jugador según su acción
            val turnResult = resolveTurn.execute(
                players     = players,
                currentTurn = game.actualTurn,
                maxTurns    = game.maxTurns
            )

            // ► Repository: actualiza cada jugador en Firestore
            turnResult.updatedPlayers.forEach { player ->
                repository.updatePlayer(
                    gameId     = gameId,
                    playerId   = player.id,
                    newCash    = player.cash,
                    lastAction = player.lastAction ?: "",
                    active     = player.active
                )
            }

            // ► UseCase: ¿terminó el juego?
            val updatedGame = verifyGame.execute(game, turnResult.updatedPlayers)

            if (updatedGame.status == "FINALIZADO") {
                repository.updateGameStatus(gameId, "FINALIZADO")
                _uiState.update { it.copy(navigateToResult = true) }
                return@launch
            }

            // ► Siguiente turno: crear documento del turno y resetear flags
            val nextTurn = game.actualTurn + 1
            val turnResult2 = repository.createTurn(gameId, nextTurn)
            if (turnResult2 is DataResult.Success) {
                _uiState.update { it.copy(currentTurnId = turnResult2.data) }
            }

            repository.resetDoneFlags(gameId)
            repository.advanceTurn(gameId, nextTurn)
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // CHAT
    // ════════════════════════════════════════════════════════════════════════

    fun sendMessage(text: String) {
        val player = _uiState.value.currentPlayer ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            repository.sendMessage(
                gameId  = gameId,
                message = ChatDocument(
                    senderId   = playerId,
                    senderName = player.name,
                    message    = text.trim()
                )
            )
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // LIMPIEZA
    // ════════════════════════════════════════════════════════════════════════

    fun onNavigatedToResult() {
        _uiState.update { it.copy(navigateToResult = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}