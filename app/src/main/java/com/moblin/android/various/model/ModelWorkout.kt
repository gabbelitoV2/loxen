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
        TODO("no Android counterpart for HealthKit")
    }

    fun stop() {
        TODO("no Android counterpart for HealthKit")
    }

    private fun handleStateChange(session: Any, toState: Any, fromState: Any, date: Instant) {
        TODO("no Android counterpart for HealthKit")
    }

    private fun handleError(session: Any, error: Throwable) {
        TODO("no Android counterpart for HealthKit")
    }

    private fun finished(session: Any) {
        TODO("no Android counterpart for HealthKit")
    }

    fun addHeartRate(heartRate: Int) {
        TODO("no Android counterpart for HealthKit")
    }

    fun addCyclingPower(cyclingPower: Int) {
        TODO("no Android counterpart for HealthKit")
    }

    fun addCyclingCadence(cyclingCadence: Int) {
        TODO("no Android counterpart for HealthKit")
    }

    private fun add(type: Any, unit: Any, value: Double) {
        TODO("no Android counterpart for HealthKit")
    }

    fun workoutSession(session: Any, toState: Any, fromState: Any, date: Instant) {
        TODO("no Android counterpart for HealthKit")
    }

    fun workoutSession(session: Any, error: Throwable) {
        TODO("no Android counterpart for HealthKit")
    }

    fun workoutBuilder(workoutBuilder: Any, collectedTypes: Set<Any>) {
        TODO("no Android counterpart for HealthKit")
    }

    fun workoutBuilderDidCollectEvent(workoutBuilder: Any) {
        TODO("no Android counterpart for HealthKit")
    }
}

fun Model.startWorkout(type: WatchProtocolWorkoutType) {
    TODO("no Android counterpart for HealthKit")
}

fun Model.stopWorkout() {
    TODO("no Android counterpart for HealthKit")
}

fun Model.addWorkoutHeartRate(heartRate: Int) {
    TODO("no Android counterpart for HealthKit")
}

fun Model.addWorkoutCyclingPower(power: Int) {
    TODO("no Android counterpart for HealthKit")
}

fun Model.addWorkoutCyclingCadence(cadence: Int) {
    TODO("no Android counterpart for HealthKit")
}

private fun Model.authorizeHealthKit(completion: () -> Unit) {
    TODO("no Android counterpart for HealthKit")
}
