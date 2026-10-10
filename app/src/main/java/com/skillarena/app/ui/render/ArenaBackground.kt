package com.skillarena.app.ui.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import com.skillarena.core.arena.Arena
import kotlin.math.sin

/** Grass, dirt paths, animated river and wooden bridges. */
fun DrawScope.drawArenaBackground(t: ArenaTransform, time: Float) {
    drawRect(Palette.GrassEdge)

    // Grass checkerboard
    for (x in 0 until Arena.WIDTH.toInt()) for (y in 0 until Arena.HEIGHT.toInt()) {
        drawRect(
            if ((x + y) % 2 == 0) Palette.Grass else Palette.GrassAlt,
            t.toScreen(x.toFloat(), y + 1f),
            Size(t.scale + 0.5f, t.scale + 0.5f),
        )
    }

    // Dirt paths: one per lane, joined behind each king tower
    for (bx in Arena.BRIDGE_XS) {
        pathRect(t, bx - 0.8f, 4.2f, bx + 0.8f, Arena.HEIGHT - 4.2f)
    }
    for (y in floatArrayOf(4.2f, Arena.HEIGHT - 4.2f)) {
        pathRect(t, Arena.BRIDGE_XS[0] - 0.8f, y - 0.7f, Arena.BRIDGE_XS[1] + 0.8f, y + 0.7f)
    }

    // River with moving waves
    val riverTop = t.toScreen(0f, Arena.RIVER_MAX_Y)
    val riverSize = Size(t.len(Arena.WIDTH), t.len(Arena.RIVER_MAX_Y - Arena.RIVER_MIN_Y))
    drawRect(Palette.GrassEdge, riverTop - Offset(0f, 3f), Size(riverSize.width, riverSize.height + 6f))
    drawRect(Palette.Water, riverTop, riverSize)
    val waveStroke = t.len(0.08f)
    clipRect(riverTop.x, riverTop.y, riverTop.x + riverSize.width, riverTop.y + riverSize.height) {
    for (row in 0 until 3) {
        val y = Arena.RIVER_MIN_Y + 0.45f + row * 0.6f
        var x = -1f + ((time * 0.6f + row * 0.7f) % 2f)
        while (x < Arena.WIDTH) {
            val a = t.toScreen(x, y)
            val b = t.toScreen(x + 0.7f, y + 0.08f * sin(time * 2f + x))
            drawLine(Palette.WaterLight.copy(alpha = 0.6f), a, b, waveStroke, StrokeCap.Round)
            x += 2f
        }
    }
    }

    // Bridges with planks and rails
    for (bx in Arena.BRIDGE_XS) {
        val left = bx - Arena.BRIDGE_HALF_WIDTH - 0.15f
        val right = bx + Arena.BRIDGE_HALF_WIDTH + 0.15f
        val top = Arena.RIVER_MAX_Y + 0.3f
        val bottom = Arena.RIVER_MIN_Y - 0.3f
        drawRect(Palette.WoodDark, t.toScreen(left, top), Size(t.len(right - left), t.len(top - bottom)))
        var y = bottom + 0.05f
        while (y < top - 0.2f) {
            drawRect(
                Palette.Wood,
                t.toScreen(left + 0.15f, y + 0.3f),
                Size(t.len(right - left - 0.3f), t.len(0.24f)),
            )
            y += 0.33f
        }
        for (rx in floatArrayOf(left + 0.07f, right - 0.07f)) {
            drawLine(Palette.WoodDark, t.toScreen(rx, top), t.toScreen(rx, bottom), t.len(0.14f))
        }
    }

    // Center line markers on each side of the river
    drawLine(Color.White.copy(alpha = 0.15f), t.toScreen(0f, Arena.RIVER_MIN_Y - 0.05f), t.toScreen(Arena.WIDTH, Arena.RIVER_MIN_Y - 0.05f), 2f)
    drawLine(Color.White.copy(alpha = 0.15f), t.toScreen(0f, Arena.RIVER_MAX_Y + 0.05f), t.toScreen(Arena.WIDTH, Arena.RIVER_MAX_Y + 0.05f), 2f)

    // Arena border
    drawRect(
        Palette.GrassEdge,
        t.toScreen(0f, Arena.HEIGHT),
        Size(t.len(Arena.WIDTH), t.len(Arena.HEIGHT)),
        style = Stroke(t.len(0.15f)),
    )
}

private fun DrawScope.pathRect(t: ArenaTransform, x0: Float, y0: Float, x1: Float, y1: Float) {
    val topLeft = t.toScreen(x0, y1)
    val size = Size(t.len(x1 - x0), t.len(y1 - y0))
    val corner = CornerRadius(t.len(0.4f))
    drawRoundRect(Palette.PathEdge, topLeft - Offset(2f, 2f), Size(size.width + 4f, size.height + 4f), corner)
    drawRoundRect(Palette.Path, topLeft, size, corner)
}
