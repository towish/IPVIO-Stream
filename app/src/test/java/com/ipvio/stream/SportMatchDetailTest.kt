package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Match Center detail: ESPN summary parsing, weather, overview stat picks and top performers. */
class SportMatchDetailTest {
    private val browns = SportData.Team("Cleveland Browns", "CLE", "CLE", null, "20", false, id = "5")
    private val texans = SportData.Team("Houston Texans", "HOU", "HOU", null, "24", true, id = "34")
    private val game = SportData.Game("401873056", "NFL", "NFL", 0L, 1, "", browns, texans)

    private val json = """
        {
          "boxscore": {
            "teams": [
              { "homeAway": "home", "team": { "id": "5", "displayName": "Cleveland Browns" },
                "statistics": [
                  { "label": "1st Downs", "name": "firstDowns", "displayValue": "15" },
                  { "label": "3rd Down Efficiency", "name": "thirdDownEff", "displayValue": "3-9" },
                  { "label": "Total Yards", "name": "totalYards", "displayValue": "278" },
                  { "label": "4th Down Efficiency", "name": "fourthDownEff", "displayValue": "0-0" },
                  { "label": "Total Plays", "name": "totalOffensivePlays", "displayValue": "51" },
                  { "label": "Yards Per Play", "name": "yardsPerPlay", "displayValue": "5.5" },
                  { "label": "Passing Yards", "name": "passingYards", "displayValue": "212" },
                  { "label": "Rushing Yards", "name": "rushingYards", "displayValue": "66" },
                  { "label": "Turnovers", "name": "turnovers", "displayValue": "1" },
                  { "label": "Penalties", "name": "penalties", "displayValue": "5" }
                ] },
              { "homeAway": "away", "team": { "id": "34", "displayName": "Houston Texans" },
                "statistics": [
                  { "label": "1st Downs", "name": "firstDowns", "displayValue": "19" },
                  { "label": "3rd Down Efficiency", "name": "thirdDownEff", "displayValue": "6-13" },
                  { "label": "Total Yards", "name": "totalYards", "displayValue": "309" },
                  { "label": "4th Down Efficiency", "name": "fourthDownEff", "displayValue": "0-0" },
                  { "label": "Total Plays", "name": "totalOffensivePlays", "displayValue": "54" },
                  { "label": "Yards Per Play", "name": "yardsPerPlay", "displayValue": "5.7" },
                  { "label": "Passing Yards", "name": "passingYards", "displayValue": "199" },
                  { "label": "Rushing Yards", "name": "rushingYards", "displayValue": "110" },
                  { "label": "Turnovers", "name": "turnovers", "displayValue": "1" },
                  { "label": "Penalties", "name": "penalties", "displayValue": "5" }
                ] }
            ],
            "players": [
              { "team": { "id": "34" }, "statistics": [
                { "name": "passing", "text": "Houston Passing", "labels": ["C/ATT", "YDS", "TD"],
                  "athletes": [
                    { "athlete": { "id": "4432577", "displayName": "C.J. Stroud", "jersey": "7",
                      "headshot": { "href": "https://a.espncdn.com/i/headshots/nfl/players/full/4432577.png" } }, "stats": ["18/29", "199", "3"], "starter": true },
                    { "athlete": { "id": "4031234", "displayName": "Davis Mills", "jersey": "10" }, "stats": ["2/3", "15", "0"], "starter": false }
                  ] }
              ] },
              { "team": { "id": "5" }, "statistics": [
                { "name": "passing", "text": "Cleveland Passing", "labels": ["C/ATT", "YDS", "TD"],
                  "athletes": [
                    { "athlete": { "id": "4041234", "displayName": "Dillon Gabriel", "jersey": "8" }, "stats": ["20/32", "212", "1"], "starter": true }
                  ] }
              ] }
            ]
          },
          "winprobability": [
            { "homeWinPercentage": 0.5 }, { "homeWinPercentage": 0.6 }, { "homeWinPercentage": 0.17 }
          ],
          "drives": { "previous": [
            { "team": { "id": "5" }, "result": "PUNT", "plays": [{}, {}, {}, {}, {}], "yards": 5,
              "start": { "yardLine": 27, "text": "CLE 27" }, "end": { "yardLine": 32, "text": "CLE 32" } },
            { "team": { "id": "34" }, "result": "TD", "plays": [{}, {}, {}, {}, {}, {}, {}, {}], "yards": 61,
              "start": { "yardLine": 39, "text": "HOU 39" }, "end": { "yardLine": 95, "text": "HOU 5" } }
          ] },
          "scoringPlays": [
            { "type": { "text": "Passing Touchdown" }, "text": "Nico Collins 12 Yd pass from C.J. Stroud",
              "clock": { "displayValue": "9:29" }, "period": { "number": 1 }, "homeScore": 0, "awayScore": 7 }
          ],
          "gameInfo": {
            "venue": { "fullName": "Huntington Bank Field" },
            "weather": { "temperature": 47, "conditionId": "Cloudy" }
          }
        }
    """.trimIndent()

