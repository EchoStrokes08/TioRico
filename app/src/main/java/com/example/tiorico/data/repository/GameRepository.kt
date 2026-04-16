package com.example.tiorico.data.repository

import com.example.tiorico.data.models.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class GameRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    // ── Rutas (para no repetir strings por todo el código) ───────────────────

    private fun gamesCol()                          = db.collection("games")
    private fun gameDoc(gameId: String)             = gamesCol().document(gameId)
    private fun playersCol(gameId: String)          = gameDoc(gameId).collection("players")
    private fun playerDoc(gameId: String, uid: String) = playersCol(gameId).document(uid)
    private fun turnsCol(gameId: String)            = gameDoc(gameId).collection("turns")
    private fun turnDoc(gameId: String, turnId: String) = turnsCol(gameId).document(turnId)
    private fun actionsCol(gameId: String, turnId: String) = turnDoc(gameId, turnId).collection("actions")
    private fun eventsCol(gameId: String, turnId: String)  = turnDoc(gameId, turnId).collection("events")
    private fun chatCol(gameId: String)             = gameDoc(gameId).collection("chat")


    // ════════════════════════════════════════════════════════════════════════
    // SECCIÓN 1 — PARTIDA (GameDocument)
    // ════════════════════════════════════════════════════════════════════════

    suspend fun createGame(maxTurns: Int): DataResult<String> = safeCall {
        val ref = gamesCol().document()
        val game = GameDocument(
            id = ref.id,
            maxTurns = maxTurns,
            status = "ESPERANDO"
        )
        ref.set(game).await()
        ref.id
    }

    fun observeGame(gameId: String): Flow<GameDocument?> = callbackFlow {
        val listener = gameDoc(gameId).addSnapshotListener { snap, error ->
            if (error != null) {
                close(error); return@addSnapshotListener
            }

            val game = snap
                ?.toObject(GameDocument::class.java)
                ?.copy(id = snap.id) // 🔥 FIX

            trySend(game)
        }
        awaitClose { listener.remove() }
    }

    suspend fun updateGameStatus(gameId: String, status: String): DataResult<Unit> = safeCall {
        gameDoc(gameId).update("status", status).await()
    }

    suspend fun advanceTurn(gameId: String, nextTurn: Int): DataResult<Unit> = safeCall {
        gameDoc(gameId).update("actualTurn", nextTurn).await()
    }


    // ════════════════════════════════════════════════════════════════════════
    // SECCIÓN 2 — JUGADORES (PlayerDocument)
    // ════════════════════════════════════════════════════════════════════════

    suspend fun joinGame(gameId: String, player: PlayerDocument): DataResult<Unit> = safeCall {
        playerDoc(gameId, player.id).set(player).await()
    }

    fun observePlayers(gameId: String): Flow<List<PlayerDocument>> = callbackFlow {
        val listener = playersCol(gameId).addSnapshotListener { snap, error ->
            if (error != null) {
                close(error); return@addSnapshotListener
            }

            val players = snap?.documents?.mapNotNull { doc ->
                doc.toObject(PlayerDocument::class.java)
                    ?.copy(id = doc.id) // 🔥 FIX
            } ?: emptyList()

            trySend(players)
        }
        awaitClose { listener.remove() }
    }

    suspend fun updatePlayer(
        gameId: String,
        playerId: String,
        newCash: Double,
        lastAction: String,
        active: Boolean
    ): DataResult<Unit> = safeCall {
        playerDoc(gameId, playerId).update(
            mapOf(
                "cash" to newCash,
                "lastAction" to lastAction,
                "active" to active,
                "done" to true
            )
        ).await()
    }

    suspend fun resetDoneFlags(gameId: String): DataResult<Unit> = safeCall {
        val players = playersCol(gameId)
            .whereEqualTo("active", true)
            .get().await()

        val batch = db.batch()
        for (doc in players.documents) {
            batch.update(doc.reference, "done", false)
        }
        batch.commit().await()
    }


    // ════════════════════════════════════════════════════════════════════════
    // SECCIÓN 3 — TURNOS y ACCIONES
    // ════════════════════════════════════════════════════════════════════════

    suspend fun createTurn(gameId: String, number: Int): DataResult<String> = safeCall {
        val ref = turnsCol(gameId).document()
        val turn = TurnDocument(id = ref.id, turnNumber = number, hasEvent = false)
        ref.set(turn).await()
        ref.id
    }

    suspend fun saveAction(
        gameId: String,
        turnId: String,
        action: ActionDocument
    ): DataResult<Unit> = safeCall {
        val ref = actionsCol(gameId, turnId).document()
        ref.set(action.copy(id = ref.id)).await()
    }

    suspend fun saveEvent(
        gameId: String,
        turnId: String,
        event: EventDocument
    ): DataResult<Unit> = safeCall {
        val ref = eventsCol(gameId, turnId).document()
        ref.set(event.copy(id = ref.id)).await()
    }

    suspend fun getActions(gameId: String, turnId: String): DataResult<List<ActionDocument>> =
        safeCall {
            val snap = actionsCol(gameId, turnId).get().await()

            snap.documents.mapNotNull { doc ->
                doc.toObject(ActionDocument::class.java)
                    ?.copy(id = doc.id) // 🔥 FIX
            }
        }


    // ════════════════════════════════════════════════════════════════════════
    // SECCIÓN 4 — CHAT
    // ════════════════════════════════════════════════════════════════════════

    suspend fun sendMessage(gameId: String, message: ChatDocument): DataResult<Unit> = safeCall {
        val ref = chatCol(gameId).document()
        ref.set(message.copy(id = ref.id)).await()
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
                    doc.toObject(ChatDocument::class.java)
                        ?.copy(id = doc.id)
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }


    // Encuentra una sala por su id:
    suspend fun findGameByCode(gameId: String): DataResult<GameDocument?> = safeCall {
        val snap = gameDoc(gameId).get().await()

        snap.toObject(GameDocument::class.java)
            ?.copy(id = snap.id) // 🔥 FIX
    }
}