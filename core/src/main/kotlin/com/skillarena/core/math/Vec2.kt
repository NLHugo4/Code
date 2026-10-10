package com.skillarena.core.math

import kotlin.math.sqrt

/** Immutable 2D vector in tile units. */
data class Vec2(val x: Float, val y: Float) {
    operator fun plus(o: Vec2) = Vec2(x + o.x, y + o.y)
    operator fun minus(o: Vec2) = Vec2(x - o.x, y - o.y)
    operator fun times(s: Float) = Vec2(x * s, y * s)

    fun length(): Float = sqrt(x * x + y * y)
    fun distanceTo(o: Vec2): Float = (this - o).length()

    fun normalized(): Vec2 {
        val l = length()
        return if (l < 1e-6f) ZERO else Vec2(x / l, y / l)
    }

    companion object {
        val ZERO = Vec2(0f, 0f)
    }
}
