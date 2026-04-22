package com.example.tiorico.data.models

data class Player(
    val id: String = "",
    val name: String = "",
    val cash: Double = 0.0,
    val isHost: Boolean = false,
    val active: Boolean = true,
    val done: Boolean = false,
    val lastAction: String = "",
    val impact: Double = 0.0
)