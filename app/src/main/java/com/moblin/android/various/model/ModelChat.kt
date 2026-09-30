package com.moblin.android.various.model

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moblin.android.AppDelegate
import com.moblin.android.common.various.RgbColor
import com.moblin.android.common.various.trim
import com.moblin.android.localized
import com.moblin.android.platform.Bundle
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.swiftui.ImageRenderer
import com.moblin.android.platform.swiftui.SwiftUIFonts
import com.moblin.android.streamingplatforms.Platform
import com.moblin.android.various.CacheAsyncImage
import com.moblin.android.various.ChatBotMessage
import com.moblin.android.various.ChatHighlight
import com.moblin.android.various.ChatHighlightKind
import com.moblin.android.various.ChatPost
import com.moblin.android.various.ChatPostSegment
import com.moblin.android.various.ChatPostState
import com.moblin.android.various.model.chat.ChatProvider
import com.moblin.android.various.network.NetworkResponse
import com.moblin.android.various.settings.SettingsChat
import com.moblin.android.various.settings.SettingsChatFilter
import java.net.URI
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val maximumNumberOfChatMessages = 50

private val mainScope = CoroutineScope(Dispatchers.Main)

fun Model.pauseChat(chat: ChatProvider) {
    chat.pause()
}

fun Model.endOfChatReachedWhenPaused(chat: ChatProvider) {
    chat.endReachedWhenPaused()
}

fun Model.disableInteractiveChat() {
    chat.endReachedWhenPaused()
    chatActivityFeed.endReachedWhenPaused()
}

fun Model.pauseQuickButtonChat() {
    quickButtonChat.pause()
}

fun Model.endOfQuickButtonChatReachedWhenPaused() {
    quickButtonChat.endReachedWhenPaused()
}

fun Model.pauseQuickButtonChatAlerts() {
    quickButtonChatAlerts.pause()
}

fun Model.endOfQuickButtonChatAlertsReachedWhenPaused() {
    quickButtonChatAlerts.endReachedWhenPaused()
}

fun Model.removeOldChatMessages(now: Instant) {
    if (!database.chat.maximumAgeEnabled) {
        return
    }
    removeOldChatMessages(now = now, chat = chat)
}

private fun Model.removeOldChatMessages(now: Instant, chat: ChatProvider) {
    if (chat.paused.value) {
        return
    }
    while (true) {
        val post = chat.posts.value.lastOrNull() ?: break
        if (Duration.between(post.timestampTime, now).seconds > database.chat.maximumAge.toDouble()) {
            chat.posts.value = chat.posts.value.dropLast(1)
        } else {
            break
        }
    }
}

fun Model.updateChat() {
    chat.update()
    chatActivityFeed.update()
    quickButtonChat.update()
    quickButtonChatAlerts.update()
    if (externalDisplay.chatEnabled.value) {
        externalDisplayChat.update()
    }
    chatWidgetChat.update()
}

fun Model.isAlertMessage(post: ChatPost): Boolean {
    return when (post.highlight?.kind) {
        ChatHighlightKind.Redemption -> true
        ChatHighlightKind.NewFollower -> true
        else -> false
    }
}

fun Model.reloadChats() {
    reloadTwitchChat()
    reloadKickPusher()
    reloadYouTubeLiveChat()
    reloadSoopChat()
    reloadOpenStreamingPlatformChat()
}

fun Model.updateChatMoreThanOneChatConfigured() {
    val moreThanOneStreamingPlatform = isMoreThanOneChatConfigured()
    chat.moreThanOneStreamingPlatform.value = moreThanOneStreamingPlatform
    chatActivityFeed.moreThanOneStreamingPlatform.value = moreThanOneStreamingPlatform
    quickButtonChat.moreThanOneStreamingPlatform.value = moreThanOneStreamingPlatform
    quickButtonChatAlerts.moreThanOneStreamingPlatform.value = moreThanOneStreamingPlatform
    externalDisplayChat.moreThanOneStreamingPlatform.value = moreThanOneStreamingPlatform
    chatWidgetChat.moreThanOneStreamingPlatform.value = moreThanOneStreamingPlatform
}

private fun Model.isMoreThanOneChatConfigured(): Boolean {
    var numberOfChats = 0
    if (isTwitchChatConfigured()) {
        numberOfChats += 1
    }
    if (isKickPusherConfigured()) {
        numberOfChats += 1
    }
    if (isYouTubeLiveChatConfigured()) {
        numberOfChats += 1
    }
    if (isSoopChatConfigured()) {
        numberOfChats += 1
    }
    if (isOpenStreamingPlatformChatConfigured()) {
        numberOfChats += 1
    }
    return numberOfChats > 1
}

