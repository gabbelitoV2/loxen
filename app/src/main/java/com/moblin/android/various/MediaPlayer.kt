package com.moblin.android.various

import android.util.Log
import com.moblin.android.media.MediaSample
import com.moblin.android.platform.avfoundation.AVAsset
import com.moblin.android.platform.avfoundation.AVAssetReader
import com.moblin.android.platform.avfoundation.AVAssetReaderTrackOutput
import com.moblin.android.platform.avfoundation.AVAssetTrack
import com.moblin.android.platform.avfoundation.AVFormatIDKey
import com.moblin.android.platform.avfoundation.AVMediaType
import com.moblin.android.platform.avfoundation.AVSampleRateKey
import com.moblin.android.platform.avfoundation.kAudioFormatLinearPCM
import com.moblin.android.platform.video.kCVPixelBufferIOSurfacePropertiesKey
import com.moblin.android.platform.video.kCVPixelBufferMetalCompatibilityKey
import com.moblin.android.platform.video.kCVPixelBufferPixelFormatTypeKey
import com.moblin.android.platform.video.kCVPixelFormatType_32BGRA
import com.moblin.android.various.settings.SettingsMediaPlayer
import com.moblin.android.various.settings.SettingsMediaPlayerFile
import com.moblin.android.various.storages.MediaPlayerStorage
import com.moblin.android.various.utils.currentPresentationTimeStamp
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

private const val TAG = "MediaPlayer"

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

val mediaPlayerLatency: Double = 0.5

open class MediaPlayer(settings: SettingsMediaPlayer, mediaStorage: MediaPlayerStorage) : AutoCloseable {
    private var asset: AVAsset? = null
    private var reader: AVAssetReader? = null
    private var videoTrackOutput: AVAssetReaderTrackOutput? = null
    private var audioTrackOutput: AVAssetReaderTrackOutput? = null
    private var settings: SettingsMediaPlayer = settings.clone()
    private var mediaStorage: MediaPlayerStorage = mediaStorage
    private var playing = false
    private var currentFileIndex = 0
    private var assetDuration = 0.0
    private var seeking = false
    private var startVideoTime: Long = 0L
    private var latestVideoTime: Long = 0L
    private var startAudioTime: Long = 0L
    private var latestAudioTime: Long = 0L
    private var outputTimer: SimpleTimer = SimpleTimer(queue = mediaPlayerQueue)
    private var active = false
    private var filename = ""
    open var delegate: MediaPlayerDelegate? = null

    init {
        CoroutineScope(mediaPlayerQueue).launch {
            loadCurrentFile()
        }
    }

    override fun close() {
        stopOutputTimer()
        CoroutineScope(mediaPlayerQueue).launch { reader?.cancelReading() }
    }

    open fun activate() {
        CoroutineScope(mediaPlayerQueue).launch {
            activateInternal()
        }
    }

    open fun deactivate() {
        CoroutineScope(mediaPlayerQueue).launch {
            active = false
        }
    }

    open fun updateSettings(settings: SettingsMediaPlayer) {
        val clonedSettings = settings.clone()
        CoroutineScope(mediaPlayerQueue).launch {
            updateSettingsInternal(clonedSettings)
        }
    }

    open fun play() {
        CoroutineScope(mediaPlayerQueue).launch {
            playing = true
            val now = outputPresentationTimeStamp()
            startVideoTime = now - latestVideoTime
            startAudioTime = now - latestAudioTime
        }
    }

    open fun pause() {
        CoroutineScope(mediaPlayerQueue).launch {
            playing = false
        }
    }

    open fun next() {
        CoroutineScope(mediaPlayerQueue).launch {
            nextInternal()
        }
    }

    open fun previous() {
        CoroutineScope(mediaPlayerQueue).launch {
            previousInternal()
        }
    }

    open fun seek(position: Double) {
        CoroutineScope(mediaPlayerQueue).launch {
            seekInternal(position)
        }
    }

