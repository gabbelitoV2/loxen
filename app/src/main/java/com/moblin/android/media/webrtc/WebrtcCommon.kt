package com.moblin.android.media.webrtc

import com.moblin.android.platform.log.Log
import com.moblin.android.platform.datachannel.*

private const val TAG = "WebrtcCommon"

const val defaultStunServer: String = "stun:stun.l.google.com:19302"

fun setupWebrtcDebugLogging() {
    rtcInitLogger(RTC_LOG_DEBUG) { _, message ->
        if (message == null) {
            return@rtcInitLogger
        }
        Log.i(TAG, "webrtc: $message")
    }
}
