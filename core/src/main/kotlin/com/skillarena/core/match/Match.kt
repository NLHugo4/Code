package com.skillarena.core.match

import com.skillarena.core.arena.Arena
import com.skillarena.core.arena.Lane
import com.skillarena.core.arena.Pathing
import com.skillarena.core.cards.BuildingCard
import com.skillarena.core.cards.Card
import com.skillarena.core.cards.CardCatalog
import com.skillarena.core.cards.Deck
import com.skillarena.core.cards.Layer
import com.skillarena.core.cards.SpellCard
import com.skillarena.core.cards.TargetPreference
import com.skillarena.core.cards.TroopCard
import com.skillarena.core.cards.UnitStats
import com.skillarena.core.entities.Entity
import com.skillarena.core.entities.EntityKind
import com.skillarena.core.math.Vec2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * A complete 1v1 match, simulated in fixed ticks.
 *
 * The simulation is deterministic: given the same seed, decks and commands it
 * always produces the same result. There is no hidden randomness in combat —
 * outcome depends only on the decisions of the players.
 */
class Match(
    decks: List<Deck>,
    val seed: Long = 0L,
    val config: MatchConfig = MatchConfig(),
) {
    init {
        require(decks.size == 2) { "A match has exactly two players" }
    }

    private val rng = Random(seed)

    val players: List<PlayerState> = decks.mapIndexed { i, d -> PlayerState(i, d, rng, config.startingElixir) }

    var tick: Long = 0L
        private set
    var phase: MatchPhase = MatchPhase.REGULAR
        private set
    var result: MatchResult? = null
        private set
    val isOver: Boolean get() = result != null

    private val _entities = mutableListOf<Entity>()
    val entities: List<Entity> get() = _entities

    private val _pendingSpells = mutableListOf<PendingSpell>()
    val pendingSpells: List<PendingSpell> get() = _pendingSpells

    private val pendingCommands = mutableListOf<Command>()

    /** Every command that was executed successfully, in order. Enough to replay the match. */
    private val _history = mutableListOf<Command>()
    val history: List<Command> get() = _history

    private val events = mutableListOf<GameEvent>()
    private var nextEntityId = 1

    init {
        for (p in 0..1) {
            spawn(p, EntityKind.KING_TOWER, CardCatalog.KING_TOWER, Arena.kingTowerPos(p), null)
            for (lane in Lane.entries) {
                spawn(p, EntityKind.PRINCESS_TOWER, CardCatalog.PRINCESS_TOWER, Arena.princessTowerPos(p, lane), null)
            }
        }
    }

    // ------------------------------------------------------------------ public API

    val secondsElapsed: Float get() = tick.toFloat() / config.ticksPerSecond

    /** Seconds left in the current period (regular time or overtime). */
    val secondsRemaining: Float
        get() {
            val end = if (tick < config.regularTicks) config.regularTicks else config.totalTicks
            return max(0L, end - tick).toFloat() / config.ticksPerSecond
        }

    /** Queues a command. It runs on [Command.tick] (or the next tick if that has passed). */
    fun submit(command: Command) {
        pendingCommands += command
    }

    /** Convenience for local play: plays a card on the next tick. */
    fun playCard(player: Int, cardId: String, pos: Vec2) =
        submit(PlayCard(player, cardId, pos.x, pos.y, tick))

    /** Checks whether a card could be played right now. Returns null when it is allowed. */
    fun validate(player: Int, cardId: String, pos: Vec2): PlayError? {
        if (isOver) return PlayError.MATCH_OVER
        val ps = players[player]
        val card = ps.hand.firstOrNull { it.id == cardId } ?: return PlayError.NOT_IN_HAND
        if (ps.elixir < card.cost) return PlayError.NOT_ENOUGH_ELIXIR
        if (!canPlaceAt(player, card, pos)) return PlayError.INVALID_POSITION
        return null
    }

    fun canPlaceAt(player: Int, card: Card, pos: Vec2): Boolean {
        if (!Arena.isInBounds(pos)) return false
        if (card is SpellCard) return true
        val lane = Arena.laneOf(pos.x)
        val allowedArea = Arena.isOwnSide(player, pos.y) ||
            (Arena.isPocket(player, pos.y) && !princessTowerAlive(1 - player, lane))
        if (!allowedArea) return false
        return _entities.none { it.isTower && it.pos.distanceTo(pos) < it.radius + 0.3f }
    }

    fun princessTowerAlive(owner: Int, lane: Lane): Boolean =
        _entities.any { it.kind == EntityKind.PRINCESS_TOWER && it.owner == owner && Arena.laneOf(it.pos.x) == lane }

    fun towers(owner: Int): List<Entity> = _entities.filter { it.owner == owner && it.isTower }

    /** Returns and clears all events since the last call. */
    fun drainEvents(): List<GameEvent> = events.toList().also { events.clear() }

    /** Advances the simulation by one tick. */
    fun step() {
        if (isOver) return
        for (e in _entities) e.prevPos = e.pos
        processCommands()
        regenerateElixir()
        updateSpells()
        for (e in _entities.toList()) if (e.isAlive) updateEntity(e)
        resolveCollisions()
        removeDead()
        if (!isOver) {
            tick++
            updateClock()
        }
    }

    /** Hash of the full simulation state; compare between clients to detect desyncs. */
    fun stateHash(): Int {
        var h = tick.hashCode()
        for (p in players) h = 31 * h + p.elixir.toBits() + 7 * p.crowns
        for (e in _entities) {
            h = 31 * h + e.id
            h = 31 * h + e.hp.toBits()
            h = 31 * h + e.pos.x.toBits()
            h = 31 * h + e.pos.y.toBits()
        }
        return h
    }

    // ------------------------------------------------------------------ commands

    private fun processCommands() {
        if (pendingCommands.isEmpty()) return
        val due = pendingCommands.filter { it.tick <= tick }.sortedWith(compareBy({ it.tick }, { it.player }))
        pendingCommands.removeAll(due)
        for (cmd in due) when (cmd) {
            is PlayCard -> execute(cmd)
        }
    }

    private fun execute(cmd: PlayCard) {
        val pos = Vec2(cmd.x, cmd.y)
        val error = validate(cmd.player, cmd.cardId, pos)
        if (error != null) {
            events += GameEvent.CardRejected(tick, cmd.player, cmd.cardId, error)
            return
        }
        val ps = players[cmd.player]
        val card = ps.hand.first { it.id == cmd.cardId }
        ps.elixir -= card.cost
        ps.cycle(card.id)
        _history += cmd.copy(tick = tick)

        when (card) {
            is TroopCard -> spawnTroops(cmd.player, card, pos)
            is BuildingCard -> spawn(cmd.player, EntityKind.BUILDING, card.building, pos, card.id)
            is SpellCard -> _pendingSpells += PendingSpell(cmd.player, card, pos, config.seconds(card.impactDelay))
        }
        events += GameEvent.CardPlayed(tick, cmd.player, card.id, pos)
    }

    private fun spawnTroops(owner: Int, card: TroopCard, center: Vec2) {
        if (card.count == 1) {
            spawn(owner, EntityKind.TROOP, card.unit, center, card.id)
            return
        }
        for (i in 0 until card.count) {
            val angle = 2.0 * PI * i / card.count
            val offset = Vec2((cos(angle) * card.spread).toFloat(), (sin(angle) * card.spread).toFloat())
            spawn(owner, EntityKind.TROOP, card.unit, clampToArena(center + offset), card.id)
        }
    }

    private fun spawn(owner: Int, kind: EntityKind, stats: UnitStats, pos: Vec2, cardId: String?): Entity {
        val e = Entity(nextEntityId++, owner, kind, stats, pos, cardId)
        e.deployTicksLeft = config.seconds(stats.deployTime)
        e.deployTicksTotal = e.deployTicksLeft
        _entities += e
        return e
    }

    // ------------------------------------------------------------------ elixir & clock

    private fun regenerateElixir() {
        val multiplier = when (phase) {
            MatchPhase.REGULAR -> 1f
            MatchPhase.DOUBLE_ELIXIR, MatchPhase.OVERTIME -> 2f
            MatchPhase.TRIPLE_ELIXIR -> 3f
            MatchPhase.ENDED -> 0f
        }
        val perTick = multiplier / (config.secondsPerElixir * config.ticksPerSecond)
        for (p in players) p.elixir = min(config.maxElixir, p.elixir + perTick)
    }

    private fun updateClock() {
        val tps = config.ticksPerSecond
        val newPhase = when {
            tick < config.regularTicks - config.doubleElixirLastSeconds * tps -> MatchPhase.REGULAR
            tick < config.regularTicks -> MatchPhase.DOUBLE_ELIXIR
            tick < config.totalTicks - config.tripleElixirLastSeconds * tps -> MatchPhase.OVERTIME
            else -> MatchPhase.TRIPLE_ELIXIR
        }

        if (tick == config.regularTicks) {
            val (c0, c1) = players[0].crowns to players[1].crowns
            if (c0 != c1 || config.overtimeSeconds == 0) {
                finishOnCrownsOrHealth(MatchResult.Reason.MORE_CROWNS)
                return
            }
        }
        if (tick >= config.totalTicks) {
            finishOnCrownsOrHealth(MatchResult.Reason.TOWER_HEALTH)
            return
        }
        if (newPhase != phase) {
            phase = newPhase
            events += GameEvent.PhaseChanged(tick, phase)
        }
    }

    private fun finishOnCrownsOrHealth(reasonIfDecidedByHealth: MatchResult.Reason) {
        val c0 = players[0].crowns
        val c1 = players[1].crowns
        when {
            c0 != c1 -> finish(if (c0 > c1) 0 else 1, MatchResult.Reason.MORE_CROWNS)
            else -> {
                // Tiebreaker: the player whose weakest tower has the least hp loses.
                val w0 = towers(0).minOf { it.hp }
                val w1 = towers(1).minOf { it.hp }
                when {
                    w0 == w1 -> finish(null, MatchResult.Reason.DRAW)
                    else -> finish(if (w0 > w1) 0 else 1, reasonIfDecidedByHealth)
                }
            }
        }
    }

    private fun finish(winner: Int?, reason: MatchResult.Reason) {
        val r = MatchResult(winner, players.map { it.crowns }, reason)
        result = r
        phase = MatchPhase.ENDED
        events += GameEvent.MatchEnded(tick, r)
    }

    // ------------------------------------------------------------------ spells

    private fun updateSpells() {
        val iter = _pendingSpells.iterator()
        while (iter.hasNext()) {
            val s = iter.next()
            if (s.ticksUntilImpact > 0) {
                s.ticksUntilImpact--
                continue
            }
            iter.remove()
            for (e in _entities) {
                if (e.owner == s.owner || !e.isAlive) continue
                if (e.pos.distanceTo(s.pos) > s.card.radius + e.radius) continue
                val dmg = if (e.isTower) s.card.damage * s.card.crownTowerDamagePercent / 100f else s.card.damage.toFloat()
                applyDamage(e, dmg)
            }
            events += GameEvent.SpellImpact(tick, s.owner, s.card.id, s.pos, s.card.radius)
        }
    }

    // ------------------------------------------------------------------ combat & movement

    private fun updateEntity(e: Entity) {
        if (e.isDeploying) {
            e.deployTicksLeft--
            return
        }
        if (e.stats.lifetime > 0f) {
            e.hp -= e.maxHp / (e.stats.lifetime * config.ticksPerSecond)
            if (!e.isAlive) return
        }
        if (!e.active) return

        e.attackCooldown = max(0f, e.attackCooldown - config.dt)
        val target = acquireTarget(e)

        if (target != null && isInRange(e, target)) {
            if (e.attackCooldown <= 0f) {
                attack(e, target)
                e.attackCooldown = e.stats.hitSpeed
            }
            return
        }
        if (!e.stats.isMobile) return
        val destination = target?.pos ?: return
        moveToward(e, destination)
    }

    private fun acquireTarget(e: Entity): Entity? {
        val current = e.targetId?.let { id -> _entities.firstOrNull { it.id == id } }
        if (current != null && current.isAlive && canTarget(e, current)) {
            val keep = if (e.stats.isMobile) {
                // Melee/ranged troops stay locked on once they're attacking.
                isInRange(e, current) || edgeDistance(e, current) <= e.stats.sightRange
            } else {
                isInRange(e, current)
            }
            if (keep) return current
        }

        val searchRadius = if (e.stats.isMobile) e.stats.sightRange else e.stats.range
        var best: Entity? = null
        var bestDist = Float.MAX_VALUE
        for (c in _entities) {
            if (c.owner == e.owner || !c.isAlive || !canTarget(e, c)) continue
            val d = edgeDistance(e, c)
            if (d <= searchRadius && d < bestDist) {
                best = c
                bestDist = d
            }
        }

        // Nothing in sight: troops march to the nearest enemy building/tower.
        if (best == null && e.stats.isMobile) {
            for (c in _entities) {
                if (c.owner == e.owner || !c.isAlive || !c.isBuilding || !canTarget(e, c)) continue
                val d = travelDistance(e.pos, c.pos, e.stats.layer)
                if (d < bestDist) {
                    best = c
                    bestDist = d
                }
            }
        }

        e.targetId = best?.id
        return best
    }

    private fun canTarget(attacker: Entity, target: Entity): Boolean {
        if (attacker.stats.targetPreference == TargetPreference.BUILDINGS && !target.isBuilding) return false
        return when (target.stats.layer) {
            Layer.GROUND -> attacker.stats.hitsGround
            Layer.AIR -> attacker.stats.hitsAir
        }
    }

    private fun edgeDistance(a: Entity, b: Entity) = a.pos.distanceTo(b.pos) - a.radius - b.radius

    private fun isInRange(a: Entity, b: Entity) = edgeDistance(a, b) <= a.stats.range

    /** Rough walking distance, taking the river into account for ground units. */
    private fun travelDistance(from: Vec2, to: Vec2, layer: Layer): Float {
        if (layer == Layer.AIR) return from.distanceTo(to)
        var d = 0f
        var p = from
        repeat(3) {
            val w = Pathing.nextWaypoint(p, to)
            d += p.distanceTo(w)
            p = w
            if (w == to) return d
        }
        return d + p.distanceTo(to)
    }

    private fun attack(attacker: Entity, target: Entity) {
        attacker.lastAttackTick = tick
        events += GameEvent.Attack(
            tick, attacker.id, target.id, attacker.owner, attacker.kind, attacker.cardId,
            attacker.pos, target.pos, attacker.stats.splashRadius,
        )
        val dmg = attacker.stats.damage.toFloat()
        if (attacker.stats.splashRadius <= 0f) {
            applyDamage(target, dmg)
            return
        }
        for (e in _entities) {
            if (e.owner == attacker.owner || !e.isAlive || !canTarget(attacker, e)) continue
            if (e.pos.distanceTo(target.pos) <= attacker.stats.splashRadius + e.radius) applyDamage(e, dmg)
        }
    }

    private fun applyDamage(target: Entity, amount: Float) {
        target.hp -= amount
        if (target.kind == EntityKind.KING_TOWER) target.active = true
    }

    private fun moveToward(e: Entity, dest: Vec2) {
        val waypoint = if (e.stats.layer == Layer.GROUND) Pathing.nextWaypoint(e.pos, dest) else dest
        val step = e.stats.speed * config.dt
        val delta = waypoint - e.pos
        val d = delta.length()
        e.pos = if (d <= step) waypoint else e.pos + delta * (step / d)
    }

    /** Simple circle separation so units don't stack on top of each other. */
    private fun resolveCollisions() {
        val n = _entities.size
        for (i in 0 until n) {
            val a = _entities[i]
            for (j in i + 1 until n) {
                val b = _entities[j]
                if (a.stats.layer != b.stats.layer) continue
                if (!a.stats.isMobile && !b.stats.isMobile) continue
                val delta = b.pos - a.pos
                val dist = delta.length()
                val overlap = a.radius + b.radius - dist
                if (overlap <= 0f) continue
                val dir = if (dist < 1e-4f) Vec2(if (a.id < b.id) 1f else -1f, 0f) else delta * (1f / dist)
                when {
                    !a.stats.isMobile -> b.pos += dir * overlap
                    !b.stats.isMobile -> a.pos -= dir * overlap
                    else -> {
                        a.pos -= dir * (overlap / 2f)
                        b.pos += dir * (overlap / 2f)
                    }
                }
            }
        }
        for (e in _entities) {
            if (!e.stats.isMobile) continue
            var p = clampToArena(e.pos)
            if (e.stats.layer == Layer.GROUND && Arena.isInRiver(p.y) && !Arena.isOnBridge(p.x)) {
                val bx = Arena.BRIDGE_XS.minBy { kotlin.math.abs(it - p.x) }
                p = Vec2(p.x.coerceIn(bx - Arena.BRIDGE_HALF_WIDTH, bx + Arena.BRIDGE_HALF_WIDTH), p.y)
            }
            e.pos = p
        }
    }

    private fun clampToArena(p: Vec2) =
        Vec2(p.x.coerceIn(0.3f, Arena.WIDTH - 0.3f), p.y.coerceIn(0.3f, Arena.HEIGHT - 0.3f))

    private fun removeDead() {
        val dead = _entities.filter { !it.isAlive }
        if (dead.isEmpty()) return
        _entities.removeAll(dead)
        for (d in dead) {
            events += GameEvent.EntityDied(tick, d.id, d.owner, d.kind, d.cardId, d.pos)
            if (!d.isTower) continue
            val opponent = players[1 - d.owner]
            events += GameEvent.TowerDestroyed(tick, d.owner, d.kind)
            if (d.kind == EntityKind.KING_TOWER) {
                opponent.crowns = 3
            } else {
                opponent.crowns++
                _entities.firstOrNull { it.owner == d.owner && it.kind == EntityKind.KING_TOWER }?.active = true
            }
        }
        val c0 = players[0].crowns
        val c1 = players[1].crowns
        when {
            c0 >= 3 && c1 >= 3 -> finish(null, MatchResult.Reason.DRAW)
            c0 >= 3 || c1 >= 3 -> finish(if (c0 > c1) 0 else 1, MatchResult.Reason.KING_TOWER_DESTROYED)
            tick >= config.regularTicks && c0 != c1 -> finish(if (c0 > c1) 0 else 1, MatchResult.Reason.SUDDEN_DEATH)
        }
    }
}

class PendingSpell(val owner: Int, val card: SpellCard, val pos: Vec2, var ticksUntilImpact: Int)
