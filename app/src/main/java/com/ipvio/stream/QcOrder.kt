package com.ipvio.stream

/**
 * T-026: the live channel list in the Québec-first order (see [QcLineupLogic]), computed once per playlist.
 * T-029: with the channel numbers that go with it (Bell numbers when the Québec order is on).
 *
 * The home screen (Live TV tab) and [MainActivity] (player, zapping, guide) both need the same order, so the channel
 * numbers match everywhere. The result is cached by the identity of the playlist list and the setting; a new playlist or
 * a changed setting computes it again. Sorting walks every channel name: call it off the main thread for a new list.
 */
object QcOrder {
    /** Channel numbers of one live list: number per channel id, and the channel per number (digit entry). */
    class Numbering(val byId: Map<String, Int>, val byNumber: Map<Int, Item>)

    private class Slot(val src: List<Item>, val key: Int, val out: List<Item>, val numbering: Numbering)
    // Two slots: the full playlist (player, Live TV) and the list of the movies/series tab can alternate without recomputing
    private var a: Slot? = null
    private var b: Slot? = null
    // Numbers of a list that did not come from here (provider order in MainActivity), built on first use
    @Volatile private var plain: Pair<List<Item>, Numbering>? = null

    /** The setting as one value: -1 = off (provider order), otherwise the preferred region (QcLineupLogic.REGION_*). */
    fun key(): Int = if (General.qcOrder) General.qcRegion else -1

    /** Live channels of [all] in playlist order, or Québec first when [key] >= 0. [live] = the live channels of [all], if already filtered. */
    fun live(all: List<Item>, key: Int, live: List<Item>? = null): List<Item> {
        peek(all, key)?.let { return it }
        // Sorted outside the lock, so a peek from the main thread never waits for it
        val l = live ?: all.filter { it.kind == Kind.LIVE }
        val r = if (key < 0) QcLineupLogic.Numbered(l, QcLineupLogic.positions(l.size)) else QcLineupLogic.sortNumbered(l, key) { it.title }
        store(Slot(all, key, r.items, numbering(r.items, r.numbers)))
        return r.items
    }

    @Synchronized private fun store(s: Slot) { b = a; a = s }

    /** Cached result for [all] and [key], or null when it still has to be computed. */
    @Synchronized fun peek(all: List<Item>, key: Int): List<Item>? {
        a?.let { if (it.src === all && it.key == key) return it.out }
        b?.let { if (it.src === all && it.key == key) { b = a; a = it; return it.out } }
        return null
    }

    /**
     * Channel numbers of the live list [list] (T-029): the numbers computed with its order when it came from [live],
     * otherwise the positions 1, 2, 3 …. Cheap when cached; a new plain list builds its map once (as Live TV did before).
     */
    fun numbering(list: List<Item>): Numbering {
        slotOf(list)?.let { return it.numbering }
        plain?.let { if (it.first === list) return it.second }
        return numbering(list, QcLineupLogic.positions(list.size)).also { plain = list to it }
    }

    /** Number per channel id of [list]. */
    fun numbers(list: List<Item>): Map<String, Int> = numbering(list).byId

    /** The channel with number [n] in [list], or null. */
    fun byNumber(list: List<Item>, n: Int): Item? = numbering(list).byNumber[n]

    @Synchronized private fun slotOf(list: List<Item>): Slot? {
        a?.let { if (it.out === list) return it }
        b?.let { if (it.out === list) return it }
        return null
    }

    private fun numbering(items: List<Item>, nums: IntArray): Numbering {
        val byId = HashMap<String, Int>(items.size * 2)
        val byNum = HashMap<Int, Item>(items.size * 2)
        for (i in items.indices) {
            // A channel id listed twice keeps its first number (same rule for the number → channel direction)
            if (byId.putIfAbsent(items[i].id, nums[i]) == null) byNum.putIfAbsent(nums[i], items[i])
        }
        return Numbering(byId, byNum)
    }

    /** Provider groups with the Québec / Canadian groups first, when the Québec order is on ([key] >= 0). */
    fun groups(g: Map<String, List<Item>>, key: Int): Map<String, List<Item>> {
        if (key < 0 || g.size < 2) return g
        val names = GuideCollectionLogic.pinnedFirst(g.keys.toList())
        if (names == g.keys.toList()) return g
        val m = LinkedHashMap<String, List<Item>>(g.size * 2)
        for (n in names) m[n] = g.getValue(n)
        return m
    }
}
