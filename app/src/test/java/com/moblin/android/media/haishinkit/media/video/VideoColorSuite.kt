package com.moblin.android.media.haishinkit.media.video

import android.media.MediaFormat
import android.util.Size
import com.moblin.android.common.various.getIsSync
import com.moblin.android.media.MediaSample
import com.moblin.android.media.kCMTimeInvalidUs
import com.moblin.android.media.haishinkit.codec.video.VideoDecoder
import com.moblin.android.media.haishinkit.codec.video.VideoDecoderDelegate
import com.moblin.android.media.haishinkit.codec.video.VideoEncoder
import com.moblin.android.media.haishinkit.codec.video.VideoEncoderDelegate
import com.moblin.android.media.haishinkit.extension.colorAttachments
import com.moblin.android.media.haishinkit.extension.defaultColorAttachments
import com.moblin.android.media.haishinkit.extension.hlgColorAttachments
import com.moblin.android.media.haishinkit.extension.rec709ColorAttachments
import com.moblin.android.media.haishinkit.extension.sdrColorAttachments
import com.moblin.android.media.haishinkit.extension.sdrYCbCrMatrix
import com.moblin.android.platform.avfoundation.AVCaptureColorSpace
import com.moblin.android.platform.coregraphics.CGColorSpace
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coreimage.CIColor
import com.moblin.android.platform.coreimage.CIContext
import com.moblin.android.platform.coreimage.CIFormat
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.coreimage.CIImageOption
import com.moblin.android.platform.darwin.NSNull
import com.moblin.android.platform.video.CMSampleBufferCreateForImageBuffer
import com.moblin.android.platform.video.CMSampleTimingInfo
import com.moblin.android.platform.video.CVAttachmentMode
import com.moblin.android.platform.video.CVBufferSetAttachments
import com.moblin.android.platform.video.CMVideoFormatDescriptionCreateForImageBuffer
import com.moblin.android.platform.video.CVPixelBuffer
import com.moblin.android.platform.video.CVPixelBufferCreate
import com.moblin.android.platform.video.CVPixelBufferGetBaseAddressOfPlane
import com.moblin.android.platform.video.CVPixelBufferGetBytesPerRowOfPlane
import com.moblin.android.platform.video.CVPixelBufferGetHeight
import com.moblin.android.platform.video.CVPixelBufferGetPixelFormatType
import com.moblin.android.platform.video.CVPixelBufferGetWidth
import com.moblin.android.platform.video.CVPixelBufferLockBaseAddress
import com.moblin.android.platform.video.CVPixelBufferLockFlags
import com.moblin.android.platform.video.CVPixelBufferUnlockBaseAddress
import com.moblin.android.platform.video.kCVImageBufferYCbCrMatrixKey
import com.moblin.android.platform.video.kCVImageBufferYCbCrMatrix_ITU_R_709_2
import com.moblin.android.platform.video.kCVPixelBufferIOSurfacePropertiesKey
import com.moblin.android.platform.video.kCVPixelFormatType_32BGRA
import com.moblin.android.platform.video.kCVPixelFormatType_420YpCbCr8BiPlanarFullRange
import com.moblin.android.platform.video.kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange
import com.moblin.android.platform.video.kCVPixelFormatType_420YpCbCr10BiPlanarFullRange
import com.moblin.android.platform.video.kCVPixelFormatType_420YpCbCr10BiPlanarVideoRange
import com.moblin.android.platform.videotoolbox.CMFormatDescriptionGetExtension
import com.moblin.android.platform.videotoolbox.kCMFormatDescriptionExtension_ColorPrimaries
import com.moblin.android.platform.videotoolbox.kCMFormatDescriptionExtension_FullRangeVideo
import com.moblin.android.platform.videotoolbox.kCMFormatDescriptionExtension_TransferFunction
import com.moblin.android.platform.videotoolbox.kCMFormatDescriptionExtension_YCbCrMatrix
import com.moblin.android.platform.videotoolbox.kVTProfileLevel_H264_Main_AutoLevel
import com.moblin.android.platform.videotoolbox.kVTProfileLevel_HEVC_Main10_AutoLevel
import com.moblin.android.platform.videotoolbox.kVTProfileLevel_HEVC_Main_AutoLevel
import com.moblin.android.various.settings.SettingsStreamColorRange
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.moblin.android.platform.coregraphics.CGSize

