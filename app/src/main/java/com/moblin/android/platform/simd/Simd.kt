package com.moblin.android.platform.simd

import kotlin.math.sqrt

private fun normalizedHash(value: Float): Int {
    return if (value == 0f) 0 else value.hashCode()
}

class SIMD2(val x: Float, val y: Float) {
    constructor(x: Double, y: Double) : this(x.toFloat(), y.toFloat())
    constructor(x: Number, y: Number) : this(x.toFloat(), y.toFloat())

    operator fun get(i: Int): Float {
        return when (i) {
            0 -> x
            1 -> y
            else -> throw IndexOutOfBoundsException("SIMD2 index $i")
        }
    }

    operator fun plus(o: SIMD2): SIMD2 = SIMD2(x + o.x, y + o.y)

    operator fun minus(o: SIMD2): SIMD2 = SIMD2(x - o.x, y - o.y)

    operator fun times(s: Float): SIMD2 = SIMD2(x * s, y * s)

    operator fun times(o: SIMD2): SIMD2 = SIMD2(x * o.x, y * o.y)

    operator fun div(s: Float): SIMD2 = SIMD2(x / s, y / s)

    operator fun div(o: SIMD2): SIMD2 = SIMD2(x / o.x, y / o.y)

    operator fun unaryMinus(): SIMD2 = SIMD2(-x, -y)

    fun copy(x: Float = this.x, y: Float = this.y): SIMD2 = SIMD2(x, y)

    override fun equals(other: Any?): Boolean {
        return other is SIMD2 && x == other.x && y == other.y
    }

    override fun hashCode(): Int {
        return 31 * normalizedHash(x) + normalizedHash(y)
    }

    override fun toString(): String {
        return "SIMD2($x, $y)"
    }

    companion object {
        val zero = SIMD2(0f, 0f)
        val one = SIMD2(1f, 1f)
    }
}

class SIMD3(val x: Float, val y: Float, val z: Float) {
    constructor(x: Double, y: Double, z: Double) : this(x.toFloat(), y.toFloat(), z.toFloat())
    constructor(x: Number, y: Number, z: Number) : this(x.toFloat(), y.toFloat(), z.toFloat())

    operator fun get(i: Int): Float {
        return when (i) {
            0 -> x
            1 -> y
            2 -> z
            else -> throw IndexOutOfBoundsException("SIMD3 index $i")
        }
    }

    operator fun plus(o: SIMD3): SIMD3 = SIMD3(x + o.x, y + o.y, z + o.z)

    operator fun minus(o: SIMD3): SIMD3 = SIMD3(x - o.x, y - o.y, z - o.z)

    operator fun times(s: Float): SIMD3 = SIMD3(x * s, y * s, z * s)

    operator fun times(o: SIMD3): SIMD3 = SIMD3(x * o.x, y * o.y, z * o.z)

    operator fun div(s: Float): SIMD3 = SIMD3(x / s, y / s, z / s)

    operator fun div(o: SIMD3): SIMD3 = SIMD3(x / o.x, y / o.y, z / o.z)

    operator fun unaryMinus(): SIMD3 = SIMD3(-x, -y, -z)

    fun copy(x: Float = this.x, y: Float = this.y, z: Float = this.z): SIMD3 = SIMD3(x, y, z)

    override fun equals(other: Any?): Boolean {
        return other is SIMD3 && x == other.x && y == other.y && z == other.z
    }

    override fun hashCode(): Int {
        return 31 * (31 * normalizedHash(x) + normalizedHash(y)) + normalizedHash(z)
    }

    override fun toString(): String {
        return "SIMD3($x, $y, $z)"
    }

    companion object {
        val zero = SIMD3(0f, 0f, 0f)
        val one = SIMD3(1f, 1f, 1f)
    }
}

class SIMD4(val x: Float, val y: Float, val z: Float, val w: Float) {
    constructor(x: Double, y: Double, z: Double, w: Double) : this(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())
    constructor(x: Number, y: Number, z: Number, w: Number) : this(
        x.toFloat(),
        y.toFloat(),
        z.toFloat(),
        w.toFloat()
    )

    operator fun get(i: Int): Float {
        return when (i) {
            0 -> x
            1 -> y
            2 -> z
            3 -> w
            else -> throw IndexOutOfBoundsException("SIMD4 index $i")
        }
    }

