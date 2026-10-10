package com.skillarena.core.match

/**
 * Player input. Commands are the ONLY way to influence a match, which makes
 * the simulation replayable and suitable for lockstep multiplayer: two clients
 * that apply the same commands on the same ticks end up in the same state.
 */
sealed interface Command {
    val player: Int
    /** Tick on which the command is executed. */
    val tick: Long
}

data class PlayCard(
    override val player: Int,
    val cardId: String,
    val x: Float,
    val y: Float,
    override val tick: Long,
) : Command