private val sRGBColorSpace = CGColorSpace(CGColorSpace.sRGB)

private val fullRange8Bit = kCVPixelFormatType_420YpCbCr8BiPlanarFullRange
private val videoRange8Bit = kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange
private val mjpeg: Int = 0x6A706567
private val fullRange10Bit = kCVPixelFormatType_420YpCbCr10BiPlanarFullRange
private val videoRange10Bit = kCVPixelFormatType_420YpCbCr10BiPlanarVideoRange

private data class TestVideoFormat(override val pixelFormat: Int) : VideoFormat

private class EncoderTester : VideoEncoderDelegate {
    private val lock = Any()
    private val formatDescriptions = mutableListOf<MediaFormat>()
    private val sampleBuffers = mutableListOf<MediaSample>()

    override fun videoEncoderOutputFormat(encoder: VideoEncoder, formatDescription: MediaFormat) {
        synchronized(lock) {
            formatDescriptions.add(formatDescription)
        }
    }

    override fun videoEncoderOutputSampleBuffer(
        encoder: VideoEncoder,
        sampleBuffer: MediaSample,
        decodeTimeStampOffset: Long,
    ) {
        synchronized(lock) {
            sampleBuffers.add(sampleBuffer)
        }
    }

    fun waitForSampleBuffers(count: Int): List<MediaSample> {
        for (i in 0 until 500) {
            val sampleBuffers = synchronized(lock) { this.sampleBuffers.toList() }
            if (sampleBuffers.size >= count) {
                return sampleBuffers
            }
            Thread.sleep(10)
        }
        return synchronized(lock) { this.sampleBuffers.toList() }
    }

    fun waitForFormatDescriptions(count: Int): List<MediaFormat> {
        for (i in 0 until 500) {
            val formatDescriptions = synchronized(lock) { this.formatDescriptions.toList() }
            if (formatDescriptions.size >= count) {
                return formatDescriptions
            }
            Thread.sleep(10)
        }
        return synchronized(lock) { this.formatDescriptions.toList() }
    }
}

private class DecoderTester : VideoDecoderDelegate {
    private val lock = Any()
    private val imageBuffers = mutableListOf<CVPixelBuffer>()

    override fun videoDecoderOutputSampleBuffer(codec: VideoDecoder, sampleBuffer: MediaSample) {
        synchronized(lock) {
            val imageBuffer = sampleBuffer.imageBuffer
            if (imageBuffer != null) {
                imageBuffers.add(imageBuffer)
            }
        }
    }

    fun waitForImageBuffers(count: Int): List<CVPixelBuffer> {
        for (i in 0 until 500) {
            val imageBuffers = synchronized(lock) { this.imageBuffers.toList() }
            if (imageBuffers.size >= count) {
                return imageBuffers
            }
            Thread.sleep(10)
        }
        return synchronized(lock) { this.imageBuffers.toList() }
    }
}

private data class ProcessorOutputCase(
    val colorRange: SettingsStreamColorRange,
    val pixelFormat: Int,
    val hlgPixelFormat: Int,
    val colorAttachments: Map<String, String>,
)

@RunWith(RobolectricTestRunner::class)
class VideoColorSuite {
    @Test
    fun colorAttachments() {
        val pixelBuffer = createPixelBuffer(width = 16, height = 16, colorAttachments = defaultColorAttachments)
        assertEquals(defaultColorAttachments, pixelBuffer.colorAttachments)
        assertTrue(
            createPixelBuffer(width = 16, height = 16, colorAttachments = emptyMap()).colorAttachments.isEmpty()
        )
    }

