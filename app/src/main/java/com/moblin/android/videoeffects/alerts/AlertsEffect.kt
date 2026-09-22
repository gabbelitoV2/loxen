package com.moblin.android.videoeffects.alerts

import android.media.Image
import android.speech.tts.TextToSpeech
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.moblin.android.localized
import com.moblin.android.common.various.color
import com.moblin.android.common.various.countFormatter
import com.moblin.android.integrations.openai.OpenAi
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectDetectionsMode
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.streamingplatforms.kick.KickPusherGiftedSubscriptionsEvent
import com.moblin.android.streamingplatforms.kick.KickPusherKicksGiftedEvent
import com.moblin.android.streamingplatforms.kick.KickPusherRewardRedeemedEvent
import com.moblin.android.streamingplatforms.kick.KickPusherStreamHostEvent
import com.moblin.android.streamingplatforms.kick.KickPusherSubscriptionEvent
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubChannelCheerEvent
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubChannelRaidEvent
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubMessageFragment
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubNotificationChannelFollowEvent
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubNotificationChannelPointsCustomRewardRedemptionAddEvent
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubNotificationChannelSubscribeEvent
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubNotificationChannelSubscriptionGiftEvent
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubNotificationChannelSubscriptionMessageEvent
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubNotificationChannelSubscriptionUpgradeEvent
import com.moblin.android.various.AudioPlayer
import com.moblin.android.various.ChatPostSegment
import com.moblin.android.various.KeepSpeakerAlivePlayer
import com.moblin.android.various.makeChatPostTextSegments
import com.moblin.android.various.settings.SettingsAlertsMediaGalleryItem
import com.moblin.android.various.settings.SettingsWidgetAlertPositionType
import com.moblin.android.various.settings.SettingsWidgetAlerts
import com.moblin.android.various.settings.SettingsWidgetAlertsAlert
import com.moblin.android.various.settings.SettingsWidgetAlertsChatBotCommandImageType
import com.moblin.android.various.settings.SettingsWidgetAlertsCheerBitsAlertOperator
import com.moblin.android.various.settings.SettingsWidgetAlertsKick
import com.moblin.android.various.settings.SettingsWidgetAlertsTwitch
import com.moblin.android.various.settings.SettingsWidgetLayout
import com.moblin.android.various.storages.AlertMediaStorage
import com.moblin.android.various.utils.createSpeechSynthesizer
import com.moblin.android.videoeffects.EffectImage
import com.moblin.android.videoeffects.EffectImageCiImage
import com.moblin.android.view.utils.ChatLineItem
import com.moblin.android.view.utils.ChatLineStyle
import com.moblin.android.view.utils.ChatLineUiView
import java.net.URI
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val mainScope = CoroutineScope(Dispatchers.Main)

sealed class AlertsEffectAlert {
    data class TwitchFollow(val event: TwitchEventSubNotificationChannelFollowEvent) : AlertsEffectAlert()

    data class TwitchSubscribe(val event: TwitchEventSubNotificationChannelSubscribeEvent) : AlertsEffectAlert()

    data class TwitchSubscrptionGift(
        val event: TwitchEventSubNotificationChannelSubscriptionGiftEvent
    ) : AlertsEffectAlert()

    data class TwitchResubscribe(
        val event: TwitchEventSubNotificationChannelSubscriptionMessageEvent
    ) : AlertsEffectAlert()

    data class TwitchSubscriptionUpgrade(
        val event: TwitchEventSubNotificationChannelSubscriptionUpgradeEvent
    ) : AlertsEffectAlert()

    data class TwitchRaid(val event: TwitchEventSubChannelRaidEvent) : AlertsEffectAlert()

    data class TwitchRedemption(
        val event: TwitchEventSubNotificationChannelPointsCustomRewardRedemptionAddEvent
    ) : AlertsEffectAlert()

    data class TwitchCheer(val event: TwitchEventSubChannelCheerEvent) : AlertsEffectAlert()

