package com.skillarena.core.cards

import com.skillarena.core.cards.Speed.FAST
import com.skillarena.core.cards.Speed.MEDIUM
import com.skillarena.core.cards.Speed.SLOW
import com.skillarena.core.cards.Speed.VERY_FAST

/**
 * All cards in the game. Every card is unlocked for every player from the start
 * and has one fixed set of stats — like chess pieces.
 */
object CardCatalog {

    val KNIGHT = TroopCard(
        "knight", "Ridder", 3,
        UnitStats("Ridder", hitpoints = 1450, damage = 165, hitSpeed = 1.2f, range = 0.8f, speed = MEDIUM, radius = 0.5f),
    )

    val ARCHERS = TroopCard(
        "archers", "Boogschutters", 3,
        UnitStats(
            "Boogschutter", hitpoints = 300, damage = 105, hitSpeed = 0.9f, range = 5f, speed = MEDIUM,
            radius = 0.4f, hitsAir = true,
        ),
        count = 2,
    )

    val GIANT = TroopCard(
        "giant", "Reus", 5,
        UnitStats(
            "Reus", hitpoints = 3900, damage = 250, hitSpeed = 1.5f, range = 0.8f, speed = SLOW,
            radius = 0.75f, targetPreference = TargetPreference.BUILDINGS, sightRange = 7.5f,
        ),
    )

    val GOBLINS = TroopCard(
        "goblins", "Goblins", 2,
        UnitStats("Goblin", hitpoints = 200, damage = 120, hitSpeed = 1.1f, range = 0.5f, speed = VERY_FAST, radius = 0.35f),
        count = 3,
    )

    val MUSKETEER = TroopCard(
        "musketeer", "Scherpschutter", 4,
        UnitStats(
            "Scherpschutter", hitpoints = 720, damage = 215, hitSpeed = 1.0f, range = 6f, speed = MEDIUM,
            radius = 0.5f, hitsAir = true, sightRange = 6f,
        ),
    )

    val DRAKE = TroopCard(
        "drake", "Draakje", 4,
        UnitStats(
            "Draakje", hitpoints = 1100, damage = 160, hitSpeed = 1.5f, range = 3.5f, speed = FAST,
            radius = 0.6f, layer = Layer.AIR, hitsAir = true, splashRadius = 1.5f,
        ),
    )

    val BRUTE = TroopCard(
        "brute", "Bruut", 4,
        UnitStats("Bruut", hitpoints = 1360, damage = 720, hitSpeed = 1.6f, range = 0.8f, speed = FAST, radius = 0.5f),
    )

    val SKELETONS = TroopCard(
        "skeletons", "Skeletten", 1,
        UnitStats("Skelet", hitpoints = 80, damage = 80, hitSpeed = 1.0f, range = 0.5f, speed = FAST, radius = 0.3f),
        count = 3,
        spread = 0.5f,
    )

    val RAM_RIDER = TroopCard(
        "ram_rider", "Ramruiter", 4,
        UnitStats(
            "Ramruiter", hitpoints = 1700, damage = 320, hitSpeed = 1.6f, range = 0.8f, speed = VERY_FAST,
            radius = 0.6f, targetPreference = TargetPreference.BUILDINGS, sightRange = 9.5f,
        ),
    )

    val CANNON = BuildingCard(
        "cannon", "Kanon", 3,
        UnitStats(
            "Kanon", hitpoints = 900, damage = 210, hitSpeed = 0.9f, range = 5.5f, speed = 0f,
            radius = 0.9f, sightRange = 5.5f, lifetime = 30f, isBuilding = true,
        ),
    )

    val FIREBALL = SpellCard("fireball", "Vuurbal", 4, radius = 2.5f, damage = 690, impactDelay = 1.0f)
    val ARROWS = SpellCard("arrows", "Pijlen", 3, radius = 4f, damage = 360, impactDelay = 0.8f)
    val ZAP = SpellCard("zap", "Schok", 2, radius = 2.5f, damage = 190, impactDelay = 0f)

    val ALL: List<Card> = listOf(
        KNIGHT, ARCHERS, GIANT, GOBLINS, MUSKETEER, DRAKE, BRUTE, SKELETONS, RAM_RIDER, CANNON,
        FIREBALL, ARROWS, ZAP,
    )

    private val byId = ALL.associateBy { it.id }

    fun byId(id: String): Card = byId[id] ?: throw IllegalArgumentException("Unknown card: $id")

    // ---- Crown towers (not cards, but stats live here too) ----

    val PRINCESS_TOWER = UnitStats(
        "Prinsestoren", hitpoints = 2500, damage = 90, hitSpeed = 0.8f, range = 7.5f, speed = 0f,
        radius = 1.5f, hitsAir = true, sightRange = 7.5f, deployTime = 0f, isBuilding = true,
    )

    val KING_TOWER = UnitStats(
        "Koningstoren", hitpoints = 4000, damage = 100, hitSpeed = 1.0f, range = 7f, speed = 0f,
        radius = 2f, hitsAir = true, sightRange = 7f, deployTime = 0f, isBuilding = true,
    )
}
