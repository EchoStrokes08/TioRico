package com.example.tiorico.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.tiorico.data.Action
import com.example.tiorico.data.GameState
import com.example.tiorico.data.GameStatus
import com.example.tiorico.data.Player
import com.example.tiorico.usecase.EveryoneIsReady
import com.example.tiorico.usecase.RegisterAction
import com.example.tiorico.usecase.ResolveTurn
import com.example.tiorico.usecase.VerifyGame

class GameViewModel(
    private val registerAction: RegisterAction,
    private val everyoneIsReady: EveryoneIsReady,
    private val resolveTurn: ResolveTurn,
    private val verifyGame: VerifyGame
) : ViewModel() {

    private val _gameState = MutableLiveData<GameState>()
    val gameState: LiveData<GameState> = _gameState

    fun startGame(jugadores: List<Player>) {
        _gameState.value = GameState(
            players = jugadores,
            currentTurn = 1,
            maxTurns = 10,
            status = GameStatus.JUGANDO
        )
    }

    fun onAccionSeleccionada(playerId: String, action: Action) {

        val estadoActual = _gameState.value ?: return

        // 1. registrar acción
        val actualizado = registerAction.execute(estadoActual, playerId, action)
        _gameState.value = actualizado

        // 2. verificar si todos jugaron
        if (everyoneIsReady.execute(actualizado)) {

            // 3. resolver turno
            var nuevoEstado = resolveTurn.execute(actualizado)

            // 4. verificar fin del juego (esto SÍ debe devolver GameState)
            nuevoEstado = verifyGame.execute(nuevoEstado)

            _gameState.value = nuevoEstado
        }
    }
}