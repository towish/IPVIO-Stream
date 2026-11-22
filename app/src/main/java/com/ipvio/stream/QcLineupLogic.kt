package com.ipvio.stream

/**
 * "Québec first" channel order in the style of Bell Fibe TV (Québec line-up, February 2026).
 *
 * Recognises the main Québec and Canadian channels by name, whatever the provider writes around the name
 * ("CA: TVA MONTREAL FHD", "QC| Noovo HD", "TVA Sports 2 (CA)", "ICI Télé Québec ᴴᴰ"), and sorts them by their
 * Bell Fibe number: 101 ICI Radio-Canada Télé, 102 Noovo, 103 TVA, 104 Télé-Québec, 105 MétéoMédia, 108 RDS …,
 * then the English Canadian networks (201 CTV …) and the US networks (220 NBC …). Several regional versions of
 * one station (TVA Québec, TVA Montréal, TVA Sherbrooke) stay together, the preferred region first.
 * All other channels keep the provider order after them.
 *
 * Matching is strict on purpose: the provider name, once cleaned of prefixes, quality tags and region words,
 * must equal an alias. Only distinctive multi-word aliases ([Entry.loose]) may appear inside a longer name.
 * This keeps channels such as "ABC News Live", "CTV News Channel" or "TV5 Monde" out of the Bell slots.
 *
 * Pure Kotlin without Android imports, so it can be unit tested.
 */
object QcLineupLogic {
    const val REGION_MONTREAL = 0
    const val REGION_QUEBEC = 1
    const val REGION_SHERBROOKE = 2
    const val REGION_OUTAOUAIS = 3
    /** A region word that is none of the four above (Saguenay, Rimouski, Trois-Rivières …). */
    const val REGION_OTHER = 4
    /** No region in the name (national feed, or the provider did not say). */
    const val REGION_NONE = -1

    /** Regions the user can put first, in the order of the setting. */
    val PREFERRED_REGIONS = listOf(REGION_MONTREAL, REGION_QUEBEC, REGION_SHERBROOKE, REGION_OUTAOUAIS)

    /**
     * One Bell Fibe slot. [aliases] are written as they read; they are cleaned with the same rules as the provider
     * names. [loose] = an alias may also appear inside a longer name (only for distinctive names).
     * [calls] = call letters of regional stations with their region (CFCM = TVA Québec).
     */
    class Entry(val number: Int, val name: String, val aliases: List<String>, val loose: Boolean = false, val calls: Map<String, Int> = emptyMap())

    /** Result of a match: the Bell Fibe number and the region found in the name. */
    data class Match(val number: Int, val region: Int)

