package com.moblin.android.videoeffects.text

import android.graphics.Bitmap
import com.moblin.android.platform.video.CVPixelBuffer as Image
import android.util.Size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.common.various.RgbColor
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.various.Variables
import com.moblin.android.various.settings.SettingsFontDesign
import com.moblin.android.various.settings.SettingsFontWeight
import com.moblin.android.various.settings.SettingsHorizontalAlignment
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidgetTextStopwatch
import com.moblin.android.various.subtitles.Subtitles
import com.moblin.android.videoeffects.EffectImageCgImage
import java.util.ArrayDeque
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private val mainScope = CoroutineScope(Dispatchers.Main)

private fun RgbColor.color(): Color = Color(
    (red / 255.0).toFloat(),
    (green / 255.0).toFloat(),
    (blue / 255.0).toFloat(),
)

private class TextViewState(
    fontSize: Float,
    fontFamily: String?,
    fontStyle: String,
    fontDesign: SettingsFontDesign,
    fontWeight: SettingsFontWeight,
    fontMonospacedDigits: Boolean,
    horizontalAlignment: SettingsHorizontalAlignment,
    minWidth: Double,
    cornerRadius: Double,
    foregroundColor: Color,
    backgroundColor: Color,
    lines: List<TextEffectLine>,
) {
    val fontSize = MutableStateFlow(fontSize)
    val fontFamily = MutableStateFlow(fontFamily)
    val fontStyle = MutableStateFlow(fontStyle)
    val fontDesign = MutableStateFlow(fontDesign)
    val fontWeight = MutableStateFlow(fontWeight)
    val fontMonospacedDigits = MutableStateFlow(fontMonospacedDigits)
    val horizontalAlignment = MutableStateFlow(horizontalAlignment)
    val minWidth = MutableStateFlow(minWidth)
    val cornerRadius = MutableStateFlow(cornerRadius)
    val foregroundColor = MutableStateFlow(foregroundColor)
    val backgroundColor = MutableStateFlow(backgroundColor)
    val size = MutableStateFlow<Size?>(null)
    val lines = MutableStateFlow(lines)
}

