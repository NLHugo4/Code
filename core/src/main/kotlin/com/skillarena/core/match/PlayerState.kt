package com.skillarena.core.match

import com.skillarena.core.cards.Card
import com.skillarena.core.cards.Deck
import kotlin.random.Random

/** Elixir and card cycle of one player. */
class PlayerState(val index: Int, deck: Deck, rng: Random, startingElixir: Float) {
    var elixir: Float = startingElixir
        internal set
    var crowns: Int = 0
        internal set

    private val _hand: MutableList<Card>
    private val queue: ArrayDeque<Card>

    init {
        val shuffled = deck.cards.shuffled(rng)
        _hand = shuffled.take(Deck.HAND_SIZE).toMutableList()
        queue = ArrayDeque(shuffled.drop(Deck.HAND_SIZE))
    }

    val hand: List<Card> get() = _hand
    val nextCard: Card get() = queue.first()

    /** Plays a card: it goes to the back of the cycle and the next card takes its slot. */
    internal fun cycle(cardId: String) {
        val idx = _hand.indexOfFirst { it.id == cardId }
        require(idx >= 0)
        val played = _hand[idx]
        _hand[idx] = queue.removeFirst()
        queue.addLast(played)
    }
}
