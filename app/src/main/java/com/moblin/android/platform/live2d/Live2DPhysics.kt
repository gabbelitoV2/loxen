package com.moblin.android.platform.live2d

import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

private const val REF_FPS = 30f
private const val DEFAULT_COMPAT_FPS = 60f
private const val ACC_FAC = REF_FPS * REF_FPS
private const val MAX_SIM_TIME = 5f
private const val SETTLE_SIM_TIME = 60f
private const val PI_F = Math.PI.toFloat()
private const val TAU_F = (2.0 * Math.PI).toFloat()

internal class PhysicsVertexConfig(val mobility: Float, val delay: Float, val acceleration: Float, val radius: Float)

internal class PhysicsRange(val minimum: Float, val default: Float, val maximum: Float)

internal class PhysicsInputConfig(val sourceId: String, val weight: Float, val isAngle: Boolean, val reflect: Boolean)

internal class PhysicsOutputConfig(
    val destinationId: String,
    val vertexIndex: Long,
    val scale: Float,
    val weight: Float,
    val reflect: Boolean,
)

internal class PhysicsSettingConfig(
    val id: String,
    val inputs: List<PhysicsInputConfig>,
    val outputs: List<PhysicsOutputConfig>,
    val vertices: List<PhysicsVertexConfig>,
    val position: PhysicsRange,
    val angle: PhysicsRange,
)

internal class Physics3Config(
    val fps: Float?,
    val gravityX: Float,
    val gravityY: Float,
    val settings: List<PhysicsSettingConfig>,
)

private fun normAngle(value: Float): Float {
    if (!value.isFinite()) {
        return value
    }
    val a = value % TAU_F
    return if (a > PI_F) {
        a - TAU_F
    } else if (a < -PI_F) {
        a + TAU_F
    } else {
        a
    }
}

private class PhysicsOptions(val minFps: Float, val worldFps: Float) {
    val gravityLookahead = true
    val angularMomentumLoss = true
    val rotationBoost = 0.2f
}

private class Pendulum(var pivotX: Float, var pivotY: Float, val config: PhysicsVertexConfig) {
    var gAngle = 0f
    var angle = 0f
    var velocity = 0f
    var bobX = pivotX + 0f
    var bobY = pivotY + config.radius

    private fun derivative(a: Float, v: Float, offset: Float, options: PhysicsOptions, out: FloatArray) {
        val da = v
        var aAdjusted = a - offset
        val worldFps = options.worldFps / config.delay
        if (options.gravityLookahead) {
            aAdjusted += da / worldFps
        }
        var dv = -ACC_FAC * config.acceleration / config.radius * sin(aAdjusted)
        if (options.angularMomentumLoss) {
            val v2 = atan(v / worldFps) * worldFps
            dv += (v2 - v) * worldFps * 3f
        }
        dv -= v * worldFps * (1f - config.mobility)
        out[0] = da
        out[1] = dv
    }

    private val d = FloatArray(2)

    fun simulate(dt: Float, pivotX: Float, pivotY: Float, gAngle: Float, options: PhysicsOptions): Boolean {
        if (dt.isInfinite()) {
            val changed = this.pivotX != pivotX || this.pivotY != pivotY || this.gAngle != gAngle
            this.pivotX = pivotX
            this.pivotY = pivotY
            this.gAngle = gAngle
            angle = gAngle
            velocity = 0f
            updateBob()
            return changed
        }
        val scaledDt = config.delay * dt
        if (pivotX != this.pivotX || pivotY != this.pivotY) {
            val deltaX = this.pivotY - pivotY
            val deltaY = this.pivotX - pivotX
            val beforeX = cos(angle) * config.radius
            val beforeY = sin(angle) * config.radius
            angle = atan2(beforeY + deltaY, beforeX + deltaX)
            this.pivotX = pivotX
            this.pivotY = pivotY
        }
        if (gAngle != this.gAngle) {
            val worldFps = options.worldFps / config.delay
            velocity += ((gAngle - this.gAngle) * options.rotationBoost) * worldFps
            this.gAngle = gAngle
        }
        val steps = ceil(rustMax(config.delay, 1f)).toInt()
        for (i in 0 until steps) {
            simulateMotion(scaledDt / steps.toFloat(), gAngle, options)
        }
        return true
    }

    private fun simulateMotion(dt: Float, gAngle: Float, options: PhysicsOptions) {
        val a = angle
        val v = velocity
        derivative(a, v, gAngle, options, d)
        val k1a = dt * d[0]
        val k1v = dt * d[1]
        derivative(a + 0.5f * k1a, v + 0.5f * k1v, gAngle, options, d)
        val k2a = dt * d[0]
        val k2v = dt * d[1]
        derivative(a + 0.5f * k2a, v + 0.5f * k2v, gAngle, options, d)
        val k3a = dt * d[0]
        val k3v = dt * d[1]
        derivative(a + k3a, v + k3v, gAngle, options, d)
        val k4a = dt * d[0]
        val k4v = dt * d[1]
        val nextA = a + (k1a + 2f * k2a + 2f * k3a + k4a) / 6f
        val nextV = v + (k1v + 2f * k2v + 2f * k3v + k4v) / 6f
        val nextAngle = normAngle(nextA)
        if (angle.isFinite() && !nextAngle.isFinite() || velocity.isFinite() && !nextV.isFinite()) {
            angle = gAngle
            velocity = 0f
        } else {
            angle = nextAngle
            velocity = nextV
        }
        updateBob()
    }

