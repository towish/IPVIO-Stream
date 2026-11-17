package com.ipvio.stream

import okhttp3.Request
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale

/**
 * CHL game highlights: direct MP4 videos (Brid/TargetVideo) published on chl.ca
 * a few hours after the game. There is no official match → video link (the
 * scorebar VideoUrl is always empty, wp-json is blocked), so the video is
 * resolved through the WordPress search of the league site, by team names and
 * score, then the MP4 is extracted from the video post page.
 * Best effort only: any failure returns null, never throws.
 */
object ChlHighlights {
    /** How long a failed lookup is remembered (a video is only posted hours after the game). */
    private const val FAIL_TTL_MS = 6L * 3600_000
    private data class Entry(val url: String?, val at: Long)
    private val cache = HashMap<String, Entry>()

    private val MP4_RE = Regex("contentUrl\"\\s+content=\"([^\"]+\\.mp4)\"")
    private val A_RE = Regex("<a\\b([^>]*)>(.*?)</a>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val HREF_RE = Regex("href=\"((?:https://chl\\.ca)?/(?:lhjmq|ohl|whl)/en/video/[^\"]+)\"", RegexOption.IGNORE_CASE)
    private val TITLE_ATTR_RE = Regex("title=\"([^\"]+)\"", RegexOption.IGNORE_CASE)

    /** Resolve the highlight MP4 of a finished CHL game, or null. */
    fun findHighlight(league: SportData.League, game: SportData.Game): String? {
        if (game.state != 2) return null
        val homeGoals = game.home.score?.toIntOrNull() ?: return null
        val awayGoals = game.away.score?.toIntOrNull() ?: return null
        val key = league.path + "|" + game.id
        val now = System.currentTimeMillis()
        synchronized(cache) {
            val hit = cache[key]
            if (hit != null && (hit.url != null || now - hit.at < FAIL_TTL_MS)) return hit.url
        }
        val base = baseUrl(league) ?: return null
        val url = runCatching { search(base, game, homeGoals, awayGoals) }.getOrNull()
        synchronized(cache) { if (cache.size > 200) cache.clear(); cache[key] = Entry(url, now) }
        return url
    }

    internal fun baseUrl(league: SportData.League): String? = when (league.path.removePrefix("chl:")) {
        "lhjmq" -> "https://chl.ca/lhjmq/en/"
        "ohl" -> "https://chl.ca/ohl/en/"
        "whl" -> "https://chl.ca/whl/en/"
        else -> null
    }

    private fun search(base: String, game: SportData.Game, homeGoals: Int, awayGoals: Int): String? {
        // City names first (titles often use them), nicknames second — both spellings exist.
        val queries = listOf(
            "${cityOf(game.home)} ${cityOf(game.away)}",
            "${game.home.short} ${game.away.short}"
        )
        for (q in queries) {
            val html = get(base + "?s=" + URLEncoder.encode(q, "UTF-8")) ?: continue
            val post = parseSearchPage(html, game, homeGoals, awayGoals) ?: continue
            val postHtml = get(post) ?: continue
            val mp4 = extractMp4(postHtml) ?: continue
            return mp4
        }
        return null
    }

    private fun get(url: String): String? {
        val req = Request.Builder().url(url).header("User-Agent", "IPVIO-Stream").build()
        Net.withTimeouts(6, 10).newCall(req).execute().use { r ->
            if (!r.isSuccessful) return null
            return r.body?.string()
        }
    }

    /** City = team name without the nickname suffix ("Québec Remparts" → "Québec"). */
    internal fun cityOf(t: SportData.Team): String {
        val n = t.name.trim()
        return if (t.short.isNotBlank() && n.endsWith(t.short)) n.removeSuffix(t.short).trim() else n
    }

    /** Accent- and case-folded, for matching titles that spell names without accents. */
    internal fun fold(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase(Locale.US)

    /** A word sequence as a regex, tolerating any whitespace between the words. */
    private fun tokenRe(s: String): String =
        fold(s).split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString("\\s+") { Regex.escape(it) }

    /**
     * True when the title names both teams (city or nickname each) and the score,
     * in either order ("Quebec 3 Charlottetown 1" or "Charlottetown 1 Quebec 3").
     */
    internal fun matchTitle(title: String, game: SportData.Game, homeGoals: Int, awayGoals: Int): Boolean {
        val t = fold(title)
        val home = listOf(game.home.short, cityOf(game.home)).map { tokenRe(it) }.filter { it.isNotEmpty() }.distinct()
        val away = listOf(game.away.short, cityOf(game.away)).map { tokenRe(it) }.filter { it.isNotEmpty() }.distinct()
        for (h in home) for (a in away) {
            if (scorePair(t, h, homeGoals, a, awayGoals)) return true
            if (scorePair(t, a, awayGoals, h, homeGoals)) return true
        }
        return false
    }

    /** "<first> <goalsFirst> … <second> <goalsSecond>" inside the folded title. */
    private fun scorePair(t: String, first: String, goalsFirst: Int, second: String, goalsSecond: Int): Boolean {
        val re = Regex("$first\\s+$goalsFirst\\b.*?$second\\s+$goalsSecond\\b")
        if (re.containsMatchIn(t)) return true
        // Compact score: "Quebec 6-2 Halifax" (second team after the combined score)
        val compact = Regex("$first\\s+$goalsFirst\\s*[-–:]\\s*$goalsSecond\\b.*?$second\\b")
        return compact.containsMatchIn(t)
    }

    /** First /en/video/ link of the search page whose title matches the game, or null. */
    internal fun parseSearchPage(html: String, game: SportData.Game, homeGoals: Int, awayGoals: Int): String? {
        val seen = HashSet<String>()
        for (m in A_RE.findAll(html)) {
            val href = HREF_RE.find(m.groupValues[1])?.groupValues?.get(1) ?: continue
            if (!seen.add(href)) continue
            val text = stripTags(m.groupValues[2])
                .ifBlank { TITLE_ATTR_RE.find(m.groupValues[1])?.groupValues?.get(1).orEmpty() }
            val title = unescape(text)
            if (title.isNotBlank() && matchTitle(title, game, homeGoals, awayGoals)) {
                return if (href.startsWith("/")) "https://chl.ca$href" else href
            }
        }
        return null
    }

    private fun stripTags(s: String): String = s.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()

    private fun unescape(s: String): String = s
        .replace("&amp;", "&").replace("&quot;", "\"").replace("&#039;", "'").replace("&apos;", "'")
        .replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ")

    /** The direct MP4 of a video post page, or null. */
    internal fun extractMp4(html: String): String? = MP4_RE.find(html)?.groupValues?.get(1)
}
