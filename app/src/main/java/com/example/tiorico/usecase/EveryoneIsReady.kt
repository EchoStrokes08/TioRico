package com.example.tiorico.usecase

import com.example.tiorico.data.GameState

class EveryoneIsReady {
    fun execute(
        gameState: GameState
    ): Boolean{
        return gameState.players
            .filter {it.isActive}
            .all {it.ready}
    }
}