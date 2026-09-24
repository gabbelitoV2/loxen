package com.moblin.android.platform.video

import android.graphics.Bitmap
import android.graphics.Color
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.opengl.GLES20
import android.opengl.GLUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.moblin.android.media.MediaSample
import com.moblin.android.platform.avfoundation.AVAsset
import com.moblin.android.platform.avfoundation.AVAssetReader
import com.moblin.android.platform.avfoundation.AVAssetReaderTrackOutput
import com.moblin.android.platform.avfoundation.AVAssetTrack
import com.moblin.android.platform.avfoundation.AVMediaType
import com.moblin.android.platform.core.PipelineThread
import com.moblin.android.platform.coreimage.CIContext
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIAlphaType
import com.moblin.android.platform.metalpetal.MTIContext
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.metalpetal.MTLCreateSystemDefaultDevice
import java.io.File
import java.util.Random
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sin
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

private val flipVertical = floatArrayOf(1f, 0f, 0f, 0f, 0f, -1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 1f)

private class Pattern(val name: String, val bitmap: Bitmap, val compared: (Int, Int) -> Boolean)

private class Difference(val maximum: Int, val psnr: Double, val alphaOk: Boolean)

private class Clip(val reader: AVAssetReader, val output: AVAssetReaderTrackOutput, val planar: Boolean)

@RunWith(AndroidJUnit4::class)
class YCbCrRoundTripSuite {
    private val keep = mutableListOf<Any>()

    @After
    fun tearDown() {
        YCbCrStorage.override = null
        keep.clear()
    }

    private fun <T> onPipeline(block: () -> T): T {
        return PipelineThread.runSync(timeoutMs = 10_000, block = block)
    }

    private fun bitmap(width: Int, height: Int, color: (Int, Int) -> Int): Bitmap {
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] = color(x, y)
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun greyRamp(): Pattern {
        return Pattern("grey ramp", bitmap(256, 16) { x, _ -> Color.rgb(x, x, x) }) { _, _ -> true }
    }

    private fun bars(): Pattern {
        val colors = listOf(
            Color.rgb(191, 191, 191),
            Color.rgb(191, 191, 0),
            Color.rgb(0, 191, 191),
            Color.rgb(0, 191, 0),
            Color.rgb(191, 0, 191),
            Color.rgb(191, 0, 0),
            Color.rgb(0, 0, 191),
        )
        return Pattern("SMPTE bars", bitmap(448, 64) { x, _ -> colors[x / 64] }) { x, _ ->
            x % 64 in 2..61
        }
    }

