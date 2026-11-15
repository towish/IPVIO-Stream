package com.ipvio.stream

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * "Watch on X": opens a new-release title directly in its streaming app.
 * The title is resolved on JustWatch (public GraphQL, no key) to a deep link
 * (netflix.com/title/<id>, app.primevideo.com/detail?gti=<asin>); when no link comes back,
 * the app itself is opened. Nothing happens when the app is not installed: no browser, no error.
 * Crave has no known Fire TV package id, so it has no button.
 * JustWatch is unofficial and can change without notice: every step is best effort and guarded,
 * a failure always falls through to opening the app (or doing nothing).
 */
object StreamLaunch {
    /** A service a title can be opened in: the TMDB provider id and the Fire TV package. */
    class Service(val providerId: String, val name: String, val pkg: String, val host: String)

    private val SERVICES = listOf(
        Service("netflix", "Netflix", "com.netflix.ninja", "netflix.com"),
        Service("prime", "Prime Video", "com.amazon.amazonvideo.livingroom", "primevideo.com"),
        Service("disney", "Disney+", "com.disney.disneyplus", "disneyplus.com"),
        Service("apple", "Apple TV+", "com.apple.atve.amazon.store", "tv.apple.com")
    )

    /** The service for a new-release item, or null (Crave, or not a new-release item). */
    fun serviceFor(item: Item): Service? {
        if (!NewReleases.isNewRelease(item)) return null
        val pid = item.id.split(":").getOrNull(1) ?: return null
        return SERVICES.firstOrNull { it.providerId == pid }
    }

    /** TMDB id carried by a synthetic new-release item id ("nr:<provider>:movie|tv:<id>"). */
    private fun tmdbIdOf(item: Item): Int = item.id.split(":").getOrNull(3)?.toIntOrNull() ?: 0

    /**
     * Opens the title: deep link when JustWatch resolves one, the app itself otherwise.
     * The lookup runs on IO; the launch runs on the calling thread.
     */
    suspend fun open(ctx: Context, item: Item) {
        val svc = serviceFor(item) ?: return
        val url = resolveDeepLink(item, svc)
        if (url != null) {
            try {
                val view = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    setPackage(svc.pkg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(view)
                return
            } catch (_: Exception) {
                // The app cannot open this link: fall through to opening the app itself
            }
        }
        try {
            val launch = ctx.packageManager.getLaunchIntentForPackage(svc.pkg) ?: return
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(launch)
        } catch (_: Exception) {
            // Not installed or cannot start: stay silent, the user is already in the app
        }
    }

    /** Deep link for the title from JustWatch (GraphQL, country CA, subscription offers only). Null on any failure. */
    private suspend fun resolveDeepLink(item: Item, svc: Service): String? = withContext(Dispatchers.IO) {
        try {
            val tmdbId = tmdbIdOf(item)
            if (tmdbId == 0) return@withContext null
            // Variables are inlined (no "$" in the query) so the Kotlin string needs no escaping
            val query = "query GetSearchTitles { " +
                "searchTitles(searchTitles: {searchQuery: ${JSONObject.quote(item.title)}}, country: CA, language: en, first: 5) { " +
                "edges { node { externalIds { tmdbId } content(country: CA, language: en) { title } " +
                "offers(country: CA, platform: WEB, filter: {monetizationTypes: [FLATRATE]}) { monetizationType standardWebURL package { technicalName } } } } } }"
            val bodyJson = JSONObject().apply {
                put("operationName", "GetSearchTitles")
                put("query", query)
            }.toString()
            val req = Request.Builder().url("https://apis.justwatch.com/graphql")
                .post(bodyJson.toRequestBody("application/json".toMediaType()))
                .header("User-Agent", "IPVIO-Stream")
                .build()
            Net.withTimeouts(8, 8).newCall(req).execute().use { r ->
                if (!r.isSuccessful) return@withContext null
                val root = JSONObject(r.body?.string().orEmpty())
                val edges = root.optJSONObject("data")?.optJSONObject("searchTitles")?.optJSONArray("edges")
                    ?: return@withContext null
                var fallback: String? = null
                for (i in 0 until edges.length()) {
                    val node = edges.optJSONObject(i)?.optJSONObject("node") ?: continue
                    val nodeTmdb = node.optJSONObject("externalIds")?.optInt("tmdbId") ?: 0
                    val title = node.optJSONObject("content")?.optString("title").orEmpty()
                    val url = offerUrl(node, svc) ?: continue
                    if (nodeTmdb != 0 && nodeTmdb == tmdbId) return@withContext url
                    if (fallback == null && title.isNotBlank() && title.equals(item.title, ignoreCase = true)) fallback = url
                }
                fallback
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Subscription deep link of this service for the title, matched by host (no guessing of JustWatch package names). */
    private fun offerUrl(node: JSONObject, svc: Service): String? {
        val offers = node.optJSONArray("offers") ?: return null
        for (i in 0 until offers.length()) {
            val o = offers.optJSONObject(i) ?: continue
            if (o.optString("monetizationType") != "FLATRATE") continue
            val url = o.optString("standardWebURL").takeIf { it.startsWith("http") } ?: continue
            if (svc.host in url) return url
        }
        return null
    }
}
