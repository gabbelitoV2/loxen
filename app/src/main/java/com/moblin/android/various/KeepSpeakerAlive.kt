package com.moblin.android.various

import android.media.MediaDataSource
import android.media.MediaPlayer
import com.moblin.android.media.haishinkit.util.Atomic
import com.moblin.android.platform.Bundle
import java.time.Duration
import java.time.Instant

open class KeepSpeakerAlivePlayer {
    private var keepSpeakerAlivePlayer: AudioPlayer? = null
    private var latestPlayTime: Atomic<Instant> = Atomic(Instant.now())

    open fun audioPlayed() {
        latestPlayTime.mutate { it.value = Instant.now() }
    }

    open fun playIfNeeded(now: Instant) {
        if (Duration.between(latestPlayTime.value, now).seconds <= 5 * 60) {
            return
        }
        val soundUrl = Bundle.url("Alerts.bundle/Silence", "mp3") ?: return
        keepSpeakerAlivePlayer = runCatching { AudioPlayer(contentsOf = soundUrl) }.getOrNull()
        keepSpeakerAlivePlayer?.play()
    }

    companion object {
        val shared = KeepSpeakerAlivePlayer()
    }
}

open class AudioPlayer {
    private val player: MediaPlayer
    private var dataSource: MediaDataSource? = null

    constructor(data: ByteArray) {
        val source = AudioPlayerDataSource(data)
        dataSource = source
        player = MediaPlayer()
        player.setDataSource(source)
        player.prepare()
    }

    constructor(contentsOf: String) {
        player = MediaPlayer()
        player.setDataSource(contentsOf)
        player.prepare()
    }

    open fun setDelegate(delegate: Any) {
        player.setOnCompletionListener {
            (delegate as? ChatTextToSpeech)?.audioPlayerDidFinishPlaying(successfully = true)
        }
    }

    open fun play() {
        KeepSpeakerAlivePlayer.shared.audioPlayed()
        player.start()
    }

    open fun stop() {
        player.stop()
    }
}

private class AudioPlayerDataSource(private val data: ByteArray) : MediaDataSource() {
    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (position >= data.size) {
            return -1
        }
        val count = minOf(size.toLong(), data.size - position).toInt()
        System.arraycopy(data, position.toInt(), buffer, offset, count)
        return count
    }

    override fun getSize(): Long {
        return data.size.toLong()
    }

    override fun close() {
    }
}
