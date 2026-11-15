package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.StringReader

/** Keyword collections for the TV guide: word-start matching, sorting, saving, favorites surviving the language filter. */
class GuideCollectionTest {
    private fun ch(id: String, title: String, group: String) = Item(id, Kind.LIVE, title, null, "http://x/$id", null, group)

    private val quebec = CollectionDef(
        1, "Postes québécois", "⚜",
        listOf("tva", "radio-canada", "tele-quebec", "noovo", "lcn", "rdi", "rds", "canal d"),
        listOf("quebec"), 0
    )
    private val all = listOf(
        ch("1", "TVA", "CA | Général"),
        ch("2", "TVA Sports", "CA | Sports"),
        ch("3", "ICI Radio-Canada Télé", "CA | Général"),
        ch("4", "Télé-Québec", "CA | Général"),
        ch("5", "Noovo", "CA | Général"),
        ch("6", "RDS 2", "CA | Sports"),
        ch("7", "RDS Info", "CA | Sports"),
        ch("8", "Canal D", "CA | Docs"),
        ch("9", "Jailbirds", "US | Series"),
        ch("10", "LCN", "CA | News"),
        ch("11", "ZDF", "DE | News"),
        ch("12", "TVA Nouvelles", "CA | Quebec")
    )

    @Test fun wortAnfangTrifft_nichtInDerMitte() {
        val items = GuideCollectionLogic.itemsOf(quebec, all)
        val titles = items.map { it.title }
        assertTrue("RDS 2" in titles)
        assertTrue("RDS Info" in titles)
        assertFalse("Jailbirds" in titles)
        assertFalse("ZDF" in titles)
    }

    @Test fun mehrwortStichwortUndAkzente() {
        val items = GuideCollectionLogic.itemsOf(quebec, all)
        val titles = items.map { it.title }
        assertTrue("Canal D" in titles)                    // "canal d" = two words
        assertTrue("Télé-Québec" in titles)                // "tele-quebec" matches with accents
        assertTrue("ICI Radio-Canada Télé" in titles)      // "radio-canada" matches inside the name
    }

    @Test fun gruppenStichwort() {
        // "TVA Nouvelles" is already in by name; a group-only hit: group "CA | Quebec" with a neutral name
        val c = CollectionDef(2, "Q", "", emptyList(), listOf("quebec"), 0)
        val items = GuideCollectionLogic.itemsOf(c, all)
        assertEquals(listOf("TVA Nouvelles"), items.map { it.title })
    }

    @Test fun sortierungNameUndNummer() {
        val byName = GuideCollectionLogic.itemsOf(quebec, all).map { it.title }
        // Sorted by folded title (accents ignored): "Télé-Québec" sorts under "tele …", before "TVA"
        assertEquals(byName.sortedBy { GuideCollectionLogic.fold(it) }, byName)
        val byNum = GuideCollectionLogic.itemsOf(quebec.copy(sort = 1), all).map { it.title }
        assertEquals(all.filter { it.title in byName }.map { it.title }, byNum)
    }

    @Test fun ohneStichwoerter_leer() {
        assertTrue(GuideCollectionLogic.itemsOf(CollectionDef(3, "Leer", "", emptyList(), emptyList(), 0), all).isEmpty())
    }

    @Test fun stichwoerterWerdenGesaeubert() {
        // Stored as typed (folding happens only for matching); empties and duplicates are dropped
        assertEquals(listOf("RDS", "tva", "rds"), GuideCollectionLogic.cleanKeywords(" RDS , tva,, rds "))
        assertEquals("Télé-Québec", GuideCollectionLogic.cleanKeywords("Télé-Québec").single())
    }

    @Test fun encodeDecodeRundreise() {
        val defs = listOf(quebec, CollectionDef(2, "Sport", "⚽", listOf("rds", "espn"), emptyList(), 1))
        assertEquals(defs, GuideCollectionLogic.decode(GuideCollectionLogic.encode(defs)))
        assertEquals(emptyList<CollectionDef>(), GuideCollectionLogic.decode("kaputt\n1\tX"))
    }

    private val m3u = """#EXTM3U
#EXTINF:-1 tvg-id="tva" group-title="CA | Général",TVA
http://h/live/tva.ts
#EXTINF:-1 group-title="TR | Ulusal",TRT 1
http://h/live/trt.ts
"""

    @Test fun favoritBleibtTrotzAusgeblendeterSprache() {
        val trtId = "http://h/live/trt.ts".hashCode().toString()
        val hidden = M3uParser.parseStream(StringReader(m3u), hidden = setOf("tr"))
        assertFalse(hidden.items.any { it.title == "TRT 1" })
        val kept = M3uParser.parseStream(StringReader(m3u), hidden = setOf("tr"), keepIds = setOf(trtId))
        assertTrue(kept.items.any { it.title == "TRT 1" })
        assertEquals(trtId, kept.items.first { it.title == "TRT 1" }.id)
    }

