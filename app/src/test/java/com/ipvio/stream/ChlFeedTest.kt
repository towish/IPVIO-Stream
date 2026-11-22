package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

    // ───────── Game summary (box score) ─────────

    private val game32756 = SportData.Game(
        "32756", "LHJMQ", "Eishockey", 1794765600L, 2, "Final",
        SportData.Team("Charlottetown Islanders", "Islanders", "Cha",
            "https://assets.leaguestat.com/lhjmq/logos/4.png", "1", false, "9-8-1", null, "4"),
        SportData.Team("Québec Remparts", "Remparts", "Que",
            "https://assets.leaguestat.com/lhjmq/logos/9.png", "3", true, "12-6-1", null, "9")
    )

    /** Real gamesummary of the Remparts game (final 3-1, 15 Nov 2026), trimmed: 2 goals, 2 penalties, 2 skaters + 1 goalie per team. */
    private val gsJson = """
        {"GC": {"Gamesummary": {"meta": {"id": "32756", "season_id": "214", "league_id": "1", "home_team": "7", "visiting_team": "9", "game_number": "178", "game_letter": "", "type_id": "", "if_necessary": "0", "quick_score": "0", "mvp1": "23481", "mvp2": "20201", "mvp3": "20221", "featured_player_id": "", "date_played": "2026-11-15", "date_tbd": "0", "schedule_time": "15:00:00", "time_tbd": "0", "timezone": "America/Halifax", "start_time": "15:03:00", "end_time": "17:24:00", "forfeit": "0", "shootout": "0", "shootout_first_shooter_home": "0", "attendance": "2355", "location": "164", "home_coach": "", "home_assistant_coach1": "0", "home_assistant_coach2": "0", "home_manager": "", "visiting_coach": "", "visiting_assistant_coach1": "0", "visiting_assistant_coach2": "0", "visiting_manager": "", "period": "3", "game_clock": "00:00:00", "status": "4", "started": "1", "pending_final": "0", "final": "1", "home_goal_count": "1", "visiting_goal_count": "3", "home_goals_actual": "1", "public_notes": "", "private_notes": "", "league_game_notes": "", "visiting_team_notes": "", "home_team_notes": "", "schedule_notes": "", "schedule_notes_fr": "", "home_audio_url": "https://max931.ca/", "home_video_url": "", "home_webcast_url": "https://www.flohockey.tv/leagues/10826800-ligue-de-hockey-junior-maritimes-quebec-lhjmq?nav_id=680", "home_audio_url_fr": "https://max931.ca/", "home_video_url_fr": "", "home_webcast_url_fr": "https://www.flohockey.tv/leagues/10826800-ligue-de-hockey-junior-maritimes-quebec-lhjmq?nav_id=680", "visiting_webcast_url": "https://www.flohockey.tv/leagues/10826800-ligue-de-hockey-junior-maritimes-quebec-lhjmq?nav_id=680", "visiting_video_url": "", "visiting_audio_url": "https://chyz.ca/emission/hockey-des-remparts-de-quebec/", "visiting_webcast_url_fr": "https://www.flohockey.tv/leagues/10826800-ligue-de-hockey-junior-maritimes-quebec-lhjmq?nav_id=680", "visiting_video_url_fr": "", "visiting_audio_url_fr": "https://chyz.ca/emission/hockey-des-remparts-de-quebec/", "tickets_url": "https://purchase.eastlinkcentrepei.com/Events", "tickets_url_fr": "https://purchase.eastlinkcentrepei.com/Events", "last_modified": "2026-11-15 21:33:16", "watch_now_url": "", "watch_now_droid_url": "", "visiting_goals_actual": "3", "imported_id": "", "created": "2026-06-16 12:32:00", "htv_game_id": "1152236", "flo_core_event_id": "16025097", "flo_live_event_id": "253002", "uses_spalk_rc": "", "tournament_id": "", "start_time_without_seconds": "15:03", "end_time_without_seconds": "17:24", "12_hour_start_time_without_seconds": "3:03 pm", "12_hour_end_time_without_seconds": "5:24 pm", "length": "2:21", "game_date_iso_8601": "2026-11-15T15:00:00-04:00", "timezone_short": "AST"}, "venue": "Eastlink Centre", "totalShots": {"visitor": 18, "home": 24}, "totalShotsOn": {"visitor": 12, "home": 11}, "totalFaceoffs": {"visitor": {"att": 57, "won": 24}, "home": {"att": 57, "won": 33}}, "totalHits": {"visitor": 19, "home": 21}, "pimTotal": {"visitor": 10, "home": 12}, "powerPlayGoals": {"visitor": 1, "home": 0}, "powerPlayCount": {"visitor": 4, "home": 3}, "goalsByPeriod": {"visitor": {"1": 1, "2": 0, "3": 2}, "home": {"1": 1, "2": 0, "3": 0}}, "shotsByPeriod": {"visitor": {"1": 8, "2": 1, "3": 9}, "home": {"1": 12, "2": 10, "3": 2}}, "goals": [{"event": "goal", "x_location": "612", "y_location": "151", "time": "6:17", "team_id": "7", "home": "1", "period_id": "1", "goal_type": "", "location_set": "0", "power_play": "0", "empty_net": "0", "penalty_shot": "0", "short_handed": "0", "insurance_goal": "0", "game_winning": "0", "game_tieing": "0", "scorer_goal_num": "2", "s": 377, "goal_scorer": {"player_id": "20201", "jersey_number": "14", "team_id": "7", "team_code": "Cha", "first_name": "Anthony", "last_name": "Flanagan"}, "assist1_player": {"player_id": "24670", "jersey_number": "77", "team_id": "7", "team_code": "Cha", "first_name": "Kage", "last_name": "Flory"}, "assist2_player": {"player_id": "24578", "jersey_number": "47", "team_id": "7", "team_code": "Cha", "first_name": "Artyom", "last_name": "Mate"}, "plus": [{"player_id": "20201", "jersey_number": "14", "first_name": "Anthony", "last_name": "Flanagan"}, {"player_id": "23335", "jersey_number": "17", "first_name": "Antoine", "last_name": "Provencher"}, {"player_id": "19680", "jersey_number": "29", "first_name": "Antoine", "last_name": "Boudreau"}, {"player_id": "24578", "jersey_number": "47", "first_name": "Artyom", "last_name": "Mate"}, {"player_id": "24670", "jersey_number": "77", "first_name": "Kage", "last_name": "Flory"}], "minus": [{"player_id": "23366", "jersey_number": "2", "first_name": "Bastien", "last_name": "Michaud"}, {"player_id": "21474", "jersey_number": "13", "first_name": "Samuel", "last_name": "Huot"}, {"player_id": "23337", "jersey_number": "19", "first_name": "James", "last_name": "Scantlebury"}, {"player_id": "23345", "jersey_number": "43", "first_name": "Carter", "last_name": "Meyer"}, {"player_id": "19561", "jersey_number": "55", "first_name": "Étienne", "last_name": "Desjardins"}]}, {"event": "goal", "x_location": "225", "y_location": "181", "time": "8:46", "team_id": "9", "home": "0", "period_id": "1", "goal_type": "PP", "location_set": "0", "power_play": "1", "empty_net": "0", "penalty_shot": "0", "short_handed": "0", "insurance_goal": "0", "game_winning": "0", "game_tieing": "0", "scorer_goal_num": "4", "s": 526, "goal_scorer": {"player_id": "20221", "jersey_number": "47", "team_id": "9", "team_code": "Que", "first_name": "Maddox", "last_name": "Labre"}, "assist1_player": {"player_id": "23354", "jersey_number": "91", "team_id": "9", "team_code": "Que", "first_name": "Alex", "last_name": "Desruisseaux"}, "assist2_player": {"player_id": "20193", "jersey_number": "6", "team_id": "9", "team_code": "Que", "first_name": "Egan", "last_name": "Beveridge"}, "plus": [{"player_id": "20193", "jersey_number": "6", "first_name": "Egan", "last_name": "Beveridge"}, {"player_id": "24577", "jersey_number": "15", "first_name": "Samuel", "last_name": "Sramaty"}, {"player_id": "20221", "jersey_number": "47", "first_name": "Maddox", "last_name": "Labre"}, {"player_id": "19671", "jersey_number": "63", "first_name": "Mavrick", "last_name": "Rousseau-Hamel"}, {"player_id": "23354", "jersey_number": "91", "first_name": "Alex", "last_name": "Desruisseaux"}], "minus": [{"player_id": "24814", "jersey_number": "2", "first_name": "Hughston", "last_name": "Hurt"}, {"player_id": "20183", "jersey_number": "10", "first_name": "Owen", "last_name": "Conrad"}, {"player_id": "23375", "jersey_number": "71", "first_name": "Cameron", "last_name": "Haye"}, {"player_id": "21345", "jersey_number": "74", "first_name": "Hugo", "last_name": "Charron"}]}], "penalties": [{"event": "penalty", "time_off_formatted": "3:09", "team_id": "7", "home": "1", "period_id": "1", "period": "1st", "offence": "403", "pp": "1", "bench": "0", "penalty_shot": "0", "minutes": 2, "minutes_formatted": "2:00", "penalty_class_id": "1", "penalty_class": "Minor", "lang_penalty_description": "Roughing", "s": 189, "player_penalized_info": {"player_id": "21345", "jersey_number": "74", "team_id": "7", "team_code": "Cha", "first_name": "Hugo", "last_name": "Charron"}, "player_served_info": {"player_id": "21345", "jersey_number": "74", "team_id": "7", "team_code": "Cha", "first_name": "Hugo", "last_name": "Charron"}}, {"event": "penalty", "time_off_formatted": "7:09", "team_id": "7", "home": "1", "period_id": "1", "period": "1st", "offence": "402", "pp": "1", "bench": "0", "penalty_shot": "0", "minutes": 2, "minutes_formatted": "2:00", "penalty_class_id": "1", "penalty_class": "Minor", "lang_penalty_description": "Holding", "s": 429, "player_penalized_info": {"player_id": "23398", "jersey_number": "43", "team_id": "7", "team_code": "Cha", "first_name": "Ryan", "last_name": "Jewer"}, "player_served_info": {"player_id": "23398", "jersey_number": "43", "team_id": "7", "team_code": "Cha", "first_name": "Ryan", "last_name": "Jewer"}}], "mvps": [{"player_id": "23481", "first_name": "Timothy", "last_name": "Dubuc", "jersey_number": "35", "home": 0}, {"player_id": "20201", "first_name": "Anthony", "last_name": "Flanagan", "jersey_number": "14", "home": 1}, {"player_id": "20221", "first_name": "Maddox", "last_name": "Labre", "jersey_number": "47", "home": 0}], "home_team_lineup": {"players": [{"person_id": "31430", "player_id": "24814", "first_name": "Hughston", "last_name": "Hurt", "start": "0", "position": "6", "position_str": "RD", "status": "", "rookie": "0", "goals": 0, "assists": 0, "plusminus": -2, "pim": 0, "faceoff_wins": "0", "faceoff_attempts": "0", "hits": "0", "icings": null, "offsides": null, "shots": "0", "shots_on": "0", "power_play_goals": 0, "short_handed_goals": 0, "game_winning_goal": 0, "jersey_number": "2"}, {"person_id": "25664", "player_id": "20183", "first_name": "Owen", "last_name": "Conrad", "start": "1", "position": "5", "position_str": "LD", "status": "C", "rookie": "0", "goals": 0, "assists": 0, "plusminus": -1, "pim": 2, "faceoff_wins": "0", "faceoff_attempts": "0", "hits": "4", "icings": null, "offsides": null, "shots": "2", "shots_on": "0", "power_play_goals": 0, "short_handed_goals": 0, "game_winning_goal": 0, "jersey_number": "10"}], "goalies": [{"person_id": "25735", "player_id": "20226", "first_name": "Donald", "last_name": "Hickey", "start": "1", "position": "7", "position_str": "G", "status": "", "rookie": "0", "seconds": 3442, "time": "57:22", "shots_against": 18, "goals_against": 3, "saves": 15, "goals": 0, "assists": 0, "pim": 0, "shots": "0", "jersey_number": "1"}]}, "visitor_team_lineup": {"players": [{"person_id": "28761", "player_id": "23366", "first_name": "Bastien", "last_name": "Michaud", "start": "0", "position": "6", "position_str": "RD", "status": "", "rookie": "1", "goals": 0, "assists": 0, "plusminus": 0, "pim": 0, "faceoff_wins": "0", "faceoff_attempts": "0", "hits": "2", "icings": null, "offsides": null, "shots": "0", "shots_on": "0", "power_play_goals": 0, "short_handed_goals": 0, "game_winning_goal": 0, "jersey_number": "2"}, {"person_id": "25482", "player_id": "20193", "first_name": "Egan", "last_name": "Beveridge", "start": "0", "position": "2", "position_str": "RW", "status": "", "rookie": "0", "goals": 1, "assists": 1, "plusminus": "+1", "pim": 0, "faceoff_wins": "0", "faceoff_attempts": "0", "hits": "1", "icings": null, "offsides": null, "shots": "2", "shots_on": "2", "power_play_goals": 0, "short_handed_goals": 0, "game_winning_goal": 1, "jersey_number": "6"}], "goalies": [{"person_id": "29278", "player_id": "23481", "first_name": "Timothy", "last_name": "Dubuc", "start": "1", "position": "7", "position_str": "G", "status": "", "rookie": "1", "seconds": 3600, "time": "60:00", "shots_against": 24, "goals_against": 1, "saves": 23, "goals": 0, "assists": 0, "pim": 0, "shots": "0", "jersey_number": "35"}]}}}}
    """.trimIndent()

    @Test
    fun gamesummaryWirdGelesen() {
        val d = SportMatch.parseChl("angular.callbacks._0($gsJson)", game32756)
        assertNotNull(d)
        d!!
        // Team stats, home first
        assertEquals(6, d.stats.size)
        assertEquals("Shots", d.stats[0].label); assertEquals("24", d.stats[0].home); assertEquals("18", d.stats[0].away)
        assertEquals("Shots on goal", d.stats[1].label); assertEquals("11", d.stats[1].home); assertEquals("12", d.stats[1].away)
        assertEquals("Faceoffs won", d.stats[2].label); assertEquals("33", d.stats[2].home); assertEquals("24", d.stats[2].away)
        assertEquals("Hits", d.stats[3].label); assertEquals("21", d.stats[3].home); assertEquals("19", d.stats[3].away)
        assertEquals("Penalty minutes", d.stats[4].label); assertEquals("12", d.stats[4].home); assertEquals("10", d.stats[4].away)
        assertEquals("Power play", d.stats[5].label); assertEquals("0/3", d.stats[5].home); assertEquals("1/4", d.stats[5].away)
        // Scoring: the trimmed fixture has 2 goals (1-1), the full game ended 3-1
        assertEquals(2, d.scoring.size)
        assertEquals(1, d.scoring[0].home); assertEquals(0, d.scoring[0].away)
        assertTrue(d.scoring[0].text.contains("Flanagan"))
        assertEquals(1, d.scoring[1].home); assertEquals(1, d.scoring[1].away)
        assertTrue(d.scoring[1].text.contains("Labre"))
        assertTrue(d.scoring[1].text.contains("AN"))
        // Timeline, in game order: penalty, goal, penalty, goal
        assertEquals(4, d.events.size)
        assertEquals("P1 3:09", d.events[0].time)
        assertTrue(d.events[0].text.contains("Charron")); assertFalse(d.events[0].goal)
        assertEquals("P1 6:17", d.events[1].time)
        assertTrue(d.events[1].text.contains("Flanagan")); assertTrue(d.events[1].goal)
        assertTrue(d.events[2].text.contains("Jewer")); assertFalse(d.events[2].goal)
        assertTrue(d.events[3].text.contains("Labre")); assertTrue(d.events[3].goal)
        // Lineups: the fixture has only defensemen at home, one forward away
        val home = d.home!!
        assertEquals(2, home.blocks.size)
        assertEquals("Defensemen", home.blocks[0].name)
        assertEquals(listOf("G", "A", "PTS", "+/-", "SOG", "PIM"), home.blocks[0].labels)
        assertEquals("Hughston Hurt", home.blocks[0].players[0].name)
        assertEquals("2", home.blocks[0].players[0].jersey)
        assertEquals(listOf("0", "0", "0", "-2", "0", "0"), home.blocks[0].players[0].stats)
        assertEquals("Owen Conrad", home.blocks[0].players[1].name)
        assertEquals(listOf("0", "0", "0", "-1", "0", "2"), home.blocks[0].players[1].stats)
        assertEquals("Goalies", home.blocks[1].name)
        assertEquals("Donald Hickey", home.blocks[1].players[0].name)
        assertEquals(listOf("18", "15", "0.833", "3", "57:22"), home.blocks[1].players[0].stats)
        val away = d.away!!
        assertEquals(3, away.blocks.size)
        assertEquals("Forwards", away.blocks[0].name)
        assertEquals("Egan Beveridge", away.blocks[0].players[0].name)
        assertEquals("RW", away.blocks[0].players[0].pos)
        assertEquals(listOf("1", "1", "2", "+1", "2", "0"), away.blocks[0].players[0].stats)
        assertEquals("", away.blocks[0].players[0].id) // no ESPN player page for CHL ids
        assertEquals("Defensemen", away.blocks[1].name)
        assertEquals("Bastien Michaud", away.blocks[1].players[0].name)
        assertEquals("Goalies", away.blocks[2].name)
        assertEquals("Timothy Dubuc", away.blocks[2].players[0].name)
        assertEquals("35", away.blocks[2].players[0].jersey)
        assertTrue(away.blocks[2].players[0].starter)
        assertEquals(listOf("24", "23", "0.958", "1", "60:00"), away.blocks[2].players[0].stats)
        // Goals per period, three stars, venue
        assertEquals(listOf("1", "0", "0"), d.homeLines)
        assertEquals(listOf("1", "0", "2"), d.awayLines)
        assertEquals(listOf("Timothy Dubuc (Que)", "Anthony Flanagan (Cha)", "Maddox Labre (Que)"), d.stars)
        assertEquals("Eastlink Centre", d.venue)
        assertEquals("2355", d.attendance)
    }

    @Test
    fun gamesummaryOhneJsonpGehtAuch() {
        val d = SportMatch.parseChl(gsJson, game32756)
        assertNotNull(d)
        assertEquals(2, d!!.scoring.size)
    }

    @Test
    fun gamesummaryMuellGibtNull() {
        assertNull(SportMatch.parseChl("angular.callbacks._0({\"GC\":{}})", game32756))
        assertNull(SportMatch.parseChl("kein json", game32756))
        assertNull(SportMatch.parseChl("", game32756))
    }

    @Test
    fun gamesummaryVorAnpfiffHatKeineStats() {
        val pre = """{"GC":{"Gamesummary":{"meta":{"id":"33001","started":"0","attendance":"0"},"venue":"Colisée Desjardins","goals":[],"penalties":[],"mvps":[],"goalsByPeriod":{},"home_team_lineup":{"players":[],"goalies":[]},"visitor_team_lineup":{"players":[],"goalies":[]}}}}"""
        val d = SportMatch.parseChl(pre, game32756)
        assertNotNull(d)
        assertTrue(d!!.stats.isEmpty())
        assertNull(d.home)
        assertNull(d.away)
        assertTrue(d.events.isEmpty())
        assertTrue(d.homeLines.isEmpty())
        assertEquals("Colisée Desjardins", d.venue)
        assertEquals("", d.attendance)
    }

    // ───────── CHL highlights ─────────

    @Test
    fun highlightStadtAusName() {
        assertEquals("Charlottetown", ChlHighlights.cityOf(game32756.home))
        assertEquals("Québec", ChlHighlights.cityOf(game32756.away))
        val cape = SportData.Team("Cape Breton Eagles", "Eagles", "Cap", null, "0", false)
        assertEquals("Cape Breton", ChlHighlights.cityOf(cape))
    }

    @Test
    fun highlightTitelStadtUndSpitzname() {
        // game32756: home Charlottetown Islanders 1, away Québec Remparts 3
        assertTrue(ChlHighlights.matchTitle("Charlottetown 1 Quebec 3", game32756, 1, 3))
        assertTrue(ChlHighlights.matchTitle("Quebec 3 Charlottetown 1", game32756, 1, 3))
        assertTrue(ChlHighlights.matchTitle("Islanders 1 Remparts 3", game32756, 1, 3))
        assertTrue(ChlHighlights.matchTitle("Remparts 3 Islanders 1", game32756, 1, 3))
        assertTrue(ChlHighlights.matchTitle("Highlights | Charlottetown 1 Quebec 3", game32756, 1, 3))
        assertTrue(ChlHighlights.matchTitle("Quebec 3-1 Charlottetown", game32756, 1, 3))
        assertFalse(ChlHighlights.matchTitle("Oceanic 2 Wildcats 8", game32756, 1, 3))
        assertFalse(ChlHighlights.matchTitle("Quebec 3 Charlottetown 2", game32756, 1, 3))
        assertFalse(ChlHighlights.matchTitle("Quebec 3 Halifax 1", game32756, 1, 3))
    }

    @Test
    fun highlightTitelAkzenteWerdenGefaltet() {
        val g = SportData.Game(
            "32999", "LHJMQ", "Eishockey", 1794610800L, 2, "Final",
            SportData.Team("Rimouski Océanic", "Océanic", "Rim", null, "2", false),
            SportData.Team("Moncton Wildcats", "Wildcats", "Mon", null, "8", true)
        )
        assertTrue(ChlHighlights.matchTitle("Oceanic 2 Wildcats 8", g, 2, 8))
        assertTrue(ChlHighlights.matchTitle("Wildcats 8 Oceanic 2", g, 2, 8))
        assertFalse(ChlHighlights.matchTitle("Oceanic 3 Wildcats 8", g, 2, 8))
    }

    @Test
    fun highlightMp4Extraktion() {
        val html = """<html><head><meta itemprop="contentUrl" content="https://cdn.target-video.com/live/partners/31038/sd/98765.mp4"></head><body>Video</body></html>"""
        assertEquals(
            "https://cdn.target-video.com/live/partners/31038/sd/98765.mp4",
            ChlHighlights.extractMp4(html)
        )
        assertNull(ChlHighlights.extractMp4("<html><body>kein Video hier</body></html>"))
        assertNull(ChlHighlights.extractMp4("""<meta itemprop="contentUrl" content="https://example.com/poster.jpg">"""))
    }

    @Test
    fun highlightSucheFindetVideo() {
        val html = """
            <html><body>
            <h2><a href="/lhjmq/en/video/oceanic-2-wildcats-8/">Oceanic 2 Wildcats 8</a></h2>
            <h2><a href="https://chl.ca/lhjmq/en/video/charlottetown-1-quebec-3-highlights/">Charlottetown 1 Quebec 3 | Highlights</a></h2>
            <h2><a href="/lhjmq/en/news/quebec-remparts-sign-player/">Quebec Remparts sign a player</a></h2>
            </body></html>
        """.trimIndent()
        assertEquals(
            "https://chl.ca/lhjmq/en/video/charlottetown-1-quebec-3-highlights/",
            ChlHighlights.parseSearchPage(html, game32756, 1, 3)
        )
        assertNull(ChlHighlights.parseSearchPage("<html><body><a href=\"/lhjmq/en/video/oceanic-2-wildcats-8/\">Oceanic 2 Wildcats 8</a></body></html>", game32756, 1, 3))
    }
}
