package com.example.tiorico.data.repository

import com.example.tiorico.data.models.ChatDocument
import com.example.tiorico.data.models.Player
import com.example.tiorico.data.models.PlayerDocument
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import java.util.UUID


import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LobbyRepository {

    private val db = FirebaseFirestore.getInstance()

    private fun gamesCol() = db.collection("games")
    private fun gameDoc(gameId: String) = gamesCol().document(gameId)
    private fun playersCol(gameId: String) = gameDoc(gameId).collection("players")
    private fun chatCol(gameId: String) = gameDoc(gameId).collection("chat")

    suspend fun createGame(playerName: String): Result<Triple<String, String, String>>{
        return try {
            val gameRef = gamesCol().document()
            val gameId = gameRef.id
            val playerId = UUID.randomUUID().toString()

            val roomCode = generateRoomCode()

            val game = mapOf(
                "id" to gameId,
                "roomCode" to roomCode,
                "hostId" to playerId,
                "actualTurn" to 1,
                "maxTurns" to 9999,
                "status" to "ESPERANDO"
            )

            val player = mapOf(
                "id" to playerId,
                "name" to playerName,
                "cash" to 1000.0,
                "active" to true,
                "done" to false,
                "lastAction" to "",
                "isHost" to true,
                "impact" to 0.0
            )

            gameRef.set(game).await()
            playersCol(gameId).document(playerId).set(player).await()

            //DEVOLVEMOS TODO
            Result.success(Triple(gameId, roomCode, playerId))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinGame(roomCode: String, playerName: String): Result<Pair<String, String>> {
        return try {

            val gameId = findGameByRoomCode(roomCode)
                ?: return Result.failure(Exception("Sala no existe"))

            val existing = playersCol(gameId)
                .whereEqualTo("name", playerName)
                .get()
                .await()

            if (!existing.isEmpty) {
                return Result.failure(Exception("Ya estás en la sala"))
            }

            val playerId = UUID.randomUUID().toString()

            val player = mapOf(
                "id" to playerId,
                "name" to playerName,
                "cash" to 1000.0,
                "active" to true,
                "done" to false,
                "lastAction" to "",
                "isHost" to false,
                "impact" to 0.0
            )

            playersCol(gameId).document(playerId).set(player).await()

            //ahora devuelve también gameId
            Result.success(gameId to playerId)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenPlayers(gameId: String): Flow<List<Player>> = callbackFlow {
        val listener = playersCol(gameId)
            .addSnapshotListener { snap, error ->

                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val players = snap?.documents?.map { doc ->
                    Player(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        cash = doc.getDouble("cash") ?: 0.0,
                        active = doc.getBoolean("active") ?: true,
                        done = doc.getBoolean("done") ?: false,
                        lastAction = doc.getString("lastAction") ?: "",
                        isHost = doc.getBoolean("isHost") ?: false
                    )
                } ?: emptyList()

                trySend(players)
            }

        awaitClose { listener.remove() }
    }

    fun listenGameStart(gameId: String): Flow<Boolean> = callbackFlow {
        val listener = gameDoc(gameId)
            .addSnapshotListener { snap, error ->

                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val started = snap?.getString("status") == "JUGANDO"
                trySend(started)
            }

        awaitClose { listener.remove() }
    }

    suspend fun startGame(gameId: String): Result<Unit> {
        return try {
            gameDoc(gameId)
                .update("status", "JUGANDO")
                .await()

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    private suspend fun findGameByRoomCode(code: String): String? {
        val snapshot = gamesCol()
            .whereEqualTo("roomCode", code)
            .get()
            .await()

        return snapshot.documents.firstOrNull()?.id
    }
    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
    suspend fun leaveRoom(gameId: String, playerId: String): Result<Unit> {
        return try {
            playersCol(gameId).document(playerId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun promoteToHost(gameId: String, playerId: String): Result<Unit> {
        return try {
            val batch = db.batch()
            batch.update(gameDoc(gameId), "hostId", playerId)
            batch.update(playersCol(gameId).document(playerId), "isHost", true)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(gameId: String, message: ChatDocument): Result<Unit> {
        return try {
            val ref = chatCol(gameId).document()
            ref.set(message.copy(id = ref.id)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeChat(gameId: String): Flow<List<ChatDocument>> = callbackFlow {
        val listener = chatCol(gameId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limitToLast(50)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snap?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatDocument::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }
}
