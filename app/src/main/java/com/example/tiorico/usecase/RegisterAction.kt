package com.example.tiorico.usecase

import com.example.tiorico.data.models.Player
import com.example.tiorico.data.models.ActionDocument

class RegisterAction {
    fun execute(
        player: Player,
        action: ActionDocument
    ): Player {
        return player.copy(
            lastAction = action.type,
            done = true
        )
    }
}