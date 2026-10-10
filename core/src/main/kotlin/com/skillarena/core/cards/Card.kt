package com.skillarena.core.cards

/** A playable card. Cost in elixir, identical for everyone. */
sealed interface Card {
    val id: String
    val name: String
    val cost: Int
}

data class TroopCard(
    override val id: String,
    override val name: String,
    override val cost: Int,
    val unit: UnitStats,
    val count: Int = 1,
    /** Radius of the circle multi-unit cards are spawned on. */
    val spread: Float = 0.7f,
) : Card

data class BuildingCard(
    override val id: String,
    override val name: String,
    override val cost: Int,
    val building: UnitStats,
) : Card

data class SpellCard(
    override val id: String,
    override val name: String,
    override val cost: Int,
    val radius: Float,
    val damage: Int,
    /** Percentage of damage dealt to crown towers. */
    val crownTowerDamagePercent: Int = 30,
    /** Seconds between casting and impact (projectile travel). */
    val impactDelay: Float = 0f,
) : Card
