package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QcLineupLogicTest {
    private fun num(title: String): Int? = QcLineupLogic.match(title)?.number

    @Test fun recognisesQuebecChannelsWhateverTheProviderWritesAround() {
        assertEquals(103, num("CA: TVA MONTREAL FHD"))
        assertEquals(102, num("QC| Noovo HD"))
        assertEquals(112, num("TVA Sports 2 (CA)"))
        assertEquals(111, num("CA| FR: TVA SPORTS HD"))
        assertEquals(101, num("ICI Télé Québec ᴴᴰ"))
        assertEquals(101, num("[CA] Radio-Canada Montréal"))
        assertEquals(104, num("Télé-Québec HD"))
        assertEquals(105, num("CA: MétéoMédia"))
        assertEquals(109, num("RDS2 HD"))
        assertEquals(110, num("RDS Info"))
        assertEquals(108, num("RDS 1080p"))
        assertEquals(122, num("Séries+ HD"))
        assertEquals(115, num("LCN"))
        assertEquals(117, num("ICI RDI HD"))
        assertEquals(137, num("CA: Canal Vie HD"))
        assertEquals(161, num("Super Écran 1 FHD"))
        assertEquals(143, num("L'Assemblée nationale du Québec"))
    }

    @Test fun keepsTeleQuebecApartFromTheQuebecRegion() {
        assertEquals(QcLineupLogic.Match(104, QcLineupLogic.REGION_NONE), QcLineupLogic.match("Télé-Québec"))
        assertEquals(QcLineupLogic.Match(103, QcLineupLogic.REGION_QUEBEC), QcLineupLogic.match("TVA Québec HD"))
        assertEquals(QcLineupLogic.Match(101, QcLineupLogic.REGION_QUEBEC), QcLineupLogic.match("ICI Télé Québec"))
    }

    @Test fun readsRegionsAndCallLetters() {
        assertEquals(QcLineupLogic.Match(103, QcLineupLogic.REGION_QUEBEC), QcLineupLogic.match("CFCM-DT"))
        assertEquals(QcLineupLogic.Match(103, QcLineupLogic.REGION_MONTREAL), QcLineupLogic.match("CA: TVA CFTM"))
        assertEquals(QcLineupLogic.Match(103, QcLineupLogic.REGION_SHERBROOKE), QcLineupLogic.match("TVA Sherbrooke"))
        assertEquals(QcLineupLogic.Match(103, QcLineupLogic.REGION_OUTAOUAIS), QcLineupLogic.match("TVA Gatineau"))
        assertEquals(QcLineupLogic.Match(103, QcLineupLogic.REGION_OTHER), QcLineupLogic.match("TVA Rivière-du-Loup"))
    }

    @Test fun leavesLookalikesOutOfTheBellSlots() {
        assertNull(num("ABC News Live"))
        assertNull(num("CTV News Channel"))
        assertNull(num("TV5 Monde"))
        assertNull(num("Investigation Discovery"))
        assertNull(num("FR: Canal Historia"))
        assertNull(num("DE: Das Erste HD"))
        assertNull(num("UK: CBS Reality"))
        assertNull(num(""))
    }

    @Test fun sortsQuebecFirstInBellOrderAndKeepsTheRestInProviderOrder() {
        val input = listOf(
            "DE: Das Erste", "CA: TVA Montréal", "US: CNN", "CA: Noovo", "CA: RDS", "CA: ICI Télé Montréal",
            "UK: BBC One", "CA: TVA Québec", "CA: Télé-Québec", "CA: CTV Montreal", "CA: TVA Sports"
        )
        val out = QcLineupLogic.sort(input, QcLineupLogic.REGION_QUEBEC) { it }
        assertEquals(
            listOf(
                "CA: ICI Télé Montréal", "CA: Noovo", "CA: TVA Québec", "CA: TVA Montréal", "CA: Télé-Québec",
                "CA: RDS", "CA: TVA Sports", "CA: CTV Montreal",
                "DE: Das Erste", "US: CNN", "UK: BBC One"
            ),
            out
        )
    }

    @Test fun preferredRegionComesFirstThenNationalThenMontreal() {
        val input = listOf("TVA Sherbrooke", "TVA Montréal", "TVA HD", "TVA Québec")
        assertEquals(listOf("TVA Québec", "TVA HD", "TVA Montréal", "TVA Sherbrooke"), QcLineupLogic.sort(input, QcLineupLogic.REGION_QUEBEC) { it })
        assertEquals(listOf("TVA Montréal", "TVA HD", "TVA Sherbrooke", "TVA Québec"), QcLineupLogic.sort(input, QcLineupLogic.REGION_MONTREAL) { it })
    }

    @Test fun sortIsStableAndLosesNothing() {
        val input = listOf("TVA HD", "TVA FHD", "TVA SD", "X", "Y", "TVA 4K")
        val out = QcLineupLogic.sort(input, QcLineupLogic.REGION_QUEBEC) { it }
        assertEquals(listOf("TVA HD", "TVA FHD", "TVA SD", "TVA 4K", "X", "Y"), out)
        assertEquals(input.sorted(), out.sorted())
    }

    @Test fun listWithoutQuebecChannelsIsReturnedUnchanged() {
        val input = listOf("DE: ARD", "DE: ZDF", "FR: TF1")
        assertEquals(input, QcLineupLogic.sort(input, QcLineupLogic.REGION_QUEBEC) { it })
    }

    @Test fun stripsShortProviderPrefixesOnly() {
        assertEquals("TVA", QcLineupLogic.stripPrefix("CA: TVA"))
        assertEquals("TVA", QcLineupLogic.stripPrefix("CA| FR: TVA"))
        assertEquals("TVA", QcLineupLogic.stripPrefix("[CA] TVA"))
        assertEquals("TVA Sports: Hockey", QcLineupLogic.stripPrefix("TVA Sports: Hockey"))
    }

    @Test fun everyBellNumberIsUnique() {
        val n = QcLineupLogic.LINEUP.map { it.number }
        assertEquals(n.size, n.toSet().size)
    }

    @Test fun bellNumbersForRecognisedChannelsAndFrom1000ForTheRest() {
        val input = listOf("DE: Das Erste", "CA: TVA Montréal", "CA: Noovo", "CA: TVA Québec", "US: CNN", "CA: RDS")
        val r = QcLineupLogic.sortNumbered(input, QcLineupLogic.REGION_QUEBEC) { it }
        assertEquals(listOf("CA: Noovo", "CA: TVA Québec", "CA: TVA Montréal", "CA: RDS", "DE: Das Erste", "US: CNN"), r.items)
        // The preferred TVA keeps 103, the second TVA version and all other channels count from 1000
        assertEquals(listOf(102, 103, 1000, 108, 1001, 1002), r.numbers.toList())
    }

    @Test fun withoutQuebecChannelsNumbersArePositions() {
        val input = listOf("DE: ARD", "DE: ZDF", "FR: TF1")
        val r = QcLineupLogic.sortNumbered(input, QcLineupLogic.REGION_QUEBEC) { it }
        assertEquals(input, r.items)
        assertEquals(listOf(1, 2, 3), r.numbers.toList())
    }

    @Test fun numbersAreUnique() {
        val input = (1..50).map { if (it % 5 == 0) "TVA HD $it" else "X $it" }
        val nums = QcLineupLogic.sortNumbered(input, QcLineupLogic.REGION_QUEBEC) { it }.numbers.toList()
        assertEquals(nums.size, nums.toSet().size)
    }
}
