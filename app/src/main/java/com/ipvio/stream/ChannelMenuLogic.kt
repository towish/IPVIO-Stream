package com.ipvio.stream

/**
 * T-027: logic of the channel menu in the live player (Left key) that does not need Android, so it can be unit tested.
 */
object ChannelMenuLogic {
    /** Pause on a category before its channels are shown next to it: stepping through the categories stays smooth. */
    const val PREVIEW_DELAY_MS = 320L

    /**
     * Category order of the menu, the same as in the Live TV tab: favorite categories first, then the provider groups in
     * the provider order ([providerOrder], group → position), with the Québec / Canadian groups pinned first by [pin]
     * when the Québec order is on, then the user's own order or A–Z ([userOrder] = CatOrder.sorted). Hidden categories
     * are left out ([favCats] comes already without them, as in the Live TV tab).
     */
    fun categories(
        groups: Collection<String>, providerOrder: Map<String, Int>, favCats: List<String>, hidden: Collection<String>,
        pin: ((List<String>) -> List<String>)?, userOrder: (List<String>) -> List<String>
    ): List<String> {
        val byProvider = if (providerOrder.isEmpty()) groups.toList() else groups.sortedBy { providerOrder[it] ?: 1_000_000 }
        val pinned = pin?.invoke(byProvider) ?: byProvider
        val favSet = favCats.toHashSet()
        val hiddenSet = hidden.toHashSet()
        return favCats + userOrder(pinned).filter { it !in favSet && it !in hiddenSet }
    }

    /**
     * T-030: smallest pause between two steps of a held Up/Down key, by how long it is held ([heldMs]): about 9 steps
     * per second for the first second (as before), then 16, then 28 after 2.5 s, so long lists are crossed quickly.
     */
    fun holdGapMs(heldMs: Long): Long = when {
        heldMs < 1000 -> 110L
        heldMs < 2500 -> 60L
        else -> 35L
    }

    /** First visible row of a list so that row [index] is on screen with a few rows above it (never negative). */
    fun firstVisible(index: Int, above: Int = 3): Int = if (index < 0) 0 else (index - above).coerceAtLeast(0)

    /**
     * Two or three letters for a channel without a logo: "CA: TVA Montréal FHD" → "TVA", "Canal Vie" → "CV",
     * "Historia" → "HI". Provider prefixes and quality tags are skipped.
     */
    fun monogram(title: String): String {
        val words = QcLineupLogic.stripPrefix(title).split(' ', '-', '_', '.', '|', ':', '/', '(', ')', '[', ']')
            .filter { w -> w.isNotEmpty() && w.lowercase() !in QUALITY && w.any { it.isLetterOrDigit() } }
        if (words.isEmpty()) return title.trim().take(2).uppercase()
        val first = words[0].filter { it.isLetterOrDigit() }
        return when {
            first.length in 2..4 && first.all { it.isUpperCase() || it.isDigit() } -> first
            words.size >= 2 -> (first.take(1) + words[1].filter { it.isLetterOrDigit() }.take(1)).uppercase()
            else -> first.take(2).uppercase()
        }
    }

    private val QUALITY = setOf("hd", "fhd", "uhd", "sd", "4k", "8k", "hevc", "hdr", "1080p", "720p", "raw", "vip", "ᴴᴰ")
}
