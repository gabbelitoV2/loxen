package com.moblin.android.platform.scenekit

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3): Vec3 = Vec3(x + o.x, y + o.y, z + o.z)

    operator fun minus(o: Vec3): Vec3 = Vec3(x - o.x, y - o.y, z - o.z)

    operator fun times(s: Float): Vec3 = Vec3(x * s, y * s, z * s)

    fun times(o: Vec3): Vec3 = Vec3(x * o.x, y * o.y, z * o.z)

    fun dot(o: Vec3): Float = x * o.x + y * o.y + z * o.z

    fun cross(o: Vec3): Vec3 = Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)

    fun lengthSquared(): Float = x * x + y * y + z * z

    fun length(): Float = sqrt(lengthSquared())

    fun normalized(): Vec3 {
        val scale = 1f / length()
        return Vec3(x * scale, y * scale, z * scale)
    }

    fun toSCNVector3(): SCNVector3 = SCNVector3(x, y, z)

    companion object {
        val zero = Vec3(0f, 0f, 0f)
        val one = Vec3(1f, 1f, 1f)

        fun of(vector: SCNVector3): Vec3 = Vec3(vector.x, vector.y, vector.z)
    }
}

internal class Quat(val x: Float, val y: Float, val z: Float, val w: Float) {
    val imag: Vec3
        get() = Vec3(x, y, z)

    operator fun times(q: Quat): Quat {
        val p = this
        val imag = q.imag * p.w + p.imag * q.w + p.imag.cross(q.imag)
        val real = p.w * q.w - p.imag.dot(q.imag)
        return Quat(imag.x, imag.y, imag.z, real)
    }

    fun lengthSquared(): Float = x * x + y * y + z * z + w * w

    fun conjugate(): Quat = Quat(-x, -y, -z, w)

    fun inverse(): Quat {
        val lengthSquared = lengthSquared()
        return Quat(-x / lengthSquared, -y / lengthSquared, -z / lengthSquared, w / lengthSquared)
    }

    fun act(v: Vec3): Vec3 {
        val result = this * (Quat(v.x, v.y, v.z, 0f) * inverse())
        return result.imag
    }

    fun rotationMatrix(): FloatArray {
        val lengthSquared = lengthSquared()
        val s = if (lengthSquared > 0f) 2f / lengthSquared else 0f
        val xx = x * x * s
        val yy = y * y * s
        val zz = z * z * s
        val xy = x * y * s
        val xz = x * z * s
        val yz = y * z * s
        val wx = w * x * s
        val wy = w * y * s
        val wz = w * z * s
        return floatArrayOf(
            1f - (yy + zz), xy + wz, xz - wy,
            xy - wz, 1f - (xx + zz), yz + wx,
            xz + wy, yz - wx, 1f - (xx + yy),
        )
    }

    fun toSCNVector4(): SCNVector4 = SCNVector4(x, y, z, w)

    companion object {
        val identity = Quat(0f, 0f, 0f, 1f)

        fun of(vector: SCNVector4): Quat = Quat(vector.x, vector.y, vector.z, vector.w)

        fun fromImagReal(imag: Vec3, real: Float): Quat = Quat(imag.x, imag.y, imag.z, real)

        fun fromAxisAngle(axisX: Float, axisY: Float, axisZ: Float, angle: Float): Quat {
            val length = sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ)
            if (length == 0f) {
                return identity
            }
            val s = sin(angle / 2f) / length
            return Quat(axisX * s, axisY * s, axisZ * s, cos(angle / 2f))
        }

        fun fromEuler(pitch: Float, yaw: Float, roll: Float): Quat {
            val qx = Quat(sin(pitch / 2f), 0f, 0f, cos(pitch / 2f))
            val qy = Quat(0f, sin(yaw / 2f), 0f, cos(yaw / 2f))
            val qz = Quat(0f, 0f, sin(roll / 2f), cos(roll / 2f))
            return qx * qy * qz
        }

        fun fromMatrix(m: FloatArray): Quat {
            val m00 = m[0]
            val m10 = m[1]
            val m20 = m[2]
            val m01 = m[3]
            val m11 = m[4]
            val m21 = m[5]
            val m02 = m[6]
            val m12 = m[7]
            val m22 = m[8]
            val trace = m00 + m11 + m22
            return if (trace > 0f) {
                val s = sqrt(trace + 1f) * 2f
                Quat((m21 - m12) / s, (m02 - m20) / s, (m10 - m01) / s, 0.25f * s)
            } else if (m00 > m11 && m00 > m22) {
                val s = sqrt(1f + m00 - m11 - m22) * 2f
                Quat(0.25f * s, (m01 + m10) / s, (m02 + m20) / s, (m21 - m12) / s)
            } else if (m11 > m22) {
                val s = sqrt(1f + m11 - m00 - m22) * 2f
                Quat((m01 + m10) / s, 0.25f * s, (m12 + m21) / s, (m02 - m20) / s)
            } else {
                val s = sqrt(1f + m22 - m00 - m11) * 2f
                Quat((m02 + m20) / s, (m12 + m21) / s, 0.25f * s, (m10 - m01) / s)
            }
        }

