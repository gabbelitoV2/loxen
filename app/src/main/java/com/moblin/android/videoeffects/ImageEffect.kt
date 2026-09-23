package com.moblin.android.videoeffects

import com.moblin.android.platform.video.CVPixelBuffer as Image
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.storages.ImageStorage
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ImageEffect(imageStorage: ImageStorage, widgetId: UUID) : VideoEffect() {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val filter: Any? = null
    private var originalImage: EffectImageCiImage? = null
    private var sceneWidget: SettingsSceneWidget? = null

    init {
        loadImage(imageStorage, widgetId)
    }

    fun loadImage(imageStorage: ImageStorage, widgetId: UUID) {
        scope.launch {
            val data = imageStorage.read(widgetId) ?: return@launch
            val originalImage: EffectImageCiImage =
                TODO()
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

    override fun execute(image: Image, info: VideoEffectInfo): Image {
        TODO()
    }

    override fun executeMetalPetal(image: Image, info: VideoEffectInfo): Image {
        TODO()
    }
}
