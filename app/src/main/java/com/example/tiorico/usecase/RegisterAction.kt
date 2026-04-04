package com.example.tiorico.usecase

import com.example.tiorico.data.Action
import com.example.tiorico.data.GameState

class RegisterAction {

    fun execute(
        gameState: GameState,
        playerId: String,
        action: Action
    ): GameState {

        val updatePlayers = gameState.players.map { player ->
            if (player.id == playerId) {
                player.copy(
                    lastAction = action,
                    ready = true
                )
            } else player
        }

        return gameState.copy(players = updatePlayers)
    }
}