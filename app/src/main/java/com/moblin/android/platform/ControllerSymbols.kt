package com.moblin.android.platform

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private const val outlineWidth = 1.8f

private class Pen(
    private val path: PathBuilder,
    private val left: Float,
    private val top: Float,
    private val width: Float,
    private val height: Float,
) {
    private fun x(u: Float) = left + u * width

    private fun y(v: Float) = top + v * height

    fun move(u: Float, v: Float) {
        path.moveTo(x(u), y(v))
    }

    fun line(u: Float, v: Float) {
        path.lineTo(x(u), y(v))
    }

    fun curve(u1: Float, v1: Float, u2: Float, v2: Float, u3: Float, v3: Float) {
        path.curveTo(x(u1), y(v1), x(u2), y(v2), x(u3), y(v3))
    }
}

private val glyphWidths = mapOf(
    'A' to 0.9f,
    'B' to 0.7f,
    'X' to 0.8f,
    'Y' to 0.84f,
    'L' to 0.6f,
    'R' to 0.72f,
    'Z' to 0.72f,
    '1' to 0.45f,
    '2' to 0.7f,
)

private fun Pen.glyph(character: Char) {
    when (character) {
        'A' -> {
            move(0f, 1f)
            line(0.5f, 0f)
            line(1f, 1f)
            move(0.18f, 0.64f)
            line(0.82f, 0.64f)
        }
        'B' -> {
            move(0f, 0.48f)
            line(0.58f, 0.48f)
            curve(1f, 0.48f, 1f, 1f, 0.58f, 1f)
            line(0f, 1f)
            line(0f, 0f)
            line(0.54f, 0f)
            curve(0.92f, 0f, 0.92f, 0.48f, 0.54f, 0.48f)
        }
        'X' -> {
            move(0f, 0f)
            line(1f, 1f)
            move(1f, 0f)
            line(0f, 1f)
        }
        'Y' -> {
            move(0f, 0f)
            line(0.5f, 0.5f)
            line(1f, 0f)
            move(0.5f, 0.5f)
            line(0.5f, 1f)
        }
        'L' -> {
            move(0f, 0f)
            line(0f, 1f)
            line(1f, 1f)
        }
        'R' -> {
            move(0f, 1f)
            line(0f, 0f)
            line(0.58f, 0f)
            curve(1f, 0f, 1f, 0.52f, 0.58f, 0.52f)
            line(0f, 0.52f)
            move(0.52f, 0.52f)
            line(1f, 1f)
        }
        'Z' -> {
            move(0f, 0f)
            line(1f, 0f)
            line(0f, 1f)
            line(1f, 1f)
        }
        '1' -> {
            move(0f, 0.24f)
            line(0.75f, 0f)
            line(0.75f, 1f)
        }
        '2' -> {
            move(0.02f, 0.26f)
            curve(0.08f, 0.06f, 0.28f, 0f, 0.5f, 0f)
            curve(0.78f, 0f, 0.98f, 0.15f, 0.98f, 0.34f)
            curve(0.98f, 0.56f, 0.62f, 0.7f, 0f, 1f)
            line(1f, 1f)
        }
    }
}

private fun ImageVector.Builder.outline(
    width: Float = outlineWidth,
    filled: Boolean = false,
    pathBuilder: PathBuilder.() -> Unit,
) {
    path(
        fill = if (filled) SolidColor(Color.Black) else null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = pathBuilder,
    )
}

private fun ImageVector.Builder.text(text: String, centerX: Float, centerY: Float, height: Float, width: Float) {
    val widths = text.map { glyphWidths.getValue(it) * height }
    val spacing = width + 0.8f
    var left = centerX - (widths.sum() + spacing * (text.length - 1)) / 2
    outline(width = width) {
        text.forEachIndexed { index, character ->
            Pen(this, left, centerY - height / 2, widths[index], height).glyph(character)
            left += widths[index] + spacing
        }
    }
}

private fun PathBuilder.circle(centerX: Float, centerY: Float, radius: Float) {
    moveTo(centerX - radius, centerY)
    arcToRelative(radius, radius, 0f, true, true, radius * 2, 0f)
    arcToRelative(radius, radius, 0f, true, true, -radius * 2, 0f)
    close()
}

