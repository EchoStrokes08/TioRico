package com.example.tiorico.data.models


data class GameUiState(
    val game: GameDocument?              = null,
    val players: List<Player> = emptyList(),
    val currentPlayer: Player? = null,
    val currentTurnId: String            = "",
    val chat: List<ChatDocument>         = emptyList(),
    val lastEvent: EventDocument?        = null,
    val isLoading: Boolean               = true,
    val errorMessage: String?            = null,
    val navigateToResult: Boolean        = false,
    val showTurnMessage: Boolean         = false
)