package app.simple.positional.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ArtFrameTest {
    @Test
    fun fillWidthKeepsAspect() {
        val frame = ArtFrame.of(66, 44, 1080)
        assertEquals(ArtFrame(1080, 720, 0, 0, 1080, 720), frame)
    }

    @Test
    fun cropCoversPortraitFrameCentered() {
        val frame = ArtFrame.of(66, 44, 1080, 2400)
        assertEquals(1080, frame.width)
        assertEquals(2400, frame.height)
        assertEquals(0, frame.top)
        assertEquals(2400, frame.bottom)
        assertEquals(-1260, frame.left)
        assertEquals(2340, frame.right)
    }

    @Test
    fun cropCoversLandscapeFrame() {
        val frame = ArtFrame.of(66, 44, 2400, 1080)
        assertEquals(0, frame.left)
        assertEquals(2400, frame.right)
        assertEquals(-260, frame.top)
        assertEquals(1340, frame.bottom)
    }
}
