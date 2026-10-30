package com.moblin.android.various.model

import android.graphics.Bitmap
import com.moblin.android.platform.log.Log
import com.moblin.android.localized
import com.moblin.android.platform.avfoundation.PHAssetCreationRequest
import com.moblin.android.platform.avfoundation.PHAssetResourceType
import com.moblin.android.platform.avfoundation.PHPhotoLibrary
import com.moblin.android.platform.coregraphics.CGImagePropertyOrientation
import com.moblin.android.platform.coreimage.CIContext
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.uikit.UIImage
import com.moblin.android.platform.uikit.cgImage
import com.moblin.android.various.network.httpRequest
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetType
import com.moblin.android.various.utils.uploadImage
import java.io.ByteArrayOutputStream
import java.net.URI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Request

private const val TAG = "Model"

data class SnapshotJob(
    val message: String,
    val user: String?,
    val takeSnapshot: (String) -> Unit,
)

data class DiscordSnapshotUpload(
    val url: String,
    val image: ByteArray,
    val message: String?,
)

private val mainScope = CoroutineScope(Dispatchers.Main)

fun Model.takeSnapshot(age: Float, isChatBot: Boolean, message: String?) {
    if (isChatPhone()) {
        return
    }
    media.takeSnapshot(age = age) { uiImage, image, portraitImage ->
        val output = ByteArrayOutputStream()
        if (uiImage.compress(Bitmap.CompressFormat.JPEG, 90, output)) {
            val imageJpeg = output.toByteArray()
            saveSnapshotToPhotos(image = uiImage, fallbackImage = imageJpeg)
            handleSnapshot(
                imageJpeg,
                image,
                portraitImage,
                message,
                isChatBot,
                !stream.value.discordSnapshotWebhookOnlyWhenLive || isLive.value,
            )
        }
    }
}

fun Model.takeSnapshotWebRemoteControl(age: Float, isChatBot: Boolean, message: String?) {
    val url = runCatching {
        val base = URI(database.chat.botCommandSnapshot.webRemoteControlUrl)
        val basePath = base.path ?: ""
        URI(base.scheme, base.authority, basePath.trimEnd('/') + "/snapshot", "age=$age", null)
    }.getOrNull()
    if (url == null) {
        makeErrorToast(title = localized("Invalid chat bot snapshot web remote control URL"))
        return
    }
    val request = Request.Builder().url(url.toString()).build()
    httpRequest(request = request, queue = Dispatchers.IO) { data, response, error ->
        val failure = localized("Failed to fetch snapshot from web remote control")
        if (error != null || response?.isSuccessful != true) {
            makeErrorToastMain(title = failure)
            return@httpRequest
        }
        val bytes = data
        if (bytes == null) {
            makeErrorToastMain(title = failure)
            return@httpRequest
        }
        val uiImage = UIImage(data = bytes)
        val output = ByteArrayOutputStream()
        if (uiImage == null || !uiImage.compress(Bitmap.CompressFormat.JPEG, 90, output)) {
            makeErrorToastMain(title = failure)
            return@httpRequest
        }
        val imageJpeg = output.toByteArray()
        val image = uiImage.cgImage
        var portraitImage = image
        if (image.width > image.height) {
            val ciImage = CIImage(cgImage = image)
            val portraitCIImage = ciImage.oriented(orientation = CGImagePropertyOrientation.left)
            val context = CIContext()
            portraitImage = context.createCGImage(portraitCIImage, from = portraitCIImage.extent) ?: image
        }
        mainScope.launch {
            saveSnapshotToPhotos(image = bytes)
            handleSnapshot(imageJpeg, image, portraitImage, message, isChatBot, true)
        }
    }
}

fun Model.takeSnapshotHeic(age: Float, onComplete: (ByteArray?) -> Unit) {
    if (isChatPhone()) {
        onComplete(null)
        return
    }
    media.takeSnapshot(age = age) { uiImage, _, _ ->
        CoroutineScope(Dispatchers.IO).launch {
            val imageHeic = encodeSnapshotForPhotos(image = uiImage)
            mainScope.launch {
                onComplete(imageHeic)
            }
        }
    }
}

