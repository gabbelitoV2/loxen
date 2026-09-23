package com.moblin.android.videoeffects.alerts

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.moblin.android.AppDelegate
import com.moblin.android.common.various.RgbColor
import com.moblin.android.common.various.countFormatter
import com.moblin.android.integrations.openai.OpenAi
import com.moblin.android.integrations.openai.OpenAiError
import com.moblin.android.localized
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectDetectionsMode
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.avfoundation.AVSpeechSynthesizer
import com.moblin.android.platform.avfoundation.AVSpeechSynthesisVoice
import com.moblin.android.platform.avfoundation.AVSpeechUtterance
import com.moblin.android.platform.coregraphics.CGAffineTransform
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.metalpetal.MTILayer
import com.moblin.android.platform.metalpetal.MTIMultilayerCompositingFilter
import com.moblin.android.platform.uikit.UIGraphicsImageRenderer
import com.moblin.android.platform.uikit.UIGraphicsImageRendererFormat
import com.moblin.android.platform.uikit.cgImage
import com.moblin.android.platform.uikit.frame
import com.moblin.android.platform.vision.VNFaceObservation
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
import com.moblin.android.various.ChatPostSegment
import com.moblin.android.various.KeepSpeakerAlivePlayer
import com.moblin.android.various.calcBoundingBox
import com.moblin.android.various.calcFaceAngle
import com.moblin.android.various.makeChatPostTextSegments
import com.moblin.android.various.rotateFace
import com.moblin.android.various.rotatePoint
import com.moblin.android.various.stableBoundingBox
import com.moblin.android.various.settings.SettingsAlertsMediaGalleryItem
import com.moblin.android.various.settings.SettingsFontDesign
import com.moblin.android.various.settings.SettingsFontWeight
import com.moblin.android.various.settings.SettingsWidgetAlertPositionType
import com.moblin.android.various.settings.SettingsWidgetAlerts
import com.moblin.android.various.settings.SettingsWidgetAlertsAlert
import com.moblin.android.various.settings.SettingsWidgetAlertsChatBotCommandImageType
import com.moblin.android.various.settings.SettingsWidgetAlertsCheerBitsAlertOperator
import com.moblin.android.various.settings.SettingsWidgetAlertsKick
import com.moblin.android.various.settings.SettingsWidgetAlertsTwitch
import com.moblin.android.various.settings.SettingsWidgetLayout
import com.moblin.android.various.storages.AlertMediaStorage
import com.moblin.android.videoeffects.EffectImageCiImage
import com.moblin.android.videoeffects.layoutPosition
import com.moblin.android.videoeffects.toEffectImage
import com.moblin.android.view.utils.ChatLineStyle
import com.moblin.android.view.utils.ChatLineUiView
import com.moblin.android.view.utils.FontDesign
import java.net.URI
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.moblin.android.various.AudioPlayer
import com.moblin.android.videoeffects.scaled
import com.moblin.android.videoeffects.translated
import com.moblin.android.common.various.uiColor

sealed class AlertsEffectAlert {
    data class TwitchFollow(val event: TwitchEventSubNotificationChannelFollowEvent) : AlertsEffectAlert()

    data class TwitchSubscribe(val event: TwitchEventSubNotificationChannelSubscribeEvent) :
        AlertsEffectAlert()

    data class TwitchSubscrptionGift(
        val event: TwitchEventSubNotificationChannelSubscriptionGiftEvent,
    ) : AlertsEffectAlert()

    data class TwitchResubscribe(
        val event: TwitchEventSubNotificationChannelSubscriptionMessageEvent,
    ) : AlertsEffectAlert()

    data class TwitchSubscriptionUpgrade(
        val event: TwitchEventSubNotificationChannelSubscriptionUpgradeEvent,
    ) : AlertsEffectAlert()

    data class TwitchRaid(val event: TwitchEventSubChannelRaidEvent) : AlertsEffectAlert()

    data class TwitchRedemption(
        val event: TwitchEventSubNotificationChannelPointsCustomRewardRedemptionAddEvent,
    ) : AlertsEffectAlert()

