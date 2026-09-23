package com.moblin.android.videoeffects.alerts

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.moblin.android.media.createWav
import com.moblin.android.media.haishinkit.util.ByteReader
import com.moblin.android.various.settings.SettingsAlertsMediaGalleryItem
import com.moblin.android.various.settings.SettingsWidgetAlertsAlert
import com.moblin.android.various.settings.SettingsWidgetAlertsAlertMediaType
import com.moblin.android.various.storages.AlertMediaStorage
import com.moblin.android.videoeffects.EffectImageCiImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.moblin.android.platform.Bundle

sealed class AlertsEffectMediaItem {
    data class BundledName(val name: String) : AlertsEffectMediaItem()

    data class CustomUrl(val url: String) : AlertsEffectMediaItem()

    data class Image(val image: EffectImageCiImage) : AlertsEffectMediaItem()
}

data class AlertsEffectGifImage(
    val image: EffectImageCiImage,
    val timeOffset: Double,
)

data class AlertsEffectPlayer(
    val images: AlertsEffectImages,
    val soundUrl: String?,
)

private val alertsEffectMediaScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class AlertsEffectMedia {
    private var mediaType: SettingsWidgetAlertsAlertMediaType =
        SettingsWidgetAlertsAlertMediaType.gifAndSound
    private var gifImages: ArrayDeque<AlertsEffectGifImage> = ArrayDeque()
    private var videoUrl: String? = null
    private var soundUrl: String? = null

    fun getPlayer(): AlertsEffectPlayer {
        val images: AlertsEffectImages = when (mediaType) {
            SettingsWidgetAlertsAlertMediaType.gifAndSound -> AlertsEffectGifImages(ArrayDeque(gifImages))
            SettingsWidgetAlertsAlertMediaType.video -> AlertsEffectVideoImages(videoUrl)
        }
        return AlertsEffectPlayer(images = images, soundUrl = soundUrl)
    }

    fun update(
        alert: SettingsWidgetAlertsAlert,
        mediaStorage: AlertMediaStorage,
        bundledImages: List<SettingsAlertsMediaGalleryItem>,
        bundledSounds: List<SettingsAlertsMediaGalleryItem>,
    ) {
        if (!alert.enabled) {
            return
        }
        mediaType = alert.mediaType
        when (alert.mediaType) {
            SettingsWidgetAlertsAlertMediaType.gifAndSound ->
                updateGifAndSound(alert, mediaStorage, bundledImages, bundledSounds)

            SettingsWidgetAlertsAlertMediaType.video ->
                updateVideo(alert, mediaStorage)
        }
    }

    private fun updateGifAndSound(
        alert: SettingsWidgetAlertsAlert,
        mediaStorage: AlertMediaStorage,
        bundledImages: List<SettingsAlertsMediaGalleryItem>,
        bundledSounds: List<SettingsAlertsMediaGalleryItem>,
    ) {
        updateGifAndSoundImages(alert, mediaStorage, bundledImages)
        updateGifAndSoundSoundUrl(alert, mediaStorage, bundledSounds)
    }

    private fun updateVideo(alert: SettingsWidgetAlertsAlert, mediaStorage: AlertMediaStorage) {
        videoUrl = null
        soundUrl = null
        val filename = alert.makeVideoFilename() ?: return
        videoUrl = mediaStorage.videos.makePath(filename).path
        val url = videoUrl ?: return
        loadVideoSound(url) {
            soundUrl = it
        }
    }

    private fun updateGifAndSoundImages(
        alert: SettingsWidgetAlertsAlert,
        mediaStorage: AlertMediaStorage,
        bundledImages: List<SettingsAlertsMediaGalleryItem>,
    ) {
        val image: AlertsEffectMediaItem = bundledImages
            .firstOrNull { it.id == alert.imageId }
            ?.let { AlertsEffectMediaItem.BundledName(it.name) }
            ?: AlertsEffectMediaItem.CustomUrl(mediaStorage.makePath(alert.imageId).path)
        val loopCount = alert.imageLoopCount
        alertsEffectMediaScope.launch {
            var images = ArrayDeque<AlertsEffectGifImage>()
            when (image) {
                is AlertsEffectMediaItem.BundledName -> {
                    val url = Bundle.url("Alerts.bundle/${image.name}", "gif")
                    if (url != null) {
                        images = loadGifImages(url, loopCount)
                    }
                }

                is AlertsEffectMediaItem.CustomUrl -> {
                    images = loadGifImages(image.url, loopCount)
                }

                is AlertsEffectMediaItem.Image -> {
                    images = loadGifImages(image.image, loopCount)
                }
            }
            withContext(Dispatchers.Main) {
                this@AlertsEffectMedia.gifImages = images
            }
        }
    }

    private fun updateGifAndSoundSoundUrl(
        alert: SettingsWidgetAlertsAlert,
        mediaStorage: AlertMediaStorage,
        bundledSounds: List<SettingsAlertsMediaGalleryItem>,
    ) {
        val sound: AlertsEffectMediaItem = bundledSounds
            .firstOrNull { it.id == alert.soundId }
            ?.let { AlertsEffectMediaItem.BundledName(it.name) }
            ?: AlertsEffectMediaItem.CustomUrl(mediaStorage.makePath(alert.soundId).path)
        when (sound) {
            is AlertsEffectMediaItem.BundledName ->
                soundUrl = Bundle.url("Alerts.bundle/${sound.name}", "mp3")

            is AlertsEffectMediaItem.CustomUrl ->
                soundUrl = if (File(sound.url).exists()) sound.url else null

            is AlertsEffectMediaItem.Image -> {}
        }
    }

    private fun loadGifImages(url: String, loopCount: Int): ArrayDeque<AlertsEffectGifImage> {
        val images = ArrayDeque<AlertsEffectGifImage>()
        val frames: List<Pair<EffectImageCiImage, Double>> = runCatching {
            File(url).readBytes()
        }.getOrNull()?.let {
            TODO()
        } ?: emptyList()
        var timeOffset = 0.0
        for (i in 0 until loopCount) {
            for (frame in frames) {
                timeOffset += frame.second
                images.addLast(AlertsEffectGifImage(image = frame.first, timeOffset = timeOffset))
            }
        }
        return images
    }

    private fun loadGifImages(image: EffectImageCiImage, loopCount: Int): ArrayDeque<AlertsEffectGifImage> {
        var timeOffset = 0.0
        val images = ArrayDeque<AlertsEffectGifImage>()
        for (i in 0 until loopCount) {
            timeOffset += 1
            images.addLast(AlertsEffectGifImage(image = image, timeOffset = timeOffset))
        }
        return images
    }
}