private fun Model.handleSnapshot(
    imageJpeg: ByteArray,
    image: Bitmap,
    portraitImage: Bitmap,
    message: String?,
    isChatBot: Boolean,
    uploadToDiscord: Boolean,
) {
    if (uploadToDiscord) {
        tryUploadSnapshotToDiscord(imageJpeg, message, isChatBot)
    }
    printSnapshotCatPrinters(image = portraitImage)
    appendSnapshotToSnapshotWidgets(image = image)
}

private fun Model.saveSnapshotToPhotos(image: Bitmap, fallbackImage: ByteArray) {
    CoroutineScope(Dispatchers.IO).launch {
        val data = encodeSnapshotForPhotos(image = image) ?: fallbackImage
        mainScope.launch {
            saveSnapshotToPhotos(image = data)
        }
    }
}

private fun Model.saveSnapshotToPhotos(image: ByteArray) {
    PHPhotoLibrary.shared().performChanges({
        val creationRequest = PHAssetCreationRequest.forAsset()
        creationRequest.addResource(with = PHAssetResourceType.photo, data = image, options = null)
    }) { _, error ->
        if (error != null) {
            makeErrorToastMain(
                title = localized("Failed to save snapshot to Photos"),
                subTitle = error.localizedMessage,
            )
        } else {
            mainScope.launch {
                makeToast(title = localized("Snapshot saved to Photos"))
            }
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
        snapshotJob.takeSnapshot(message)
        mainScope.launch {
            delay(10_000)
            snapshot.currentJob.value = null
            tryTakeNextSnapshot()
        }
    }
}

fun Model.takeSnapshotWithCountdown(message: String, user: String?, takeSnapshot: (String) -> Unit) {
    snapshotJobs.add(SnapshotJob(message = message, user = user, takeSnapshot = takeSnapshot))
    tryTakeNextSnapshot()
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

private fun Model.getDiscordWebhookUrl(isChatBot: Boolean): String? {
    return if (isChatBot) {
        stream.value.discordChatBotSnapshotWebhook.takeIf { it.isNotBlank() }
    } else {
        stream.value.discordSnapshotWebhook.takeIf { it.isNotBlank() }
    }
}

private fun Model.tryUploadSnapshotToDiscord(image: ByteArray, message: String?, isChatBot: Boolean) {
    val url = getDiscordWebhookUrl(isChatBot)
    if (url == null) {
        return
    }
    if (discordSnapshotUploads.size == 20) {
        Log.i(TAG, "snapshot: Discord upload queue full, dropping oldest snapshot")
        discordSnapshotUploads.removeFirst()
    }
    discordSnapshotUploads.add(DiscordSnapshotUpload(url = url, image = image, message = message))
    tryUploadNextSnapshotToDiscord()
}

private fun Model.tryUploadNextSnapshotToDiscord() {
    if (isUploadingSnapshotToDiscord || discordSnapshotUploads.isEmpty()) {
        return
    }
    val upload = discordSnapshotUploads.removeFirst()
    isUploadingSnapshotToDiscord = true
    uploadImage(
        url = upload.url,
        paramName = "snapshot",
        fileName = "snapshot.jpg",
        image = upload.image,
        message = upload.message,
    ) { ok ->
        isUploadingSnapshotToDiscord = false
        if (ok) {
            makeToast(title = localized("Snapshot uploaded to Discord"))
        } else {
            makeErrorToast(title = localized("Failed to upload snapshot to Discord"))
        }
        tryUploadNextSnapshotToDiscord()
    }
}

private fun Model.getDiscordSnapshotUploadMaximumBytesPerSecond(): Int? {
    if (!isLive.value) {
        return null
    }
    val bitrate = media.getVideoStreamBitrate(bitrate = stream.value.bitrate)
    val divisor = if (bitrate >= stream.value.bitrate) 2 else 4
    return (bitrate.toInt() / 8 / divisor).coerceIn(16_000, 1_000_000)
}

private fun encodeSnapshotForPhotos(image: Bitmap): ByteArray? {
    val cgImage = image.cgImage
    val output = ByteArrayOutputStream()
    if (!cgImage.compress(Bitmap.CompressFormat.JPEG, 90, output)) {
        return null
    }
    return output.toByteArray()
}
