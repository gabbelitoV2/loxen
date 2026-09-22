package com.moblin.android.videoeffects

import android.media.Image
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.various.settings.SettingsSceneWidget
import kotlinx.coroutines.launch

class SnapshotEffect(showtime: Int) : VideoEffect() {
    private val snapshots: ArrayDeque<CIImage> = ArrayDeque()
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
            appendSnapshotInternal(image)
        }
    }

    override fun execute(image: Image, info: VideoEffectInfo): Image {
        val sceneWidget = this.sceneWidget ?: return image
        updateCurrentSnapshot(info)
        val currentSnapshot = this.currentSnapshot ?: return image
        return TODO("OpenGL ES port: composite snapshot over image")
    }

    override fun executeMetalPetal(image: Image, info: VideoEffectInfo): Image =
        TODO("OpenGL ES port")

    override fun isEnabled(): Boolean {
        return currentSnapshot != null
    }

    private fun updateCurrentSnapshot(info: VideoEffectInfo) {
        val seconds = info.presentationTimeStamp / 1_000_000.0
        if (hideSnapshotTime == null) {
            hideSnapshotTime = seconds + showtime
        }
        val hideSnapshotTime = this.hideSnapshotTime
        if (hideSnapshotTime != null && seconds > hideSnapshotTime) {
            setCurrentSnapshot(snapshots.removeFirstOrNull())
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
        setCurrentSnapshot(snapshots.removeFirstOrNull())
        hideSnapshotTime = null
    }
}
