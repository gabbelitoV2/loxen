package com.moblin.android.videoeffects.text

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.common.various.RgbColor
import com.moblin.android.common.various.color
import com.moblin.android.localized
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.combine.AnyCancellable
import com.moblin.android.platform.core.ContinuousClock
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.swiftui.ImageRenderer
import com.moblin.android.platform.swiftui.SwiftUIFonts
import com.moblin.android.platform.swiftui.hasSystemImage
import com.moblin.android.platform.swiftui.monospacedDigit
import com.moblin.android.various.Variables
import com.moblin.android.various.settings.SettingsFont
import com.moblin.android.various.settings.SettingsHorizontalAlignment
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidgetTextStopwatch
import com.moblin.android.various.subtitles.Subtitles
import com.moblin.android.videoeffects.EffectImageCgImage
import com.moblin.android.videoeffects.move
import com.moblin.android.videoeffects.moveComposited
import com.moblin.android.videoeffects.toEffectImage
import com.moblin.android.view.utils.FontDesign
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.outlined.MicOff

private class TextViewState(
    fontSize: Float,
    font: SettingsFont,
    fontDesign: FontDesign,
    fontWeight: FontWeight,
    fontMonospacedDigits: Boolean,
    horizontalAlignment: SettingsHorizontalAlignment,
    minWidth: Double,
    cornerRadius: Double,
    foregroundColor: Color,
    backgroundColor: Color,
    lines: List<TextEffectLine>,
) {
    val fontSize = MutableStateFlow(fontSize)
    val font = MutableStateFlow(font)
    val fontDesign = MutableStateFlow(fontDesign)
    val fontWeight = MutableStateFlow(fontWeight)
    val fontMonospacedDigits = MutableStateFlow(fontMonospacedDigits)
    val horizontalAlignment = MutableStateFlow(horizontalAlignment)
    val minWidth = MutableStateFlow(minWidth)
    val cornerRadius = MutableStateFlow(cornerRadius)
    val foregroundColor = MutableStateFlow(foregroundColor)
    val backgroundColor = MutableStateFlow(backgroundColor)
    val size = MutableStateFlow<CGSize?>(null)
    val lines = MutableStateFlow(lines)
}

private fun scaledFontSize(fontSize: Float, size: CGSize): Float {
    return fontSize * (size.maximum() / 1920.0).toFloat()
}

private fun font(
    font: SettingsFont,
    fontDesign: FontDesign,
    fontWeight: FontWeight,
    size: Float,
): TextStyle {
    val name = font.name()
    if (name != null) {
        return SwiftUIFonts.custom(name, size)
    } else {
        return SwiftUIFonts.system(size, fontWeight, fontDesign)
    }
}

