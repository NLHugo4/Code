package com.skillarena.core.cards

/** Exactly 8 distinct cards. */
class Deck(val cards: List<Card>) {
    init {
        require(cards.size == SIZE) { "A deck needs exactly $SIZE cards (got ${cards.size})" }
        require(cards.map { it.id }.toSet().size == SIZE) { "A deck may not contain duplicate cards" }
    }

    val averageElixir: Float get() = cards.sumOf { it.cost }.toFloat() / SIZE

    companion object {
        const val SIZE = 8
        const val HAND_SIZE = 4

        fun of(vararg ids: String) = Deck(ids.map { CardCatalog.byId(it) })

        val STARTER = of("knight", "archers", "giant", "musketeer", "fireball", "arrows", "cannon", "skeletons")
        val AGGRO = of("goblins", "ram_rider", "drake", "brute", "zap", "knight", "archers", "fireball")
    }
}
