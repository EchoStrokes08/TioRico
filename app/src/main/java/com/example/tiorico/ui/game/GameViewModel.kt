package com.example.tiorico.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiorico.data.models.ActionDocument
import com.example.tiorico.data.models.ChatDocument
import com.example.tiorico.data.repository.DataResult
import com.example.tiorico.data.models.GameUiState
import com.example.tiorico.data.models.Player
import com.example.tiorico.data.repository.GameRepository
import com.example.tiorico.usecase.ApplyEvent
import com.example.tiorico.usecase.ResolveTurn
import com.example.tiorico.usecase.VerifyGame
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    private val repository = GameRepository()
    private val resolveTurn = ResolveTurn()
    private val applyEvent = ApplyEvent()
    private val verifyGame = VerifyGame()

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState = _uiState.asStateFlow()

    private var gameId: String = ""
    private var playerId: String = ""

    private var isInitialized = false
    private var firstTurnCreated = false
    private var turnLock = false
    private var isProcessingTurn = false

    private var lastProcessedTurnId = ""
    private var lastTurn = -1

    private var eventsJob: Job? = null

    fun initialize(gameId: String, playerId: String) {
        if (isInitialized && this.gameId == gameId && this.playerId == playerId) return
        
        clearState()
        
        this.gameId = gameId
        this.playerId = playerId
        isInitialized = true

        observeGame()
        observePlayers()
        observeTurns()
        observeChat()
        createFirstTurnIfHost()
    }

    private fun observeGame() {
        if (gameId.isBlank()) return
        viewModelScope.launch {
            repository.observeGame(gameId).collect { game ->
                if (game != null) {
                    _uiState.update {
                        it.copy(
                            game = game,
                            isLoading = false,
                            showTurnMessage = game.actualTurn > lastTurn && lastTurn != -1,
                            navigateToResult = game.status == "FINALIZADO"
                        )
                    }
                    if (game.actualTurn > lastTurn) {
                        lastTurn = game.actualTurn
                    }
                }
            }
        }
    }

    private fun observePlayers() {
        if (gameId.isBlank()) return
        viewModelScope.launch {
            repository.observePlayers(gameId).collect { players ->
                val currentPlayer = players.find { it.id == playerId }
                _uiState.update {
                    it.copy(
                        players = players,
                        currentPlayer = currentPlayer
                    )
                }
                // Si somos host, cada vez que cambien los jugadores checamos si ya terminaron todos
                val currentTurnId = _uiState.value.currentTurnId
                if (currentTurnId.isNotBlank()) {
                    checkAndResolveTurn(currentTurnId)
                }
            }
        }
    }

    private fun observeTurns() {
        if (gameId.isBlank()) return
        viewModelScope.launch {
            repository.observeCurrentTurn(gameId).collect { turn ->
                if (turn != null) {
                    _uiState.update { it.copy(currentTurnId = turn.id) }
                    
                    if (turn.status == "WAITING") {
                        checkAndResolveTurn(turn.id)
                    }
                    
                    if (turn.id != lastProcessedTurnId) {
                        observeEvents(turn.id)
                        lastProcessedTurnId = turn.id
                    }
                }
            }
        }
    }

    private fun observeEvents(turnId: String) {
        eventsJob?.cancel()
        eventsJob = viewModelScope.launch {
            repository.observeEvents(gameId, turnId, playerId).collect { events ->
                _uiState.update { it.copy(lastEvent = events.lastOrNull()) }
            }
        }
    }

    private fun observeChat() {
        if (gameId.isBlank()) return
        viewModelScope.launch {
            repository.observeChat(gameId).collect { messages ->
                _uiState.update { it.copy(chat = messages) }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || gameId.isBlank()) return
        viewModelScope.launch {
            val senderName = _uiState.value.currentPlayer?.name ?: "Jugador"
            val message = ChatDocument(
                senderId = playerId,
                senderName = senderName,
                message = text
            )
            repository.sendMessage(gameId, message)
        }
    }

    fun playAction(action: ActionDocument) {
        val state = _uiState.value
        val turnId = state.currentTurnId
        if (turnId.isBlank()) return

        viewModelScope.launch {
            val actionToSave = action.copy(
                playerId = playerId,
                playerName = state.currentPlayer?.name ?: "",
                cashBefore = state.currentPlayer?.cash ?: 0.0
            )
            repository.saveAction(gameId, turnId, actionToSave)

            // IMPORTANTE: Marcamos al jugador como "listo" (done = true)
            // y guardamos su última acción para que el ResolveTurn sepa qué procesar.
            state.currentPlayer?.let { p ->
                repository.updatePlayer(
                    gameId = gameId,
                    playerId = playerId,
                    newCash = p.cash,
                    lastAction = action.type,
                    active = p.active
                )
            }
        }
    }

    private fun checkAndResolveTurn(turnId: String) {
        if (turnLock || isProcessingTurn) return
        
        viewModelScope.launch {
            val players = _uiState.value.players
            val game = _uiState.value.game ?: return@launch
            val me = players.find { it.id == playerId }

            if (me?.isHost != true) return@launch

            val activos = players.count { it.active }
            val listos = players.count { it.active && it.done }

            if (listos >= activos && activos > 0) {
                turnLock = true
                isProcessingTurn = true

                val lockResult = repository.updateTurnStatus(gameId, turnId, "PROCESSING")
                if (lockResult is DataResult.Error) {
                    turnLock = false
                    isProcessingTurn = false
                    return@launch
                }

                val actionsResult = repository.getActions(gameId, turnId)
                val actions = (actionsResult as? DataResult.Success)?.data ?: emptyList()

                val turnResult = resolveTurn.execute(
                    players = players,
                    currentTurn = game.actualTurn,
                    maxTurns = game.maxTurns
                )
                
                val (playersWithEvent, events) = applyEvent.execute(
                    players = turnResult.updatedPlayers,
                    currentTurn = game.actualTurn,
                    maxTurns = game.maxTurns
                )

                if (events != null) {
                    events.forEach { event ->
                        repository.saveEvent(gameId, turnId, event)
                    }
                }

                playersWithEvent.forEach { player ->
                    repository.updatePlayer(
                        gameId = gameId,
                        playerId = player.id,
                        newCash = player.cash,
                        lastAction = player.lastAction ?: "",
                        active = player.active
                    )
                }

                val updatedGame = verifyGame.execute(game, playersWithEvent)
                
                if (updatedGame.status == "FINALIZADO") {
                    repository.updateGameStatus(gameId, "FINALIZADO")
                } else {
                    val nextTurn = turnResult.nextTurn
                    repository.advanceTurn(gameId, nextTurn)
                    repository.createTurn(gameId, nextTurn)
                    repository.resetDoneFlags(gameId)
                }

                repository.updateTurnStatus(gameId, turnId, "RESOLVED")
                
                isProcessingTurn = false
                turnLock = false
            }
        }
    }

    fun clearState() {
        isInitialized = false
        firstTurnCreated = false
        turnLock = false
        isProcessingTurn = false
        lastProcessedTurnId = ""
        gameId = ""
        playerId = ""
        lastTurn = -1
        eventsJob?.cancel()
        _uiState.value = GameUiState()
    }

    fun leaveGame(onDone: () -> Unit) {
        viewModelScope.launch {
            if (gameId.isNotBlank() && playerId.isNotBlank()) {
                repository.updatePlayer(gameId, playerId, 0.0, "SALIO", false)
            }
            clearState()
            onDone()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun hideTurnMessage() {
        _uiState.update { it.copy(showTurnMessage = false) }
    }

    fun onNavigatedToResult() {
        _uiState.update { it.copy(navigateToResult = false) }
    }

    private fun createFirstTurnIfHost() {
        viewModelScope.launch {
            repository.observePlayers(gameId).collect { list ->
                val me = list.firstOrNull { it.id == playerId }
                val amIHost = me?.isHost == true

                if (!amIHost || firstTurnCreated) return@collect

                firstTurnCreated = true
                val result = repository.createTurn(gameId, 1)

                if (result is DataResult.Success) {
                    _uiState.update {
                        it.copy(currentTurnId = result.data)
                    }
                }
                cancel()
            }
        }
    }
}
