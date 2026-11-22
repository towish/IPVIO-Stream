package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelMenuLogicTest {
    private val canadaFirst: (List<String>) -> List<String> = { l -> l.partition { it.startsWith("CA") }.let { it.first + it.second } }

    @Test fun categoriesFollowProviderOrderWithQuebecPinnedAndFavoritesFirst() {
        val groups = listOf("US| News", "CA| Québec", "DE| Sport", "CA| Sports", "UK| Kids")
        val provider = mapOf("DE| Sport" to 0, "US| News" to 1, "UK| Kids" to 2, "CA| Québec" to 3, "CA| Sports" to 4)
        val out = ChannelMenuLogic.categories(groups, provider, listOf("UK| Kids"), listOf("US| News"), canadaFirst) { it }
        assertEquals(listOf("UK| Kids", "CA| Québec", "CA| Sports", "DE| Sport"), out)
    }

    @Test fun withoutPinTheProviderOrderStays() {
        val groups = listOf("B", "A", "C")
        val out = ChannelMenuLogic.categories(groups, mapOf("C" to 0, "A" to 1, "B" to 2), emptyList(), emptyList(), null) { it }
        assertEquals(listOf("C", "A", "B"), out)
    }

    @Test fun userOrderIsAppliedAfterPinning() {
        val out = ChannelMenuLogic.categories(listOf("b", "CA x", "a"), emptyMap(), emptyList(), emptyList(), canadaFirst) { it.sorted() }
        assertEquals(listOf("CA x", "a", "b"), out)
    }

    @Test fun missingProviderOrderKeepsTheGroupOrder() {
        assertEquals(listOf("x", "y"), ChannelMenuLogic.categories(listOf("x", "y"), emptyMap(), emptyList(), emptyList(), null) { it })
    }

    @Test fun firstVisibleKeepsRowsAbove() {
        assertEquals(0, ChannelMenuLogic.firstVisible(-1))
        assertEquals(0, ChannelMenuLogic.firstVisible(2))
        assertEquals(7, ChannelMenuLogic.firstVisible(10))
    }

    @Test fun monogramSkipsPrefixAndQualityTags() {
        assertEquals("TVA", ChannelMenuLogic.monogram("CA: TVA Montréal FHD"))
        assertEquals("CV", ChannelMenuLogic.monogram("Canal Vie HD"))
        assertEquals("HI", ChannelMenuLogic.monogram("Historia"))
        assertEquals("TV5", ChannelMenuLogic.monogram("TV5 Monde"))
        assertEquals("NO", ChannelMenuLogic.monogram("QC| Noovo HD"))
        assertEquals("CNN", ChannelMenuLogic.monogram("US: CNN International"))
        assertEquals("??", ChannelMenuLogic.monogram("??"))
    }

    @Test fun heldKeyGetsFasterOverTime() {
        assertEquals(110L, ChannelMenuLogic.holdGapMs(0))
        assertEquals(110L, ChannelMenuLogic.holdGapMs(999))
        assertEquals(60L, ChannelMenuLogic.holdGapMs(1000))
        assertEquals(35L, ChannelMenuLogic.holdGapMs(2500))
    }
}