    operator fun plus(o: SIMD4): SIMD4 = SIMD4(x + o.x, y + o.y, z + o.z, w + o.w)

    operator fun minus(o: SIMD4): SIMD4 = SIMD4(x - o.x, y - o.y, z - o.z, w - o.w)

    operator fun times(s: Float): SIMD4 = SIMD4(x * s, y * s, z * s, w * s)

    operator fun times(o: SIMD4): SIMD4 = SIMD4(x * o.x, y * o.y, z * o.z, w * o.w)

    operator fun div(s: Float): SIMD4 = SIMD4(x / s, y / s, z / s, w / s)

    operator fun div(o: SIMD4): SIMD4 = SIMD4(x / o.x, y / o.y, z / o.z, w / o.w)

    operator fun unaryMinus(): SIMD4 = SIMD4(-x, -y, -z, -w)

    fun copy(x: Float = this.x, y: Float = this.y, z: Float = this.z, w: Float = this.w): SIMD4 {
        return SIMD4(x, y, z, w)
    }

    override fun equals(other: Any?): Boolean {
        return other is SIMD4 && x == other.x && y == other.y && z == other.z && w == other.w
    }

    override fun hashCode(): Int {
        var result = normalizedHash(x)
        result = 31 * result + normalizedHash(y)
        result = 31 * result + normalizedHash(z)
        return 31 * result + normalizedHash(w)
    }

    override fun toString(): String {
        return "SIMD4($x, $y, $z, $w)"
    }

    companion object {
        val zero = SIMD4(0f, 0f, 0f, 0f)
        val one = SIMD4(1f, 1f, 1f, 1f)
    }
}

typealias simd_float2 = SIMD2
typealias simd_float3 = SIMD3
typealias simd_float4 = SIMD4
typealias float2 = SIMD2
typealias float3 = SIMD3
typealias float4 = SIMD4

operator fun Float.times(v: SIMD2): SIMD2 = v * this

operator fun Float.times(v: SIMD3): SIMD3 = v * this

operator fun Float.times(v: SIMD4): SIMD4 = v * this

fun simd_make_float2(x: Float, y: Float): SIMD2 = SIMD2(x, y)

fun simd_make_float3(x: Float, y: Float, z: Float): SIMD3 = SIMD3(x, y, z)

fun simd_make_float4(x: Float, y: Float, z: Float, w: Float): SIMD4 = SIMD4(x, y, z, w)

fun dot(a: SIMD2, b: SIMD2): Float = a.x * b.x + a.y * b.y

fun dot(a: SIMD3, b: SIMD3): Float = a.x * b.x + a.y * b.y + a.z * b.z

fun dot(a: SIMD4, b: SIMD4): Float = a.x * b.x + a.y * b.y + a.z * b.z + a.w * b.w

fun length(v: SIMD2): Float = sqrt(dot(v, v))

fun length(v: SIMD3): Float = sqrt(dot(v, v))

fun length(v: SIMD4): Float = sqrt(dot(v, v))

fun distance(a: SIMD2, b: SIMD2): Float = length(a - b)

fun distance(a: SIMD3, b: SIMD3): Float = length(a - b)

fun normalize(v: SIMD2): SIMD2 = v / length(v)

fun normalize(v: SIMD3): SIMD3 = v / length(v)

fun normalize(v: SIMD4): SIMD4 = v / length(v)

fun cross(a: SIMD3, b: SIMD3): SIMD3 {
    return SIMD3(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x)
}

class simd_float3x3 private constructor(private val columnList: List<SIMD3>) {
    constructor(c0: SIMD3, c1: SIMD3, c2: SIMD3) : this(listOf(c0, c1, c2))

    constructor(columns: List<SIMD3>? = null, rows: List<SIMD3>? = null) : this(
        makeColumns3(columns, rows)
    )

    constructor(diagonal: SIMD3) : this(
        listOf(SIMD3(diagonal.x, 0f, 0f), SIMD3(0f, diagonal.y, 0f), SIMD3(0f, 0f, diagonal.z))
    )

    val columns: List<SIMD3>
        get() = columnList

    operator fun get(column: Int): SIMD3 = columnList[column]

    operator fun get(column: Int, row: Int): Float = columnList[column][row]

    operator fun times(o: simd_float3x3): simd_float3x3 {
        return simd_float3x3(this * o[0], this * o[1], this * o[2])
    }