    data class TwitchCheer(val event: TwitchEventSubChannelCheerEvent) : AlertsEffectAlert()

    data class KickSubscription(val event: KickPusherSubscriptionEvent) : AlertsEffectAlert()

    data class KickGiftedSubscriptions(val event: KickPusherGiftedSubscriptionsEvent) :
        AlertsEffectAlert()

    data class KickHost(val event: KickPusherStreamHostEvent) : AlertsEffectAlert()

    data class KickReward(val event: KickPusherRewardRedeemedEvent) : AlertsEffectAlert()

    data class KickKicks(val event: KickPusherKicksGiftedEvent) : AlertsEffectAlert()

    data class ChatBotCommand(val command: String, val name: String) : AlertsEffectAlert()

    data class SpeechToTextString(val id: UUID) : AlertsEffectAlert()

    object QuickButton : AlertsEffectAlert()
}

interface AlertsEffectDelegate {
    fun alertsMakeErrorToast(title: String)

    fun alertsMakeTwitchSegments(
        text: String,
        fragments: List<TwitchEventSubMessageFragment>,
        bits: String?,
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

private data class FacePlacement(val center: CGPoint, val height: Double, val rotation: Double)

class AlertsEffect(
    private var settings: SettingsWidgetAlerts,
    delegate: AlertsEffectDelegate,
    private val mediaStorage: AlertMediaStorage,
    private val bundledImages: List<SettingsAlertsMediaGalleryItem>,
    private val bundledSounds: List<SettingsAlertsMediaGalleryItem>,
) : VideoEffect() {
    private var audioPlayer: AudioPlayer? = null
    private var rate: Float = 0.4f
    private var volume: Float = 1.0f
    private val synthesizer = AVSpeechSynthesizer().also { it.usesApplicationAudioSession = false }
    private var alertsQueue: ArrayDeque<AlertsEffectAlert> = ArrayDeque()
    private val delegate: AlertsEffectDelegate? = delegate
    private var isPlaying: Boolean = false
    private var delayAfterPlaying: Double = 3.0
    private var twitchFollowMedia = AlertsEffectMedia()
    private var twitchSubscribeMedia = AlertsEffectMedia()
    private var twitchRaidMedia = AlertsEffectMedia()
    private var twitchCheersMedias: MutableList<AlertsEffectMedia> = mutableListOf()
    private var twitchRedemptionMedias: MutableList<AlertsEffectMedia> = mutableListOf()
    private var kickSubscriptionMedia = AlertsEffectMedia()
    private var kickGiftedSubscriptionsMedias = AlertsEffectMedia()
    private var kickHostMedia = AlertsEffectMedia()
    private var kickRewardMedia = AlertsEffectMedia()
    private var kickGiftsMedias: MutableList<AlertsEffectMedia> = mutableListOf()
    private var chatBotCommandsMedias: MutableList<AlertsEffectMedia> = mutableListOf()
    private var speechToTextStringsMedias: MutableList<AlertsEffectMedia> = mutableListOf()
    private var quickButtonMedias = AlertsEffectMedia()
    private var aiBaseUrl: URI? = null
    private var pipeline = Pipeline()
    private var messageLineView: ChatLineUiView? = null

    init {
        setSettings(settings = settings)
    }

    override fun needsFaceDetections(interval: Double): VideoEffectDetectionsMode {
        return if (pipeline.landmarkSettings != null) {
            VideoEffectDetectionsMode.Now(null)
        } else {
            VideoEffectDetectionsMode.Off
        }
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        val (alertImage, messageImage) = getNext(info.presentationTimeStamp / 1_000_000.0)
        if (alertImage == null || messageImage == null) {
            return image
        }
        val landmarkSettings = pipeline.landmarkSettings
        return if (landmarkSettings != null) {
            executePositionFace(
                image,
                info.sceneFaceDetections(),
                alertImage.getCiImage(),
                landmarkSettings,
            )
        } else {
            executePositionScene(
                image,
                alertImage.getCiImage(),
                messageImage.getCiImage(),
                pipeline.layout,
            )
        }
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        val (alertImage, messageImage) = getNext(info.presentationTimeStamp / 1_000_000.0)
        if (alertImage == null || messageImage == null) {
            return image
        }
        val landmarkSettings = pipeline.landmarkSettings
        return if (landmarkSettings != null) {
            executePositionFaceMetalPetal(
                image,
                info.sceneFaceDetections(),
                alertImage.getMetalPetalImage(),
                landmarkSettings,
            )
        } else {
            executePositionSceneMetalPetal(
                image,
                alertImage.getMetalPetalImage(),
                messageImage.getMetalPetalImage(),
                pipeline.layout,
            )
        }
    }

    override fun isEnabled(): Boolean {
        return pipeline.playing
    }

    fun setSettings(settings: SettingsWidgetAlerts) {
        setTwitchSettings(twitch = settings.twitch)
        setKickSettings(kick = settings.kick)
        setChatBotSettings(settings = settings)
        setSpeechToTextSettings(settings = settings)
        setQuickButtonSettings(alert = settings.quickButton)
        aiBaseUrl = runCatching { URI(settings.ai.baseUrl) }.getOrNull()
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
        if (!shouldAppendAlert(alert = alert)) {
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
        chatBotCommandsMedias = mutableListOf()
        for (command in settings.chatBot.commands) {
            val media = AlertsEffectMedia()
            media.update(command.alert, mediaStorage, bundledImages, bundledSounds)
            chatBotCommandsMedias.add(media)
        }
    }

    private fun setSpeechToTextSettings(settings: SettingsWidgetAlerts) {
        speechToTextStringsMedias = mutableListOf()
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
            is AlertsEffectAlert.TwitchFollow -> playTwitchFollow(event = alert.event)
            is AlertsEffectAlert.TwitchSubscribe -> playTwitchSubscribe(event = alert.event)
            is AlertsEffectAlert.TwitchSubscrptionGift ->
                playTwitchSubscriptionGift(event = alert.event)
            is AlertsEffectAlert.TwitchResubscribe -> playTwitchResubscribe(event = alert.event)
            is AlertsEffectAlert.TwitchSubscriptionUpgrade ->
                playTwitchSubscriptionUpgrade(event = alert.event)
            is AlertsEffectAlert.TwitchRaid -> playTwitchRaid(event = alert.event)
            is AlertsEffectAlert.TwitchRedemption -> playTwitchRedemption(event = alert.event)
            is AlertsEffectAlert.TwitchCheer -> playTwitchCheer(event = alert.event)
            is AlertsEffectAlert.KickSubscription -> playKickSubscription(event = alert.event)
            is AlertsEffectAlert.KickGiftedSubscriptions ->
                playKickGiftedSubscriptions(event = alert.event)
            is AlertsEffectAlert.KickHost -> playKickHost(event = alert.event)
            is AlertsEffectAlert.KickReward -> playKickReward(event = alert.event)
            is AlertsEffectAlert.KickKicks -> playKickKicks(event = alert.event)
            is AlertsEffectAlert.ChatBotCommand ->
                playChatBotCommand(command = alert.command, name = alert.name)
            is AlertsEffectAlert.SpeechToTextString -> playSpeechToTextString(id = alert.id)
            is AlertsEffectAlert.QuickButton -> playQuickButton()
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
                settings = commandSettings.alert,
            )
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
            delayAfterPlaying = 0.0,
        )
    }

    private fun play(
        media: AlertsEffectMedia,
        username: String,
        message: String,
        segments: List<ChatPostSegment>? = null,
        settings: SettingsWidgetAlertsAlert,
        delayAfterPlaying: Double = 3.0,
    ) {
        isPlaying = true
        this.delayAfterPlaying = delayAfterPlaying
        setMessage(
            username = username,
            segments = segments ?: makeChatPostTextSegments(text = message),
            settings = settings,
        )
        val landmarkSettings = calculateLandmarkSettings(settings = settings)
        val player = media.getPlayer()
        val ai = this.settings.ai
        val aiBaseUrl = this.aiBaseUrl
        if (this.settings.aiEnabled && aiBaseUrl != null && ai.isConfigured()) {
            OpenAi(baseUrl = aiBaseUrl, apiKey = ai.apiKey).ask(
                content = message,
                model = ai.model,
                role = ai.personality,
            ) { result ->
                var message = message
                val answer = result.getOrNull()
                if (answer != null) {
                    message += ". " + answer
                } else {
                    val error = result.exceptionOrNull()
                    val description = (error as? OpenAiError)?.description ?: error?.message ?: ""
                    delegate?.alertsMakeErrorToast(
                        title = localized("Got no AI response: $description"),
                    )
                }
                CoroutineScope(Dispatchers.Main.immediate).launch {
                    play(
                        player = player,
                        username = username,
                        message = message,
                        landmarkSettings = landmarkSettings,
                        settings = settings,
                    )
                }
            }
        } else {
            play(
                player = player,
                username = username,
                message = message,
                landmarkSettings = landmarkSettings,
                settings = settings,
            )
        }
    }

    private fun play(
        player: AlertsEffectPlayer,
        username: String,
        message: String,
        landmarkSettings: AlertsEffectLandmarkSettings?,
        settings: SettingsWidgetAlertsAlert,
    ) {
        processorPipelineQueue.launch {
            pipeline.images = player.images
            pipeline.playing = true
            pipeline.landmarkSettings = landmarkSettings
        }
        val soundUrl = player.soundUrl
        if (soundUrl != null) {
            audioPlayer = runCatching { AudioPlayer(contentsOf = soundUrl) }.getOrNull()
            audioPlayer?.play()
        }
        if (settings.textToSpeechEnabled) {
            say(username = username, message = message, settings = settings)
        }
    }

    private fun say(username: String, message: String, settings: SettingsWidgetAlertsAlert) {
        val voice = getVoice(settings = settings) ?: return
        val utterance = AVSpeechUtterance(string = "$username $message")
        utterance.rate = rate
        utterance.pitchMultiplier = 0.8f
        utterance.volume = volume
        utterance.voice = voice
        CoroutineScope(Dispatchers.Main.immediate).launch {
            delay((settings.textToSpeechDelay * 1000.0).toLong())
            synthesizer.speak(utterance)
            KeepSpeakerAlivePlayer.shared.audioPlayed()
        }
    }

    private fun getVoice(settings: SettingsWidgetAlertsAlert): AVSpeechSynthesisVoice? {
        val language = Locale.getDefault().language
        if (language.isEmpty()) {
            return null
        }
        val voiceIdentifier = settings.textToSpeechLanguageVoices[language]?.apple?.voice
        if (voiceIdentifier != null) {
            return AVSpeechSynthesisVoice(identifier = voiceIdentifier)
        }
        val voice = AVSpeechSynthesisVoice.speechVoices()
            .filter { it.language.startsWith(language) }
            .firstOrNull()
        if (voice != null) {
            return AVSpeechSynthesisVoice(identifier = voice.identifier)
        }
        return null
    }

    private fun setMessage(
        username: String,
        segments: List<ChatPostSegment>,
        settings: SettingsWidgetAlertsAlert,
    ) {
        val style = ChatLineStyle(
            fontSize = settings.fontSize.toFloat(),
            borderColor = Color.Black,
            borderWidth = 2f,
            leadingPadding = 0f,
            fontWeight = settings.fontWeight.toUiKit(),
            fontDesign = settings.fontDesign.toUiKit(),
        )
        val textColor = settings.textColor.uiColor()
        val items = mutableListOf(
            style.textItem(
                text = "$username ",
                color = settings.accentColor.uiColor(),
                deleted = false,
            ),
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
        val existing = messageLineView
        if (existing != null) {
            return existing
        }
        val lineView = ChatLineUiView(AppDelegate.context)
        val weakSelf = java.lang.ref.WeakReference(this)
        lineView.onImageLoaded = { weakSelf.get()?.updateMessageImage() }
        messageLineView = lineView
        return lineView
    }

    private fun updateMessageImage() {
        val lineView = getMessageLineView()
        val size = lineView.size(availableWidth = 1000f)
        lineView.frame = CGRect(x = 0.0, y = 0.0, width = size.width, height = size.height)
        val format = UIGraphicsImageRendererFormat()
        format.scale = 1f
        format.opaque = false
        val image = UIGraphicsImageRenderer(
            size = CGSize(width = size.width, height = size.height),
            format = format,
        ).image { context ->
            lineView.draw(context.cgContext)
        }
        val messageImage = CIImage(cgImage = image.cgImage).toEffectImage(isOpaque = false)
        processorPipelineQueue.launch {
            pipeline.messageImage = messageImage
        }
    }

    private fun isInRectangle(
        x: Double,
        y: Double,
        rectangle: AlertsEffectBackgroundLandmarkRectangle,
    ): Boolean {
        return x > rectangle.topLeftX && x < rectangle.bottomRightX && y > rectangle.topLeftY &&
            y < rectangle.bottomRightY
    }

    private fun calculateLandmark(settings: SettingsWidgetAlertsAlert): AlertsEffectFaceLandmark {
        val centerX = settings.facePosition.x + settings.facePosition.width / 2
        val centerY = settings.facePosition.y + settings.facePosition.height / 2
        return if (isInRectangle(centerX, centerY, alertsEffectBackgroundLeftEyeRectangle)) {
            AlertsEffectFaceLandmark.leftEye
        } else if (isInRectangle(centerX, centerY, alertsEffectBackgroundRightEyeRectangle)) {
            AlertsEffectFaceLandmark.rightEye
        } else if (isInRectangle(centerX, centerY, alertsEffectBackgroundMouthRectangle)) {
            AlertsEffectFaceLandmark.mouth
        } else {
            AlertsEffectFaceLandmark.face
        }
    }

    private fun calculateLandmarkSettings(
        settings: SettingsWidgetAlertsAlert,
    ): AlertsEffectLandmarkSettings? {
        if (settings.positionType != SettingsWidgetAlertPositionType.face) {
            return null
        }
        val landmark = calculateLandmark(settings = settings)
        val centerX = settings.facePosition.x + settings.facePosition.width / 2
        val centerY = settings.facePosition.y + settings.facePosition.height / 2
        val landmarkRectangle = when (landmark) {
            AlertsEffectFaceLandmark.face -> alertsEffectBackgroundFaceRectangle
            AlertsEffectFaceLandmark.leftEye -> alertsEffectBackgroundLeftEyeRectangle
            AlertsEffectFaceLandmark.rightEye -> alertsEffectBackgroundRightEyeRectangle
            AlertsEffectFaceLandmark.mouth -> alertsEffectBackgroundMouthRectangle
        }
        val x = (centerX - landmarkRectangle.topLeftX) / landmarkRectangle.width()
        val y = (centerY - landmarkRectangle.topLeftY) / landmarkRectangle.height()
        val height = settings.facePosition.height / alertsEffectBackgroundFaceRectangle.height()
        return AlertsEffectLandmarkSettings(
            landmark = landmark,
            height = height,
            centerX = x,
            centerY = y,
        )
    }

    private fun getNext(
        presentationTimeStamp: Double,
    ): Pair<EffectImageCiImage?, EffectImageCiImage?> {
        try {
            val image = pipeline.getImage(presentationTimeStamp)
            return if (image != null) {
                Pair<EffectImageCiImage?, EffectImageCiImage?>(image, pipeline.messageImage)
            } else {
                Pair<EffectImageCiImage?, EffectImageCiImage?>(null, null)
            }
        } finally {
            if (!pipeline.playing) {
                pipeline.landmarkSettings = null
                val delayAfterPlaying = this.delayAfterPlaying
                CoroutineScope(Dispatchers.Main.immediate).launch {
                    delay((delayAfterPlaying * 1000.0).toLong())
                    isPlaying = false
                    tryPlayNextAlert()
                }
            }
        }
    }

    private fun calcFacePlacement(
        detection: VNFaceObservation,
        imageSize: CGSize,
        landmarkSettings: AlertsEffectLandmarkSettings,
    ): FacePlacement? {
        val rotationAngle = detection.calcFaceAngle(imageSize = imageSize) ?: return null
        val boundingBox = detection.stableBoundingBox(
            imageSize = imageSize,
            rotationAngle = rotationAngle,
        ) ?: return null
        val faceMinX = boundingBox.minX
        val faceMaxY = boundingBox.maxY
        val faceWidth = boundingBox.width
        val faceHeight = boundingBox.height
        val alertImageHeight = faceHeight * landmarkSettings.height
        var centerX = 0.0
        var centerY = 0.0
        when (landmarkSettings.landmark) {
            AlertsEffectFaceLandmark.face -> {
                centerX = faceMinX + landmarkSettings.centerX * faceWidth
                centerY = faceMaxY - landmarkSettings.centerY * faceHeight
            }
            AlertsEffectFaceLandmark.leftEye -> {
                val leftEye = detection.landmarks?.leftEye ?: return null
                val points = rotateFace(
                    allPoints = leftEye.pointsInImage(imageSize = imageSize),
                    rotationAngle = -rotationAngle,
                )
                val eyeBoundingBox = calcBoundingBox(points = points) ?: return null
                centerX = eyeBoundingBox.minX + landmarkSettings.centerX * eyeBoundingBox.width
                centerY = eyeBoundingBox.minY - landmarkSettings.centerY * eyeBoundingBox.height
            }
            AlertsEffectFaceLandmark.rightEye -> {
                val rightEye = detection.landmarks?.rightEye ?: return null
                val points = rotateFace(
                    allPoints = rightEye.pointsInImage(imageSize = imageSize),
                    rotationAngle = -rotationAngle,
                )
                val eyeBoundingBox = calcBoundingBox(points = points) ?: return null
                centerX = eyeBoundingBox.minX + landmarkSettings.centerX * eyeBoundingBox.width
                centerY = eyeBoundingBox.minY - landmarkSettings.centerY * eyeBoundingBox.height
            }
            AlertsEffectFaceLandmark.mouth -> {
                val outerLips = detection.landmarks?.outerLips ?: return null
                val points = rotateFace(
                    allPoints = outerLips.pointsInImage(imageSize = imageSize),
                    rotationAngle = -rotationAngle,
                )
                val lipsBoundingBox = calcBoundingBox(points = points) ?: return null
                centerX = lipsBoundingBox.minX + landmarkSettings.centerX * lipsBoundingBox.width
                centerY = lipsBoundingBox.minY - landmarkSettings.centerY * lipsBoundingBox.height
            }
        }
        return FacePlacement(
            center = CGPoint(x = centerX, y = centerY),
            height = alertImageHeight,
            rotation = rotationAngle,
        )
    }

    private fun executePositionFace(
        image: CIImage,
        faceDetections: List<VNFaceObservation>?,
        alertImage: CIImage,
        landmarkSettings: AlertsEffectLandmarkSettings,
    ): CIImage {
        if (faceDetections == null) {
            return image
        }
        var outputImage = image
        for (detection in faceDetections) {
            val placement = calcFacePlacement(
                detection = detection,
                imageSize = image.extent.size,
                landmarkSettings = landmarkSettings,
            ) ?: continue
            val scale = placement.height / alertImage.extent.height
            val moblinImage = alertImage.scaled(x = scale, y = scale)
            val centerPoint = rotatePoint(
                point = CGPoint(
                    x = placement.center.x - moblinImage.extent.midX,
                    y = placement.center.y - moblinImage.extent.midY,
                ),
                alpha = placement.rotation,
            )
            outputImage = moblinImage
                .transformed(by = CGAffineTransform(rotationAngle = placement.rotation))
                .translated(x = centerPoint.x, y = centerPoint.y)
                .composited(over = outputImage)
        }
        return outputImage.cropped(to = image.extent)
    }

    private fun executePositionFaceMetalPetal(
        image: MTIImage,
        faceDetections: List<VNFaceObservation>?,
        alertImage: MTIImage,
        landmarkSettings: AlertsEffectLandmarkSettings,
    ): MTIImage {
        if (faceDetections == null) {
            return image
        }
        val imageSize = image.extent.size
        val layers: List<MTILayer> = faceDetections.mapNotNull { detection ->
            val placement = calcFacePlacement(
                detection = detection,
                imageSize = imageSize,
                landmarkSettings = landmarkSettings,
            ) ?: return@mapNotNull null
            val scale = placement.height / alertImage.extent.height
            val size = CGSize(width = alertImage.extent.width * scale, height = placement.height)
            val centerPoint = rotatePoint(
                point = CGPoint(
                    x = placement.center.x - size.width / 2,
                    y = placement.center.y - size.height / 2,
                ),
                alpha = placement.rotation,
            )
            val rotatedCenter = rotatePoint(
                point = CGPoint(x = size.width / 2, y = size.height / 2),
                alpha = placement.rotation,
            )
            val center = CGPoint(
                x = rotatedCenter.x + centerPoint.x,
                y = rotatedCenter.y + centerPoint.y,
            )
            MTILayer(
                content = alertImage,
                position = CGPoint(x = center.x, y = imageSize.height - center.y),
                size = size,
                rotation = (-placement.rotation).toFloat(),
            )
        }
        if (layers.isEmpty()) {
            return image
        }
        val filter = MTIMultilayerCompositingFilter()
        filter.inputBackgroundImage = image
        filter.layers = layers
        return filter.outputImage ?: image
    }

    private fun alertAndMessageSize(alertSize: CGSize, messageSize: CGSize): CGSize {
        return CGSize(width = alertSize.width, height = alertSize.height + messageSize.height)
    }

    private fun executePositionScene(
        image: CIImage,
        alertImage: CIImage,
        messageImage: CIImage,
        layout: SettingsWidgetLayout,
    ): CIImage {
        val alertSize = alertImage.extent.size
        val messageSize = messageImage.extent.size
        val size = alertAndMessageSize(alertSize = alertSize, messageSize = messageSize)
        val position = layoutPosition(layout = layout, size = size, streamSize = image.extent.size)
        val xPos = position.x
        val yPos = image.extent.height - position.y - alertSize.height
        return messageImage
            .translated(x = -(messageSize.width - alertSize.width) / 2, y = -messageSize.height)
            .composited(over = alertImage)
            .translated(x = xPos, y = yPos)
            .composited(over = image)
            .cropped(to = image.extent)
    }

    private fun executePositionSceneMetalPetal(
        image: MTIImage,
        alertImage: MTIImage,
        messageImage: MTIImage,
        layout: SettingsWidgetLayout,
    ): MTIImage {
        val alertSize = alertImage.extent.size
        val messageSize = messageImage.extent.size
        val size = alertAndMessageSize(alertSize = alertSize, messageSize = messageSize)
        val position = layoutPosition(layout = layout, size = size, streamSize = image.extent.size)
        val xPos = position.x + alertSize.width / 2
        val filter = MTIMultilayerCompositingFilter()
        filter.inputBackgroundImage = image
        filter.layers = listOf(
            MTILayer(
                content = alertImage,
                position = CGPoint(x = xPos, y = position.y + alertSize.height / 2),
            ),
            MTILayer(
                content = messageImage,
                position = CGPoint(
                    x = xPos,
                    y = position.y + alertSize.height + messageSize.height / 2,
                ),
            ),
        )
        return filter.outputImage ?: image
    }

    private fun setTwitchSettings(twitch: SettingsWidgetAlertsTwitch) {
        twitchFollowMedia.update(twitch.follows, mediaStorage, bundledImages, bundledSounds)
        twitchSubscribeMedia.update(
            twitch.subscriptions,
            mediaStorage,
            bundledImages,
            bundledSounds,
        )
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
            settings = settings.twitch.follows,
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
            settings = settings.twitch.subscriptions,
        )
    }

    private fun playTwitchSubscriptionGift(
        event: TwitchEventSubNotificationChannelSubscriptionGiftEvent,
    ) {
        if (!settings.twitch.subscriptions.enabled) {
            return
        }
        play(
            media = twitchSubscribeMedia,
            username = event.user_name ?: "Anomymous",
            message = localized(
                "just gifted ${event.total} tier ${event.tierAsNumber()} subscriptions!",
            ),
            settings = settings.twitch.subscriptions,
        )
    }

    private fun playTwitchResubscribe(
        event: TwitchEventSubNotificationChannelSubscriptionMessageEvent,
    ) {
        if (!settings.twitch.subscriptions.enabled) {
            return
        }
        val streakMonths = event.streak_months
        val text = if (streakMonths != null) {
            localized(
                "just resubscribed tier ${event.tierAsNumber()} for " +
                    "${event.cumulative_months} months, $streakMonths in a row!",
            )
        } else {
            localized(
                "just resubscribed tier ${event.tierAsNumber()} for " +
                    "${event.cumulative_months} months!",
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
                bits = null,
            ),
            settings = settings.twitch.subscriptions,
        )
    }

    private fun playTwitchSubscriptionUpgrade(
        event: TwitchEventSubNotificationChannelSubscriptionUpgradeEvent,
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
            settings = settings.twitch.subscriptions,
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
            settings = settings.twitch.raids,
        )
    }