    data class KickSubscription(val event: KickPusherSubscriptionEvent) : AlertsEffectAlert()

    data class KickGiftedSubscriptions(
        val event: KickPusherGiftedSubscriptionsEvent
    ) : AlertsEffectAlert()

    data class KickHost(val event: KickPusherStreamHostEvent) : AlertsEffectAlert()

    data class KickReward(val event: KickPusherRewardRedeemedEvent) : AlertsEffectAlert()

    data class KickKicks(val event: KickPusherKicksGiftedEvent) : AlertsEffectAlert()

    data class ChatBotCommand(val command: String, val name: String) : AlertsEffectAlert()

    data class SpeechToTextString(val id: UUID) : AlertsEffectAlert()

    data object QuickButton : AlertsEffectAlert()
}

interface AlertsEffectDelegate {
    fun alertsMakeErrorToast(title: String)

    fun alertsMakeTwitchSegments(
        text: String,
        fragments: List<TwitchEventSubMessageFragment>,
        bits: String?
    ): List<ChatPostSegment>

    fun alertsMakeKickSegments(text: String): List<ChatPostSegment>
}

private class Pipeline {
    var playing: Boolean = false
    var messageImage: EffectImageCiImage? = null
    var images: AlertsEffectImages = AlertsEffectGifImages()
    var layout: SettingsWidgetLayout = SettingsWidgetLayout()
    var landmarkSettings: AlertsEffectLandmarkSettings? = null

    fun getImage(presentationTimeStamp: Double): EffectImageCiImage? {
        try {
            return images.getImage(presentationTimeStamp)
        } finally {
            playing = !images.isEmpty()
        }
    }
}

private data class FacePlacement(val center: Offset, val height: Double, val rotation: Float)

