package com.skillarena.core

import com.skillarena.core.ai.SimpleBot
import com.skillarena.core.arena.Arena
import com.skillarena.core.arena.Lane
import com.skillarena.core.cards.CardCatalog
import com.skillarena.core.cards.Deck
import com.skillarena.core.entities.EntityKind
import com.skillarena.core.match.GameEvent
import com.skillarena.core.match.Match
import com.skillarena.core.match.MatchConfig
import com.skillarena.core.match.PlayError
import com.skillarena.core.match.Replay
import com.skillarena.core.math.Vec2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MatchTest {
    private val decks = listOf(Deck.STARTER, Deck.AGGRO)

    @Test
    fun `match starts with three towers per player and five elixir`() {
        val m = Match(decks)
        assertEquals(3, m.towers(0).size)
        assertEquals(3, m.towers(1).size)
        assertEquals(5f, m.players[0].elixir)
        assertEquals(4, m.players[0].hand.size)
    }

    @Test
    fun `cannot deploy troops on enemy side`() {
        val m = Match(decks)
        val troop = m.players[0].hand.first { it.cost <= 5 && it !is com.skillarena.core.cards.SpellCard }
        assertEquals(PlayError.INVALID_POSITION, m.validate(0, troop.id, Vec2(9f, 25f)))
        assertNull(m.validate(0, troop.id, Vec2(9f, 10f)))
    }

    @Test
    fun `playing a card costs elixir and cycles the hand`() {
        val m = Match(decks)
        val card = m.players[0].hand.first { it.cost <= 5 }
        val next = m.players[0].nextCard
        m.playCard(0, card.id, Vec2(9f, 10f))
        m.step()
        assertTrue(m.players[0].elixir < 5f - card.cost + 0.1f)
        assertTrue(card !in m.players[0].hand)
        assertTrue(next in m.players[0].hand)
    }

    @Test
    fun `elixir caps at ten`() {
        val m = Match(decks)
        repeat(20 * 60) { m.step() }
        assertEquals(10f, m.players[0].elixir)
    }

    @Test
    fun `giant walks over the bridge and takes a princess tower`() {
        val m = Match(listOf(Deck.of("giant", "knight", "archers", "musketeer", "fireball", "arrows", "cannon", "skeletons"), Deck.AGGRO))
        repeat(20 * 5) { m.step() } // build some elixir
        // Force the giant into hand by cycling cheap plays until it shows up.
        var guard = 0
        while (m.players[0].hand.none { it.id == "giant" } && guard++ < 20) {
            val c = m.players[0].hand.minBy { it.cost }
            repeat(20 * 10) { m.step() }
            m.playCard(0, c.id, Vec2(1f, 1f))
            m.step()
        }
        repeat(20 * 10) { m.step() }
        m.playCard(0, "giant", Vec2(Arena.laneX(Lane.LEFT), 12f))
        repeat(20 * 60) { m.step() }
        assertTrue(!m.princessTowerAlive(1, Lane.LEFT) || m.players[0].crowns >= 1, "Giant should have destroyed the left tower")
    }

    @Test
    fun `fireball deals reduced damage to towers`() {
        val m = Match(listOf(Deck.of("fireball", "zap", "arrows", "knight", "archers", "giant", "musketeer", "cannon"), Deck.AGGRO))
        repeat(20 * 10) { m.step() }
        val spell = m.players[0].hand.first { it is com.skillarena.core.cards.SpellCard }
                as com.skillarena.core.cards.SpellCard
        val tower = m.towers(1).first { it.kind == EntityKind.PRINCESS_TOWER }
        m.playCard(0, spell.id, tower.pos)
        repeat(20 * 2) { m.step() }
        val expected = CardCatalog.PRINCESS_TOWER.hitpoints - spell.damage * spell.crownTowerDamagePercent / 100f
        assertEquals(expected, tower.hp, 1f)
    }

    @Test
    fun `short match without overtime always ends`() {
        val m = Match(decks, config = MatchConfig(regularTimeSeconds = 30, doubleElixirLastSeconds = 10, overtimeSeconds = 0))
        while (!m.isOver) m.step()
        assertNotNull(m.result)
    }

    @Test
    fun `bot versus bot is deterministic and replayable`() {
        fun run(): Match {
            val m = Match(decks, seed = 42)
            val bots = listOf(SimpleBot(0, seed = 1), SimpleBot(1, seed = 2))
            while (!m.isOver) {
                bots.forEach { b -> b.think(m)?.let(m::submit) }
                m.step()
            }
            return m
        }
        val a = run()
        val b = run()
        assertEquals(a.stateHash(), b.stateHash())
        assertEquals(a.result, b.result)
        assertTrue(a.history.isNotEmpty())

        val replayed = Replay.of(a, decks).play()
        assertEquals(a.result, replayed.result)
        assertEquals(a.stateHash(), replayed.stateHash())
    }

    @Test
    fun `match emits end event`() {
        val m = Match(decks, config = MatchConfig(regularTimeSeconds = 5, doubleElixirLastSeconds = 1, overtimeSeconds = 0))
        val events = mutableListOf<GameEvent>()
        while (!m.isOver) {
            m.step()
            events += m.drainEvents()
        }
        assertTrue(events.any { it is GameEvent.MatchEnded })
    }
}
