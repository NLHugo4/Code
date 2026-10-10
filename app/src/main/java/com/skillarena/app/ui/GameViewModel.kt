package com.skillarena.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.skillarena.app.ui.render.EffectSystem
import com.skillarena.app.ui.render.Palette
import com.skillarena.core.ai.PlayerController
import com.skillarena.core.ai.SimpleBot
import com.skillarena.core.cards.CardCatalog
import com.skillarena.core.cards.Deck
import com.skillarena.core.cards.SpellCard
import com.skillarena.core.match.GameEvent
import com.skillarena.core.match.Match
import com.skillarena.core.match.PlayError
import com.skillarena.core.math.Vec2
import com.skillarena.core.rating.Elo
import kotlin.math.min

/**
 * Owns the running match. The screen calls [onFrame] once per display frame;
 * the simulation advances in fixed ticks and the renderer interpolates in between.
 * The local human is always player 0 (bottom of the screen).
 */
class GameViewModel : ViewModel() {
    var match: Match by mutableStateOf(newMatch())
        private set

    /** Incremented every frame so Compose redraws. */
    var frame by mutableLongStateOf(0L)
        private set

    /** 0..1 between the previous and the current tick. */
    var renderAlpha by mutableFloatStateOf(0f)
        private set

    /** Seconds since the match started (real time, for animations). */
    var now by mutableFloatStateOf(0f)
        private set

    val effects = EffectSystem()

    var selectedCardId: String? by mutableStateOf(null)
    var lastError: PlayError? by mutableStateOf(null)
        private set

    var rating by mutableIntStateOf(Elo.DEFAULT_RATING)
        private set
    private val botRating = Elo.DEFAULT_RATING
    private var gamesPlayed = 0

    private var opponent: PlayerController = SimpleBot(player = 1)
    private var startNanos = 0L
    private var lastNanos = 0L
    private var accumulator = 0L

    private fun newMatch() = Match(listOf(Deck.STARTER, Deck.AGGRO), seed = System.nanoTime())

    fun startMatch() {
        match = newMatch()
        opponent = SimpleBot(player = 1, seed = match.seed)
        effects.clear()
        selectedCardId = null
        lastError = null
        startNanos = 0L
        accumulator = 0L
    }

    fun onFrame(frameNanos: Long) {
        if (startNanos == 0L) {
            startNanos = frameNanos
            lastNanos = frameNanos
        }
        // Clamp so a paused app doesn't fast-forward the match when it comes back.
        accumulator += min(frameNanos - lastNanos, 250_000_000L)
        lastNanos = frameNanos
        now = (frameNanos - startNanos) / 1e9f

        val tickNanos = 1_000_000_000L / match.config.ticksPerSecond
        while (accumulator >= tickNanos) {
            accumulator -= tickNanos
            if (match.isOver) continue
            opponent.think(match)?.let(match::submit)
            match.step()
            handleEvents(match.drainEvents())
        }
        renderAlpha = if (match.isOver) 1f else accumulator.toFloat() / tickNanos
        effects.update(now)
        frame++
    }

    private fun handleEvents(events: List<GameEvent>) {
        for (e in events) {
            effects.onEvent(e, now)
            when (e) {
                is GameEvent.CardPlayed ->
                    if (CardCatalog.byId(e.cardId) !is SpellCard) effects.onSpawn(e.pos, Palette.team(e.player), now)
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
}
