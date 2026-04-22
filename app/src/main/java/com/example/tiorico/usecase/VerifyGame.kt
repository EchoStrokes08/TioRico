package com.example.tiorico.usecase

import com.example.tiorico.data.models.GameDocument
import com.example.tiorico.data.models.Player

class VerifyGame {

    fun execute(game: GameDocument, players: List<Player>): GameDocument {

        val actives = players.count { it.active }
        val winner = players.firstOrNull { it.cash >= game.targetCash }
        
        // El juego termina si se alcanza el máximo de turnos
        val turnsExceeded = game.maxTurns > 0 && game.actualTurn > game.maxTurns

        return when {
            winner != null -> game.copy(status = "FINALIZADO")
            actives <= 1   -> game.copy(status = "FINALIZADO")
            turnsExceeded  -> game.copy(status = "FINALIZADO")
            else           -> game
        }
    }
}