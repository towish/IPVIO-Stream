package com.ipvio.stream

/**
 * T-033: the "Tonight" row of the home screen: for each of the user's channels the show worth watching tonight, taken
 * from the TV guide on the device (nothing is sent anywhere). Pure Kotlin, so it can be unit tested.
 */
object TonightLogic {
    /** One show of the row: [channel] and the show's title and times (epoch seconds). */
    class Pick<T>(val channel: T, val title: String, val start: Long, val end: Long)

    /** A guide entry as plain values. */
    class Show(val title: String, val start: Long, val end: Long)

    private const val DAY = 86_400L

    /**
     * The "tonight" window for [nowSec] in a time zone [offsetSec] ahead of UTC: before 19:00 it is 19:00–23:00 of today;
     * from 19:00 to 23:00 it runs from now to 23:30; later it is the next three hours.
     */
    fun window(nowSec: Long, offsetSec: Int): Pair<Long, Long> {
        val local = nowSec + offsetSec
        val midnight = local - Math.floorMod(local, DAY) - offsetSec
        val h19 = midnight + 19 * 3600; val h23 = midnight + 23 * 3600
        return when {
            nowSec < h19 -> h19 to h23
            nowSec < h23 -> nowSec to h23 + 1800
            else -> nowSec to nowSec + 3 * 3600
        }
    }

    /**
     * For each channel the first show of at least 20 minutes that starts in the window (one of 30 minutes or more is
     * preferred), or the show running now when it began less than 15 minutes ago. Sorted by start time, then by channel
     * order; at most [max] entries.
     */
    fun <T> pick(channels: List<Pair<T, List<Show>>>, nowSec: Long, offsetSec: Int, max: Int = 12): List<Pick<T>> {
        val (ws, we) = window(nowSec, offsetSec)
        val out = ArrayList<Pair<Int, Pick<T>>>()
        for ((ci, pair) in channels.withIndex()) {
            val (ch, shows) = pair
            val running = shows.firstOrNull { nowSec in it.start until it.end && nowSec >= ws && nowSec - it.start <= 15 * 60 && it.end - it.start >= 20 * 60 }
            val starting = shows.filter { it.start in ws until we && it.end - it.start >= 20 * 60 }.sortedBy { it.start }
            val best = running ?: starting.firstOrNull { it.end - it.start >= 30 * 60 } ?: starting.firstOrNull() ?: continue
            out.add(ci to Pick(ch, best.title, best.start, best.end))
        }
        return out.sortedWith(compareBy<Pair<Int, Pick<T>>> { it.second.start }.thenBy { it.first }).take(max).map { it.second }
    }
}
