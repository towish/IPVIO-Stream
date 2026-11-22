package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** TMDB search/multi parsing for the full TMDB search in the search tab. Fixture = real shape, trimmed. */
class TmdbSearchTest {
    private val multi = """
        { "page": 1, "results": [
          { "id": 2190, "media_type": "tv", "name": "South Park", "original_name": "South Park",
            "first_air_date": "1997-08-13", "poster_path": "/xJnbM2fI2E3wOvtS8GGpMJAJRqy.jpg",
            "popularity": 210.4, "vote_average": 8.4 },
          { "id": 1213, "media_type": "movie", "title": "South Park: Bigger, Longer & Uncut",
            "release_date": "1999-06-30", "poster_path": "/mmq0u3WNmOF5kDzQ6jJzC2YrYgS.jpg", "popularity": 95.2 },
          { "id": 555, "media_type": "person", "name": "Trey Parker", "popularity": 60.0 },
          { "id": 777, "media_type": "tv", "name": "Sans Affiche", "first_air_date": "2020-01-01",
            "poster_path": null, "popularity": 400.0 },
          { "id": 888, "media_type": "movie", "title": "", "release_date": "2021-05-05",
            "poster_path": "/x.jpg", "popularity": 300.0 }
        ], "total_results": 5 }
    """.trimIndent()

    @Test
    fun parseMultiNurFilmeUndSerienMitAffiche() {
        val list = TmdbSearch.parseMulti(multi)
        // Person, poster-less and title-less entries are dropped
        assertEquals(2, list.size)
        // Sorted by popularity: the series (210) before the movie (95)
        val tv = list[0]
        assertEquals(Kind.SERIES, tv.kind)
        assertEquals("South Park", tv.title)
        assertEquals(1997, tv.year)
        assertEquals("nr:tmdb:tv:2190", tv.id)
        assertEquals(2190, WebSources.tmdbIdOf(tv))
        assertTrue(tv.logo!!.endsWith("/xJnbM2fI2E3wOvtS8GGpMJAJRqy.jpg"))
        val movie = list[1]
        assertEquals(Kind.MOVIE, movie.kind)
        assertEquals("nr:tmdb:movie:1213", movie.id)
        assertEquals(1999, movie.year)
        assertEquals(1213, WebSources.tmdbIdOf(movie))
    }

    @Test
    fun parseMultiMuellGibtLeer() {
        assertTrue(TmdbSearch.parseMulti("not json").isEmpty())
        assertTrue(TmdbSearch.parseMulti("{}").isEmpty())
        assertTrue(TmdbSearch.parseMulti("""{ "results": [] }""").isEmpty())
    }
}
