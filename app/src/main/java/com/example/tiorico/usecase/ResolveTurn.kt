package com.example.tiorico.usecase

import com.example.tiorico.data.ActionDocument
import com.example.tiorico.data.PlayerDocument

data class TurnResult(
    val updatedPlayers: List<PlayerDocument>,
    val nextTurn: Int
)

class ResolveTurn {

    fun execute(
        players: List<PlayerDocument>,
        currentTurn: Int,
        maxTurns: Int
    ): TurnResult {

        val factor = currentTurn.toDouble() / maxTurns

        val updatedPlayers = players.map { player ->

            if (!player.active) return@map player

            var dinero = player.cash

            when (player.lastAction) {

                "AHORRAR" -> {
                    val ganancia = (50.0 * (1 - factor))
                    dinero += ganancia
                }

                "INVERTIR" -> {
                    val probGanar = 0.6 - (factor * 0.3)
                    val random = Math.random()
                    dinero += if (random < probGanar) 100.0 else -50.0
                }

                "GASTAR" -> {
                    val costo = (50 * (1 + factor))
                    dinero -= costo
                }
            }

            player.copy(
                cash       = dinero,
                active     = dinero > 0,
                lastAction = "",
                done       = false       // reset para el siguiente turno
            )
        }

        return TurnResult(
            updatedPlayers = updatedPlayers,
            nextTurn       = currentTurn + 1
        )
    }
}