    private fun updateBob() {
        bobX = sin(angle) * config.radius + pivotX
        bobY = cos(angle) * config.radius + pivotY
    }
}

private class PhysicsSystem(
    val setting: PhysicsSettingConfig,
    val outputs: List<PhysicsOutputConfig>,
    val pendulums: List<Pendulum>,
    val gravityAngle: Float,
) {
    private fun normalize(value: Float, range: PhysicsRange): Float {
        val v = rustClamp(value, -1f, 1f)
        return if (v > 0f) {
            range.default + v * (range.maximum - range.default)
        } else {
            range.default - v * (range.minimum - range.default)
        }
    }

    private fun simulate(dt: Float, inputX: Float, inputAngle: Float, options: PhysicsOptions): Boolean {
        var pivotX = inputX * cos(inputAngle)
        var pivotY = inputX * (0.5f * sin(inputAngle * 2f))
        var changed = false
        for (pendulum in pendulums) {
            if (pendulum.simulate(dt, pivotX, pivotY, inputAngle, options)) {
                changed = true
            }
            pivotX = pendulum.bobX
            pivotY = pendulum.bobY
        }
        return changed
    }

    private fun angle(index: Int): Float {
        val base = if (index == 0) gravityAngle else pendulums[index - 1].angle
        return normAngle(pendulums[index].angle - base)
    }

    fun update(pose: Live2DPose, dt: Float, options: PhysicsOptions): Boolean {
        var angle = 0f
        var x = 0f
        for (input in setting.inputs) {
            val index = pose.map.paramIndex(input.sourceId) ?: continue
            val descriptor = pose.map.descriptors[index]
            val value = pose.getFlattened(index)
            var t = (value - descriptor.min) / (descriptor.max - descriptor.min)
            t = 2f * t - 1f
            t = t * input.weight / 100f
            if (input.reflect) {
                t = -t
            }
            if (input.isAngle) {
                angle += t
            } else {
                x += t
            }
        }
        angle = normalize(angle, setting.angle)
        x = normalize(x, setting.position)
        val changed = simulate(dt, x, toRadians(angle), options)
        for (output in outputs) {
            val index = pose.map.paramIndex(output.destinationId) ?: continue
            val descriptor = pose.map.descriptors[index]
            var value = angle(output.vertexIndex.toInt() - 1) * output.scale
            if (output.reflect) {
                value = -value
            }
            value = rustClamp(value, descriptor.min, descriptor.max)
            if (!value.isFinite()) {
                value = descriptor.default
            }
            val current = pose.flattenedForUpdate(index)
            if (output.weight != 100f) {
                val a = output.weight / 100f
                pose.set(index, value * a + current * (1f - a))
            } else {
                pose.set(index, value)
            }
        }
        return changed
    }
}

internal class Live2DPhysicsEngine(config: Physics3Config) {
    private val options = PhysicsOptions(minFps = 50f, worldFps = config.fps ?: DEFAULT_COMPAT_FPS)
    private val systems: List<PhysicsSystem>

    init {
        val gravityAngle = -atan2(config.gravityX, -config.gravityY)
        systems = config.settings.map { setting ->
            val pendulums = ArrayList<Pendulum>()
            var pivotY = 0f
            for (vertex in setting.vertices.drop(1)) {
                pendulums.add(Pendulum(0f, pivotY, vertex))
                pivotY += vertex.radius
            }
            val outputs = setting.outputs.filter { it.vertexIndex != 0L && it.vertexIndex <= pendulums.size }
            PhysicsSystem(setting, outputs, pendulums, gravityAngle)
        }
    }

    fun update(pose: Live2DPose, deltaTime: Float) {
        var dt = deltaTime
        if (dt < 0f) {
            dt = 0f
        } else if (dt > MAX_SIM_TIME) {
            dt = MAX_SIM_TIME
        }
        var ticks = 1
        if (options.minFps > 0f) {
            val maxDt = 1f / options.minFps
            val rawTicks = floor((maxDt + dt) / maxDt)
            ticks = if (rawTicks.isNaN() || rawTicks <= 0f) 0 else rawTicks.toInt()
            dt /= ticks.toFloat()
        }
        for (i in 0 until ticks) {
            for (system in systems) {
                system.update(pose, dt, options)
            }
        }
    }

    fun settle(pose: Live2DPose) {
        val settlePose = pose.clone()
        for (i in 0 until systems.size + 1) {
            var changed = false
            for (system in systems) {
                if (system.update(settlePose, Float.POSITIVE_INFINITY, options)) {
                    changed = true
                }
            }
            if (!changed) {
                return
            }
        }
        for (i in 0 until ceil(SETTLE_SIM_TIME / MAX_SIM_TIME).toInt()) {
            update(settlePose, MAX_SIM_TIME)
        }
    }
}