    val LINEUP: List<Entry> = listOf(
        // French channels (Bell Fibe Québec 100–199)
        Entry(101, "ICI Radio-Canada Télé", listOf("ICI Radio-Canada Télé", "ICI Radio-Canada", "Radio-Canada", "Radio-Canada Télé", "ICI Télé", "ICI", "SRC"),
            calls = mapOf("cbft" to REGION_MONTREAL, "cbvt" to REGION_QUEBEC, "cksh" to REGION_SHERBROOKE, "cboft" to REGION_OUTAOUAIS)),
        Entry(102, "Noovo", listOf("Noovo", "V Télé", "VTélé", "V"),
            calls = mapOf("cfjp" to REGION_MONTREAL, "cfap" to REGION_QUEBEC, "cfks" to REGION_SHERBROOKE, "cfgs" to REGION_OUTAOUAIS)),
        Entry(103, "TVA", listOf("TVA"),
            calls = mapOf("cftm" to REGION_MONTREAL, "cfcm" to REGION_QUEBEC, "chlt" to REGION_SHERBROOKE, "chot" to REGION_OUTAOUAIS)),
        Entry(104, "Télé-Québec", listOf("Télé-Québec", "TeleQuebec", "CIVM")),
        Entry(105, "MétéoMédia", listOf("MétéoMédia", "Météo Média", "Meteo")),
        Entry(108, "RDS", listOf("RDS", "Réseau des sports")),
        Entry(109, "RDS2", listOf("RDS 2")),
        Entry(110, "RDS Info", listOf("RDS Info")),
        Entry(111, "TVA Sports", listOf("TVA Sports", "TVA Sport")),
        Entry(112, "TVA Sports 2", listOf("TVA Sports 2", "TVA Sport 2")),
        Entry(113, "France 24", listOf("France 24", "France 24 Français")),
        Entry(115, "LCN", listOf("LCN", "Le Canal Nouvelles", "TVA Nouvelles")),
        Entry(117, "ICI RDI", listOf("ICI RDI", "RDI")),
        Entry(118, "Euronews", listOf("Euronews", "Euronews Français")),
        Entry(119, "TéléMag", listOf("TéléMag", "Télé Mag")),
        Entry(120, "Addik", listOf("Addik", "AddikTV", "Addik TV")),
        Entry(121, "Z", listOf("Z", "Z Télé", "ZTélé")),
        Entry(122, "Séries+", listOf("Séries+", "Série+", "Séries Plus")),
        Entry(123, "Évasion", listOf("Évasion", "Canal Évasion")),
        Entry(124, "Historia", listOf("Historia")),
        Entry(125, "Canal D", listOf("Canal D")),
        Entry(126, "ICI Explora", listOf("ICI Explora", "Explora")),
        Entry(127, "Investigation", listOf("Investigation", "Canal D Investigation")),
        Entry(129, "Mezzo Live", listOf("Mezzo Live", "Mezzo")),
        Entry(130, "Elle Fictions", listOf("Elle Fictions", "Elle Fiction")),
        Entry(131, "MAX", listOf("MAX")),
        Entry(133, "ICI ARTV", listOf("ICI ARTV", "ARTV")),
        Entry(134, "TV5", listOf("TV5", "TV5 Québec Canada", "TV5 Canada")),
        Entry(135, "Casa", listOf("Casa", "Casa TV")),
        Entry(136, "Zeste", listOf("Zeste")),
        Entry(137, "Canal Vie", listOf("Canal Vie"), loose = true),
        Entry(138, "Témoin", listOf("Témoin")),
        Entry(140, "Prise 2", listOf("Prise 2")),
        Entry(141, "Unis", listOf("Unis", "Unis TV")),
        Entry(143, "L'Assemblée nationale du Québec", listOf("Assemblée nationale"), loose = true),
        Entry(144, "CPAC", listOf("CPAC", "CPAC Français")),
        Entry(145, "TFO", listOf("TFO")),
        Entry(147, "Savoir média", listOf("Savoir média", "Canal Savoir", "Savoir")),
        Entry(153, "Télétoon", listOf("Télétoon", "Teletoon French")),
        Entry(161, "Super Écran 1", listOf("Super Écran 1", "Super Écran")),
        Entry(162, "Super Écran 2", listOf("Super Écran 2")),
        Entry(163, "Super Écran 3", listOf("Super Écran 3")),
        Entry(164, "Super Écran 4", listOf("Super Écran 4")),
        Entry(165, "Cinépop", listOf("Cinépop")),
        Entry(166, "Frissons TV", listOf("Frissons", "Frissons TV")),
        Entry(168, "Natyf TV", listOf("Natyf", "Natyf TV")),
        // English Canadian networks (Bell Fibe 201–205)
        Entry(201, "CTV", listOf("CTV"), calls = mapOf("cfcf" to REGION_MONTREAL)),
        Entry(202, "CTV2", listOf("CTV 2", "CTV Two")),
        Entry(203, "Global", listOf("Global", "Global TV")),
        Entry(204, "Citytv", listOf("Citytv", "City TV", "City")),
        Entry(205, "CBC", listOf("CBC", "CBC TV"), calls = mapOf("cbmt" to REGION_MONTREAL)),
        // US networks (Bell Fibe 220–224)
        Entry(220, "NBC", listOf("NBC")),
        Entry(221, "ABC", listOf("ABC")),
        Entry(222, "CBS", listOf("CBS")),
        Entry(223, "FOX", listOf("FOX")),
        Entry(224, "PBS", listOf("PBS"))
    )

    // ------------------------------------------------------------------ Cleaning

    /** Tags that say nothing about which channel it is (quality, codec, country/language codes, feed labels). */
    private val NOISE = setOf(
        "hd", "fhd", "uhd", "sd", "hq", "lq", "4k", "8k", "hdr", "hevc", "h264", "h265", "x264", "x265",
        "720p", "1080p", "1080i", "2160p", "25fps", "30fps", "50fps", "60fps", "fps", "raw", "backup", "bkp", "vip",
        "low", "bit", "lowbit", "web", "geo", "feed", "live", "direct", "east", "est", "ouest", "west",
        "ca", "can", "canada", "canadian", "qc", "fr", "fra", "french", "francais", "en", "eng", "english", "us", "usa"
    )

