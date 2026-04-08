package com.example.tiorico.usecase

import com.example.tiorico.data.models.PlayerDocument

class EveryoneIsReady {
    fun execute(
        players: List<PlayerDocument>
    ): Boolean {
        return players
            .filter { it.active }
            .all { it.done }
    }
}