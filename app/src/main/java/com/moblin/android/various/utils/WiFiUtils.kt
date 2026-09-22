package com.moblin.android.various.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

private val mainScope = CoroutineScope(Dispatchers.Main)

private fun fetchCurrentWiFiSsidCoreWlan(): String? {
    return null
}

fun fetchCurrentWiFiSsid(onCompleted: (String?) -> Unit) {
    Unit
}
