package com.skillarena.app.ui.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.skillarena.core.arena.Arena
import com.skillarena.core.arena.Lane
import com.skillarena.core.cards.Card
import com.skillarena.core.cards.Layer
import com.skillarena.core.cards.SpellCard
import com.skillarena.core.entities.Entity
import com.skillarena.core.entities.EntityKind
import com.skillarena.core.match.Match
import com.skillarena.core.math.Vec2
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sin

/**
 * Draws one frame of the battle.
 * @param alpha 0..1, how far we are between the previous and the current tick (for smooth movement).
 * @param now seconds, used for animations and effects.
 */
fun DrawScope.drawBattle(
    match: Match,
    alpha: Float,
    now: Float,
    selected: Card?,
    effects: EffectSystem,
    text: TextMeasurer,
    assets: GameAssets? = null,
) {
    val t = ArenaTransform(size)
    val shake = effects.shakeOffset(now)
    translate(t.len(shake.x), t.len(shake.y)) {
        if (assets == null || !drawArenaImages(assets, t, now)) drawArenaBackground(t, now)
        if (selected != null) drawDeployZone(match, t, selected)
        drawRubble(match, t, assets)

        // Spell target markers
        for (s in match.pendingSpells) {
            val c = t.toScreen(s.pos)
            drawCircle(Palette.team(s.owner).copy(alpha = 0.18f), t.len(s.card.radius), c)
            drawCircle(
                Color.White.copy(alpha = 0.7f), t.len(s.card.radius), c,
                style = Stroke(t.len(0.08f), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), now * 30f)),
            )
        }

        effects.drawGroundEffects(this, t, now)

        // Painter's order: ground before air, far before near
        val sorted = match.entities.sortedWith(
            compareBy<Entity>({ it.stats.layer == Layer.AIR }, { -(it.prevPos.y + (it.pos.y - it.prevPos.y) * alpha) }),
        )
        for (e in sorted) drawEntity(match, e, t, alpha, now, effects, assets)

        drawSpellsInFlight(match, t, alpha, now, assets)
        effects.drawAirEffects(this, t, now, assets)

        for (e in sorted) drawHealthBar(e, t, alpha, text, hasArt = artFor(e, assets) != null)
    }
}

private fun artFor(e: Entity, assets: GameAssets?) = when (e.kind) {
    EntityKind.KING_TOWER -> assets?.tower(isKing = true, owner = e.owner)
    EntityKind.PRINCESS_TOWER -> assets?.tower(isKing = false, owner = e.owner)
    else -> assets?.unit(e.cardId, e.owner)
}

/** Drawn width in tiles for the PNG art (graphics/LEESMIJ.md). */
private fun artWidthTiles(e: Entity): Float = when (e.kind) {
    EntityKind.KING_TOWER -> 4.8f
    EntityKind.PRINCESS_TOWER -> 3.2f
    else -> maxOf(1.6f, e.radius * 3.2f)
}

/** Anchor point of the PNG art, as a fraction of the image size (graphics/LEESMIJ.md). */
private fun artAnchor(e: Entity): Offset = when {
    e.kind == EntityKind.KING_TOWER -> Offset(120f / 240f, 205f / 260f)
    e.kind == EntityKind.PRINCESS_TOWER -> Offset(80f / 160f, 180f / 210f)
    e.cardId == "cannon" -> Offset(0.5f, 150f / 200f)
    else -> Offset(0.5f, 182f / 200f)
}

