package com.moblin.android.various

import android.media.MediaDataSource
import android.media.MediaPlayer
import com.moblin.android.media.haishinkit.util.Atomic
import java.time.Duration
import java.time.Instant

class KeepSpeakerAlivePlayer {
    private var keepSpeakerAlivePlayer: AudioPlayer? = null
    private var latestPlayTime: Atomic<Instant> = Atomic(Instant.now())

    fun audioPlayed() {
        latestPlayTime.mutate { Instant.now() }
    }

    fun playIfNeeded(now: Instant) {
        if (Duration.between(latestPlayTime.value, now) <= Duration.ofSeconds(5 * 60L)) {
            return
        }
        return
    }

    companion object {
        val shared = KeepSpeakerAlivePlayer()
    }
}

class AudioPlayer {
    private val player: MediaPlayer
    private var mediaDataSource: MediaDataSource? = null

    constructor(data: ByteArray) {
        val source = object : MediaDataSource() {
            override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
                if (position >= data.size) {
                    return -1
                }
                val count = minOf(size.toLong(), data.size.toLong() - position).toInt()
                System.arraycopy(data, position.toInt(), buffer, offset, count)
                return count
            }

            override fun getSize(): Long {
                return data.size.toLong()
            }

            override fun close() {
            }
        }
        mediaDataSource = source
        player = MediaPlayer()
        player.setDataSource(source)
        player.prepare()
    }

    constructor(contentsOf: String) {
        player = MediaPlayer()
        player.setDataSource(contentsOf)
        player.prepare()
    }

    fun setDelegate(delegate: Any) {
        player.setOnCompletionListener {
            (delegate as? ChatTextToSpeech)?.audioPlayerDidFinishPlaying(successfully = true)
        }
    }

    fun play() {
        KeepSpeakerAlivePlayer.shared.audioPlayed()
        player.start()
    }

    fun stop() {
        player.stop()
    }
}
