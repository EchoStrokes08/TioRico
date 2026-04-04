package com.example.tiorico.data

data class Player (
    val id: String,
    val name : String,
    val cash: Int,
    val isActive: Boolean,
    val lastAction: Action?,
    val ready: Boolean
)

