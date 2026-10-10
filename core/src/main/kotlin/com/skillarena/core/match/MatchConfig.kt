package com.skillarena.core.match

/** All timing rules of a match. Identical for every player. */
data class MatchConfig(
    val ticksPerSecond: Int = 20,
    val regularTimeSeconds: Int = 180,
    /** Elixir doubles for the last N seconds of regular time. */
    val doubleElixirLastSeconds: Int = 60,
    /** Sudden-death overtime. 0 disables overtime. */
    val overtimeSeconds: Int = 120,
    /** Elixir triples for the last N seconds of overtime. */
    val tripleElixirLastSeconds: Int = 60,
    val startingElixir: Float = 5f,
    val maxElixir: Float = 10f,
    /** Seconds to generate one elixir at normal speed. */
    val secondsPerElixir: Float = 2.8f,
) {
    val dt: Float get() = 1f / ticksPerSecond
    val regularTicks: Long get() = regularTimeSeconds.toLong() * ticksPerSecond
    val totalTicks: Long get() = (regularTimeSeconds + overtimeSeconds).toLong() * ticksPerSecond

    fun seconds(s: Float): Int = (s * ticksPerSecond).toInt()
}

enum class MatchPhase { REGULAR, DOUBLE_ELIXIR, OVERTIME, TRIPLE_ELIXIR, ENDED }
