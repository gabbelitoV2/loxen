package com.moblin.android.media.haishinkit.media

import android.graphics.Bitmap
import android.media.Image
import android.media.MediaFormat
import android.util.Log
import android.util.Size
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.codec.audio.AudioEncoder
import com.moblin.android.media.haishinkit.codec.audio.AudioEncoderDelegate
import com.moblin.android.media.haishinkit.codec.audio.AudioEncoderSettings
import com.moblin.android.media.haishinkit.codec.video.VideoEncoder
import com.moblin.android.media.haishinkit.codec.video.VideoEncoderDelegate
import com.moblin.android.media.haishinkit.codec.video.VideoEncoderSettings
import com.moblin.android.media.haishinkit.media.audio.AudioUnit
import com.moblin.android.media.haishinkit.media.audio.AudioUnitAttachParams
import com.moblin.android.media.haishinkit.media.video.PreviewView
import com.moblin.android.media.haishinkit.media.video.SceneSwitchTransition
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoUnit
import com.moblin.android.media.haishinkit.media.video.VideoUnitAttachParams
import com.moblin.android.various.settings.SettingsGraphicsImplementation
import java.net.URI
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

interface ProcessorDelegate {
    fun streamAudioLevel(audioLevel: Float, numberOfAudioChannels: Int, sampleRate: Double)
    fun streamLowFpsImage(lowFpsImage: ByteArray?, frameNumber: Long)
    fun streamVideoAttachCameraError()
    fun streamVideoCaptureSessionError(message: String)
    fun streamVideoBufferedVideoReady(cameraId: UUID)
    fun streamVideoBufferedVideoRemoved(cameraId: UUID)
    fun streamVideoFps(fps: Int)
    fun streamVideoEncoderResolution(resolution: Size)
    fun streamRecorderInitSegment(data: ByteArray)
    fun streamRecorderDataSegment(segment: RecorderDataSegment)
    fun streamRecorderFinished()
    fun streamAudio(sampleBuffer: MediaSample)
    fun streamNoTorch()
    fun streamSetZoomX(x: Float)
    fun streamSetExposureBias(bias: Float)
    fun streamSelectedFps(auto: Boolean)
}

interface AudioVideoEncoderDelegate : AudioEncoderDelegate, VideoEncoderDelegate

private val mainScope = CoroutineScope(Dispatchers.Main)

val processorControlQueue = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

val processorPipelineQueue = CoroutineScope(
    Executors.newSingleThreadExecutor().asCoroutineDispatcher()
)

private class Stream(var delegate: AudioVideoEncoderDelegate? = null)