    @Test
    fun encoderUsesFrameColorAttachments() {
        val lockQueue = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val tester = EncoderTester()
        val encoder = VideoEncoder(lockQueue = CoroutineScope(lockQueue), colorRange = SettingsStreamColorRange.full)
        encoder.delegate = tester
        encoder.settings.value.videoSize = Size(320, 240)
        encoder.settings.value.bitrate = 1_000_000
        encoder.startRunning()
        var frame: Long = 0
        for (colorAttachments in listOf(defaultColorAttachments, rec709ColorAttachments)) {
            val pixelBuffer = createPixelBuffer(width = 320, height = 240, colorAttachments = colorAttachments)
            for (i in 0 until 10) {
                runBlocking(lockQueue) {
                    encoder.encodeImageBuffer(
                        pixelBuffer,
                        presentationTimeStamp = frame * 1_000_000L / 30,
                        duration = 1_000_000L / 30,
                    )
                }
                frame += 1
            }
            tester.waitForFormatDescriptions(if (colorAttachments == defaultColorAttachments) 1 else 2)
        }
        encoder.stopRunning()
        val formatDescriptions = tester.waitForFormatDescriptions(2)
        assertEquals(2, formatDescriptions.size)
        assertEquals(defaultColorAttachments, colorExtensions(formatDescriptions.first()))
        assertEquals(rec709ColorAttachments, colorExtensions(formatDescriptions.last()))
    }

    @Test
    fun encoderUsesOutputColorAttachments() {
        val lockQueue = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val tester = EncoderTester()
        val encoder = VideoEncoder(lockQueue = CoroutineScope(lockQueue), colorRange = SettingsStreamColorRange.full)
        encoder.delegate = tester
        encoder.settings.value.videoSize = Size(320, 240)
        encoder.settings.value.bitrate = 1_000_000
        encoder.outputColorAttachments = defaultColorAttachments
        encoder.startRunning()
        var frame: Long = 0
        for (colorAttachments in listOf(defaultColorAttachments, rec709ColorAttachments, emptyMap<String, String>())) {
            val pixelBuffer = createPixelBuffer(width = 320, height = 240, colorAttachments = colorAttachments)
            for (i in 0 until 10) {
                runBlocking(lockQueue) {
                    encoder.encodeImageBuffer(
                        pixelBuffer,
                        presentationTimeStamp = frame * 1_000_000L / 30,
                        duration = 1_000_000L / 30,
                    )
                }
                frame += 1
            }
        }
        val sampleBuffers = tester.waitForSampleBuffers(30)
        encoder.stopRunning()
        assertEquals(30, sampleBuffers.size)
        assertEquals(1, sampleBuffers.count { it.getIsSync() })
        val formatDescriptions = tester.waitForFormatDescriptions(1)
        assertEquals(listOf(defaultColorAttachments), formatDescriptions.map { colorExtensions(it) })
    }

    @Test
    fun coreImageRenderKeepsSrgbValues() {
        for (sourceColorAttachments in listOf(defaultColorAttachments, rec709ColorAttachments)) {
            for (colorRange in listOf(SettingsStreamColorRange.full, SettingsStreamColorRange.limited)) {
                val pixelFormat = colorRange.pixelFormatType()
                val processor = VideoEffectsProcessor(colorRange = colorRange)
                processor.canvasSize = CGSize(width = 64, height = 64)
                for (color in listOf(
                    Triple(0, 255, 0),
                    Triple(255, 0, 255),
                    Triple(255, 0, 0),
                    Triple(128, 128, 128),
                    Triple(98, 122, 157),
                )) {
                    val source = createPixelBuffer(
                        width = 64,
                        height = 64,
                        colorAttachments = sourceColorAttachments,
                        pixelFormat = pixelFormat,
                    )
                    val colorImage = CIImage(
                        color = CIColor(
                            red = color.first.toDouble() / 255,
                            green = color.second.toDouble() / 255,
                            blue = color.third.toDouble() / 255,
                            colorSpace = sRGBColorSpace,
                        ),
                    ).cropped(to = CGRect(x = 0, y = 0, width = 64, height = 64))
                    processor.context.render(
                        colorImage,
                        to = source,
                        bounds = colorImage.extent,
                        colorSpace = sRGBColorSpace,
                    )
                    val output = requireNotNull(processor.createPixelBuffer(createSampleBuffer(source)))
                    val image = processor.makeCiImage(source)
                    processor.renderCoreImage(image, output, image.extent)
                    assertEquals(sdrColorAttachments(colorRange), output.colorAttachments)
                    val actual = readCenterPixel(output)
                    assertTrue(abs(actual.first - color.first) <= 3, "$color -> $actual")
                    assertTrue(abs(actual.second - color.second) <= 3, "$color -> $actual")
                    assertTrue(abs(actual.third - color.third) <= 3, "$color -> $actual")
                }
            }
        }
    }

