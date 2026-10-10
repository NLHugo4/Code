package com.skillarena.core

import com.skillarena.core.rating.Elo
import com.skillarena.core.rating.Matchmaker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EloTest {
    @Test
    fun `equal players exchange half the k-factor`() {
        val (a, b) = Elo.update(1200, 1200, 1.0)
        assertEquals(1210, a)
        assertEquals(1190, b)
    }

    @Test
    fun `upset gains more than expected win`() {
        val (underdog, _) = Elo.update(1000, 1400, 1.0)
        val (favourite, _) = Elo.update(1400, 1000, 1.0)
        assertTrue(underdog - 1000 > favourite - 1400)
    }

    @Test
    fun `matchmaker pairs closest ratings`() {
        val mm = Matchmaker(baseWindow = 100)
        mm.enqueue(Matchmaker.Ticket("a", 1200, 0))
        mm.enqueue(Matchmaker.Ticket("b", 1800, 0))
        mm.enqueue(Matchmaker.Ticket("c", 1250, 0))
        val pairs = mm.findMatches(0)
        assertEquals(1, pairs.size)
        assertEquals(setOf("a", "c"), setOf(pairs[0].first.playerId, pairs[0].second.playerId))
    }
}
