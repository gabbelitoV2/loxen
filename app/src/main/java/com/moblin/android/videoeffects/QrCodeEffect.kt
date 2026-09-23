package com.moblin.android.videoeffects

import com.moblin.android.platform.video.CVPixelBuffer as Image
import android.util.Size
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidgetQrCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class QrCodeEffect(private val widget: SettingsWidgetQrCode) : VideoEffect() {
    private var newSceneWidget: SettingsSceneWidget? = null
    private var sceneWidget: SettingsSceneWidget? = null
    private var size: Size = Size(0, 0)
    private var qrCodeImage: EffectImageCiImage? = null

    fun setSceneWidget(sceneWidget: SettingsSceneWidget) {
        processorPipelineQueue.launch {
            newSceneWidget = sceneWidget
        }
    }

    override fun execute(image: Image, info: VideoEffectInfo): Image {
        return TODO("OpenGL ES port")
    }

    override fun executeMetalPetal(image: Image, info: VideoEffectInfo): Image {
        return TODO("OpenGL ES port")
    }

    private fun update(newSceneWidget: SettingsSceneWidget, size: Size) {
        if (newSceneWidget.layout.extent() == sceneWidget?.layout?.extent() && size == this.size) {
            return
        }
        val data = widget.message.toByteArray(Charsets.UTF_8)
        sceneWidget = newSceneWidget
        this.size = size
        qrCodeImage = TODO("OpenGL ES port: encode data as a QR code (correction level M), scale it to 400 px width and wrap the result in an EffectImageCiImage")
    }
}
