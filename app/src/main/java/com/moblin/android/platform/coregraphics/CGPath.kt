package com.moblin.android.platform.coregraphics

import android.graphics.Path
import android.graphics.RectF

open class CGPath : Path() {
    val isEmptyPath: Boolean
        get() = isEmpty

    val boundingBox: CGRect
        get() {
            if (isEmpty) {
                return CGRect.nullRect
            }
            val bounds = RectF()
            @Suppress("DEPRECATION")
            computeBounds(bounds, true)
            return CGRect(
                bounds.left.toDouble(),
                bounds.top.toDouble(),
                (bounds.right - bounds.left).toDouble(),
                (bounds.bottom - bounds.top).toDouble()
            )
        }
}

class CGMutablePath : CGPath() {
    fun move(to: CGPoint) {
        moveTo(to.x.toFloat(), to.y.toFloat())
    }

    fun addLine(to: CGPoint) {
        lineTo(to.x.toFloat(), to.y.toFloat())
    }

    fun addLines(between: List<CGPoint>) {
        if (between.isEmpty()) {
            return
        }
        move(to = between[0])
        for (point in between.drop(1)) {
            addLine(to = point)
        }
    }

    fun addCurve(to: CGPoint, control1: CGPoint, control2: CGPoint) {
        cubicTo(
            control1.x.toFloat(),
            control1.y.toFloat(),
            control2.x.toFloat(),
            control2.y.toFloat(),
            to.x.toFloat(),
            to.y.toFloat()
        )
    }

    fun addQuadCurve(to: CGPoint, control: CGPoint) {
        quadTo(control.x.toFloat(), control.y.toFloat(), to.x.toFloat(), to.y.toFloat())
    }

    fun addRect(rect: CGRect) {
        if (rect.isNull) {
            return
        }
        addRect(
            rect.minX.toFloat(),
            rect.minY.toFloat(),
            rect.maxX.toFloat(),
            rect.maxY.toFloat(),
            Direction.CW
        )
    }

    fun addEllipse(`in`: CGRect) {
        if (`in`.isNull) {
            return
        }
        addOval(
            `in`.minX.toFloat(),
            `in`.minY.toFloat(),
            `in`.maxX.toFloat(),
            `in`.maxY.toFloat(),
            Direction.CW
        )
    }

    fun addPath(path: CGPath) {
        addPath(path as Path)
    }

    fun closeSubpath() {
        close()
    }
}
