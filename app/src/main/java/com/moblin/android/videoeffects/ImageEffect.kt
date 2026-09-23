package com.moblin.android.videoeffects

import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.coreimage.CIFilter
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.coreimage.CIImageOption
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.storages.ImageStorage
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ImageEffect(imageStorage: ImageStorage, widgetId: UUID) : VideoEffect() {
    private val filter = CIFilter.sourceOverCompositing()
    private var originalImage: EffectImageCiImage? = null
    private var sceneWidget: SettingsSceneWidget? = null

    init {
        loadImage(imageStorage = imageStorage, widgetId = widgetId)
    }

    fun loadImage(imageStorage: ImageStorage, widgetId: UUID) {
        CoroutineScope(Dispatchers.Default).launch {
            val data = imageStorage.read(id = widgetId) ?: return@launch
            val image = CIImage(data = data, options = mapOf(CIImageOption.applyOrientationProperty to true))
                ?: return@launch
            val originalImage = image.toEffectImage(isOpaque = false)
            processorPipelineQueue.launch {
                this@ImageEffect.originalImage = originalImage
            }
        }
    }

    fun setSceneWidget(sceneWidget: SettingsSceneWidget) {
        processorPipelineQueue.launch {
            this@ImageEffect.sceneWidget = sceneWidget
        }
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        val originalImage = originalImage ?: return image
        val sceneWidget = sceneWidget ?: return image
        filter.inputImage = applyEffectsResizeMirrorMove(
            image = originalImage.getCiImage(),
            sceneWidget = sceneWidget,
            mirror = false,
            backgroundImageExtent = image.extent,
            info = info,
        )
        filter.backgroundImage = image
        return filter.outputImage ?: image
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        val originalImage = originalImage ?: return image
        val sceneWidget = sceneWidget ?: return image
        return applyEffectsResizeMirrorMoveMetalPetal(
            image = originalImage.getMetalPetalImage(),
            sceneWidget = sceneWidget,
            mirror = false,
            backgroundImage = image,
            info = info,
        )
    }
}
