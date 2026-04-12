package com.example.tiorico.ui.Views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
fun GameScreen(
    onFinishGame: (Boolean) -> Unit
) {
    val money = 1200
    val turn = 5
    val maxTurns = 10

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

            Text("Dinero: $$money", color = Color.White)
            Text("Turno: $turn / $maxTurns", color = Color.White)

            Spacer(modifier = Modifier.height(16.dp))

            Text("¡Ganaste $300!", color = Color.Green)

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onFinishGame(Random.nextBoolean()) },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Ahorrar")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onFinishGame(Random.nextBoolean()) },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Invertir")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onFinishGame(Random.nextBoolean()) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Gastar")
            }
        }
    }
}