package com.ipvio.stream

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.using
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

/*
 * Round 17b – "Cinematic Remake" of the home screen (design "IPVIO Cinematic Remake", picture 1):
 *  - The whole screen shows the scene image of the selected title (soft crossfade, slow zoom).
 *  - Top left are title, ratings, year, genre, length and description of this title (billboard).
 *  - The selected row stays in the lower half (PivotSpec), rows above disappear.
 *  - In the selected row the other cards step back (darkened), the selected one grows.
 */

/** Last selected movie/series (set by the cards); the home screen shows it in the background. */
object Billboard {
    var focused by mutableStateOf<Item?>(null)
    /** true while the focus is on a title card (not in the big picture, on the tiles or in the sidebar). */
    var cardFocus by mutableStateOf(false)
    /** Position of the selected card (to compute the top edge of its row, see hiding in HomeScreen). */
    var focusCo: LayoutCoordinates? = null
    /** true while the background with the info at the top is really shown (then the big picture pauses). */
    var active by mutableStateOf(false)
    /** T-032: dominant color of the shown picture (ARGB), or null; tints the background ("ambient color"). */
    var ambient by mutableStateOf<Int?>(null)
    fun mark(item: Item) {
        if (item.kind == Kind.LIVE) return
        cardFocus = true
        if (focused?.id != item.id) focused = item
    }
}

/** Like onFocusChanged for title cards: reports the focus, marks the title for the background and remembers the card's position. */
fun Modifier.billboardFocus(item: Item, onFocus: (Boolean) -> Unit): Modifier = composed {
    val holder = remember { arrayOfNulls<LayoutCoordinates>(1) }
    this.onGloballyPositioned { holder[0] = it }.onFocusChanged { s ->
        onFocus(s.isFocused)
        if (s.isFocused) { Billboard.mark(item); Billboard.focusCo = holder[0] }
    }
}

/** true while the row containing a card has the focus (the other cards are darkened). */
val LocalRowFocus = compositionLocalOf { false }

/**
 * Keeps the selected element of a list at a fixed height ([pivot] = fraction of the height where the top edge sits).
 * Very large elements (the big picture at the top) are only brought into view as far as necessary, as before.
 */
@OptIn(ExperimentalFoundationApi::class)
class PivotSpec(private val pivot: Float, private val list: () -> androidx.compose.foundation.lazy.LazyListState) : BringIntoViewSpec {
    @Suppress("OVERRIDE_DEPRECATION")
    override val scrollAnimationSpec = tween<Float>(if (Look.motion == 0) 1 else 380, easing = FastOutSlowInEasing)
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        if (size >= containerSize * 0.45f) return SmoothSpec.calculateScrollDistance(offset, size, containerSize)
        val d = offset - containerSize * pivot
        // 17c: At the start/end of the list it cannot go further – then ask for nothing more (otherwise the list keeps trying endlessly,
        // which keeps the Fire TV busy permanently). Ignore small remainders.
        val st = list()
        if (d < 0f && !st.canScrollBackward) return 0f
        if (d > 0f && !st.canScrollForward) return 0f
        return if (kotlin.math.abs(d) < 2f) 0f else d
    }
}

