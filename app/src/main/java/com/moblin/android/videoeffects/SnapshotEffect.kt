package com.moblin.android.videoeffects

import com.moblin.android.media.kCMTimeInvalidUs
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.various.settings.SettingsSceneWidget
import kotlinx.coroutines.launch

class SnapshotEffect(showtime: Int) : VideoEffect() {
    private var snapshots: ArrayDeque<CIImage> = ArrayDeque()
    private var sceneWidget: SettingsSceneWidget? = null
    private var currentSnapshot: EffectImageCiImage? = null
    private var hideSnapshotTime: Double? = null
    private var showtime: Double = showtime.toDouble()

    fun setSceneWidget(sceneWidget: SettingsSceneWidget) {
        processorPipelineQueue.launch {
            this@SnapshotEffect.sceneWidget = sceneWidget
        }
    }

    fun setSettings(showtime: Int) {
        processorPipelineQueue.launch {
            this@SnapshotEffect.showtime = showtime.toDouble()
        }
    }

    fun appendSnapshot(image: CIImage) {
        processorPipelineQueue.launch {
            this@SnapshotEffect.appendSnapshotInternal(image = image)
        }
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        val sceneWidget = sceneWidget ?: return image
        updateCurrentSnapshot(info = info)
        val currentSnapshot = currentSnapshot ?: return image
        return applyEffectsResizeMirrorMove(
            image = currentSnapshot.getCiImage(),
            sceneWidget = sceneWidget,
            mirror = false,
            backgroundImageExtent = image.extent,
            info = info,
        )
            .composited(over = image)
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        val sceneWidget = sceneWidget ?: return image
        updateCurrentSnapshot(info = info)
        val currentSnapshot = currentSnapshot ?: return image
        return applyEffectsResizeMirrorMoveMetalPetal(
            image = currentSnapshot.getMetalPetalImage(),
            sceneWidget = sceneWidget,
            mirror = false,
            backgroundImage = image,
            info = info,
        )
    }

    override fun isEnabled(): Boolean {
        return currentSnapshot != null
    }

    private fun updateCurrentSnapshot(info: VideoEffectInfo) {
        val seconds = snapshotSeconds(presentationTimeStampUs = info.presentationTimeStamp)
        if (hideSnapshotTime == null) {
            hideSnapshotTime = seconds + showtime
        }
        val hideSnapshotTime = hideSnapshotTime
        if (hideSnapshotTime != null && seconds > hideSnapshotTime) {
            setCurrentSnapshot(image = snapshots.removeFirstOrNull())
            this.hideSnapshotTime = null
        }
    }

    private fun setCurrentSnapshot(image: CIImage?) {
        currentSnapshot = image?.toEffectImage(isOpaque = true)
    }

    private fun appendSnapshotInternal(image: CIImage) {
        snapshots.addLast(image)
        if (currentSnapshot != null) {
            return
        }
        setCurrentSnapshot(image = snapshots.removeFirstOrNull())
        hideSnapshotTime = null
    }
}

private fun snapshotSeconds(presentationTimeStampUs: Long): Double =
    if (presentationTimeStampUs == kCMTimeInvalidUs) {
        Double.NaN
    } else {
        presentationTimeStampUs / 1_000_000.0
    }
