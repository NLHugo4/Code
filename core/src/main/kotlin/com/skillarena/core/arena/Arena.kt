package com.skillarena.core.arena

import com.skillarena.core.math.Vec2
import kotlin.math.abs

enum class Lane { LEFT, RIGHT }

/**
 * Static arena geometry, in tiles. Player 0 owns the bottom half (low y),
 * player 1 the top half (high y). Everything is mirrored over the river.
 */
object Arena {
    const val WIDTH = 18f
    const val HEIGHT = 32f

    const val RIVER_MIN_Y = 15f
    const val RIVER_MAX_Y = 17f

    /** Bridge centre x-coordinates (one per lane). */
    val BRIDGE_XS = floatArrayOf(3.5f, 14.5f)
    const val BRIDGE_HALF_WIDTH = 1f

    /** How far a player may deploy into enemy territory after taking that lane's princess tower. */
    const val POCKET_DEPTH = 4f

    fun laneOf(x: Float): Lane = if (x < WIDTH / 2) Lane.LEFT else Lane.RIGHT

    fun laneX(lane: Lane): Float = if (lane == Lane.LEFT) BRIDGE_XS[0] else BRIDGE_XS[1]

    /** Converts a y-coordinate seen from player 0 to the same spot for [player]. */
    fun forPlayer(player: Int, y: Float): Float = if (player == 0) y else HEIGHT - y

    fun kingTowerPos(player: Int) = Vec2(WIDTH / 2, forPlayer(player, 3f))

    fun princessTowerPos(player: Int, lane: Lane) = Vec2(laneX(lane), forPlayer(player, 6.5f))

    fun isInBounds(p: Vec2) = p.x in 0f..WIDTH && p.y in 0f..HEIGHT

    fun isInRiver(y: Float) = y > RIVER_MIN_Y && y < RIVER_MAX_Y

    fun isOnBridge(x: Float) = BRIDGE_XS.any { abs(x - it) <= BRIDGE_HALF_WIDTH }

    /** -1 = bottom half, +1 = top half, 0 = river. */
    fun sideOf(y: Float): Int = when {
        y <= RIVER_MIN_Y -> -1
        y >= RIVER_MAX_Y -> 1
        else -> 0
    }

    fun isOwnSide(player: Int, y: Float): Boolean =
        if (player == 0) y < RIVER_MIN_Y else y > RIVER_MAX_Y

    /** Enemy area a player may deploy into once the enemy princess tower of that lane is gone. */
    fun isPocket(player: Int, y: Float): Boolean =
        if (player == 0) y > RIVER_MAX_Y && y < RIVER_MAX_Y + POCKET_DEPTH
        else y < RIVER_MIN_Y && y > RIVER_MIN_Y - POCKET_DEPTH
}