fun Model.isChatConfigured(): Boolean {
    return isTwitchChatConfigured() || isKickPusherConfigured() ||
        isYouTubeLiveChatConfigured() || isSoopChatConfigured() ||
        isOpenStreamingPlatformChatConfigured()
}

fun Model.isRemoteControlChatAndEvents(platform: Platform?): Boolean {
    when (platform) {
        Platform.twitch, null -> {}
        else -> return false
    }
    return useRemoteControlForChatAndEvents
}

fun Model.isChatConnected(): Boolean {
    if (isTwitchChatConfigured() && !isTwitchChatConnected()) {
        return false
    }
    if (isKickPusherConfigured() && !isKickPusherConnected()) {
        return false
    }
    if (isYouTubeLiveChatConfigured() && !isYouTubeLiveChatConnected()) {
        return false
    }
    if (isSoopChatConfigured() && !isSoopChatConnected()) {
        return false
    }
    if (isOpenStreamingPlatformChatConfigured() && !isOpenStreamingPlatformChatConnected()) {
        return false
    }
    return true
}

fun Model.hasChatEmotes(): Boolean {
    return hasTwitchChatEmotes() ||
        hasKickPusherEmotes() ||
        hasYouTubeLiveChatEmotes() ||
        hasSoopChatEmotes() ||
        hasOpenStreamingPlatformChatEmotes()
}

fun Model.resetChat() {
    chatTextToSpeech.reset(running = true)
}

fun Model.sendChatMessage(message: String) {
    if (stream.value.twitchSendMessagesTo && stream.value.twitchLoggedIn) {
        sendTwitchChatMessage(message = message) { }
    }
    if (stream.value.kickSendMessagesTo && stream.value.kickLoggedIn) {
        sendKickChatMessage(message = message)
    }
}

fun Model.sendChatMessageShowLogin(message: String) {
    if (stream.value.twitchSendMessagesTo) {
        sendTwitchChatMessage(message = message) { response ->
            if (response is NetworkResponse.AuthError) {
                twitchLogin(stream = stream.value) {
                    showTwitchAuth.value = true
                }
            }
        }
    }
    if (stream.value.kickSendMessagesTo) {
        if (stream.value.kickLoggedIn) {
            sendKickChatMessage(message = message)
        } else {
            makeNotLoggedInToToast(platform = Platform.kick)
        }
    }
}

private fun Model.evaluateFilters(user: String?, segments: List<ChatPostSegment>): SettingsChatFilter? {
    return database.chat.filters.firstOrNull { it.isMatching(user = user, segments = segments) }
}

fun Model.appendChatMessage(
    platform: Platform?,
    messageId: String?,
    displayName: String?,
    user: String?,
    userId: String?,
    userColor: RgbColor?,
    userBadges: List<String>,
    segments: List<ChatPostSegment>,
    timestamp: String,
    timestampTime: Instant,
    isAction: Boolean,
    isSubscriber: Boolean,
    isModerator: Boolean,
    isOwner: Boolean,
    bits: String?,
    highlight: ChatHighlight?,
    live: Boolean,
    sourceChannelIcon: String? = null,
) {
    val filter = evaluateFilters(user = user, segments = segments)
    if (platform != null && database.chat.botEnabled && live && filter?.chatBot != false &&
        segments.firstOrNull()?.text?.trim()?.startsWith("!") == true
    ) {
        if (chatBotMessages.size < 25 || isModerator) {
            chatBotMessages.add(
                ChatBotMessage(
                    platform = platform,
                    user = user,
                    isOwner = isOwner,
                    isModerator = isModerator,
                    isSubscriber = isSubscriber,
                    userId = userId,
                    segments = segments,
                )
            )
        }
    }
    if (pollEnabled && live && filter?.poll != false) {
        handlePollVote(vote = segments.firstOrNull()?.text?.trim())
    }
    val post = ChatPost(
        id = chatPostId,
        messageId = messageId,
        displayName = displayName,
        user = user,
        userId = userId,
        userColor = makeUserColor(userColor = userColor),
        userBadges = userBadges,
        segments = segments,
        timestamp = timestamp,
        timestampTime = timestampTime,
        isAction = isAction,
        isSubscriber = isSubscriber,
        bits = bits,
        highlight = highlight ?: if (isModerator) ChatHighlight.makeModerator() else null,
        live = live,
        filter = filter,
        platform = platform,
        sourceChannelIcon = sourceChannelIcon,
        state = ChatPostState(),
    )
    chatPostId += 1
    if (isTextToSpeechEnabledForMessage(post = post)) {
        val message = post.text()
        if (!message.trim().isEmpty()) {
            chatTextToSpeech.say(
                messageId = post.messageId,
                user = post.shortDisplayName(nicknames = database.chat.nicknames),
                userId = post.userId,
                message = message,
                isRedemption = post.isRedemption(),
            )
        }
    }
    if (filter?.print != false && isAnyConnectedCatPrinterPrintingChat()) {
        printChatMessage(post = post)
    }
    if (filter?.showOnScreen != false) {
        val isAlert = highlight?.isAlert() == true
        if (isAlert) {
            chatActivityFeed.appendMessage(post = post)
            quickButtonChatAlerts.appendMessage(post = post)
        }
        chat.appendMessage(post = post)
        quickButtonChat.appendMessage(post = post)
        for (browserEffect in browserEffects.values) {
            browserEffect.sendChatMessage(post = post)
        }
        if (isWatchLocal()) {
            sendChatMessageToWatch(post = post)
        }
        if (externalDisplay.chatEnabled.value) {
            externalDisplayChat.appendMessage(post = post)
        }
        if (enabledChatEffects.isNotEmpty()) {
            chatWidgetChat.appendMessage(post = post)
        }
        for (effect in enabledChatEmoteComboEffects) {
            effect.appendMessage(post = post)
        }
    }
}

