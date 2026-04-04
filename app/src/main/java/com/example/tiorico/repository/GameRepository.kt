package com.example.tiorico.repository

import com.example.tiorico.data.GameState

interface GameRepository {
    fun getGame(): GameState
    fun updateGame(gameState: GameState)
}