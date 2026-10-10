package com.skillarena.app.ui.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Everything a sprite needs to draw itself.
 * [c] is the centre in pixels, [r] the collision radius in pixels and
 * [aim] a unit vector (screen space) pointing at the current target.
 */
class SpriteContext(
    val c: Offset,
    val r: Float,
    val team: Color,
    val teamDark: Color,
    val aim: Offset,
    val time: Float,
    val alpha: Float = 1f,
    val flash: Boolean = false,
    val seed: Int = 0,
) {
    fun col(color: Color): Color {
        val base = if (flash) lerp(color, Color.White, 0.6f) else color
        return base.copy(alpha = base.alpha * alpha)
    }

    val outline get() = col(Palette.Outline)
    val stroke get() = Stroke(width = r * 0.09f)
    val side: Float get() = if (aim.x < 0f) -1f else 1f
    /** Point perpendicular to [aim], useful for hands. */
    val perp: Offset get() = Offset(-aim.y, aim.x)
}

// ------------------------------------------------------------------ small helpers

private fun DrawScope.blob(s: SpriteContext, color: Color, radius: Float, center: Offset) {
    drawCircle(s.col(color), radius, center)
    drawCircle(s.outline, radius, center, style = s.stroke)
}

private fun DrawScope.stick(s: SpriteContext, color: Color, from: Offset, to: Offset, width: Float) {
    drawLine(s.outline, from, to, width + s.r * 0.09f, StrokeCap.Round)
    drawLine(s.col(color), from, to, width, StrokeCap.Round)
}

private fun DrawScope.groundShadow(s: SpriteContext, scale: Float = 1f, lift: Float = 0f) {
    val w = s.r * 2f * scale
    val h = s.r * 0.9f * scale
    drawOval(
        Color.Black.copy(alpha = 0.25f * s.alpha),
        Offset(s.c.x - w / 2, s.c.y + s.r * 0.45f - h / 2 + lift),
        Size(w, h),
    )
}

private fun DrawScope.teamBase(s: SpriteContext) {
    val w = s.r * 2.1f
    val h = s.r * 1.0f
    val topLeft = Offset(s.c.x - w / 2, s.c.y + s.r * 0.35f - h / 2)
    drawOval(s.col(s.team).copy(alpha = 0.55f * s.alpha), topLeft, Size(w, h))
    drawOval(s.col(s.teamDark), topLeft, Size(w, h), style = Stroke(s.r * 0.08f))
}

private fun Offset.rotate(angle: Float): Offset {
    val c = cos(angle)
    val s = sin(angle)
    return Offset(x * c - y * s, x * s + y * c)
}

// ------------------------------------------------------------------ troops

/** Draws the sprite for [cardId]. Unknown ids fall back to a generic soldier. */
fun DrawScope.drawUnitSprite(cardId: String?, s: SpriteContext) {
    when (cardId) {
        "knight" -> knight(s)
        "archers" -> archer(s)
        "giant" -> giant(s)
        "goblins" -> goblin(s)
        "musketeer" -> musketeer(s)
        "drake" -> drake(s)
        "brute" -> brute(s)
        "skeletons" -> skeleton(s)
        "ram_rider" -> ramRider(s)
        "cannon" -> cannon(s)
        else -> soldier(s)
    }
}

private fun DrawScope.humanoid(s: SpriteContext, bodyColor: Color, headColor: Color = Palette.Skin, bodyScale: Float = 1f) {
    groundShadow(s)
    teamBase(s)
    blob(s, bodyColor, s.r * 0.62f * bodyScale, s.c + Offset(0f, s.r * 0.1f))
    blob(s, headColor, s.r * 0.42f * bodyScale, s.c - Offset(0f, s.r * 0.45f * bodyScale))
}

private fun DrawScope.soldier(s: SpriteContext) = humanoid(s, s.team)