        private fun reduced(from: Vec3, to: Vec3): Quat {
            val half = (from + to).normalized()
            return fromImagReal(from.cross(half), from.dot(half))
        }

        fun rotation(from: Vec3, to: Vec3): Quat {
            if (from.dot(to) >= 0f) {
                return reduced(from, to)
            }
            var half = from + to
            if (half.lengthSquared() == 0f) {
                val absX = abs(from.x)
                val absY = abs(from.y)
                val absZ = abs(from.z)
                val axis = if (absX <= absY && absX <= absZ) {
                    from.cross(Vec3(1f, 0f, 0f))
                } else if (absY <= absZ) {
                    from.cross(Vec3(0f, 1f, 0f))
                } else {
                    from.cross(Vec3(0f, 0f, 1f))
                }
                return fromImagReal(axis.normalized(), 0f)
            }
            half = half.normalized()
            return reduced(from, half) * reduced(half, to)
        }
    }
}

internal fun eulerFromQuat(q: Quat): Vec3 {
    val r = q.rotationMatrix()
    val m02 = r[6]
    val pitch: Float
    val yaw = asin(m02.coerceIn(-1f, 1f))
    val roll: Float
    if (abs(m02) < 0.9999999f) {
        pitch = atan2(-r[7], r[8])
        roll = atan2(-r[3], r[0])
    } else {
        pitch = atan2(r[5], r[4])
        roll = 0f
    }
    return Vec3(pitch, yaw, roll)
}

