package com.skillarena.app.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import com.skillarena.core.arena.Arena
import com.skillarena.core.cards.Card
import com.skillarena.core.cards.Layer
import com.skillarena.core.cards.SpellCard
import com.skillarena.core.entities.Entity
import com.skillarena.core.match.Match
import com.skillarena.core.math.Vec2
import kotlin.math.min

/** Maps arena tiles to screen pixels. Player 0 is drawn at the bottom. */
class ArenaTransform(size: Size) {
    val scale: Float = min(size.width / Arena.WIDTH, size.height / Arena.HEIGHT)
    private val offsetX = (size.width - Arena.WIDTH * scale) / 2f
    private val offsetY = (size.height - Arena.HEIGHT * scale) / 2f

    fun toScreen(p: Vec2) = Offset(offsetX + p.x * scale, offsetY + (Arena.HEIGHT - p.y) * scale)
    fun toArena(o: Offset) = Vec2((o.x - offsetX) / scale, Arena.HEIGHT - (o.y - offsetY) / scale)
    fun len(tiles: Float) = tiles * scale
}

private val Grass = Color(0xFF3E8E41)
private val GrassDark = Color(0xFF367D39)
private val River = Color(0xFF2E86C1)
private val Bridge = Color(0xFF8D6E63)
private val Blue = Color(0xFF2979FF)
private val Red = Color(0xFFE53935)
private val ValidZone = Color(0x3300FF00)
private val InvalidZone = Color(0x33FF0000)

fun DrawScope.drawArena(match: Match, t: ArenaTransform, selected: Card?, text: TextMeasurer) {
    // Checkerboard grass
    for (x in 0 until Arena.WIDTH.toInt()) for (y in 0 until Arena.HEIGHT.toInt()) {
        val tl = t.toScreen(Vec2(x.toFloat(), y + 1f))
        drawRect(if ((x + y) % 2 == 0) Grass else GrassDark, tl, Size(t.scale, t.scale))
    }
    // River and bridges
    drawRect(River, t.toScreen(Vec2(0f, Arena.RIVER_MAX_Y)), Size(t.len(Arena.WIDTH), t.len(Arena.RIVER_MAX_Y - Arena.RIVER_MIN_Y)))
    for (bx in Arena.BRIDGE_XS) {
        drawRect(
            Bridge,
            t.toScreen(Vec2(bx - Arena.BRIDGE_HALF_WIDTH, Arena.RIVER_MAX_Y)),
            Size(t.len(Arena.BRIDGE_HALF_WIDTH * 2), t.len(Arena.RIVER_MAX_Y - Arena.RIVER_MIN_Y)),
        )
    }

    // Deploy zone overlay while a card is selected
    if (selected != null && selected !is SpellCard) {
        for (x in 0 until Arena.WIDTH.toInt()) for (y in 0 until Arena.HEIGHT.toInt()) {
            val center = Vec2(x + 0.5f, y + 0.5f)
            val ok = match.canPlaceAt(0, selected, center)
            if (!ok && y > Arena.RIVER_MAX_Y + Arena.POCKET_DEPTH) continue
            drawRect(if (ok) ValidZone else InvalidZone, t.toScreen(Vec2(x.toFloat(), y + 1f)), Size(t.scale, t.scale))
        }
    }

    // Spells in flight
    for (s in match.pendingSpells) {
        drawCircle(Color(0x66FFA000), t.len(s.card.radius), t.toScreen(s.pos))
        drawCircle(Color(0xFFFFA000), t.len(s.card.radius), t.toScreen(s.pos), style = Stroke(width = 3f))
    }

    // Ground first, then air on top
    for (e in match.entities.sortedBy { if (it.stats.layer == Layer.AIR) 1 else 0 }) drawEntity(e, t, text)
}

private fun DrawScope.drawEntity(e: Entity, t: ArenaTransform, text: TextMeasurer) {
    val c = t.toScreen(e.pos)
    val r = t.len(e.radius)
    val teamColor = if (e.owner == 0) Blue else Red
    val alpha = if (e.isDeploying) 0.4f else 1f

    if (e.isTower || e.isBuilding) {
        drawRect(teamColor.copy(alpha = alpha), Offset(c.x - r, c.y - r), Size(r * 2, r * 2))
        drawRect(Color.Black, Offset(c.x - r, c.y - r), Size(r * 2, r * 2), style = Stroke(2f))
        if (!e.active) drawCircle(Color.DarkGray, r * 0.4f, c) // sleeping king
    } else {
        if (e.stats.layer == Layer.AIR) drawCircle(Color(0x55000000), r, Offset(c.x, c.y + r * 0.8f)) // shadow
        drawCircle(teamColor.copy(alpha = alpha), r, c)
        drawCircle(Color.White.copy(alpha = alpha), r, c, style = Stroke(2f))
        val label = text.measure(e.stats.name.take(1), TextStyle(color = Color.White, fontSize = 10.sp))
        drawText(label, topLeft = Offset(c.x - label.size.width / 2f, c.y - label.size.height / 2f))
    }

    // HP bar
    if (e.hp < e.maxHp) {
        val w = maxOf(r * 2, t.len(1f))
        val top = Offset(c.x - w / 2, c.y - r - 8f)
        drawRect(Color.Black, top, Size(w, 5f))
        drawRect(if (e.owner == 0) Color.Cyan else Color.Yellow, top, Size(w * (e.hp / e.maxHp), 5f))
    }
}
