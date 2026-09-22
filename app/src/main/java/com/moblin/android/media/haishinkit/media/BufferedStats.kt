package com.moblin.android.media.haishinkit.media

class BufferedStats {
    private var numberOfDuplicated = 0
    private var numberOfDropped = 0
    private var latestPresentationTimeStamp = 0.0

    fun incrementDuplicated() {
        numberOfDuplicated += 1
    }

    fun incrementDropped(count: Int) {
        numberOfDropped += count
    }

    fun getStats(presentationTimeStamp: Double): Pair<Int, Int>? {
        if (presentationTimeStamp <= latestPresentationTimeStamp + outputInterval) {
            return null
        }
        if (numberOfDuplicated <= 0 && numberOfDropped <= 0) {
            return null
        }
        val result = numberOfDuplicated to numberOfDropped
        numberOfDuplicated = 0
        numberOfDropped = 0
        latestPresentationTimeStamp = presentationTimeStamp
        return result
    }

    companion object {
        private const val outputInterval = 5.0
    }
}
