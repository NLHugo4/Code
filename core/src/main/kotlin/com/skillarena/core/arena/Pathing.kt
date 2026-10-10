package com.skillarena.core.arena

import com.skillarena.core.math.Vec2
import kotlin.math.abs

/** Lane-based pathing for ground units: the river can only be crossed via a bridge. */
object Pathing {
    private const val EDGE_EPS = 0.05f

    fun nextWaypoint(from: Vec2, to: Vec2): Vec2 {
        val fromSide = Arena.sideOf(from.y)
        val toSide = Arena.sideOf(to.y)
        if (fromSide == toSide && fromSide != 0) return to
        if (toSide == 0 && Arena.isOnBridge(to.x)) {
            if (fromSide == 0) return to
        }

        val bx = Arena.BRIDGE_XS.minBy { abs(from.x - it) + abs(to.x - it) }

        if (fromSide == 0) {
            // On a bridge: walk straight off it towards the destination side.
            val exitSide = if (toSide != 0) toSide else if (to.y > from.y) 1 else -1
            val exitY = if (exitSide > 0) Arena.RIVER_MAX_Y + EDGE_EPS else Arena.RIVER_MIN_Y - EDGE_EPS
            return Vec2(bx, exitY)
        }

        val nearEdge = if (fromSide < 0) Arena.RIVER_MIN_Y else Arena.RIVER_MAX_Y
        val farEdge = if (fromSide < 0) Arena.RIVER_MAX_Y + EDGE_EPS else Arena.RIVER_MIN_Y - EDGE_EPS
        return if (abs(from.x - bx) > EDGE_EPS || abs(from.y - nearEdge) > 0.5f) {
            Vec2(bx, nearEdge + fromSide * EDGE_EPS)
        } else {
            Vec2(bx, farEdge)
        }
    }
}