/** Full-screen background with the scene image of the title; without an image a gradient from the title color. */
@Composable
fun BillboardBackdrop(item: Item?, modifier: Modifier = Modifier) {
    // T-032: ambient color. The background takes a little of the picture's dominant color and changes softly with it.
    val amb = Billboard.ambient
    val target = if (amb == null) Bg else androidx.compose.ui.graphics.lerp(Bg, Color(amb), 0.22f)
    val tint by androidx.compose.animation.animateColorAsState(target, tween(if (Look.motion == 0) 0 else 700), label = "ambient")
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Box(modifier.background(tint)) {
        // 17c: calm picture (no constant zoom), medium image size (w780) instead of full resolution – saves memory and computing time
        // T-030: 300 ms instead of 600 ms: two large decoded pictures are on screen for half the time
        Crossfade(item, animationSpec = tween(if (Look.motion == 0) 0 else 300), label = "bb") { it0 ->
            if (it0 != null) {
                val m = rememberMeta(it0)
                val url = m?.backdrop?.let { b -> cardBackdrop(b) } ?: m?.poster?.let { p -> cardPoster(p) }
                if (url != null) AsyncImage(
                    url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopEnd,
                    onSuccess = { st ->
                        // Read the color off the main thread from a tiny copy of the picture
                        val d = st.result.drawable
                        scope.launch { Billboard.ambient = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { AmbientColor.of(d) } }
                    }
                )
                else ArtPlaceholder(it0, Modifier.fillMaxSize(), showTitle = false)
            }
        }
        // Gradients: strong on the left (text), at the bottom into the background (rows), subtle at the top
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to tint, 0.24f to tint.copy(alpha = .94f), 0.48f to tint.copy(alpha = .5f), 0.72f to Color.Transparent)))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to tint.copy(alpha = .5f), 0.16f to Color.Transparent, 0.38f to Color.Transparent, 0.58f to tint.copy(alpha = .82f), 0.78f to tint)))
    }
}

/** Info about the selected title at the top left (like the big picture, but for every selected title). */
@Composable
fun BillboardInfo(item: Item, modifier: Modifier = Modifier) {
    // T-030: one after the other (old text out in 120 ms, then the new one in): with a crossfade both titles were visible
    // on top of each other for a moment
    AnimatedContent(
        item, modifier = modifier, label = "bbi", contentAlignment = Alignment.TopStart,
        transitionSpec = {
            if (Look.motion == 0) EnterTransition.None togetherWith ExitTransition.None
            else (fadeIn(tween(220, delayMillis = 120)) togetherWith fadeOut(tween(120))) using null
        }
    ) { c ->
        val m = rememberFullMeta(c, rememberMeta(c))
        Column(Modifier.widthIn(max = dpx(980)), verticalArrangement = Arrangement.spacedBy(dpx(14))) {
            Kicker(Lang.tr(kindLabel(c.kind)) + (qualityOf(c)?.let { q -> "  ·  " + q.uppercase() } ?: ""))
            // The info block must fit into the space above the selected row: title always single-line (long titles smaller),
            // description at most two lines. Otherwise it runs into the row title.
            val tsz = if (c.title.length <= 24) 84 else if (c.title.length <= 34) 66 else 54
            Text(
                raw = true, text = c.title, color = Tok.TextHi, fontSize = spx(tsz), lineHeight = spx(84),
                fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.5).sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            HeroMetaLine(c, m, dots = false)
            m?.overview?.takeIf { it.isNotBlank() }?.let {
                Text(raw = true, text = TitleText.short(it, 170), color = Tok.TextMid, fontSize = spx(20), lineHeight = spx(29), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Small overline with a glowing bar (kind, New, quality). */
@Composable
fun Kicker(text: String, raw: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(dpx(12))) {
        Box(
            Modifier.width(dpx(5)).height(dpx(24)).shadow(dpx(10), RoundedCornerShape(50), ambientColor = Accent1, spotColor = Accent1)
                .clip(RoundedCornerShape(50)).background(Accent1)
        )
        Text(raw = raw, text = text, color = Accent2, fontSize = spx(17), fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp, maxLines = 1)
    }
}


/** T-032: reads the dominant color of a loaded picture ([AmbientLogic]) from a 16 × 9 copy. Call off the main thread. */
object AmbientColor {
    fun of(d: android.graphics.drawable.Drawable): Int? = runCatching {
        val src = (d as? android.graphics.drawable.BitmapDrawable)?.bitmap ?: return null
        // Hardware bitmaps (Coil's default) cannot be read pixel by pixel: copy to a normal one first
        val soft = if (android.os.Build.VERSION.SDK_INT >= 26 && src.config == android.graphics.Bitmap.Config.HARDWARE)
            src.copy(android.graphics.Bitmap.Config.ARGB_8888, false) else src
        val small = android.graphics.Bitmap.createScaledBitmap(soft, 16, 9, true)
        val px = IntArray(16 * 9)
        small.getPixels(px, 0, 16, 0, 0, 16, 9)
        AmbientLogic.dominant(px)
    }.getOrNull()
}
