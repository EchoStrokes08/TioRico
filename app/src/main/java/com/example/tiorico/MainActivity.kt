package com.example.tiorico

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.tiorico.data.models.AuthUiState
import com.example.tiorico.ui.Views.*
import com.example.tiorico.ui.auth.AuthViewModel
import com.example.tiorico.ui.auth.RegisterViewModel
import com.example.tiorico.ui.lobby.LobbyViewModel
import com.example.tiorico.ui.game.GameViewModel
import com.example.tiorico.ui.theme.TioRicoTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val lobbyViewModel: LobbyViewModel by viewModels()
    private val gameViewModel: GameViewModel by viewModels()
    private val registerViewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {

            TioRicoTheme {

                var currentScreen by remember { mutableStateOf("login") }
                var isWinner by remember { mutableStateOf(false) }
                var finalCash by remember { mutableStateOf(0.0) }

                //IDs DE PARTIDA
                var gameId by remember { mutableStateOf("") }
                var playerId by remember { mutableStateOf("") }

                //Eventos de Auth
                LaunchedEffect(Unit) {
                    authViewModel.eventFlow.collectLatest { event ->
                        when (event) {
                            is AuthViewModel.UiEvent.NavigateToHome -> {
                                currentScreen = "lobby"
                            }
                            else -> {}
                        }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    when (currentScreen) {

                        "login" -> {
                            LoginScreen(
                                viewModel = authViewModel,
                                onRegisterClick = {
                                    currentScreen = "register"
                                },
                                onGoToLobby = {
                                    val user = authViewModel.uiState.value
                                    if (user is AuthUiState.Success) {
                                        currentScreen = "lobby"
                                    }
                                }
                            )
                        }

                        //--------------------------------------------------------------------------------------------
                        "register" -> {
                            RegisterScreen(
                                viewModel = registerViewModel,
                                onBackClick = {
                                    currentScreen = "login"
                                },
                                onRegisterSuccess = {
                                    currentScreen = "lobby"
                                }
                            )
                        }

                        //-------------------------------------------------------------------------------------------
                        "lobby" -> {

                            LaunchedEffect(Unit) {
                                lobbyViewModel.resetState()
                                lobbyViewModel.loadUserName()
                            }

                            LobbyScreen(
                                viewModel = lobbyViewModel,
                                onNavigateToGame = { gameIdValue, playerIdValue ->

                                    gameId = gameIdValue   // ahora sí es el ID real
                                    playerId = playerIdValue

                                    currentScreen = "game"
                                },
                                onExit = {
                                    currentScreen = "login" // AQUÍ ESTÁ LA CLAVE
                                }
                            )
                        }

                        // -------------------------------------------------------------------------------------------
                        "game" -> {

                            //IMPORTANTE: inicializar solo cuando cambia el room
                            LaunchedEffect(gameId, playerId) {
                                if (gameId.isBlank() || playerId.isBlank()) return@LaunchedEffect

                                gameViewModel.initialize(gameId, playerId)
                            }

                            Box(modifier = Modifier.padding(innerPadding)) {
                                GameScreen(
                                    viewModel = gameViewModel,
                                    onFinishGame = {
                                        val state = gameViewModel.uiState.value
                                        finalCash = state.currentPlayer?.cash ?: 0.0
                                        isWinner = state.currentPlayer?.active == true
                                        
                                        gameViewModel.leaveGame {
                                            currentScreen = "result"
                                        }
                                    }
                                )
                            }
                        }

                        "result" -> {
                            ResultScreen(
                                isWinner = isWinner,
                                finalCash = finalCash,
                                onBackToLobby = {
                                    gameViewModel.clearState()
                                    currentScreen = "lobby"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
