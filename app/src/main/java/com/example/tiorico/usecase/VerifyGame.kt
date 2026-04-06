package com.example.tiorico.usecase

import com.example.tiorico.data.GameDocument
import com.example.tiorico.data.PlayerDocument

class VerifyGame {

    fun execute(gameDocument: GameDocument, players: List<PlayerDocument>): GameDocument {

        val actives = players.count { it.active }

        val finished = gameDocument.actualTurn >= gameDocument.maxTurns || actives == 0

        return if (finished) {
            gameDocument.copy(status = "FINALIZADO")
        } else gameDocument
    }
}