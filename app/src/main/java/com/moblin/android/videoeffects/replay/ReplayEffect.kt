package com.moblin.android.videoeffects.replay

import android.util.Size
import com.moblin.android.localized
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.coreimage.CIFilter
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIBlendFilter
import com.moblin.android.platform.metalpetal.MTIBlendMode
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.various.ReplayBufferFile
import com.moblin.android.various.settings.SettingsWidgetLayout
import com.moblin.android.videoeffects.EffectImageCiImage
import com.moblin.android.videoeffects.move
import com.moblin.android.videoeffects.resizeMirror
import com.moblin.android.videoeffects.resizeMirrorMoveComposited
import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import kotlin.math.ceil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import com.moblin.android.videoeffects.MetalPetalWidgetShape

private const val fadeTransitionLength = 0.5

val replayEffectQueue = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

sealed class ReplayEffectTransitionMode {
    object Fade : ReplayEffectTransitionMode()

    data class Stingers(
        val inPath: String,
        val inTransitionPoint: Double,
        val outPath: String,
        val outTransitionPoint: Double,
    ) : ReplayEffectTransitionMode()

    object None : ReplayEffectTransitionMode()
}

interface ReplayEffectDelegate {
    fun replayEffectStatus(timeLeft: Int)
    fun replayEffectCompleted()
    fun replayEffectError(message: String)
}

private enum class StingersState {
    setup,
    begin,
    middle,
    end,
}

private sealed class ReplayEffectOutput {
    object Background : ReplayEffectOutput()

    data class Replay(val image: EffectImageCiImage) : ReplayEffectOutput()

    data class FadeToReplay(val image: EffectImageCiImage, val ratio: Double) : ReplayEffectOutput()

    data class FadeToBackground(val image: EffectImageCiImage, val ratio: Double) : ReplayEffectOutput()

    data class Stinger(
        val stingerImage: EffectImageCiImage,
        val backgroundImage: EffectImageCiImage?,
    ) : ReplayEffectOutput()
}