private fun Model.makeUserColor(userColor: RgbColor?): RgbColor {
    if (database.chat.sameUsernameColor) {
        return database.chat.usernameColor
    }
    return userColor?.makeReadableOnDarkBackground() ?: database.chat.usernameColor
}

fun Model.reloadChatMessages() {
    chat.posts.value = newPostIds(posts = chat.posts.value)
    chatActivityFeed.posts.value = newPostIds(posts = chatActivityFeed.posts.value)
    quickButtonChat.posts.value = newPostIds(posts = quickButtonChat.posts.value)
    quickButtonChatAlerts.posts.value = newPostIds(posts = quickButtonChatAlerts.posts.value)
    externalDisplayChat.posts.value = newPostIds(posts = externalDisplayChat.posts.value)
    chatWidgetChat.posts.value = newPostIds(posts = chatWidgetChat.posts.value)
}

private fun Model.newPostIds(posts: List<ChatPost>): ArrayDeque<ChatPost> {
    val newPosts = ArrayDeque<ChatPost>()
    for (post in posts) {
        val newPost = post.copy(id = chatPostId)
        chatPostId += 1
        newPosts.addLast(newPost)
    }
    return newPosts
}

fun Model.isShowingStatusChat(): Boolean {
    return database.show.chat && isChatConfigured()
}

fun Model.updateStatusChatText() {
    val status: String
    val statuses = mutableListOf<ChatPlatformStatus>()
    if (!isChatConfigured()) {
        status = localized("Not configured")
    } else if (isRemoteControlChatAndEvents(platform = null)) {
        if (isRemoteControlStreamerConnected()) {
            status = localized("Connected (remote control)")
        } else {
            status = localized("Disconnected (remote control)")
        }
    } else {
        if (isTwitchChatConfigured()) {
            statuses.add(ChatPlatformStatus(platform = Platform.twitch, connected = isTwitchChatConnected()))
        }
        if (isKickPusherConfigured()) {
            statuses.add(ChatPlatformStatus(platform = Platform.kick, connected = isKickPusherConnected()))
        }
        if (isYouTubeLiveChatConfigured()) {
            statuses.add(
                ChatPlatformStatus(
                    platform = Platform.youTube,
                    connected = isYouTubeLiveChatConnected(),
                )
            )
        }
        if (isSoopChatConfigured()) {
            statuses.add(ChatPlatformStatus(platform = Platform.soop, connected = isSoopChatConnected()))
        }
        if (isOpenStreamingPlatformChatConfigured()) {
            statuses.add(
                ChatPlatformStatus(
                    platform = Platform.openStreamingPlatform,
                    connected = isOpenStreamingPlatformChatConnected(),
                )
            )
        }
        if (statuses.all { it.connected }) {
            status = localized("Connected")
        } else {
            status = localized("Disconnected")
        }
    }
    if (status != statusTopLeft.statusChatText.value) {
        statusTopLeft.statusChatText.value = status
    }
    if (statuses != statusTopLeft.chatPlatformStatuses.value) {
        statusTopLeft.chatPlatformStatuses.value = statuses
    }
}

fun Model.showChatLabelsForAWhile() {
    chat.showLabelForAWhile()
    chatActivityFeed.showLabelForAWhile()
}