    /** Region words → region. "quebec" is handled separately (it is also part of "Télé-Québec"). */
    private val REGION_WORDS = mapOf(
        "montreal" to REGION_MONTREAL, "mtl" to REGION_MONTREAL,
        "sherbrooke" to REGION_SHERBROOKE, "estrie" to REGION_SHERBROOKE,
        "gatineau" to REGION_OUTAOUAIS, "ottawa" to REGION_OUTAOUAIS, "outaouais" to REGION_OUTAOUAIS,
        "saguenay" to REGION_OTHER, "chicoutimi" to REGION_OTHER, "jonquiere" to REGION_OTHER,
        "trois" to REGION_OTHER, "rivieres" to REGION_OTHER, "mauricie" to REGION_OTHER,
        "rimouski" to REGION_OTHER, "riviere" to REGION_OTHER, "loup" to REGION_OTHER,
        "carleton" to REGION_OTHER, "abitibi" to REGION_OTHER, "rouyn" to REGION_OTHER, "noranda" to REGION_OTHER,
        "gaspesie" to REGION_OTHER, "matane" to REGION_OTHER, "sept" to REGION_OTHER, "iles" to REGION_OTHER
    )

    private val FILLERS = setOf("du", "de", "des", "la", "le")

    private fun foldChar(c: Char): String = when (c) {
        'à', 'â', 'ä', 'á', 'ã', 'å' -> "a"
        'ç' -> "c"
        'é', 'è', 'ê', 'ë' -> "e"
        'î', 'ï', 'í', 'ì' -> "i"
        'ô', 'ö', 'ó', 'ò', 'õ' -> "o"
        'û', 'ü', 'ú', 'ù' -> "u"
        'ÿ' -> "y"
        'œ' -> "oe"
        'æ' -> "ae"
        '+' -> " plus "
        else -> if (c in 'a'..'z' || c in '0'..'9') c.toString() else " "
    }

    /** Lower case, accents removed, every other character a space. No regex: runs on 16,000 names. */
    fun fold(s: String): String {
        val sb = StringBuilder(s.length + 8)
        for (ch in s.lowercase()) sb.append(foldChar(ch))
        return sb.toString()
    }

    /**
     * Removes the provider prefix: "CA: ", "CA| ", "QC|FR: ", "[CA] ", "(CA) ". A prefix is a short segment (at most
     * 8 characters, no digits) before ':' or '|', or a short bracketed tag at the start.
     */
    private val BRACKET_TAG = Regex("""^[\[(]\s*[A-Za-z]{1,6}\s*[\])]\s*""")

    fun stripPrefix(title: String): String {
        var t = title.trim()
        repeat(3) {
            val b = BRACKET_TAG.find(t)
            if (b != null) { t = t.substring(b.range.last + 1).trim(); return@repeat }
            val i = t.indexOfFirst { it == ':' || it == '|' }
            if (i in 1..8 && t.substring(0, i).none { it.isDigit() } && i < t.length - 1) t = t.substring(i + 1).trim()
        }
        return t
    }

    /** Splits a token like "rds2", "tv5", "ctv2" into letters and digits ("rds", "2"); "720p", "4k" stay whole. */
    private fun splitLettersDigits(tok: String): List<String> {
        val k = tok.indexOfFirst { it.isDigit() }
        if (k <= 0 || tok.substring(k).any { !it.isDigit() }) return listOf(tok)
        return listOf(tok.substring(0, k), tok.substring(k))
    }

    /** Cleaned tokens of a name plus the region it names. */
    class Cleaned(val tokens: List<String>, val region: Int, val calls: List<String>)

    fun clean(title: String): Cleaned {
        val raw = fold(stripPrefix(title)).split(' ').filter { it.isNotEmpty() }
        val hasIci = "ici" in raw || "radio" in raw
        val out = ArrayList<String>()
        var region = REGION_NONE
        var afterRegion = false
        for ((i, tok) in raw.withIndex()) {
            // "Rivière-du-Loup", "Îles-de-la-Madeleine": small words inside a region name
            if (afterRegion && tok in FILLERS) continue
            afterRegion = false
            if (tok in NOISE) continue
            if (tok == "quebec") {
                // "Télé-Québec" is a channel; "TVA Québec" / "ICI Télé Québec" name the region
                if (i > 0 && raw[i - 1] == "tele" && !hasIci) out.add(tok)
                else { if (region == REGION_NONE) region = REGION_QUEBEC; afterRegion = true }
                continue
            }
            val r = REGION_WORDS[tok]
            if (r != null) { if (region == REGION_NONE) region = r; afterRegion = true; continue }
            for (p in splitLettersDigits(tok)) out.add(p)
        }
        return Cleaned(out, region, raw)
    }

    // ------------------------------------------------------------------ Matching

