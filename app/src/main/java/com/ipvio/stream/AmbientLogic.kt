package com.ipvio.stream

/**
 * T-032: "ambient color" of the home screen: the background takes on the dominant color of the selected title's picture.
 * Pure Kotlin (no Android), so it can be unit tested; the picture is read as a few ARGB pixels (for example 16 × 9).
 */
object AmbientLogic {
    /**
     * Dominant color of [pixels] (ARGB ints) as ARGB, or null when the picture has no usable color (all gray, black or
     * transparent). Colorful, bright pixels weigh most; the result is toned down to sit behind text: saturation at most
     * 0.75, brightness between 0.30 and 0.55.
     */
    fun dominant(pixels: IntArray): Int? {
        var r = 0.0; var g = 0.0; var b = 0.0; var w = 0.0
        val hsv = FloatArray(3)
        for (p in pixels) {
            val a = (p ushr 24) and 0xFF
            if (a < 128) continue
            val pr = (p shr 16) and 0xFF; val pg = (p shr 8) and 0xFF; val pb = p and 0xFF
            toHsv(pr, pg, pb, hsv)
            val wt = (hsv[1] * hsv[1] * hsv[2]).toDouble()
            if (wt <= 0.0) continue
            r += pr * wt; g += pg * wt; b += pb * wt; w += wt
        }
        if (w < 1e-3) return null
        toHsv((r / w).toInt(), (g / w).toInt(), (b / w).toInt(), hsv)
        if (hsv[1] < 0.12f) return null
        hsv[1] = hsv[1].coerceAtMost(0.75f)
        hsv[2] = hsv[2].coerceIn(0.30f, 0.55f)
        return fromHsv(hsv)
    }

    private fun toHsv(r: Int, g: Int, b: Int, out: FloatArray) {
        val rf = r / 255f; val gf = g / 255f; val bf = b / 255f
        val max = maxOf(rf, gf, bf); val min = minOf(rf, gf, bf); val d = max - min
        var h = when {
            d == 0f -> 0f
            max == rf -> 60f * (((gf - bf) / d) % 6f)
            max == gf -> 60f * (((bf - rf) / d) + 2f)
            else -> 60f * (((rf - gf) / d) + 4f)
        }
        if (h < 0) h += 360f
        out[0] = h; out[1] = if (max == 0f) 0f else d / max; out[2] = max
    }

    private fun fromHsv(hsv: FloatArray): Int {
        val h = hsv[0]; val s = hsv[1]; val v = hsv[2]
        val c = v * s; val x = c * (1 - kotlin.math.abs((h / 60f) % 2f - 1)); val m = v - c
        val (r1, g1, b1) = when {
            h < 60 -> Triple(c, x, 0f); h < 120 -> Triple(x, c, 0f); h < 180 -> Triple(0f, c, x)
            h < 240 -> Triple(0f, x, c); h < 300 -> Triple(x, 0f, c); else -> Triple(c, 0f, x)
        }
        val r = ((r1 + m) * 255).toInt().coerceIn(0, 255); val g = ((g1 + m) * 255).toInt().coerceIn(0, 255); val b = ((b1 + m) * 255).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
