package com.example.tiorico.data.repository

import com.example.tiorico.data.models.Player
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class LobbyRepository {

    private val db = FirebaseDatabase.getInstance().reference

    suspend fun createGame(playerName: String): Result<Pair<String, String>> {
        return try {
            val roomCode = UUID.randomUUID().toString().take(6)
            val playerId = UUID.randomUUID().toString()

            val player = mapOf(
                "id" to playerId,
                "name" to playerName
            )

            val room = mapOf(
                "players" to mapOf(playerId to player),
                "host" to playerId,
                "started" to false
            )

            db.child("rooms").child(roomCode).setValue(room).await()

            Result.success(roomCode to playerId)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinGame(roomCode: String, playerName: String): Result<String> {
        return try {
            val playerId = UUID.randomUUID().toString()

            val player = mapOf(
                "id" to playerId,
                "name" to playerName
            )

            db.child("rooms")
                .child(roomCode)
                .child("players")
                .child(playerId)
                .setValue(player)
                .await()

            Result.success(playerId)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenPlayers(roomCode: String, onUpdate: (List<Player>) -> Unit) {
        db.child("rooms")
            .child(roomCode)
            .child("players")
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {

                    val players = mutableListOf<Player>()

                    snapshot.children.forEach {
                        val player = it.getValue(Player::class.java)
                        if (player != null) players.add(player)
                    }

                    onUpdate(players)
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            })
    }

    suspend fun startGame(roomCode: String): Result<Unit> {
        return try {

            val firestore = FirebaseFirestore.getInstance()

            // 1. marcar lobby iniciado
            db.child("rooms")
                .child(roomCode)
                .child("started")
                .setValue(true)
                .await()

            // 2. crear game
            val gameRef = firestore.collection("games").document(roomCode)

            val game = mapOf(
                "id" to roomCode,
                "actualTurn" to 1,
                "maxTurns" to 10,
                "status" to "JUGANDO"
            )

            gameRef.set(game).await()

            // 3. COPIAR PLAYERS DE RTDB → FIRESTORE (🔥 FALTANTE CRÍTICO)
            val snapshot = db.child("rooms")
                .child(roomCode)
                .child("players")
                .get()
                .await()

            snapshot.children.forEach { child ->

                val playerId = child.child("id").getValue(String::class.java) ?: return@forEach
                val name = child.child("name").getValue(String::class.java) ?: ""

                gameRef.collection("players").document(playerId).set(
                    mapOf(
                        "id" to playerId,
                        "name" to name,
                        "cash" to 100.0,
                        "active" to true,
                        "done" to false,
                        "lastAction" to ""
                    )
                )
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenGameStart(roomCode: String, onStart: () -> Unit) {
        db.child("rooms")
            .child(roomCode)
            .child("started")
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val started = snapshot.getValue(Boolean::class.java) ?: false
                    if (started) onStart()
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            })
    }
}