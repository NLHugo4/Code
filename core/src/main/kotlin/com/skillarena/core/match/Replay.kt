package com.skillarena.core.match

import com.skillarena.core.cards.Deck

/**
 * Everything needed to reproduce a match exactly: seed, decks and the executed commands.
 * Useful for replays, anti-cheat (server re-simulates) and debugging.
 */
data class Replay(
    val seed: Long,
    val deckIds: List<List<String>>,
    val commands: List<Command>,
    val config: MatchConfig = MatchConfig(),
) {
    fun play(): Match {
        val match = Match(deckIds.map { ids -> Deck.of(*ids.toTypedArray()) }, seed, config)
        commands.forEach(match::submit)
        while (!match.isOver) match.step()
        return match
    }

    companion object {
        fun of(match: Match, decks: List<Deck>) =
            Replay(match.seed, decks.map { d -> d.cards.map { it.id } }, match.history, match.config)
    }
}