private fun DrawScope.knight(s: SpriteContext) {
    humanoid(s, s.team)
    val head = s.c - Offset(0f, s.r * 0.45f)
    // Helmet with visor
    drawArc(s.col(Palette.Steel), 180f, 180f, true, head - Offset(s.r * 0.45f, s.r * 0.47f), Size(s.r * 0.9f, s.r * 0.9f))
    drawArc(s.outline, 180f, 180f, true, head - Offset(s.r * 0.45f, s.r * 0.47f), Size(s.r * 0.9f, s.r * 0.9f), style = s.stroke)
    drawLine(s.outline, head + Offset(-s.r * 0.3f, s.r * 0.02f), head + Offset(s.r * 0.3f, s.r * 0.02f), s.r * 0.07f)
    // Sword towards the target
    val hand = s.c + s.perp * (s.r * 0.55f * s.side)
    stick(s, Palette.Steel, hand, hand + s.aim * (s.r * 1.1f), s.r * 0.16f)
    stick(s, Palette.Gold, hand - s.perp * (s.r * 0.18f), hand + s.perp * (s.r * 0.18f), s.r * 0.1f)
    // Shield on the other side
    blob(s, s.teamDark, s.r * 0.32f, s.c - s.perp * (s.r * 0.55f * s.side) + Offset(0f, s.r * 0.15f))
    drawCircle(s.col(Palette.Gold), s.r * 0.1f, s.c - s.perp * (s.r * 0.55f * s.side) + Offset(0f, s.r * 0.15f))
}

private fun DrawScope.archer(s: SpriteContext) {
    groundShadow(s)
    teamBase(s)
    blob(s, s.team, s.r * 0.62f, s.c + Offset(0f, s.r * 0.1f))
    val head = s.c - Offset(0f, s.r * 0.45f)
    blob(s, Color(0xFF2E7D32), s.r * 0.48f, head - Offset(0f, s.r * 0.05f)) // hood
    blob(s, Palette.Skin, s.r * 0.32f, head + Offset(0f, s.r * 0.05f))
    bow(s, s.c + s.aim * (s.r * 0.6f), s.r * 0.7f)
}

private fun DrawScope.bow(s: SpriteContext, at: Offset, size: Float) {
    val angle = atan2(s.aim.y, s.aim.x) * 180f / PI.toFloat()
    drawArc(
        s.col(Palette.WoodDark), angle - 70f, 140f, false,
        at - Offset(size, size) - s.aim * (size * 0.6f), Size(size * 2, size * 2),
        style = Stroke(s.r * 0.14f, cap = StrokeCap.Round),
    )
    val tipA = at - s.aim * (size * 0.6f) + (s.aim.rotate((-70f * PI / 180f).toFloat())) * size
    val tipB = at - s.aim * (size * 0.6f) + (s.aim.rotate((70f * PI / 180f).toFloat())) * size
    drawLine(s.col(Color.White), tipA, tipB, s.r * 0.04f)
}

private fun DrawScope.giant(s: SpriteContext) {
    groundShadow(s, 1.1f)
    teamBase(s)
    blob(s, Color(0xFF8D6E63), s.r * 0.72f, s.c + Offset(0f, s.r * 0.1f)) // tunic
    drawLine(s.col(Palette.WoodDark), s.c + Offset(-s.r * 0.65f, s.r * 0.25f), s.c + Offset(s.r * 0.65f, s.r * 0.25f), s.r * 0.14f)
    blob(s, Palette.Skin, s.r * 0.38f, s.c - Offset(0f, s.r * 0.5f))
    drawArc(s.col(Color(0xFFE65100)), 180f, 180f, true, s.c - Offset(s.r * 0.38f, s.r * 0.88f), Size(s.r * 0.76f, s.r * 0.6f)) // hair
    // Fists, the leading one punches
    val lunge = s.aim * (s.r * 0.25f)
    blob(s, Palette.Skin, s.r * 0.24f, s.c + Offset(s.r * 0.72f, 0f) + lunge)
    blob(s, Palette.Skin, s.r * 0.24f, s.c + Offset(-s.r * 0.72f, 0f) + lunge)
    // Team sash
    drawLine(s.col(s.team), s.c + Offset(-s.r * 0.45f, -s.r * 0.35f), s.c + Offset(s.r * 0.45f, s.r * 0.45f), s.r * 0.16f)
}