    @Test
    fun colorRangePixelFormats() {
        assertEquals(fullRange8Bit, SettingsStreamColorRange.full.pixelFormatType())
        assertEquals(videoRange8Bit, SettingsStreamColorRange.limited.pixelFormatType())
        assertFalse(isVideoRangePixelFormat(fullRange8Bit))
        assertTrue(isVideoRangePixelFormat(videoRange8Bit))
        assertFalse(isVideoRangePixelFormat(fullRange10Bit))
        assertTrue(isVideoRangePixelFormat(videoRange10Bit))
        assertFalse(isVideoRangePixelFormat(kCVPixelFormatType_32BGRA))
        assertTrue(isFullRangePixelFormat(fullRange8Bit))
        assertFalse(isFullRangePixelFormat(videoRange8Bit))
        assertTrue(isFullRangePixelFormat(fullRange10Bit))
        assertFalse(isFullRangePixelFormat(videoRange10Bit))
        assertFalse(isFullRangePixelFormat(mjpeg))
        assertFalse(isVideoRangePixelFormat(mjpeg))
    }

    @Test
    fun cameraFormatsFollowColorRange() {
        val formats = listOf(videoRange8Bit, fullRange8Bit, videoRange10Bit).map { TestVideoFormat(it) }
        assertEquals(
            listOf(fullRange8Bit),
            filterFormatsByColorRange(formats, SettingsStreamColorRange.full).map { it.pixelFormat },
        )
        assertEquals(
            listOf(videoRange8Bit, videoRange10Bit),
            filterFormatsByColorRange(formats, SettingsStreamColorRange.limited).map { it.pixelFormat },
        )
        val fullRangeFormats = listOf(TestVideoFormat(fullRange8Bit))
        assertEquals(
            listOf(fullRange8Bit),
            filterFormatsByColorRange(fullRangeFormats, SettingsStreamColorRange.limited).map { it.pixelFormat },
        )
        val videoRangeFormats = listOf(videoRange8Bit, videoRange10Bit).map { TestVideoFormat(it) }
        assertEquals(
            listOf(videoRange10Bit),
            filterFormatsByColorRange(videoRangeFormats, SettingsStreamColorRange.full).map { it.pixelFormat },
        )
        val videoRange8BitFormats = listOf(TestVideoFormat(videoRange8Bit))
        assertEquals(
            listOf(videoRange8Bit),
            filterFormatsByColorRange(videoRange8BitFormats, SettingsStreamColorRange.full).map { it.pixelFormat },
        )
        val usbCameraFormats = listOf(videoRange8Bit, mjpeg).map { TestVideoFormat(it) }
        assertEquals(
            listOf(mjpeg),
            filterFormatsByColorRange(usbCameraFormats, SettingsStreamColorRange.full).map { it.pixelFormat },
        )
        assertEquals(
            listOf(videoRange8Bit),
            filterFormatsByColorRange(usbCameraFormats, SettingsStreamColorRange.limited).map { it.pixelFormat },
        )
    }

    @Test
    fun processorOutputPixelFormatFollowsColorRange() {
        for (testCase in listOf(
            ProcessorOutputCase(
                SettingsStreamColorRange.full,
                fullRange8Bit,
                fullRange10Bit,
                defaultColorAttachments,
            ),
            ProcessorOutputCase(
                SettingsStreamColorRange.limited,
                videoRange8Bit,
                videoRange10Bit,
                rec709ColorAttachments,
            ),
        )) {
            val processor = VideoEffectsProcessor(colorRange = testCase.colorRange)
            processor.colorSpace = AVCaptureColorSpace.sRGB
            assertEquals(testCase.pixelFormat, processor.outputPixelFormatType())
            assertEquals(testCase.colorAttachments, processor.outputColorAttachments)
            processor.colorSpace = AVCaptureColorSpace.HLG_BT2020
            assertEquals(testCase.hlgPixelFormat, processor.outputPixelFormatType())
        }
    }

