package com.skillarena.core.cards

enum class Layer { GROUND, AIR }

enum class TargetPreference {
    /** Attacks whatever enemy is nearest. */
    ANY,
    /** Ignores troops and only attacks buildings (towers included). */
    BUILDINGS,
}

/**
 * Fixed stats for a unit, building or tower.
 *
 * There are NO levels: every player gets exactly these numbers, always.
 * Balance is changed by editing these values, never by progression.
 *
 * Units: tiles, seconds, hitpoints.
 */
data class UnitStats(
    val name: String,
    val hitpoints: Int,
    val damage: Int,
    /** Seconds between attacks. */
    val hitSpeed: Float,
    /** Attack range, measured edge-to-edge. */
    val range: Float,
    /** Tiles per second; 0 = stationary. */
    val speed: Float,
    /** Collision radius. */
    val radius: Float,
    val layer: Layer = Layer.GROUND,
    val hitsGround: Boolean = true,
    val hitsAir: Boolean = false,
    val targetPreference: TargetPreference = TargetPreference.ANY,
    /** > 0 deals area damage around the target. */
    val splashRadius: Float = 0f,
    /** Distance at which a unit notices enemies. */
    val sightRange: Float = 5.5f,
    val deployTime: Float = 1f,
    /** > 0 means the entity decays to zero over this many seconds (buildings). */
    val lifetime: Float = 0f,
    val isBuilding: Boolean = false,
) {
    val isMobile: Boolean get() = speed > 0f
}

object Speed {
    const val SLOW = 0.75f
    const val MEDIUM = 1.0f
    const val FAST = 1.5f
    const val VERY_FAST = 2.0f
}
