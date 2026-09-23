package com.moblin.android.videoeffects.alerts

import com.moblin.android.media.createWav
import com.moblin.android.media.haishinkit.util.ByteReader
import com.moblin.android.platform.Bundle
import com.moblin.android.platform.avfoundation.AVAsset
import com.moblin.android.platform.avfoundation.AVAssetReader
import com.moblin.android.platform.avfoundation.AVAssetReaderTrackOutput
import com.moblin.android.platform.avfoundation.AVFormatIDKey
import com.moblin.android.platform.avfoundation.AVLinearPCMBitDepthKey
import com.moblin.android.platform.avfoundation.AVLinearPCMIsBigEndianKey
import com.moblin.android.platform.avfoundation.AVLinearPCMIsFloatKey
import com.moblin.android.platform.avfoundation.AVMediaType
import com.moblin.android.platform.avfoundation.AVNumberOfChannelsKey
import com.moblin.android.platform.avfoundation.AVSampleRateKey
import com.moblin.android.platform.avfoundation.kAudioFormatLinearPCM
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.sdwebimage.SDAnimatedImage
import com.moblin.android.platform.uikit.cgImage
import com.moblin.android.various.settings.SettingsAlertsMediaGalleryItem
import com.moblin.android.various.settings.SettingsWidgetAlertsAlert
import com.moblin.android.various.settings.SettingsWidgetAlertsAlertMediaType
import com.moblin.android.various.storages.AlertMediaStorage
import com.moblin.android.videoeffects.EffectImageCiImage
import com.moblin.android.videoeffects.toEffectImage
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class AlertsEffectMediaItem {
    data class BundledName(val name: String) : AlertsEffectMediaItem()

    data class CustomUrl(val url: String) : AlertsEffectMediaItem()

    data class Image(val image: CIImage) : AlertsEffectMediaItem()
}

data class AlertsEffectGifImage(val image: EffectImageCiImage, val timeOffset: Double)

data class AlertsEffectPlayer(val images: AlertsEffectImages, val soundUrl: String?)

open class AlertsEffectMedia {
    private var mediaType: SettingsWidgetAlertsAlertMediaType = SettingsWidgetAlertsAlertMediaType.gifAndSound
    private var gifImages: ArrayDeque<AlertsEffectGifImage> = ArrayDeque()
    private var videoUrl: String? = null
    private var soundUrl: String? = null

    open fun getPlayer(): AlertsEffectPlayer {
        val images: AlertsEffectImages = when (mediaType) {
            SettingsWidgetAlertsAlertMediaType.gifAndSound ->
                AlertsEffectGifImages(images = ArrayDeque(gifImages))
            SettingsWidgetAlertsAlertMediaType.video ->
                AlertsEffectVideoImages(videoUrl = videoUrl)
        }
        return AlertsEffectPlayer(images = images, soundUrl = soundUrl)
    }

    open fun update(
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
        videoUrl = mediaStorage.videos.makePath(filename = filename).path
        val path = videoUrl ?: return
        loadVideoSound(path = path) {
            soundUrl = it
        }
    }

    private fun updateGifAndSoundImages(
        alert: SettingsWidgetAlertsAlert,
        mediaStorage: AlertMediaStorage,
        bundledImages: List<SettingsAlertsMediaGalleryItem>,
    ) {
        val bundledImage = bundledImages.firstOrNull { it.id == alert.imageId }
        val image: AlertsEffectMediaItem = if (bundledImage != null) {
            AlertsEffectMediaItem.BundledName(bundledImage.name)
        } else {
            AlertsEffectMediaItem.CustomUrl(mediaStorage.makePath(id = alert.imageId).path)
        }
        val loopCount = alert.imageLoopCount
        alertsBackgroundScope.launch {
            var images: ArrayDeque<AlertsEffectGifImage> = ArrayDeque()
            when (image) {
                is AlertsEffectMediaItem.BundledName -> {
                    val url = Bundle.url("Alerts.bundle/${image.name}", "gif")
                    if (url != null) {
                        images = loadGifImages(url = url, loopCount = loopCount)
                    }
                }
                is AlertsEffectMediaItem.CustomUrl -> {
                    images = loadGifImages(url = image.url, loopCount = loopCount)
                }
                is AlertsEffectMediaItem.Image -> {
                    images = loadGifImages(image = image.image, loopCount = loopCount)
                }
            }
            alertsMainScope.launch {
                gifImages = images
            }
        }
    }

    private fun updateGifAndSoundSoundUrl(
        alert: SettingsWidgetAlertsAlert,
        mediaStorage: AlertMediaStorage,
        bundledSounds: List<SettingsAlertsMediaGalleryItem>,
    ) {
        val bundledSound = bundledSounds.firstOrNull { it.id == alert.soundId }
        val sound: AlertsEffectMediaItem = if (bundledSound != null) {
            AlertsEffectMediaItem.BundledName(bundledSound.name)
        } else {
            AlertsEffectMediaItem.CustomUrl(mediaStorage.makePath(id = alert.soundId).path)
        }
        when (sound) {
            is AlertsEffectMediaItem.BundledName -> {
                soundUrl = Bundle.url("Alerts.bundle/${sound.name}", "mp3")
            }
            is AlertsEffectMediaItem.CustomUrl -> {
                if (File(sound.url).exists()) {
                    soundUrl = sound.url
                } else {
                    soundUrl = null
                }
            }
            is AlertsEffectMediaItem.Image -> {
            }
        }
    }