    @Test fun cacheFavoritBleibt() {
        val items = M3uParser.parse(m3u)
        val trtId = items.first { it.title == "TRT 1" }.id
        val out = ByteArrayOutputStream()
        PlaylistCacheIO.write(out, "http://x/get.php", items, emptyList(), emptyMap())
        val dropped = PlaylistCacheIO.read(ByteArrayInputStream(out.toByteArray()), "http://x/get.php", setOf("tr"))!!
        assertFalse(dropped.items.any { it.title == "TRT 1" })
        val kept = PlaylistCacheIO.read(ByteArrayInputStream(out.toByteArray()), "http://x/get.php", setOf("tr"), keepIds = setOf(trtId))!!
        assertTrue(kept.items.any { it.title == "TRT 1" })
    }

    @Test fun foldCacheErstLeerDannVoll() {
        // The guide must open without the collections while the cache builds (no main-thread scan)
        val other = listOf(ch("99", "TVA", "CA | Général"))
        assertFalse(GuideCollectionLogic.cacheReady(all))
        assertEquals(emptyList<Item>(), GuideCollectionLogic.itemsOfCached(quebec, all, all))
        GuideCollectionLogic.buildCache(all)
        assertTrue(GuideCollectionLogic.cacheReady(all))
        assertEquals(GuideCollectionLogic.itemsOf(quebec, all).map { it.id },
            GuideCollectionLogic.itemsOfCached(quebec, all, all).map { it.id })
        // A different list has no cache yet; rebuilding is safe to call again
        assertFalse(GuideCollectionLogic.cacheReady(other))
        GuideCollectionLogic.buildCache(all)
        assertTrue(GuideCollectionLogic.cacheReady(all))
    }

    private val sports = listOf(
        ch("s1", "RDS", "CA | Sports"),
        ch("s2", "TSN 2", "CA | Sports"),
        ch("s3", "Sportsnet One", "CA | Sports"),
        ch("s4", "TVA Sports", "CA | Sports"),
        ch("s5", "NHL Network", "US | Sports"),
        ch("s6", "Golf Channel", "US | Sports"),
        ch("s7", "NFL Network", "US | Sports"),
        ch("s8", "NBA TV", "US | Sports"),
        ch("s9", "MLB Network", "US | Sports"),
        ch("s10", "beIN Sports", "US | Soccer"),
        ch("s11", "F1 TV", "US | Sports"),
        ch("s12", "Tennis Channel", "US | Sports"),
        ch("s13", "TVA Nouvelles", "CA | News"),
        ch("s14", "ZDF", "DE | News")
    )

    @Test fun sportSammlungenTreffen() {
        val defs = GuideCollectionLogic.defaultCollections().associateBy { it.name }
        fun titles(name: String) = GuideCollectionLogic.itemsOf(defs.getValue(name), sports).map { it.title }
        assertEquals(listOf("RDS", "Sportsnet One", "TSN 2", "TVA Sports"), titles("Sports Canada"))
        assertEquals(listOf("NHL Network"), titles("NHL"))
        assertEquals(listOf("Golf Channel"), titles("Golf"))
        assertEquals(listOf("NFL Network"), titles("NFL"))
        assertEquals(listOf("NBA TV"), titles("NBA"))
        assertEquals(listOf("MLB Network"), titles("Baseball"))
        assertEquals(listOf("beIN Sports"), titles("Soccer"))
        assertEquals(listOf("F1 TV"), titles("Course auto"))
        assertEquals(listOf("Tennis Channel"), titles("Tennis"))
        assertFalse("TVA Nouvelles" in titles("Sports Canada"))
        assertFalse("ZDF" in titles("NHL"))
    }

    @Test fun alteListeWirdUmSportErgaenzt() {
        val onlyQuebec = listOf(quebec)
        val migrated = GuideCollectionLogic.migrateDefaults(onlyQuebec)
        assertEquals(10, migrated.size)
        assertEquals(listOf("Postes québécois", "Sports Canada", "NHL", "Golf", "NFL", "NBA", "Baseball", "Soccer", "Course auto", "Tennis"), migrated.map { it.name })
        // Already upgraded, or Quebec deleted: left alone
        assertEquals(migrated, GuideCollectionLogic.migrateDefaults(migrated))
        val custom = listOf(quebec, CollectionDef(2, "Perso", "📺", listOf("x"), emptyList(), 0))
        assertEquals(custom, GuideCollectionLogic.migrateDefaults(custom))
        assertEquals(emptyList<CollectionDef>(), GuideCollectionLogic.migrateDefaults(emptyList()))
    }

    @Test fun kanadaGruppenZuerst() {
        val groups = listOf("USA Entertainment", "CA: Canada FR", "USA News", "Québec", "CA | Sports")
        assertEquals(
            listOf("CA: Canada FR", "Québec", "CA | Sports", "USA Entertainment", "USA News"),
            GuideCollectionLogic.pinnedFirst(groups)
        )
        // End to end: the Canada group lands right after the collections, before the US groups
        val items = listOf(
            ch("g1", "CNN", "USA News"),
            ch("g2", "ICI RDI", "CA: Canada FR"),
            ch("g3", "ESPN", "USA Entertainment"),
            ch("g4", "TVA", "Québec")
        )
        val cats = guideCategories(items, items)
        assertEquals(listOf("Alle Sender", "CA: Canada FR", "Québec", "USA News", "USA Entertainment"), cats.map { it.name })
    }
}
