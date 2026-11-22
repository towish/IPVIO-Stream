package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Web sources (Streambert-style embed aggregators): URL templates keyed by TMDB id. */
class WebSourcesTest {
    private fun src(id: String) = WebSources.SOURCES.first { it.id == id }

    @Test
    fun frembedFilmUndSerie() {
        assertEquals("https://frembed.uno/embed/movie/550", src("frembed").movie(550))
        assertEquals("https://frembed.uno/embed/serie/550?sa=2&epi=3", src("frembed").tv(550, 2, 3))
    }

    @Test
    fun zielBautDieRichtigeUrl() {
        val film = WebSources.Target("Film", 550, 0, 0)
        assertEquals("https://vsembed.su/embed/movie/550", film.url(src("vidsrc")))
        val serie = WebSources.Target("Serie", 550, 1, 2)
        assertEquals("https://vidfast.vc/tv/550/1/2", serie.url(src("vidfast")))
        assertTrue(serie.label().contains("S1 E2"))
    }

    @Test
    fun extraktionErkenntUndWaehltStreams() {
        assertTrue(WebExtract.isStream("https://cdn.example.com/hls/master.m3u8?tok=1"))
        assertTrue(WebExtract.isStream("https://cdn.example.com/dash/manifest.mpd"))
        assertFalse(WebExtract.isStream("https://cdn.example.com/seg-12.ts"))
        val variant = "https://cdn.example.com/hls/720p/index.m3u8"
        val master = "https://cdn.example.com/hls/master.m3u8"
        assertEquals(master, WebExtract.better(variant, master))
        assertEquals(variant, WebExtract.better(variant, "https://cdn.example.com/hls/360p/index.m3u8"))
        assertEquals(variant, WebExtract.better(null, variant))
    }

    @Test
    fun tmdbIdAusNeuheitenItem() {
        val item = Item("nr:netflix:movie:550", Kind.MOVIE, "X", null, "", null, "Nouveautés")
        assertEquals(550, WebSources.tmdbIdOf(item))
        val normal = Item("12345", Kind.MOVIE, "Y", null, "", null, "Films")
        assertEquals(0, WebSources.tmdbIdOf(normal))
    }
}
