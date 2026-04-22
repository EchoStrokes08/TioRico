package com.example.tiorico.data.models

import com.google.firebase.Timestamp

data class GameDocument(
    val id: String = "",
    val actualTurn: Int = 1,
    val maxTurns: Int = 0,
    val targetCash: Double = 5000.0,
    val status: String = ""
){
    constructor(): this("")
}

data class PlayerDocument(
    val id: String = "",
    val name: String = "",
    val cash: Double = 0.0,
    val active: Boolean = false,
    val done: Boolean = false,
    val lastAction: String = "",
    val isHost: Boolean = false
)

data class TurnDocument(
    val id: String = "",
    val turnNumber: Int = 0,
    val hasEvent: Boolean = false,
    val status: String = "WAITING"
){
    constructor(): this("")
}

data class ActionDocument(
    val id: String = "",
    val playerId: String = "",
    val playerName: String = "",
    val type: String = "",
    val cashBefore: Double = 0.0,
    val cashAfter: Double = 0.0
){
    constructor(): this("")
}

data class EventDocument(
    val id: String = "",
    val playerId: String = "",
    val description: String = "",
    val impact: String = "",
    val value: Double = 0.0
)

data class ChatDocument(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Timestamp = Timestamp.now()
){
    constructor(): this("")
}