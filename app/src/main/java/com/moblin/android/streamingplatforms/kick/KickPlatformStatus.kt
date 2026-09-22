package com.moblin.android.streamingplatforms.kick

import com.moblin.android.various.model.PlatformStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class KickPlatformStatus {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var task: Job? = null
    var platformStatus: PlatformStatus = PlatformStatus.unknown

    fun start(channelName: String) {
        task = scope.launch {
            var delaySeconds = 1.0
            while (true) {
                try {
                    delay((delaySeconds * 1000).toLong())
                    val info = getKickChannelInfo(channelName = channelName)
                    val livestream = info.livestream
                    if (livestream != null) {
                        setNumberOfViewers(PlatformStatus.live(viewerCount = livestream.viewers))
                    } else {
                        setNumberOfViewers(PlatformStatus.offline)
                    }
                } catch (e: Exception) {
                    setNumberOfViewers(PlatformStatus.unknown)
                }
                if (!isActive) {
                    setNumberOfViewers(PlatformStatus.unknown)
                    break
                }
                delaySeconds = 30.0
            }
        }
    }

    private fun setNumberOfViewers(status: PlatformStatus) {
        platformStatus = status
    }

    fun stop() {
        task?.cancel()
        task = null
    }
}
