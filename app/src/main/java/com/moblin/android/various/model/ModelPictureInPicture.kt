package com.moblin.android.various.model

import com.moblin.android.platform.avfoundation.CMTimeRange
import com.moblin.android.platform.avkit.AVPictureInPictureController
import com.moblin.android.platform.avkit.AVPictureInPictureSampleBufferPlaybackDelegate
import com.moblin.android.platform.avkit.CMVideoDimensions
import com.moblin.android.platform.video.layer

fun Model.setupPictureInPicture() {
    if (!AVPictureInPictureController.isPictureInPictureSupported()) {
        return
    }
    val contentSource = AVPictureInPictureController.ContentSource(
        sampleBufferDisplayLayer = streamPreviewView.layer,
        playbackDelegate = ModelPictureInPicturePlaybackDelegate(this),
    )
    val controller = AVPictureInPictureController(contentSource = contentSource)
    controller.canStartPictureInPictureAutomaticallyFromInline = true
    pipController = controller
    updatePictureInPicture()
}

fun Model.updatePictureInPicture() {
    if (stream.value.backgroundStreaming && stream.value.backgroundStreamingPiP && (isLive.value || isRecording.value)) {
        if (pipController?.contentSource == null) {
            pipController?.contentSource = AVPictureInPictureController.ContentSource(
                sampleBufferDisplayLayer = streamPreviewView.layer,
                playbackDelegate = ModelPictureInPicturePlaybackDelegate(this),
            )
        }
    } else {
        pipController?.contentSource = null
    }
}

fun Model.pictureInPictureEnabled(): Boolean {
    return pipController?.contentSource != null
}

fun Model.pictureInPictureController(
    pictureInPictureController: AVPictureInPictureController,
    setPlaying: Boolean,
) {
}

fun Model.pictureInPictureControllerTimeRangeForPlayback(
    pictureInPictureController: AVPictureInPictureController,
): CMTimeRange {
    return CMTimeRange(start = Long.MIN_VALUE, duration = Long.MAX_VALUE)
}

fun Model.pictureInPictureControllerIsPlaybackPaused(
    pictureInPictureController: AVPictureInPictureController,
): Boolean {
    return false
}

fun Model.pictureInPictureController(
    pictureInPictureController: AVPictureInPictureController,
    didTransitionToRenderSize: CMVideoDimensions,
) {
}

fun Model.pictureInPictureController(
    pictureInPictureController: AVPictureInPictureController,
    skipByInterval: Long,
    completion: () -> Unit,
) {
    completion()
}

private class ModelPictureInPicturePlaybackDelegate(
    private val model: Model,
) : AVPictureInPictureSampleBufferPlaybackDelegate {
    override fun pictureInPictureController(
        pictureInPictureController: AVPictureInPictureController,
        setPlaying: Boolean,
    ) {
        model.pictureInPictureController(pictureInPictureController, setPlaying = setPlaying)
    }

    override fun pictureInPictureControllerTimeRangeForPlayback(
        pictureInPictureController: AVPictureInPictureController,
    ): CMTimeRange {
        return model.pictureInPictureControllerTimeRangeForPlayback(pictureInPictureController)
    }

    override fun pictureInPictureControllerIsPlaybackPaused(
        pictureInPictureController: AVPictureInPictureController,
    ): Boolean {
        return model.pictureInPictureControllerIsPlaybackPaused(pictureInPictureController)
    }

    override fun pictureInPictureController(
        pictureInPictureController: AVPictureInPictureController,
        didTransitionToRenderSize: CMVideoDimensions,
    ) {
        model.pictureInPictureController(pictureInPictureController, didTransitionToRenderSize = didTransitionToRenderSize)
    }

    override fun pictureInPictureController(
        pictureInPictureController: AVPictureInPictureController,
        skipByInterval: Long,
        completion: () -> Unit,
    ) {
        model.pictureInPictureController(pictureInPictureController, skipByInterval = skipByInterval, completion = completion)
    }
}
