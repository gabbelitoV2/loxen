package com.moblin.android.platform.swiftui.layout

import androidx.compose.ui.graphics.Color
import kotlin.math.ceil
import kotlin.math.max
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

private class FakeMeasurer : TextMeasurer {
    override val pixelLength = 1.0

    override fun fontMetrics(font: ResolvedFont): FontMetricsInfo = FontMetricsInfo(
        ascender = 0.95 * font.size,
        descender = -0.25 * font.size,
        leading = 0.0,
        capHeight = 0.7 * font.size,
    )

    private fun charWidth(font: ResolvedFont, scale: Double): Double = 0.5 * font.size * scale

    override fun measure(
        text: String,
        font: ResolvedFont,
        scale: Double,
        width: Double?,
        maxLines: Int?,
        ellipsize: Boolean,
    ): TextMeasure {
        val advance = charWidth(font, scale)
        val lineHeight = 1.2 * font.size * scale
        val lines = mutableListOf<String>()
        if (width == null || text.length * advance <= width + 0.01) {
            lines.add(text)
        } else {
            val perLine = max(1, (width / advance + 0.001).toInt())
            var current = ""
            for (word in text.split(" ")) {
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (candidate.length <= perLine) {
                    current = candidate
                    continue
                }
                if (current.isNotEmpty()) {
                    lines.add(current)
                }
                current = word
                while (current.length > perLine) {
                    lines.add(current.take(perLine))
                    current = current.drop(perLine)
                }
            }
            lines.add(current)
        }
        var truncated = false
        var shown = lines.toList()
        if (maxLines != null && shown.size > maxLines) {
            shown = shown.take(maxLines)
            truncated = true
        }
        var lineWidth = shown.maxOf { it.length * advance }
        if (width != null && lineWidth > width) {
            lineWidth = width
            truncated = true
        }
        return TextMeasure(
            width = lineWidth,
            height = shown.size * lineHeight,
            lineCount = shown.size,
            truncated = truncated,
            firstBaseline = 0.95 * font.size * scale,
            lastBaseline = (shown.size - 1) * lineHeight + 0.95 * font.size * scale,
        )
    }
}

private object FakeImages : ImageProvider {
    override fun asset(name: String): ImageSource? = ImageSource.Bitmap(Any(), 40.0, 20.0)

    override fun symbol(name: String, pointSize: Double): ImageSource? = ImageSource.Symbol(Any(), pointSize)
}

private class Frame(val x: Double, val y: Double, val width: Double, val height: Double)

private class Recorder : DrawContext {
    val texts = mutableMapOf<String, Frame>()
    val scales = mutableMapOf<String, Double>()
    val shapes = mutableListOf<Pair<ShapePaint, Frame>>()
    val images = mutableListOf<Frame>()

    override fun drawShape(kind: ShapeKind, paint: ShapePaint, x: Double, y: Double, width: Double, height: Double) {
        shapes.add(paint to Frame(x, y, width, height))
    }

    override fun drawText(request: TextDrawRequest) {
        texts[request.text] = Frame(request.x, request.y, request.width, request.height)
        scales[request.text] = request.scale
    }

    override fun drawImage(source: ImageSource, tint: Color?, x: Double, y: Double, width: Double, height: Double) {
        images.add(Frame(x, y, width, height))
    }

    override fun drawPie(sectors: List<Pair<Double, Color>>, x: Double, y: Double, width: Double, height: Double) {}

    override fun save() {}

    override fun restore() {}

    override fun translate(dx: Double, dy: Double) {}

    override fun rotate(degrees: Double, pivotX: Double, pivotY: Double) {}

    override fun beginOpacity(alpha: Double) {}

    override fun endOpacity() {}

    override fun beginClip(kind: ShapeKind, x: Double, y: Double, width: Double, height: Double) {}

    override fun endClip() {}

    fun fill(color: Color): Frame = shapes.first { (it.first as? ShapePaint.Fill)?.color == color }.second
}

private class Rendered(val size: LayoutSize, val recorder: Recorder)

private fun render(content: ViewBuilder.() -> Unit): Rendered {
    val node = singleView(buildViews(content)).resolve(Environment(), ResolveContext(FakeMeasurer(), FakeImages))
    val size = node.size(Proposal.unspecified)
    node.place(0.0, 0.0, size, Proposal.unspecified)
    val recorder = Recorder()
    node.draw(recorder)
    return Rendered(size, recorder)
}

private fun assertClose(expected: Double, actual: Double, message: String? = null) {
    assertTrue(kotlin.math.abs(expected - actual) < 1e-6, "${message ?: ""} expected $expected, got $actual")
}

private val red = Color(0xFFFF0000)
private val green = Color(0xFF00FF00)
private val blue = Color(0xFF0000FF)

class SwiftUILayoutSuite {
    @Test
    fun spacersHaveNoSpacingAndDefaultMinimumLength() {
        val rendered = render {
            HStack {
                Text("ab")
                Spacer()
                Text("cd")
            }
        }
        assertClose(17.0 + 8 + 17.0, rendered.size.width)
        assertClose(17.0 + 8, rendered.recorder.texts["cd"]!!.x)
    }

