package com.moblin.android.various.model

import android.graphics.Bitmap
import com.moblin.android.localized
import com.moblin.android.platform.avfoundation.PHAssetCreationRequest
import com.moblin.android.platform.avfoundation.PHAssetResourceType
import com.moblin.android.platform.avfoundation.PHPhotoLibrary
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetType
import com.moblin.android.various.utils.uploadImage
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SnapshotJob(
    val isChatBot: Boolean,
    val message: String,
    val user: String?,
)

private val mainScope = CoroutineScope(Dispatchers.Main)

fun Model.takeSnapshot(isChatBot: Boolean = false, message: String? = null, noDelay: Boolean = false) {
    if (isChatPhone()) {
        return
    }
    val age = if (isChatBot && !noDelay) stream.value.estimatedViewerDelay else 0.0
    media.takeSnapshot(age = age.toFloat()) { uiImage, image, portraitImage ->
        val output = ByteArrayOutputStream()
        if (uiImage.compress(Bitmap.CompressFormat.JPEG, 90, output)) {
            val imageJpeg = output.toByteArray()
            PHPhotoLibrary.shared().performChanges({
                PHAssetCreationRequest.forAsset().addResource(with = PHAssetResourceType.photo,
                                                              data = imageJpeg,
                                                              options = null)
            })
            makeToast(title = localized("Snapshot saved to Photos"))
            tryUploadSnapshotToDiscord(imageJpeg, message, isChatBot)
            printSnapshotCatPrinters(image = portraitImage)
            appendSnapshotToSnapshotWidgets(image = image)
        }
    }
}

private fun Model.appendSnapshotToSnapshotWidgets(image: Bitmap) {
    for (snapshotEffect in enabledSnapshotEffects) {
        snapshotEffect.appendSnapshot(image = com.moblin.android.platform.coreimage.CIImage(cgImage = image))
    }
}

private fun Model.tryTakeNextSnapshot() {
    if (snapshot.currentJob.value != null) {
        return
    }
    snapshot.currentJob.value = snapshotJobs.removeFirstOrNull()
    if (snapshot.currentJob.value == null) {
        return
    }
    snapshot.countdown.value = 5
    snapshotCountdownTick()
}

fun Model.formatSnapshotTakenBy(user: String): String {
    return localized("Snapshot taken by $user.")
}

fun Model.formatSnapshotTakenSuccessfully(user: String): String {
    return localized("$user, thanks for bringing our photo album to life. 🎉")
}

fun Model.formatSnapshotTakenNotAllowed(user: String): String {
    return localized(" $user, you are not allowed to take snapshots, sorry. 😢")
}

private fun Model.snapshotCountdownTick() {
    mainScope.launch {
        delay(1_000)
        snapshot.countdown.value = snapshot.countdown.value - 1
        if (snapshot.countdown.value != 0) {
            snapshotCountdownTick()
            return@launch
        }
        val snapshotJob = snapshot.currentJob.value ?: return@launch
        var message = snapshotJob.message
        snapshotJob.user?.let { user ->
            message += "\n"
            message += formatSnapshotTakenBy(user = user)
        }
        takeSnapshot(isChatBot = snapshotJob.isChatBot, message = message, noDelay = true)
        mainScope.launch {
            delay(10_000)
            snapshot.currentJob.value = null
            tryTakeNextSnapshot()
        }
    }
}

fun Model.takeSnapshotWithCountdown(isChatBot: Boolean, message: String, user: String?) {
    if (isChatPhone()) {
        return
    }
    snapshotJobs.add(SnapshotJob(isChatBot = isChatBot, message = message, user = user))
    tryTakeNextSnapshot()
}

private fun Model.getDiscordWebhookUrl(isChatBot: Boolean): String? {
    return if (isChatBot) {
        stream.value.discordChatBotSnapshotWebhook.takeIf { it.isNotBlank() }
    } else {
        stream.value.discordSnapshotWebhook.takeIf { it.isNotBlank() }
    }
}

private fun Model.tryUploadSnapshotToDiscord(image: ByteArray, message: String?, isChatBot: Boolean) {
    val url = getDiscordWebhookUrl(isChatBot)
    if ((!stream.value.discordSnapshotWebhookOnlyWhenLive || isLive.value) && url != null) {
        uploadImage(
            url = url,
            paramName = "snapshot",
            fileName = "snapshot.jpg",
            image = image,
            message = message,
        ) { ok ->
            if (ok) {
                makeToast(title = localized("Snapshot uploaded to Discord"))
            } else {
                makeErrorToast(title = localized("Failed to upload snapshot to Discord"))
            }
        }
    }
}

fun Model.takeVideoSourcePreviewImage(
    widget: SettingsWidget,
    onComplete: (Bitmap?) -> Unit,
) {
    val videoSourceId = getVideoSourceId(cameraId = widget.videoSource.toCameraId())
    if (widget.type != SettingsWidgetType.videoSource || videoSourceId == null) {
        onComplete(null)
        return
    }
    media.takeVideoSourceSnapshot(videoSourceId = videoSourceId, onComplete = onComplete)
}

fun Model.setCleanSnapshots() {
    media.setCleanSnapshots(enabled = stream.value.recording.cleanSnapshots)
}
