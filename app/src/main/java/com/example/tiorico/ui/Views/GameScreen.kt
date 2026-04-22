package com.example.tiorico.ui.Views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tiorico.data.models.ActionDocument
import com.example.tiorico.ui.components.ChatComponent
import com.example.tiorico.ui.game.GameViewModel
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.tiorico.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign


@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onFinishGame: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    val player = state.currentPlayer
    val game = state.game

    LaunchedEffect(state.showTurnMessage) {
        if (state.showTurnMessage) {
            kotlinx.coroutines.delay(2000)
            viewModel.hideTurnMessage()
        }
    }

    LaunchedEffect(state.navigateToResult) {
        if (state.navigateToResult) {
            onFinishGame()
            viewModel.onNavigatedToResult()
        }
    }

    if (state.isLoading || player == null || game == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.Yellow)
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF010570),
                        Color(0xFF0105FF),
                        Color(0xFF010570)
                    )
                )
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            //  LOGO (igual que login)
            Image(
                painter = painterResource(id = R.drawable.logo_tiorico),
                contentDescription = "Logo",
                modifier = Modifier
                    .fillMaxWidth(0.7f)
            )
            if (state.showTurnMessage) {
                Text(
                    "Nuevo turno!",
                    color = Color.Green,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            //  CARD PRINCIPAL
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0D47A1).copy(alpha = 0.85f)
                ),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "$${player.cash}",
                        fontSize = 28.sp,
                        color = Color.Yellow
                    )
                    if (!player.active) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            " Eliminado",
                            color = Color.Red,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        "Turno ${game.actualTurn} | Meta: $${game.targetCash}",
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    state.lastEvent?.let { event ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.2f))
                                .padding(8.dp)
                        ) {
                            Text(
                                "EVENTO",
                                color = Color.Yellow,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                event.description,
                                color = Color(0xFFB2FF59),
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            when(event.impact) {
                                "BONUS" -> Text("Felicidades Ganaste", color = Color.Green)
                                "LOSS"  -> Text("Te toca pagar", color = Color.Red)
                                "GIFT"  -> Text("Es momento de incrementar tu dinero", color = Color.Yellow)
                            }
                            Text(
                                event.value.toString(),
                                color = Color(0xFFB2FF59),
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } ?: Text(
                        "Sin eventos recientes",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            val canPlay = player.active && (player.done == false)

            // BOTONES DE ACCIÓN
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {

                Button(
                    onClick = {
                        println("CLICK AHORRAR")
                        viewModel.playAction(ActionDocument(type = "AHORRAR"))
                    },
                    enabled = canPlay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canPlay) Color(0xFF00C853) else Color.Gray
                    )
                ) {
                    Text("Ahorrar ", fontSize = 16.sp)
                }

                Button(
                    onClick = {

                        viewModel.playAction(ActionDocument(type = "INVERTIR"))
                    },
                    enabled = canPlay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canPlay) Color(0xFF2962FF) else Color.Gray
                    )

                ) {
                    Text("Invertir ", fontSize = 16.sp)
                }

                Button(
                    onClick = {
                        viewModel.playAction(ActionDocument(type = "GASTAR"))
                    },
                    enabled = canPlay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canPlay) Color(0xFFD50000) else Color.Gray
                    )
                ) {
                    Text("Gastar ", fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(25.dp))

            val faltan = state.players.count { it.active && !it.done }

            if (!canPlay && player.active) {
                Text(
                    "Esperando a otros jugadores...",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
            if (player.active) {
                Text(
                    "Faltan $faltan jugadores",
                    color = Color.Yellow,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onFinishGame,
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text("Salir")
            }
        }

        ChatComponent(
            messages = state.chat,
            onSendMessage = { viewModel.sendMessage(it) }
        )
    }
}


