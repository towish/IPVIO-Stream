package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrandIntroLogicTest {
    @Test fun startsInvisibleAndTurned() {
        val f = BrandIntroLogic.frame(0f)
        assertEquals(1f, f.overall, 0.001f)
        assertEquals(0f, f.backdrop, 0.001f)
        assertEquals(0f, f.markAlpha, 0.001f)
        assertEquals(BrandIntroLogic.TURN_START_DEG, f.markTurn, 0.001f)
        assertEquals(0f, f.wordAlpha, 0.001f)
        assertNull(f.markSweep)
        assertNull(f.wordSweep)
    }

    @Test fun holdShowsTheFinalBannerLayout() {
        val f = BrandIntroLogic.frame(2800f)
        assertEquals(1f, f.overall, 0.001f)
        assertEquals(1f, f.backdrop, 0.001f)
        assertEquals(1f, f.markAlpha, 0.001f)
        assertEquals(0f, f.markTurn, 0.001f)
        assertEquals(1f, f.markScale, 0.001f)
        assertEquals(1f, f.wordAlpha, 0.001f)
        assertEquals(0f, f.wordShift, 0.001f)
        assertNull(f.markSweep)
        assertNull(f.wordSweep)
    }

    @Test fun turnIsMonotonicAndSweepsRunInOrder() {
        var last = BrandIntroLogic.TURN_START_DEG
        var t = 0f
        while (t <= BrandIntroLogic.TOTAL_MS) {
            val turn = BrandIntroLogic.frame(t).markTurn
            assertTrue("turn went back at $t", turn >= last - 0.0001f)
            last = turn
            t += 25f
        }
        assertNotNull(BrandIntroLogic.frame(1300f).markSweep)
        assertNull(BrandIntroLogic.frame(1300f).wordSweep)
        assertNotNull(BrandIntroLogic.frame(2300f).wordSweep)
        assertNull(BrandIntroLogic.frame(2300f).markSweep)
    }

    @Test fun endsFullyFadedAndClampsOutsideTheTimeline() {
        assertEquals(0f, BrandIntroLogic.frame(BrandIntroLogic.TOTAL_MS.toFloat()).overall, 0.001f)
        assertEquals(0f, BrandIntroLogic.frame(99_999f).overall, 0.001f)
        assertEquals(0f, BrandIntroLogic.frame(-50f).markAlpha, 0.001f)
    }

    @Test fun showsOnlyOnAFreshStartWithMotionAndNoPendingChannel() {
        assertTrue(BrandIntroLogic.shouldShow(freshStart = true, enabled = true, motion = 2, openChannelPending = false))
        assertTrue(BrandIntroLogic.shouldShow(freshStart = true, enabled = true, motion = 1, openChannelPending = false))
        assertFalse(BrandIntroLogic.shouldShow(freshStart = false, enabled = true, motion = 2, openChannelPending = false))
        assertFalse(BrandIntroLogic.shouldShow(freshStart = true, enabled = false, motion = 2, openChannelPending = false))
        assertFalse(BrandIntroLogic.shouldShow(freshStart = true, enabled = true, motion = 0, openChannelPending = false))
        assertFalse(BrandIntroLogic.shouldShow(freshStart = true, enabled = true, motion = 2, openChannelPending = true))
    }
}
