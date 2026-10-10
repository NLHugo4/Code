package com.skillarena.core.entities

import com.skillarena.core.cards.UnitStats
import com.skillarena.core.math.Vec2

enum class EntityKind { TROOP, BUILDING, PRINCESS_TOWER, KING_TOWER }

/** Anything with hitpoints on the battlefield: troops, buildings and crown towers. */
class Entity(
    val id: Int,
    val owner: Int,
    val kind: EntityKind,
    val stats: UnitStats,
    var pos: Vec2,
    /** Card that spawned this entity, null for towers. */
    val cardId: String? = null,
) {
    var hp: Float = stats.hitpoints.toFloat()
    var deployTicksLeft: Int = 0
    /** Seconds until the next attack can fire. */
    var attackCooldown: Float = 0f
    var targetId: Int? = null
    /** King tower sleeps until it takes damage or loses a princess tower. */
    var active: Boolean = kind != EntityKind.KING_TOWER

    // ---- Presentation helpers (not part of the simulation state) ----
    /** Position at the start of the current tick; renderers interpolate between this and [pos]. */
    var prevPos: Vec2 = pos
    var deployTicksTotal: Int = 0
    var lastAttackTick: Long = Long.MIN_VALUE / 2

    val maxHp: Int get() = stats.hitpoints
    val isAlive: Boolean get() = hp > 0f
    val isDeploying: Boolean get() = deployTicksLeft > 0
    val isTower: Boolean get() = kind == EntityKind.PRINCESS_TOWER || kind == EntityKind.KING_TOWER
    val isBuilding: Boolean get() = stats.isBuilding
    val radius: Float get() = stats.radius

    override fun toString() = "${stats.name}#$id(p$owner, hp=${hp.toInt()}, ${pos.x}/${pos.y})"
}
