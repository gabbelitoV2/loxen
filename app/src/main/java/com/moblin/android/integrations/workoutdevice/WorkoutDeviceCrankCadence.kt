package com.moblin.android.integrations.workoutdevice

import com.moblin.android.platform.core.ContinuousClock
import kotlin.time.Duration.Companion.seconds

private const val averageSampleCount = 3

open class WorkoutDeviceAverageCalculator {
    private var values = DoubleArray(averageSampleCount)
    private var nextIndex = 0

    open fun reset() {
        values = DoubleArray(averageSampleCount)
        nextIndex = 0
    }

    open fun update(value: Double) {
        values[nextIndex] = value
        nextIndex += 1
        nextIndex %= averageSampleCount
    }

    open fun average(): Double {
        return values.sum() / averageSampleCount.toDouble()
    }

    open fun averageIgnoreZeros(): Double {
        val nonZeroValues = values.filter { it != 0.0 }
        if (nonZeroValues.isEmpty()) {
            return 0.0
        }
        return nonZeroValues.sum() / nonZeroValues.size.toDouble()
    }
}

open class WorkoutDeviceCrankCadence {
    private var previousRevolutions: UShort? = null
    private var previousRevolutionsTime: UShort? = null
    private val averageCadence = WorkoutDeviceAverageCalculator()
    private var latestAverageCadenceUpdateTime = ContinuousClock.now
    private var reportsCadence = false

    open fun reset() {
        previousRevolutions = null
        previousRevolutionsTime = null
        averageCadence.reset()
        reportsCadence = false
    }

    open fun update(revolutions: UShort?, time: UShort?, now: ContinuousClock.Instant): Int? {
        var cadence = -1.0
        if (revolutions != null && time != null) {
            val prevRevolutions = previousRevolutions
            val prevRevolutionsTime = previousRevolutionsTime
            if (prevRevolutions != null && prevRevolutionsTime != null) {
                var deltaRevolutions = revolutions.toInt() - prevRevolutions.toInt()
                if (deltaRevolutions < 0) {
                    deltaRevolutions += 65536
                }
                var deltaTime = time.toInt() - prevRevolutionsTime.toInt()
                if (deltaTime < 0) {
                    deltaTime += 65536
                }
                val deltaTimeSeconds = deltaTime.toDouble() / 1024
                if (deltaTimeSeconds > 0) {
                    cadence = 60 * deltaRevolutions.toDouble() / deltaTimeSeconds
                    cadence = minOf(cadence, 10000.0)
                }
            }
            previousRevolutions = revolutions
            previousRevolutionsTime = time
        }
        if (cadence != -1.0) {
            reportsCadence = true
            averageCadence.update(value = cadence)
            latestAverageCadenceUpdateTime = now
        } else if (latestAverageCadenceUpdateTime.duration(to = now) > 3.seconds) {
            averageCadence.update(value = 0.0)
        }
        if (!reportsCadence) {
            return null
        }
        return averageCadence.averageIgnoreZeros().toInt()
    }
}
