package com.ipvio.stream

/**
 * Stream extraction for the web sources (see WebSources / WebPlayer): while a source's
 * embed page plays in the WebView, its player mints the real stream URL on the fly.
 * The WebView sniffs that request (an .m3u8 / .mpd plus the headers the page sent with
 * it) and offers it here, so the title can be played in IPVIO's own player instead.
 * The URL alone is often useless — the CDN checks the referer / user agent / cookie
 * of the page that created it — so the headers travel with it and PlayerScreen
 * applies them when it opens exactly this URL.
 */
object WebExtract {
    data class Found(val url: String, val headers: Map<String, String>)

    @Volatile
    private var byUrl: Map<String, Map<String, String>> = emptyMap()

    fun put(f: Found) {
        byUrl = byUrl + (f.url to f.headers)
    }

    fun headersFor(url: String): Map<String, String>? = byUrl[url]

    /** A request URL that carries a playable stream (HLS playlist or DASH manifest). */
    fun isStream(url: String): Boolean {
        val u = url.lowercase()
        return u.contains(".m3u8") || u.contains(".mpd")
    }

    /** How likely the URL is the main playlist rather than a variant or a segment list. */
    fun score(url: String): Int {
        val u = url.lowercase()
        var s = 0
        if (u.contains(".m3u8")) s += 2
        if (u.contains(".mpd")) s += 1
        if (u.contains("master")) s += 3
        if (u.contains("playlist")) s += 2
        if (u.contains("index")) s += 1
        return s
    }

    /** The better of the two candidates; ties keep the current one (first found wins). */
    fun better(current: String?, candidate: String): String =
        if (current == null || score(candidate) > score(current)) candidate else current
}
