package com.skillarena.core.match

import com.skillarena.core.entities.EntityKind
import com.skillarena.core.math.Vec2

/** Things that happened, for UI, sound and analytics. Drained via [Match.drainEvents]. */
sealed interface GameEvent {
    val tick: Long

    data class CardPlayed(override val tick: Long, val player: Int, val cardId: String, val pos: Vec2) : GameEvent
    data class CardRejected(override val tick: Long, val player: Int, val cardId: String, val reason: PlayError) : GameEvent
    data class SpellImpact(override val tick: Long, val player: Int, val cardId: String, val pos: Vec2, val radius: Float) : GameEvent
    data class TowerDestroyed(override val tick: Long, val owner: Int, val kind: EntityKind) : GameEvent
    data class PhaseChanged(override val tick: Long, val phase: MatchPhase) : GameEvent
    data class MatchEnded(override val tick: Long, val result: MatchResult) : GameEvent
}

enum class PlayError { NOT_IN_HAND, NOT_ENOUGH_ELIXIR, INVALID_POSITION, MATCH_OVER }

data class MatchResult(
    /** null = draw. */
    val winner: Int?,
    val crowns: List<Int>,
    val reason: Reason,
) {
    enum class Reason { KING_TOWER_DESTROYED, MORE_CROWNS, SUDDEN_DEATH, TOWER_HEALTH, DRAW }
}
