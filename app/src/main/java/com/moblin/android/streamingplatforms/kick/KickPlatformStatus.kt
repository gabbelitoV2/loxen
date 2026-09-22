package com.moblin.android.streamingplatforms.kick

import com.moblin.android.common.various.sleep
import com.moblin.android.various.model.PlatformStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class KickPlatformStatus {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var task: Job? = null
    var platformStatus: PlatformStatus = PlatformStatus.Unknown

    fun start(channelName: String) {
        task = scope.launch {
            var delay = 1.0
            while (true) {
                try {
                    sleep(seconds = delay)
                    val info = getKickChannelInfo(channelName = channelName)
                    val livestream = info.livestream
                    if (livestream != null) {
                        setNumberOfViewers(PlatformStatus.Live(viewerCount = livestream.viewers))
                    } else {
                        setNumberOfViewers(PlatformStatus.Offline)
                    }
                } catch (e: Exception) {
                    setNumberOfViewers(PlatformStatus.Unknown)
                }
                if (!isActive) {
                    setNumberOfViewers(PlatformStatus.Unknown)
                    break
                }
                delay = 30.0
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