class AlertsEffect(
    private var settings: SettingsWidgetAlerts,
    private val delegate: AlertsEffectDelegate?,
    private val mediaStorage: AlertMediaStorage,
    private val bundledImages: List<SettingsAlertsMediaGalleryItem>,
    private val bundledSounds: List<SettingsAlertsMediaGalleryItem>
) : VideoEffect() {
    private var audioPlayer: AudioPlayer? = null
    private var rate: Float = 0.4f
    private var volume: Float = 1.0f
    private val synthesizer = createSpeechSynthesizer()
    private val alertsQueue = ArrayDeque<AlertsEffectAlert>()
    private var isPlaying: Boolean = false
    private var delayAfterPlaying: Double = 3.0
    private var twitchFollowMedia = AlertsEffectMedia()
    private var twitchSubscribeMedia = AlertsEffectMedia()
    private var twitchRaidMedia = AlertsEffectMedia()
    private var twitchCheersMedias = mutableListOf<AlertsEffectMedia>()
    private var twitchRedemptionMedias = mutableListOf<AlertsEffectMedia>()
    private var kickSubscriptionMedia = AlertsEffectMedia()
    private var kickGiftedSubscriptionsMedias = AlertsEffectMedia()
    private var kickHostMedia = AlertsEffectMedia()
    private var kickRewardMedia = AlertsEffectMedia()
    private var kickGiftsMedias = mutableListOf<AlertsEffectMedia>()
    private var chatBotCommandsMedias = mutableListOf<AlertsEffectMedia>()
    private var speechToTextStringsMedias = mutableListOf<AlertsEffectMedia>()
    private var quickButtonMedias = AlertsEffectMedia()
    private var aiBaseUrl: String? = null
    private var pipeline = Pipeline()
    private var messageLineView: ChatLineUiView? = null

    init {
        setSettings(settings)
    }

    override fun needsFaceDetections(presentationTimeStamp: Double): VideoEffectDetectionsMode {
        return if (pipeline.landmarkSettings != null) {
            VideoEffectDetectionsMode.Now(null)
        } else {
            VideoEffectDetectionsMode.Off
        }
    }

    override fun execute(image: Image, info: VideoEffectInfo): Image {
        TODO("OpenGL ES port")
    }

    override fun executeMetalPetal(image: Image, info: VideoEffectInfo): Image {
        TODO("OpenGL ES port")
    }

    override fun isEnabled(): Boolean {
        return pipeline.playing
    }

    fun setSettings(settings: SettingsWidgetAlerts) {
        setTwitchSettings(settings.twitch)
        setKickSettings(settings.kick)
        setChatBotSettings(settings)
        setSpeechToTextSettings(settings)
        setQuickButtonSettings(settings.quickButton)
        aiBaseUrl = settings.ai.baseUrl
        this.settings = settings
    }

    fun getSettings(): SettingsWidgetAlerts {
        return settings
    }

    fun setLayout(layout: SettingsWidgetLayout) {
        processorPipelineQueue.launch {
            pipeline.layout = layout
        }
    }

    fun play(alert: AlertsEffectAlert) {
        if (!shouldAppendAlert(alert)) {
            return
        }
        alertsQueue.addLast(alert)
        tryPlayNextAlert()
    }

    private fun shouldAppendAlert(alert: AlertsEffectAlert): Boolean {
        if (alert !is AlertsEffectAlert.QuickButton) {
            return true
        }
        return !isPlaying
    }

    private fun setChatBotSettings(settings: SettingsWidgetAlerts) {
        chatBotCommandsMedias.clear()
        for (command in settings.chatBot.commands) {
            val media = AlertsEffectMedia()
            media.update(command.alert, mediaStorage, bundledImages, bundledSounds)
            chatBotCommandsMedias.add(media)
        }
    }

    private fun setSpeechToTextSettings(settings: SettingsWidgetAlerts) {
        speechToTextStringsMedias.clear()
        for (string in settings.speechToText.strings) {
            val media = AlertsEffectMedia()
            media.update(string.alert, mediaStorage, bundledImages, bundledSounds)
            speechToTextStringsMedias.add(media)
        }
    }

    private fun tryPlayNextAlert() {
        if (isPlaying) {
            return
        }
        val alert = alertsQueue.removeFirstOrNull() ?: return
        when (alert) {
            is AlertsEffectAlert.TwitchFollow -> playTwitchFollow(alert.event)
            is AlertsEffectAlert.TwitchSubscribe -> playTwitchSubscribe(alert.event)
            is AlertsEffectAlert.TwitchSubscrptionGift -> playTwitchSubscriptionGift(alert.event)
            is AlertsEffectAlert.TwitchResubscribe -> playTwitchResubscribe(alert.event)
            is AlertsEffectAlert.TwitchSubscriptionUpgrade -> playTwitchSubscriptionUpgrade(alert.event)
            is AlertsEffectAlert.TwitchRaid -> playTwitchRaid(alert.event)
            is AlertsEffectAlert.TwitchRedemption -> playTwitchRedemption(alert.event)
            is AlertsEffectAlert.TwitchCheer -> playTwitchCheer(alert.event)
            is AlertsEffectAlert.KickSubscription -> playKickSubscription(alert.event)
            is AlertsEffectAlert.KickGiftedSubscriptions -> playKickGiftedSubscriptions(alert.event)
            is AlertsEffectAlert.KickHost -> playKickHost(alert.event)
            is AlertsEffectAlert.KickReward -> playKickReward(alert.event)
            is AlertsEffectAlert.KickKicks -> playKickKicks(alert.event)
            is AlertsEffectAlert.ChatBotCommand -> playChatBotCommand(alert.command, alert.name)
            is AlertsEffectAlert.SpeechToTextString -> playSpeechToTextString(alert.id)
            AlertsEffectAlert.QuickButton -> playQuickButton()
        }
    }

    private fun playChatBotCommand(command: String, name: String) {
        val commandIndex = settings.chatBot.commands.indexOfFirst {
            command == it.name && it.alert.enabled
        }
        if (commandIndex == -1) {
            return
        }
        if (commandIndex >= chatBotCommandsMedias.size) {
            return
        }
        val media = chatBotCommandsMedias[commandIndex]
        val commandSettings = settings.chatBot.commands[commandIndex]
        when (commandSettings.imageType) {
            SettingsWidgetAlertsChatBotCommandImageType.file -> play(
                media = media,
                username = name,
                message = command,
                settings = commandSettings.alert
            )
            else -> Unit
        }
    }

    private fun playSpeechToTextString(id: UUID) {
        val stringIndex = settings.speechToText.strings.indexOfFirst {
            it.id == id && it.alert.enabled
        }
        if (stringIndex == -1) {
            return
        }
        if (stringIndex >= speechToTextStringsMedias.size) {
            return
        }
        play(
            media = speechToTextStringsMedias[stringIndex],
            username = "",
            message = "",
            settings = settings.speechToText.strings[stringIndex].alert,
            delayAfterPlaying = 0.0
        )
    }

    private fun play(
        media: AlertsEffectMedia,
        username: String,
        message: String,
        segments: List<ChatPostSegment>? = null,
        settings: SettingsWidgetAlertsAlert,
        delayAfterPlaying: Double = 3.0
    ) {
        isPlaying = true
        this.delayAfterPlaying = delayAfterPlaying
        setMessage(
            username = username,
            segments = segments ?: makeChatPostTextSegments(text = message),
            settings = settings
        )
        val landmarkSettings = calculateLandmarkSettings(settings)
        val player = media.getPlayer()
        val ai = this.settings.ai
        val aiBaseUrl = this.aiBaseUrl
        if (this.settings.aiEnabled && aiBaseUrl != null && ai.isConfigured()) {
            OpenAi(baseUrl = URI(aiBaseUrl), apiKey = ai.apiKey)
                .ask(message, model = ai.model, role = ai.personality) { result ->
                    var message = message
                    result
                        .onSuccess { answer ->
                            message += ". " + answer
                        }
                        .onFailure { error ->
                            delegate?.alertsMakeErrorToast(
                                title = localized("Got no AI response: ${error.message}")
                            )
                        }
                    play(
                        player = player,
                        username = username,
                        message = message,
                        landmarkSettings = landmarkSettings,
                        settings = settings
                    )
                }
        } else {
            play(
                player = player,
                username = username,
                message = message,
                landmarkSettings = landmarkSettings,
                settings = settings
            )
        }
    }

    private fun play(
        player: AlertsEffectPlayer,
        username: String,
        message: String,
        landmarkSettings: AlertsEffectLandmarkSettings?,
        settings: SettingsWidgetAlertsAlert
    ) {
        processorPipelineQueue.launch {
            pipeline.images = player.images
            pipeline.playing = true
            pipeline.landmarkSettings = landmarkSettings
        }
        val soundUrl = player.soundUrl
        if (soundUrl != null) {
            audioPlayer = runCatching { AudioPlayer(soundUrl) }.getOrNull()
            audioPlayer?.play()
        }
        if (settings.textToSpeechEnabled) {
            say(username = username, message = message, settings = settings)
        }
    }

    private fun say(username: String, message: String, settings: SettingsWidgetAlertsAlert) {
        val voice = getVoice(settings) ?: return
        mainScope.launch {
            delay((settings.textToSpeechDelay * 1000.0).toLong())
            synthesizer.setSpeechRate(rate)
            synthesizer.setPitch(0.8f)
            synthesizer.setVolume(volume)
            synthesizer.speak(
                "$username $message",
                TextToSpeech.QUEUE_FLUSH,
                null,
                null
            )
            KeepSpeakerAlivePlayer.shared.audioPlayed()
        }
    }

    private fun getVoice(settings: SettingsWidgetAlertsAlert): String? {
        val language = Locale.getDefault().language
        settings.textToSpeechLanguageVoices[language]?.apple?.voice?.let { return it }
        TODO("No Android counterpart for AVSpeechSynthesisVoice.speechVoices()")
    }

    private fun setMessage(
        username: String,
        segments: List<ChatPostSegment>,
        settings: SettingsWidgetAlertsAlert
    ) {
        val style = ChatLineStyle(
            fontSize = settings.fontSize.toFloat(),
            borderColor = Color.Black,
            borderWidth = 2f,
            leadingPadding = 0f,
            fontWeight = settings.fontWeight.toSystem(),
            fontDesign = settings.fontDesign.toSystem()
        )
        val textColor = settings.textColor.color()
        val items = mutableListOf<ChatLineItem>()
        items.add(
            style.textItem(
                text = "$username ",
                color = settings.accentColor.color(),
                deleted = false
            )
        )
        for (segment in segments) {
            val text = segment.text
            if (text != null) {
                items.add(style.textItem(text = text, color = textColor, deleted = false))
            }
            val url = segment.url?.url(animated = false)
            if (url != null) {
                items.add(style.emoteItem(url = url, deleted = false))
                items.add(style.textItem(text = " ", color = textColor, deleted = false))
            }
        }
        getMessageLineView().setContent(style.content(items = items))
        updateMessageImage()
    }

    private fun getMessageLineView(): ChatLineUiView {
        val messageLineView = this.messageLineView
        if (messageLineView != null) {
            return messageLineView
        }
        val lineView = ChatLineUiView(context = TODO("no Android counterpart for a UIView context here"))
        lineView.onImageLoaded = {
            updateMessageImage()
        }
        this.messageLineView = lineView
        return lineView
    }

    private fun updateMessageImage() {
        TODO("No Android counterpart for UIGraphicsImageRenderer")
    }

    private fun isInRectangle(
        x: Double,
        y: Double,
        rectangle: AlertsEffectBackgroundLandmarkRectangle
    ): Boolean {
        return x > rectangle.topLeftX && x < rectangle.bottomRightX &&
            y > rectangle.topLeftY && y < rectangle.bottomRightY
    }

    private fun calculateLandmark(settings: SettingsWidgetAlertsAlert): AlertsEffectFaceLandmark {
        val centerX = settings.facePosition.x + settings.facePosition.width / 2
        val centerY = settings.facePosition.y + settings.facePosition.height / 2
        return if (isInRectangle(centerX, centerY, alertsEffectBackgroundLeftEyeRectangle)) {
            AlertsEffectFaceLandmark.LEFT_EYE
        } else if (isInRectangle(centerX, centerY, alertsEffectBackgroundRightEyeRectangle)) {
            AlertsEffectFaceLandmark.RIGHT_EYE
        } else if (isInRectangle(centerX, centerY, alertsEffectBackgroundMouthRectangle)) {
            AlertsEffectFaceLandmark.MOUTH
        } else {
            AlertsEffectFaceLandmark.FACE
        }
    }

    private fun calculateLandmarkSettings(
        settings: SettingsWidgetAlertsAlert
    ): AlertsEffectLandmarkSettings? {
        if (settings.positionType == SettingsWidgetAlertPositionType.face) {
            val landmark = calculateLandmark(settings)
            val centerX = settings.facePosition.x + settings.facePosition.width / 2
            val centerY = settings.facePosition.y + settings.facePosition.height / 2
            val landmarkRectangle = when (landmark) {
                AlertsEffectFaceLandmark.FACE -> alertsEffectBackgroundFaceRectangle
                AlertsEffectFaceLandmark.LEFT_EYE -> alertsEffectBackgroundLeftEyeRectangle
                AlertsEffectFaceLandmark.RIGHT_EYE -> alertsEffectBackgroundRightEyeRectangle
                AlertsEffectFaceLandmark.MOUTH -> alertsEffectBackgroundMouthRectangle
            }
            val x = (centerX - landmarkRectangle.topLeftX) / landmarkRectangle.width()
            val y = (centerY - landmarkRectangle.topLeftY) / landmarkRectangle.height()
            val height = settings.facePosition.height / alertsEffectBackgroundFaceRectangle.height()
            return AlertsEffectLandmarkSettings(
                landmark = landmark,
                height = height,
                centerX = x,
                centerY = y
            )
        } else {
            return null
        }
    }

    private fun getNext(presentationTimeStamp: Double): Pair<EffectImageCiImage?, EffectImageCiImage?> {
        try {
            val image = pipeline.getImage(presentationTimeStamp)
            return if (image != null) {
                Pair(image, pipeline.messageImage)
            } else {
                Pair(null, null)
            }
        } finally {
            if (!pipeline.playing) {
                pipeline.landmarkSettings = null
                mainScope.launch {
                    delay((delayAfterPlaying * 1000.0).toLong())
                    isPlaying = false
                    tryPlayNextAlert()
                }
            }
        }
    }

    private fun calcFacePlacement(
        detection: Any,
        imageSize: Size,
        landmarkSettings: AlertsEffectLandmarkSettings
    ): FacePlacement? {
        TODO("Vision has no Android counterpart")
    }

    private fun executePositionFace(
        image: EffectImage,
        faceDetections: List<Any>?,
        alertImage: EffectImage,
        landmarkSettings: AlertsEffectLandmarkSettings
    ): EffectImage {
        TODO("Vision has no Android counterpart")
    }

    private fun executePositionFaceMetalPetal(
        image: EffectImage,
        faceDetections: List<Any>?,
        alertImage: EffectImage,
        landmarkSettings: AlertsEffectLandmarkSettings
    ): EffectImage {
        TODO("OpenGL ES port")
    }

    private fun alertAndMessageSize(alertSize: Size, messageSize: Size): Size {
        return Size(alertSize.width, alertSize.height + messageSize.height)
    }

    private fun executePositionScene(
        image: EffectImage,
        alertImage: EffectImage,
        messageImage: EffectImage,
        layout: SettingsWidgetLayout
    ): EffectImage {
        TODO("OpenGL ES port")
    }

    private fun executePositionSceneMetalPetal(
        image: EffectImage,
        alertImage: EffectImage,
        messageImage: EffectImage,
        layout: SettingsWidgetLayout
    ): EffectImage {
        TODO("OpenGL ES port")
    }

    private fun setTwitchSettings(twitch: SettingsWidgetAlertsTwitch) {
        twitchFollowMedia.update(twitch.follows, mediaStorage, bundledImages, bundledSounds)
        twitchSubscribeMedia.update(twitch.subscriptions, mediaStorage, bundledImages, bundledSounds)
        twitchRaidMedia.update(twitch.raids, mediaStorage, bundledImages, bundledSounds)
        twitchCheersMedias.clear()
        for (cheerBits in twitch.cheerBits) {
            val media = AlertsEffectMedia()
            media.update(cheerBits.alert, mediaStorage, bundledImages, bundledSounds)
            twitchCheersMedias.add(media)
        }
        twitchRedemptionMedias.clear()
        for (redemption in twitch.redemptions) {
            val media = AlertsEffectMedia()
            media.update(redemption, mediaStorage, bundledImages, bundledSounds)
            twitchRedemptionMedias.add(media)
        }
    }

    private fun playTwitchFollow(event: TwitchEventSubNotificationChannelFollowEvent) {
        if (!settings.twitch.follows.enabled) {
            return
        }
        play(
            media = twitchFollowMedia,
            username = event.user_name,
            message = localized("just followed!"),
            settings = settings.twitch.follows
        )
    }

    private fun playTwitchSubscribe(event: TwitchEventSubNotificationChannelSubscribeEvent) {
        if (!settings.twitch.subscriptions.enabled) {
            return
        }
        val message = if (event.isPrime()) {
            localized("just subscribed with Prime!")
        } else {
            localized("just subscribed tier ${event.tierAsNumber()}!")
        }
        play(
            media = twitchSubscribeMedia,
            username = event.user_name,
            message = message,
            settings = settings.twitch.subscriptions
        )
    }

    private fun playTwitchSubscriptionGift(
        event: TwitchEventSubNotificationChannelSubscriptionGiftEvent
    ) {
        if (!settings.twitch.subscriptions.enabled) {
            return
        }
        play(
            media = twitchSubscribeMedia,
            username = event.user_name ?: "Anomymous",
            message = localized(
                "just gifted ${event.total} tier ${event.tierAsNumber()} subscriptions!"
            ),
            settings = settings.twitch.subscriptions
        )
    }

    private fun playTwitchResubscribe(
        event: TwitchEventSubNotificationChannelSubscriptionMessageEvent
    ) {
        if (!settings.twitch.subscriptions.enabled) {
            return
        }
        val text = if (event.streak_months != null) {
            localized(
                "just resubscribed tier ${event.tierAsNumber()} for ${event.cumulative_months} months, ${event.streak_months} in a row!"
            )
        } else {
            localized(
                "just resubscribed tier ${event.tierAsNumber()} for ${event.cumulative_months} months!"
            )
        }
        val message = if (event.message.text.isEmpty()) text else "$text ${event.message.text}"
        play(
            media = twitchSubscribeMedia,
            username = event.user_name,
            message = message,
            segments = delegate?.alertsMakeTwitchSegments(
                text = text,
                fragments = event.message.fragments,
                bits = null
            ),
            settings = settings.twitch.subscriptions
        )
    }

    private fun playTwitchSubscriptionUpgrade(
        event: TwitchEventSubNotificationChannelSubscriptionUpgradeEvent
    ) {
        if (!settings.twitch.subscriptions.enabled) {
            return
        }
        val tier = event.tierAsNumber()
        val message = if (tier != null) {
            localized("just converted their Prime subscription to tier $tier!")
        } else {
            localized("just continued their gift subscription!")
        }
        play(
            media = twitchSubscribeMedia,
            username = event.user_name,
            message = message,
            settings = settings.twitch.subscriptions
        )
    }

    private fun playTwitchRaid(event: TwitchEventSubChannelRaidEvent) {
        if (!settings.twitch.raids.enabled) {
            return
        }
        play(
            media = twitchRaidMedia,
            username = event.from_broadcaster_user_name,
            message = localized("raided with a party of ${event.viewers}!"),
            settings = settings.twitch.raids
        )
    }

    private fun playTwitchRedemption(
        event: TwitchEventSubNotificationChannelPointsCustomRewardRedemptionAddEvent
    ) {
        for ((index, redemption) in settings.twitch.redemptions.withIndex()) {
            if (!redemption.enabled) {
                continue
            }
            play(
                media = twitchRedemptionMedias[index],
                username = event.user_name,
                message = localized("redeemed ${event.reward.title}!"),
                settings = redemption
            )
            break
        }
    }

    private fun playTwitchCheer(event: TwitchEventSubChannelCheerEvent) {
        for ((index, cheerBit) in settings.twitch.cheerBits.withIndex()) {
            if (!cheerBit.alert.enabled) {
                continue
            }
            when (cheerBit.comparisonOperator) {
                SettingsWidgetAlertsCheerBitsAlertOperator.equal -> {
                    if (event.bits != cheerBit.bits) {
                        continue
                    }
                }
                SettingsWidgetAlertsCheerBitsAlertOperator.greaterEqual -> {
                    if (event.bits < cheerBit.bits) {
                        continue
                    }
                }
            }
            if (index >= twitchCheersMedias.size) {
                return
            }
            val bits = countFormatter.format(event.bits)
            val message = localized("cheered $bits bits! ${event.message}")
            play(
                media = twitchCheersMedias[index],
                username = event.user_name ?: "Anonymous",
                message = message,
                segments = delegate?.alertsMakeTwitchSegments(
                    text = message,
                    fragments = emptyList(),
                    bits = ""
                ),
                settings = cheerBit.alert
            )
            break
        }
    }

    private fun setKickSettings(kick: SettingsWidgetAlertsKick) {
        kickSubscriptionMedia.update(kick.subscriptions, mediaStorage, bundledImages, bundledSounds)
        kickGiftedSubscriptionsMedias.update(
            kick.giftedSubscriptions,
            mediaStorage,
            bundledImages,
            bundledSounds
        )
        kickHostMedia.update(kick.hosts, mediaStorage, bundledImages, bundledSounds)
        kickRewardMedia.update(kick.rewards, mediaStorage, bundledImages, bundledSounds)
        kickGiftsMedias.clear()
        for (kickGift in kick.kickGifts) {
            val media = AlertsEffectMedia()
            media.update(kickGift.alert, mediaStorage, bundledImages, bundledSounds)
            kickGiftsMedias.add(media)
        }
    }

    private fun playKickSubscription(event: KickPusherSubscriptionEvent) {
        if (!settings.kick.subscriptions.enabled) {
            return
        }
        play(
            media = kickSubscriptionMedia,
            username = event.username,
            message = localized(
                "just subscribed! They've been subscribed for ${event.months} months!"
            ),
            settings = settings.kick.subscriptions
        )
    }

    private fun playKickGiftedSubscriptions(event: KickPusherGiftedSubscriptionsEvent) {
        if (!settings.kick.giftedSubscriptions.enabled) {
            return
        }
        play(
            media = kickGiftedSubscriptionsMedias,
            username = event.gifter_username,
            message = localized(
                "just gifted ${event.gifted_usernames.size} subscription(s)! They've gifted ${event.gifter_total} in total!"
            ),
            settings = settings.kick.giftedSubscriptions
        )
    }

    private fun playKickHost(event: KickPusherStreamHostEvent) {
        if (!settings.kick.hosts.enabled) {
            return
        }
        play(
            media = kickHostMedia,
            username = event.host_username,
            message = localized("is now hosting with ${event.number_viewers} viewers!"),
            settings = settings.kick.hosts
        )
    }

    private fun playKickReward(event: KickPusherRewardRedeemedEvent) {
        if (!settings.kick.rewards.enabled) {
            return
        }
        val baseMessage = localized("redeemed ${event.reward_title}")
        val message = if (event.user_input.isEmpty()) baseMessage else "$baseMessage: ${event.user_input}"
        play(
            media = kickRewardMedia,
            username = event.username,
            message = message,
            segments = delegate?.alertsMakeKickSegments(text = message),
            settings = settings.kick.rewards
        )
    }

    private fun playKickKicks(event: KickPusherKicksGiftedEvent) {
        for ((index, kickGift) in settings.kick.kickGifts.withIndex()) {
            val matches = when (kickGift.comparisonOperator) {
                SettingsWidgetAlertsCheerBitsAlertOperator.equal ->
                    event.gift.amount == kickGift.amount
                SettingsWidgetAlertsCheerBitsAlertOperator.greaterEqual ->
                    event.gift.amount >= kickGift.amount
            }
            if (!matches || !kickGift.alert.enabled) {
                continue
            }
            if (index >= kickGiftsMedias.size) {
                return
            }
            val formattedAmount = countFormatter.format(event.gift.amount)
            play(
                media = kickGiftsMedias[index],
                username = event.sender.username,
                message = localized("sent ${event.gift.name} $formattedAmount Kicks!"),
                settings = kickGift.alert
            )
            break
        }
    }

    private fun setQuickButtonSettings(alert: SettingsWidgetAlertsAlert) {
        quickButtonMedias.update(alert, mediaStorage, bundledImages, bundledSounds)
    }

    private fun playQuickButton() {
        if (!settings.quickButton.enabled) {
            return
        }
        play(
            media = quickButtonMedias,
            username = "",
            message = "",
            settings = settings.quickButton,
            delayAfterPlaying = 0.0
        )
    }
}
