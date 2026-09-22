package com.moblin.android.integrations.workoutdevice

import java.time.Duration
import java.time.Instant

private const val averageSampleCount = 3

class WorkoutDeviceAverageCalculator {
    private var values = DoubleArray(averageSampleCount)
    private var nextIndex = 0

    fun reset() {
        values = DoubleArray(averageSampleCount)
        nextIndex = 0
    }

    fun update(value: Double) {
        values[nextIndex] = value
        nextIndex += 1
        nextIndex %= averageSampleCount
    }

    fun average(): Double {
        return values.sum() / averageSampleCount.toDouble()
    }

    fun averageIgnoreZeros(): Double {
        val nonZeroValues = values.filter { it != 0.0 }
        if (nonZeroValues.isEmpty()) {
            return 0.0
        }
        return nonZeroValues.sum() / nonZeroValues.size.toDouble()
    }
}

class WorkoutDeviceCrankCadence {
    private var previousRevolutions: UShort? = null
    private var previousRevolutionsTime: UShort? = null
    private val averageCadence = WorkoutDeviceAverageCalculator()
    private var latestAverageCadenceUpdateTime: Instant = Instant.now()
    private var reportsCadence = false

    fun reset() {
        previousRevolutions = null
        previousRevolutionsTime = null
        averageCadence.reset()
        reportsCadence = false
    }

    fun update(revolutions: UShort?, time: UShort?, now: Instant): Int? {
        var cadence = -1.0
        if (revolutions != null && time != null) {
            val lastRevolutions = previousRevolutions
            val lastRevolutionsTime = previousRevolutionsTime
            if (lastRevolutions != null && lastRevolutionsTime != null) {
                var deltaRevolutions = revolutions.toInt() - lastRevolutions.toInt()
                if (deltaRevolutions < 0) {
                    deltaRevolutions += 65536
                }
                var deltaTime = time.toInt() - lastRevolutionsTime.toInt()
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
            averageCadence.update(cadence)
            latestAverageCadenceUpdateTime = now
        } else if (Duration.between(latestAverageCadenceUpdateTime, now) > Duration.ofSeconds(3)) {
            averageCadence.update(0.0)
        }
        if (!reportsCadence) {
            return null
        }
        return averageCadence.averageIgnoreZeros().toInt()
    }
}
