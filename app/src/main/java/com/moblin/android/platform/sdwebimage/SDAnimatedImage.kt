package com.moblin.android.platform.sdwebimage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.bumptech.glide.gifdecoder.GifDecoder
import com.bumptech.glide.gifdecoder.GifHeader
import com.bumptech.glide.gifdecoder.StandardGifDecoder
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "MoblinEffects"
private const val DEFAULT_FRAME_DURATION = 0.1

private val loggedMessages = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

private fun logOnce(key: String, message: String) {
    if (!loggedMessages.add(key)) {
        return
    }
    try {
        Log.i(TAG, message)
    } catch (_: Throwable) {
    }
}

internal enum class AnimatedImageFormat {
    gif,
    png,
    unsupported,
}

internal fun animatedImageFormat(data: ByteArray): AnimatedImageFormat {
    if (data.size >= 6 && data[0] == 'G'.code.toByte() && data[1] == 'I'.code.toByte() &&
        data[2] == 'F'.code.toByte() && data[3] == '8'.code.toByte()
    ) {
        return AnimatedImageFormat.gif
    }
    val pngSignature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    if (data.size >= pngSignature.size && pngSignature.indices.all { data[it] == pngSignature[it] }) {
        return AnimatedImageFormat.png
    }
    return AnimatedImageFormat.unsupported
}

internal fun sdFrameDuration(delayMilliseconds: Int): Double {
    val seconds = delayMilliseconds / 1000.0
    return if (seconds < 0.011) DEFAULT_FRAME_DURATION else seconds
}

internal fun sdLoopCount(netscapeLoopCount: Int): Int {
    return if (netscapeLoopCount == GifHeader.NETSCAPE_LOOP_COUNT_DOES_NOT_EXIST) 1 else netscapeLoopCount
}

private class FreshBitmapProvider : GifDecoder.BitmapProvider {
    override fun obtain(width: Int, height: Int, config: Bitmap.Config): Bitmap {
        return Bitmap.createBitmap(width, height, config)
    }

    override fun release(bitmap: Bitmap) {}

    override fun obtainByteArray(size: Int): ByteArray {
        return ByteArray(size)
    }

    override fun release(bytes: ByteArray) {}

    override fun obtainIntArray(size: Int): IntArray {
        return IntArray(size)
    }

    override fun release(array: IntArray) {}
}

class SDAnimatedImage private constructor(
    private val frames: List<Bitmap>,
    private val durations: List<Double>,
    val animatedImageLoopCount: Int,
) {
    val animatedImageFrameCount: Int
        get() = frames.size

    fun animatedImageFrame(at: Int): Bitmap? {
        return frames.getOrNull(at)
    }

    fun animatedImageDuration(at: Int): Double {
        return durations.getOrNull(at) ?: 0.0
    }

    companion object {
        operator fun invoke(data: ByteArray): SDAnimatedImage? {
            return try {
                when (animatedImageFormat(data)) {
                    AnimatedImageFormat.gif -> decodeGif(data)
                    AnimatedImageFormat.png -> decodePng(data)
                    AnimatedImageFormat.unsupported -> {
                        logOnce("unsupported", "SDAnimatedImage: only GIF and PNG data are animated images")
                        null
                    }
                }
            } catch (error: Throwable) {
                logOnce("decode:${error.javaClass.name}", "SDAnimatedImage: decoding failed: $error")
                null
            }
        }

        private fun decodeGif(data: ByteArray): SDAnimatedImage? {
            val decoder = StandardGifDecoder(FreshBitmapProvider())
            decoder.setDefaultBitmapConfig(Bitmap.Config.ARGB_8888)
            val status = decoder.read(data)
            val frameCount = decoder.frameCount
            if (status != GifDecoder.STATUS_OK || frameCount <= 0) {
                logOnce("gifStatus:$status", "SDAnimatedImage: GIF decoding failed with status $status")
                decoder.clear()
                return null
            }
            val frames = ArrayList<Bitmap>(frameCount)
            val durations = ArrayList<Double>(frameCount)
            for (index in 0 until frameCount) {
                decoder.advance()
                val frame = decoder.nextFrame ?: break
                frames.add(frame)
                durations.add(sdFrameDuration(decoder.getDelay(index)))
            }
            val loopCount = sdLoopCount(decoder.netscapeLoopCount)
            decoder.clear()
            if (frames.isEmpty()) {
                return null
            }
            return SDAnimatedImage(frames, durations, loopCount)
        }

        private fun decodePng(data: ByteArray): SDAnimatedImage? {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inPremultiplied = true
            }
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options) ?: return null
            return SDAnimatedImage(listOf(bitmap), listOf(DEFAULT_FRAME_DURATION), 1)
        }
    }
}
