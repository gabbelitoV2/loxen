package com.moblin.android.videoeffects

import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coreimage.CIFilter
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidgetQrCode
import kotlinx.coroutines.launch

class QrCodeEffect(private val widget: SettingsWidgetQrCode) : VideoEffect() {
    private var newSceneWidget: SettingsSceneWidget? = null
    private var sceneWidget: SettingsSceneWidget? = null
    private var size: CGSize = CGSize.zero
    private var qrCodeImage: EffectImageCiImage? = null

    fun setSceneWidget(sceneWidget: SettingsSceneWidget) {
        processorPipelineQueue.launch {
            newSceneWidget = sceneWidget
        }
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        val newSceneWidget = newSceneWidget ?: return image
        update(newSceneWidget = newSceneWidget, size = image.extent.size)
        val qrCodeImage = qrCodeImage ?: return image
        return applyEffectsResizeMirrorMove(qrCodeImage.getCiImage(),
                                            newSceneWidget,
                                            false,
                                            image.extent,
                                            info)
            .composited(over = image)
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        val newSceneWidget = newSceneWidget ?: return image
        update(newSceneWidget = newSceneWidget, size = image.extent.size)
        val qrCodeImage = qrCodeImage ?: return image
        return applyEffectsResizeMirrorMoveMetalPetal(qrCodeImage.getMetalPetalImage(),
                                                      newSceneWidget,
                                                      false,
                                                      image,
                                                      info)
    }

    private fun update(newSceneWidget: SettingsSceneWidget, size: CGSize) {
        val oldExtent = sceneWidget?.layout?.extent()
        val newExtent = newSceneWidget.layout.extent()
        val sameExtent = newExtent.width() == oldExtent?.width() && newExtent.height() == oldExtent?.height()
        val sameSize = size.width == this.size.width && size.height == this.size.height
        if (sameExtent && sameSize) {
            return
        }
        val data = widget.message.toByteArray(Charsets.UTF_8)
        val filter = CIFilter.qrCodeGenerator()
        filter.message = data
        filter.correctionLevel = "M"
        sceneWidget = newSceneWidget
        this.size = size
        val image = filter.outputImage ?: return
        val scale = 400.0 / image.extent.size.width
        qrCodeImage = image.scaled(x = scale, y = scale).toEffectImage(isOpaque = true)
    }
}
