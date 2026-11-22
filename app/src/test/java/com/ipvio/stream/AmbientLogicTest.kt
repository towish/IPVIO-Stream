package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientLogicTest {
    private fun argb(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    private fun parts(c: Int) = Triple((c shr 16) and 0xFF, (c shr 8) and 0xFF, c and 0xFF)

    @Test fun grayPicturesHaveNoAmbientColor() {
        assertNull(AmbientLogic.dominant(IntArray(16) { argb(120, 120, 120) }))
        assertNull(AmbientLogic.dominant(IntArray(16) { 0 }))
    }

    @Test fun colorfulPixelsWinOverGray() {
        val px = IntArray(16) { if (it < 4) argb(230, 30, 40) else argb(90, 90, 90) }
        val (r, g, b) = parts(AmbientLogic.dominant(px)!!)
        assertTrue(r > g && r > b)
    }

    @Test fun resultIsTonedDownForText() {
        // Pure green: saturation capped at 0.75, brightness at 0.55
        assertEquals(Triple(35, 140, 35), parts(AmbientLogic.dominant(IntArray(4) { argb(0, 255, 0) })!!))
    }
}
