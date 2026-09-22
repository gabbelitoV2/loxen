package com.moblin.android.videoeffects

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidgetWheelOfLuck
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val mainScope = CoroutineScope(Dispatchers.Main)

private const val wheelSize = 400.0
private val optionColors: List<Color> = listOf(
    Color.Blue,
    Color.Red,
    Color.Yellow,
    Color.Green,
    Color(0xFFFFC0CB),
    Color.Cyan,
)

data class WheelOfLuckEffectOption(
    val id: Int,
    val weight: Int,
    val text: String,
    val textAngle: Double,
)

@Composable
private fun WheelView(size: Double, options: List<WheelOfLuckEffectOption>) {
    val offset = size / 3.4
    val font = size / 10
    TODO("no Android counterpart for the Swift Charts SectorMark based wheel rendering")
}

@Composable
private fun ArrowView(size: Double) {
    TODO("no Android counterpart for the SF Symbol location.north.fill arrow rendering")
}

class WheelOfLuckEffect(private val canvasSize: Size) : VideoEffect() {
    private var wheel: EffectImageCgImage? = null
    private var arrow: EffectImageCgImage? = null
    private var startPresentationTimeStamp: Double = Double.POSITIVE_INFINITY
    private var previousPresentationTimeStamp: Double = 0.0
    private var speed: Double = 0.0
    private var angle: Double = 0.0
    private val spinTime: Double = 12.0
    private var sceneWidget: SettingsSceneWidget = SettingsSceneWidget(widgetId = UUID.randomUUID())

    fun setSceneWidget(sceneWidget: SettingsSceneWidget) {
        processorPipelineQueue.launch {
            this@WheelOfLuckEffect.sceneWidget = sceneWidget
        }
    }

    fun setSettings(settings: SettingsWidgetWheelOfLuck) {
        val options = mutableListOf<WheelOfLuckEffectOption>()
        val totalWeight = settings.totalWeight.toDouble()
        var angle = 0.0
        for ((index, inputOption) in settings.options.withIndex()) {
            val ratio = inputOption.weight.toDouble() / totalWeight
            val textAngle = angle + ratio * 360 / 2 - 90
            angle += ratio * 360
            options.add(
                WheelOfLuckEffectOption(
                    id = index,
                    weight = inputOption.weight,
                    text = inputOption.text,
                    textAngle = textAngle,
                )
            )
        }
        val size = wheelSize * (canvasSize.width.toDouble() / 1920)
        render(size, options)
    }

    fun spin() {
        processorPipelineQueue.launch {
            this@WheelOfLuckEffect.speed = Random.nextDouble(8.0, 15.0)
            this@WheelOfLuckEffect.startPresentationTimeStamp = Double.NaN
        }
    }

    override fun execute(image: Bitmap, info: VideoEffectInfo): Bitmap {
        if (wheel == null || arrow == null) {
            return image
        }
        updateAngle(info.presentationTimeStamp / 1_000_000.0)
        return TODO("OpenGL ES port: Core Image wheel and arrow compositing")
    }

    override fun executeMetalPetal(image: Bitmap, info: VideoEffectInfo): Bitmap {
        if (wheel == null || arrow == null) {
            return image
        }
        updateAngle(info.presentationTimeStamp / 1_000_000.0)
        return TODO("OpenGL ES port: MetalPetal multilayer compositing")
    }

    private fun updateAngle(presentationTimeStamp: Double) {
        if (startPresentationTimeStamp.isInfinite()) {
            return
        } else if (startPresentationTimeStamp.isNaN()) {
            startPresentationTimeStamp = presentationTimeStamp
            previousPresentationTimeStamp = presentationTimeStamp
        }
        val elapsedSinceStart = presentationTimeStamp - startPresentationTimeStamp
        val ratio = maxOf(1 - elapsedSinceStart / spinTime, 0.0)
        angle += -speed * ratio * (presentationTimeStamp - previousPresentationTimeStamp)
        previousPresentationTimeStamp = presentationTimeStamp
    }

    private fun render(size: Double, options: List<WheelOfLuckEffectOption>) {
        mainScope.launch {
            val wheel = renderWheel(size, options)
            val arrow = renderArrow(size)
            processorPipelineQueue.launch {
                this@WheelOfLuckEffect.wheel = wheel
                this@WheelOfLuckEffect.arrow = arrow
            }
        }
    }

    private fun renderWheel(size: Double, options: List<WheelOfLuckEffectOption>): EffectImageCgImage? =
        TODO("no Android counterpart for SwiftUI ImageRenderer wheel rendering")

    private fun renderArrow(size: Double): EffectImageCgImage? =
        TODO("no Android counterpart for SwiftUI ImageRenderer arrow rendering")
}
