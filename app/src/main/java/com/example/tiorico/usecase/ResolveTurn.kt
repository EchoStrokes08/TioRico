package com.example.tiorico.usecase

import com.example.tiorico.data.Action
import com.example.tiorico.data.GameState

class ResolveTurn{

    fun execute(gameState: GameState): GameState {

        val factor = gameState.currentTurn.toDouble() / gameState.maxTurns

        val jugadoresActualizados = gameState.players.map { player ->

            if (!player.isActive) return@map player

            var dinero = player.cash

            when (player.lastAction) {

                Action.AHORRAR -> {
                    val ganancia = (50 * (1 - factor)).toInt()
                    dinero += ganancia
                }

                Action.INVERTIR -> {
                    val probGanar = 0.6 - (factor * 0.3)
                    val random = Math.random()

                    dinero += if (random < probGanar) 100 else -50
                }

                Action.GASTAR -> {
                    val costo = (50 * (1 + factor)).toInt()
                    dinero -= costo
                }

                else -> {}
            }

            player.copy(
                cash = dinero,
                isActive = dinero > 0,
                lastAction = null,
                ready = false
            )
        }

        return gameState.copy(
            players = jugadoresActualizados,
            currentTurn = gameState.currentTurn + 1
        )
    }
}