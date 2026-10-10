package com.skillarena.app.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.skillarena.core.arena.Arena
import com.skillarena.core.math.Vec2
import kotlin.math.min

/** Maps arena tiles to screen pixels. Player 0 is drawn at the bottom. */
class ArenaTransform(size: Size) {
    val scale: Float = min(size.width / Arena.WIDTH, size.height / Arena.HEIGHT)
    private val offsetX = (size.width - Arena.WIDTH * scale) / 2f
    private val offsetY = (size.height - Arena.HEIGHT * scale) / 2f

    fun toScreen(p: Vec2) = Offset(offsetX + p.x * scale, offsetY + (Arena.HEIGHT - p.y) * scale)
    fun toScreen(x: Float, y: Float) = Offset(offsetX + x * scale, offsetY + (Arena.HEIGHT - y) * scale)
    fun toArena(o: Offset) = Vec2((o.x - offsetX) / scale, Arena.HEIGHT - (o.y - offsetY) / scale)
    fun len(tiles: Float) = tiles * scale

    /** Interpolated screen position between two ticks. */
    fun lerp(a: Vec2, b: Vec2, t: Float) = toScreen(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
}