    @Test
    fun encoderKeepsColorRange() {
        for ((profileLevel, pixelFormat) in listOf(
            kVTProfileLevel_H264_Main_AutoLevel to fullRange8Bit,
            kVTProfileLevel_H264_Main_AutoLevel to videoRange8Bit,
            kVTProfileLevel_HEVC_Main_AutoLevel to fullRange8Bit,
            kVTProfileLevel_HEVC_Main_AutoLevel to videoRange8Bit,
            kVTProfileLevel_HEVC_Main10_AutoLevel to fullRange10Bit,
            kVTProfileLevel_HEVC_Main10_AutoLevel to videoRange10Bit,
        )) {
            val lockQueue = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
            val tester = EncoderTester()
            val encoder = VideoEncoder(
                lockQueue = CoroutineScope(lockQueue),
                colorRange = colorRange(pixelFormat),
            )
            encoder.delegate = tester
            encoder.settings.value.videoSize = Size(320, 240)
            encoder.settings.value.bitrate = 1_000_000
            encoder.settings.value.profileLevel = profileLevel
            encoder.startRunning()
            val pixelBuffer = createPixelBuffer(
                width = 320,
                height = 240,
                colorAttachments = defaultColorAttachments,
                pixelFormat = pixelFormat,
            )
            for (frame in 0 until 10) {
                runBlocking(lockQueue) {
                    encoder.encodeImageBuffer(
                        pixelBuffer,
                        presentationTimeStamp = frame.toLong() * 1_000_000L / 30,
                        duration = 1_000_000L / 30,
                    )
                }
            }
            val formatDescriptions = tester.waitForFormatDescriptions(1)
            encoder.stopRunning()
            assertEquals(1, formatDescriptions.size)
            assertEquals(!isVideoRangePixelFormat(pixelFormat), isFullRange(formatDescriptions.first()))
        }
    }

    @Test
    fun decoderOutputsColorRange() {
        for (streamColorRange in listOf(SettingsStreamColorRange.full, SettingsStreamColorRange.limited)) {
            for (outputColorRange in listOf(SettingsStreamColorRange.full, SettingsStreamColorRange.limited)) {
                val processor = VideoEffectsProcessor(colorRange = SettingsStreamColorRange.full)
                val color = Triple(204, 51, 102)
                val sampleBuffers = encodeColorStream(streamColorRange, color, processor.context)
                val decoded = requireNotNull(decode(sampleBuffers, outputColorRange).firstOrNull())
                assertEquals(outputColorRange.pixelFormatType(), CVPixelBufferGetPixelFormatType(decoded))
                val actual = readCenterPixel(processor.makeCiImage(decoded), processor.context)
                assertTrue(abs(actual.first - color.first) <= 3, "$color -> $actual")
                assertTrue(abs(actual.second - color.second) <= 3, "$color -> $actual")
                assertTrue(abs(actual.third - color.third) <= 3, "$color -> $actual")
                assertEquals(
                    sdrYCbCrMatrix(outputColorRange),
                    decoded.colorAttachments[kCVImageBufferYCbCrMatrixKey],
                )
            }
        }
    }

    @Test
    fun hlgRenderKeepsCameraSignal() {
        for (pixelFormat in listOf(videoRange10Bit, fullRange10Bit)) {
            val processor = VideoEffectsProcessor(colorRange = SettingsStreamColorRange.full)
            processor.colorSpace = AVCaptureColorSpace.HLG_BT2020
            for (signal in listOf(
                Triple(0.0, 0.0, 0.0),
                Triple(0.75, 0.75, 0.75),
                Triple(0.2, 0.5, 0.8),
                Triple(0.9, 0.3, 0.1),
                Triple(1.0, 1.0, 1.0),
            )) {
                val source = createHlgPixelBuffer(pixelFormat)
                val signalImage = CIImage(
                    color = CIColor(red = signal.first, green = signal.second, blue = signal.third),
                ).cropped(to = CGRect(x = 0, y = 0, width = 64, height = 64))
                processor.context.render(signalImage, to = source, bounds = signalImage.extent, colorSpace = null)
                val expected = readHlgSignal(source, processor.context)
                val output = createHlgPixelBuffer(pixelFormat)
                val image = processor.makeCiImage(source)
                processor.renderCoreImage(image, output, image.extent)
                val actual = readHlgSignal(output, processor.context)
                for ((actualValue, expectedValue) in actual.zip(expected)) {
                    assertTrue(abs(actualValue - expectedValue) < 0.004, "$signal -> $actualValue != $expectedValue")
                }
            }
        }
    }

