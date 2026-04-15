package com.example.tiorico.ui.Views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tiorico.data.models.ActionDocument
import com.example.tiorico.ui.game.GameViewModel

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onFinishGame: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    val player = state.currentPlayer
    val game = state.game

    // 🚀 navegación a resultados
    LaunchedEffect(state.navigateToResult) {
        if (state.navigateToResult) {
            onFinishGame()
            viewModel.onNavigatedToResult()
        }
    }

    // ⛔ loading inicial
    if (state.isLoading || player == null || game == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("Cargando partida...", color = Color.White)
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D47A1)),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text("TÍO RICO", fontSize = 32.sp, color = Color.Yellow)

            Spacer(modifier = Modifier.height(16.dp))

            // 💰 DINERO REAL DEL PLAYER
            Text("Dinero: $${player.cash}", color = Color.White)

            // 🔁 TURNO REAL DEL JUEGO
            Text("Turno: ${game.actualTurn} / ${game.maxTurns}", color = Color.White)

            Spacer(modifier = Modifier.height(16.dp))

            // 🎲 ÚLTIMO EVENTO
            Text(
                state.lastEvent?.description ?: "Esperando evento...",
                color = Color.Green
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 🚫 DESACTIVAR SI YA JUGÓ
            val canPlay = player.active && player.lastAction.isNullOrBlank()

            Button(
                onClick = {
                    viewModel.playAction(
                        ActionDocument(type = "AHORRAR")
                    )
                },
                enabled = canPlay,
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Ahorrar")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.playAction(
                        ActionDocument(type = "INVERTIR")
                    )
                },
                enabled = canPlay,
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Invertir")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.playAction(
                        ActionDocument(type = "GASTAR")
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                enabled = canPlay,
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Gastar")
            }

            // ⏳ feedback cuando ya jugó
            if (!canPlay) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Esperando a otros jugadores...", color = Color.White)
            }
        }
    }
}