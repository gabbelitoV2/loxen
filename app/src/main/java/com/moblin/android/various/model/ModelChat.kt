package com.moblin.android.various.model

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.AppDelegate
import com.moblin.android.common.various.RgbColor
import com.moblin.android.localized
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

const val maximumNumberOfChatMessages = 50
const val maximumNumberOfInteractiveChatMessages = 100

private val mainScope = CoroutineScope(Dispatchers.Main)

fun Model.pauseChat(chat: ChatProvider) {
    chat.pause(redLine = createRedLineChatPost())
}

fun Model.endOfChatReachedWhenPaused(chat: ChatProvider) {
    chat.endReachedWhenPaused()
}

fun Model.disableInteractiveChat() {
    chat.endReachedWhenPaused()
    chatActivityFeed.endReachedWhenPaused()
}

fun Model.pauseQuickButtonChat() {
    quickButtonChat.pause(redLine = createRedLineChatPost())
}

fun Model.endOfQuickButtonChatReachedWhenPaused() {
    quickButtonChat.endReachedWhenPaused()
}

fun Model.pauseQuickButtonChatAlerts() {
    quickButtonChatState.chatAlertsPaused.value = true
    quickButtonChatState.pausedChatAlertsPostsCount.value = 0
    pausedQuickButtonChatAlertsPosts = ArrayDeque(listOf(createRedLineChatPost()))
    while (true) {
        val post = newQuickButtonChatAlertsPosts.removeFirstOrNull() ?: break
        pausedQuickButtonChatAlertsPosts.addLast(post)
    }
}

fun Model.endOfQuickButtonChatAlertsReachedWhenPaused() {
    while (true) {
        val post = pausedQuickButtonChatAlertsPosts.removeFirstOrNull() ?: break
        if (post.isRedLine()) {
            if (quickButtonChatState.chatAlertsPosts.value.firstOrNull()?.isRedLine() == true) {
                continue
            }
            if (pausedQuickButtonChatAlertsPosts.isEmpty()) {
                continue
            }
        }
        if (quickButtonChatState.chatAlertsPosts.value.size > maximumNumberOfInteractiveChatMessages - 1) {
            quickButtonChatState.chatAlertsPosts.value.removeLast()
        }
        quickButtonChatState.chatAlertsPosts.value.addFirst(post)
    }
    quickButtonChatState.chatAlertsPaused.value = false
}

fun Model.removeOldChatMessages(now: Instant) {
    if (!database.chat.maximumAgeEnabled) {
        return
    }
    removeOldChatMessages(now, chat)
}