private fun DrawScope.goblin(s: SpriteContext) {
    groundShadow(s)
    teamBase(s)
    blob(s, Color(0xFF795548), s.r * 0.55f, s.c + Offset(0f, s.r * 0.12f))
    val head = s.c - Offset(0f, s.r * 0.4f)
    // Pointy ears
    for (dir in floatArrayOf(-1f, 1f)) {
        val ear = Path().apply {
            moveTo(head.x + dir * s.r * 0.25f, head.y - s.r * 0.1f)
            lineTo(head.x + dir * s.r * 0.85f, head.y - s.r * 0.3f)
            lineTo(head.x + dir * s.r * 0.25f, head.y + s.r * 0.15f)
            close()
        }
        drawPath(ear, s.col(Palette.GoblinGreen))
        drawPath(ear, s.outline, style = s.stroke)
    }
    blob(s, Palette.GoblinGreen, s.r * 0.4f, head)
    drawCircle(s.col(Color.Black), s.r * 0.07f, head + Offset(s.r * 0.13f * s.side, -s.r * 0.03f))
    val hand = s.c + s.perp * (s.r * 0.5f * s.side)
    stick(s, Palette.Steel, hand, hand + s.aim * (s.r * 0.6f), s.r * 0.12f)
    drawCircle(s.col(s.team), s.r * 0.14f, s.c + Offset(0f, s.r * 0.2f)) // team badge
}

private fun DrawScope.musketeer(s: SpriteContext) {
    humanoid(s, s.team)
    val head = s.c - Offset(0f, s.r * 0.45f)
    // Wide hat with feather
    drawOval(s.col(Color(0xFF4A148C)), head - Offset(s.r * 0.6f, s.r * 0.35f), Size(s.r * 1.2f, s.r * 0.4f))
    blob(s, Color(0xFF6A1B9A), s.r * 0.26f, head - Offset(0f, s.r * 0.25f))
    drawLine(s.col(Color.White), head - Offset(0f, s.r * 0.35f), head + Offset(s.r * 0.45f * s.side, -s.r * 0.7f), s.r * 0.1f, StrokeCap.Round)
    // Long gun
    val hand = s.c + s.perp * (s.r * 0.3f * s.side)
    stick(s, Palette.WoodDark, hand - s.aim * (s.r * 0.2f), hand + s.aim * (s.r * 0.35f), s.r * 0.18f)
    stick(s, Color(0xFF37474F), hand + s.aim * (s.r * 0.3f), hand + s.aim * (s.r * 1.3f), s.r * 0.1f)
}

private fun DrawScope.drake(s: SpriteContext) {
    // Flying: shadow on the ground, sprite lifted
    groundShadow(s, 0.8f, lift = s.r * 0.5f)
    val lifted = SpriteContext(s.c - Offset(0f, s.r * 0.7f), s.r, s.team, s.teamDark, s.aim, s.time, s.alpha, s.flash)
    val c = lifted.c
    val flap = sin(s.time * 12f + s.seed) * 0.35f
    for (dir in floatArrayOf(-1f, 1f)) {
        val wing = Path().apply {
            moveTo(c.x + dir * s.r * 0.25f, c.y)
            lineTo(c.x + dir * s.r * 1.25f, c.y - s.r * (0.55f + flap))
            lineTo(c.x + dir * s.r * 1.0f, c.y + s.r * 0.25f)
            lineTo(c.x + dir * s.r * 0.5f, c.y + s.r * 0.35f)
            close()
        }
        drawPath(wing, lifted.col(Color(0xFF81C784)))
        drawPath(wing, lifted.outline, style = lifted.stroke)
    }
    blob(lifted, Palette.DrakeGreen, s.r * 0.6f, c)
    drawOval(lifted.col(Color(0xFFFFF59D)), c + Offset(-s.r * 0.3f, -s.r * 0.05f), Size(s.r * 0.6f, s.r * 0.5f)) // belly
    val head = c + s.aim * (s.r * 0.55f) - Offset(0f, s.r * 0.25f)
    blob(lifted, Palette.DrakeGreen, s.r * 0.35f, head)
    drawCircle(lifted.col(Color.Black), s.r * 0.07f, head + Offset(s.r * 0.12f * s.side, -s.r * 0.05f))
    drawCircle(lifted.col(s.team), s.r * 0.13f, c + Offset(0f, s.r * 0.35f)) // team badge
}

