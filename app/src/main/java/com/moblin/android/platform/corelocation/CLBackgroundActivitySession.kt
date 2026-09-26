package com.moblin.android.platform.corelocation

import com.moblin.android.platform.host.StreamingService

class CLBackgroundActivitySession {
    private var valid = true

    init {
        StreamingService.startLocation()
    }

    @Synchronized
    fun invalidate() {
        if (!valid) {
            return
        }
        valid = false
        StreamingService.stopLocation()
    }
}
