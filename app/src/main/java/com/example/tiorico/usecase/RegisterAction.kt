package com.example.tiorico.usecase

import com.example.tiorico.data.ActionDocument
import com.example.tiorico.data.PlayerDocument

class RegisterAction {
    fun execute(
        player: PlayerDocument,
        action: ActionDocument
    ): PlayerDocument {
        return player.copy(
            lastAction = action.type,  // enum → String para Firestore
            done = true
        )
    }
}