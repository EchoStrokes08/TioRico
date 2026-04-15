package com.example.tiorico.data.models

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val userId: String, val username: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}