    @Test
    fun spacerWithZeroMinimumLengthCollapses() {
        val rendered = render {
            HStack {
                Text("ab")
                Spacer(minLength = 0.0)
                Text("cd")
            }
        }
        assertClose(34.0, rendered.size.width)
    }

    @Test
    fun defaultSpacingBetweenViewsIsEight() {
        val rendered = render {
            VStack {
                ColorView(red).frame(width = 10.0, height = 10.0)
                ColorView(green).frame(width = 10.0, height = 10.0)
            }
        }
        assertClose(28.0, rendered.size.height)
        assertClose(18.0, rendered.recorder.fill(green).y)
    }

    @Test
    fun textToTextSpacingIsTheFontLeading() {
        val rendered = render {
            VStack {
                Text("a")
                Text("b")
            }
        }
        assertClose(2 * 1.2 * 17, rendered.size.height)
    }

    @Test
    fun textToViewSpacingUsesTextMetrics() {
        val rendered = render {
            VStack {
                Text("a").font(Font.system(size = 10.0))
                ColorView(red).frame(width = 10.0, height = 10.0)
            }
        }
        assertClose(12.0 + 7.0 + 10.0, rendered.size.height)
    }

    @Test
    fun explicitSpacingAppliesAroundSpacers() {
        val rendered = render {
            HStack(spacing = 5.0) {
                Text("ab")
                Spacer(minLength = 0.0)
                Text("cd")
            }
        }
        assertClose(34.0 + 10.0, rendered.size.width)
    }

    @Test
    fun verticalStackProposesItsWidthWhenPlacing() {
        val rendered = render {
            VStack(alignment = HorizontalAlignment.leading, spacing = 0.0) {
                HStack {
                    Text("x")
                    Spacer()
                }
                    .background(red)
                Text("a long text")
            }
        }
        val width = 11 * 8.5
        assertClose(width, rendered.size.width)
        assertClose(width, rendered.recorder.fill(red).width)
        assertClose(0.0, rendered.recorder.texts["x"]!!.x)
    }

    @Test
    fun horizontalStackProposesItsHeightWhenPlacing() {
        val rendered = render {
            HStack(spacing = 0.0) {
                VStack {
                    Spacer(minLength = 0.0)
                    Text("n")
                    Spacer(minLength = 0.0)
                }
                ColorView(red).frame(width = 10.0, height = 100.0)
            }
        }
        assertClose(100.0, rendered.size.height)
        assertClose((100.0 - 1.2 * 17) / 2, rendered.recorder.texts["n"]!!.y)
    }

    @Test
    fun flexibleFrameTakesRemainingWidthAfterFixedSiblings() {
        val rendered = render {
            HStack(spacing = 0.0) {
                ColorView(red).frame(width = 20.0)
                Text("ab")
                    .frame(maxWidth = Double.POSITIVE_INFINITY, alignment = Alignment.trailing)
                    .background(green)
                ColorView(blue).frame(width = 30.0)
            }
                .frame(width = 200.0, height = 40.0)
        }
        assertClose(150.0, rendered.recorder.fill(green).width)
        assertClose(20.0, rendered.recorder.fill(green).x)
        assertClose(170.0 - 17.0, rendered.recorder.texts["ab"]!!.x)
        assertClose(170.0, rendered.recorder.fill(blue).x)
    }

    @Test
    fun spacersAreSizedAfterOtherChildren() {
        val rendered = render {
            HStack {
                Spacer()
                Text("abcdef")
                Spacer()
            }
                .frame(width = 100.0)
        }
        val text = rendered.recorder.texts["abcdef"]!!
        assertClose(51.0, text.width)
        assertClose((100.0 - 51.0) / 2, text.x)
    }

    @Test
    fun minimumScaleFactorFitsExactly() {
        val rendered = render {
            Text("abcdefghij")
                .font(Font.system(size = 10.0))
                .lineLimit(1)
                .minimumScaleFactor(0.5)
                .frame(width = 40.0)
        }
        assertTrue(kotlin.math.abs(0.8 - rendered.recorder.scales["abcdefghij"]!!) < 0.01)
        assertTrue(rendered.recorder.texts["abcdefghij"]!!.width <= 40.0 + 0.01)
    }

    @Test
    fun minimumScaleFactorStopsAtTheMinimum() {
        val rendered = render {
            Text("abcdefghij")
                .font(Font.system(size = 10.0))
                .lineLimit(1)
                .minimumScaleFactor(0.5)
                .frame(width = 10.0)
        }
        assertClose(0.5, rendered.recorder.scales["abcdefghij"]!!)
    }

    @Test
    fun paddingResetsTextSpacingOnPaddedEdges() {
        val rendered = render {
            VStack {
                Text("a").font(Font.system(size = 10.0)).padding(EdgeSet.bottom, 1.0)
                ColorView(red).frame(width = 10.0, height = 10.0)
            }
        }
        assertClose(12.0 + 1.0 + 8.0 + 10.0, rendered.size.height)
    }

