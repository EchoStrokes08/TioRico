package com.example.tiorico.usecase

import com.example.tiorico.data.models.Player

data class TurnResult(
    val updatedPlayers: List<Player>,
    val nextTurn: Int
)

class ResolveTurn {

    fun execute(
        players: List<Player>,
        currentTurn: Int,
        maxTurns: Int
    ): TurnResult {

        // El factor de riesgo/recompensa aumenta con el turno
        val progressFactor = currentTurn.toDouble() / maxTurns.coerceAtLeast(1)

        val updatedPlayers = players.map { player ->

            if (!player.active) return@map player

            var dinero = player.cash

            when (player.lastAction) {

                "AHORRAR" -> {
                    // Base 50, disminuye con el tiempo pero siempre suma al menos 10
                    val baseIngreso = 50.0
                    val ingresoLeve = (baseIngreso * (1.0 - (progressFactor * 0.8))).coerceAtLeast(10.0)
                    dinero += ingresoLeve
                }

                "INVERTIR" -> {
                    // Probabilidad de ganar baja (de 70% inicial a 40% final)
                    val probGanar = (0.7 - (progressFactor * 0.3)).coerceAtLeast(0.3)
                    
                    // Ganancia aumenta (de 100 inicial a 400 final)
                    val gananciaPotencial = 100.0 + (progressFactor * 300.0)
                    // Pérdida también aumenta (de 50 inicial a 200 final)
                    val perdidaPotencial = 50.0 + (progressFactor * 150.0)

                    val random = Math.random()
                    dinero += if (random < probGanar) gananciaPotencial else -perdidaPotencial
                }

                "GASTAR" -> {
                    // Costo aumenta (de 50 inicial a 200 final)
                    val costo = 50.0 + (progressFactor * 150.0)
                    dinero -= costo
                }
            }

            player.copy(
                cash = dinero,
                active = dinero > 0,
                lastAction = "",
                done = false
            )
        }

        return TurnResult(
            updatedPlayers = updatedPlayers,
            nextTurn = currentTurn + 1
        )
    }
}