    @Test
    fun hlgRenderMapsSdrFramesToBt2100() {
        for (pixelFormat in listOf(videoRange10Bit, fullRange10Bit)) {
            val processor = VideoEffectsProcessor(colorRange = SettingsStreamColorRange.full)
            processor.colorSpace = AVCaptureColorSpace.HLG_BT2020
            for ((color, expected) in listOf(
                Triple(1.0, 1.0, 1.0) to listOf(0.7499, 0.7499, 0.7499),
                Triple(1.0, 0.0, 0.0) to listOf(0.7086, 0.2664, 0.1297),
                Triple(0.0, 0.0, 1.0) to listOf(0.2310, 0.1183, 0.8133),
            )) {
                val source = createPixelBuffer(width = 64, height = 64, colorAttachments = defaultColorAttachments)
                val colorImage = CIImage(
                    color = CIColor(
                        red = color.first,
                        green = color.second,
                        blue = color.third,
                        colorSpace = sRGBColorSpace,
                    ),
                ).cropped(to = CGRect(x = 0, y = 0, width = 64, height = 64))
                processor.context.render(
                    colorImage,
                    to = source,
                    bounds = colorImage.extent,
                    colorSpace = sRGBColorSpace,
                )
                val output = createHlgPixelBuffer(pixelFormat)
                val image = processor.makeCiImage(source)
                processor.renderCoreImage(image, output, image.extent)
                val actual = readHlgSignal(output, processor.context)
                for ((actualValue, expectedValue) in actual.zip(expected)) {
                    assertTrue(abs(actualValue - expectedValue) < 0.008, "$color -> $actualValue != $expectedValue")
                }
            }
        }
    }

    @Test
    fun hlgRenderMapsSrgbToBt2100() {
        for (pixelFormat in listOf(videoRange10Bit, fullRange10Bit)) {
            val processor = VideoEffectsProcessor(colorRange = SettingsStreamColorRange.full)
            processor.colorSpace = AVCaptureColorSpace.HLG_BT2020
            for ((color, expected) in listOf(
                Triple(1.0, 1.0, 1.0) to listOf(0.7499, 0.7499, 0.7499),
                Triple(0.5, 0.5, 0.5) to listOf(0.4689, 0.4689, 0.4689),
                Triple(1.0, 0.0, 0.0) to listOf(0.7086, 0.2664, 0.1297),
                Triple(0.0, 1.0, 0.0) to listOf(0.5248, 0.7444, 0.2719),
                Triple(0.0, 0.0, 1.0) to listOf(0.2310, 0.1183, 0.8133),
                Triple(0.878, 0.639, 0.180) to listOf(0.6724, 0.5833, 0.2511),
                Triple(0.220, 0.239, 0.588) to listOf(0.2592, 0.2483, 0.5788),
            )) {
                val image = CIImage(
                    color = CIColor(
                        red = color.first,
                        green = color.second,
                        blue = color.third,
                        colorSpace = sRGBColorSpace,
                    ),
                ).cropped(to = CGRect(x = 0, y = 0, width = 64, height = 64))
                val output = createHlgPixelBuffer(pixelFormat)
                processor.renderCoreImage(image, output, image.extent)
                val actual = readHlgSignal(output, processor.context)
                for ((actualValue, expectedValue) in actual.zip(expected)) {
                    assertTrue(abs(actualValue - expectedValue) < 0.006, "$color -> $actualValue != $expectedValue")
                }
            }
        }
    }
}

private fun isFullRange(formatDescription: MediaFormat): Boolean {
    return CMFormatDescriptionGetExtension(
        formatDescription,
        kCMFormatDescriptionExtension_FullRangeVideo,
    ) as? Boolean ?: false
}

