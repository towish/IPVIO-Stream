package com.ipvio.stream

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * New releases on the streaming services, from TMDB discover (watch_region=CA).
 * One rail per service on the home screen; a title opens its TMDB detail (info only — the title
 * is not in the IPTV playlist, so there is nothing to play). Needs a TMDB key (own key in the
 * settings, or the key baked into the build); without a key the section stays empty.
 * Provider ids are TMDB's (stable for the big services, but JustWatch reorganises them now and
 * then — if a rail comes up empty, check /3/watch/providers/movie?watch_region=CA).
 */
object NewReleases {
    class Provider(val id: String, val name: String, val tmdbId: Int)

    val PROVIDERS = listOf(
        Provider("netflix", "Netflix", 8),
        Provider("prime", "Prime Video", 9),
        Provider("crave", "Crave", 230),
        Provider("disney", "Disney+", 337),
        Provider("apple", "Apple TV+", 350)
    )

    private const val CACHE_MS = 6 * 60 * 60 * 1000L
    private const val MAX_PER_PROVIDER = 12
    private const val MOVIE_DAYS = 45
    private const val TV_DAYS = 30

    private val cache = HashMap<String, Pair<Long, List<Item>>>()

    fun enabled(): Boolean = MetaRepo.enabled()

    /** Synthetic items from the "new releases" rails: info only, no playable stream. */
    fun isNewRelease(item: Item): Boolean = item.id.startsWith("nr:")

    suspend fun fetchAll(): Map<String, List<Item>> = withContext(Dispatchers.IO) {
        if (!enabled()) return@withContext emptyMap()
        coroutineScope {
            PROVIDERS.map { p -> async { p.id to fetchProvider(p) } }.awaitAll().toMap()
        }
    }

    private suspend fun fetchProvider(p: Provider): List<Item> {
        val now = System.currentTimeMillis()
        synchronized(cache) {
            val hit = cache[p.id]
            if (hit != null && now - hit.first < CACHE_MS) return hit.second
        }
        val stale = synchronized(cache) { cache[p.id]?.second }.orEmpty()
        val movies = getPage(discoverUrl("movie", p.tmdbId, MOVIE_DAYS, "primary_release_date"), false, p)
        val tv = getPage(discoverUrl("tv", p.tmdbId, TV_DAYS, "air_date"), true, p)
        if (movies == null && tv == null) return stale
        val out = ((movies ?: emptyList()) + (tv ?: emptyList()))
            .sortedByDescending { it.first }.take(MAX_PER_PROVIDER).map { it.second }
        synchronized(cache) { cache[p.id] = now to out }
        return out
    }

    private fun discoverUrl(type: String, providerId: Int, daysBack: Int, dateField: String): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val to = Calendar.getInstance()
        val from = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysBack) }
        return "https://api.themoviedb.org/3/discover/$type?api_key=${Config.TMDB_KEY}&language=${Lang.tmdb()}" +
            "&watch_region=CA&with_watch_providers=$providerId&with_watch_monetization_types=flatrate" +
            "&sort_by=popularity.desc&page=1&$dateField.gte=${fmt.format(from.time)}&$dateField.lte=${fmt.format(to.time)}"
    }

    /** One discover page: (popularity, item) per result with a poster; null when the call fails. */
    internal fun parsePage(json: String, isTv: Boolean, provider: Provider): List<Pair<Float, Item>> {
        val out = ArrayList<Pair<Float, Item>>()
        val results = try {
            JSONObject(json).optJSONArray("results")
        } catch (_: Exception) {
            null
        } ?: return out
        for (i in 0 until results.length()) {
            val o = results.optJSONObject(i) ?: continue
            // No poster, no card: a bare coloured box would look broken in the rail
            if (o.optString("poster_path").isBlank()) continue
            val title = o.optString(if (isTv) "name" else "title").trim()
            if (title.isBlank()) continue
            val date = o.optString(if (isTv) "first_air_date" else "release_date")
            val id = o.optInt("id")
            if (id == 0) continue
            val item = Item(
                "nr:${provider.id}:${if (isTv) "tv" else "movie"}:$id",
                if (isTv) Kind.SERIES else Kind.MOVIE,
                title, date.take(4).toIntOrNull(), "", null, "Nouveautés"
            )
            out.add(o.optDouble("popularity").toFloat() to item)
        }
        return out
    }

    private suspend fun getPage(url: String, isTv: Boolean, provider: Provider): List<Pair<Float, Item>>? =
        withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder().url(url).header("User-Agent", "IPVIO-Stream").build()
                Net.withTimeouts(8, 12).newCall(req).execute().use { r ->
                    if (!r.isSuccessful) return@withContext null
                    parsePage(r.body?.string().orEmpty(), isTv, provider)
                }
            } catch (_: Exception) {
                null
            }
        }
}