interface AlertsEffectImages {
    fun getImage(presentationTimeStamp: Double): EffectImageCiImage?

    fun isEmpty(): Boolean
}

class AlertsEffectGifImages : AlertsEffectImages {
    private var images: ArrayDeque<AlertsEffectGifImage> = ArrayDeque()
    private var basePresentationTimeStamp: Double? = null

    constructor()

    constructor(images: ArrayDeque<AlertsEffectGifImage>) {
        this.images = images
    }

    override fun getImage(presentationTimeStamp: Double): EffectImageCiImage? {
        val base = basePresentationTimeStamp ?: presentationTimeStamp.also { basePresentationTimeStamp = it }
        val timeOffset = presentationTimeStamp - base
        while (true) {
            val image = images.firstOrNull() ?: break
            if (timeOffset >= image.timeOffset) {
                images.removeFirst()
                continue
            }
            return image.image
        }
        return null
    }

    override fun isEmpty(): Boolean {
        return images.isEmpty()
    }
}

class AlertsEffectVideoImages : AlertsEffectImages {
    private val reader: AlertsEffectVideoReader?

    constructor(videoUrl: String?) {
        if (videoUrl != null) {
            reader = AlertsEffectVideoReader(videoUrl)
        } else {
            reader = null
        }
    }

    override fun getImage(presentationTimeStamp: Double): EffectImageCiImage? {
        return reader?.getImage(presentationTimeStamp)
    }

    override fun isEmpty(): Boolean {
        return reader?.hasEnded() ?: true
    }
}

