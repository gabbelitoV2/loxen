package com.moblin.android.various.model

import com.moblin.android.common.various.activeEnergyBurnedType
import com.moblin.android.common.various.cyclingCadenceType
import com.moblin.android.common.various.cyclingPowerType
import com.moblin.android.common.various.distanceCyclingType
import com.moblin.android.common.various.distanceWalkingRunningType
import com.moblin.android.common.various.heartRateType
import com.moblin.android.common.various.runningPowerType
import com.moblin.android.common.various.stepCountType
import java.time.Instant

private fun types(): MutableSet<Any> {
    val types = mutableSetOf<Any>(
        heartRateType,
        distanceCyclingType,
        distanceWalkingRunningType,
        stepCountType,
        activeEnergyBurnedType,
        runningPowerType,
    )
    types.add(cyclingPowerType)
    types.add(cyclingCadenceType)
    return types
}

private class Workout {
    companion object {
        val shared: Workout = Workout()
    }

    private val healthStore: Any? = null
    private var workoutSession: Any? = null
    private var workoutBuilder: Any? = null
    private var model: Model? = null
    private val latestSampleTimes: MutableMap<Any, Instant> = mutableMapOf()
    private var workoutType: WatchProtocolWorkoutType? = null

    fun isActive(): Boolean {
        return workoutSession != null
    }

    fun start(model: Model, type: WatchProtocolWorkoutType): Boolean {
        return false
    }

    fun stop() {
        Unit
    }

    private fun handleStateChange(session: Any, toState: Any, fromState: Any, date: Instant) {
        Unit
    }

    private fun handleError(session: Any, error: Throwable) {
        Unit
    }

    private fun finished(session: Any) {
        Unit
    }

    fun addHeartRate(heartRate: Int) {
        Unit
    }

    fun addCyclingPower(cyclingPower: Int) {
        Unit
    }

    fun addCyclingCadence(cyclingCadence: Int) {
        Unit
    }

    private fun add(type: Any, unit: Any, value: Double) {
        Unit
    }

    fun workoutSession(session: Any, toState: Any, fromState: Any, date: Instant) {
        Unit
    }

    fun workoutSession(session: Any, error: Throwable) {
        Unit
    }

    fun workoutBuilder(workoutBuilder: Any, collectedTypes: Set<Any>) {
        Unit
    }

    fun workoutBuilderDidCollectEvent(workoutBuilder: Any) {
        Unit
    }
}

fun Model.startWorkout(type: WatchProtocolWorkoutType) {
    Unit
}

fun Model.stopWorkout() {
    Unit
}

fun Model.addWorkoutHeartRate(heartRate: Int) {
    Unit
}

fun Model.addWorkoutCyclingPower(power: Int) {
    Unit
}

fun Model.addWorkoutCyclingCadence(cadence: Int) {
    Unit
}

private fun Model.authorizeHealthKit(completion: () -> Unit) {
    Unit
}
