package com.example.tiorico.data.models

import com.example.tiorico.data.models.Player

data class LobbyUiState(
    val playerName: String = "",
    val roomCode: String = "",
    val players: List<Player> = emptyList(),
    val isHost: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val navigateToGame: Boolean = false,
    val playerId: String = "",
    val gameId: String = "",
    val chat: List<ChatDocument> = emptyList()
)