    private class Alias(val tokens: List<String>, val key: String, val entry: Entry)

    private val exactIndex: Map<String, Entry> by lazy {
        val m = HashMap<String, Entry>()
        for (e in LINEUP) for (a in e.aliases) {
            val k = clean(a).tokens.joinToString(" ")
            if (k.isNotEmpty() && k !in m) m[k] = e
        }
        m
    }

    private val looseAliases: List<Alias> by lazy {
        LINEUP.filter { it.loose }.flatMap { e ->
            e.aliases.map { a -> val t = clean(a).tokens; Alias(t, t.joinToString(" "), e) }
        }.filter { it.tokens.isNotEmpty() }.sortedByDescending { it.tokens.size }
    }

    private val callIndex: Map<String, Pair<Entry, Int>> by lazy {
        val m = HashMap<String, Pair<Entry, Int>>()
        for (e in LINEUP) for ((call, r) in e.calls) m[call] = e to r
        m
    }

    private fun containsRun(hay: List<String>, needle: List<String>): Boolean {
        if (needle.size > hay.size) return false
        for (s in 0..hay.size - needle.size) {
            var ok = true
            for (j in needle.indices) if (hay[s + j] != needle[j]) { ok = false; break }
            if (ok) return true
        }
        return false
    }

    /** The Bell Fibe slot of a channel name, or null when it is not one of the listed channels. */
    fun match(title: String): Match? {
        val c = clean(title)
        // Call letters ("CFCM-DT", "TVA CFCM Québec") name the station and its region directly
        for (t in c.calls) callIndex[t]?.let { (e, r) -> return Match(e.number, if (c.region != REGION_NONE) c.region else r) }
        if (c.tokens.isEmpty()) return null
        exactIndex[c.tokens.joinToString(" ")]?.let { return Match(it.number, c.region) }
        for (a in looseAliases) if (containsRun(c.tokens, a.tokens)) return Match(a.entry.number, c.region)
        return null
    }

    /**
     * Rank of a region among the versions of one station: the preferred region first, then the national feed
     * (no region named), then Montréal (the most common feed), then all others.
     */
    fun regionRank(region: Int, preferred: Int): Int = when {
        region == preferred -> 0
        region == REGION_NONE -> 1
        region == REGION_MONTREAL -> 2
        else -> 3
    }

    /**
     * Québec first: recognised channels in Bell Fibe order (versions of one station by [regionRank], then the provider
     * order), followed by every other channel in the provider order. Stable; never drops or duplicates an entry.
     */
    fun <T> sort(items: List<T>, preferredRegion: Int, title: (T) -> String): List<T> = sortNumbered(items, preferredRegion, title).items

    /** First number for channels without a Bell slot, and for the second and further versions of a Bell channel (T-029). */
    const val OVERFLOW_START = 1000

    /** A sorted list with the channel number of each entry ([numbers] has the same order as [items]). */
    class Numbered<T>(val items: List<T>, val numbers: IntArray)

    /**
     * [sort] plus channel numbers (T-029): the first version of a recognised channel gets its Bell Fibe number
     * (TVA = 103, RDS = 108), every other channel a number from [OVERFLOW_START] up, in list order. Without any
     * recognised channel the list stays as it is and the numbers are the positions 1, 2, 3 … (as before).
     */
    fun <T> sortNumbered(items: List<T>, preferredRegion: Int, title: (T) -> String): Numbered<T> {
        val keyed = ArrayList<Pair<Long, Int>>()
        val bell = IntArray(items.size)
        val rest = ArrayList<Int>()
        for ((i, it) in items.withIndex()) {
            val m = match(title(it))
            if (m == null) rest.add(i)
            else { bell[i] = m.number; keyed.add((m.number.toLong() * 10 + regionRank(m.region, preferredRegion)) to i) }
        }
        if (keyed.isEmpty()) return Numbered(items, positions(items.size))
        keyed.sortWith(compareBy<Pair<Long, Int>> { it.first }.thenBy { it.second })
        val out = ArrayList<T>(items.size)
        val nums = IntArray(items.size)
        val used = HashSet<Int>()
        var next = OVERFLOW_START
        for ((_, i) in keyed) {
            val b = bell[i]
            nums[out.size] = if (used.add(b)) b else next++
            out.add(items[i])
        }
        for (i in rest) { nums[out.size] = next++; out.add(items[i]) }
        return Numbered(out, nums)
    }

    /** Numbers 1 … [n]: the channel numbers of a list without the Québec order. */
    fun positions(n: Int): IntArray = IntArray(n) { it + 1 }
}