@Composable
private fun TextView(state: TextViewState) {
    val size by state.size.collectAsState()
    val fontSize by state.fontSize.collectAsState()
    val settingsFont by state.font.collectAsState()
    val fontDesign by state.fontDesign.collectAsState()
    val fontWeight by state.fontWeight.collectAsState()
    val fontMonospacedDigits by state.fontMonospacedDigits.collectAsState()
    val horizontalAlignment by state.horizontalAlignment.collectAsState()
    val minWidth by state.minWidth.collectAsState()
    val cornerRadius by state.cornerRadius.collectAsState()
    val foregroundColor by state.foregroundColor.collectAsState()
    val backgroundColor by state.backgroundColor.collectAsState()
    val lines by state.lines.collectAsState()
    @Composable fun TextEffectSymbol(name: String, fontSize: androidx.compose.ui.unit.TextUnit, tint: Color) = androidx.compose.material3.Icon(imageVector = when (name) { "checkmark.square" -> Icons.Outlined.CheckBox; "square" -> Icons.Outlined.CheckBoxOutlineBlank; "mic.slash" -> Icons.Outlined.MicOff; else -> com.moblin.android.platform.systemImage(name) }, contentDescription = null, tint = tint, modifier = Modifier.size(with(androidx.compose.ui.platform.LocalDensity.current) { fontSize.toDp() }))
    val currentSize = size ?: return
    val scaledSize = scaledFontSize(fontSize = fontSize, size = currentSize)
    var textStyle = font(
        font = settingsFont,
        fontDesign = fontDesign,
        fontWeight = fontWeight,
        size = scaledSize,
    )
    if (settingsFont.family == null && fontMonospacedDigits) {
        textStyle = textStyle.monospacedDigit()
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = when (horizontalAlignment) {
            SettingsHorizontalAlignment.leading -> Alignment.Start
            SettingsHorizontalAlignment.center -> Alignment.CenterHorizontally
            SettingsHorizontalAlignment.trailing -> Alignment.End
            else -> Alignment.CenterHorizontally
        },
    ) {
        lines.forEach { line ->
            key(line.id) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(cornerRadius.dp))
                        .background(backgroundColor)
                        .widthIn(min = minWidth.dp)
                        .padding(
                            horizontal = (
                                7 * scaledSize / 30 +
                                    minOf((cornerRadius / 5).toFloat(), scaledSize / 7.5f)
                                ).dp,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (horizontalAlignment != SettingsHorizontalAlignment.leading && minWidth != 0.0) {
                        Spacer(Modifier.weight(1f))
                    }
                    line.parts.forEach { part ->
                        key(part.id) {
                            when (val data = part.data) {
                                is TextEffectPartData.Text -> Text(
                                    text = data.text,
                                    style = textStyle,
                                    color = foregroundColor,
                                )
                                is TextEffectPartData.ImageSystemName -> TextEffectSymbol(
                                    name = data.systemName,
                                    fontSize = scaledSize.sp,
                                    tint = foregroundColor,
                                )
                                is TextEffectPartData.ImageSystemNameTryFill -> {
                                    if (hasSystemImage("${data.systemName}.fill")) {
                                        SystemImage(
                                            name = "${data.systemName}.fill",
                                            fontSize = scaledSize.sp,
                                            tint = Color.Unspecified,
                                        )
                                    } else {
                                        SystemImage(
                                            name = data.systemName,
                                            fontSize = scaledSize.sp,
                                            tint = foregroundColor,
                                        )
                                    }
                                }
                                is TextEffectPartData.Rating -> {
                                    for (index in 0 until 5) {
                                        if (index < data.rating) {
                                            Text(
                                                text = localized("★"),
                                                style = textStyle,
                                                color = Color(0xFFFFCC00),
                                            )
                                        } else {
                                            Text(
                                                text = localized("☆"),
                                                style = textStyle,
                                                color = foregroundColor,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (horizontalAlignment != SettingsHorizontalAlignment.trailing && minWidth != 0.0) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

class TextEffect(
    format: String,
    backgroundColor: RgbColor,
    foregroundColor: RgbColor,
    fontSize: Float,
    font: SettingsFont,
    fontDesign: FontDesign,
    fontWeight: FontWeight,
    fontMonospacedDigits: Boolean,
    horizontalAlignment: SettingsHorizontalAlignment,
    width: Int?,
    cornerRadius: Double,
    private val delay: Double,
    timersEndTime: List<ContinuousClock.Instant>,
    stopwatches: List<SettingsWidgetTextStopwatch>,
    checkboxes: List<Boolean>,
    ratings: List<Int>,
    lapTimes: List<List<Double>>,
) : VideoEffect() {
    private var variables: ArrayDeque<Variables> = ArrayDeque()
    private var overlay: EffectImageCgImage? = null
    private var nextUpdateTime: ContinuousClock.Instant = ContinuousClock.now
    private val formatter: TextEffectFormatter = TextEffectFormatter(
        formatParts = loadTextFormat(format = format),
        timersEndTime = timersEndTime,
        stopwatches = stopwatches,
        checkboxes = checkboxes,
        ratings = ratings,
        lapTimes = lapTimes,
    )
    private var sceneWidget: SettingsSceneWidget = SettingsSceneWidget(widgetId = UUID.randomUUID())
    private val state: TextViewState = TextViewState(
        fontSize = fontSize,
        font = font,
        fontDesign = fontDesign,
        fontWeight = fontWeight,
        fontMonospacedDigits = fontMonospacedDigits,
        horizontalAlignment = horizontalAlignment,
        minWidth = (width ?: 0).toDouble(),
        cornerRadius = cornerRadius,
        foregroundColor = foregroundColor.color(),
        backgroundColor = backgroundColor.color(),
        lines = emptyList(),
    )
    private var renderer: ImageRenderer? = null
    private var cancellable: AnyCancellable? = null
    private var forceUpdate: Boolean = false
    private var previousLines: List<TextEffectLine>? = null

    init {
        val self = this
        CoroutineScope(Dispatchers.Main.immediate).launch {
            val state0 = self.state
            self.renderer = ImageRenderer(content = { TextView(state = state0) })
            val weakSelf = java.lang.ref.WeakReference(self)
            self.cancellable = self.renderer?.objectWillChange?.sink {
                weakSelf.get()?.setOverlay(image = weakSelf.get()?.renderer?.cgImage)
            }
            self.setOverlay(image = self.renderer?.cgImage)
        }
    }

    fun setSceneWidget(sceneWidget: SettingsSceneWidget) {
        processorPipelineQueue.launch {
            this@TextEffect.sceneWidget = sceneWidget
            this@TextEffect.forceUpdate = true
        }
        previousLines = null
    }

    fun forceOverlayUpdate() {
        processorPipelineQueue.launch {
            this@TextEffect.forceUpdate = true
        }
        previousLines = null
    }

    fun setFormat(format: String) {
        formatter.formatParts = loadTextFormat(format = format)
        forceOverlayUpdate()
    }

    fun setBackgroundColor(color: RgbColor) {
        state.backgroundColor.value = color.color()
    }

    fun setForegroundColor(color: RgbColor) {
        state.foregroundColor.value = color.color()
    }

    fun setFontSize(size: Float) {
        state.fontSize.value = size
    }

    fun setFont(font: SettingsFont) {
        state.font.value = font
    }

    fun setFontDesign(design: FontDesign) {
        state.fontDesign.value = design
    }

    fun setFontWeight(weight: FontWeight) {
        state.fontWeight.value = weight
    }

    fun setFontMonospacedDigits(enabled: Boolean) {
        state.fontMonospacedDigits.value = enabled
    }

    fun setLayout(alignment: SettingsHorizontalAlignment, width: Int?, cornerRadius: Double) {
        state.horizontalAlignment.value = alignment
        state.minWidth.value = (width ?: 0).toDouble()
        state.cornerRadius.value = cornerRadius
    }

    fun setTimersEndTime(endTimes: List<ContinuousClock.Instant>) {
        formatter.timersEndTime = endTimes
        forceOverlayUpdate()
    }

    fun setEndTime(index: Int, endTime: ContinuousClock.Instant) {
        if (index >= formatter.timersEndTime.size) {
            return
        }
        formatter.timersEndTime = formatter.timersEndTime.toMutableList().also { it[index] = endTime }
        forceOverlayUpdate()
    }

    fun setStopwatches(stopwatches: List<SettingsWidgetTextStopwatch>) {
        formatter.stopwatches = stopwatches
        forceOverlayUpdate()
    }

    fun setStopwatch(index: Int, stopwatch: SettingsWidgetTextStopwatch) {
        if (index >= formatter.stopwatches.size) {
            return
        }
        formatter.stopwatches = formatter.stopwatches.toMutableList().also { it[index] = stopwatch }
        forceOverlayUpdate()
    }

    fun setCheckboxes(checkboxes: List<Boolean>) {
        formatter.checkboxes = checkboxes
        forceOverlayUpdate()
    }

    fun setCheckbox(index: Int, checked: Boolean) {
        if (index >= formatter.checkboxes.size) {
            return
        }
        formatter.checkboxes = formatter.checkboxes.toMutableList().also { it[index] = checked }
        forceOverlayUpdate()
    }

    fun setRatings(ratings: List<Int>) {
        formatter.ratings = ratings
        forceOverlayUpdate()
    }

    fun setRating(index: Int, rating: Int) {
        if (index >= formatter.ratings.size) {
            return
        }
        formatter.ratings = formatter.ratings.toMutableList().also { it[index] = rating }
        forceOverlayUpdate()
    }

    fun setLapTimes(lapTimes: List<List<Double>>) {
        formatter.lapTimes = lapTimes
        forceOverlayUpdate()
    }

    fun setLapTimes(index: Int, lapTimes: List<Double>) {
        if (index >= formatter.lapTimes.size) {
            return
        }
        formatter.lapTimes = formatter.lapTimes.toMutableList().also { it[index] = lapTimes }
        forceOverlayUpdate()
    }

    fun clearSubtitles() {
        formatter.subtitles.clear()
        forceOverlayUpdate()
    }

    fun updateSubtitles(position: Int, text: String, languageIdentifier: String?) {
        val current = formatter.subtitles[languageIdentifier]
        if (current != null) {
            current.updateSubtitles(position = position, text = text)
        } else {
            val subtitles = Subtitles(languageIdentifier = languageIdentifier)
            subtitles.updateSubtitles(position = position, text = text)
            formatter.subtitles[languageIdentifier] = subtitles
        }
        forceOverlayUpdate()
    }

    fun updateVariables(variables: Variables) {
        this.variables.addLast(variables)
        if (this.variables.size > 10) {
            this.variables.removeFirst()
        }
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        updateOverlayIfNeeded(size = image.extent.size)
        return overlay?.getCiImage()
            ?.move(layout = sceneWidget.layout, streamSize = image.extent.size)
            ?.cropped(to = image.extent)
            ?.composited(over = image)
            ?: image
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        updateOverlayIfNeeded(size = image.extent.size)
        return overlay?.getMetalPetalImage()
            ?.moveComposited(layout = sceneWidget.layout, backgroundImage = image)
            ?: image
    }

    override fun prepare(size: CGSize, info: VideoEffectInfo) {
        updateOverlayIfNeeded(size = size)
    }

    private fun formatted(now: ContinuousClock.Instant): List<TextEffectLine> {
        val variables = this.variables.lastOrNull {
            ContinuousClock.Instant(it.timestamp).advanced(bySeconds = delay - 1) <= now
        } ?: this.variables.firstOrNull() ?: return emptyList()
        return formatter.format(variables = variables, now = now)
    }

    private fun updateOverlayIfNeeded(size: CGSize) {
        try {
            val now = ContinuousClock.now
            if (!(now >= nextUpdateTime || forceUpdate)) {
                return
            }
            if (!forceUpdate) {
                nextUpdateTime = nextUpdateTime.advanced(bySeconds = 1.0)
            }
            CoroutineScope(Dispatchers.Main.immediate).launch {
                this@TextEffect.updateOverlayInternal(size = size, now = now)
            }
        } finally {
            forceUpdate = false
        }
    }

    private fun updateOverlayInternal(size: CGSize, now: ContinuousClock.Instant) {
        val lines = formatted(now = now)
        val stateSize = state.size.value
        val sameSize = stateSize != null && size.width == stateSize.width && size.height == stateSize.height
        if (lines == previousLines && sameSize) {
            return
        }
        previousLines = lines
        state.size.value = size
        state.lines.value = lines
    }

    private fun setOverlay(image: android.graphics.Bitmap?) {
        val overlay = image?.toEffectImage()
        processorPipelineQueue.launch {
            this@TextEffect.overlay = overlay
        }
    }
}
