package com.moblin.android.platform.swiftui

import android.content.res.Configuration
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

enum class Glass {
    regular,
    clear,
}

fun Modifier.glassEffect(glass: Glass = Glass.regular, shape: Shape = RoundedCornerShape(percent = 50)): Modifier {
    return this
        .shadow(
            elevation = 6.dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.3f),
            spotColor = Color.Black.copy(alpha = 0.3f),
        )
        .clip(shape)
        .then(GlassEffectElement(glass = glass, shape = shape))
}

private data class GlassEffectElement(val glass: Glass, val shape: Shape) : ModifierNodeElement<GlassEffectNode>() {
    override fun create(): GlassEffectNode {
        return GlassEffectNode(glass = glass, shape = shape)
    }

    override fun update(node: GlassEffectNode) {
        node.glass = glass
        node.shape = shape
        node.invalidateOutline()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "glassEffect"
        properties["glass"] = glass
        properties["shape"] = shape
    }
}

private class GlassEffectNode(var glass: Glass, var shape: Shape) :
    Modifier.Node(),
    DrawModifierNode,
    CompositionLocalConsumerModifierNode {
    private var outline: Outline? = null
    private var outlineSize = Size.Unspecified
    private var outlineDirection: LayoutDirection? = null

    fun invalidateOutline() {
        outline = null
    }

    override fun ContentDrawScope.draw() {
        val isDark = (currentValueOf(LocalConfiguration).uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val outline = outline(size, layoutDirection)
        val fill = when {
            glass == Glass.clear -> listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.04f))
            isDark -> listOf(Color(0xFF3A3A3C).copy(alpha = 0.62f), Color(0xFF1C1C1E).copy(alpha = 0.55f))
            else -> listOf(Color.White.copy(alpha = 0.72f), Color(0xFFF2F2F7).copy(alpha = 0.60f))
        }
        drawOutline(outline, Brush.verticalGradient(fill))
        drawOutline(
            outline,
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = if (isDark) 0.10f else 0.35f),
                    Color.Transparent,
                ),
                endY = size.height * 0.55f,
            ),
        )
        drawContent()
        drawOutline(
            outline,
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = if (isDark) 0.45f else 0.85f),
                    Color.White.copy(alpha = if (isDark) 0.10f else 0.30f),
                ),
            ),
            style = Stroke(width = 1.dp.toPx()),
        )
    }

    private fun ContentDrawScope.outline(size: Size, direction: LayoutDirection): Outline {
        val cached = outline
        if (cached != null && outlineSize == size && outlineDirection == direction) {
            return cached
        }
        val created = shape.createOutline(size, direction, this)
        outline = created
        outlineSize = size
        outlineDirection = direction
        return created
    }
}
