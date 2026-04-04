package com.example.tiorico.data

data class GameState (
    val players: List<Player>,
    val currentTurn: Int,
    val maxTurns: Int,
    val status: GameStatus
)
