package com.moblin.android.platform.coregraphics

import com.moblin.android.platform.video.CVPixelBuffer
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private fun normalizedHash(value: Double): Int {
    return if (value == 0.0) 0 else value.hashCode()
}

class CGPoint(val x: Double, val y: Double) {
    constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())
    constructor(x: Float, y: Float) : this(x.toDouble(), y.toDouble())
    constructor(x: Number, y: Number) : this(x.toDouble(), y.toDouble())

    fun applying(t: CGAffineTransform): CGPoint {
        return CGPoint(t.a * x + t.c * y + t.tx, t.b * x + t.d * y + t.ty)
    }

    fun copy(x: Double = this.x, y: Double = this.y): CGPoint {
        return CGPoint(x, y)
    }

    override fun equals(other: Any?): Boolean {
        return other is CGPoint && x == other.x && y == other.y
    }

    override fun hashCode(): Int {
        return 31 * normalizedHash(x) + normalizedHash(y)
    }

    override fun toString(): String {
        return "CGPoint(x=$x, y=$y)"
    }

    companion object {
        val zero = CGPoint(0.0, 0.0)
    }
}

class CGSize(val width: Double, val height: Double) {
    constructor(width: Int, height: Int) : this(width.toDouble(), height.toDouble())
    constructor(width: Float, height: Float) : this(width.toDouble(), height.toDouble())
    constructor(width: Number, height: Number) : this(width.toDouble(), height.toDouble())

    fun applying(t: CGAffineTransform): CGSize {
        return CGSize(t.a * width + t.c * height, t.b * width + t.d * height)
    }

    fun copy(width: Double = this.width, height: Double = this.height): CGSize {
        return CGSize(width, height)
    }

    fun minimum(): Double {
        return min(width, height)
    }

    fun maximum(): Double {
        return max(width, height)
    }

    override fun equals(other: Any?): Boolean {
        return other is CGSize && width == other.width && height == other.height
    }

    override fun hashCode(): Int {
        return 31 * normalizedHash(width) + normalizedHash(height)
    }

    override fun toString(): String {
        return "CGSize(width=$width, height=$height)"
    }

    companion object {
        val zero = CGSize(0.0, 0.0)
    }
}

class CGRect(val origin: CGPoint, val size: CGSize) {
    constructor(x: Double, y: Double, width: Double, height: Double) : this(CGPoint(x, y), CGSize(width, height))
    constructor(x: Int, y: Int, width: Int, height: Int) : this(
        x.toDouble(),
        y.toDouble(),
        width.toDouble(),
        height.toDouble()
    )
    constructor(x: Float, y: Float, width: Float, height: Float) : this(
        x.toDouble(),
        y.toDouble(),
        width.toDouble(),
        height.toDouble()
    )
    constructor(x: Number, y: Number, width: Number, height: Number) : this(
        x.toDouble(),
        y.toDouble(),
        width.toDouble(),
        height.toDouble()
    )

    fun copy(
        x: Double = origin.x,
        y: Double = origin.y,
        width: Double = size.width,
        height: Double = size.height,
    ): CGRect {
        return CGRect(x, y, width, height)
    }

    val isNull: Boolean
        get() = origin.x == Double.POSITIVE_INFINITY || origin.y == Double.POSITIVE_INFINITY

    val isInfinite: Boolean
        get() = origin.x == INFINITE_ORIGIN && origin.y == INFINITE_ORIGIN &&
            size.width == INFINITE_SIZE && size.height == INFINITE_SIZE

    val isEmpty: Boolean
        get() = isNull || size.width == 0.0 || size.height == 0.0

    val minX: Double
        get() = if (size.width < 0) origin.x + size.width else origin.x

    val maxX: Double
        get() = if (size.width < 0) origin.x else origin.x + size.width

    val midX: Double
        get() = origin.x + size.width / 2

    val minY: Double
        get() = if (size.height < 0) origin.y + size.height else origin.y

    val maxY: Double
        get() = if (size.height < 0) origin.y else origin.y + size.height

    val midY: Double
        get() = origin.y + size.height / 2

    val width: Double
        get() = abs(size.width)

    val height: Double
        get() = abs(size.height)

    val standardized: CGRect
        get() {
            if (isNull || (size.width >= 0 && size.height >= 0)) {
                return this
            }
            return CGRect(minX, minY, width, height)
        }

    val integral: CGRect
        get() {
            if (isNull || isInfinite) {
                return this
            }
            val rect = standardized
            val x = floor(rect.origin.x)
            val y = floor(rect.origin.y)
            val maxX = ceil(rect.origin.x + rect.size.width)
            val maxY = ceil(rect.origin.y + rect.size.height)
            return CGRect(x, y, maxX - x, maxY - y)
        }