private fun DrawScope.brute(s: SpriteContext) {
    groundShadow(s)
    teamBase(s)
    blob(s, Color(0xFF37474F), s.r * 0.66f, s.c + Offset(0f, s.r * 0.1f))
    val head = s.c - Offset(0f, s.r * 0.45f)
    // Horns
    for (dir in floatArrayOf(-1f, 1f)) {
        stick(s, Palette.Bone, head + Offset(dir * s.r * 0.3f, -s.r * 0.1f), head + Offset(dir * s.r * 0.6f, -s.r * 0.55f), s.r * 0.12f)
    }
    blob(s, Color(0xFF455A64), s.r * 0.42f, head)
    drawLine(s.col(Color(0xFF80DEEA)), head + Offset(-s.r * 0.2f, 0f), head + Offset(s.r * 0.2f, 0f), s.r * 0.1f, StrokeCap.Round) // glowing visor
    // Huge sword
    val hand = s.c + s.perp * (s.r * 0.6f * s.side)
    stick(s, Palette.Steel, hand, hand + s.aim * (s.r * 1.5f), s.r * 0.26f)
    drawCircle(s.col(s.team), s.r * 0.15f, s.c + Offset(0f, s.r * 0.2f))
}

private fun DrawScope.skeleton(s: SpriteContext) {
    groundShadow(s)
    teamBase(s)
    // Rib cage
    blob(s, Palette.Bone, s.r * 0.45f, s.c + Offset(0f, s.r * 0.15f))
    for (i in -1..1) {
        drawLine(s.outline, s.c + Offset(-s.r * 0.3f, s.r * (0.15f + i * 0.15f)), s.c + Offset(s.r * 0.3f, s.r * (0.15f + i * 0.15f)), s.r * 0.06f)
    }
    val head = s.c - Offset(0f, s.r * 0.45f)
    blob(s, Palette.Bone, s.r * 0.42f, head)
    drawCircle(s.col(Color.Black), s.r * 0.11f, head + Offset(-s.r * 0.15f, 0f))
    drawCircle(s.col(Color.Black), s.r * 0.11f, head + Offset(s.r * 0.15f, 0f))
    val hand = s.c + s.perp * (s.r * 0.45f * s.side)
    stick(s, Palette.Bone, hand, hand + s.aim * (s.r * 0.7f), s.r * 0.12f)
}

private fun DrawScope.ramRider(s: SpriteContext) {
    groundShadow(s, 1.1f)
    teamBase(s)
    // Ram body
    blob(s, Color(0xFFEDE7D9), s.r * 0.75f, s.c + Offset(0f, s.r * 0.15f))
    val ramHead = s.c + s.aim * (s.r * 0.75f) + Offset(0f, s.r * 0.05f)
    blob(s, Color(0xFF8D6E63), s.r * 0.35f, ramHead)
    for (dir in floatArrayOf(-1f, 1f)) {
        drawArc(
            s.col(Palette.WoodDark), 0f, 300f, false,
            ramHead + Offset(dir * s.r * 0.25f - s.r * 0.18f, -s.r * 0.3f), Size(s.r * 0.36f, s.r * 0.36f),
            style = Stroke(s.r * 0.1f),
        )
    }
    // Rider
    blob(s, s.team, s.r * 0.35f, s.c - Offset(0f, s.r * 0.25f))
    blob(s, Palette.Skin, s.r * 0.26f, s.c - Offset(0f, s.r * 0.7f))
    drawArc(s.col(Color(0xFFFFB300)), 180f, 180f, true, s.c - Offset(s.r * 0.28f, s.r * 0.97f), Size(s.r * 0.56f, s.r * 0.5f))
}

private fun DrawScope.cannon(s: SpriteContext) {
    groundShadow(s, 1.1f)
    teamBase(s)
    val half = s.r * 0.75f
    drawRoundRect(s.col(Palette.Wood), s.c - Offset(half, half), Size(half * 2, half * 2), CornerRadius(s.r * 0.2f))
    drawRoundRect(s.outline, s.c - Offset(half, half), Size(half * 2, half * 2), CornerRadius(s.r * 0.2f), style = s.stroke)
    drawLine(s.col(Palette.WoodDark), s.c - Offset(half, 0f), s.c + Offset(half, 0f), s.r * 0.06f)
    stick(s, Color(0xFF263238), s.c, s.c + s.aim * (s.r * 1.05f), s.r * 0.42f)
    blob(s, Color(0xFF37474F), s.r * 0.36f, s.c)
    drawCircle(s.col(s.team), s.r * 0.14f, s.c)
}

