package com.example.tiorico.usecase

import com.example.tiorico.data.models.Player

class EveryoneIsReady {
    fun execute(
        players: List<Player>
    ): Boolean {
        return players
            .filter { it.active }
            .all { it.done }
    }
}