    fun insetBy(dx: Double, dy: Double): CGRect {
        if (isNull || isInfinite) {
            return this
        }
        val rect = standardized
        val width = rect.size.width - 2 * dx
        val height = rect.size.height - 2 * dy
        if (width < 0 || height < 0) {
            return nullRect
        }
        return CGRect(rect.origin.x + dx, rect.origin.y + dy, width, height)
    }

    fun insetBy(dx: Number, dy: Number): CGRect {
        return insetBy(dx.toDouble(), dy.toDouble())
    }

    fun offsetBy(dx: Double, dy: Double): CGRect {
        if (isNull || isInfinite) {
            return this
        }
        val rect = standardized
        return CGRect(rect.origin.x + dx, rect.origin.y + dy, rect.size.width, rect.size.height)
    }

    fun offsetBy(dx: Number, dy: Number): CGRect {
        return offsetBy(dx.toDouble(), dy.toDouble())
    }

    fun intersection(r2: CGRect): CGRect {
        if (isNull || r2.isNull) {
            return nullRect
        }
        val x1 = max(minX, r2.minX)
        val x2 = min(maxX, r2.maxX)
        if (x1 > x2) {
            return nullRect
        }
        val y1 = max(minY, r2.minY)
        val y2 = min(maxY, r2.maxY)
        if (y1 > y2) {
            return nullRect
        }
        return CGRect(x1, y1, x2 - x1, y2 - y1)
    }

    fun union(r2: CGRect): CGRect {
        if (isNull) {
            return r2.standardized
        }
        if (r2.isNull) {
            return standardized
        }
        val x1 = min(minX, r2.minX)
        val y1 = min(minY, r2.minY)
        val x2 = max(maxX, r2.maxX)
        val y2 = max(maxY, r2.maxY)
        return CGRect(x1, y1, x2 - x1, y2 - y1)
    }

    fun intersects(r2: CGRect): Boolean {
        return !intersection(r2).isNull
    }

    fun contains(point: CGPoint): Boolean {
        if (isNull) {
            return false
        }
        return point.x >= minX && point.x < maxX && point.y >= minY && point.y < maxY
    }

    fun contains(rect2: CGRect): Boolean {
        if (isNull || rect2.isNull) {
            return false
        }
        return rect2.minX >= minX && rect2.maxX <= maxX && rect2.minY >= minY && rect2.maxY <= maxY
    }

    fun applying(t: CGAffineTransform): CGRect {
        if (isNull || isInfinite) {
            return this
        }
        val x0 = minX
        val y0 = minY
        val x1 = maxX
        val y1 = maxY
        val ax = t.a * x0 + t.c * y0 + t.tx
        val ay = t.b * x0 + t.d * y0 + t.ty
        val bx = t.a * x1 + t.c * y0 + t.tx
        val by = t.b * x1 + t.d * y0 + t.ty
        val cx = t.a * x0 + t.c * y1 + t.tx
        val cy = t.b * x0 + t.d * y1 + t.ty
        val dx = t.a * x1 + t.c * y1 + t.tx
        val dy = t.b * x1 + t.d * y1 + t.ty
        val minX = min(min(ax, bx), min(cx, dx))
        val maxX = max(max(ax, bx), max(cx, dx))
        val minY = min(min(ay, by), min(cy, dy))
        val maxY = max(max(ay, by), max(cy, dy))
        return CGRect(minX, minY, maxX - minX, maxY - minY)
    }

    override fun equals(other: Any?): Boolean {
        return other is CGRect &&
            origin.x == other.origin.x &&
            origin.y == other.origin.y &&
            size.width == other.size.width &&
            size.height == other.size.height
    }

    override fun hashCode(): Int {
        return 31 * origin.hashCode() + size.hashCode()
    }

    override fun toString(): String {
        return when {
            isNull -> "CGRect.null"
            isInfinite -> "CGRect.infinite"
            else -> "CGRect(x=${origin.x}, y=${origin.y}, width=${size.width}, height=${size.height})"
        }
    }

    companion object {
        private const val INFINITE_ORIGIN = -Double.MAX_VALUE / 2
        private const val INFINITE_SIZE = Double.MAX_VALUE
        val zero = CGRect(0.0, 0.0, 0.0, 0.0)
        val nullRect = CGRect(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0, 0.0)
        val infinite = CGRect(INFINITE_ORIGIN, INFINITE_ORIGIN, INFINITE_SIZE, INFINITE_SIZE)
    }
}

