package com.moblin.android.various.model

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.asAndroidBitmap
import com.moblin.android.integrations.catprinter.CatPrinter
import com.moblin.android.integrations.catprinter.CatPrinterDelegate
import com.moblin.android.integrations.catprinter.CatPrinterState
import com.moblin.android.streamingplatforms.Platform
import com.moblin.android.streamingplatforms.kick.fetchKickProfilePicture
import com.moblin.android.streamingplatforms.twitch.fetchTwitchProfilePicture
import com.moblin.android.various.settings.SettingsCatPrinter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val mainScope = CoroutineScope(Dispatchers.Main)

private val eventTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm:ss", Locale.US)

sealed class CatPrinterEvent {
    object TwitchFollow : CatPrinterEvent()
    object TwitchSubscribe : CatPrinterEvent()
    object TwitchSubscriptionGift : CatPrinterEvent()
    object TwitchResubscribe : CatPrinterEvent()
    object TwitchRaid : CatPrinterEvent()
    data class TwitchCheer(val amount: Int) : CatPrinterEvent()
    object TwitchReward : CatPrinterEvent()
    object KickSubscription : CatPrinterEvent()
    object KickGiftedSubscriptions : CatPrinterEvent()
    object KickHost : CatPrinterEvent()
    object KickReward : CatPrinterEvent()
    data class KickKicks(val amount: Int) : CatPrinterEvent()

    fun platform(): Platform {
        return when (this) {
            is TwitchFollow -> Platform.twitch
            is TwitchSubscribe -> Platform.twitch
            is TwitchSubscriptionGift -> Platform.twitch
            is TwitchResubscribe -> Platform.twitch
            is TwitchRaid -> Platform.twitch
            is TwitchCheer -> Platform.twitch
            is TwitchReward -> Platform.twitch
            is KickSubscription -> Platform.kick
            is KickGiftedSubscriptions -> Platform.kick
            is KickHost -> Platform.kick
            is KickReward -> Platform.kick
            is KickKicks -> Platform.kick
        }
    }
}

fun Model.printAllCatPrinters(image: Bitmap, feedPaperDelay: Double? = null) {
    for (catPrinter in catPrinters.values) {
        catPrinter.print(com.moblin.android.platform.coreimage.CIImage(cgImage = image), feedPaperDelay)
    }
}

fun Model.printSnapshotCatPrinters(image: Bitmap) {
    for (catPrinter in catPrinters.values) {
        if (getCatPrinterSettings(catPrinter)?.printSnapshots?.value == true) {
            catPrinter.print(com.moblin.android.platform.coreimage.CIImage(cgImage = image), null)
        }
    }
}

fun Model.printEventCatPrinters(event: CatPrinterEvent, username: String, message: String) {
    mainScope.launch {
        var image: Bitmap? = null
        for (catPrinter in catPrinters.values) {
            val settings = getCatPrinterSettings(catPrinter) ?: continue
            if (!isCatPrinterEventEnabled(event, settings)) {
                continue
            }
            if (image == null) {
                image = createEventImage(
                    username = username,
                    message = message,
                    platform = event.platform()
                )
            }
            val currentImage = image
            if (currentImage != null) {
                catPrinter.print(com.moblin.android.platform.coreimage.CIImage(cgImage = currentImage), null)
            }
        }
    }
}

fun Model.catPrinterPrintTestImage(device: SettingsCatPrinter) {
    val image = Bitmap.createBitmap(100, 10, Bitmap.Config.ARGB_8888)
    image.eraseColor(Color.BLACK)
    catPrinters[device.id]?.print(com.moblin.android.platform.coreimage.CIImage(cgImage = image), null)
}

fun Model.isCatPrinterEnabled(device: SettingsCatPrinter): Boolean {
    return device.enabled.value
}

fun Model.enableCatPrinter(device: SettingsCatPrinter) {
    if (!catPrinters.containsKey(device.id)) {
        val catPrinter = CatPrinter()
        catPrinter.delegate = ModelCatPrinterDelegate(this)
        catPrinters[device.id] = catPrinter
    }
    catPrinters[device.id]?.start(
        deviceId = device.bluetoothPeripheralId.value,
        meowSoundEnabled = device.faxMeowSound.value
    )
}

fun Model.catPrinterSetFaxMeowSound(device: SettingsCatPrinter) {
    catPrinters[device.id]?.setMeowSoundEnabled(meowSoundEnabled = device.faxMeowSound.value)
}

fun Model.disableCatPrinter(device: SettingsCatPrinter) {
    catPrinters[device.id]?.stop()
}

fun Model.getCatPrinterSettings(catPrinter: CatPrinter): SettingsCatPrinter? {
    return database.catPrinters.devices.value.firstOrNull { catPrinters[it.id] === catPrinter }
}

fun Model.setCurrentCatPrinter(device: SettingsCatPrinter) {
    currentCatPrinterSettings = device
    statusTopRight.catPrinterState.value = getCatPrinterState(device)
}

fun Model.getCatPrinterState(device: SettingsCatPrinter): CatPrinterState {
    return catPrinters[device.id]?.getState() ?: CatPrinterState.disconnected
}

fun Model.autoStartCatPrinters() {
    for (device in database.catPrinters.devices.value) {
        if (device.enabled.value) {
            enableCatPrinter(device)
        }
    }
}

fun Model.stopCatPrinters() {
    for (catPrinter in catPrinters.values) {
        catPrinter.stop()
    }
}

fun Model.isAnyConnectedCatPrinterPrintingChat(): Boolean {
    return catPrinters.values.any {
        it.getState() == CatPrinterState.connected &&
            getCatPrinterSettings(it)?.printChat?.value == true
    }
}

