package com.moblin.android.various.model

import android.util.Log
import com.moblin.android.media.wifiaware.WiFiAwareReceiver
import com.moblin.android.media.wifiaware.WiFiAwareSender
import com.moblin.android.view.settings.wifiaware.WACapabilities
import com.moblin.android.view.settings.wifiaware.WAFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val wiFiAwareScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

fun Model.wiFiAwareUpdated() {
    stopWiFiAware()
    if (database.wiFiAware.enabled && WACapabilities.supportedFeatures.contains(WAFeature.wifiAware)) {
        startWiFiAware()
    }
}

private fun Model.startWiFiAware() {
    when (database.wiFiAware.role.toString()) {
        "sender" -> {
            wiFiAwareSenderTask = wiFiAwareScope.launch {
                try {
                    WiFiAwareSender.shared.browse()
                } catch (exception: Exception) {
                    Log.i("Model", "wifi: Sender error: $exception")
                }
            }
        }
        "receiver" -> {
            wiFiAwareReceiverTask = wiFiAwareScope.launch {
                try {
                    WiFiAwareReceiver.shared.listen()
                } catch (exception: Exception) {
                    Log.i("Model", "wifi: Receiver error: $exception")
                }
            }
        }
    }
}

private fun Model.stopWiFiAware() {
    wiFiAwareSenderTask?.cancel()
    wiFiAwareSenderTask = null
    wiFiAwareReceiverTask?.cancel()
    wiFiAwareReceiverTask = null
}
