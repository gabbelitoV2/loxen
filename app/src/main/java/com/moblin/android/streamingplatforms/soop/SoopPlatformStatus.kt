package com.moblin.android.streamingplatforms.soop

import com.moblin.android.common.various.httpGet
import com.moblin.android.common.various.sleep
import com.moblin.android.various.model.PlatformStatus
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
    var platformStatus: PlatformStatus = PlatformStatus.Unknown

    fun start(userId: String) {
        platformStatus = PlatformStatus.Unknown
        val url = "$baseUrl/v1.1/channel/$userId/home/section/broad"
        task = scope.launch {
            var delaySeconds = 5
            while (true) {
                try {
                    sleep(seconds = delaySeconds)
                    val channelInfo = getChannelInfo(url)
                    platformStatus = PlatformStatus.Live(viewerCount = channelInfo.currentSumViewer)
                } catch (e: Exception) {
                    platformStatus = PlatformStatus.Unknown
                }
                if (!isActive) {
                    platformStatus = PlatformStatus.Unknown
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
        val response = httpGet(url)
        val data = response.body?.bytes() ?: ByteArray(0)
        return Json.decodeFromString(data.decodeToString())
    }
}