internal object Mat4 {
    fun identity(): FloatArray {
        return floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)
    }

    fun compose(translation: Vec3, rotation: Quat, scale: Vec3): FloatArray {
        val r = rotation.rotationMatrix()
        return floatArrayOf(
            r[0] * scale.x, r[1] * scale.x, r[2] * scale.x, 0f,
            r[3] * scale.y, r[4] * scale.y, r[5] * scale.y, 0f,
            r[6] * scale.z, r[7] * scale.z, r[8] * scale.z, 0f,
            translation.x, translation.y, translation.z, 1f,
        )
    }

    fun multiply(a: FloatArray, b: FloatArray): FloatArray {
        val result = FloatArray(16)
        for (column in 0 until 4) {
            for (row in 0 until 4) {
                var sum = 0f
                for (k in 0 until 4) {
                    sum += a[k * 4 + row] * b[column * 4 + k]
                }
                result[column * 4 + row] = sum
            }
        }
        return result
    }

    fun transformPoint(m: FloatArray, v: Vec3): Vec3 {
        val x = m[0] * v.x + m[4] * v.y + m[8] * v.z + m[12]
        val y = m[1] * v.x + m[5] * v.y + m[9] * v.z + m[13]
        val z = m[2] * v.x + m[6] * v.y + m[10] * v.z + m[14]
        val w = m[3] * v.x + m[7] * v.y + m[11] * v.z + m[15]
        val inverse = 1f / w
        return Vec3(x * inverse, y * inverse, z * inverse)
    }

    fun invert(m: FloatArray): FloatArray {
        val inv = FloatArray(16)
        inv[0] = m[5] * m[10] * m[15] - m[5] * m[11] * m[14] - m[9] * m[6] * m[15] +
            m[9] * m[7] * m[14] + m[13] * m[6] * m[11] - m[13] * m[7] * m[10]
        inv[4] = -m[4] * m[10] * m[15] + m[4] * m[11] * m[14] + m[8] * m[6] * m[15] -
            m[8] * m[7] * m[14] - m[12] * m[6] * m[11] + m[12] * m[7] * m[10]
        inv[8] = m[4] * m[9] * m[15] - m[4] * m[11] * m[13] - m[8] * m[5] * m[15] +
            m[8] * m[7] * m[13] + m[12] * m[5] * m[11] - m[12] * m[7] * m[9]
        inv[12] = -m[4] * m[9] * m[14] + m[4] * m[10] * m[13] + m[8] * m[5] * m[14] -
            m[8] * m[6] * m[13] - m[12] * m[5] * m[10] + m[12] * m[6] * m[9]
        inv[1] = -m[1] * m[10] * m[15] + m[1] * m[11] * m[14] + m[9] * m[2] * m[15] -
            m[9] * m[3] * m[14] - m[13] * m[2] * m[11] + m[13] * m[3] * m[10]
        inv[5] = m[0] * m[10] * m[15] - m[0] * m[11] * m[14] - m[8] * m[2] * m[15] +
            m[8] * m[3] * m[14] + m[12] * m[2] * m[11] - m[12] * m[3] * m[10]
        inv[9] = -m[0] * m[9] * m[15] + m[0] * m[11] * m[13] + m[8] * m[1] * m[15] -
            m[8] * m[3] * m[13] - m[12] * m[1] * m[11] + m[12] * m[3] * m[9]
        inv[13] = m[0] * m[9] * m[14] - m[0] * m[10] * m[13] - m[8] * m[1] * m[14] +
            m[8] * m[2] * m[13] + m[12] * m[1] * m[10] - m[12] * m[2] * m[9]
        inv[2] = m[1] * m[6] * m[15] - m[1] * m[7] * m[14] - m[5] * m[2] * m[15] +
            m[5] * m[3] * m[14] + m[13] * m[2] * m[7] - m[13] * m[3] * m[6]
        inv[6] = -m[0] * m[6] * m[15] + m[0] * m[7] * m[14] + m[4] * m[2] * m[15] -
            m[4] * m[3] * m[14] - m[12] * m[2] * m[7] + m[12] * m[3] * m[6]
        inv[10] = m[0] * m[5] * m[15] - m[0] * m[7] * m[13] - m[4] * m[1] * m[15] +
            m[4] * m[3] * m[13] + m[12] * m[1] * m[7] - m[12] * m[3] * m[5]
        inv[14] = -m[0] * m[5] * m[14] + m[0] * m[6] * m[13] + m[4] * m[1] * m[14] -
            m[4] * m[2] * m[13] - m[12] * m[1] * m[6] + m[12] * m[2] * m[5]
        inv[3] = -m[1] * m[6] * m[11] + m[1] * m[7] * m[10] + m[5] * m[2] * m[11] -
            m[5] * m[3] * m[10] - m[9] * m[2] * m[7] + m[9] * m[3] * m[6]
        inv[7] = m[0] * m[6] * m[11] - m[0] * m[7] * m[10] - m[4] * m[2] * m[11] +
            m[4] * m[3] * m[10] + m[8] * m[2] * m[7] - m[8] * m[3] * m[6]
        inv[11] = -m[0] * m[5] * m[11] + m[0] * m[7] * m[9] + m[4] * m[1] * m[11] -
            m[4] * m[3] * m[9] - m[8] * m[1] * m[7] + m[8] * m[3] * m[5]
        inv[15] = m[0] * m[5] * m[10] - m[0] * m[6] * m[9] - m[4] * m[1] * m[10] +
            m[4] * m[2] * m[9] + m[8] * m[1] * m[6] - m[8] * m[2] * m[5]
        val determinant = m[0] * inv[0] + m[1] * inv[4] + m[2] * inv[8] + m[3] * inv[12]
        val scale = 1f / determinant
        for (i in 0 until 16) {
            inv[i] *= scale
        }
        return inv
    }

    fun decompose(m: FloatArray): Triple<Vec3, Quat, Vec3> {
        val translation = Vec3(m[12], m[13], m[14])
        var sx = Vec3(m[0], m[1], m[2]).length()
        val sy = Vec3(m[4], m[5], m[6]).length()
        val sz = Vec3(m[8], m[9], m[10]).length()
        val determinant = m[0] * (m[5] * m[10] - m[9] * m[6]) -
            m[4] * (m[1] * m[10] - m[9] * m[2]) +
            m[8] * (m[1] * m[6] - m[5] * m[2])
        if (determinant < 0f) {
            sx = -sx
        }
        val rotation = floatArrayOf(
            safeDivide(m[0], sx), safeDivide(m[1], sx), safeDivide(m[2], sx),
            safeDivide(m[4], sy), safeDivide(m[5], sy), safeDivide(m[6], sy),
            safeDivide(m[8], sz), safeDivide(m[9], sz), safeDivide(m[10], sz),
        )
        return Triple(translation, Quat.fromMatrix(rotation), Vec3(sx, sy, sz))
    }

    private fun safeDivide(value: Float, divisor: Float): Float {
        return if (divisor == 0f) 0f else value / divisor
    }
}