private fun loadVideoSound(path: String, onCompleted: (String?) -> Unit) {
    alertsEffectMediaScope.launch {
        val soundUrl: String? = withContext(Dispatchers.IO) {
            var extractor: MediaExtractor? = null
            var codec: MediaCodec? = null
            try {
                val mediaExtractor = MediaExtractor()
                extractor = mediaExtractor
                mediaExtractor.setDataSource(path)
                var audioTrackIndex = -1
                for (index in 0 until mediaExtractor.trackCount) {
                    val trackFormat = mediaExtractor.getTrackFormat(index)
                    val trackMime = trackFormat.getString(MediaFormat.KEY_MIME) ?: continue
                    if (trackMime.startsWith("audio/")) {
                        audioTrackIndex = index
                        break
                    }
                }
                if (audioTrackIndex == -1) {
                    return@withContext null
                }
                mediaExtractor.selectTrack(audioTrackIndex)
                val inputFormat = mediaExtractor.getTrackFormat(audioTrackIndex)
                val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: return@withContext null
                val sampleRate = if (inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                } else {
                    48000
                }
                val channelCount = if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                    inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                } else {
                    1
                }
                val decoder = MediaCodec.createDecoderByType(mime)
                codec = decoder
                decoder.configure(inputFormat, null, null, 0)
                decoder.start()
                val pcm = ByteArrayOutputStream()
                val bufferInfo = MediaCodec.BufferInfo()
                var inputEnded = false
                var outputEnded = false
                while (!outputEnded) {
                    if (!inputEnded) {
                        val inputIndex = decoder.dequeueInputBuffer(10000)
                        if (inputIndex >= 0) {
                            val inputBuffer = decoder.getInputBuffer(inputIndex)
                            if (inputBuffer == null) {
                                inputEnded = true
                            } else {
                                val size = mediaExtractor.readSampleData(inputBuffer, 0)
                                if (size < 0) {
                                    decoder.queueInputBuffer(
                                        inputIndex,
                                        0,
                                        0,
                                        0,
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                                    )
                                    inputEnded = true
                                } else {
                                    decoder.queueInputBuffer(inputIndex, 0, size, mediaExtractor.sampleTime, 0)
                                    mediaExtractor.advance()
                                }
                            }
                        }
                    }
                    val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000)
                    if (outputIndex >= 0) {
                        if (bufferInfo.size > 0) {
                            val outputBuffer = decoder.getOutputBuffer(outputIndex)
                            if (outputBuffer != null) {
                                val chunk = ByteArray(bufferInfo.size)
                                outputBuffer.position(bufferInfo.offset)
                                outputBuffer.get(chunk, 0, bufferInfo.size)
                                outputBuffer.clear()
                                pcm.write(chunk)
                            }
                        }
                        decoder.releaseOutputBuffer(outputIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputEnded = true
                        }
                    }
                }
                val samples = ArrayList<Short>()
                val reader = ByteReader(pcm.toByteArray())
                while (true) {
                    val sample = runCatching { reader.readUInt16Le() }.getOrNull() ?: break
                    samples.add(sample.toShort())
                }
                val interleaved = samples.toShortArray()
                val channels = if (channelCount > 0) channelCount else 1
                val frameCount = interleaved.size / channels
                val mono = ShortArray(frameCount)
                for (frame in 0 until frameCount) {
                    if (channels == 1) {
                        mono[frame] = interleaved[frame]
                    } else {
                        var sum = 0
                        for (channel in 0 until channels) {
                            sum += interleaved[frame * channels + channel].toInt()
                        }
                        mono[frame] = (sum / channels).toShort()
                    }
                }
                val wavSamples = if (sampleRate == 48000 || frameCount == 0) {
                    mono
                } else {
                    val count = (frameCount.toDouble() * 48000.0 / sampleRate).toInt()
                    val resampled = ShortArray(count)
                    val ratio = sampleRate.toDouble() / 48000.0
                    for (i in 0 until count) {
                        val position = i * ratio
                        val index0 = position.toInt().coerceAtMost(frameCount - 1)
                        val index1 = (index0 + 1).coerceAtMost(frameCount - 1)
                        val fraction = position - index0
                        val value = mono[index0] * (1.0 - fraction) + mono[index1] * fraction
                        resampled[i] = value.toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                            .toShort()
                    }
                    resampled
                }
                val wav = createWav(sampleRate = 48000, samples = listOf(wavSamples.toList()))
                val soundFile = File("$path.wav")
                try {
                    if (wav != null) {
                        soundFile.writeBytes(wav)
                        soundFile.path
                    } else {
                        null
                    }
                } catch (error: IOException) {
                    null
                }
            } catch (error: Exception) {
                null
            } finally {
                runCatching { codec?.stop() }
                runCatching { codec?.release() }
                runCatching { extractor?.release() }
            }
        }
        withContext(Dispatchers.Main) {
            onCompleted(soundUrl)
        }
    }
}