private fun Model.removeOldChatMessages(now: Instant, chat: ChatProvider) {
    if (chat.paused.value) {
        return
    }
    while (true) {
        val post = chat.posts.value.lastOrNull() ?: break
        if (Duration.between(post.timestampTime, now).seconds > database.chat.maximumAge) {
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
    if (externalDisplay.chatEnabled.value) {
        externalDisplayChat.update()
    }
    if (quickButtonChatState.chatAlertsPaused.value) {
        quickButtonChatState.pausedChatAlertsPostsCount.value = maxOf(pausedQuickButtonChatAlertsPosts.size - 1, 0)
    } else {
        while (true) {
            val post = newQuickButtonChatAlertsPosts.removeFirstOrNull() ?: break
            if (quickButtonChatState.chatAlertsPosts.value.size > maximumNumberOfInteractiveChatMessages - 1) {
                quickButtonChatState.chatAlertsPosts.value.removeLast()
            }
            quickButtonChatState.chatAlertsPosts.value.addFirst(post)
        }
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
    externalDisplayChat.moreThanOneStreamingPlatform.value = moreThanOneStreamingPlatform
    chatWidgetChat.moreThanOneStreamingPlatform.value = moreThanOneStreamingPlatform
}

private fun Model.createRedLineChatPost(): ChatPost {
    val post = ChatPost(
        id = chatPostId,
        messageId = null,
        displayName = null,
        user = null,
        userId = null,
        userColor = RgbColor(red = 0, green = 0, blue = 0),
        userBadges = emptyList(),
        segments = emptyList(),
        timestamp = "",
        timestampTime = Instant.now(),
        isAction = false,
        isSubscriber = false,
        bits = null,
        highlight = null,
        live = true,
        filter = null,
        platform = null,
        sourceChannelIcon = null,
        state = ChatPostState()
    )
    chatPostId += 1
    return post
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
        Platform.twitch, null -> Unit
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
        sendTwitchChatMessage(message = message) { result ->
            when (result) {
                is NetworkResponse.AuthError -> {
                    twitchLogin(stream = stream.value) {
                        showTwitchAuth.value = true
                    }
                }
                else -> {}
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
    return database.chat.filters.firstOrNull { it.isMatching(user, segments) }
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
    sourceChannelIcon: String? = null
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
                    segments = segments
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
        highlight = highlight ?: (if (isModerator) ChatHighlight.makeModerator() else null),
        live = live,
        filter = filter,
        platform = platform,
        sourceChannelIcon = sourceChannelIcon,
        state = ChatPostState()
    )
    chatPostId += 1
    if (isTextToSpeechEnabledForMessage(post = post)) {
        val message = post.text()
        if (message.trim().isNotEmpty()) {
            chatTextToSpeech.say(
                messageId = post.messageId,
                user = post.shortDisplayName(nicknames = database.chat.nicknames),
                userId = post.userId,
                message = message,
                isRedemption = post.isRedemption()
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
        if (isAlert) {
            if (quickButtonChatState.chatAlertsPaused.value) {
                if (pausedQuickButtonChatAlertsPosts.size < 2 * maximumNumberOfInteractiveChatMessages) {
                    pausedQuickButtonChatAlertsPosts.addLast(post)
                }
            } else {
                newQuickButtonChatAlertsPosts.addLast(post)
            }
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
    chat.posts.value = newPostIds(posts = chat.posts.value.toMutableList())
    chatActivityFeed.posts.value = newPostIds(posts = chatActivityFeed.posts.value.toMutableList())
    quickButtonChat.posts.value = newPostIds(posts = quickButtonChat.posts.value.toMutableList())
    externalDisplayChat.posts.value = newPostIds(posts = externalDisplayChat.posts.value.toMutableList())
    chatWidgetChat.posts.value = newPostIds(posts = chatWidgetChat.posts.value.toMutableList())
    quickButtonChatState.chatAlertsPosts.value =
        newPostIds(posts = quickButtonChatState.chatAlertsPosts.value.toMutableList())
}

private fun Model.newPostIds(posts: MutableList<ChatPost>): ArrayDeque<ChatPost> {
    val newPosts = ArrayDeque<ChatPost>()
    for (post in posts) {
        newPosts.addLast(post.copy(id = chatPostId))
        chatPostId += 1
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
            statuses.add(ChatPlatformStatus(platform = Platform.youTube, connected = isYouTubeLiveChatConnected()))
        }
        if (isSoopChatConfigured()) {
            statuses.add(ChatPlatformStatus(platform = Platform.soop, connected = isSoopChatConnected()))
        }
        if (isOpenStreamingPlatformChatConfigured()) {
            statuses.add(
                ChatPlatformStatus(
                    platform = Platform.openStreamingPlatform,
                    connected = isOpenStreamingPlatformChatConnected()
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
        delay(2_000)
        val image = return@launch
        for (catPrinter in catPrinters.values) {
            if (getCatPrinterSettings(catPrinter = catPrinter)?.printChat?.value == true) {
                catPrinter.print(image = image, feedPaperDelay = 3.0)
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
    val clipboard = AppDelegate.context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(null, post.text()))
}

fun Model.deleteChatMessage(messageId: String) {
    chat.deleteMessage(messageId = messageId)
    chatActivityFeed.deleteMessage(messageId = messageId)
    quickButtonChat.deleteMessage(messageId = messageId)
    externalDisplayChat.deleteMessage(messageId = messageId)
    chatWidgetChat.deleteMessage(messageId = messageId)
    chatTextToSpeech.delete(messageId = messageId)
}

fun Model.deleteChatUser(userId: String) {
    chat.deleteUser(userId = userId)
    chatActivityFeed.deleteUser(userId = userId)
    quickButtonChat.deleteUser(userId = userId)
    externalDisplayChat.deleteUser(userId = userId)
    chatWidgetChat.deleteUser(userId = userId)
    chatTextToSpeech.deleteByUserId(userId = userId)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatPrinterMessage(post: ChatPost, chat: SettingsChat) {
    Row(modifier = Modifier.width(384.dp)) {
        FlowRow(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = post.displayName(nicknames = chat.nicknames, displayStyle = chat.displayStyle),
                maxLines = 1,
                color = Color.Black,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (post.isRedemption()) " " else ": ",
                color = Color.Black,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            for (segment in post.segments) {
                val text = segment.text
                if (text != null) {
                    Text(
                        text = text,
                        color = Color.Black,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                val url = (segment.url ?: segment.bigGifUrl)?.url(animated = false)
                if (url != null) {
                    CacheAsyncImage(
                        url = URI(url),
                        content = { image ->
                            Image(
                                bitmap = image,
                                contentDescription = null,
                                modifier = Modifier.height(45.dp)
                            )
                        },
                        placeholder = { }
                    )
                    Text(
                        text = " ",
                        color = Color.Black,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
