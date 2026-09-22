package com.moblin.android.various.model

class PictureInPictureController {
    var contentSource: Any? = null
    var canStartPictureInPictureAutomaticallyFromInline: Boolean = false
}

data class PictureInPictureTimeRange(val start: Long, val duration: Long)

data class PictureInPictureVideoDimensions(val width: Int, val height: Int)

interface PictureInPictureSampleBufferPlaybackDelegate {
    fun pictureInPictureController(controller: PictureInPictureController, setPlaying: Boolean)

    fun pictureInPictureControllerTimeRangeForPlayback(
        controller: PictureInPictureController,
    ): PictureInPictureTimeRange

    fun pictureInPictureControllerIsPlaybackPaused(
        controller: PictureInPictureController,
    ): Boolean

    fun pictureInPictureController(
        controller: PictureInPictureController,
        didTransitionToRenderSize: PictureInPictureVideoDimensions,
    )

    fun pictureInPictureController(
        controller: PictureInPictureController,
        skipByInterval: Long,
        completion: () -> Unit,
    )
}

fun Model.setupPictureInPicture() {
    TODO("no Android counterpart for AVPictureInPictureController")
}

fun Model.updatePictureInPicture() {
    if (stream.value.backgroundStreaming && stream.value.backgroundStreamingPiP && (isLive.value || isRecording.value)) {
        if (pipController.value?.contentSource == null) {
            pipController.value?.contentSource =
                TODO("no Android counterpart for AVPictureInPictureController.ContentSource")
        }
    } else {
        pipController.value?.contentSource = null
    }
}

fun Model.pictureInPictureEnabled(): Boolean {
    return pipController.value?.contentSource != null
}

fun Model.pictureInPictureController(
    controller: PictureInPictureController,
    setPlaying: Boolean,
) {
}

fun Model.pictureInPictureControllerTimeRangeForPlayback(
    controller: PictureInPictureController,
): PictureInPictureTimeRange {
    return PictureInPictureTimeRange(start = Long.MIN_VALUE, duration = Long.MAX_VALUE)
}

fun Model.pictureInPictureControllerIsPlaybackPaused(
    controller: PictureInPictureController,
): Boolean {
    return false
}

fun Model.pictureInPictureController(
    controller: PictureInPictureController,
    didTransitionToRenderSize: PictureInPictureVideoDimensions,
) {
}

fun Model.pictureInPictureController(
    controller: PictureInPictureController,
    skipByInterval: Long,
    completion: () -> Unit,
) {
    completion()
}
