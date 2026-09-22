package com.moblin.android.various.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

private val mainScope = CoroutineScope(Dispatchers.Main)

private fun fetchCurrentWiFiSsidCoreWlan(): String? {
    TODO("no Android counterpart for NetworkExtension")
}

fun fetchCurrentWiFiSsid(onCompleted: (String?) -> Unit) {
    TODO("no Android counterpart for NetworkExtension")
}
