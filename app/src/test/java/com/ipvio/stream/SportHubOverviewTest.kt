package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Overview of the Sports Hub: two featured games, results grouped by sport, compact logo-only cards. */
class SportHubOverviewTest {
    private fun team(name: String, abbr: String, score: String? = null, winner: Boolean = false) =
        SportData.Team(name, name, abbr, "https://x/$abbr.png", score, winner)

    private fun game(id: String, league: String, state: Int, start: Long = 1_760_000_000L) =
        SportData.Game(id, league, league, start, state, "", team("Home $id", "HOM"), team("Away $id", "AWY"))

    @Test fun spieleNachSportGruppiert() {
        val games = listOf(
            game("nfl1", "NFL", 1),
            game("nba1", "NBA", 1),
            game("pl1", "Premier League", 1),
            game("nba2", "NBA", 0),
            game("nhl1", "NHL", 1),
            game("xx1", "Unbekannte Liga", 1)
        )
        val grouped = groupBySport(games)
        // Order follows the hub's sport list (soccer first), unknown leagues are dropped
        assertEquals(listOf("soccer", "basketball", "nfl", "nhl"), grouped.map { it.first.key })
        assertEquals(listOf("pl1"), grouped[0].second.map { it.id })
        assertEquals(listOf("nba1", "nba2"), grouped[1].second.map { it.id })
        assertEquals(listOf("nfl1"), grouped[2].second.map { it.id })
        assertEquals(listOf("nhl1"), grouped[3].second.map { it.id })
        assertEquals(emptyList<Pair<SportData.Sport, List<SportData.Game>>>(), groupBySport(emptyList()))
    }

    @Test fun zweiTopspieleUnterschiedlich() {
        val nba1 = game("nba1", "NBA", 1)
        val nba2 = game("nba2", "NBA", 1)
        // The game with a channel comes first, the other one second – never twice the same
        val tops = featuredTwo(listOf(nba1, nba2)) { it.id == "nba2" }
        assertEquals(listOf("nba2", "nba1"), tops.map { it.id })
        // One game only, and no games at all
        assertEquals(listOf("nba1"), featuredTwo(listOf(nba1)) { false }.map { it.id })
        assertEquals(emptyList<SportData.Game>(), featuredTwo(emptyList()) { true })
    }

    @Test fun liveVorDemnaechst() {
        val live = game("nba1", "NBA", 1)
        val up = game("nfl1", "NFL", 0, start = 1_760_001_000L)
        val tops = featuredTwo(listOf(up, live)) { false }
        assertEquals(listOf("nba1", "nfl1"), tops.map { it.id })
    }

    @Test fun sportEmojiBekanntUndFallback() {
        assertEquals("🏀", sportEmoji("basketball"))
        assertEquals("🏈", sportEmoji("nfl"))
        assertEquals("🏒", sportEmoji("nhl"))
        assertEquals("⚾", sportEmoji("mlb"))
        assertEquals("🎾", sportEmoji("tennis"))
        assertEquals("⚽", sportEmoji("soccer"))
        assertEquals("🏆", sportEmoji("unbekannt"))
        assertTrue(SportData.SPORTS.all { sportEmoji(it.key).isNotBlank() })
    }
}
