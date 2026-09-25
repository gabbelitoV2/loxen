package com.moblin.android.various.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val mainScope = CoroutineScope(Dispatchers.Main)

private fun fetchCurrentWiFiSsidCoreWlan(): String? {
    return null
}

fun fetchCurrentWiFiSsid(onCompleted: (String?) -> Unit) {
    com.moblin.android.platform.networkextension.NEHotspotNetwork.fetchCurrent { network -> val ssid = network?.ssid; mainScope.launch { onCompleted(ssid) } }
}