fun Model.printChatMessage(post: ChatPost) {
    mainScope.launch {
        delay(2000)
        val post0 = post
        val chat0 = database.chat
        val renderer = ImageRenderer(content = { ChatPrinterMessage(post = post0, chat = chat0) })
        val image = renderer.uiImage ?: return@launch
        val ciImage = CIImage(cgImage = image)
        for (catPrinter in catPrinters.values) {
            if (getCatPrinterSettings(catPrinter = catPrinter)?.printChat?.value == true) {
                catPrinter.print(image = ciImage, feedPaperDelay = 3.0)
            }
        }
    }
}

fun Model.banUser(post: ChatPost) {
    val user = post.user ?: return
    when (post.platform) {
        Platform.twitch -> {
            val userId = post.userId ?: return
            banTwitchUser(user = user, userId = userId, duration = null) { }
        }
        Platform.kick -> banKickUser(user = user, duration = null) { }
        Platform.youTube -> {
            val userId = post.userId ?: return
            banYouTubeUser(user = user, channelId = userId, duration = null)
        }
        else -> makeErrorToast(title = "Ban not supported for this platform")
    }
}

fun Model.timeoutUser(post: ChatPost, duration: Int) {
    val user = post.user ?: return
    when (post.platform) {
        Platform.twitch -> {
            val userId = post.userId ?: return
            banTwitchUser(user = user, userId = userId, duration = duration) { }
        }
        Platform.kick -> banKickUser(user = user, duration = duration) { }
        Platform.youTube -> {
            val userId = post.userId ?: return
            banYouTubeUser(user = user, channelId = userId, duration = duration)
        }
        else -> makeErrorToast(title = "Timeout not supported for this platform")
    }
}

fun Model.deleteMessage(post: ChatPost) {
    val messageId = post.messageId ?: return
    when (post.platform) {
        Platform.twitch -> deleteTwitchChatMessage(messageId = messageId)
        Platform.kick -> deleteKickMessage(messageId = messageId)
        else -> makeErrorToast(title = "Delete message not supported for this platform")
    }
}

fun Model.copyMessage(post: ChatPost) {
    AppDelegate.context.getSystemService(ClipboardManager::class.java)
        ?.setPrimaryClip(ClipData.newPlainText(null, post.text()))
}

fun Model.deleteChatMessage(messageId: String) {
    chat.deleteMessage(messageId = messageId)
    chatActivityFeed.deleteMessage(messageId = messageId)
    quickButtonChat.deleteMessage(messageId = messageId)
    quickButtonChatAlerts.deleteMessage(messageId = messageId)
    externalDisplayChat.deleteMessage(messageId = messageId)
    chatWidgetChat.deleteMessage(messageId = messageId)
    chatTextToSpeech.delete(messageId = messageId)
}

fun Model.deleteChatUser(userId: String) {
    chat.deleteUser(userId = userId)
    chatActivityFeed.deleteUser(userId = userId)
    quickButtonChat.deleteUser(userId = userId)
    quickButtonChatAlerts.deleteUser(userId = userId)
    externalDisplayChat.deleteUser(userId = userId)
    chatWidgetChat.deleteUser(userId = userId)
    chatTextToSpeech.deleteByUserId(userId = userId)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatPrinterMessage(post: ChatPost, chat: SettingsChat) {
    Row(modifier = Modifier.width(384.dp)) {
        CompositionLocalProvider(
            LocalTextStyle provides SwiftUIFonts.system(30, FontWeight.Bold),
            LocalContentColor provides Color.Black,
        ) {
            FlowRow(
                modifier = Modifier.weight(1f, fill = false),
                horizontalArrangement = Arrangement.Start,
                verticalArrangement = Arrangement.Top,
            ) {
                Text(
                    text = post.displayName(nicknames = chat.nicknames, displayStyle = chat.displayStyle),
                    maxLines = 1,
                )
                if (post.isRedemption()) {
                    Text(text = " ")
                } else {
                    Text(text = ": ")
                }
                post.segments.forEach { segment ->
                    key(segment.id) {
                        segment.text?.let { text ->
                            Text(text = text)
                        }
                        (segment.url ?: segment.bigGifUrl)?.url(animated = false)?.let { url ->
                            CacheAsyncImage(
                                url = URI(url),
                                content = { image ->
                                    Image(
                                        bitmap = image,
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.height(45.dp),
                                    )
                                },
                                placeholder = {
                                    val icon = Bundle.image("AppIconNoBackground")
                                    if (icon != null) {
                                        Image(
                                            bitmap = icon.asImageBitmap(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.height(45.dp),
                                        )
                                    }
                                },
                            )
                            Text(text = " ")
                        }
                    }
                }
            }
        }
    }
}