// ------------------------------------------------------------------ towers

fun DrawScope.drawTowerSprite(isKing: Boolean, active: Boolean, s: SpriteContext) {
    val half = s.r * 0.95f
    // Shadow
    drawRoundRect(Color.Black.copy(alpha = 0.25f), s.c - Offset(half, half) + Offset(s.r * 0.08f, s.r * 0.15f), Size(half * 2, half * 2), CornerRadius(s.r * 0.25f))
    // Stone body
    drawRoundRect(s.col(Palette.StoneDark), s.c - Offset(half, half), Size(half * 2, half * 2), CornerRadius(s.r * 0.25f))
    val inner = half * 0.88f
    drawRoundRect(s.col(Palette.Stone), s.c - Offset(inner, inner), Size(inner * 2, inner * 2), CornerRadius(s.r * 0.2f))
    // Brick lines
    for (i in -1..1) {
        val y = s.c.y + i * inner * 0.6f
        drawLine(s.col(Palette.StoneDark).copy(alpha = 0.5f * s.alpha), Offset(s.c.x - inner, y), Offset(s.c.x + inner, y), s.r * 0.03f)
    }
    // Battlements in the team colour
    val b = s.r * 0.22f
    for (ix in -1..1) for (iy in -1..1) {
        if (ix == 0 && iy == 0) continue
        val p = s.c + Offset(ix * (half - b * 0.6f), iy * (half - b * 0.6f))
        drawRect(s.col(s.teamDark), p - Offset(b / 2, b / 2), Size(b, b))
    }
    // Platform
    blob(s, s.team, s.r * 0.55f, s.c)

    if (isKing) {
        // King with crown, cannon when awake
        if (active) stick(s, Color(0xFF263238), s.c, s.c + s.aim * (s.r * 0.75f), s.r * 0.2f)
        blob(s, Palette.Skin, s.r * 0.3f, s.c)
        val crownBase = s.c - Offset(0f, s.r * 0.18f)
        val w = s.r * 0.28f
        val crown = Path().apply {
            moveTo(crownBase.x - w, crownBase.y)
            lineTo(crownBase.x - w, crownBase.y - w * 0.9f)
            lineTo(crownBase.x - w * 0.5f, crownBase.y - w * 0.45f)
            lineTo(crownBase.x, crownBase.y - w * 1.1f)
            lineTo(crownBase.x + w * 0.5f, crownBase.y - w * 0.45f)
            lineTo(crownBase.x + w, crownBase.y - w * 0.9f)
            lineTo(crownBase.x + w, crownBase.y)
            close()
        }
        drawPath(crown, s.col(if (active) Palette.Gold else Palette.Gold.copy(alpha = 0.6f)))
        drawPath(crown, s.outline, style = Stroke(s.r * 0.04f))
        if (!active) {
            // Sleeping: little "z"s floating up
            val zt = (s.time % 2f) / 2f
            drawCircle(Color.White.copy(alpha = (1f - zt) * 0.8f), s.r * 0.06f, s.c + Offset(s.r * 0.35f, -s.r * (0.3f + zt * 0.4f)))
        }
    } else {
        // Princess archer
        blob(s, Color(0xFFF48FB1), s.r * 0.25f, s.c + Offset(0f, s.r * 0.08f))
        blob(s, Palette.Skin, s.r * 0.18f, s.c - Offset(0f, s.r * 0.14f))
        drawArc(s.col(Color(0xFFFFD54F)), 180f, 180f, true, s.c - Offset(s.r * 0.2f, s.r * 0.34f), Size(s.r * 0.4f, s.r * 0.36f))
        val small = SpriteContext(s.c, s.r * 0.45f, s.team, s.teamDark, s.aim, s.time, s.alpha, s.flash)
        bow(small, s.c + s.aim * (s.r * 0.3f), s.r * 0.25f)
    }
}

fun DrawScope.drawRubble(c: Offset, r: Float, seed: Int) {
    drawCircle(Color(0xFF3E3E3E).copy(alpha = 0.35f), r * 0.9f, c)
    for (i in 0 until 7) {
        val a = (seed * 37 + i * 53) % 360 * PI.toFloat() / 180f
        val d = r * (0.2f + ((seed + i * 13) % 7) / 10f)
        val p = c + Offset(cos(a) * d, sin(a) * d)
        val size = r * (0.12f + (i % 3) * 0.06f)
        drawCircle(Palette.StoneDark, size, p)
        drawCircle(Palette.Stone, size * 0.7f, p - Offset(size * 0.15f, size * 0.15f))
    }
}

