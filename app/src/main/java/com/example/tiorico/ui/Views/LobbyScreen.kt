package com.example.tiorico.ui.Views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tiorico.ui.lobby.LobbyViewModel

@Composable
fun LobbyScreen(
    viewModel: LobbyViewModel,
    onNavigateToGame: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    // Navegación automática cuando el VM lo diga
    LaunchedEffect(state.navigateToGame) {
        if (state.navigateToGame) {
            onNavigateToGame()
            viewModel.onNavigatedToGame()
        }
    }

    Scaffold { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF2D8F00),
                                Color(0xFF4BFF30),
                                Color(0xFF4BFF30),
                                Color(0xFF2D8F00)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        "Sala de Juego",
                        fontSize = 36.sp,
                        color = Color.Blue,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 🔒 NOMBRE (AUTO DESDE LOGIN - SOLO LECTURA)
                    OutlinedTextField(
                        value = state.playerName,
                        onValueChange = {}, // ❌ ya no se puede cambiar
                        enabled = false,    // 🔥 bloqueado
                        placeholder = { Text("Tu nombre") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Blue,
                            unfocusedBorderColor = Color.Blue,
                            disabledBorderColor = Color.Blue,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            disabledTextColor = Color.White,
                            focusedPlaceholderColor = Color.Blue,
                            unfocusedPlaceholderColor = Color.Blue,
                            disabledPlaceholderColor = Color.Blue
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // ROOM CODE
                    OutlinedTextField(
                        value = state.roomCode,
                        onValueChange = viewModel::onRoomCodeChanged,
                        placeholder = { Text("Código de sala") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Blue,
                            unfocusedBorderColor = Color.Blue,
                            errorBorderColor = Color.Red,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedPlaceholderColor = Color.Blue,
                            unfocusedPlaceholderColor = Color.Blue,
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // JOIN
                    Button(
                        onClick = { viewModel.joinGame() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF0FF1FF),
                                            Color(0xFF0647E1)
                                        )
                                    ),
                                    shape = MaterialTheme.shapes.medium
                                )
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Unirse a Sala", color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // CREATE
                    Button(
                        onClick = { viewModel.createGame() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF0FF1FF),
                                            Color(0xFF0647E1)
                                        )
                                    ),
                                    shape = MaterialTheme.shapes.medium
                                )
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Crear Sala",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        "Jugadores:",
                        color = Color.DarkGray,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // LISTA
                    state.players.forEach {
                        Text("• ${it.name}", color = Color.DarkGray, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // HOST
                    if (state.isHost) {
                        Button(
                            onClick = { viewModel.startGame() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Iniciar Partida")
                        }
                    }

                    // LOADING
                    if (state.isLoading) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Cargando...", color = Color.White)
                    }

                    // ERROR
                    state.errorMessage?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(it, color = Color.Red)
                    }
                }
            }
        }
    }
}