@Composable
private fun TextView(state: TextViewState) {
    val size = state.size.collectAsState().value ?: return
    val fontSize = scaledFontSize(state = state, size = size)
    val horizontalAlignment = state.horizontalAlignment.collectAsState().value
    val minWidth = state.minWidth.collectAsState().value
    val cornerRadius = state.cornerRadius.collectAsState().value
    val foregroundColor = state.foregroundColor.collectAsState().value
    val backgroundColor = state.backgroundColor.collectAsState().value
    val fontFamily = state.fontFamily.collectAsState().value
    val fontMonospacedDigits = state.fontMonospacedDigits.collectAsState().value
    val lines = state.lines.collectAsState().value
    val baseTextStyle = font(state = state, size = fontSize)
    val textStyle = if (fontFamily == null && fontMonospacedDigits) {
        baseTextStyle.copy(fontFeatureSettings = "tnum")
    } else {
        baseTextStyle
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = horizontalAlignment.toSystem(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        lines.forEach { line ->
            Row(
                modifier = Modifier
                    .padding(
                        horizontal = (
                            7 * fontSize / 30f +
                                min((cornerRadius / 5).toFloat(), fontSize / 7.5f)
                            ).dp
                    )
                    .widthIn(min = minWidth.dp)
                    .clip(RoundedCornerShape(cornerRadius.dp))
                    .background(backgroundColor),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                if (horizontalAlignment.toSystem() != Alignment.Start && minWidth != 0.0) {
                    Spacer(Modifier.weight(1f))
                }
                line.parts.forEach { part ->
                    when (val data = part.data) {
                        is TextEffectPartData.Text -> {
                            Text(text = data.text, style = textStyle, color = foregroundColor)
                        }
                        is TextEffectPartData.ImageSystemName -> {
                            Icon(
                                imageVector = TODO("no Android counterpart for UIImage(systemName:)"),
                                contentDescription = null,
                                tint = foregroundColor,
                            )
                        }
                        is TextEffectPartData.ImageSystemNameTryFill -> {
                            Icon(
                                imageVector = TODO("no Android counterpart for UIImage(systemName:)"),
                                contentDescription = null,
                                tint = foregroundColor,
                            )
                        }
                        is TextEffectPartData.Rating -> {
                            for (index in 0 until 5) {
                                if (index < data.rating) {
                                    Text(text = "★", style = textStyle, color = Color.Yellow)
                                } else {
                                    Text(text = "☆", style = textStyle, color = foregroundColor)
                                }
                            }
                        }
                    }
                }
                if (horizontalAlignment.toSystem() != Alignment.End && minWidth != 0.0) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private fun scaledFontSize(state: TextViewState, size: Size): Float {
    return state.fontSize.value * (max(size.width, size.height).toFloat() / 1920f)
}

private fun font(state: TextViewState, size: Float): TextStyle {
    return if (state.fontFamily.value != null) {
        TextStyle(
            fontFamily = TODO("no Compose counterpart for iOS custom font family"),
            fontSize = size.sp,
        )
    } else {
        TextStyle(
            fontWeight = TODO("no Compose counterpart for SettingsFontWeight"),
            fontSize = size.sp,
        )
    }
}

class TextEffect(
    format: String,
    backgroundColor: RgbColor,
    foregroundColor: RgbColor,
    fontSize: Float,
    fontFamily: String?,
    fontStyle: String,
    fontDesign: SettingsFontDesign,
    fontWeight: SettingsFontWeight,
    fontMonospacedDigits: Boolean,
    horizontalAlignment: SettingsHorizontalAlignment,
    width: Int?,
    cornerRadius: Double,
    delay: Double,
    timersEndTime: MutableList<com.moblin.android.platform.core.ContinuousClock.Instant>,
    stopwatches: MutableList<SettingsWidgetTextStopwatch>,
    checkboxes: MutableList<Boolean>,
    ratings: MutableList<Int>,
    lapTimes: MutableList<MutableList<Double>>,
) : VideoEffect() {
    private val variables = ArrayDeque<Variables>()
    private var overlay: EffectImageCgImage? = null
    private var nextUpdateTime = com.moblin.android.platform.core.ContinuousClock.now.nanoseconds
    fun setEndTime(index: Int, endTime: Long) = setEndTime(index = index, endTime = com.moblin.android.platform.core.ContinuousClock.Instant(endTime))
    private var delay: Double
    private val formatter: TextEffectFormatter
    private var sceneWidget: SettingsSceneWidget
    private val state: TextViewState
    private var renderer: Any? = null
    private var cancellable: Job? = null
    private var forceUpdate: Boolean = false
    private var previousLines: List<TextEffectLine>? = null

    init {
        formatter = TextEffectFormatter(
            formatParts = loadTextFormat(format = format),
            timersEndTime = timersEndTime,
            stopwatches = stopwatches,
            checkboxes = checkboxes,
            ratings = ratings,
            lapTimes = lapTimes,
        )
        sceneWidget = SettingsSceneWidget(widgetId = UUID.randomUUID())
        state = TextViewState(
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontStyle = fontStyle,
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
        this.delay = delay
        mainScope.launch {
            Unit
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

    fun setFontFamily(family: String?) {
        state.fontFamily.value = family
    }

    fun setFontStyle(style: String) {
        state.fontStyle.value = style
    }

    fun setFontDesign(design: SettingsFontDesign) {
        state.fontDesign.value = design
    }

    fun setFontWeight(weight: SettingsFontWeight) {
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

    fun setTimersEndTime(endTimes: MutableList<com.moblin.android.platform.core.ContinuousClock.Instant>) {
        formatter.timersEndTime = endTimes
        forceOverlayUpdate()
    }

    fun setEndTime(index: Int, endTime: com.moblin.android.platform.core.ContinuousClock.Instant) {
        if (index >= formatter.timersEndTime.size) {
            return
        }
        formatter.timersEndTime = formatter.timersEndTime.toMutableList().also { it[index] = endTime }
        forceOverlayUpdate()
    }

    fun setStopwatches(stopwatches: MutableList<SettingsWidgetTextStopwatch>) {
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

    fun setCheckboxes(checkboxes: MutableList<Boolean>) {
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

    fun setRatings(ratings: MutableList<Int>) {
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

    fun setLapTimes(lapTimes: MutableList<MutableList<Double>>) {
        formatter.lapTimes = lapTimes
        forceOverlayUpdate()
    }

    fun setLapTimes(index: Int, lapTimes: MutableList<Double>) {
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
        val subtitles = formatter.subtitles[languageIdentifier]
        if (subtitles != null) {
            subtitles.updateSubtitles(position = position, text = text)
        } else {
            val newSubtitles = Subtitles(languageIdentifier)
            newSubtitles.updateSubtitles(position = position, text = text)
            formatter.subtitles[languageIdentifier] = newSubtitles
        }
        forceOverlayUpdate()
    }

    fun updateVariables(variables: Variables) {
        this.variables.addLast(variables)
        if (this.variables.size > 10) {
            this.variables.removeFirst()
        }
    }

    override fun execute(image: Image, info: VideoEffectInfo): Image {
        updateOverlayIfNeeded(size = Size(image.width, image.height))
        return TODO("no Android counterpart for CoreImage compositing")
    }

    override fun executeMetalPetal(image: Image, info: VideoEffectInfo): Image {
        updateOverlayIfNeeded(size = Size(image.width, image.height))
        return TODO("no Android counterpart for MetalPetal")
    }

    override fun prepare(size: Size, info: VideoEffectInfo) {
        updateOverlayIfNeeded(size = size)
    }

    private fun formatted(now: Long): List<TextEffectLine> {
        val variables = this.variables
            .lastOrNull { it.timestamp + ((delay - 1) * 1_000_000_000.0).toLong() <= now }
            ?: this.variables.firstOrNull()
            ?: return emptyList()
        return formatter.format(variables = variables, now = com.moblin.android.platform.core.ContinuousClock.Instant(now))
    }

    private fun updateOverlayIfNeeded(size: Size) {
        try {
            val now = com.moblin.android.platform.core.ContinuousClock.now.nanoseconds
            if (now < nextUpdateTime && !forceUpdate) {
                return
            }
            if (!forceUpdate) {
                nextUpdateTime += 1_000_000_000L
            }
            mainScope.launch {
                updateOverlayInternal(size = size, now = now)
            }
        } finally {
            forceUpdate = false
        }
    }

    private fun updateOverlayInternal(size: Size, now: Long) {
        val lines = formatted(now = now)
        if (lines == previousLines && size == state.size.value) {
            return
        }
        previousLines = lines
        state.size.value = size
        state.lines.value = lines
    }

    private fun setOverlay(image: Bitmap?) {
        val overlay = if (image == null) {
            null
        } else {
            TODO()
        }
        processorPipelineQueue.launch {
            this@TextEffect.overlay = overlay
        }
    }
}
