package com.ipvio.stream

/**
 * T-028: layout rules of the TV guide grid that do not need Android, so they can be unit tested.
 */
object GuideGridLogic {
    /** Shows shorter than this are left out of the grid (station IDs, fillers): drawn they would be slivers. */
    const val MIN_SHOW_SEC = 120L

    /** Room for a time mark in the header (dp): "12:30 PM" at 13 sp. */
    const val TIME_LABEL_W = 62f

    /** Room for a day mark at midnight (dp): "sam. 22 nov." at 13 sp. */
    const val DAY_LABEL_W = 96f

    /**
     * Date pattern of the day marks per UI language (the German pattern "EE dd.MM." read oddly in the other languages):
     * de "Sa. 22.11.", en "Sat Nov 22", fr "sam. 22 nov.", tr "22 Kas Cmt", bs/hr/sr "sub 22.11.".
     */
    fun dayPattern(lang: String): String = when (lang) {
        "de" -> "EE dd.MM."
        "en" -> "EEE MMM d"
        "tr" -> "d MMM EEE"
        "bs", "hr", "sr" -> "EEE d.M."
        else -> "EEE d MMM"
    }

    /** A header mark starting at [x] dp is drawn only when it fits completely into the header ([width] dp). */
    fun labelFits(x: Float, labelW: Float, width: Float): Boolean = x >= 0f && x + labelW <= width

    /** Width of a show block in dp: duration minus a 3 dp gap, never below 4 dp. */
    fun blockWidth(durSec: Long, dpm: Float): Float = (durSec / 60f * dpm - 3f).coerceIn(4f, 6000f)

    /** Corner radius of a block (dp): narrow blocks get small corners, so they stay a bar and not a dot. */
    fun blockRadius(widthDp: Float, full: Float): Float = minOf(full, widthDp / 3f).coerceAtLeast(2f)

    /** Shows of the grid: long enough to be seen. */
    fun <T> visible(entries: List<T>, start: (T) -> Long, end: (T) -> Long): List<T> =
        if (entries.none { end(it) - start(it) < MIN_SHOW_SEC }) entries else entries.filter { end(it) - start(it) >= MIN_SHOW_SEC }

    /** Number of whole text lines of [lineHeight] that fit into [height] (at least 1). */
    fun linesThatFit(height: Float, lineHeight: Float): Int = if (lineHeight <= 0f) 1 else (height / lineHeight).toInt().coerceAtLeast(1)
}