    @Test
    fun zStackProposesItsSizeToChildren() {
        val rendered = render {
            ZStack {
                ColorView(red)
                Text("abc")
            }
        }
        assertClose(max(10.0, 25.5), rendered.size.width)
        assertClose(max(10.0, 1.2 * 17), rendered.size.height)
        assertClose(rendered.size.width, rendered.recorder.fill(red).width)
    }

    @Test
    fun overlayGetsTheContentSize() {
        val rendered = render {
            Text("abc")
                .frame(width = 50.0, height = 20.0)
                .overlay(Rectangle().stroke(red, lineWidth = 0.5))
        }
        val stroke = rendered.recorder.shapes.first { it.first is ShapePaint.Stroke }.second
        assertClose(50.0, stroke.width)
        assertClose(20.0, stroke.height)
        assertEquals(1, rendered.recorder.shapes.size)
    }

    @Test
    fun clipShapeArgumentIsNotAView() {
        val rendered = render {
            VStack {
                ColorView(red).frame(width = 10.0, height = 10.0)
            }
                .clipShape(RoundedRectangle(cornerRadius = 3.0))
        }
        assertEquals(1, rendered.recorder.shapes.size)
        assertClose(10.0, rendered.size.height)
    }

    @Test
    fun resizableImageFitsTheProposedHeight() {
        val rendered = render {
            HStack(spacing = 0.0) {
                Image("VolleyballIndicator")
                    .resizable()
                    .scaledToFit()
                    .padding(2.0)
                Text("ab")
                    .frame(maxWidth = Double.POSITIVE_INFINITY)
            }
                .frame(width = 200.0, height = 30.0)
        }
        val image = rendered.recorder.images.single()
        assertClose(26.0, image.height)
        assertClose(52.0, image.width)
    }

    @Test
    fun genericScoreboardNamesAreCenteredOnTheScores() {
        val scale = 1.0
        val rendered = render {
            VStack(alignment = HorizontalAlignment.leading, spacing = 0.0) {
                HStack(spacing = 6 * scale) {
                    VStack(alignment = HorizontalAlignment.leading) {
                        VStack(alignment = HorizontalAlignment.leading) {
                            Spacer(minLength = 0.0)
                            Text("HOME")
                            Spacer(minLength = 0.0)
                        }
                        VStack(alignment = HorizontalAlignment.leading) {
                            Spacer(minLength = 0.0)
                            Text("AWAY")
                            Spacer(minLength = 0.0)
                        }
                    }
                        .font(Font.system(size = 25 * scale))
                    Spacer()
                    VStack {
                        VStack {
                            Spacer(minLength = 0.0)
                            Text("1")
                            Spacer(minLength = 0.0)
                        }
                        VStack {
                            Spacer(minLength = 0.0)
                            Text("2")
                            Spacer(minLength = 0.0)
                        }
                    }
                        .font(Font.system(size = 37 * scale))
                        .frame(width = 37 * 1.33 * scale)
                }
            }
        }
        val rowHeight = 2 * 1.2 * 37
        assertClose(rowHeight, rendered.size.height)
        val home = rendered.recorder.texts["HOME"]!!
        val one = rendered.recorder.texts["1"]!!
        assertClose(one.y + one.height / 2, home.y + home.height / 2)
        assertClose(4 * 12.5 + 6 + 8 + 6 + 37 * 1.33, rendered.size.width)
    }

    @Test
    fun emptyTextHasNoLineHeight() {
        val rendered = render {
            VStack(spacing = 0.0) {
                Text("")
                ColorView(red).frame(width = 10.0, height = 10.0)
            }
        }
        assertClose(11.0, rendered.size.height)
        assertClose(10.0, rendered.size.width)
        assertTrue("" !in rendered.recorder.texts)
    }

    @Test
    fun textHasNoMinimumWidthInStacks() {
        val view = buildViews { Text("abc").opacity(0.5) }.single()
        val node = view.resolve(Environment(), ResolveContext(FakeMeasurer(), FakeImages))
        assertClose(0.0, node.lengthThatFits(Proposal(0.0, 30.0), Axis.horizontal))
        assertClose(0.0, node.size(Proposal.zero).width)
        assertClose(0.0, node.size(Proposal.zero).height)
        assertClose(25.5, node.lengthThatFits(Proposal(null, 30.0), Axis.horizontal))
    }

    @Test
    fun backgroundContentWithSeveralViewsIsAZStack() {
        val rendered = render {
            ColorView(red)
                .frame(width = 20.0, height = 20.0)
                .background(alignment = Alignment.bottomTrailing) {
                    ColorView(green).frame(width = 10.0, height = 10.0)
                    ColorView(blue).frame(width = 4.0, height = 4.0)
                }
        }
        assertClose(10.0, rendered.recorder.fill(green).x)
        assertClose(10.0, rendered.recorder.fill(green).y)
        assertClose(16.0, rendered.recorder.fill(blue).x)
        assertClose(16.0, rendered.recorder.fill(blue).y)
    }

    @Test
    fun imageRendererSizeRoundsUp() {
        val rendered = render {
            Text("abc").font(Font.system(size = 11.0))
        }
        assertEquals(ceil(3 * 5.5).toInt(), ceil(rendered.size.width).toInt())
    }
}