    private fun playTwitchRedemption(
        event: TwitchEventSubNotificationChannelPointsCustomRewardRedemptionAddEvent,
    ) {
        for ((index, redemption) in settings.twitch.redemptions.withIndex()) {
            if (!redemption.enabled) {
                continue
            }
            play(
                media = twitchRedemptionMedias[index],
                username = event.user_name,
                message = localized("redeemed ${event.reward.title}!"),
                settings = redemption,
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
                SettingsWidgetAlertsCheerBitsAlertOperator.equal ->
                    if (event.bits != cheerBit.bits) {
                        continue
                    }
                SettingsWidgetAlertsCheerBitsAlertOperator.greaterEqual ->
                    if (event.bits < cheerBit.bits) {
                        continue
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
                    bits = "",
                ),
                settings = cheerBit.alert,
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
            bundledSounds,
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
                "just subscribed! They've been subscribed for ${event.months} months!",
            ),
            settings = settings.kick.subscriptions,
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
                "just gifted ${event.gifted_usernames.size} subscription(s)! They've " +
                    "gifted ${event.gifter_total} in total!",
            ),
            settings = settings.kick.giftedSubscriptions,
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
            settings = settings.kick.hosts,
        )
    }

    private fun playKickReward(event: KickPusherRewardRedeemedEvent) {
        if (!settings.kick.rewards.enabled) {
            return
        }
        val baseMessage = localized("redeemed ${event.reward_title}")
        val message = if (event.user_input.isEmpty()) {
            baseMessage
        } else {
            "$baseMessage: ${event.user_input}"
        }
        play(
            media = kickRewardMedia,
            username = event.username,
            message = message,
            segments = delegate?.alertsMakeKickSegments(text = message),
            settings = settings.kick.rewards,
        )
    }

    private fun playKickKicks(event: KickPusherKicksGiftedEvent) {
        for ((index, kickGift) in settings.kick.kickGifts.withIndex()) {
            val matches: Boolean = when (kickGift.comparisonOperator) {
                SettingsWidgetAlertsCheerBitsAlertOperator.equal ->
                    event.gift.amount == kickGift.amount
                SettingsWidgetAlertsCheerBitsAlertOperator.greaterEqual ->
                    event.gift.amount >= kickGift.amount
                else -> false
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
                settings = kickGift.alert,
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
            delayAfterPlaying = 0.0,
        )
    }
}

private fun RgbColor.color(): Color = Color(
    red = red.toInt(),
    green = green.toInt(),
    blue = blue.toInt(),
)
