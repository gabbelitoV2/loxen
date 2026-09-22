package com.moblin.android.videoeffects.replay

import android.media.Image
import android.util.Size
import com.moblin.android.localized
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.various.ReplayBufferFile
import com.moblin.android.various.settings.SettingsWidgetLayout
import com.moblin.android.videoeffects.EffectImageCiImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max

private const val fadeTransitionLength = 0.5
val replayEffectQueue = CoroutineScope(Dispatchers.Default)

sealed class ReplayEffectTransitionMode {
    object Fade : ReplayEffectTransitionMode()

    data class Stingers(
        val inPath: String,
        val inTransitionPoint: Double,
        val outPath: String,
        val outTransitionPoint: Double
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
    end
}

private sealed class ReplayEffectOutput {
    object Background : ReplayEffectOutput()
    data class Replay(val image: EffectImageCiImage) : ReplayEffectOutput()
    data class FadeToReplay(val image: EffectImageCiImage, val ratio: Double) : ReplayEffectOutput()
    data class FadeToBackground(val image: EffectImageCiImage, val ratio: Double) : ReplayEffectOutput()
    data class Stinger(val stinger: EffectImageCiImage, val replay: EffectImageCiImage?) : ReplayEffectOutput()
}

class ReplayEffect internal constructor(
    video: ReplayBufferFile,
    start: Double,
    stop: Double,
    private val speed: Double,
    size: Size,
    private var layout: SettingsWidgetLayout,
    private val transitionMode: ReplayEffectTransitionMode,
    private val delegate: ReplayEffectDelegate
) : VideoEffect() {
    private var playbackCompleted = false
    private val reader: ReplayEffectReplayReader
    private var startPresentationTimeStamp: Double? = null
    private var lastImageOffset: Double? = null
    private var latestImage: EffectImageCiImage? = null
    private var cancelled = false
    private var cancelledOffset: Double? = null
    private val duration: Double = stop - start
    private var latestTimeLeft = Int.MAX_VALUE
    private var stingersState = StingersState.setup
    private var stingersInReader: ReplayEffectStingerReader? = null
    private var stingersOutReader: ReplayEffectStingerReader? = null
    private var stingersInTransitionPoint: Double = 0.0
    private var stingersOutTransitionPoint: Double = 0.0
    private var stingersInTransitionPointPresentationTimeStamp: Double = 0.0
    private var stingersOutTransitionStartPresentationTimeStamp: Double = 0.0
    private var stingersOutTransitionPointPresentationTimeStamp: Double = 0.0

    init {
        reader = ReplayEffectReplayReader(video, start, duration, size)
        val mode = transitionMode
        if (mode is ReplayEffectTransitionMode.Stingers) {
            stingersInReader = ReplayEffectStingerReader(mode.inPath, size)
            stingersInTransitionPoint = mode.inTransitionPoint
            stingersOutReader = ReplayEffectStingerReader(mode.outPath, size)
            stingersOutTransitionPoint = mode.outTransitionPoint
        }
        updateStatus(0.0)
    }

    fun setLayout(layout: SettingsWidgetLayout) {
        processorPipelineQueue.launch {
            this@ReplayEffect.layout = layout
        }
    }

    fun cancel() {
        processorPipelineQueue.launch {
            cancelled = true
        }
    }

    override fun execute(image: Image, info: VideoEffectInfo): Image {
        val output = update(info.presentationTimeStamp / 1_000_000.0)
        return when (output) {
            is ReplayEffectOutput.Background -> image
            is ReplayEffectOutput.Replay -> applyLayoutToReplay(output.image, image)
            is ReplayEffectOutput.FadeToReplay ->
                fade(image, applyLayoutToReplay(output.image, image), output.ratio) ?: image
            is ReplayEffectOutput.FadeToBackground ->
                fade(applyLayoutToReplay(output.image, image), image, output.ratio) ?: image
            is ReplayEffectOutput.Stinger -> {
                val backgroundImage = output.replay?.let { applyLayoutToReplay(it, image) } ?: image
                TODO()
            }
        }
    }

    override fun executeMetalPetal(image: Image, info: VideoEffectInfo): Image {
        val output = update(info.presentationTimeStamp / 1_000_000.0)
        return when (output) {
            is ReplayEffectOutput.Background -> image
            is ReplayEffectOutput.Replay -> applyLayoutToReplayMetalPetal(output.image, image)
            is ReplayEffectOutput.FadeToReplay ->
                fadeMetalPetal(image, applyLayoutToReplayMetalPetal(output.image, image), output.ratio)
            is ReplayEffectOutput.FadeToBackground ->
                fadeMetalPetal(applyLayoutToReplayMetalPetal(output.image, image), image, output.ratio)
            is ReplayEffectOutput.Stinger -> {
                val backgroundImage = output.replay?.let { applyLayoutToReplayMetalPetal(it, image) } ?: image
                blendMetalPetal(output.stinger, backgroundImage, 1f)
            }
        }
    }

    override fun shouldRemove(): Boolean {
        return playbackCompleted
    }

    private fun update(presentationTimeStamp: Double): ReplayEffectOutput {
        return when (transitionMode) {
            ReplayEffectTransitionMode.None, ReplayEffectTransitionMode.Fade ->
                updateNoneAndFade(presentationTimeStamp)
            is ReplayEffectTransitionMode.Stingers -> updateStingers(presentationTimeStamp)
        }
    }

    private fun applyLayoutToReplay(replayImage: EffectImageCiImage, image: Image): Image {
        TODO()
    }

    private fun applyLayoutToReplayMetalPetal(replayImage: EffectImageCiImage,
                                              image: Image): Image {
        TODO()
    }

    private fun fade(input: Image,
                     target: Image,
                     ratio: Double): Image? {
        TODO()
    }

    private fun fadeMetalPetal(input: Image,
                               target: Image,
                               ratio: Double): Image {
        return blendMetalPetal(target, input, ratio.toFloat())
    }

    private fun blendMetalPetal(image: Any,
                                backgroundImage: Image,
                                intensity: Float): Image {
        TODO()
    }

    private fun updateStatus(offset: Double) {
        if (cancelled) {
            return
        }
        val timeLeft = max(ceil(duration / speed - offset).toInt(), 0)
        if (timeLeft != latestTimeLeft) {
            latestTimeLeft = timeLeft
            delegate.replayEffectStatus(timeLeft)
        }
    }

    private fun replayCompleted() {
        playbackCompleted = true
        if (!cancelled) {
            delegate.replayEffectCompleted()
        }
    }

    private fun updateNoneAndFade(presentationTimeStamp: Double): ReplayEffectOutput {
        if (startPresentationTimeStamp == null) {
            startPresentationTimeStamp = presentationTimeStamp
        }
        val offset = presentationTimeStamp - startPresentationTimeStamp!!
        updateStatus(offset)
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
        val replayImage = reader.getImage(offset * speed)
        latestImage = replayImage.image ?: latestImage
        if (replayImage.isLast) {
            lastImageOffset = offset
        } else if (replayImage.image == null) {
            startPresentationTimeStamp = null
        }
        val image = latestImage ?: return ReplayEffectOutput.Background
        return if (transitionMode is ReplayEffectTransitionMode.Fade && offset <= fadeTransitionLength) {
            ReplayEffectOutput.FadeToReplay(image, offset / fadeTransitionLength)
        } else {
            ReplayEffectOutput.Replay(image)
        }
    }

    private fun updateEndNoneAndFade(offset: Double): ReplayEffectOutput {
        return if (transitionMode is ReplayEffectTransitionMode.Fade && offset <= fadeTransitionLength) {
            val image = latestImage ?: return ReplayEffectOutput.Background
            ReplayEffectOutput.FadeToBackground(image, offset / fadeTransitionLength)
        } else {
            replayCompleted()
            ReplayEffectOutput.Background
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
        val inReader = stingersInReader ?: return ReplayEffectOutput.Background
        val outReader = stingersOutReader ?: return ReplayEffectOutput.Background
        if (inReader.setupState == ReplayEffectStingerReaderSetupState.ok &&
            outReader.setupState == ReplayEffectStingerReaderSetupState.ok)
        {
            startPresentationTimeStamp = presentationTimeStamp
            stingersInTransitionPointPresentationTimeStamp = presentationTimeStamp +
                inReader.duration * stingersInTransitionPoint
            stingersOutTransitionPointPresentationTimeStamp = stingersInTransitionPointPresentationTimeStamp +
                duration / speed
            stingersOutTransitionStartPresentationTimeStamp = stingersOutTransitionPointPresentationTimeStamp -
                outReader.duration * stingersOutTransitionPoint
            stingersState = StingersState.begin
        } else if (inReader.setupState == ReplayEffectStingerReaderSetupState.failed) {
            reportBadStingerVideo()
            replayCompleted()
        } else if (outReader.setupState == ReplayEffectStingerReaderSetupState.failed) {
            reportBadStingerVideo()
            replayCompleted()
        }
        return ReplayEffectOutput.Background
    }

    private fun updateStingersBegin(presentationTimeStamp: Double): ReplayEffectOutput {
        updateCancelled(presentationTimeStamp)
        val backgroundImage = getStingersBackgroundImage(presentationTimeStamp)
        val offset = presentationTimeStamp - startPresentationTimeStamp!!
        val stingerImage = stingersInReader?.getImage(offset)?.image
        return if (stingerImage != null) {
            ReplayEffectOutput.Stinger(stingerImage, backgroundImage)
        } else {
            stingersState = StingersState.middle
            makeBackgroundOutput(backgroundImage)
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
        val stingerImage = stingersOutReader?.getImage(offset)?.image
        return if (stingerImage != null) {
            ReplayEffectOutput.Stinger(stingerImage, backgroundImage)
        } else {
            replayCompleted()
            makeBackgroundOutput(backgroundImage)
        }
    }

    private fun makeBackgroundOutput(replayImage: EffectImageCiImage?): ReplayEffectOutput {
        if (replayImage == null) {
            return ReplayEffectOutput.Background
        }
        return ReplayEffectOutput.Replay(replayImage)
    }

    private fun getStingersBackgroundImage(presentationTimeStamp: Double): EffectImageCiImage? {
        return if (presentationTimeStamp < stingersInTransitionPointPresentationTimeStamp) {
            null
        } else if (presentationTimeStamp > stingersOutTransitionPointPresentationTimeStamp) {
            updateStatus(duration / speed)
            null
        } else {
            getReplayImage(presentationTimeStamp)
        }
    }

    private fun getReplayImage(presentationTimeStamp: Double): EffectImageCiImage? {
        val offset = presentationTimeStamp - stingersInTransitionPointPresentationTimeStamp
        updateStatus(offset)
        return reader.getImage(offset * speed).image
    }

    private fun updateCancelled(presentationTimeStamp: Double) {
        if (!cancelled) {
            return
        }
        val outReader = stingersOutReader ?: return
        stingersState = StingersState.end
        stingersOutTransitionStartPresentationTimeStamp = presentationTimeStamp
        stingersOutTransitionPointPresentationTimeStamp = presentationTimeStamp +
            outReader.duration * stingersOutTransitionPoint
    }

    private fun reportBadStingerVideo() {
        delegate.replayEffectError(localized("Bad replay stinger video"))
    }
}
