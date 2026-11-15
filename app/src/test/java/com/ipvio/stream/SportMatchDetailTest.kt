package com.ipvio.stream

import org.junit.Assert.assertEquals
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
                    { "athlete": { "id": "4432577", "displayName": "C.J. Stroud", "jersey": "7" }, "stats": ["18/29", "199", "3"], "starter": true },
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
        assertNull(topPlayers(SportMatch.Side("", listOf(SportMatch.Block("", emptyList(), listOf(SportMatch.Player("X", "1", "", true, emptyList()))))))))
    }
}
