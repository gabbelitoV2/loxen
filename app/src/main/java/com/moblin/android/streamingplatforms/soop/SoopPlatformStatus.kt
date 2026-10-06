package com.moblin.android.streamingplatforms.soop

import kotlinx.coroutines.delay
import com.moblin.android.various.model.PlatformStatus
import com.moblin.android.various.network.getHttpsUrl
import com.moblin.android.various.network.httpGet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class SoopChannelInfo(
    val currentSumViewer: Int,
)

private const val baseUrl = "https://api-channel.sooplive.com"

class SoopPlatformStatus {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var task: Job? = null
    var platformStatus: PlatformStatus = PlatformStatus.unknown

    fun start(userId: String) {
        platformStatus = PlatformStatus.unknown
        val url = "$baseUrl/v1.1/channel/$userId/home/section/broad"
        task = scope.launch {
            var delaySeconds = 5
            while (true) {
                try {
                    delay(delaySeconds * 1000L)
                    val channelInfo = getChannelInfo(url)
                    platformStatus = PlatformStatus.live(viewerCount = channelInfo.currentSumViewer)
                } catch (e: Exception) {
                    platformStatus = PlatformStatus.unknown
                }
                if (!isActive) {
                    platformStatus = PlatformStatus.unknown
                    break
                }
                delaySeconds = 60
            }
        }
    }

    fun stop() {
        task?.cancel()
        task = null
    }

    private suspend fun getChannelInfo(url: String): SoopChannelInfo {
        val uri = getHttpsUrl(url) ?: throw IllegalStateException("Invalid url")
        val (data, _) = httpGet(from = uri)
        return Json.decodeFromString(data.decodeToString())
    }
}
