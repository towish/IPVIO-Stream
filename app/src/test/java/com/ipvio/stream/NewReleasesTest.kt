package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** TMDB discover parsing for the "new releases" rails. Fixture = real discover shape, trimmed. */
class NewReleasesTest {
    private val netflix = NewReleases.Provider("netflix", "Netflix", 8)

    private val discoverMovie = """
        { "page": 1, "results": [
          { "id": 123456, "title": "Le Film Test", "original_title": "Le Film Test", "release_date": "2026-11-10",
            "poster_path": "/abc.jpg", "backdrop_path": "/def.jpg", "overview": "Un film.", "vote_average": 7.8,
            "vote_count": 1200, "popularity": 320.5, "genre_ids": [28], "original_language": "fr" },
          { "id": 999, "title": "Sans Affiche", "release_date": "2026-11-12", "poster_path": null,
            "popularity": 500.0, "vote_average": 9.0, "vote_count": 5 }
        ], "total_results": 2 }
    """.trimIndent()

    private val discoverTv = """
        { "page": 1, "results": [
          { "id": 789, "name": "La Série Test", "original_name": "La Série Test", "first_air_date": "2026-11-01",
            "poster_path": "/ghi.jpg", "overview": "Une série.", "vote_average": 8.1, "vote_count": 300,
            "popularity": 210.0, "genre_ids": [18], "original_language": "en", "origin_country": ["US"] }
        ], "total_results": 1 }
    """.trimIndent()

    @Test
    fun parsePageFilme() {
        val list = NewReleases.parsePage(discoverMovie, false, netflix)
        assertEquals(1, list.size) // "Sans Affiche" has no poster
        val (pop, item) = list[0]
        assertEquals(320.5f, pop, 0.01f)
        assertEquals("nr:netflix:movie:123456", item.id)
        assertEquals(Kind.MOVIE, item.kind)
        assertEquals("Le Film Test", item.title)
        assertEquals(2026, item.year)
        assertEquals("", item.url)
        assertEquals("Nouveautés", item.group)
        assertTrue(NewReleases.isNewRelease(item))
    }

    @Test
    fun parsePageSerie() {
        val list = NewReleases.parsePage(discoverTv, true, netflix)
        assertEquals(1, list.size)
        val (_, item) = list[0]
        assertEquals("nr:netflix:tv:789", item.id)
        assertEquals(Kind.SERIES, item.kind)
        assertEquals("La Série Test", item.title)
        assertEquals(2026, item.year)
    }

    @Test
    fun parsePageMuellGibtLeer() {
        assertTrue(NewReleases.parsePage("kein json", false, netflix).isEmpty())
        assertTrue(NewReleases.parsePage("{}", false, netflix).isEmpty())
        assertTrue(NewReleases.parsePage("""{"results":[]}""", true, netflix).isEmpty())
    }

    @Test
    fun isNewReleaseErkenntNurSynthetik() {
        assertTrue(NewReleases.isNewRelease(Item("nr:netflix:movie:1", Kind.MOVIE, "X", 2026, "", null, "Nouveautés")))
        assertFalse(NewReleases.isNewRelease(Item("abc123", Kind.MOVIE, "X", 2026, "http://x", null, "Films")))
    }
}