// ------------------------------------------------------------------ spells & icons

fun DrawScope.drawFireball(c: Offset, r: Float, time: Float, alpha: Float = 1f) {
    for (i in 0 until 3) {
        val flicker = 1f + 0.12f * sin(time * 30f + i * 2f)
        drawCircle(Palette.Fire.copy(alpha = 0.35f * alpha), r * (1.3f - i * 0.15f) * flicker, c + Offset(0f, r * 0.2f * i))
    }
    drawCircle(Palette.Fire.copy(alpha = alpha), r, c)
    drawCircle(Palette.FireCore.copy(alpha = alpha), r * 0.55f, c - Offset(r * 0.15f, r * 0.15f))
}

fun DrawScope.drawArrow(tip: Offset, dir: Offset, length: Float, color: Color = Palette.WoodDark, alpha: Float = 1f) {
    val tail = tip - dir * length
    drawLine(color.copy(alpha = alpha), tail, tip, length * 0.12f, StrokeCap.Round)
    val head = Path().apply {
        val side = Offset(-dir.y, dir.x) * (length * 0.18f)
        moveTo(tip.x + dir.x * length * 0.15f, tip.y + dir.y * length * 0.15f)
        lineTo(tip.x + side.x, tip.y + side.y)
        lineTo(tip.x - side.x, tip.y - side.y)
        close()
    }
    drawPath(head, Palette.SteelDark.copy(alpha = alpha))
    drawLine(Color.White.copy(alpha = alpha), tail, tail + dir * (length * 0.2f), length * 0.16f)
}

fun DrawScope.drawLightningBolt(from: Offset, to: Offset, width: Float, seed: Int, alpha: Float = 1f) {
    val segments = 6
    var prev = from
    for (i in 1..segments) {
        val t = i / segments.toFloat()
        val jitter = if (i == segments) 0f else (((seed * 31 + i * 17) % 11) - 5) / 5f * width * 2f
        val next = Offset(from.x + (to.x - from.x) * t + jitter, from.y + (to.y - from.y) * t)
        drawLine(Color(0xFF80D8FF).copy(alpha = alpha), prev, next, width * 2.2f, StrokeCap.Round)
        drawLine(Color.White.copy(alpha = alpha), prev, next, width, StrokeCap.Round)
        prev = next
    }
}

/** Icon drawn on the cards in your hand. */
fun DrawScope.drawCardIcon(cardId: String, time: Float) {
    val c = Offset(size.width / 2, size.height / 2)
    val r = minOf(size.width, size.height) * 0.28f
    val aim = Offset(1f, -0.3f).let { it / it.getDistance() }
    fun ctx(at: Offset, radius: Float) = SpriteContext(at, radius, Palette.Blue, Palette.BlueDark, aim, time)
    when (cardId) {
        "fireball" -> drawFireball(c, r * 0.9f, time)
        "arrows" -> for (i in -1..1) drawArrow(c + Offset(i * r * 0.55f, r * 0.6f - abs(i) * r * 0.2f), Offset(0.3f, -1f).let { it / it.getDistance() } * -1f, r * 1.4f)
        "zap" -> drawLightningBolt(c - Offset(r * 0.3f, r * 1.1f), c + Offset(r * 0.2f, r * 1.0f), r * 0.12f, 3)
        "archers" -> {
            drawUnitSprite(cardId, ctx(c + Offset(-r * 0.45f, 0f), r * 0.7f))
            drawUnitSprite(cardId, ctx(c + Offset(r * 0.45f, r * 0.2f), r * 0.7f))
        }
        "goblins", "skeletons" -> {
            drawUnitSprite(cardId, ctx(c + Offset(0f, -r * 0.35f), r * 0.55f))
            drawUnitSprite(cardId, ctx(c + Offset(-r * 0.55f, r * 0.35f), r * 0.55f))
            drawUnitSprite(cardId, ctx(c + Offset(r * 0.55f, r * 0.35f), r * 0.55f))
        }
        else -> drawUnitSprite(cardId, ctx(c, r))
    }
}
