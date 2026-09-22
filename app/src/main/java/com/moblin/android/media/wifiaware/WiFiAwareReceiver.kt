package com.moblin.android.media.wifiaware

import kotlinx.coroutines.Job

private data class Sender(
    val connection: Any,
    val task: Job
)

class WiFiAwareReceiver {
    companion object {
        val shared: WiFiAwareReceiver = WiFiAwareReceiver()
    }

    private val connections: MutableList<Sender> = mutableListOf()

    suspend fun listen() {
        TODO("no Android counterpart for WiFiAware")
    }
}
