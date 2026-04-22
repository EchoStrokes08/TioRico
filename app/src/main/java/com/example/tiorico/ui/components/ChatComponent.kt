package com.example.tiorico.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tiorico.data.models.ChatDocument

@Composable
fun ChatComponent(
    messages: List<ChatDocument>,
    onSendMessage: (String) -> Unit
) {
    var isOpen by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isOpen) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 80.dp, end = 16.dp)
                    .width(300.dp)
                    .height(400.dp),
                elevation = CardDefaults.cardElevation(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A237E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0D47A1))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Chat de Sala", color = Color.White, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { isOpen = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                        }
                    }

                    // Messages
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(8.dp),
                        reverseLayout = false
                    ) {
                        items(messages) { msg ->
                            Column(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    msg.senderName,
                                    fontSize = 10.sp,
                                    color = Color.Yellow,
                                    fontWeight = FontWeight.Bold
                                )
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        msg.message,
                                        modifier = Modifier.padding(8.dp),
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    // Input
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Escribe algo...", fontSize = 12.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            maxLines = 2
                        )
                        IconButton(onClick = {
                            if (text.isNotBlank()) {
                                onSendMessage(text)
                                text = ""
                            }
                        }) {
                            Icon(Icons.Default.Send, contentDescription = "Enviar", tint = Color.Yellow)
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { isOpen = !isOpen },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Color.Yellow,
            contentColor = Color.Black
        ) {
            Icon(Icons.Default.Chat, contentDescription = "Abrir Chat")
        }
    }
}
