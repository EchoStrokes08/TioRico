package com.example.tiorico.ui.Views

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tiorico.R

@Composable
fun ResultScreen(
    isWinner: Boolean,
    finalCash: Double,
    onBackToLobby: () -> Unit
) {
    val title = if (isWinner) "¡GANASTE!" else "¡HAS PERDIDO!"
    val subtitle = if (isWinner) "¡Felicidades, eres el nuevo Tio Rico!" else "Te has quedado en la quiebra..."
    val mainColor = if (isWinner) Color(0xFF00C853) else Color(0xFFD50000)
    val icon = if (isWinner) R.drawable.logo_tiorico else R.drawable.logo_tiorico // Podrías cambiar el icono si tuvieras uno de derrota

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF010530),
                        Color(0xFF010570)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Image(
                painter = painterResource(id = icon),
                contentDescription = "Resultado",
                modifier = Modifier.fillMaxWidth(0.6f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = title,
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = mainColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Tu capital final:",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
            Text(
                text = "$$finalCash",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Yellow
            )

            Spacer(modifier = Modifier.height(60.dp))

            Button(
                onClick = onBackToLobby,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2962FF)
                )
            ) {
                Text(
                    "VOLVER AL LOBBY",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