    @Test fun wetterUndStatsWerdenGeparst() {
        val d = SportMatch.parse(json, game)
        assertEquals("47°F · Cloudy", d.weather)
        assertEquals("Huntington Bank Field", d.venue)
        val downs = d.stats.first { it.label == "1st Downs" }
        assertEquals("15", downs.home)
        assertEquals("19", downs.away)
        val yards = d.stats.first { it.label == "Total Yards" }
        assertEquals("278", yards.home)
        assertEquals("309", yards.away)
        assertEquals("Houston Passing", d.away?.blocks?.first()?.name)
        assertEquals("C.J. Stroud", d.away?.blocks?.first()?.players?.first()?.name)
    }

    @Test fun ohneWetterBleibtEsLeer() {
        val d = SportMatch.parse("""{ "boxscore": { "teams": [] }, "gameInfo": { "venue": { "fullName": "X" } } }""", game)
        assertEquals("", d.weather)
    }

    @Test fun topStatsUeberspringtNullUnentschieden() {
        val d = SportMatch.parse(json, game)
        val top = topStats(d.stats)
        assertEquals(8, top.size)
        assertTrue(top.none { it.label == "4th Down Efficiency" })   // 0-0 on both sides adds nothing
        assertEquals("1st Downs", top.first().label)
        assertEquals("Turnovers", top.last().label)                   // 9th stat (Penalties) is cut
        assertTrue(topStats(emptyList()).isEmpty())
    }

    @Test fun analyseWirdGeparst() {
        val d = SportMatch.parse(json, game)
        assertEquals(listOf(0.5f, 0.6f, 0.17f), d.winProb)
        assertEquals(2, d.drives.size)
        val first = d.drives[0]
        assertTrue(first.home)
        assertEquals("PUNT", first.result)
        assertEquals(5, first.plays)
        assertEquals(5, first.yards)
        assertEquals(27, first.startPos)
        assertEquals(32, first.endPos)
        assertEquals("CLE 27 → CLE 32", first.label)
        val second = d.drives[1]
        assertFalse(second.home)
        assertEquals("TD", second.result)
        assertEquals(39, second.startPos)
        assertEquals(95, second.endPos)
        assertEquals(1, d.scoring.size)
        val s = d.scoring[0]
        assertEquals(1, s.period)
        assertEquals("9:29", s.clock)
        assertEquals(0, s.home)
        assertEquals(7, s.away)
        assertTrue(d.away?.blocks?.first()?.players?.first()?.headshot?.endsWith("4432577.png") == true)
    }

    @Test fun topPlayersSortiertNachBesterSpalte() {
        val d = SportMatch.parse(json, game)
        val tb = topPlayers(d.away)!!
        assertEquals(1, tb.key)                                       // YDS: C/ATT ("18/29") is not numeric
        assertEquals("YDS", tb.block.labels[tb.key])
        assertEquals("C.J. Stroud", tb.players[0].name)
        assertEquals("Davis Mills", tb.players[1].name)
        val home = topPlayers(d.home)!!
        assertEquals("Dillon Gabriel", home.players[0].name)
        assertNull(topPlayers(null))
        val emptySide = SportMatch.Side("", listOf(SportMatch.Block("", emptyList(),
            listOf(SportMatch.Player("X", "1", "", true, emptyList())))))
        assertNull(topPlayers(emptySide))
    }
}
