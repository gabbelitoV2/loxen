package com.moblin.android.various.model

import com.moblin.android.media.haishinkit.media.video.PreviewView
import com.moblin.android.media.haishinkit.media.video.VideoGravity
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VideoPreviewFeed(val cameraId: UUID, val name: String) {
    val previewView: PreviewView = PreviewView()

    init {
        previewView.videoGravity = VideoGravity.RESIZE_ASPECT
    }
}

class VideoPreviewProvider {
    val feeds = MutableStateFlow<List<VideoPreviewFeed>>(emptyList())

    fun removeAllFeeds() {
        feeds.value = emptyList()
    }

    fun appendFeed(feed: VideoPreviewFeed) {
        feeds.value = feeds.value + feed
    }
}

fun Model.updateVideoPreviews() {
    val oldFeeds = videoPreview.feeds.value
    videoPreview.removeAllFeeds()
    if (streamOverlay.showingVideoPreview) {
        val scene = getSelectedScene() ?: return
        val devices = getBuiltinCameraDevices(scene = scene, sceneDevice = cameraDevice)
        for (camera in listCameras()) {
            val device = devices.devices.firstOrNull { it.device.uniqueID == camera.id }
            if (device != null) {
                appendVideoPreviewIfNeeded(
                    cameraId = device.id,
                    name = device.device.name(),
                    oldFeeds = oldFeeds,
                )
                continue
            }
            val cameraId = runCatching { UUID.fromString(camera.id) }.getOrNull()
            if (cameraId == null) {
                continue
            }
            if (!activeBufferedVideoIds.contains(cameraId)) {
                continue
            }
            appendVideoPreviewIfNeeded(cameraId = cameraId, name = camera.name, oldFeeds = oldFeeds)
        }
    } else {
        media.removeAllVideoPreviews()
    }
}

private fun Model.appendVideoPreviewIfNeeded(
    cameraId: UUID,
    name: String,
    oldFeeds: List<VideoPreviewFeed>,
) {
    val feed = oldFeeds.firstOrNull { it.cameraId == cameraId }
    if (feed != null) {
        videoPreview.appendFeed(feed)
    } else {
        val newFeed = VideoPreviewFeed(cameraId = cameraId, name = name)
        videoPreview.appendFeed(newFeed)
        media.setVideoPreview(cameraId = cameraId, drawable = newFeed.previewView)
    }
}
