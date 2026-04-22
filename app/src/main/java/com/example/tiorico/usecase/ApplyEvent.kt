package com.example.tiorico.usecase

import com.example.tiorico.data.models.Player
import com.example.tiorico.data.models.EventDocument

class ApplyEvent {

    private val eventsPool = listOf(
        EventDocument(description = "Feliz año nuevo, tu jefe te da un bodo de", impact = "BONUS", value = 100.0),
        EventDocument(description = "Ve mas lento mientras conduces, tu multa es", impact = "LOSS", value = 70.0),
        EventDocument(description = "No siempre se gana en el mundo de la bolsa", impact = "LOSS", value = 50.0),
        EventDocument(description = "Feliz navidad la abuela te regala", impact = "GIFT", value = listOf<Double>(10.0, 40.0, 100.0).random()),
        EventDocument(description = "El banco te gira tus intereses", impact = "BONUS", value = 80.0),
        EventDocument(description = "Ten cuidado con el ladron ahora", impact = "LOSS", value = 90.0),
        EventDocument(description = "Toma el 2 lugar en la competencia de belleza", impact = "BONUS", value = 120.0),
        EventDocument(description = "Por no pagar tus impuestos el banco te cobra", impact = "LOSS", value = 60.0)
    )

    fun execute(
        players: List<Player>,
        currentTurn: Int,
        maxTurns: Int
    ): Pair<List<Player>, List<EventDocument>> {

        // La probabilidad de evento aumenta con los turnos (ej: de 20% a 80%)
        val progressFactor = currentTurn.toDouble() / maxTurns.coerceAtLeast(1)
        val eventChance = 0.2 + (progressFactor * 0.4) // Un poco menos frecuente para que sea especial

        val appliedEvents = mutableListOf<EventDocument>()

        val updatedPlayers = players.map { player ->
            if (!player.active) return@map player

            // Check de probabilidad INDIVIDUAL por jugador
            val chance = Math.random()
            if (chance > eventChance) return@map player

            // Seleccionamos un evento aleatorio
            val eventTemplate = eventsPool.random()
            
            // Variamos un poco el valor para que no sea siempre el mismo
            val variability = 0.8 + (Math.random() * 0.4) // entre 80% y 120% del valor base
            val finalValue = (eventTemplate.value * variability).toInt().toDouble()

            val event = eventTemplate.copy(
                playerId = player.id,
                value = finalValue
            )

            val newCash = when (event.impact) {
                "BONUS" -> player.cash + event.value
                "LOSS"  -> player.cash - event.value
                "GIFT"  -> player.cash + event.value
                else    -> player.cash
            }

            appliedEvents.add(event)

            player.copy(
                cash = newCash,
                active = newCash > 0
            )
        }

        return Pair(updatedPlayers, appliedEvents)
    }
}