class Processor(val delegate: ProcessorDelegate) :
    AudioEncoderDelegate,
    VideoEncoderDelegate,
    RecorderDelegate {
    val audio = AudioUnit()
    val video = VideoUnit()
    val recorder = Recorder()
    private val streams = mutableListOf<Stream>()
    private val driftTrackers = mutableMapOf<UUID, DriftTracker>()

    init {
        audio.processor = this
        video.processor = this
        recorder.delegate = this
    }

    fun setTorch(value: Boolean) {
        processorControlQueue.launch {
            video.torch = value
        }
    }

    fun setTorchLevel(value: Float) {
        processorControlQueue.launch {
            video.torchLevel = value
        }
    }

    fun setFps(value: Double, preferAutoFps: Boolean) {
        processorControlQueue.launch {
            video.setFps(value, preferAutoFps)
        }
    }

    fun getFps(): Double {
        return video.getFps()
    }

    fun setColorSpace(colorSpace: Int, onComplete: () -> Unit) {
        processorControlQueue.launch {
            video.setColorSpace(colorSpace)
            mainScope.launch {
                onComplete()
            }
        }
    }

    fun setVideoSize(capture: Size, canvas: Size) {
        processorControlQueue.launch {
            video.setSize(capture, canvas)
        }
    }

    fun setVideoOrientation(value: Int) {
        processorControlQueue.launch {
            video.videoOrientation = value
        }
    }

    fun setGraphicsImplementation(value: SettingsGraphicsImplementation) {
        processorControlQueue.launch {
            video.setGraphicsImplementation(value)
        }
    }

    fun setHasAudio(value: Boolean) {
        processorControlQueue.launch {
            audio.muted = !value
        }
    }

    fun setAudioGain(gain: Float) {
        processorControlQueue.launch {
            audio.gain = gain
        }
    }

    fun setAudioDelay(delay: Double) {
        audio.setDelay(delay)
    }

    fun setAudioEncoderSettings(settings: AudioEncoderSettings) {
        audio.encoder.setSettings(settings)
    }

    fun setVideoEncoderSettings(settings: VideoEncoderSettings) {
        video.encoder.settings = settings
    }

    fun attachCamera(
        params: VideoUnitAttachParams,
        onError: ((Throwable) -> Unit)? = null,
        onSuccess: (() -> Unit)? = null,
    ) {
        processorControlQueue.launch {
            try {
                attachCameraInternal(params)
                mainScope.launch {
                    onSuccess?.invoke()
                }
            } catch (e: Exception) {
                mainScope.launch {
                    onError?.invoke(e)
                }
            }
        }
    }

    fun attachAudio(
        params: AudioUnitAttachParams,
        onError: ((Throwable) -> Unit)? = null,
    ) {
        processorControlQueue.launch {
            try {
                attachAudioInternal(params)
            } catch (e: Exception) {
                mainScope.launch {
                    onError?.invoke(e)
                }
            }
        }
    }

    fun setCameraControls(enabled: Boolean) {
        processorControlQueue.launch {
            video.setCameraControl(enabled)
        }
    }

    fun addBufferedVideo(cameraId: UUID, name: String, latency: Double) {
        video.addBufferedVideo(cameraId, name, latency)
    }

    fun removeBufferedVideo(cameraId: UUID) {
        video.removeBufferedVideo(cameraId)
    }

    fun appendBufferedVideoSampleBuffer(cameraId: UUID, sampleBuffer: MediaSample) {
        video.appendBufferedVideoSampleBuffer(cameraId, sampleBuffer)
    }

    fun addBufferedAudio(cameraId: UUID, name: String, latency: Double) {
        audio.addBufferedAudio(cameraId, name, latency)
    }

    fun removeBufferedAudio(cameraId: UUID) {
        audio.removeBufferedAudio(cameraId)
    }

    fun appendBufferedAudioSampleBuffer(cameraId: UUID, sampleBuffer: MediaSample) {
        audio.appendBufferedAudioSampleBuffer(cameraId, sampleBuffer)
    }

    fun registerVideoEffect(effect: VideoEffect) {
        video.registerEffect(effect)
    }

    fun registerVideoEffectBack(effect: VideoEffect) {
        video.registerEffectBack(effect)
    }

    fun unregisterVideoEffect(effect: VideoEffect) {
        video.unregisterEffect(effect)
    }

    fun unregisterAllVideoEffects() {
        video.unregisterAllEffects()
    }

    fun setPendingAfterAttachEffects(effects: List<VideoEffect>, rotation: Double, mirror: Boolean) {
        video.setPendingAfterAttachEffects(effects, rotation, mirror)
    }

    fun usePendingAfterAttachEffects() {
        video.usePendingAfterAttachEffects()
    }

    fun setScreenPreview(enabled: Boolean) {
        video.setScreenPreview(enabled)
    }

    fun setShowCameraPreview(show: Boolean) {
        video.setShowCameraPreview(show)
    }

    fun setVideoPreviewEnabled(enabled: Boolean) {
        video.setVideoPreviewEnabled(enabled)
    }

    fun setVideoPreview(cameraId: UUID, drawable: PreviewView) {
        video.setVideoPreview(cameraId, drawable)
    }

    fun removeAllVideoPreviews() {
        video.removeAllVideoPreviews()
    }

    fun setLowFpsImage(fps: Float) {
        video.setLowFpsImage(fps)
    }

    fun setSceneSwitchTransition(sceneSwitchTransition: SceneSwitchTransition) {
        video.setSceneSwitchTransition(sceneSwitchTransition)
    }

    fun takeSnapshot(age: Float, onComplete: (Bitmap, Image, Image) -> Unit) {
        video.takeSnapshot(age, onComplete)
    }

    fun takePhoto() {
        video.takePhoto()
    }

    fun takeVideoSourceSnapshot(videoSourceId: UUID, onComplete: (Bitmap?) -> Unit) {
        video.takeVideoSourceSnapshot(videoSourceId, onComplete)
    }

    fun setCleanRecordings(enabled: Boolean) {
        video.setCleanRecordings(enabled)
    }

    fun setCleanSnapshots(enabled: Boolean) {
        video.setCleanSnapshots(enabled)
    }

    fun setCleanExternalDisplay(enabled: Boolean) {
        video.setCleanExternalDisplay(enabled)
    }

    fun setAudioChannelsMap(map: Map<Int, Int>) {
        recorder.setAudioChannelsMap(map)
    }

    fun setSpeechToText(enabled: Boolean) {
        audio.setSpeechToText(enabled)
    }

    fun setTalkback(cameraId: UUID?) {
        audio.setTalkback(cameraId)
    }

    fun startRecording(
        url: URI?,
        replay: Boolean,
        audioSettings: Map<String, Any>,
        videoSettings: Map<String, Any>,
    ) {
        recorder.startRunning(
            url,
            replay,
            audioSettings,
            videoSettings,
        )
    }

    fun stopRecording() {
        recorder.stopRunning()
    }

    fun setUrl(url: URI?) {
        recorder.setUrl(url)
    }

    fun setReplayBuffering(enabled: Boolean) {
        recorder.setReplayBuffering(enabled)
    }

    fun stop() {
        processorControlQueue.launch {
            stopRunning()
        }
    }

    fun startEncoding(delegate: AudioVideoEncoderDelegate) {
        streams.add(Stream(delegate))
        Log.i("Processor", "processor: Starting encoding")
        video.startEncoding(this)
        audio.startEncoding(this)
    }

    fun stopEncoding(delegate: AudioVideoEncoderDelegate) {
        streams.removeAll { it.delegate === delegate }
        if (streams.isEmpty()) {
            Log.i("Processor", "processor: Stopping encoding")
            video.stopEncoding()
            audio.stopEncoding()
        }
    }

    fun startPreviewEncoding(
        delegate: AudioVideoEncoderDelegate,
        videoSettings: VideoEncoderSettings,
        audioSettings: AudioEncoderSettings,
    ) {
        video.startPreviewEncoding(delegate, videoSettings)
        audio.startPreviewEncoding(delegate, audioSettings)
    }

    fun stopPreviewEncoding() {
        video.stopPreviewEncoding()
        audio.stopPreviewEncoding()
    }

    fun startRunning() {
        video.startRunning()
        audio.startRunning()
    }

    fun stopRunning() {
        video.stopRunning()
        audio.stopRunning()
    }

    fun setDrawable(drawable: PreviewView?) {
        video.drawable = drawable
    }

    fun setExternalDisplayDrawable(drawable: PreviewView?) {
        video.externalDisplayDrawable = drawable
    }

    fun getAudioEncoder(): AudioEncoder {
        return audio.encoder
    }

    fun getVideoEncoder(): VideoEncoder {
        return video.encoder
    }

    fun driftTracker(cameraId: UUID, name: String): DriftTracker {
        driftTrackers[cameraId]?.let { return it }
        val driftTracker = DriftTracker(name)
        driftTrackers[cameraId] = driftTracker
        return driftTracker
    }

    fun removeDriftTracker(cameraId: UUID) {
        driftTrackers.remove(cameraId)
    }

    private fun attachCameraInternal(params: VideoUnitAttachParams) {
        video.attach(params)
    }

    private fun attachAudioInternal(params: AudioUnitAttachParams) {
        audio.attach(params)
    }

    override fun audioEncoderOutputFormat(format: MediaFormat) {
        for (stream in streams) {
            stream.delegate?.audioEncoderOutputFormat(format)
        }
    }

    override fun audioEncoderOutputBuffer(buffer: MediaSample, presentationTimeStamp: Long) {
        for (stream in streams) {
            stream.delegate?.audioEncoderOutputBuffer(buffer, presentationTimeStamp)
        }
    }

    override fun videoEncoderOutputFormat(encoder: VideoEncoder, formatDescription: MediaFormat) {
        for (stream in streams) {
            stream.delegate?.videoEncoderOutputFormat(encoder, formatDescription)
        }
    }

    override fun videoEncoderOutputSampleBuffer(
        encoder: VideoEncoder,
        sampleBuffer: MediaSample,
        decodeTimeStampOffset: Long,
    ) {
        for (stream in streams) {
            stream.delegate?.videoEncoderOutputSampleBuffer(encoder, sampleBuffer, decodeTimeStampOffset)
        }
    }

    override fun recorderInitSegment(data: ByteArray) {
        delegate.streamRecorderInitSegment(data)
    }

    override fun recorderDataSegment(segment: RecorderDataSegment) {
        delegate.streamRecorderDataSegment(segment)
    }

    override fun recorderFinished() {
        delegate.streamRecorderFinished()
    }
}
