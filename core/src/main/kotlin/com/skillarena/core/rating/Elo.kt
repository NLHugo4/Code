package com.skillarena.core.rating

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Chess-style Elo rating. Your rating only reflects wins and losses against
 * other players — there is nothing to grind or buy.
 */
object Elo {
    const val DEFAULT_RATING = 1200

    /** Expected score (0..1) of a player rated [ra] against [rb]. */
    fun expectedScore(ra: Int, rb: Int): Double = 1.0 / (1.0 + 10.0.pow((rb - ra) / 400.0))

    /**
     * New ratings after a game.
     * @param scoreA 1.0 = A wins, 0.5 = draw, 0.0 = A loses.
     * @param gamesPlayedA used to pick a higher K-factor for new players (as FIDE does).
     */
    fun update(ra: Int, rb: Int, scoreA: Double, gamesPlayedA: Int = 30, gamesPlayedB: Int = 30): Pair<Int, Int> {
        val ea = expectedScore(ra, rb)
        val newA = ra + kFactor(gamesPlayedA) * (scoreA - ea)
        val newB = rb + kFactor(gamesPlayedB) * ((1.0 - scoreA) - (1.0 - ea))
        return newA.roundToInt() to newB.roundToInt()
    }

    fun kFactor(gamesPlayed: Int): Int = if (gamesPlayed < 30) 40 else 20
}

/** Pairs players with the closest rating, widening the window the longer someone waits. */
class Matchmaker(private val baseWindow: Int = 100, private val widenPerSecond: Int = 10) {
    data class Ticket(val playerId: String, val rating: Int, val queuedAtSeconds: Long)

    private val queue = mutableListOf<Ticket>()

    fun enqueue(ticket: Ticket) {
        queue += ticket
    }

    fun cancel(playerId: String) {
        queue.removeAll { it.playerId == playerId }
    }

    /** Returns all pairs that can be matched now and removes them from the queue. */
    fun findMatches(nowSeconds: Long): List<Pair<Ticket, Ticket>> {
        val result = mutableListOf<Pair<Ticket, Ticket>>()
        val waiting = queue.sortedBy { it.queuedAtSeconds }.toMutableList()
        while (waiting.isNotEmpty()) {
            val a = waiting.removeAt(0)
            val window = baseWindow + (nowSeconds - a.queuedAtSeconds).toInt() * widenPerSecond
            val b = waiting.filter { kotlin.math.abs(it.rating - a.rating) <= window }
                .minByOrNull { kotlin.math.abs(it.rating - a.rating) } ?: continue
            waiting.remove(b)
            result += a to b
        }
        val matched = result.flatMap { listOf(it.first, it.second) }.toSet()
        queue.removeAll { it in matched }
        return result
    }
}