    operator fun times(v: SIMD3): SIMD3 {
        return columnList[0] * v.x + columnList[1] * v.y + columnList[2] * v.z
    }

    val transpose: simd_float3x3
        get() = simd_float3x3(rows = columnList)

    override fun equals(other: Any?): Boolean {
        return other is simd_float3x3 && columnList == other.columnList
    }

    override fun hashCode(): Int {
        return columnList.hashCode()
    }

    override fun toString(): String {
        return "simd_float3x3(columns=$columnList)"
    }

    companion object {
        val identity = simd_float3x3(SIMD3(1f, 0f, 0f), SIMD3(0f, 1f, 0f), SIMD3(0f, 0f, 1f))
    }
}

private fun makeColumns3(columns: List<SIMD3>?, rows: List<SIMD3>?): List<SIMD3> {
    if (columns != null) {
        return List(3) { columns.getOrElse(it) { SIMD3.zero } }
    }
    if (rows != null) {
        val r = List(3) { rows.getOrElse(it) { SIMD3.zero } }
        return List(3) { column -> SIMD3(r[0][column], r[1][column], r[2][column]) }
    }
    return List(3) { SIMD3.zero }
}

class simd_float4x4 private constructor(private val columnList: List<SIMD4>) {
    constructor(c0: SIMD4, c1: SIMD4, c2: SIMD4, c3: SIMD4) : this(listOf(c0, c1, c2, c3))

    constructor(columns: List<SIMD4>? = null, rows: List<SIMD4>? = null) : this(
        makeColumns4(columns, rows)
    )

    constructor(diagonal: SIMD4) : this(
        listOf(
            SIMD4(diagonal.x, 0f, 0f, 0f),
            SIMD4(0f, diagonal.y, 0f, 0f),
            SIMD4(0f, 0f, diagonal.z, 0f),
            SIMD4(0f, 0f, 0f, diagonal.w)
        )
    )

    val columns: List<SIMD4>
        get() = columnList

    operator fun get(column: Int): SIMD4 = columnList[column]

    operator fun get(column: Int, row: Int): Float = columnList[column][row]

    operator fun times(o: simd_float4x4): simd_float4x4 {
        return simd_float4x4(this * o[0], this * o[1], this * o[2], this * o[3])
    }

    operator fun times(v: SIMD4): SIMD4 {
        return columnList[0] * v.x + columnList[1] * v.y + columnList[2] * v.z + columnList[3] * v.w
    }

    val transpose: simd_float4x4
        get() = simd_float4x4(rows = columnList)

    fun setting(column: Int, row: Int, value: Float): simd_float4x4 {
        val updated = columnList.toMutableList()
        val c = updated[column]
        updated[column] = when (row) {
            0 -> c.copy(x = value)
            1 -> c.copy(y = value)
            2 -> c.copy(z = value)
            else -> c.copy(w = value)
        }
        return simd_float4x4(updated[0], updated[1], updated[2], updated[3])
    }

    override fun equals(other: Any?): Boolean {
        return other is simd_float4x4 && columnList == other.columnList
    }

    override fun hashCode(): Int {
        return columnList.hashCode()
    }

    override fun toString(): String {
        return "simd_float4x4(columns=$columnList)"
    }

    companion object {
        val identity = simd_float4x4(
            SIMD4(1f, 0f, 0f, 0f),
            SIMD4(0f, 1f, 0f, 0f),
            SIMD4(0f, 0f, 1f, 0f),
            SIMD4(0f, 0f, 0f, 1f)
        )
    }
}

private fun makeColumns4(columns: List<SIMD4>?, rows: List<SIMD4>?): List<SIMD4> {
    if (columns != null) {
        return List(4) { columns.getOrElse(it) { SIMD4.zero } }
    }
    if (rows != null) {
        val r = List(4) { rows.getOrElse(it) { SIMD4.zero } }
        return List(4) { column -> SIMD4(r[0][column], r[1][column], r[2][column], r[3][column]) }
    }
    return List(4) { SIMD4.zero }
}

typealias float3x3 = simd_float3x3
typealias float4x4 = simd_float4x4

val matrix_identity_float3x3: simd_float3x3 = simd_float3x3.identity
val matrix_identity_float4x4: simd_float4x4 = simd_float4x4.identity
