package com.ipvio.stream

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import java.text.Normalizer

/**
 * Keyword collection for the TV guide: groups channels by words in the channel name or in the
 * provider category – e.g. all Québec channels, no matter which provider category they are in.
 * A keyword matches when a word starts with it ("rds" matches "RDS 2", not "Jailbirds";
 * "canal d" matches "Canal D"). Logic in [GuideCollectionLogic] (without Android, testable).
 */
data class CollectionDef(
    val id: Int,
    val name: String,
    val emoji: String,
    val nameKeywords: List<String>,
    val groupKeywords: List<String>,
    /** 0 = by name (A–Z), 1 = by number (playlist order) */
    val sort: Int
)

/** Create, match and sort keyword collections (without Android, testable). */
object GuideCollectionLogic {
    const val MAX_NAME = 40
    const val MAX_KEYWORDS = 200

    fun cleanName(n: String): String = n.replace(Regex("[\\t\\n\\r]"), " ").trim().take(MAX_NAME)
    fun cleanEmoji(e: String): String = e.replace(Regex("[\\t\\n\\r]"), "").trim().take(6)

    /** Keywords: comma-separated, empties and duplicates dropped (kept as typed, folded only for matching). */
    fun cleanKeywords(s: String): List<String> =
        s.split(',').map { it.replace(Regex("[\\t\\n\\r]"), " ").trim() }.filter { it.isNotEmpty() }.distinct().take(40)

    /** Lowercase, accents stripped, anything else becomes a space (word separator). */
    fun fold(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
            .lowercase(java.util.Locale.ROOT).map { if (it.isLetterOrDigit()) it else ' ' }.joinToString("")

    private fun words(folded: String): List<String> = folded.split(' ').filter { it.isNotEmpty() }

    /**
     * [keywordWords] match the text words when they appear in order, the first one as a word start:
     * "rds" matches "RDS 2", "canal d" matches "Canal D", neither matches "Jailbirds".
     */
    fun matchesKeyword(textWords: List<String>, keywordWords: List<String>): Boolean {
        if (keywordWords.isEmpty()) return false
        for (i in textWords.indices) {
            if (!textWords[i].startsWith(keywordWords[0])) continue
            var ok = true
            for (j in 1 until keywordWords.size) {
                if (i + j >= textWords.size || textWords[i + j] != keywordWords[j]) { ok = false; break }
            }
            if (ok) return true
        }
        return false
    }

    /** Channels of the collection, in its sort order (name A–Z, or playlist order). Empty without keywords. */
    fun itemsOf(c: CollectionDef, all: List<Item>): List<Item> {
        val nk = c.nameKeywords.map { words(fold(it)) }.filter { it.isNotEmpty() }
        val gk = c.groupKeywords.map { words(fold(it)) }.filter { it.isNotEmpty() }
        if (nk.isEmpty() && gk.isEmpty()) return emptyList()
        val l = all.filter { item ->
            val nw = words(fold(item.title))
            if (nk.any { matchesKeyword(nw, it) }) return@filter true
            if (gk.isEmpty()) return@filter false
            val gw = words(fold(item.group))
            gk.any { matchesKeyword(gw, it) }
        }
        return if (c.sort == 1) l else l.sortedBy { fold(it.title) }
    }

    /** Text form: one line per collection "id TAB emoji TAB name TAB nameKw,nameKw TAB groupKw,groupKw TAB sort". */
    fun encode(list: List<CollectionDef>): String = list.joinToString("\n") {
        "${it.id}\t${cleanEmoji(it.emoji)}\t${cleanName(it.name)}\t${it.nameKeywords.joinToString(",")}\t${it.groupKeywords.joinToString(",")}\t${it.sort}"
    }

    fun decode(s: String?): List<CollectionDef> {
        if (s.isNullOrBlank()) return emptyList()
        return s.split('\n').mapNotNull { line ->
            val p = line.split('\t')
            if (p.size != 6) return@mapNotNull null
            val id = p[0].toIntOrNull() ?: return@mapNotNull null
            CollectionDef(id, cleanName(p[2]), cleanEmoji(p[1]), cleanKeywords(p[3]), cleanKeywords(p[4]), p[5].toIntOrNull()?.coerceIn(0, 1) ?: 0)
        }
    }
}

/** Keyword collections of the TV guide (per profile). Logic in [GuideCollectionLogic]. */
object GuideCollections {
    val list = mutableStateListOf<CollectionDef>()
    /** Changes with every switch (trigger to rebuild the guide categories). */
    var version by mutableIntStateOf(0)
        private set
    private var inited = false
    private fun prefs(c: Context) = Profiles.sp(c, "ipvio_guidecoll")

    fun init(c: Context) {
        if (inited) return
        inited = true
        val p = prefs(c)
        // First run only (no key yet): one collection to start with. Deleting every collection stays deleted.
        if (!p.contains("l")) {
            list.addAll(defaultCollections())
            save(c)
        } else {
            list.addAll(GuideCollectionLogic.decode(p.getString("l", null)))
        }
    }

    private fun defaultCollections(): List<CollectionDef> = listOf(
        CollectionDef(
            1, "Postes québécois", "⚜",
            listOf("tva", "radio-canada", "tele-quebec", "noovo", "lcn", "rdi", "rds", "canal d"),
            listOf("quebec"), 0
        )
    )

    private fun save(c: Context) {
        prefs(c).edit().putString("l", GuideCollectionLogic.encode(list.toList())).apply()
        version++
    }

    /** New collection; returns the new ID. */
    fun create(c: Context, def: CollectionDef): Int {
        init(c)
        val id = (list.maxOfOrNull { it.id } ?: 0) + 1
        list.add(def.copy(id = id))
        save(c)
        return id
    }

    fun update(c: Context, def: CollectionDef) {
        init(c)
        val i = list.indexOfFirst { it.id == def.id }
        if (i < 0) return
        list[i] = def
        save(c)
    }

    fun delete(c: Context, id: Int) {
        init(c)
        if (list.removeAll { it.id == id }) save(c)
    }
}