fun Model.isAnyCatPrinterConfigured(): Boolean {
    return database.catPrinters.devices.value.any { it.enabled.value }
}

fun Model.areAllCatPrintersConnected(): Boolean {
    return catPrinters.values.none {
        getCatPrinterSettings(it)?.enabled?.value == true && it.getState() != CatPrinterState.connected
    }
}

private fun Model.isCatPrinterEventEnabled(
    event: CatPrinterEvent,
    settings: SettingsCatPrinter
): Boolean {
    return when (event) {
        is CatPrinterEvent.TwitchFollow -> settings.printTwitch.value.follows
        is CatPrinterEvent.TwitchSubscribe -> settings.printTwitch.value.subscriptions
        is CatPrinterEvent.TwitchSubscriptionGift -> settings.printTwitch.value.giftSubscriptions
        is CatPrinterEvent.TwitchResubscribe -> settings.printTwitch.value.resubscriptions
        is CatPrinterEvent.TwitchRaid -> settings.printTwitch.value.raids
        is CatPrinterEvent.TwitchCheer -> settings.printTwitch.value.isBitsEnabled(amount = event.amount)
        is CatPrinterEvent.TwitchReward -> settings.printTwitch.value.rewards
        is CatPrinterEvent.KickSubscription -> settings.printKick.value.subscriptions
        is CatPrinterEvent.KickGiftedSubscriptions -> settings.printKick.value.giftedSubscriptions
        is CatPrinterEvent.KickHost -> settings.printKick.value.hosts
        is CatPrinterEvent.KickReward -> settings.printKick.value.rewards
        is CatPrinterEvent.KickKicks -> settings.printKick.value.isKicksEnabled(amount = event.amount)
    }
}

private fun Paint.lineHeight(): Float {
    return fontMetrics.descent - fontMetrics.ascent
}

private fun textLayout(
    text: String,
    paint: TextPaint,
    width: Int,
    alignment: Layout.Alignment
): StaticLayout {
    return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
        .setAlignment(alignment)
        .setIncludePad(false)
        .build()
}

private suspend fun Model.createEventImage(
    username: String,
    message: String,
    platform: Platform
): Bitmap? {
    val profileImage = fetchProfilePicture(username = username, platform = platform)
    val width = 384
    val padding = 20f
    val spacing = 16f
    val dividerHeight = 3f
    val avatarSize = 120f
    val contentWidth = width - 2 * padding

    val platformPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 36f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    val usernamePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 40f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val messagePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    val timestampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 24f
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
    }
    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
    }
    val avatarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
    }

    val usernameLayout = textLayout(
        username,
        usernamePaint,
        contentWidth.toInt(),
        Layout.Alignment.ALIGN_CENTER
    )
    val messageLayout = textLayout(
        message,
        messagePaint,
        contentWidth.toInt(),
        Layout.Alignment.ALIGN_CENTER
    )
    val heights = listOf(
        platformPaint.lineHeight(),
        avatarSize,
        usernameLayout.height.toFloat(),
        dividerHeight,
        messageLayout.height.toFloat(),
        dividerHeight,
        timestampPaint.lineHeight()
    )
    val height = (padding * 2f + heights.sum() + spacing * (heights.size - 1)).toInt()

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.WHITE)

    var y = padding

    canvas.drawText(platform.displayName(), width / 2f, y - platformPaint.fontMetrics.ascent, platformPaint)
    y += platformPaint.lineHeight() + spacing

    val avatarLeft = (width - avatarSize) / 2f
    val avatarRect = RectF(avatarLeft, y, avatarLeft + avatarSize, y + avatarSize)
    if (profileImage != null) {
        val avatarPath = Path().apply {
            addCircle(avatarRect.centerX(), avatarRect.centerY(), avatarSize / 2f, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(avatarPath)
        canvas.drawBitmap(profileImage, null, avatarRect, Paint(Paint.FILTER_BITMAP_FLAG))
        canvas.restore()
    } else {
        canvas.drawCircle(avatarRect.centerX(), avatarRect.centerY(), avatarSize / 2f, avatarPaint)
    }
    y += avatarSize + spacing

    canvas.save()
    canvas.translate(padding, y)
    usernameLayout.draw(canvas)
    canvas.restore()
    y += usernameLayout.height.toFloat() + spacing

    canvas.drawRect(padding, y, width - padding, y + dividerHeight, dividerPaint)
    y += dividerHeight + spacing

    canvas.save()
    canvas.translate(padding, y)
    messageLayout.draw(canvas)
    canvas.restore()
    y += messageLayout.height.toFloat() + spacing

    canvas.drawRect(padding, y, width - padding, y + dividerHeight, dividerPaint)
    y += dividerHeight + spacing

    val timestamp = ZonedDateTime.now().format(eventTimestampFormatter)
    canvas.drawText(timestamp, width / 2f, y - timestampPaint.fontMetrics.ascent, timestampPaint)

    return bitmap
}

private suspend fun Model.fetchProfilePicture(username: String, platform: Platform): Bitmap? {
    return when (platform) {
        Platform.twitch -> fetchTwitchProfilePicture(username = username)?.asAndroidBitmap()
        Platform.kick -> fetchKickProfilePicture(username = username)
        else -> null
    }
}

class ModelCatPrinterDelegate(private val model: Model) : CatPrinterDelegate {
    override fun catPrinterState(catPrinter: CatPrinter, state: CatPrinterState) {
        mainScope.launch {
            val device = model.getCatPrinterSettings(catPrinter) ?: return@launch
            if (device === model.currentCatPrinterSettings) {
                model.statusTopRight.catPrinterState.value = state
            }
        }
    }
}
