package com.moblin.android.integrations.emotes

import android.util.Log
import com.moblin.android.common.various.sleep
import com.moblin.android.localized
import com.moblin.android.various.ChatPostSegment
import com.moblin.android.various.ChatPostUrl
import com.moblin.android.various.settings.SettingsStreamChat
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "Emotes"

enum class EmotesPlatform {
    twitch,
    kick,
    youtube,
}

class Emote(val url: String, val stillUrl: String? = null)

class Emotes {
    private val emotes: MutableMap<String, Emote> = mutableMapOf()
    private var task: Job? = null
    private var ready: Boolean = false
    private val scope = CoroutineScope(Dispatchers.Main)

    fun isReady(): Boolean {
        return ready
    }

    fun start(
        platform: EmotesPlatform,
        channelId: String,
        onError: (String, String) -> Unit,
        onOk: (String) -> Unit,
        settings: SettingsStreamChat,
    ) {
        val chatSettings = settings.clone()
        ready = false
        emotes.clear()
        task = scope.launch {
            var firstRetry = true
            var retryTime = 30
            while (!ready) {
                val (bttvEmotes, bttvError) = fetchBttvEmotes(
                    platform = platform,
                    channelId = channelId,
                    enabled = chatSettings.bttvEmotes,
                )
                addEmotes(bttvEmotes)
                val (ffzEmotes, ffzError) = fetchFfzEmotes(
                    platform = platform,
                    channelId = channelId,
                    enabled = chatSettings.ffzEmotes,
                )
                addEmotes(ffzEmotes)
                val (seventvEmotes, seventvError) = fetchSeventvEmotes(
                    platform = platform,
                    channelId = channelId,
                    enabled = chatSettings.seventvEmotes,
                )
                addEmotes(seventvEmotes)
                if (!isActive) {
                    return@launch
                }
                val error = bttvError ?: ffzError ?: seventvError
                if (error != null) {
                    Log.i(TAG, "emotes: $error")
                    if (firstRetry) {
                        onError(error, localized("Retrying later"))
                    }
                    firstRetry = false
                    ready = false
                    try {
                        sleep(seconds = retryTime.toDouble())
                        retryTime *= 2
                        retryTime = minOf(retryTime, 3600)
                    } catch (e: Exception) {
                        return@launch
                    }
                } else {
                    ready = true
                    if (!firstRetry) {
                        onOk("Emotes fetched")
                    }
                }
            }
            Log.d(TAG, "emotes: Emotes lists fetched")
        }
    }

    fun addEmotes(emotes: Map<String, Emote>) {
        this.emotes.putAll(emotes)
    }

    fun stop() {
        ready = false
        task?.cancel()
        task = null
    }

    fun createSegments(text: String, id: AtomicInteger): List<ChatPostSegment> {
        val segments: MutableList<ChatPostSegment> = mutableListOf()
        for (word in text.split(Regex("\\s+")).filter { it.isNotEmpty() }) {
            val emote = emotes[word]
            if (emote == null) {
                segments.add(ChatPostSegment(id = id.get(), text = "$word "))
                id.incrementAndGet()
                continue
            }
            segments.add(
                ChatPostSegment(
                    id = id.get(),
                    text = "",
                    url = ChatPostUrl(moving = emote.url, still = emote.stillUrl ?: emote.url),
                ),
            )
            id.incrementAndGet()
            segments.add(ChatPostSegment(id = id.get(), text = ""))
            id.incrementAndGet()
        }
        return segments
    }
}