    private fun loadGifImages(url: String, loopCount: Int): ArrayDeque<AlertsEffectGifImage> {
        val images = ArrayDeque<AlertsEffectGifImage>()
        val data = runCatching { File(url).readBytes() }.getOrNull() ?: return images
        val animatedImage = SDAnimatedImage(data = data) ?: return images
        val frames = mutableListOf<Pair<EffectImageCiImage, Double>>()
        for (index in 0 until animatedImage.animatedImageFrameCount) {
            val cgImage = animatedImage.animatedImageFrame(at = index)?.cgImage
            if (cgImage != null) {
                val image = CIImage(cgImage = cgImage).toEffectImage(isOpaque = false)
                frames.add(image to animatedImage.animatedImageDuration(at = index))
            }
        }
        var timeOffset = 0.0
        repeat(loopCount) {
            for (frame in frames) {
                timeOffset += frame.second
                images.addLast(AlertsEffectGifImage(image = frame.first, timeOffset = timeOffset))
            }
        }
        return images
    }

    private fun loadGifImages(image: CIImage, loopCount: Int): ArrayDeque<AlertsEffectGifImage> {
        var timeOffset = 0.0
        val images = ArrayDeque<AlertsEffectGifImage>()
        val effectImage = image.toEffectImage(isOpaque = false)
        repeat(loopCount) {
            timeOffset += 1
            images.addLast(AlertsEffectGifImage(image = effectImage, timeOffset = timeOffset))
        }
        return images
    }
}

interface AlertsEffectImages {
    fun getImage(presentationTimeStamp: Double): EffectImageCiImage?

    fun isEmpty(): Boolean
}

open class AlertsEffectGifImages : AlertsEffectImages {
    private var images: ArrayDeque<AlertsEffectGifImage> = ArrayDeque()
    private var basePresentationTimeStamp: Double? = null

    constructor()

    constructor(images: ArrayDeque<AlertsEffectGifImage>) {
        this.images = images
    }

    override fun getImage(presentationTimeStamp: Double): EffectImageCiImage? {
        if (basePresentationTimeStamp == null) {
            basePresentationTimeStamp = presentationTimeStamp
        }
        val timeOffset = presentationTimeStamp - basePresentationTimeStamp!!
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

    override fun isEmpty(): Boolean = images.isEmpty()
}

open class AlertsEffectVideoImages(videoUrl: String?) : AlertsEffectImages {
    private val reader: AlertsEffectVideoReader? =
        if (videoUrl != null) AlertsEffectVideoReader(path = videoUrl) else null

    override fun getImage(presentationTimeStamp: Double): EffectImageCiImage? =
        reader?.getImage(presentationTimeStamp = presentationTimeStamp)

    override fun isEmpty(): Boolean = reader?.hasEnded() ?: true
}

private fun loadVideoSound(path: String, onCompleted: (String?) -> Unit) {
    val asset = AVAsset(url = path)
    asset.loadTracks(withMediaType = AVMediaType.audio) { tracks, error ->
        alertsBackgroundScope.launch {
            val reader = runCatching { AVAssetReader(asset = asset) }.getOrNull()
            if (reader == null) {
                alertsMainScope.launch { onCompleted(null) }
                return@launch
            }
            val track = tracks?.firstOrNull()
            if (track == null || error != null) {
                alertsMainScope.launch { onCompleted(null) }
                return@launch
            }
            val outputSettings: Map<String, Any> = mapOf(
                AVFormatIDKey to kAudioFormatLinearPCM,
                AVNumberOfChannelsKey to 1,
                AVSampleRateKey to 48000.0,
                AVLinearPCMBitDepthKey to 16,
                AVLinearPCMIsFloatKey to false,
                AVLinearPCMIsBigEndianKey to false,
            )
            val trackOutput = AVAssetReaderTrackOutput(track = track, outputSettings = outputSettings)
            reader.add(output = trackOutput)
            reader.startReading()
            val samples = mutableListOf<Short>()
            while (true) {
                val sampleBuffer = trackOutput.copyNextSampleBuffer() ?: break
                val data = sampleBuffer.data
                val byteReader = ByteReader(data = data)
                while (true) {
                    val sample = runCatching { byteReader.readUInt16Le() }.getOrNull() ?: break
                    samples.add(sample.toShort())
                }
            }
            val wav = createWav(sampleRate = 48000, samples = listOf(samples))
            val soundUrl = "$path.wav"
            val created = runCatching {
                File(soundUrl).writeBytes(wav ?: ByteArray(0))
                true
            }.getOrDefault(false)
            if (!created) {
                alertsMainScope.launch { onCompleted(null) }
                return@launch
            }
            alertsMainScope.launch { onCompleted(soundUrl) }
        }
    }
}

private val alertsMainScope = CoroutineScope(Dispatchers.Main.immediate)

private val alertsBackgroundScope = CoroutineScope(Dispatchers.IO)
