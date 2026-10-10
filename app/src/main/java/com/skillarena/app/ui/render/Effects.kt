package com.skillarena.app.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.skillarena.core.entities.EntityKind
import com.skillarena.core.match.GameEvent
import com.skillarena.core.math.Vec2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

enum class ProjectileStyle(val tilesPerSecond: Float, val arc: Float) {
    ARROW(14f, 0.12f),
    BULLET(28f, 0f),
    CANNONBALL(12f, 0.08f),
    FIREBALL(9f, 0.05f),
}

/** Short-lived visual effects. They never influence the simulation. */
sealed class Effect(val start: Float, val duration: Float) {
    fun progress(now: Float) = ((now - start) / duration).coerceIn(0f, 1f)
    fun isDone(now: Float) = now >= start + duration

    class Projectile(
        start: Float, duration: Float,
        val from: Vec2, val to: Vec2, val style: ProjectileStyle,
        val targetId: Int, val splash: Float, val owner: Int,
    ) : Effect(start, duration)

    class Spark(start: Float, val pos: Vec2, val color: Color, val size: Float) : Effect(start, 0.22f)
    class Puff(start: Float, val pos: Vec2, val size: Float, duration: Float = 0.45f) : Effect(start, duration)
    class Ring(start: Float, val pos: Vec2, val radius: Float, val color: Color, duration: Float = 0.35f) : Effect(start, duration)
    class Explosion(start: Float, val pos: Vec2, val radius: Float) : Effect(start, 0.55f)
    class Lightning(start: Float, val pos: Vec2, val radius: Float, val seed: Int) : Effect(start, 0.3f)
    class ArrowRain(start: Float, val pos: Vec2, val radius: Float) : Effect(start, 0.35f)
}

class EffectSystem {
    private val effects = mutableListOf<Effect>()
    private val flashUntil = HashMap<Int, Float>()
    private var shakeUntil = 0f
    private var shakeStrength = 0f

    fun clear() {
        effects.clear()
        flashUntil.clear()
        shakeUntil = 0f
    }

    fun onEvent(e: GameEvent, now: Float) {
        when (e) {
            is GameEvent.Attack -> onAttack(e, now)
            is GameEvent.CardPlayed -> Unit
            is GameEvent.SpellImpact -> when (e.cardId) {
                "fireball" -> {
                    effects += Effect.Explosion(now, e.pos, e.radius)
                    shake(now, 0.2f, 0.12f)
                }
                "zap" -> effects += Effect.Lightning(now, e.pos, e.radius, e.tick.toInt())
                "arrows" -> effects += Effect.ArrowRain(now, e.pos, e.radius)
                else -> effects += Effect.Ring(now, e.pos, e.radius, Color.White)
            }
            is GameEvent.EntityDied -> when (e.kind) {
                EntityKind.TROOP -> effects += Effect.Puff(now, e.pos, 0.7f)
                EntityKind.BUILDING -> effects += Effect.Puff(now, e.pos, 1.3f, 0.6f)
                EntityKind.PRINCESS_TOWER, EntityKind.KING_TOWER -> {
                    effects += Effect.Explosion(now, e.pos, 2.5f)
                    effects += Effect.Puff(now, e.pos, 2.2f, 0.9f)
                    shake(now, 0.5f, 0.35f)
                }
            }
            else -> Unit
        }
    }

    /** Ring that appears when a troop or building lands. Called by the UI for every new entity. */
    fun onSpawn(pos: Vec2, color: Color, now: Float) {
        effects += Effect.Ring(now, pos, 1.0f, color, 0.4f)
    }

    private fun onAttack(e: GameEvent.Attack, now: Float) {
        val style = when {
            e.attackerKind == EntityKind.PRINCESS_TOWER -> ProjectileStyle.ARROW
            e.attackerKind == EntityKind.KING_TOWER -> ProjectileStyle.CANNONBALL
            e.attackerCardId == "cannon" -> ProjectileStyle.CANNONBALL
            e.attackerCardId == "musketeer" -> ProjectileStyle.BULLET
            e.attackerCardId == "drake" -> ProjectileStyle.FIREBALL
            e.attackerCardId == "archers" -> ProjectileStyle.ARROW
            else -> null
        }
        if (style == null) {
            // Melee: instant hit
            effects += Effect.Spark(now + 0.05f, e.to, Color.White, 0.5f)
            flashUntil[e.targetId] = now + 0.15f
            return
        }
        val duration = max(0.06f, e.from.distanceTo(e.to) / style.tilesPerSecond)
        effects += Effect.Projectile(now, duration, e.from, e.to, style, e.targetId, e.splashRadius, e.owner)
    }

    private fun shake(now: Float, duration: Float, strengthTiles: Float) {
        shakeUntil = max(shakeUntil, now + duration)
        shakeStrength = max(if (now < shakeUntil) shakeStrength else 0f, strengthTiles)
    }

    fun isFlashing(entityId: Int, now: Float): Boolean = (flashUntil[entityId] ?: 0f) > now

    /** Current camera shake in tiles. */
    fun shakeOffset(now: Float): Offset {
        if (now >= shakeUntil) return Offset.Zero
        val k = (shakeUntil - now) * shakeStrength * 3f
        return Offset(sin(now * 90f) * k, cos(now * 77f) * k)
    }

