package com.example.tiorico.data.models

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.Timestamp;

// SALA -> games/{gameId}

data class GameDocument (
    @DocumentId val id: String = "",
    val actualTurn: Int = 0,
    val maxTurns: Int = 10,
    val status: String = ""
){
    constructor(): this(id = "")
}

//games/{gameId}/players/{playerId}       ← PlayerDocument  (name, cash, active, done, lastAction)

data class PlayerDocument (
    @DocumentId val id: String = "",
    val name: String = "",
    val cash: Double = 0.0,
    val active: Boolean = false,
    val done: Boolean = false,
    val lastAction: String = ""
){
    constructor(): this(id = "")
}

//games/{gameId}/turns/{turnId}           ← TurnDocument

data class TurnDocument (
    @DocumentId val id: String = "",
    val turnNumber: Int = 0,
    val hasEvent: Boolean = false
){
    constructor(): this(id = "")
}

//games/{gameId}/turns/{turnId}/actions/  ← ActionDocument

data class ActionDocument (
    @DocumentId val id: String = "",
    val playerId: String = "",
    val playerName: String = "",
    val type: String = "",
    val cashBefore: Double = 0.0,
    val cashAfter: Double = 0.0
){
    constructor(): this(id = "")
}

//games/{gameId}/turns/{turnId}/events/   ← EventDocument

data class EventDocument (
    @DocumentId val id: String = "",
    val description: String = "",
    val impact: String = ""
){
    constructor(): this(id = "")
}

//games/{gameId}/chat/{messageId}         ← ChatDocument

data class ChatDocument (
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Timestamp = Timestamp.now()
){
    constructor(): this(id = "")
}