private fun colorRange(pixelFormat: Int): SettingsStreamColorRange {
    return if (isVideoRangePixelFormat(pixelFormat)) {
        SettingsStreamColorRange.limited
    } else {
        SettingsStreamColorRange.full
    }
}

private fun encodeColorStream(
    colorRange: SettingsStreamColorRange,
    color: Triple<Int, Int, Int>,
    context: CIContext,
): List<MediaSample> {
    val lockQueue = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    val tester = EncoderTester()
    val encoder = VideoEncoder(lockQueue = CoroutineScope(lockQueue), colorRange = colorRange)
    encoder.delegate = tester
    encoder.settings.value.videoSize = Size(320, 240)
    encoder.settings.value.bitrate = 2_000_000
    encoder.startRunning()
    val source = createPixelBuffer(
        width = 320,
        height = 240,
        colorAttachments = sdrColorAttachments(colorRange),
        pixelFormat = colorRange.pixelFormatType(),
    )
    val colorImage = CIImage(
        color = CIColor(
            red = color.first.toDouble() / 255,
            green = color.second.toDouble() / 255,
            blue = color.third.toDouble() / 255,
            colorSpace = sRGBColorSpace,
        ),
    ).cropped(to = CGRect(x = 0, y = 0, width = 320, height = 240))
    context.render(colorImage, to = source, bounds = colorImage.extent, colorSpace = sRGBColorSpace)
    for (frame in 0 until 10) {
        runBlocking(lockQueue) {
            encoder.encodeImageBuffer(
                source,
                presentationTimeStamp = frame.toLong() * 1_000_000L / 30,
                duration = 1_000_000L / 30,
            )
        }
    }
    val sampleBuffers = tester.waitForSampleBuffers(10)
    encoder.stopRunning()
    return sampleBuffers
}

private fun decode(
    sampleBuffers: List<MediaSample>,
    colorRange: SettingsStreamColorRange,
): List<CVPixelBuffer> {
    val lockQueue = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    val tester = DecoderTester()
    val decoder = VideoDecoder(
        name = "test",
        lockQueue = CoroutineScope(lockQueue),
        softwareDecoding = false,
        colorRange = colorRange,
    )
    decoder.delegate = tester
    runBlocking(lockQueue) {
        decoder.startRunning(sampleBuffers.firstOrNull()?.format)
        for (sampleBuffer in sampleBuffers) {
            decoder.decodeSampleBuffer(sampleBuffer)
        }
    }
    val imageBuffers = tester.waitForImageBuffers(sampleBuffers.size)
    runBlocking(lockQueue) {
        decoder.stopRunning()
    }
    return imageBuffers
}

private fun createHlgPixelBuffer(pixelFormat: Int): CVPixelBuffer {
    val pixelBuffer = CVPixelBufferCreate(
        null,
        64,
        64,
        pixelFormat,
        mapOf(kCVPixelBufferIOSurfacePropertiesKey to emptyMap<String, Any>()),
    )!!
    CVBufferSetAttachments(pixelBuffer, hlgColorAttachments, CVAttachmentMode.shouldPropagate)
    return pixelBuffer
}

private fun readHlgSignal(pixelBuffer: CVPixelBuffer, context: CIContext): List<Double> {
    val image = CIImage(cvPixelBuffer = pixelBuffer, options = mapOf(CIImageOption.colorSpace to NSNull()))
    val pixel = FloatArray(4)
    context.render(
        image,
        toBitmap = pixel,
        rowBytes = 16,
        bounds = CGRect(x = 32, y = 32, width = 1, height = 1),
        format = CIFormat.RGBAf,
        colorSpace = null,
    )
    return (0 until 3).map { pixel[it].toDouble() }
}

private fun colorExtensions(formatDescription: MediaFormat): Map<String, String> {
    val extensions = mutableMapOf<String, String>()
    for (key in listOf(
        kCMFormatDescriptionExtension_ColorPrimaries,
        kCMFormatDescriptionExtension_TransferFunction,
        kCMFormatDescriptionExtension_YCbCrMatrix,
    )) {
        val value = CMFormatDescriptionGetExtension(formatDescription, key) as? String
        if (value != null) {
            extensions[key] = value
        }
    }
    return extensions
}

