package com.example.tiorico.ui.Views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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

@Composable
fun ResultScreen(
    isWinner: Boolean,
    onRetry: () -> Unit,
    onExit: () -> Unit
) {

    val title = if (isWinner) "¡Ganaste!" else "¡Perdiste!"
    val color = if (isWinner) Color.Green else Color.Red

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF263238)),
        contentAlignment = Alignment.Center
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Text(title, fontSize = 32.sp, color = color)

            Spacer(modifier = Modifier.height(16.dp))

            Text("Dinero final: $0", color = Color.White)

            Spacer(modifier = Modifier.height(24.dp))

            Row {

                Button(onClick = onRetry) {
                    Text("Reintentar")
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = onExit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("Salir")
                }
            }
        }
    }
}