    private fun blocks(): Pattern {
        val random = Random(7)
        val colors = IntArray(64) { Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256)) }
        return Pattern("2x2-constant blocks", bitmap(128, 128) { x, y -> colors[(y / 16) * 8 + x / 16] }) { x, y ->
            x % 16 in 2..13 && y % 16 in 2..13
        }
    }

    private fun noise(): Pattern {
        val random = Random(11)
        return Pattern(
            "luma noise",
            bitmap(256, 256) { x, y ->
                val grey = random.nextInt(49) - 24
                Color.rgb((x + grey).coerceIn(0, 255), (y + grey).coerceIn(0, 255), (128 + grey).coerceIn(0, 255))
            },
        ) { _, _ -> true }
    }

    private fun oddFrame(): Pattern {
        return Pattern(
            "odd 1919x1079",
            bitmap(1919, 1079) { x, y -> Color.rgb(x * 255 / 1918, y * 255 / 1078, (x + y) * 255 / (1918 + 1078)) },
        ) { _, _ -> true }
    }

    private fun rgbaBuffer(source: Bitmap): CVPixelBuffer {
        val pool = CVPixelBufferPool(source.width, source.height, kCVPixelFormatType_32BGRA, 2)
        keep.add(pool)
        val buffer = checkNotNull(pool.createPixelBuffer())
        onPipeline { PixelBufferGl.upload(source, buffer) }
        return buffer
    }

    private fun emptyRgbaBuffer(width: Int, height: Int): CVPixelBuffer {
        val pool = CVPixelBufferPool(width, height, kCVPixelFormatType_32BGRA, 2)
        keep.add(pool)
        return checkNotNull(pool.createPixelBuffer())
    }

    private fun planarBuffer(source: Bitmap, layout: PixelBufferLayout): CVPixelBuffer {
        val pool = CVPixelBufferPool(source.width, source.height, YCbCrStorage.tagFor(layout, 0x23), 2, layout)
        keep.add(pool)
        val buffer = checkNotNull(pool.createPixelBuffer())
        assertEquals(layout, buffer.layout)
        onPipeline {
            val ids = IntArray(1)
            GLES20.glGenTextures(1, ids, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ids[0])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, source, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            GlRenderer.drawTextureToPlanes(ids[0], flipVertical, buffer)
            GLES20.glDeleteTextures(1, ids, 0)
        }
        return buffer
    }

    private fun pixels(bitmap: Bitmap): IntArray {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels
    }

    private fun read(buffer: CVPixelBuffer): IntArray {
        return pixels(checkNotNull(buffer.toBitmap()))
    }

    private fun difference(
        expected: IntArray,
        actual: IntArray,
        width: Int,
        compared: (Int, Int) -> Boolean,
    ): Difference {
        var maximum = 0
        var squares = 0.0
        var count = 0
        var alphaOk = true
        for (index in expected.indices) {
            if (Color.alpha(actual[index]) != 255) {
                alphaOk = false
            }
            if (!compared(index % width, index / width)) {
                continue
            }
            var shift = 0
            while (shift <= 16) {
                val error = abs(((expected[index] shr shift) and 0xFF) - ((actual[index] shr shift) and 0xFF))
                maximum = max(maximum, error)
                squares += error.toDouble() * error
                count += 1
                shift += 8
            }
        }
        val mse = squares / max(count, 1)
        val psnr = if (mse == 0.0) 99.0 else 10 * log10(255.0 * 255.0 / mse)
        return Difference(maximum, psnr, alphaOk)
    }

    private fun edgeMaximum(expected: IntArray, actual: IntArray, width: Int, height: Int): Int {
        var maximum = 0
        for (index in expected.indices) {
            val x = index % width
            val y = index / width
            if (x > 1 && y > 1 && x < width - 2 && y < height - 2) {
                continue
            }
            var shift = 0
            while (shift <= 16) {
                maximum = max(maximum, abs(((expected[index] shr shift) and 0xFF) - ((actual[index] shr shift) and 0xFF)))
                shift += 8
            }
        }
        return maximum
    }

    private fun check(pattern: Pattern, layout: PixelBufferLayout, expected: IntArray, actual: IntArray) {
        val width = pattern.bitmap.width
        val result = difference(expected, actual, width, pattern.compared)
        val label = "${pattern.name} ${layout.name}: max ${result.maximum}, PSNR ${result.psnr}"
        assertTrue(label, result.alphaOk)
        when (pattern.name) {
            "grey ramp" -> assertTrue(label, result.maximum <= if (layout == PixelBufferLayout.ycbcr420Full) 0 else 1)
            "SMPTE bars", "2x2-constant blocks" -> assertTrue(
                label,
                result.maximum <= if (layout == PixelBufferLayout.ycbcr420Full) 1 else 2,
            )
            "luma noise" -> assertTrue(label, result.psnr >= 45)
            else -> {
                assertTrue(label, result.psnr >= 45)
                assertTrue(label, edgeMaximum(expected, actual, width, pattern.bitmap.height) <= 2)
            }
        }
    }

    private fun patterns(): List<Pattern> {
        return listOf(greyRamp(), bars(), blocks(), noise(), oddFrame())
    }

    @Test
    fun planarStorageRoundTripsKnownImages() {
        YCbCrStorage.override = true
        for (pattern in patterns()) {
            val reference = rgbaBuffer(pattern.bitmap)
            val expected = read(reference)
            assertEquals(pixels(pattern.bitmap).toList(), expected.toList())
            for (layout in listOf(PixelBufferLayout.ycbcr420Full, PixelBufferLayout.ycbcr420Video)) {
                val planar = planarBuffer(pattern.bitmap, layout)
                check(pattern, layout, expected, read(planar))
            }
        }
    }

    @Test
    fun effectsSeeTheSameImageFromPlanarSources() {
        YCbCrStorage.override = true
        val device = checkNotNull(MTLCreateSystemDefaultDevice())
        for (pattern in patterns()) {
            val width = pattern.bitmap.width
            val height = pattern.bitmap.height
            val reference = rgbaBuffer(pattern.bitmap)
            val planar = planarBuffer(pattern.bitmap, PixelBufferLayout.ycbcr420Full)
            val coreImageReference = emptyRgbaBuffer(width, height)
            val coreImagePlanar = emptyRgbaBuffer(width, height)
            CIContext().render(CIImage(cvPixelBuffer = reference), to = coreImageReference)
            CIContext().render(CIImage(cvPixelBuffer = planar), to = coreImagePlanar)
            check(pattern, PixelBufferLayout.ycbcr420Full, read(coreImageReference), read(coreImagePlanar))
            val metalPetalReference = emptyRgbaBuffer(width, height)
            val metalPetalPlanar = emptyRgbaBuffer(width, height)
            MTIContext(device = device).render(
                MTIImage(cvPixelBuffer = reference, alphaType = MTIAlphaType.alphaIsOne),
                to = metalPetalReference,
            )
            MTIContext(device = device).render(
                MTIImage(cvPixelBuffer = planar, alphaType = MTIAlphaType.alphaIsOne),
                to = metalPetalPlanar,
            )
            check(pattern, PixelBufferLayout.ycbcr420Full, read(metalPetalReference), read(metalPetalPlanar))
        }
    }

    private fun fillFrame(image: Image, frame: Int) {
        val luma = image.planes[0]
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                luma.buffer.put(y * luma.rowStride + x * luma.pixelStride, (16 + (x + y + frame * 8) % 220).toByte())
            }
        }
        for (plane in 1..2) {
            val chroma = image.planes[plane]
            for (y in 0 until image.height / 2) {
                for (x in 0 until image.width / 2) {
                    val value = if (plane == 1) {
                        128 + (60 * sin(x / 40.0 + frame / 5.0)).toInt()
                    } else {
                        128 + (60 * cos(y / 30.0 + frame / 7.0)).toInt()
                    }
                    chroma.buffer.put(y * chroma.rowStride + x * chroma.pixelStride, value.toByte())
                }
            }
        }
    }

    private fun encodeClip(file: File, width: Int, height: Int, frames: Int) {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
        format.setInteger(MediaFormat.KEY_BIT_RATE, 20_000_000)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
        val muxer = MediaMuxer(file.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var track = -1
        var queued = 0
        var inputDone = false
        val info = MediaCodec.BufferInfo()
        try {
            while (true) {
                if (!inputDone) {
                    val index = codec.dequeueInputBuffer(10_000)
                    if (index >= 0) {
                        val presentationTimeUs = queued * 33_333L
                        if (queued == frames) {
                            codec.queueInputBuffer(index, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            fillFrame(checkNotNull(codec.getInputImage(index)), queued)
                            codec.queueInputBuffer(index, 0, width * height * 3 / 2, presentationTimeUs, 0)
                            queued += 1
                        }
                    }
                }
                val index = codec.dequeueOutputBuffer(info, 10_000)
                if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    track = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                } else if (index >= 0) {
                    val data = checkNotNull(codec.getOutputBuffer(index))
                    val isConfig = info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                    if (!isConfig && info.size > 0 && track >= 0) {
                        data.position(info.offset)
                        data.limit(info.offset + info.size)
                        muxer.writeSampleData(track, data, info)
                    }
                    codec.releaseOutputBuffer(index, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        break
                    }
                }
            }
        } finally {
            codec.stop()
            codec.release()
            if (track >= 0) {
                muxer.stop()
            }
            muxer.release()
        }
    }

    private fun openClip(file: File, planar: Boolean): Clip {
        val asset = AVAsset(url = file.path)
        val loaded = CountDownLatch(1)
        val track = AtomicReference<AVAssetTrack?>()
        asset.loadTracks(withMediaType = AVMediaType.video) { tracks, _ ->
            track.set(tracks?.firstOrNull())
            loaded.countDown()
        }
        assertTrue(loaded.await(10, TimeUnit.SECONDS))
        val reader = AVAssetReader(asset = asset)
        val output = AVAssetReaderTrackOutput(
            track = checkNotNull(track.get()),
            outputSettings = mapOf(kCVPixelBufferPixelFormatTypeKey to 0x23),
        )
        output.leasesSampleBuffers = true
        reader.add(output)
        assertTrue(reader.startReading())
        return Clip(reader, output, planar)
    }

    private fun nextFrame(clip: Clip): MediaSample? {
        YCbCrStorage.override = clip.planar
        return clip.output.copyNextSampleBuffer()
    }

    private fun readFrame(clip: Clip, sample: MediaSample): IntArray {
        val buffer = checkNotNull(sample.imageBuffer)
        assertEquals(clip.planar, buffer.layout.isPlanar)
        return read(buffer)
    }

    @Test
    fun decodedClipLooksTheSameWithPlanarStorage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "ycbcr-round-trip.mp4")
        encodeClip(file, 1920, 1080, 30)
        val stale = PixelBufferStale.reported.get()
        val leaked = PixelBufferReaper.leakedLeases.get()
        val rgba = openClip(file, planar = false)
        val planar = openClip(file, planar = true)
        var compared = 0
        try {
            while (true) {
                val expected = nextFrame(rgba)
                val actual = nextFrame(planar)
                try {
                    assertEquals(expected == null, actual == null)
                    if (expected == null || actual == null) {
                        break
                    }
                    assertEquals(expected.presentationTimeUs, actual.presentationTimeUs)
                    val reference = readFrame(rgba, expected)
                    val result = difference(reference, readFrame(planar, actual), 1920) { _, _ -> true }
                    assertTrue("frame ${expected.presentationTimeUs}: PSNR ${result.psnr}", result.psnr >= 42)
                    assertTrue(result.alphaOk)
                    compared += 1
                } finally {
                    releaseLease(expected)
                    releaseLease(actual)
                }
            }
        } finally {
            rgba.reader.cancelReading()
            planar.reader.cancelReading()
            YCbCrStorage.override = null
        }
        assertTrue("decoded $compared frames", compared >= 25)
        assertEquals(stale, PixelBufferStale.reported.get())
        assertEquals(leaked, PixelBufferReaper.leakedLeases.get())
        file.delete()
    }
}
