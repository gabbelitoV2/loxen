package com.moblin.android.platform.coreimage

import com.moblin.android.platform.coregraphics.CGAffineTransform
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGRect

class CIVector(val values: DoubleArray) {
    constructor(x: Double) : this(doubleArrayOf(x))
    constructor(x: Double, y: Double) : this(doubleArrayOf(x, y))
    constructor(x: Double, y: Double, z: Double) : this(doubleArrayOf(x, y, z))
    constructor(x: Double, y: Double, z: Double, w: Double) : this(doubleArrayOf(x, y, z, w))
    constructor(x: Number, y: Number) : this(doubleArrayOf(x.toDouble(), y.toDouble()))
    constructor(x: Number, y: Number, z: Number) : this(doubleArrayOf(x.toDouble(), y.toDouble(), z.toDouble()))
    constructor(x: Number, y: Number, z: Number, w: Number) : this(
        doubleArrayOf(x.toDouble(), y.toDouble(), z.toDouble(), w.toDouble())
    )
    constructor(values: List<Double>, count: Int) : this(DoubleArray(count) { values.getOrElse(it) { 0.0 } })
    constructor(cgPoint: CGPoint) : this(doubleArrayOf(cgPoint.x, cgPoint.y))
    constructor(cgRect: CGRect) : this(doubleArrayOf(cgRect.origin.x, cgRect.origin.y, cgRect.size.width, cgRect.size.height))
    constructor(cgAffineTransform: CGAffineTransform) : this(
        doubleArrayOf(
            cgAffineTransform.a,
            cgAffineTransform.b,
            cgAffineTransform.c,
            cgAffineTransform.d,
            cgAffineTransform.tx,
            cgAffineTransform.ty
        )
    )

    val count: Int
        get() = values.size

    val x: Double
        get() = value(at = 0)

    val y: Double
        get() = value(at = 1)

    val z: Double
        get() = value(at = 2)

    val w: Double
        get() = value(at = 3)

    val cgPointValue: CGPoint
        get() = CGPoint(x, y)

    val cgRectValue: CGRect
        get() = CGRect(x, y, z, w)

    fun value(at: Int): Double {
        return values.getOrElse(at) { 0.0 }
    }

    override fun equals(other: Any?): Boolean {
        return other is CIVector && values.contentEquals(other.values)
    }

    override fun hashCode(): Int {
        return values.contentHashCode()
    }

    override fun toString(): String {
        return "CIVector(${values.joinToString(", ")})"
    }
}
