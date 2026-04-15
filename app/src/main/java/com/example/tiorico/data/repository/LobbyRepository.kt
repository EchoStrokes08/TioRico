package com.example.tiorico.data.repository

import com.example.tiorico.data.models.Player
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.util.UUID

class LobbyRepository {

    private val db = FirebaseDatabase.getInstance().reference

    suspend fun createGame(playerName: String): Result<String> {
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

            Result.success(roomCode)

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun joinGame(roomCode: String, playerName: String): Result<List<Player>> {
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

            Result.success(emptyList())

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
            db.child("rooms")
                .child(roomCode)
                .child("started")
                .setValue(true)
                .await()

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