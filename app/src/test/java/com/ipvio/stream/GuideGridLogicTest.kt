package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuideGridLogicTest {
    @Test fun dayPatternPerLanguage() {
        assertEquals("EE dd.MM.", GuideGridLogic.dayPattern("de"))
        assertEquals("EEE d MMM", GuideGridLogic.dayPattern("fr"))
        assertEquals("EEE MMM d", GuideGridLogic.dayPattern("en"))
        assertEquals("EEE d.M.", GuideGridLogic.dayPattern("hr"))
        assertEquals("EEE d MMM", GuideGridLogic.dayPattern("xx"))
    }

    @Test fun dayPatternFormatsInFrench() {
        val f = java.text.SimpleDateFormat(GuideGridLogic.dayPattern("fr"), java.util.Locale.CANADA_FRENCH)
        f.timeZone = java.util.TimeZone.getTimeZone("America/Toronto")
        // 22 Nov 2026, 12:00 in Montréal
        assertEquals("dim. 22 nov.", f.format(java.util.Date(1_795_366_800_000L)))
    }

    @Test fun headerMarksOnlyWhenTheyFit() {
        assertFalse(GuideGridLogic.labelFits(-4f, 62f, 600f))
        assertTrue(GuideGridLogic.labelFits(0f, 62f, 600f))
        assertTrue(GuideGridLogic.labelFits(538f, 62f, 600f))
        assertFalse(GuideGridLogic.labelFits(540f, 62f, 600f))
    }

    @Test fun blockWidthAndRadius() {
        assertEquals(102f, GuideGridLogic.blockWidth(1800, 3.5f), 0.01f)
        assertEquals(4f, GuideGridLogic.blockWidth(30, 3.5f), 0.01f)
        assertEquals(12f, GuideGridLogic.blockRadius(102f, 12f), 0.01f)
        assertEquals(2f, GuideGridLogic.blockRadius(4f, 12f), 0.01f)
    }

    @Test fun shortFillersAreLeftOut() {
        val l = listOf(0L to 1800L, 1800L to 1860L, 1860L to 3600L)
        assertEquals(listOf(0L to 1800L, 1860L to 3600L), GuideGridLogic.visible(l, { it.first }, { it.second }))
        val ok = listOf(0L to 1800L)
        assertTrue(GuideGridLogic.visible(ok, { it.first }, { it.second }) === ok)
    }

    @Test fun linesThatFit() {
        assertEquals(4, GuideGridLogic.linesThatFit(80f, 19f))
        assertEquals(1, GuideGridLogic.linesThatFit(10f, 19f))
        assertEquals(1, GuideGridLogic.linesThatFit(10f, 0f))
    }
}
