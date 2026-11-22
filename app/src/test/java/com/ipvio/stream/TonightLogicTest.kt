package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Test

class TonightLogicTest {
    // 22 Nov 2026, Montréal (UTC-5): local midnight = 1_795_323_600 (UTC 05:00)
    private val off = -5 * 3600
    private val midnight = 1_795_323_600L
    private fun at(h: Int, m: Int = 0) = midnight + h * 3600 + m * 60
    private fun s(t: String, a: Long, b: Long) = TonightLogic.Show(t, a, b)

    @Test fun windowBeforeAndDuringTheEvening() {
        assertEquals(at(19) to at(23), TonightLogic.window(at(14), off))
        assertEquals(at(20) to at(23, 30), TonightLogic.window(at(20), off))
        assertEquals(at(23, 30) to at(26, 30), TonightLogic.window(at(23, 30), off))
    }

    @Test fun picksTheFirstLongShowOfTheEveningPerChannel() {
        val tva = listOf(s("Nouvelles", at(18), at(18, 30)), s("Pub", at(19), at(19, 10)), s("Star Académie", at(19, 30), at(21)))
        val rds = listOf(s("Hockey : Canadiens", at(19), at(22)))
        val r = TonightLogic.pick(listOf("TVA" to tva, "RDS" to rds), at(15), off)
        assertEquals(listOf("RDS" to "Hockey : Canadiens", "TVA" to "Star Académie"), r.map { it.channel to it.title })
    }

    @Test fun showRunningSinceAFewMinutesCountsDuringTheEvening() {
        val ch = listOf(s("Film", at(20), at(22)), s("Suite", at(22), at(23)))
        assertEquals("Film", TonightLogic.pick(listOf("X" to ch), at(20, 10), off).single().title)
        assertEquals("Suite", TonightLogic.pick(listOf("X" to ch), at(20, 40), off).single().title)
    }

    @Test fun channelsWithoutAGuideAreLeftOut() {
        assertEquals(0, TonightLogic.pick(listOf("X" to emptyList<TonightLogic.Show>()), at(15), off).size)
    }
}
