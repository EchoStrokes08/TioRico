package com.example.tiorico.data.repository

import com.example.tiorico.data.models.ActionDocument
import com.example.tiorico.data.models.ChatDocument
import com.example.tiorico.data.models.EventDocument
import com.example.tiorico.data.models.GameDocument
import com.example.tiorico.data.models.PlayerDocument
import com.example.tiorico.data.models.TurnDocument
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

    // Crea una partida nueva y retorna su id
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

    // Observa la partida en tiempo real
    // Cada vez que cambia actualTurn o status, el Flow emite el nuevo GameDocument
    fun observeGame(gameId: String): Flow<GameDocument?> = callbackFlow {
        val listener = gameDoc(gameId).addSnapshotListener { snap, error ->
            if (error != null) {
                close(error); return@addSnapshotListener
            }
            trySend(snap?.toObject(GameDocument::class.java))
        }
        awaitClose { listener.remove() }
    }

    // Cambia el estado de la partida (ESPERANDO → JUGANDO → FINALIZADO)
    suspend fun updateGameStatus(gameId: String, status: String): DataResult<Unit> = safeCall {
        gameDoc(gameId).update("status", status).await()
    }

    // Avanza al siguiente turno
    suspend fun advanceTurn(gameId: String, nextTurn: Int): DataResult<Unit> = safeCall {
        gameDoc(gameId).update("actualTurn", nextTurn).await()
    }


    // ════════════════════════════════════════════════════════════════════════
    // SECCIÓN 2 — JUGADORES (PlayerDocument)
    // ════════════════════════════════════════════════════════════════════════

    // Agrega un jugador a la partida (cuando entra a la sala)
    suspend fun joinGame(gameId: String, player: PlayerDocument): DataResult<Unit> = safeCall {
        playerDoc(gameId, player.id).set(player).await()
    }

    // Observa TODOS los jugadores en tiempo real
    // Cuando cualquier jugador cambia su cash, done o active → el Flow emite la lista nueva
    fun observePlayers(gameId: String): Flow<List<PlayerDocument>> = callbackFlow {
        val listener = playersCol(gameId).addSnapshotListener { snap, error ->
            if (error != null) {
                close(error); return@addSnapshotListener
            }
            val players = snap?.toObjects(PlayerDocument::class.java) ?: emptyList()
            trySend(players)
        }
        awaitClose { listener.remove() }
    }

    // Actualiza el estado del jugador después de ejecutar su acción
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
                "done" to true         // marcamos que ya jugó este turno
            )
        ).await()
    }

    // Resetea el campo "done" de todos los jugadores activos al iniciar turno nuevo
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

    // Crea el documento del turno actual
    suspend fun createTurn(gameId: String, number: Int): DataResult<String> = safeCall {
        val ref = turnsCol(gameId).document()
        val turn = TurnDocument(id = ref.id, turnNumber = number, hasEvent = false)
        ref.set(turn).await()
        ref.id
    }

    // Registra la acción de un jugador en el turno actual
    suspend fun saveAction(
        gameId: String,
        turnId: String,
        action: ActionDocument
    ): DataResult<Unit> = safeCall {
        val ref = actionsCol(gameId, turnId).document()
        ref.set(action.copy(id = ref.id)).await()
    }

    // Registra un evento aleatorio en el turno
    suspend fun saveEvent(
        gameId: String,
        turnId: String,
        event: EventDocument
    ): DataResult<Unit> = safeCall {
        val ref = eventsCol(gameId, turnId).document()
        ref.set(event.copy(id = ref.id)).await()
    }

    // Obtiene las acciones del turno (para mostrar resumen al final del turno)
    suspend fun getActions(gameId: String, turnId: String): DataResult<List<ActionDocument>> =
        safeCall {
            val snap = actionsCol(gameId, turnId).get().await()
            snap.toObjects(ActionDocument::class.java)
        }


    // ════════════════════════════════════════════════════════════════════════
    // SECCIÓN 4 — CHAT
    // ════════════════════════════════════════════════════════════════════════

    // Envía un mensaje al chat
    suspend fun sendMessage(gameId: String, message: ChatDocument): DataResult<Unit> = safeCall {
        val ref = chatCol(gameId).document()
        ref.set(message.copy(id = ref.id)).await()
    }

    // Observa el chat en tiempo real (últimos 50 mensajes, ordenados por timestamp)
    fun observeChat(gameId: String): Flow<List<ChatDocument>> = callbackFlow {
        val listener = chatCol(gameId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limitToLast(50)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error); return@addSnapshotListener
                }
                val messages = snap?.toObjects(ChatDocument::class.java) ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }


    //Encuentra una sala por su id:
    suspend fun findGameByCode(gameId: String): DataResult<GameDocument?> = safeCall {
        val snap = gameDoc(gameId).get().await()
        snap.toObject(GameDocument::class.java)
    }
}