    open fun setSeeking(on: Boolean) {
        CoroutineScope(mediaPlayerQueue).launch {
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
        reader?.cancelReading()
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
        asset = AVAsset(url = url)
        val asset = asset ?: run {
            Log.i(TAG, "media-player: No asset $url")
            return
        }
        try {
            reader = AVAssetReader(asset = asset)
        } catch (error: Throwable) {
            Log.i(TAG, "media-player: Failed to create reader with error: $error")
        }
        assetDuration = max(asset.duration(), 1.0)
        asset.loadTracks(withMediaType = AVMediaType.video) { tracks, error ->
            CoroutineScope(mediaPlayerQueue).launch {
                loadVideoTrackCompletion(tracks, error)
            }
        }
    }

    private fun loadVideoTrackCompletion(tracks: List<AVAssetTrack>?, error: Throwable?) {
        if (error != null) {
            return
        }
        val videoTrack = tracks?.firstOrNull() ?: return
        val asset = this.asset ?: return
        val reader = this.reader ?: return
        val videoOutputSettings: Map<String, Any> = mapOf(
            kCVPixelBufferPixelFormatTypeKey to com.moblin.android.media.haishinkit.media.video.pixelFormatType,
            kCVPixelBufferIOSurfacePropertiesKey to emptyMap<String, Any>(),
            kCVPixelBufferMetalCompatibilityKey to true,
        )
        val videoOutput = AVAssetReaderTrackOutput(
            track = videoTrack,
            outputSettings = videoOutputSettings,
        )
        videoTrackOutput = videoOutput
        videoOutput.leasesSampleBuffers = true
        reader.add(output = videoOutput)
        asset.loadTracks(withMediaType = AVMediaType.audio) { audioTracks, audioError ->
            CoroutineScope(mediaPlayerQueue).launch {
                loadAudioTrackCompletion(audioTracks, audioError)
            }
        }
    }

    private fun loadAudioTrackCompletion(tracks: List<AVAssetTrack>?, error: Throwable?) {
        val audioTrack = tracks?.firstOrNull()
        if (audioTrack == null) {
            Log.i(TAG, "media-player: No audio in file.")
            startReading()
            return
        }
        if (error != null) {
            Log.i(TAG, "media-player: Some error 2")
            return
        }
        val reader = this.reader ?: return
        val audioOutputSettings: Map<String, Any> = mapOf(
            AVFormatIDKey to kAudioFormatLinearPCM,
            AVSampleRateKey to 48000.0,
        )
        val audioOutput = AVAssetReaderTrackOutput(
            track = audioTrack,
            outputSettings = audioOutputSettings,
        )
        audioTrackOutput = audioOutput
        reader.add(output = audioOutput)
        startReading()
    }

    private fun startReading() {
        val reader = this.reader
        if (reader == null || !reader.startReading()) {
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
        val sampleBuffer = videoTrackOutput?.copyNextSampleBuffer() ?: return null
        latestVideoTime = sampleBuffer.presentationTimeUs
        val presentationTimeStamp = startVideoTime + sampleBuffer.presentationTimeUs
        val newSampleBuffer = sampleBuffer.replacePresentationTimeStamp(presentationTimeStamp)
        delegate?.mediaPlayerVideoBuffer(playerId = settings.id, sampleBuffer = newSampleBuffer)
        com.moblin.android.platform.video.releaseLease(newSampleBuffer)
        return presentationTimeStamp
    }

    private fun outputAudioBuffer(): Long? {
        val sampleBuffer = audioTrackOutput?.copyNextSampleBuffer() ?: return null
        latestAudioTime = sampleBuffer.presentationTimeUs
        val presentationTimeStamp = startAudioTime + sampleBuffer.presentationTimeUs
        val newSampleBuffer = sampleBuffer.replacePresentationTimeStamp(presentationTimeStamp)
        delegate?.mediaPlayerAudioBuffer(playerId = settings.id, sampleBuffer = newSampleBuffer)
        return presentationTimeStamp
    }

    private fun startOutputTimer() {
        val (weakSelf, timer) = java.lang.ref.WeakReference(this) to outputTimer
        outputTimer.startPeriodic(interval = 0.3, initial = 0.0) {
            weakSelf.get()?.handleOutputTimer() ?: timer.stop()
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
        val seconds = String.format(Locale.US, "%02d", time % 60)
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

private fun outputPresentationTimeStamp(): Long =
    currentPresentationTimeStamp() + (mediaPlayerLatency * 1_000_000.0).toLong()
