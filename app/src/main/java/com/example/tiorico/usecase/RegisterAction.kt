package com.example.tiorico.usecase

import com.example.tiorico.data.models.ActionDocument
import com.example.tiorico.data.models.PlayerDocument

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