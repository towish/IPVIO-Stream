package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

/** CHL (HockeyTech feed, chl.ca): scorebar parsing for LHJMQ/OHL/WHL. Fixture = real feed shape, trimmed. */
class ChlFeedTest {
    private val lhjmq = SportData.League("LHJMQ", "chl:lhjmq", "Eishockey", "nhl")
    private val nhl = SportData.League("NHL", "hockey/nhl", "Eishockey", "nhl")

    private val json = """
        { "SiteKit": { "Scorebar": [
          { "ID": "32917", "GameStatus": "4", "Period": "4", "GameClock": "00:00", "GameStatusString": "Final",
            "GameDateISO8601": "2026-11-13T19:00:00-04:00", "Date": "2026-11-13",
            "HomeID": "1", "HomeCity": "Moncton", "HomeNickname": "Wildcats", "HomeCode": "Mon", "HomeGoals": "5",
            "HomeLogo": "https://assets.leaguestat.com/lhjmq/logos/1.png",
            "HomeWins": "11", "HomeRegulationLosses": "9", "HomeOTLosses": "0", "HomeShootoutLosses": "1",
            "VisitorID": "8", "VisitorCity": "Saint John", "VisitorNickname": "Sea Dogs", "VisitorCode": "SNB", "VisitorGoals": "4",
            "VisitorLogo": "https://assets.leaguestat.com/lhjmq/logos/8.png",
            "VisitorWins": "7", "VisitorRegulationLosses": "12", "VisitorOTLosses": "1", "VisitorShootoutLosses": "0",
            "venue_name": "Avenir Centre", "venue_location": "Moncton (NB)" },
          { "ID": "32756", "GameStatus": "2", "Period": "3", "GameClock": "05:43", "GameStatusString": "05:43 3rd",
            "GameDateISO8601": "2026-11-15T14:00:00-04:00", "Date": "2026-11-15",
            "HomeID": "4", "HomeCity": "Charlottetown", "HomeNickname": "Islanders", "HomeCode": "Cha", "HomeGoals": "1",
            "HomeLogo": "https://assets.leaguestat.com/lhjmq/logos/4.png",
            "HomeWins": "9", "HomeRegulationLosses": "8", "HomeOTLosses": "1", "HomeShootoutLosses": "0",
            "VisitorID": "9", "VisitorCity": "Québec", "VisitorNickname": "Remparts", "VisitorCode": "Que", "VisitorGoals": "3",
            "VisitorLogo": "https://assets.leaguestat.com/lhjmq/logos/9.png",
            "VisitorWins": "12", "VisitorRegulationLosses": "6", "VisitorOTLosses": "0", "VisitorShootoutLosses": "1",
            "venue_name": "Eastlink Centre", "venue_location": "Charlottetown (PE)" },
          { "ID": "33001", "GameStatus": "1", "Period": "1", "GameClock": "00:00", "GameStatusString": "7:00PM",
            "GameDateISO8601": "2026-11-18T19:00:00-04:00", "Date": "2026-11-18",
            "HomeID": "7", "HomeCity": "Victoriaville", "HomeNickname": "Tigres", "HomeCode": "Vic", "HomeGoals": "0",
            "HomeLogo": "https://assets.leaguestat.com/lhjmq/logos/7.png",
            "HomeWins": "10", "HomeRegulationLosses": "7", "HomeOTLosses": "2", "HomeShootoutLosses": "0",
            "VisitorID": "2", "VisitorCity": "Cape Breton", "VisitorNickname": "Eagles", "VisitorCode": "Cap", "VisitorGoals": "0",
            "VisitorLogo": "https://assets.leaguestat.com/lhjmq/logos/2.png",
            "VisitorWins": "8", "VisitorRegulationLosses": "9", "VisitorOTLosses": "1", "VisitorShootoutLosses": "1",
            "venue_name": "Colisée Desjardins", "venue_location": "Victoriaville (QC)" }
        ] } }
    """.trimIndent()

    @Test
    fun isChlErkenntNurChlPfade() {
        assertTrue(SportData.isChl(lhjmq))
        assertTrue(SportData.isChl(SportData.League("OHL", "chl:ohl", "Eishockey", "nhl")))
        assertFalse(SportData.isChl(nhl))
    }

    @Test
    fun parseIsoVerstehtOffset() {
        assertEquals(1794610800L, SportData.parseIso("2026-11-13T19:00:00-04:00"))
        assertEquals(1794765600L, SportData.parseIso("2026-11-15T14:00:00-04:00"))
        assertEquals(0L, SportData.parseIso(""))
    }

    @Test
    fun scorebarWirdGelesen() {
        val games = SportData.parse(json, lhjmq)
        assertEquals(3, games.size)

        val fin = games[0]
        assertEquals("32917", fin.id)
        assertEquals("LHJMQ", fin.league)
        assertEquals(2, fin.state)
        assertEquals("Final", fin.detail)
        assertEquals("Moncton Wildcats", fin.home.name)
        assertEquals("Wildcats", fin.home.short)
        assertEquals("Mon", fin.home.abbr)
        assertEquals("5", fin.home.score)
        assertTrue(fin.home.winner)
        assertEquals("11-9-1", fin.home.record)
        assertEquals("https://assets.leaguestat.com/lhjmq/logos/1.png", fin.home.logo)
        assertEquals("Saint John Sea Dogs", fin.away.name)
        assertEquals("4", fin.away.score)
        assertFalse(fin.away.winner)
        assertEquals("7-12-1", fin.away.record)
        assertEquals("Avenir Centre, Moncton (NB)", fin.venue)
        assertEquals(1794610800L, fin.start)

        val live = games[1]
        assertEquals(1, live.state)
        assertEquals("05:43 3rd", live.detail)
        assertEquals("05:43", live.clock)
        assertEquals(3, live.period)
        assertEquals("Québec Remparts", live.away.name)
        assertEquals("Remparts", live.away.short)
        assertEquals("3", live.away.score)
        assertEquals("12-6-1", live.away.record)
        assertEquals("Charlottetown Islanders", live.home.name)
        assertEquals("1", live.home.score)
        assertFalse(live.away.winner) // winner only on final
        assertEquals(1794765600L, live.start)
        assertTrue(SportData.onDay(live, "20261115", TimeZone.getTimeZone("America/Toronto")))
        assertFalse(SportData.onDay(live, "20261114", TimeZone.getTimeZone("America/Toronto")))

        val next = games[2]
        assertEquals(0, next.state)
        assertEquals("7:00PM", next.detail)
        assertNull(next.home.score)
        assertNull(next.away.score)
        assertEquals("Cape Breton Eagles", next.away.name)
        assertEquals(1795042800L, next.start)
    }

    @Test
    fun unvollstaendigeEintraegeWerdenUebersprungen() {
        val bad = """{ "SiteKit": { "Scorebar": [
            { "ID": "1", "GameStatus": "2", "HomeCity": "", "HomeNickname": "", "VisitorCity": "Québec", "VisitorNickname": "Remparts" },
            { "ID": "2", "GameStatus": "2", "HomeCity": "Rimouski", "HomeNickname": "Océanic", "VisitorCity": "Québec", "VisitorNickname": "Remparts", "GameDateISO8601": "2026-11-15T14:00:00-04:00" }
        ] } }"""
        val games = SportData.parse(bad, lhjmq)
        assertEquals(1, games.size)
        assertEquals("2", games[0].id)
    }
}
