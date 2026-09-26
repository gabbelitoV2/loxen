package com.moblin.android.various

import android.media.MediaPlayer
import com.moblin.android.platform.Bundle
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.util.DataSource

@RunWith(RobolectricTestRunner::class)
class KeepSpeakerAliveSuite {
    private fun field(target: Any, name: String): Any? {
        val field = target.javaClass.getDeclaredField(name)
        field.isAccessible = true
        return field.get(target)
    }

    @Test
    fun playsSilenceOnlyAfterFiveMinutesWithoutAudio() {
        val path = assertNotNull(Bundle.url("Alerts.bundle/Silence", "mp3"))
        ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(path), ShadowMediaPlayer.MediaInfo(1000, 0))
        val keepAlive = KeepSpeakerAlivePlayer.shared
        keepAlive.audioPlayed()
        val initial = field(keepAlive, "keepSpeakerAlivePlayer")
        keepAlive.playIfNeeded(now = Instant.now().plusSeconds(4 * 60))
        assertSame(initial, field(keepAlive, "keepSpeakerAlivePlayer"))
        keepAlive.playIfNeeded(now = Instant.now().plusSeconds(5 * 60 + 1))
        val silence = assertNotNull(field(keepAlive, "keepSpeakerAlivePlayer"))
        assertNotSame(initial, silence)
        val mediaPlayer = field(silence, "player") as MediaPlayer
        assertEquals(ShadowMediaPlayer.State.STARTED, shadowOf(mediaPlayer).state)
        keepAlive.playIfNeeded(now = Instant.now().plusSeconds(60))
        assertSame(silence, field(keepAlive, "keepSpeakerAlivePlayer"))
    }
}