private fun DrawScope.drawEntity(
    match: Match, e: Entity, t: ArenaTransform, alpha: Float, now: Float, effects: EffectSystem, assets: GameAssets?,
) {
    var c = t.lerp(e.prevPos, e.pos, alpha)
    val r = t.len(visualRadius(e))

    // Aim at the current target, otherwise face the walking direction (or the enemy side)
    val target = e.targetId?.let { id -> match.entities.firstOrNull { it.id == id } }
    var aim = when {
        target != null -> t.toScreen(target.pos) - t.toScreen(e.pos)
        e.pos != e.prevPos -> t.toScreen(e.pos) - t.toScreen(e.prevPos)
        else -> Offset(0f, if (e.owner == 0) -1f else 1f)
    }
    aim = aim.getDistance().let { if (it < 0.001f) Offset(0f, -1f) else aim / it }

    // Attack lunge and walking bob
    val tps = match.config.ticksPerSecond
    val sinceAttack = (match.tick - e.lastAttackTick - 1 + alpha) / (0.3f * tps)
    if (e.stats.isMobile && sinceAttack in 0f..1f) c += aim * (r * 0.35f * sin(sinceAttack * PI.toFloat()))
    val moving = e.pos != e.prevPos
    if (moving && e.stats.layer == Layer.GROUND) c -= Offset(0f, kotlin.math.abs(sin(now * 12f + e.id)) * r * 0.12f)

    val art = artFor(e, assets)
    if (art != null) {
        val anchor = artAnchor(e)
        drawSprite(
            art, c, Offset(anchor.x * art.width, anchor.y * art.height), t.len(artWidthTiles(e)),
            alpha = if (e.isDeploying) 0.55f else 1f,
            flipX = !e.isTower && aim.x < -0.2f,
            flash = effects.isFlashing(e.id, now),
        )
        if (e.isDeploying) drawDeployRing(e, c, t.len(artWidthTiles(e) * 0.35f), t, alpha)
        return
    }

    val ctx = SpriteContext(
        c = c,
        r = r,
        team = Palette.team(e.owner),
        teamDark = Palette.teamDark(e.owner),
        aim = aim,
        time = now,
        alpha = if (e.isDeploying) 0.55f else 1f,
        flash = effects.isFlashing(e.id, now),
        seed = e.id,
    )
    when (e.kind) {
        EntityKind.KING_TOWER -> drawTowerSprite(isKing = true, active = e.active, s = ctx)
        EntityKind.PRINCESS_TOWER -> drawTowerSprite(isKing = false, active = true, s = ctx)
        else -> drawUnitSprite(e.cardId, ctx)
    }

    if (e.isDeploying) drawDeployRing(e, c, r * 1.15f, t, alpha)
}

/** Countdown ring while a unit is still deploying. */
private fun DrawScope.drawDeployRing(e: Entity, c: Offset, rr: Float, t: ArenaTransform, alpha: Float) {
    if (e.deployTicksTotal <= 0) return
    val progress = 1f - (e.deployTicksLeft - alpha) / e.deployTicksTotal
    drawArc(
        Color.White, -90f, 360f * progress.coerceIn(0f, 1f), false,
        c - Offset(rr, rr), Size(rr * 2, rr * 2),
        style = Stroke(t.len(0.08f)),
    )
}

/** Sprites are drawn a bit larger than their collision circle so they read well on a phone. */
private fun visualRadius(e: Entity): Float = if (e.isTower) e.radius else maxOf(e.radius * 1.6f, 0.55f)

private fun DrawScope.drawHealthBar(e: Entity, t: ArenaTransform, alpha: Float, text: TextMeasurer, hasArt: Boolean) {
    val c = t.lerp(e.prevPos, e.pos, alpha)
    val r = t.len(visualRadius(e))
    val lift = if (e.stats.layer == Layer.AIR) r * 0.7f else 0f
    val team = Palette.team(e.owner)
    val frac = (e.hp / e.maxHp).coerceIn(0f, 1f)

    if (e.isTower) {
        val w = r * 1.6f
        val h = t.len(0.32f)
        // With art the tower is tall, so the bar goes just below its base.
        val top = if (hasArt) {
            Offset(c.x - w / 2, c.y + t.len(if (e.kind == EntityKind.KING_TOWER) 1.15f else 0.75f))
        } else {
            Offset(c.x - w / 2, c.y - r - h * 1.6f)
        }
        drawRoundRect(Color.Black.copy(alpha = 0.7f), top - Offset(2f, 2f), Size(w + 4f, h + 4f), CornerRadius(h / 2))
        drawRoundRect(team, top, Size(w * frac, h), CornerRadius(h / 2))
        val label = text.measure(
            e.hp.toInt().coerceAtLeast(0).toString(),
            TextStyle(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold),
        )
        drawText(label, topLeft = Offset(c.x - label.size.width / 2f, top.y + h / 2 - label.size.height / 2f))
        return
    }
    if (e.hp >= e.maxHp) return
    val w = maxOf(r * 1.6f, t.len(0.9f))
    val h = t.len(0.14f)
    val top = if (hasArt) {
        Offset(c.x - w / 2, c.y - t.len(artWidthTiles(e)) * 0.85f - h)
    } else {
        Offset(c.x - w / 2, c.y - r * 1.35f - lift - h)
    }
    drawRoundRect(Color.Black.copy(alpha = 0.7f), top - Offset(1.5f, 1.5f), Size(w + 3f, h + 3f), CornerRadius(h))
    drawRoundRect(team, top, Size(w * frac, h), CornerRadius(h))
}

