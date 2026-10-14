package com.moblin.android.various.model

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModelMusicSuite {
    @Test
    fun songUrl() {
        val url = "https://music.apple.com/us/song/bicycle-race/6781079509"
        assertEquals("6781079509", appleMusicSongId(url))
    }

    @Test
    fun albumUrlWithSong() {
        val url = "https://music.apple.com/us/album/jazz/6781079000?i=6781079509"
        assertEquals("6781079509", appleMusicSongId(url))
    }

    @Test
    fun albumUrlWithoutSong() {
        val url = "https://music.apple.com/us/album/jazz/6781079000"
        assertNull(appleMusicSongId(url))
    }
}
