package com.example.tiorico.usecase

import com.example.tiorico.data.GameState
import com.example.tiorico.data.GameStatus

class VerifyGame {

    fun execute(gameState: GameState): GameState {

        val actives = gameState.players.count { it.isActive }

        val finished = gameState.currentTurn > gameState.maxTurns || actives <= 1

        return if (finished) {
            gameState.copy(status = GameStatus.FINALIZADO)
        } else gameState
    }
}
