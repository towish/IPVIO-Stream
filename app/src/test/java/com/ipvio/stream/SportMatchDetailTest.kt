package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Match Center detail: ESPN summary parsing, weather and the combined top-scorer ranking. */
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

    @Test fun topScorerKombiniertBeideTeams() {
        val labels = listOf("G", "A", "PTS", "+/-", "SOG", "PIM")
        fun p(name: String, g: Int, a: Int) = SportMatch.Player(name, "9", "C", true, listOf("$g", "$a", "${g + a}", "0", "3", "0"))
        val home = SportMatch.Side("", listOf(
            SportMatch.Block("Forwards", labels, listOf(p("Home Star", 2, 1), p("Home Null", 0, 0))),
            SportMatch.Block("Goalies", listOf("SA", "SV", "SV%", "GA", "TOI"),
                listOf(SportMatch.Player("Home Goalie", "30", "G", true, listOf("24", "23", "0.958", "1", "60:00"))))
        ))
        val away = SportMatch.Side("", listOf(
            SportMatch.Block("Forwards", labels, listOf(p("Away Star", 1, 3), p("Away Mid", 1, 1)))
        ))
        val det = SportMatch.Detail(emptyList(), home, away, emptyList(), emptyList(), "", "", "", "", emptyList(), emptyList(), false)
        val top = topScorers(det, game)
        // Both teams combined, points first; the goalie block (no PTS column) and the pointless player drop out
        assertEquals(listOf("Away Star", "Home Star", "Away Mid"), top.map { it.player.name })
        assertEquals("HOU", top[0].teamAbbr)
        assertEquals(4, top[0].points)
        assertEquals(1, top[0].goals)
        assertEquals(3, top[0].assists)
        assertEquals("CLE", top[1].teamAbbr)
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

}
