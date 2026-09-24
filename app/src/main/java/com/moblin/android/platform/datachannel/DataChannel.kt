package com.moblin.android.platform.datachannel

import java.io.IOException

enum class DataChannelConnectionState {
    new,
    connecting,
    connected,
    disconnected,
    failed,
    closed,
    ;

    companion object {
        operator fun invoke(value: rtcState): DataChannelConnectionState? {
            return when (value) {
                RTC_NEW -> new
                RTC_CONNECTING -> connecting
                RTC_CONNECTED -> connected
                RTC_DISCONNECTED -> disconnected
                RTC_FAILED -> failed
                RTC_CLOSED -> closed
                else -> null
            }
        }
    }
}

enum class DataChannelGatheringState {
    new,
    inProgress,
    complete,
    ;

    companion object {
        operator fun invoke(value: rtcGatheringState): DataChannelGatheringState? {
            return when (value) {
                RTC_GATHERING_NEW -> new
                RTC_GATHERING_INPROGRESS -> inProgress
                RTC_GATHERING_COMPLETE -> complete
                else -> null
            }
        }
    }
}

class DataChannelError(message: String) : IOException(message) {
    override fun toString(): String {
        return message ?: ""
    }
}
