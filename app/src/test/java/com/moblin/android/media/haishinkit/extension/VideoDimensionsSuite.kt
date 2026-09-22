package com.moblin.android.media.haishinkit.extension

import android.util.Size
import kotlin.test.assertEquals
import org.junit.Test

class VideoDimensionsSuite {
    @Test
    fun landscape16x9() {
        val resolution = Size(1920, 1080)
        assertEquals(Size(1280, 720), resolution.convertTo(dimension = 720))
        assertEquals(Size(854, 480), resolution.convertTo(dimension = 480))
        assertEquals(Size(640, 360), resolution.convertTo(dimension = 360))
        assertEquals(Size(284, 160), resolution.convertTo(dimension = 160))
    }

    @Test
    fun portrait9x16() {
        val resolution = Size(1080, 1920)
        assertEquals(Size(720, 1280), resolution.convertTo(dimension = 720))
        assertEquals(Size(480, 854), resolution.convertTo(dimension = 480))
        assertEquals(Size(360, 640), resolution.convertTo(dimension = 360))
        assertEquals(Size(160, 284), resolution.convertTo(dimension = 160))
    }

    @Test
    fun landscape4x3() {
        val resolution = Size(1920, 1440)
        assertEquals(Size(1440, 1080), resolution.convertTo(dimension = 1080))
        assertEquals(Size(960, 720), resolution.convertTo(dimension = 720))
        assertEquals(Size(640, 480), resolution.convertTo(dimension = 480))
        assertEquals(Size(480, 360), resolution.convertTo(dimension = 360))
        assertEquals(Size(214, 160), resolution.convertTo(dimension = 160))
    }

    @Test
    fun portrait4x3() {
        val resolution = Size(1440, 1920)
        assertEquals(Size(1080, 1440), resolution.convertTo(dimension = 1080))
        assertEquals(Size(720, 960), resolution.convertTo(dimension = 720))
        assertEquals(Size(480, 640), resolution.convertTo(dimension = 480))
        assertEquals(Size(360, 480), resolution.convertTo(dimension = 360))
        assertEquals(Size(160, 214), resolution.convertTo(dimension = 160))
    }
}
