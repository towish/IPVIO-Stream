package com.ipvio.stream

import kotlin.math.pow

/**
 * Timeline of the DubTvP start intro (pure logic, unit-tested). All values are functions of the elapsed time, so the
 * Compose side only reads one float per frame in the draw phase.
 *
 * Sequence (ms): spun-titanium backdrop fades in (0–350); the glass D turns in and settles (120–1400); a band of light
 * crosses the D (1000–1650); the metal wordmark slides in (1350–1950); a band of light crosses the wordmark
 * (1950–2600); hold; everything fades out (2950–3300).
 */
object BrandIntroLogic {
    const val TOTAL_MS = 3300

    /** Start angle of the D around its vertical axis, in degrees (it turns to 0). */
    const val TURN_START_DEG = -34f

    class Frame(
        /** Opacity of the whole intro (fade-out at the end). */
        val overall: Float,
        val backdrop: Float,
        val markAlpha: Float,
        /** Rotation of the D around its vertical axis, degrees. */
        val markTurn: Float,
        val markScale: Float,
        /** Position of the light band over the D, 0..1 across it; null = no band. */
        val markSweep: Float?,
        /** Red light behind the glass, 0..1. */
        val glow: Float,
        val wordAlpha: Float,
        /** Horizontal offset of the wordmark as a fraction of its height (slides in from the right). */
        val wordShift: Float,
        val wordSweep: Float?
    )

    /** Progress of a segment from [start] to [end] ms, clamped to 0..1. */
    fun seg(t: Float, start: Int, end: Int): Float = ((t - start) / (end - start)).coerceIn(0f, 1f)

    fun easeOutCubic(x: Float): Float = 1f - (1f - x).pow(3)

    fun smooth(x: Float): Float = x * x * (3f - 2f * x)

    fun frame(tMs: Float): Frame {
        val t = tMs.coerceIn(0f, TOTAL_MS.toFloat())
        val turn = easeOutCubic(seg(t, 120, 1400))
        val ms = seg(t, 1000, 1650)
        val word = smooth(seg(t, 1350, 1950))
        val ws = seg(t, 1950, 2600)
        return Frame(
            overall = 1f - smooth(seg(t, 2950, TOTAL_MS)),
            backdrop = smooth(seg(t, 0, 350)),
            markAlpha = smooth(seg(t, 120, 520)),
            markTurn = TURN_START_DEG * (1f - turn),
            markScale = 0.9f + 0.1f * turn,
            markSweep = if (ms <= 0f || ms >= 1f) null else ms,
            glow = smooth(seg(t, 300, 1500)),
            wordAlpha = word,
            wordShift = 0.35f * (1f - word),
            wordSweep = if (ws <= 0f || ws >= 1f) null else ws
        )
    }

    /**
     * Whether the intro plays: only on a fresh start of the screen (not after a rotation or recreation), when it is
     * switched on, when animations are not off ([motion] 0), and not when the app was opened to go straight to a
     * channel (reminder).
     */
    fun shouldShow(freshStart: Boolean, enabled: Boolean, motion: Int, openChannelPending: Boolean): Boolean =
        freshStart && enabled && motion != 0 && !openChannelPending
}
