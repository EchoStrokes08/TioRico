package com.example.tiorico.ui.Views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tiorico.ui.lobby.LobbyViewModel
import com.google.firebase.auth.FirebaseAuth
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    viewModel: LobbyViewModel,
    onNavigateToGame: (String, String) -> Unit,
    onExit: () -> Unit // 🔥 nuevo callback para volver a login
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.navigateToGame) {
        if (state.navigateToGame && state.playerId.isNotBlank()) {
            onNavigateToGame(state.roomCode, state.playerId)
            viewModel.onNavigatedToGame()
        }
    }

    Scaffold { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2D8F00),
                            Color(0xFF4BFF30),
                            Color(0xFF2D8F00)
                        )
                    )
                )
        ) {

            //BOTÓN CERRAR SESIÓN (ARRIBA DERECHA)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        FirebaseAuth.getInstance().signOut()
                        onExit()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Cerrar sesión",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Cerrar sesión",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }

            // CONTENIDO PRINCIPAL
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1B5E20).copy(alpha = 0.85f)
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        // Nombre + Host
                        Text(
                            text = buildString {
                                append(state.playerName.ifBlank { "Cargando..." })
                                if (state.isHost) append(" (HOST)")
                            },
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Código de sala
                        OutlinedTextField(
                            value = state.roomCode,
                            onValueChange = viewModel::onRoomCodeChanged,
                            placeholder = { Text("Código de sala") },
                            enabled = !state.isHost,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {

                            Button(
                                onClick = { viewModel.joinGame() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Unirse")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { viewModel.createGame() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Crear")
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            "Jugadores",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        state.players.forEach { player ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF388E3C)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "• ${player.name}",
                                        color = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (player.id == state.playerId && state.isHost) {
                                        Text("HOST", color = Color.Yellow)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        if (state.isHost) {
                            Button(
                                onClick = { viewModel.startGame() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF6D00)
                                )
                            ) {
                                Text("Iniciar Partida")
                            }
                        }

                        if (state.isLoading) {
                            Spacer(modifier = Modifier.height(12.dp))
                            CircularProgressIndicator(color = Color.White)
                        }

                        state.errorMessage?.let {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(it, color = Color.Red)
                        }
                    }
                }
            }
        }
    }
}