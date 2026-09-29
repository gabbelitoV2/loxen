package com.moblin.android.media

import com.moblin.android.platform.log.Log

class WrappingTimestamp(name: String, maximumTimestamp: Long) {
    private val name: String = name
    private val maximumTimestamp: Long = maximumTimestamp
    private val halfMaximumTimestamp: Long = maximumTimestamp / 2
    private var highTimestamp: Long = 0
    private var previousTimestamp: Long = 0

    fun update(timestamp: Long): Long {
        if (timestamp >= previousTimestamp) {
            if (timestamp - previousTimestamp < halfMaximumTimestamp) {
                try {
                    return highTimestamp + timestamp
                } finally {
                    previousTimestamp = timestamp
                }
            } else {
                return highTimestamp - maximumTimestamp + timestamp
            }
        } else {
            try {
                if (previousTimestamp - timestamp > halfMaximumTimestamp) {
                    Log.i(TAG, "Wrapping timestamp $name just wrapped around.")
                    highTimestamp = highTimestamp + maximumTimestamp
                }
                return highTimestamp + timestamp
            } finally {
                previousTimestamp = timestamp
            }
        }
    }

    companion object {
        private const val TAG = "WrappingTimestamp"
    }
}