class CGAffineTransform(
    val a: Double,
    val b: Double,
    val c: Double,
    val d: Double,
    val tx: Double,
    val ty: Double,
) {
    val isIdentity: Boolean
        get() = a == 1.0 && b == 0.0 && c == 0.0 && d == 1.0 && tx == 0.0 && ty == 0.0

    fun translatedBy(x: Double, y: Double): CGAffineTransform {
        return CGAffineTransform(a, b, c, d, a * x + c * y + tx, b * x + d * y + ty)
    }

    fun scaledBy(x: Double, y: Double): CGAffineTransform {
        return CGAffineTransform(a * x, b * x, c * y, d * y, tx, ty)
    }

    fun rotated(by: Double): CGAffineTransform {
        val cosine = cos(by)
        val sine = sin(by)
        return CGAffineTransform(
            a * cosine + c * sine,
            b * cosine + d * sine,
            c * cosine - a * sine,
            d * cosine - b * sine,
            tx,
            ty
        )
    }

    fun concatenating(t2: CGAffineTransform): CGAffineTransform {
        return CGAffineTransform(
            a * t2.a + b * t2.c,
            a * t2.b + b * t2.d,
            c * t2.a + d * t2.c,
            c * t2.b + d * t2.d,
            tx * t2.a + ty * t2.c + t2.tx,
            tx * t2.b + ty * t2.d + t2.ty
        )
    }

    fun inverted(): CGAffineTransform {
        val determinant = a * d - b * c
        if (determinant == 0.0 || determinant.isNaN()) {
            return this
        }
        return CGAffineTransform(
            d / determinant,
            -b / determinant,
            -c / determinant,
            a / determinant,
            (c * ty - d * tx) / determinant,
            (b * tx - a * ty) / determinant
        )
    }

    override fun equals(other: Any?): Boolean {
        return other is CGAffineTransform &&
            a == other.a && b == other.b && c == other.c && d == other.d && tx == other.tx && ty == other.ty
    }

    override fun hashCode(): Int {
        var result = normalizedHash(a)
        for (value in listOf(b, c, d, tx, ty)) {
            result = 31 * result + normalizedHash(value)
        }
        return result
    }

    override fun toString(): String {
        return "CGAffineTransform(a=$a, b=$b, c=$c, d=$d, tx=$tx, ty=$ty)"
    }

    companion object {
        val identity = CGAffineTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)

        operator fun invoke(
            translationX: Double? = null,
            scaleX: Double? = null,
            y: Double? = null,
            rotationAngle: Double? = null,
        ): CGAffineTransform {
            if (translationX != null) {
                return CGAffineTransform(1.0, 0.0, 0.0, 1.0, translationX, y ?: 0.0)
            }
            if (scaleX != null) {
                return CGAffineTransform(scaleX, 0.0, 0.0, y ?: 1.0, 0.0, 0.0)
            }
            if (rotationAngle != null) {
                val cosine = cos(rotationAngle)
                val sine = sin(rotationAngle)
                return CGAffineTransform(cosine, sine, -sine, cosine, 0.0, 0.0)
            }
            return identity
        }
    }
}

enum class CGImagePropertyOrientation(val rawValue: Int) {
    up(1),
    upMirrored(2),
    down(3),
    downMirrored(4),
    leftMirrored(5),
    right(6),
    rightMirrored(7),
    left(8),
    ;

    companion object {
        operator fun invoke(rawValue: Int): CGImagePropertyOrientation? {
            return entries.firstOrNull { it.rawValue == rawValue }
        }
    }
}

fun android.util.Size.toCGSize(): CGSize {
    return CGSize(width.toDouble(), height.toDouble())
}

fun android.util.SizeF.toCGSize(): CGSize {
    return CGSize(width.toDouble(), height.toDouble())
}

fun androidx.compose.ui.geometry.Size.toCGSize(): CGSize {
    return CGSize(width.toDouble(), height.toDouble())
}

fun CGSize.toComposeSize(): androidx.compose.ui.geometry.Size {
    return androidx.compose.ui.geometry.Size(width.toFloat(), height.toFloat())
}

fun CGSize.toSize(): android.util.Size {
    return android.util.Size(width.roundToInt(), height.roundToInt())
}

fun androidx.compose.ui.geometry.Offset.toCGPoint(): CGPoint {
    return CGPoint(x.toDouble(), y.toDouble())
}

fun CGPoint.toOffset(): androidx.compose.ui.geometry.Offset {
    return androidx.compose.ui.geometry.Offset(x.toFloat(), y.toFloat())
}

fun androidx.compose.ui.geometry.Rect.toCGRect(): CGRect {
    return CGRect(left.toDouble(), top.toDouble(), width.toDouble(), height.toDouble())
}

fun CGRect.toComposeRect(): androidx.compose.ui.geometry.Rect {
    return androidx.compose.ui.geometry.Rect(
        minX.toFloat(),
        minY.toFloat(),
        maxX.toFloat(),
        maxY.toFloat()
    )
}

val CVPixelBuffer.cgSize: CGSize
    get() = CGSize(width.toDouble(), height.toDouble())