private fun PathBuilder.roundedRectangle(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    topRadius: Float,
    bottomRadius: Float,
) {
    moveTo(left + topRadius, top)
    lineTo(right - topRadius, top)
    arcTo(topRadius, topRadius, 0f, false, true, right, top + topRadius)
    lineTo(right, bottom - bottomRadius)
    arcTo(bottomRadius, bottomRadius, 0f, false, true, right - bottomRadius, bottom)
    lineTo(left + bottomRadius, bottom)
    arcTo(bottomRadius, bottomRadius, 0f, false, true, left, bottom - bottomRadius)
    lineTo(left, top + topRadius)
    arcTo(topRadius, topRadius, 0f, false, true, left + topRadius, top)
    close()
}

private fun symbol(name: String, content: ImageVector.Builder.() -> Unit): ImageVector {
    return ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(content).build()
}

private fun letterCircle(name: String, letter: String): Pair<String, ImageVector> {
    return name to symbol(name) {
        outline { circle(12f, 12f, 9f) }
        text(letter, 12f, 12f, 8.4f, outlineWidth)
    }
}

private fun shapeCircle(name: String, shape: PathBuilder.() -> Unit): Pair<String, ImageVector> {
    return name to symbol(name) {
        outline { circle(12f, 12f, 9f) }
        outline(pathBuilder = shape)
    }
}

private fun shoulderButton(name: String, label: String, roundedTop: Boolean): Pair<String, ImageVector> {
    return name to symbol(name) {
        outline {
            roundedRectangle(3f, 5.5f, 21f, 18.5f, if (roundedTop) 6f else 2f, if (roundedTop) 2f else 6f)
        }
        text(label, 12f, if (roundedTop) 12.4f else 11.6f, 6.6f, 1.5f)
    }
}

private fun dpad(name: String, rotation: Float): Pair<String, ImageVector> {
    return name to symbol(name) {
        outline {
            moveTo(8.7f, 2.8f)
            lineTo(15.3f, 2.8f)
            lineTo(15.3f, 8.7f)
            lineTo(21.2f, 8.7f)
            lineTo(21.2f, 15.3f)
            lineTo(15.3f, 15.3f)
            lineTo(15.3f, 21.2f)
            lineTo(8.7f, 21.2f)
            lineTo(8.7f, 15.3f)
            lineTo(2.8f, 15.3f)
            lineTo(2.8f, 8.7f)
            lineTo(8.7f, 8.7f)
            close()
        }
        group(rotate = rotation, pivotX = 12f, pivotY = 12f) {
            outline(filled = true) {
                moveTo(8.7f, 2.8f)
                lineTo(15.3f, 2.8f)
                lineTo(15.3f, 8.7f)
                lineTo(8.7f, 8.7f)
                close()
            }
        }
    }
}

internal val controllerSymbols: Map<String, ImageVector> = mapOf(
    letterCircle("a.circle", "A"),
    letterCircle("b.circle", "B"),
    letterCircle("x.circle", "X"),
    letterCircle("y.circle", "Y"),
    shapeCircle("circle.circle") { circle(12f, 12f, 4.4f) },
    shapeCircle("square.circle") {
        moveTo(8.4f, 8.4f)
        lineTo(15.6f, 8.4f)
        lineTo(15.6f, 15.6f)
        lineTo(8.4f, 15.6f)
        close()
    },
    shapeCircle("triangle.circle") {
        moveTo(12f, 7.6f)
        lineTo(16.4f, 15.2f)
        lineTo(7.6f, 15.2f)
        close()
    },
    shapeCircle("xmark.circle") {
        moveTo(8.6f, 8.6f)
        lineTo(15.4f, 15.4f)
        moveTo(15.4f, 8.6f)
        lineTo(8.6f, 15.4f)
    },
    dpad("dpad.up.fill", 0f),
    dpad("dpad.right.fill", 90f),
    dpad("dpad.down.fill", 180f),
    dpad("dpad.left.fill", 270f),
    shoulderButton("l.rectangle.roundedbottom", "L", roundedTop = false),
    shoulderButton("r.rectangle.roundedbottom", "R", roundedTop = false),
    shoulderButton("l1.rectangle.roundedbottom", "L1", roundedTop = false),
    shoulderButton("r1.rectangle.roundedbottom", "R1", roundedTop = false),
    shoulderButton("l2.rectangle.roundedtop", "L2", roundedTop = true),
    shoulderButton("r2.rectangle.roundedtop", "R2", roundedTop = true),
    shoulderButton("zl.rectangle.roundedtop", "ZL", roundedTop = true),
    shoulderButton("zr.rectangle.roundedtop", "ZR", roundedTop = true),
)
