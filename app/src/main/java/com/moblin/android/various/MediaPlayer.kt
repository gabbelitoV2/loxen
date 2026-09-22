package com.moblin.android.various

import android.util.Log
import com.moblin.android.media.MediaSample
import com.moblin.android.various.settings.SettingsMediaPlayer
import com.moblin.android.various.settings.SettingsMediaPlayerFile
import com.moblin.android.various.storages.MediaPlayerStorage
import com.moblin.android.various.utils.currentPresentationTimeStamp
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

interface MediaPlayerDelegate {
    fun mediaPlayerFileLoaded(playerId: UUID, name: String)

    fun mediaPlayerFileUnloaded(playerId: UUID)

    fun mediaPlayerStateUpdate(
        playerId: UUID,
        name: String,
        playing: Boolean,
        position: Double,
        time: String,
    )

    fun mediaPlayerVideoBuffer(playerId: UUID, sampleBuffer: MediaSample)

    fun mediaPlayerAudioBuffer(playerId: UUID, sampleBuffer: MediaSample)
}

private val mediaPlayerQueue: CoroutineDispatcher =
    Executors.newSingleThreadExecutor().asCoroutineDispatcher()

private val mediaPlayerScope = CoroutineScope(mediaPlayerQueue)

const val mediaPlayerLatency = 0.5

class MediaPlayer(settings: SettingsMediaPlayer, mediaStorage: MediaPlayerStorage) {
    private var asset: Any? = null
    private var reader: Any? = null
    private var videoTrackOutput: Any? = null
    private var audioTrackOutput: Any? = null
    private var settings: SettingsMediaPlayer = settings.clone()
    private var mediaStorage: MediaPlayerStorage = mediaStorage
    private var playing = false
    private var currentFileIndex = 0
    private var assetDuration = 0.0
    private var seeking = false
    private var startVideoTime = 0L
    private var latestVideoTime = 0L
    private var startAudioTime = 0L
    private var latestAudioTime = 0L
    private var outputTimer = SimpleTimer(mediaPlayerQueue)
    private var active = false
    private var filename = ""
    var delegate: MediaPlayerDelegate? = null

    init {
        mediaPlayerScope.launch {
            loadCurrentFile()
        }
    }

    fun deinit() {
        stopOutputTimer()
    }

    fun activate() {
        mediaPlayerScope.launch {
            activateInternal()
        }
    }

    fun deactivate() {
        mediaPlayerScope.launch {
            active = false
        }
    }

    fun updateSettings(settings: SettingsMediaPlayer) {
        val settings = settings.clone()
        mediaPlayerScope.launch {
            updateSettingsInternal(settings)
        }
    }

    fun play() {
        mediaPlayerScope.launch {
            playing = true
            val now = outputPresentationTimeStamp()
            startVideoTime = now - latestVideoTime
            startAudioTime = now - latestAudioTime
        }
    }

    fun pause() {
        mediaPlayerScope.launch {
            playing = false
        }
    }

    fun next() {
        mediaPlayerScope.launch {
            nextInternal()
        }
    }

    fun previous() {
        mediaPlayerScope.launch {
            previousInternal()
        }
    }

    fun seek(position: Double) {
        mediaPlayerScope.launch {
            seekInternal(position)
        }
    }

    fun setSeeking(on: Boolean) {
        mediaPlayerScope.launch {
            seeking = on
        }
    }

    private fun updateSettingsInternal(settings: SettingsMediaPlayer) {
        this.settings = settings
    }

    private fun activateInternal() {
        active = true
        reportState()
    }

    private fun reportState() {
        if (!active) {
            return
        }
        val time = latestVideoTime / 1_000_000.0
        delegate?.mediaPlayerStateUpdate(
            playerId = settings.id,
            name = filename,
            playing = playing,
            position = 100 * time / assetDuration,
            time = formatTime(time),
        )
    }

    private fun nextInternal() {
        currentFileIndex += 1
        if (currentFileIndex >= settings.playlist.size) {
            currentFileIndex = 0
        }
        loadCurrentFile()
    }

    private fun previousInternal() {
        currentFileIndex -= 1
        if (currentFileIndex == -1) {
            currentFileIndex = settings.playlist.size - 1
        }
        loadCurrentFile()
    }

