package com.ipvio.stream

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/**
 * T-031: the DubTvP mark: a D in red glass whose counter is the play sign. The picture (res/drawable-nodpi/
 * ic_brand_mark_3d.webp, 512 px, transparent) is a Cycles render from Blender: ruby glass lit from behind, with
 * polished rims and one glint. The scene that makes it is described in docs/tasks/T-029-T-035-next-level.md.
 */
@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_brand_mark_3d),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

/**
 * Loading and buffering animation of the mark (T-031): a red light breathes behind the glass D and a band of light
 * glides over it, about every 1.8 s. The band is drawn with [BlendMode.SrcAtop] on an offscreen layer, so it only
 * lights the pixels of the picture and follows its outline exactly. With reduced motion ([Look.motion] = 0) or
 * [active] = false the mark stands still. Only the draw phase changes: no recomposition per frame.
 */
@Composable
fun BrandLoader(modifier: Modifier = Modifier, active: Boolean = true) {
    if (!active || Look.motion == 0) { BrandMark(modifier); return }
    val t = rememberInfiniteTransition(label = "brandLoader")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart), label = "light")
    Box(modifier) {
        // Red glow behind the glass, breathing
        Canvas(Modifier.fillMaxSize()) {
            val k = 0.55f + 0.45f * kotlin.math.sin(p * 2f * Math.PI.toFloat()).let { it * it }
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFFFF2A47).copy(alpha = 0.34f * k), Color.Transparent),
                    center = center, radius = size.minDimension * 0.62f
                ),
                radius = size.minDimension * 0.62f
            )
        }
        // The mark, with a band of light kept inside its own pixels
        BrandMark(Modifier.fillMaxSize().glassSweep { -0.35f + p * 1.7f })
    }
}

/**
 * A band of white light over this element's own pixels only: the element is drawn on an offscreen layer and the band
 * with [BlendMode.SrcAtop], so it follows the picture's outline. [pos] gives the band centre along the diagonal
 * (0 = top left, 1 = bottom right) and is read in the draw phase; null draws no band.
 */
fun Modifier.glassSweep(strength: Float = 0.36f, width: Float = 0.16f, pos: () -> Float?): Modifier =
    this.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val c = pos() ?: return@drawWithContent
            val band = Brush.linearGradient(
                0f to Color.Transparent,
                (c - width).coerceIn(0f, 1f) to Color.Transparent,
                c.coerceIn(0f, 1f) to Color.White.copy(alpha = strength),
                (c + width).coerceIn(0f, 1f) to Color.Transparent,
                1f to Color.Transparent,
                start = Offset.Zero, end = Offset(size.width, size.height)
            )
            drawRect(band, blendMode = BlendMode.SrcAtop)
        }

/** Whether the start intro is on screen (T-036). Read by [MainActivity] to let any key skip it. */
object BrandIntroState {
    var active by mutableStateOf(false)
}

/**
 * T-036: the DubTvP start intro, drawn above the whole app for about 3 s on a fresh start: spun-titanium backdrop, the
 * glass D turning into place with a band of light, then the brushed-metal wordmark. The last frame matches the Android
 * TV banner. Timing comes from [BrandIntroLogic]; only the draw phase changes per frame. A tap ends it; keys are
 * handled in [MainActivity.dispatchKeyEvent]. The app loads underneath meanwhile.
 */
@Composable
fun BrandIntro(onDone: () -> Unit) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        p.animateTo(1f, tween(BrandIntroLogic.TOTAL_MS, easing = LinearEasing))
        onDone()
    }
    fun f() = BrandIntroLogic.frame(p.value * BrandIntroLogic.TOTAL_MS)
    val word = painterResource(R.drawable.brand_wordmark_metal)
    BoxWithConstraints(
        Modifier.fillMaxSize()
            .graphicsLayer { alpha = f().overall }
            .background(Color(0xFF060607))
            .pointerInput(Unit) { detectTapGestures { onDone() } }
    ) {
        // Layout of the TV banner: D 50 % of the height, wordmark 15 %, gap 6.5 %, centred as a group
        val h = maxHeight
        val w = maxWidth
        val markH = h * 0.5f
        val markW = markH * 0.936f                       // width of the D itself
        val box = markH * (512f / 452f)                   // the picture keeps a transparent margin around the D
        val wordH = h * 0.15f
        val ratio = word.intrinsicSize.let { if (it.height > 0f) it.width / it.height else 4.63f }
        val wordW = wordH * ratio
        val gap = h * 0.065f
        val x0 = (w - (markW + gap + wordW)) / 2
        val markCx = x0 + markW / 2
        val wordCx = x0 + markW + gap + wordW / 2
        Image(
            painterResource(R.drawable.intro_backdrop_spun), null,
            Modifier.fillMaxSize().graphicsLayer { alpha = f().backdrop },
            contentScale = ContentScale.Crop
        )
        // Red light of the glass on the metal
        Canvas(Modifier.fillMaxSize()) {
            val fr = f()
            val r = markH.toPx() * 0.8f
            val c = Offset(markCx.toPx(), size.height / 2f)
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFF2A47).copy(alpha = 0.32f * fr.glow), Color.Transparent), center = c, radius = r), radius = r, center = c)
        }
        Box(
            Modifier.align(Alignment.Center).offset(x = markCx - w / 2).size(box)
                .graphicsLayer {
                    val fr = f()
                    alpha = fr.markAlpha
                    rotationY = fr.markTurn
                    scaleX = fr.markScale
                    scaleY = fr.markScale
                    cameraDistance = 14f * density
                }
        ) {
            Image(
                painterResource(R.drawable.ic_brand_mark_3d), null,
                Modifier.fillMaxSize().glassSweep { f().markSweep?.let { -0.2f + it * 1.4f } },
                contentScale = ContentScale.Fit
            )
        }
        Image(
            word, "DubTvP",
            Modifier.align(Alignment.Center).offset(x = wordCx - w / 2).size(wordW, wordH)
                .graphicsLayer {
                    val fr = f()
                    alpha = fr.wordAlpha
                    translationX = fr.wordShift * wordH.toPx()
                }
                .glassSweep(strength = 0.55f, width = 0.08f) { f().wordSweep?.let { -0.1f + it * 1.2f } },
            contentScale = ContentScale.Fit
        )
    }
}
