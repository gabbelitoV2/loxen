package com.moblin.android.videoeffects

import android.util.Log
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.videoeffects.text.TextEffect
import java.util.UUID

data class SlideshowEffectSlide(
    val widgetId: UUID,
    val effect: VideoEffect,
    val time: Double,
)

class SlideshowEffect(val slides: List<SlideshowEffectSlide>) : VideoEffect() {
    private var currentSlideIndex: Int = 0
    private var currentSlideEndTime: Double? = null
    private var preparedSlideIndex: Int = 0

    fun setSceneWidget(sceneWidget: SettingsSceneWidget) {
        for (slide in slides) {
            val effect = slide.effect
            if (effect is TextEffect) {
                effect.setSceneWidget(sceneWidget = sceneWidget)
            } else if (effect is ImageEffect) {
                effect.setSceneWidget(sceneWidget = sceneWidget)
            } else {
                Log.i("SlideshowEffect", "slideshow-effect: Unsupported effect.")
            }
        }
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        val (effect, prepareEffect) = getEffects(info.presentationTimeStamp / 1_000_000.0)
        prepareEffect?.prepare(image.extent.size, info)
        return effect?.execute(image, info) ?: image
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        val (effect, prepareEffect) = getEffects(info.presentationTimeStamp / 1_000_000.0)
        prepareEffect?.prepare(image.extent.size, info)
        return effect?.executeMetalPetal(image, info) ?: image
    }

    private fun getEffects(presentationTimeStamp: Double): Pair<VideoEffect?, VideoEffect?> {
        if (slides.isEmpty()) {
            return Pair(null, null)
        }
        val currentSlideEndTime = this.currentSlideEndTime
        if (currentSlideEndTime != null) {
            if (presentationTimeStamp + 0.25 > currentSlideEndTime) {
                var prepareEffect: VideoEffect? = null
                val nextSlideIndex = (currentSlideIndex + 1) % slides.size
                if (nextSlideIndex != preparedSlideIndex) {
                    prepareEffect = slides[nextSlideIndex].effect
                    preparedSlideIndex = nextSlideIndex
                }
                if (presentationTimeStamp > currentSlideEndTime) {
                    currentSlideIndex = nextSlideIndex
                    val slide = slides[currentSlideIndex]
                    this.currentSlideEndTime = presentationTimeStamp + slide.time
                    return Pair(slide.effect, prepareEffect)
                } else {
                    return Pair(slides[currentSlideIndex].effect, prepareEffect)
                }
            } else {
                return Pair(slides[currentSlideIndex].effect, null)
            }
        } else {
            val slide = slides[currentSlideIndex]
            this.currentSlideEndTime = presentationTimeStamp + slide.time
            return Pair(slide.effect, null)
        }
    }
}