private fun createPixelBuffer(
    width: Int,
    height: Int,
    colorAttachments: Map<String, String>,
    pixelFormat: Int = kCVPixelFormatType_420YpCbCr8BiPlanarFullRange,
): CVPixelBuffer {
    val pixelBuffer = CVPixelBufferCreate(
        null,
        width,
        height,
        pixelFormat,
        mapOf(kCVPixelBufferIOSurfacePropertiesKey to emptyMap<String, Any>()),
    )!!
    CVBufferSetAttachments(pixelBuffer, colorAttachments, CVAttachmentMode.shouldPropagate)
    return pixelBuffer
}

private fun createSampleBuffer(pixelBuffer: CVPixelBuffer): MediaSample {
    val formatDescription = CMVideoFormatDescriptionCreateForImageBuffer(pixelBuffer)
    val timingInfo = CMSampleTimingInfo(
        duration = 1L * 1_000_000L / 30,
        presentationTimeStamp = 7L * 1_000_000L / 30,
        decodeTimeStamp = kCMTimeInvalidUs,
    )
    return CMSampleBufferCreateForImageBuffer(
        null,
        pixelBuffer,
        true,
        null,
        null,
        formatDescription,
        timingInfo,
    )!!
}

private fun readCenterPixel(pixelBuffer: CVPixelBuffer): Triple<Int, Int, Int> {
    CVPixelBufferLockBaseAddress(pixelBuffer, CVPixelBufferLockFlags.readOnly)
    try {
        val x = CVPixelBufferGetWidth(pixelBuffer) / 2
        val y = CVPixelBufferGetHeight(pixelBuffer) / 2
        val lumaPlane = CVPixelBufferGetBaseAddressOfPlane(pixelBuffer, 0)!!
        val chromaPlane = CVPixelBufferGetBaseAddressOfPlane(pixelBuffer, 1)!!
        val luma = (lumaPlane.get(
            y * CVPixelBufferGetBytesPerRowOfPlane(pixelBuffer, 0) + x,
        ).toInt() and 0xFF).toDouble()
        val chromaOffset = (y / 2) * CVPixelBufferGetBytesPerRowOfPlane(pixelBuffer, 1) + (x / 2) * 2
        val blueDifference = (chromaPlane.get(chromaOffset).toInt() and 0xFF).toDouble() - 128
        val redDifference = (chromaPlane.get(chromaOffset + 1).toInt() and 0xFF).toDouble() - 128
        val (kr, kb) =
            if (pixelBuffer.colorAttachments[kCVImageBufferYCbCrMatrixKey] == kCVImageBufferYCbCrMatrix_ITU_R_709_2) {
                0.2126 to 0.0722
            } else {
                0.299 to 0.114
            }
        val (yScale, cScale, yOffset) =
            if (isVideoRangePixelFormat(CVPixelBufferGetPixelFormatType(pixelBuffer))) {
                Triple(219.0, 224.0, 16.0)
            } else {
                Triple(255.0, 255.0, 0.0)
            }
        val yNormalized = (luma - yOffset) / yScale
        val red = yNormalized + 2 * (1 - kr) * redDifference / cScale
        val blue = yNormalized + 2 * (1 - kb) * blueDifference / cScale
        val green = (yNormalized - kr * red - kb * blue) / (1 - kr - kb)
        return Triple((255 * red).roundToInt(), (255 * green).roundToInt(), (255 * blue).roundToInt())
    } finally {
        CVPixelBufferUnlockBaseAddress(pixelBuffer, CVPixelBufferLockFlags.readOnly)
    }
}

private fun readCenterPixel(image: CIImage, context: CIContext): Triple<Int, Int, Int> {
    val pixel = ByteArray(4)
    context.render(
        image,
        toBitmap = pixel,
        rowBytes = 4,
        bounds = CGRect(x = image.extent.midX, y = image.extent.midY, width = 1, height = 1),
        format = CIFormat.RGBA8,
        colorSpace = sRGBColorSpace,
    )
    return Triple(pixel[0].toInt() and 0xFF, pixel[1].toInt() and 0xFF, pixel[2].toInt() and 0xFF)
}