    private fun seekInternal(position: Double) {
        val currentFile = getCurrentFile() ?: return
        delegate?.mediaPlayerStateUpdate(
            playerId = settings.id,
            name = currentFile.name,
            playing = playing,
            position = position,
            time = formatTime(position / 100 * assetDuration),
        )
    }

    private fun loadCurrentFile() {
        stopOutputTimer()
        latestVideoTime = 0L
        if (reader != null) {
            delegate?.mediaPlayerFileUnloaded(playerId = settings.id)
            reportState()
            reader = null
        }
        videoTrackOutput = null
        audioTrackOutput = null
        asset = null
        val currentFile = getCurrentFile() ?: return
        filename = currentFile.name
        val url = mediaStorage.makePath(id = currentFile.id)
        Unit
    }

    private fun loadVideoTrackCompletion(tracks: List<Any?>?, error: Throwable?) {
        if (error != null || tracks.isNullOrEmpty() || asset == null || reader == null) {
            return
        }
        Unit
    }

    private fun loadAudioTrackCompletion(tracks: List<Any?>?, error: Throwable?) {
        if (tracks.isNullOrEmpty()) {
            Log.i(TAG, "media-player: No audio in file.")
            startReading()
            return
        }
        if (error != null || reader == null) {
            Log.i(TAG, "media-player: Some error 2")
            return
        }
        Unit
    }

    private fun startReading() {
        val started: Boolean = TODO("no Android counterpart for AVFoundation AVAssetReader")
        if (reader == null || !started) {
            Log.i(TAG, "media-player: Start reading failed")
            return
        }
        val currentFile = getCurrentFile() ?: return
        delegate?.mediaPlayerFileLoaded(playerId = settings.id, name = currentFile.name)
        reportState()
        startVideoTime = outputPresentationTimeStamp()
        startAudioTime = startVideoTime
        outputVideoBuffer()
        reportState()
        startOutputTimer()
    }

    private fun outputVideoBuffer(): Long? {
        if (videoTrackOutput == null) {
            return null
        }
        val sampleBuffer: MediaSample = TODO("no Android counterpart for AVFoundation CMSampleBuffer")
        latestVideoTime = sampleBuffer.presentationTimeUs
        val presentationTimeStamp = startVideoTime + sampleBuffer.presentationTimeUs
        delegate?.mediaPlayerVideoBuffer(playerId = settings.id, sampleBuffer = sampleBuffer)
        return presentationTimeStamp
    }

    private fun outputAudioBuffer(): Long? {
        if (audioTrackOutput == null) {
            return null
        }
        val sampleBuffer: MediaSample = TODO("no Android counterpart for AVFoundation CMSampleBuffer")
        latestAudioTime = sampleBuffer.presentationTimeUs
        val presentationTimeStamp = startAudioTime + sampleBuffer.presentationTimeUs
        delegate?.mediaPlayerAudioBuffer(playerId = settings.id, sampleBuffer = sampleBuffer)
        return presentationTimeStamp
    }

    private fun startOutputTimer() {
        outputTimer.startPeriodic(interval = 0.3, initial = 0.0) {
            handleOutputTimer()
        }
    }

    private fun stopOutputTimer() {
        outputTimer.stop()
    }

    private fun handleOutputTimer() {
        if (!playing) {
            return
        }
        val now = outputPresentationTimeStamp()
        while (true) {
            val time = outputVideoBuffer()
            if (time != null) {
                if (time >= now) {
                    break
                }
            } else {
                nextInternal()
                break
            }
        }
        if (audioTrackOutput != null) {
            while (true) {
                val time = outputAudioBuffer()
                if (time != null) {
                    if (time >= now) {
                        break
                    }
                } else {
                    nextInternal()
                    break
                }
            }
        }
        reportState()
    }

    private fun formatTime(time: Double): String {
        val time = time.roundToInt()
        val seconds = "%02d".format(time % 60)
        val minutes = time / 60
        return "$minutes:$seconds"
    }

    private fun getCurrentFile(): SettingsMediaPlayerFile? {
        if (currentFileIndex < 0 || currentFileIndex >= settings.playlist.size) {
            Log.i(TAG, "media-player: File index out of range")
            return null
        }
        return settings.playlist[currentFileIndex]
    }
}

private const val TAG = "MediaPlayer"

private fun outputPresentationTimeStamp(): Long {
    return currentPresentationTimeStamp() + (mediaPlayerLatency * 1_000_000).toLong()
}
