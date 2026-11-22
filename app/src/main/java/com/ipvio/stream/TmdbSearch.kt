package com.ipvio.stream

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Full TMDB search, Streambert-style: the search tab also asks TMDB directly, so a title
 * the IPTV provider does not carry is still findable. Hits become synthetic items with the
 * "nr:tmdb:…" id scheme (info only, like the new-release rails): the detail page shows the
 * TMDB info and the web sources; for a series the episode is picked in [TmdbEpisodePicker].
 * Needs a TMDB key (MetaRepo.enabled); without one the section stays empty.
 */
object TmdbSearch {
    private const val MAX_RESULTS = 20

    fun enabled(): Boolean = MetaRepo.enabled()

    suspend fun search(query: String): List<Item> = withContext(Dispatchers.IO) {
        if (!enabled() || query.length < 2) return@withContext emptyList()
        val q = URLEncoder.encode(query, "UTF-8")
        val url = "https://api.themoviedb.org/3/search/multi?api_key=${Config.TMDB_KEY}" +
            "&language=${Lang.tmdb()}&query=$q&page=1&include_adult=false"
        try {
            val req = Request.Builder().url(url).header("User-Agent", "IPVIO-Stream").build()
            Net.withTimeouts(8, 12).newCall(req).execute().use { r ->
                if (!r.isSuccessful) emptyList() else parseMulti(r.body?.string().orEmpty())
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** One search/multi page: movies and series with a poster, most popular first. */
    internal fun parseMulti(json: String): List<Item> {
        val results = try {
            JSONObject(json).optJSONArray("results")
        } catch (_: Exception) {
            null
        } ?: return emptyList()
        val scored = ArrayList<Pair<Float, Item>>()
        for (i in 0 until results.length()) {
            val o = results.optJSONObject(i) ?: continue
            val isTv = when (o.optString("media_type")) {
                "tv" -> true
                "movie" -> false
                else -> continue // persons and anything else are not playable titles
            }
            val poster = o.optString("poster_path")
            if (poster.isBlank()) continue
            val title = o.optString(if (isTv) "name" else "title").trim()
            if (title.isBlank()) continue
            val id = o.optInt("id")
            if (id == 0) continue
            val date = o.optString(if (isTv) "first_air_date" else "release_date")
            val item = Item(
                "nr:tmdb:${if (isTv) "tv" else "movie"}:$id",
                if (isTv) Kind.SERIES else Kind.MOVIE,
                title, date.take(4).toIntOrNull(), "",
                "https://image.tmdb.org/t/p/w342$poster", "TMDB"
            )
            scored.add(o.optDouble("popularity").toFloat() to item)
        }
        return scored.sortedByDescending { it.first }.take(MAX_RESULTS).map { it.second }
    }
}

/**
 * Season/episode picker for a TMDB series without playlist episodes: seasons come from
 * TMDB (MetaRepo.tvSeasons), episode names from MetaRepo.season; if the episode list
 * cannot be loaded, the season's episode count still gives plain numbered rows.
 * Season 0 (specials) is skipped: the web-source URL scheme has no place for it.
 * Picking an episode hands a WebSources.Target back, the caller opens the source picker.
 */
@Composable
fun TmdbEpisodePicker(title: String, tmdbId: Int, onPick: (WebSources.Target) -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var loaded by remember(tmdbId) { mutableStateOf(false) }
    val seasons by produceState(emptyList<EpisodeLogic.TmdbSeason>(), tmdbId) {
        value = MetaRepo.tvSeasons(ctx, tmdbId).filter { it.num >= 1 }
        loaded = true
    }
    var season by remember(tmdbId) { mutableIntStateOf(-1) }
    LaunchedEffect(seasons) { if (season < 0 && seasons.isNotEmpty()) season = seasons.first().num }
    val episodes by produceState(emptyList<EpMeta>(), tmdbId, season, seasons) {
        if (season < 1) return@produceState
        val metas = MetaRepo.season(tmdbId, season, null)
        value = if (metas.isNotEmpty()) metas else {
            val count = seasons.firstOrNull { it.num == season }?.count ?: 0
            (1..count).map { EpMeta(it, null, null, null, null, null, null, null) }
        }
    }
    val firstChip = remember { FocusRequester() }
    LaunchedEffect(seasons) { if (seasons.isNotEmpty()) { delay(80); runCatching { firstChip.requestFocus() } } }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.width(760.dp).clip(RoundedCornerShape(DS.RadiusL)).background(Tok.GlassStrong).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Web-Quellen", color = Accent2, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(raw = true, text = title, color = Muted, fontSize = 14.sp)
            if (loaded && seasons.isEmpty()) {
                Text("Keine Treffer", color = Muted, fontSize = 15.sp)
            }
            if (seasons.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    seasons.forEachIndexed { i, s ->
                        var f by remember { mutableStateOf(false) }
                        val sel = s.num == season
                        Row(
                            Modifier.clip(RoundedCornerShape(50))
                                .background(if (f) DS.FocusFill else if (sel) DS.Overlay3 else Color(0x08FFFFFF))
                                .border(1.dp, if (f) DS.FocusFill else if (sel) Accent2 else Color.Transparent, RoundedCornerShape(50))
                                .onFocusChanged { f = it.isFocused }
                                .then(if (i == 0) Modifier.focusRequester(firstChip) else Modifier)
                                .clickable { season = s.num }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                raw = true, text = "S${s.num}", fontSize = 15.sp,
                                color = if (f) DS.FocusInk else Color.White,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
            if (season >= 1) {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(episodes, key = { it.num }) { ep ->
                        var f by remember { mutableStateOf(false) }
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(DS.RadiusM))
                                .background(if (f) Color(0x1FFFFFFF) else Color(0x08FFFFFF))
                                .border(1.dp, if (f) Accent2 else Color.Transparent, RoundedCornerShape(DS.RadiusM))
                                .onFocusChanged { f = it.isFocused }
                                .clickable { onPick(WebSources.Target(title, tmdbId, season, ep.num)) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(raw = true, text = "E${ep.num}", color = Accent2, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(
                                raw = true, text = ep.name?.takeIf { it.isNotBlank() } ?: "",
                                color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
