package com.skillarena.core.ai

import com.skillarena.core.arena.Arena
import com.skillarena.core.cards.BuildingCard
import com.skillarena.core.cards.SpellCard
import com.skillarena.core.cards.TroopCard
import com.skillarena.core.entities.Entity
import com.skillarena.core.entities.EntityKind
import com.skillarena.core.match.Command
import com.skillarena.core.match.Match
import com.skillarena.core.match.PlayCard
import com.skillarena.core.math.Vec2
import kotlin.random.Random

/** Anything that can play a match: local bot, network peer, scripted test player... */
interface PlayerController {
    val player: Int
    /** Called every tick; return a command or null to do nothing. */
    fun think(match: Match): Command?
}

/**
 * A simple rule-based opponent, deterministic for a given seed.
 * Defends threats on its own side, punishes clumps with spells, and pushes when it has elixir.
 */
class SimpleBot(
    override val player: Int,
    seed: Long = 1L,
    /** Minimum ticks between two plays; lower = harder. */
    private val reactionTicks: Int = 30,
) : PlayerController {
    private val rng = Random(seed)
    private var cooldown = 0

    override fun think(match: Match): Command? {
        if (match.isOver) return null
        if (cooldown > 0) {
            cooldown--
            return null
        }
        val cmd = decide(match) ?: return null
        if (match.validate(player, cmd.cardId, Vec2(cmd.x, cmd.y)) != null) return null
        cooldown = reactionTicks
        return cmd
    }

    private fun decide(match: Match): PlayCard? {
        val me = match.players[player]
        val enemy = 1 - player
        val affordable = me.hand.filter { it.cost <= me.elixir }
        if (affordable.isEmpty()) return null

        val enemyTroops = match.entities.filter { it.owner == enemy && it.kind == EntityKind.TROOP }

        // 1. Spell a clump of 3+ enemy troops.
        for (spell in affordable.filterIsInstance<SpellCard>()) {
            val clump = enemyTroops.maxByOrNull { t -> enemyTroops.count { it.pos.distanceTo(t.pos) <= spell.radius } }
            if (clump != null && enemyTroops.count { it.pos.distanceTo(clump.pos) <= spell.radius } >= 3) {
                return play(match, spell.id, clump.pos)
            }
        }

        // 2. Defend: an enemy troop is on our side of the river.
        val threat = enemyTroops.filter { Arena.isOwnSide(player, it.pos.y) }.minByOrNull { distanceToKing(it) }
        if (threat != null) {
            val defender = affordable.filter { it is TroopCard || it is BuildingCard }.maxByOrNull { it.cost }
            if (defender != null) {
                val kingY = Arena.kingTowerPos(player).y
                val y = threat.pos.y + (kingY - threat.pos.y) * 0.4f
                return play(match, defender.id, Vec2(threat.pos.x, y))
            }
        }

        // 3. Attack when elixir is (nearly) full.
        if (me.elixir >= 9f) {
            val troop = affordable.filterIsInstance<TroopCard>().randomOrNull(rng) ?: return null
            val laneX = Arena.BRIDGE_XS[rng.nextInt(Arena.BRIDGE_XS.size)]
            return play(match, troop.id, Vec2(laneX, Arena.forPlayer(player, 13.5f)))
        }
        return null
    }

    private fun distanceToKing(e: Entity) = e.pos.distanceTo(Arena.kingTowerPos(player))

    private fun play(match: Match, cardId: String, pos: Vec2) = PlayCard(player, cardId, pos.x, pos.y, match.tick)
}
