package com.skillarena.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skillarena.core.ai.PlayerController
import com.skillarena.core.ai.SimpleBot
import com.skillarena.core.cards.Deck
import com.skillarena.core.match.GameEvent
import com.skillarena.core.match.Match
import com.skillarena.core.match.PlayError
import com.skillarena.core.math.Vec2
import com.skillarena.core.rating.Elo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Owns the running match and drives it with a fixed-timestep loop.
 * The local human is always player 0 (bottom of the screen).
 */
class GameViewModel : ViewModel() {
    var match: Match by mutableStateOf(newMatch())
        private set

    /** Incremented every rendered frame so Compose redraws the arena. */
    var frame by mutableLongStateOf(0L)
        private set

    var selectedCardId: String? by mutableStateOf(null)
    var lastError: PlayError? by mutableStateOf(null)
        private set

    var rating by mutableIntStateOf(Elo.DEFAULT_RATING)
        private set
    private val botRating = Elo.DEFAULT_RATING
    private var gamesPlayed = 0

    private var opponent: PlayerController = SimpleBot(player = 1)
    private var loop: Job? = null

    private fun newMatch() = Match(listOf(Deck.STARTER, Deck.AGGRO), seed = System.nanoTime())

    fun startMatch() {
        loop?.cancel()
        match = newMatch()
        opponent = SimpleBot(player = 1, seed = match.seed)
        selectedCardId = null
        lastError = null
        loop = viewModelScope.launch { runLoop() }
    }

    private suspend fun runLoop() {
        val tickNanos = 1_000_000_000L / match.config.ticksPerSecond
        var last = System.nanoTime()
        var accumulator = 0L
        while (viewModelScope.isActive && !match.isOver) {
            val now = System.nanoTime()
            accumulator += now - last
            last = now
            while (accumulator >= tickNanos && !match.isOver) {
                opponent.think(match)?.let(match::submit)
                match.step()
                handleEvents(match.drainEvents())
                accumulator -= tickNanos
            }
            frame++
            delay(16)
        }
        frame++
    }

    private fun handleEvents(events: List<GameEvent>) {
        for (e in events) when (e) {
            is GameEvent.CardRejected -> if (e.player == 0) lastError = e.reason
            is GameEvent.MatchEnded -> {
                val score = when (e.result.winner) {
                    0 -> 1.0
                    null -> 0.5
                    else -> 0.0
                }
                rating = Elo.update(rating, botRating, score, gamesPlayed).first
                gamesPlayed++
            }
            else -> Unit
        }
    }

    /** Called when the player taps the arena at tile coordinates [pos]. */
    fun onArenaTap(pos: Vec2) {
        val cardId = selectedCardId ?: return
        val error = match.validate(0, cardId, pos)
        lastError = error
        if (error == null) {
            match.playCard(0, cardId, pos)
            selectedCardId = null
        }
    }

    fun onCardTap(cardId: String) {
        selectedCardId = if (selectedCardId == cardId) null else cardId
        lastError = null
    }

    override fun onCleared() {
        loop?.cancel()
    }
}
