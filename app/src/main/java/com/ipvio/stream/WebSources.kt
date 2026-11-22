package com.ipvio.stream

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

/**
 * Web sources in the style of the Streambert desktop app: public embed aggregators that
 * serve a player page per TMDB id (movie, or series + season + episode). Streambert itself
 * is an Electron desktop app and cannot run on a TV; what transfers is its source list —
 * the same URL templates, opened here in the app's own web player (WebPlayerScreen).
 * The list mirrors Streambert's PLAYER_SOURCES; Frembed (French) comes first for this user.
 * These hosters come and go: a dead source fails in the player and the user picks another.
 */
object WebSources {
    class Source(
        val id: String, val label: String, val note: String?,
        val movie: (Int) -> String, val tv: (Int, Int, Int) -> String
    )

    val SOURCES = listOf(
        Source("frembed", "Frembed", "Français",
            { id -> "https://frembed.uno/embed/movie/$id" },
            { id, s, e -> "https://frembed.uno/embed/serie/$id?sa=$s&epi=$e" }),
        Source("vidsrc", "VidSrc", null,
            { id -> "https://vsembed.su/embed/movie/$id" },
            { id, s, e -> "https://vsembed.su/embed/tv/$id/$s/$e" }),
        Source("vidfast", "Vidfast", null,
            { id -> "https://vidfast.vc/movie/$id" },
            { id, s, e -> "https://vidfast.vc/tv/$id/$s/$e" }),
        Source("vidnest", "Vidnest", "Anime ✓",
            { id -> "https://vidnest.fun/movie/$id" },
            { id, s, e -> "https://vidnest.fun/tv/$id/$s/$e" }),
        Source("vidrift", "Vidrift", "Multi-langue",
            { id -> "https://embed.vidrift.net/embed/movie/$id" },
            { id, s, e -> "https://embed.vidrift.net/embed/tv/$id/$s/$e" }),
        Source("aether", "Aether", "Multi-langue",
            { id -> "https://aether.ist/media/tmdb-movie-$id" },
            { id, s, e -> "https://aether.ist/media/tmdb-tv-$id/$s/$e" }),
        Source("vidlove", "Vidlove", null,
            { id -> "https://player.vidlove.cc/embed/movie/$id" },
            { id, s, e -> "https://player.vidlove.cc/embed/tv/$id/$s/$e" })
    )

    /** What a source picker is opened for: a movie (season 0), or one episode of a series. */
    class Target(val title: String, val tmdbId: Int, val season: Int, val episode: Int) {
        fun url(s: Source): String =
            if (season > 0) s.tv(tmdbId, season, episode) else s.movie(tmdbId)

        fun label(): String =
            if (season > 0) "$title · S$season E$episode" else title
    }

    /** TMDB id of a new-release item ("nr:<provider>:movie|tv:<id>"); 0 for other items. */
    fun tmdbIdOf(item: Item): Int =
        if (NewReleases.isNewRelease(item)) item.id.split(":").getOrNull(3)?.toIntOrNull() ?: 0 else 0
}

/** Source picker: one focusable row per web source; picking one opens the web player. */
@Composable
fun WebSourcePicker(target: WebSources.Target, onPick: (String) -> Unit, onClose: () -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(80); runCatching { first.requestFocus() } }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.width(720.dp).clip(RoundedCornerShape(DS.RadiusL)).background(Tok.GlassStrong).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Quelle wählen", color = Accent2, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(raw = true, text = target.label(), color = Muted, fontSize = 14.sp)
            WebSources.SOURCES.forEachIndexed { i, s ->
                var f by remember { mutableStateOf(false) }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(DS.RadiusM))
                        .background(if (f) Color(0x1FFFFFFF) else Color(0x08FFFFFF))
                        .border(1.dp, if (f) Accent2 else Color.Transparent, RoundedCornerShape(DS.RadiusM))
                        .onFocusChanged { f = it.isFocused }
                        .then(if (i == 0) Modifier.focusRequester(first) else Modifier)
                        .clickable { onPick(target.url(s)) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(raw = true, text = s.label, color = Color.White, fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    if (s.note != null) Text(raw = true, text = s.note, color = Accent2, fontSize = 13.sp)
                }
            }
        }
    }
}
