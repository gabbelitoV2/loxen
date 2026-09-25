package com.moblin.android.platform.avkit

import com.moblin.android.platform.avfoundation.CMTimeRange
import com.moblin.android.platform.video.AVSampleBufferDisplayLayer

data class CMVideoDimensions(val width: Int, val height: Int)

interface AVPictureInPictureSampleBufferPlaybackDelegate {
    fun pictureInPictureController(pictureInPictureController: AVPictureInPictureController, setPlaying: Boolean)

    fun pictureInPictureControllerTimeRangeForPlayback(
        pictureInPictureController: AVPictureInPictureController,
    ): CMTimeRange

    fun pictureInPictureControllerIsPlaybackPaused(pictureInPictureController: AVPictureInPictureController): Boolean

    fun pictureInPictureController(
        pictureInPictureController: AVPictureInPictureController,
        didTransitionToRenderSize: CMVideoDimensions,
    )

    fun pictureInPictureController(
        pictureInPictureController: AVPictureInPictureController,
        skipByInterval: Long,
        completion: () -> Unit,
    )
}

class AVPictureInPictureController(contentSource: ContentSource?) {
    class ContentSource(
        val sampleBufferDisplayLayer: AVSampleBufferDisplayLayer,
        val playbackDelegate: AVPictureInPictureSampleBufferPlaybackDelegate,
    )

    var contentSource: ContentSource? = contentSource
        set(value) {
            if (field === value) {
                return
            }
            field = value
            PictureInPictureWindow.update()
        }

    var canStartPictureInPictureAutomaticallyFromInline: Boolean = false
        set(value) {
            if (field == value) {
                return
            }
            field = value
            PictureInPictureWindow.update()
        }

    val isPictureInPictureActive: Boolean
        get() = PictureInPictureWindow.isActive(this)

    init {
        PictureInPictureWindow.register(this)
    }

    companion object {
        fun isPictureInPictureSupported(): Boolean {
            return PictureInPictureWindow.isSupported()
        }
    }
}