class ReplayEffect internal constructor(
    video: ReplayBufferFile,
    start: Double,
    stop: Double,
    speed: Double,
    size: Size,
    layout: SettingsWidgetLayout,
    transitionMode: ReplayEffectTransitionMode,
    delegate: ReplayEffectDelegate,
) : VideoEffect() {
    private var playbackCompleted = false
    private val speed = speed
    private val reader = ReplayEffectReplayReader(
        video = video,
        start = start,
        duration = stop - start,
        size = size,
    )
    private var startPresentationTimeStamp: Double? = null
    private val delegate = java.util.concurrent.atomic.AtomicReference(delegate)
    private var lastImageOffset: Double? = null
    private var latestImage: EffectImageCiImage? = null
    private var cancelled = false
    private var cancelledOffset: Double? = null
    private val transitionMode = transitionMode
    private val duration = stop - start
    private var layout = layout
    private var latestTimeLeft = Int.MAX_VALUE
    private var stingersState = StingersState.setup
    private var stingersInReader: ReplayEffectStingerReader? = null
    private var stingersOutReader: ReplayEffectStingerReader? = null
    private var stingersInTransitionPoint = 0.0
    private var stingersOutTransitionPoint = 0.0
    private var stingersInTransitionPointPresentationTimeStamp = 0.0
    private var stingersOutTransitionStartPresentationTimeStamp = 0.0
    private var stingersOutTransitionPointPresentationTimeStamp = 0.0

    init {
        val stingers = transitionMode as? ReplayEffectTransitionMode.Stingers
        if (stingers != null) {
            stingersInReader = ReplayEffectStingerReader(path = stingers.inPath, size = size)
            stingersInTransitionPoint = stingers.inTransitionPoint
            stingersOutReader = ReplayEffectStingerReader(path = stingers.outPath, size = size)
            stingersOutTransitionPoint = stingers.outTransitionPoint
        }
        updateStatus(offset = 0.0)
    }

    fun setLayout(layout: SettingsWidgetLayout) {
        processorPipelineQueue.launch {
            this@ReplayEffect.layout = layout
        }
    }

    fun cancel() {
        processorPipelineQueue.launch {
            this@ReplayEffect.cancelled = true
        }
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        val output = update(info.presentationTimeStamp / 1_000_000.0)
        return when (output) {
            is ReplayEffectOutput.Background -> image
            is ReplayEffectOutput.Replay -> applyLayoutToReplay(output.image, image)
            is ReplayEffectOutput.FadeToReplay -> {
                fade(image, applyLayoutToReplay(output.image, image), output.ratio) ?: image
            }
            is ReplayEffectOutput.FadeToBackground -> {
                fade(applyLayoutToReplay(output.image, image), image, output.ratio) ?: image
            }
            is ReplayEffectOutput.Stinger -> {
                val backgroundImage = output.backgroundImage
                    ?.let { applyLayoutToReplay(it, image) } ?: image
                output.stingerImage.getCiImage().composited(over = backgroundImage)
            }
        }
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        val output = update(info.presentationTimeStamp / 1_000_000.0)
        return when (output) {
            is ReplayEffectOutput.Background -> image
            is ReplayEffectOutput.Replay -> applyLayoutToReplayMetalPetal(output.image, image)
            is ReplayEffectOutput.FadeToReplay -> {
                fadeMetalPetal(image, applyLayoutToReplayMetalPetal(output.image, image), output.ratio)
            }
            is ReplayEffectOutput.FadeToBackground -> {
                fadeMetalPetal(applyLayoutToReplayMetalPetal(output.image, image), image, output.ratio)
            }
            is ReplayEffectOutput.Stinger -> {
                val backgroundImage = output.backgroundImage
                    ?.let { applyLayoutToReplayMetalPetal(it, image) } ?: image
                blendMetalPetal(output.stingerImage.getMetalPetalImage(), backgroundImage, 1f)
            }
        }
    }

    override fun shouldRemove(): Boolean {
        return playbackCompleted
    }

    private fun update(presentationTimeStamp: Double): ReplayEffectOutput {
        return when (transitionMode) {
            ReplayEffectTransitionMode.None,
            ReplayEffectTransitionMode.Fade,
            -> updateNoneAndFade(presentationTimeStamp)
            is ReplayEffectTransitionMode.Stingers -> updateStingers(presentationTimeStamp)
        }
    }

    private fun applyLayoutToReplay(replayImage: EffectImageCiImage, image: CIImage): CIImage {
        return replayImage.getCiImage()
            .resizeMirror(layout, image.extent.size, false)
            .move(layout, image.extent.size)
            .cropped(to = image.extent)
            .composited(over = image)
    }

    private fun applyLayoutToReplayMetalPetal(
        replayImage: EffectImageCiImage,
        image: MTIImage,
    ): MTIImage {
        val replayMetalPetalImage = replayImage.getMetalPetalImage()
        return replayMetalPetalImage.resizeMirrorMoveComposited(
            layout,
            false,
            image,
            MetalPetalWidgetShape(contentRegion = replayMetalPetalImage.extent),
        )
    }

    private fun fade(input: CIImage, target: CIImage, ratio: Double): CIImage? {
        val filter = CIFilter.dissolveTransition()
        filter.inputImage = input
        filter.targetImage = target
        filter.time = ratio.toFloat()
        return filter.outputImage
    }

    private fun fadeMetalPetal(input: MTIImage, target: MTIImage, ratio: Double): MTIImage {
        return blendMetalPetal(target, input, ratio.toFloat())
    }

    private fun blendMetalPetal(
        image: MTIImage,
        backgroundImage: MTIImage,
        intensity: Float,
    ): MTIImage {
        val filter = MTIBlendFilter(blendMode = MTIBlendMode.normal)
        filter.inputBackgroundImage = backgroundImage
        filter.inputImage = image
        filter.intensity = intensity
        return filter.outputImage ?: backgroundImage
    }

    private fun updateStatus(offset: Double) {
        if (cancelled) {
            return
        }
        val timeLeft = maxOf(ceil(duration / speed - offset).toInt(), 0)
        if (timeLeft != latestTimeLeft) {
            latestTimeLeft = timeLeft
            delegate.get()?.replayEffectStatus(timeLeft = timeLeft)
        }
    }

    private fun replayCompleted() {
        playbackCompleted = true
        if (!cancelled) {
            delegate.get()?.replayEffectCompleted()
        }
    }

    private fun updateNoneAndFade(presentationTimeStamp: Double): ReplayEffectOutput {
        if (startPresentationTimeStamp == null) {
            startPresentationTimeStamp = presentationTimeStamp
        }
        val offset = presentationTimeStamp - startPresentationTimeStamp!!
        updateStatus(offset = offset)
        if (cancelled) {
            if (cancelledOffset == null) {
                cancelledOffset = offset
            }
            return updateEndNoneAndFade(offset - cancelledOffset!!)
        } else if (lastImageOffset != null) {
            return updateEndNoneAndFade(offset - lastImageOffset!!)
        } else {
            return updateBeginAndMiddleNoneAndFade(offset)
        }
    }

    private fun updateBeginAndMiddleNoneAndFade(offset: Double): ReplayEffectOutput {
        val replayImage = reader.getImage(offset = offset * speed)
        latestImage = replayImage.image ?: latestImage
        if (replayImage.isLast) {
            lastImageOffset = offset
        } else if (replayImage.image == null) {
            startPresentationTimeStamp = null
        }
        val latestImage = this.latestImage ?: return ReplayEffectOutput.Background
        if (transitionMode is ReplayEffectTransitionMode.Fade && offset <= fadeTransitionLength) {
            return ReplayEffectOutput.FadeToReplay(latestImage, offset / fadeTransitionLength)
        } else {
            return ReplayEffectOutput.Replay(latestImage)
        }
    }

    private fun updateEndNoneAndFade(offset: Double): ReplayEffectOutput {
        if (transitionMode is ReplayEffectTransitionMode.Fade && offset <= fadeTransitionLength) {
            val latestImage = this.latestImage ?: return ReplayEffectOutput.Background
            return ReplayEffectOutput.FadeToBackground(latestImage, offset / fadeTransitionLength)
        } else {
            replayCompleted()
            return ReplayEffectOutput.Background
        }
    }

    private fun updateStingers(presentationTimeStamp: Double): ReplayEffectOutput {
        return when (stingersState) {
            StingersState.setup -> updateStingersSetup(presentationTimeStamp)
            StingersState.begin -> updateStingersBegin(presentationTimeStamp)
            StingersState.middle -> updateStingersMiddle(presentationTimeStamp)
            StingersState.end -> updateStingersEnd(presentationTimeStamp)
        }
    }

    private fun updateStingersSetup(presentationTimeStamp: Double): ReplayEffectOutput {
        val stingersInReader = this.stingersInReader ?: return ReplayEffectOutput.Background
        val stingersOutReader = this.stingersOutReader ?: return ReplayEffectOutput.Background
        if (stingersInReader.setupState == ReplayEffectStingerReaderSetupState.ok &&
            stingersOutReader.setupState == ReplayEffectStingerReaderSetupState.ok
        ) {
            startPresentationTimeStamp = presentationTimeStamp
            stingersInTransitionPointPresentationTimeStamp = presentationTimeStamp +
                stingersInReader.duration * stingersInTransitionPoint
            stingersOutTransitionPointPresentationTimeStamp =
                stingersInTransitionPointPresentationTimeStamp + duration / speed
            stingersOutTransitionStartPresentationTimeStamp =
                stingersOutTransitionPointPresentationTimeStamp -
                    stingersOutReader.duration * stingersOutTransitionPoint
            stingersState = StingersState.begin
        } else if (stingersInReader.setupState == ReplayEffectStingerReaderSetupState.failed) {
            reportBadStingerVideo()
            replayCompleted()
        } else if (stingersOutReader.setupState == ReplayEffectStingerReaderSetupState.failed) {
            reportBadStingerVideo()
            replayCompleted()
        }
        return ReplayEffectOutput.Background
    }

    private fun updateStingersBegin(presentationTimeStamp: Double): ReplayEffectOutput {
        updateCancelled(presentationTimeStamp)
        val backgroundImage = getStingersBackgroundImage(presentationTimeStamp)
        val offset = presentationTimeStamp - startPresentationTimeStamp!!
        val stingerImage = stingersInReader?.getImage(offset = offset)?.image
        if (stingerImage != null) {
            return ReplayEffectOutput.Stinger(stingerImage, backgroundImage)
        } else {
            stingersState = StingersState.middle
            return makeBackgroundOutput(backgroundImage)
        }
    }

    private fun updateStingersMiddle(presentationTimeStamp: Double): ReplayEffectOutput {
        updateCancelled(presentationTimeStamp)
        if (presentationTimeStamp >= stingersOutTransitionStartPresentationTimeStamp) {
            stingersState = StingersState.end
        }
        return makeBackgroundOutput(getReplayImage(presentationTimeStamp))
    }

    private fun updateStingersEnd(presentationTimeStamp: Double): ReplayEffectOutput {
        val backgroundImage = getStingersBackgroundImage(presentationTimeStamp)
        val offset = presentationTimeStamp - stingersOutTransitionStartPresentationTimeStamp
        val stingerImage = stingersOutReader?.getImage(offset = offset)?.image
        if (stingerImage != null) {
            return ReplayEffectOutput.Stinger(stingerImage, backgroundImage)
        } else {
            replayCompleted()
            return makeBackgroundOutput(backgroundImage)
        }
    }

    private fun makeBackgroundOutput(replayImage: EffectImageCiImage?): ReplayEffectOutput {
        if (replayImage == null) {
            return ReplayEffectOutput.Background
        }
        return ReplayEffectOutput.Replay(replayImage)
    }

    private fun getStingersBackgroundImage(
        presentationTimeStamp: Double,
    ): EffectImageCiImage? {
        return if (presentationTimeStamp < stingersInTransitionPointPresentationTimeStamp) {
            null
        } else if (presentationTimeStamp > stingersOutTransitionPointPresentationTimeStamp) {
            updateStatus(offset = duration / speed)
            null
        } else {
            getReplayImage(presentationTimeStamp)
        }
    }

    private fun getReplayImage(presentationTimeStamp: Double): EffectImageCiImage? {
        val offset = presentationTimeStamp - stingersInTransitionPointPresentationTimeStamp
        updateStatus(offset = offset)
        return reader.getImage(offset = offset * speed).image
    }

    private fun updateCancelled(presentationTimeStamp: Double) {
        val stingersOutReader = this.stingersOutReader
        if (!cancelled || stingersOutReader == null) {
            return
        }
        stingersState = StingersState.end
        stingersOutTransitionStartPresentationTimeStamp = presentationTimeStamp
        stingersOutTransitionPointPresentationTimeStamp = presentationTimeStamp +
            stingersOutReader.duration * stingersOutTransitionPoint
    }

    private fun reportBadStingerVideo() {
        delegate.get()?.replayEffectError(message = localized("Bad replay stinger video"))
    }
}
