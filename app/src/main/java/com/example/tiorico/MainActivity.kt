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

                // 🔥 Eventos de Auth (login exitoso)
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

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    when (currentScreen) {

                        // 🔐 LOGIN
                        "login" -> {
                            LoginScreen(
                                viewModel = authViewModel,
                                onRegisterClick = {
                                    currentScreen = "register" // 🔥 ir a registro
                                },
                                onGoToLobby = {
                                    currentScreen = "lobby" // debug
                                }
                            )
                        }

                        // 📝 REGISTER
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

                        // 🟢 LOBBY
                        "lobby" -> {

                            LaunchedEffect(Unit) {
                                lobbyViewModel.loadUserName()
                            }

                            LobbyScreen(
                                viewModel = lobbyViewModel,
                                onNavigateToGame = {
                                    currentScreen = "game"
                                }
                            )
                        }

                        // 🎮 GAME
                        "game" -> {
                            Box(modifier = Modifier.padding(innerPadding)) {
                                GameScreen(
                                    viewModel = gameViewModel,
                                    onFinishGame = {
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
}