    /** Advances effects; projectiles that land spawn impact effects. */
    fun update(now: Float) {
        val landed = effects.filterIsInstance<Effect.Projectile>().filter { it.isDone(now) }
        for (p in landed) {
            flashUntil[p.targetId] = now + 0.12f
            when (p.style) {
                ProjectileStyle.FIREBALL -> effects += Effect.Ring(now, p.to, max(p.splash, 0.8f), Palette.Fire, 0.3f)
                ProjectileStyle.CANNONBALL -> effects += Effect.Spark(now, p.to, Color(0xFFFFCC80), 0.7f)
                else -> effects += Effect.Spark(now, p.to, Color.White, 0.4f)
            }
        }
        effects.removeAll { it.isDone(now) }
        flashUntil.entries.removeAll { it.value < now }
    }

    fun drawGroundEffects(scope: DrawScope, t: ArenaTransform, now: Float) = with(scope) {
        for (e in effects) {
            val p = e.progress(now)
            when (e) {
                is Effect.Ring -> drawCircle(
                    e.color.copy(alpha = (1f - p) * 0.8f), t.len(e.radius * (0.4f + 0.6f * p)), t.toScreen(e.pos),
                    style = Stroke(t.len(0.15f) * (1f - p) + 1f),
                )
                is Effect.Explosion -> {
                    val c = t.toScreen(e.pos)
                    drawCircle(Color(0xFF3E2723).copy(alpha = 0.35f * (1f - p)), t.len(e.radius), c) // scorch
                    drawCircle(Palette.Fire.copy(alpha = 1f - p), t.len(e.radius * (0.3f + 0.8f * p)), c)
                    drawCircle(Palette.FireCore.copy(alpha = (1f - p) * 0.9f), t.len(e.radius * (0.2f + 0.5f * p)), c)
                }
                is Effect.ArrowRain -> {
                    val c = t.toScreen(e.pos)
                    for (i in 0 until 12) {
                        val a = i * 2.39996f
                        val d = e.radius * ((i * 7919 % 100) / 100f)
                        val hit = c + Offset(cos(a) * t.len(d), sin(a) * t.len(d) * 0.8f)
                        val fall = (1f - p) * t.len(2f)
                        drawArrow(hit - Offset(0f, fall), Offset(0.15f, 1f) / Offset(0.15f, 1f).getDistance(), t.len(0.7f), alpha = 1f - p * 0.5f)
                        drawCircle(Color(0xFFD7CCC8).copy(alpha = 0.6f * p * (1f - p) * 4f), t.len(0.25f), hit)
                    }
                }
                else -> Unit
            }
        }
    }

    fun drawAirEffects(scope: DrawScope, t: ArenaTransform, now: Float) = with(scope) {
        for (e in effects) {
            val p = e.progress(now)
            when (e) {
                is Effect.Projectile -> {
                    val pos = arcPoint(t, e, p)
                    val dir = (pos - arcPoint(t, e, max(0f, p - 0.05f))).let { d ->
                        val l = d.getDistance()
                        if (l < 0.01f) Offset(1f, 0f) else d / l
                    }
                    when (e.style) {
                        ProjectileStyle.ARROW -> drawArrow(pos, dir, t.len(0.6f))
                        ProjectileStyle.BULLET -> {
                            drawLine(Color(0xFFFFF59D).copy(alpha = 0.6f), pos - dir * t.len(0.5f), pos, t.len(0.08f))
                            drawCircle(Color(0xFFFFEE58), t.len(0.09f), pos)
                        }
                        ProjectileStyle.CANNONBALL -> {
                            drawCircle(Color.Black.copy(alpha = 0.6f), t.len(0.2f), pos)
                            drawCircle(Color(0xFF616161), t.len(0.12f), pos - Offset(t.len(0.04f), t.len(0.04f)))
                        }
                        ProjectileStyle.FIREBALL -> drawFireball(pos, t.len(0.22f), now)
                    }
                }
                is Effect.Spark -> {
                    val c = t.toScreen(e.pos)
                    val len = t.len(e.size) * (0.3f + p)
                    for (i in 0 until 6) {
                        val a = i * PI.toFloat() / 3f + 0.4f
                        val d = Offset(cos(a), sin(a))
                        drawLine(e.color.copy(alpha = 1f - p), c + d * (len * 0.3f), c + d * len, t.len(0.07f))
                    }
                }
                is Effect.Puff -> {
                    val c = t.toScreen(e.pos)
                    for (i in 0 until 5) {
                        val a = i * 1.2566f + e.start
                        val d = Offset(cos(a), sin(a)) * t.len(e.size * 0.5f * p)
                        drawCircle(Color(0xFFECEFF1).copy(alpha = (1f - p) * 0.8f), t.len(e.size * (0.25f + 0.25f * p)), c + d - Offset(0f, t.len(p * 0.5f)))
                    }
                }
                is Effect.Lightning -> {
                    val c = t.toScreen(e.pos)
                    drawCircle(Color(0xFF80D8FF).copy(alpha = (1f - p) * 0.5f), t.len(e.radius), c)
                    drawLightningBolt(c - Offset(0f, t.len(6f)), c, t.len(0.12f), e.seed, 1f - p)
                    for (i in 0 until 4) {
                        val a = i * PI.toFloat() / 2f + e.seed
                        drawLightningBolt(c, c + Offset(cos(a), sin(a)) * t.len(e.radius), t.len(0.06f), e.seed + i, 1f - p)
                    }
                }
                else -> Unit
            }
        }
    }

    private fun arcPoint(t: ArenaTransform, e: Effect.Projectile, p: Float): Offset {
        val base = t.lerp(e.from, e.to, p)
        val height = sin(p * PI.toFloat()) * e.from.distanceTo(e.to) * e.style.arc
        return base - Offset(0f, t.len(height))
    }
}