private fun DrawScope.drawSpellsInFlight(match: Match, t: ArenaTransform, alpha: Float, now: Float, assets: GameAssets?) {
    for (s in match.pendingSpells) {
        val total = match.config.seconds(s.card.impactDelay)
        if (total <= 0) continue
        val p = (1f - (s.ticksUntilImpact - alpha) / total).coerceIn(0f, 1f)
        val from = Arena.kingTowerPos(s.owner)
        val pos = t.lerp(from, s.pos, p) - Offset(0f, t.len(sin(p * PI.toFloat()) * from.distanceTo(s.pos) * 0.15f))
        val art = assets?.spell(s.card.id)
        if (art != null && s.card.id != "zap") {
            // The spell art flies towards the lower-left (135 degrees); rotate it to the real direction.
            val d = t.toScreen(s.pos) - t.toScreen(from)
            val angle = atan2(d.y, d.x) * 180f / PI.toFloat()
            val width = if (s.card.id == "arrows") 2.4f else 1.8f
            drawSprite(art, pos, Offset(art.width / 2f, art.height / 2f), t.len(width), rotationDegrees = angle - 135f)
            continue
        }
        when (s.card.id) {
            "fireball" -> drawFireball(pos, t.len(0.55f), now)
            "arrows" -> {
                val dir = (t.toScreen(s.pos) - t.toScreen(from)).let { it / it.getDistance() }
                for (i in -2..2) drawArrow(pos + Offset(-dir.y, dir.x) * t.len(i * 0.35f), dir, t.len(0.7f))
            }
            else -> drawCircle(Color.White, t.len(0.3f), pos)
        }
    }
}

private fun DrawScope.drawDeployZone(match: Match, t: ArenaTransform, selected: Card) {
    if (selected is SpellCard) return
    val blocked = Color(0xFFB71C1C).copy(alpha = 0.28f)
    for (x in 0 until Arena.WIDTH.toInt()) for (y in 0 until Arena.HEIGHT.toInt()) {
        if (match.canPlaceAt(0, selected, Vec2(x + 0.5f, y + 0.5f))) continue
        drawRect(blocked, t.toScreen(x.toFloat(), y + 1f), Size(t.scale + 0.5f, t.scale + 0.5f))
    }
}

/** Rubble where a crown tower used to stand. */
private fun DrawScope.drawRubble(match: Match, t: ArenaTransform, assets: GameAssets?) {
    val puin = assets?.get("torens/puin.png")
    fun rubble(pos: Vec2, tiles: Float, seed: Int) {
        if (puin != null) {
            drawSprite(puin, t.toScreen(pos), Offset(puin.width * 0.5f, puin.height * 150f / 180f), t.len(tiles * 2f))
        } else {
            drawRubble(t.toScreen(pos), t.len(tiles), seed)
        }
    }
    for (owner in 0..1) {
        for (lane in Lane.entries) {
            if (!match.princessTowerAlive(owner, lane)) rubble(Arena.princessTowerPos(owner, lane), 1.6f, owner * 2 + lane.ordinal)
        }
        if (match.entities.none { it.owner == owner && it.kind == EntityKind.KING_TOWER }) {
            rubble(Arena.kingTowerPos(owner), 2.2f, 9 + owner)
        }
    }
}
