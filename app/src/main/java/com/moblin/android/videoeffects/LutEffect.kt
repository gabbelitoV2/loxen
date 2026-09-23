package com.moblin.android.videoeffects

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.moblin.android.platform.video.CVPixelBuffer as Image
import com.moblin.android.localized
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.coregraphics.CGColorSpaceCreateDeviceRGB
import com.moblin.android.platform.coreimage.CIColorCubeWithColorSpace
import com.moblin.android.platform.swiftcube.LutEntry
import com.moblin.android.platform.swiftcube.SC3DLut
import com.moblin.android.platform.swiftcube.SwiftCubeError
import com.moblin.android.various.settings.SettingsColorLut
import com.moblin.android.various.settings.SettingsColorLutType
import com.moblin.android.various.storages.ImageStorage
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cbrt
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val loaderQueue = CoroutineScope(Dispatchers.IO)

private val mainScope = CoroutineScope(Dispatchers.Main)

data class SIMD3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: SIMD3): SIMD3 {
        return SIMD3(x + other.x, y + other.y, z + other.z)
    }

    operator fun times(value: Float): SIMD3 {
        return SIMD3(x * value, y * value, z * value)
    }
}

class MTIColorLookupFilter {
    var inputImage: Image? = null

    var inputColorLookupTable: Bitmap? = null

    val outputImage: Image?
        get() = null
}

fun interpolate3d(point: SIMD3, lut: List<SIMD3>, dimension: Int): SIMD3 {
    val maxIndex = (dimension - 1).toFloat()
    val x = minOf(maxOf(point.x * maxIndex, 0f), maxIndex)
    val y = minOf(maxOf(point.y * maxIndex, 0f), maxIndex)
    val z = minOf(maxOf(point.z * maxIndex, 0f), maxIndex)
    val x0 = floor(x).toInt()
    val x1 = minOf(x0 + 1, dimension - 1)
    val y0 = floor(y).toInt()
    val y1 = minOf(y0 + 1, dimension - 1)
    val z0 = floor(z).toInt()
    val z1 = minOf(z0 + 1, dimension - 1)
    val xd = x - x0.toFloat()
    val yd = y - y0.toFloat()
    val zd = z - z0.toFloat()
    val c00 = lut[x0 * dimension * dimension + y0 * dimension + z0] * (1f - xd) +
        lut[x1 * dimension * dimension + y0 * dimension + z0] * xd
    val c01 = lut[x0 * dimension * dimension + y0 * dimension + z1] * (1f - xd) +
        lut[x1 * dimension * dimension + y0 * dimension + z1] * xd
    val c10 = lut[x0 * dimension * dimension + y1 * dimension + z0] * (1f - xd) +
        lut[x1 * dimension * dimension + y1 * dimension + z0] * xd
    val c11 = lut[x0 * dimension * dimension + y1 * dimension + z1] * (1f - xd) +
        lut[x1 * dimension * dimension + y1 * dimension + z1] * xd
    val c0 = c00 * (1f - yd) + c10 * yd
    val c1 = c01 * (1f - yd) + c11 * yd
    return c0 * (1f - zd) + c1 * zd
}

fun convertLutTo64(bigLut: List<SIMD3>, bigDimension: Int): List<SIMD3> {
    val newPoints = FloatArray(64) { it / 63f }
    val lut64 = MutableList(64 * 64 * 64) { SIMD3(0f, 0f, 0f) }
    for (i in 0 until 64) {
        for (j in 0 until 64) {
            for (k in 0 until 64) {
                val point = SIMD3(newPoints[i], newPoints[j], newPoints[k])
                lut64[i * 64 * 64 + j * 64 + k] = interpolate3d(
                    point = point,
                    lut = bigLut,
                    dimension = bigDimension,
                )
            }
        }
    }
    return lut64
}

fun lutEffectConvertCube(data: ByteArray): SC3DLut {
    val sc3dLut = SC3DLut(fileData = data)
    if (sc3dLut.size > 64) {
        val bigLut = sc3dLut.entries.map { entry -> SIMD3(entry.red, entry.green, entry.blue) }
        sc3dLut.entries = convertLutTo64(bigLut = bigLut, bigDimension = sc3dLut.size).map { entry ->
            LutEntry(red = entry.x, green = entry.y, blue = entry.z)
        }
        sc3dLut.size = 64
    }
    return sc3dLut
}

private fun halfToFloat(bits: Int): Float {
    val sign = bits and 0x8000
    val exponent = (bits shr 10) and 0x1F
    val mantissa = bits and 0x3FF
    val floatSign = sign shl 16
    return when {
        exponent == 0 && mantissa == 0 -> Float.fromBits(floatSign)
        exponent == 0 -> {
            var m = mantissa
            var e = -1
            while ((m and 0x400) == 0) {
                m = m shl 1
                e += 1
            }
            m = m and 0x3FF
            val floatExponent = (127 - 15 - e) shl 23
            Float.fromBits(floatSign or floatExponent or (m shl 13))
        }
        exponent == 0x1F -> Float.fromBits(floatSign or 0x7F800000 or (mantissa shl 13))
        else -> Float.fromBits(
            floatSign or ((exponent - 15 + 127) shl 23) or (mantissa shl 13)
        )
    }
}

private val pngSignature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

private fun pngHasTransparency(data: ByteArray): Boolean {
    var offset = 8
    while (offset + 8 <= data.size) {
        val length = ByteBuffer.wrap(data, offset, 4).int
        val type = String(data, offset + 4, 4, Charsets.ISO_8859_1)
        if (type == "tRNS") {
            return true
        }
        if (type == "IDAT" || length < 0 || length > data.size - offset - 12) {
            return false
        }
        offset += 12 + length
    }
    return false
}

fun pngComponentsPerPixel(data: ByteArray): Int {
    if (data.size < 26 || !data.copyOfRange(0, 8).contentEquals(pngSignature)) {
        return 4
    }
    return when (data[25].toInt()) {
        0 -> 1
        2 -> 3
        3 -> if (pngHasTransparency(data)) 4 else 1
        4 -> 2
        else -> 4
    }
}

fun lutEffectConvertLut(image: Bitmap, imageComponentsPerPixel: Int = 4): Pair<Float, ByteArray> {
    val width = image.width
    val height = image.height
    val dimension = cbrt((width * height).toDouble()).toInt()
    if (dimension <= 0 || width % dimension != 0 || height % dimension != 0) {
        throw Exception(localized("LUT image is not a cube"))
    }
    if (dimension * dimension * dimension != width * height) {
        throw Exception(localized("LUT image is not a cube"))
    }
    val componentsPerPixel = 4
    val pixels: FloatArray = when (image.config) {
        Bitmap.Config.ARGB_8888 -> {
            val argb = IntArray(width * height)
            image.getPixels(argb, 0, width, 0, 0, width, height)
            val result = FloatArray(argb.size * 4)
            var index = 0
            for (pixel in argb) {
                result[index++] = ((pixel shr 16) and 0xFF) / 255f
                result[index++] = ((pixel shr 8) and 0xFF) / 255f
                result[index++] = (pixel and 0xFF) / 255f
                result[index++] = ((pixel ushr 24) and 0xFF) / 255f
            }
            result
        }
        Bitmap.Config.RGBA_F16 -> {
            val buffer = ByteBuffer.allocate(image.byteCount).order(ByteOrder.nativeOrder())
            image.copyPixelsToBuffer(buffer)
            buffer.rewind()
            val shorts = buffer.asShortBuffer()
            val result = FloatArray(shorts.remaining())
            var index = 0
            while (shorts.hasRemaining()) {
                result[index++] = halfToFloat(shorts.get().toInt() and 0xFFFF)
            }
            result
        }
        else -> throw Exception(localized("LUT image is not 8 or 16 bits per pixel component"))
    }
    if (imageComponentsPerPixel != 3 && imageComponentsPerPixel != 4) {
        throw Exception(localized("LUT image is not 3 or 4 components per pixel"))
    }
    val hasAlpha = componentsPerPixel == 4
    val numberOfPixels = width * height
    val numberOutputOfComponents = numberOfPixels * 4
    val cube = FloatArray(numberOutputOfComponents)
    val rows = height / dimension
    val columns = width / dimension
    var cubeIndex = 0
    for (row in 0 until rows) {
        for (column in 0 until columns) {
            for (lr in 0 until dimension) {
                val rowStrides = width * (row * dimension + lr) * componentsPerPixel
                val columnStrides = column * dimension * componentsPerPixel
                var index = rowStrides + columnStrides
                for (n in 0 until dimension) {
                    cube[cubeIndex++] = pixels[index]
                    index += 1
                    cube[cubeIndex++] = pixels[index]
                    index += 1
                    cube[cubeIndex++] = pixels[index]
                    index += 1
                    if (hasAlpha) {
                        cube[cubeIndex++] = pixels[index]
                        index += 1
                    } else {
                        cube[cubeIndex++] = 1.0f
                    }
                }
            }
        }
    }
    if (dimension > 64) {
        val bigLut = (0 until numberOutputOfComponents step 4).map { index ->
            SIMD3(cube[index], cube[index + 1], cube[index + 2])
        }
        val lut64 = convertLutTo64(bigLut = bigLut, bigDimension = dimension)
        return Pair(
            64f,
            makeCubeData(
                lut64.map { entry -> LutEntry(red = entry.x, green = entry.y, blue = entry.z) },
            ),
        )
    }
    return Pair(dimension.toFloat(), floatArrayToByteArray(cube))
}

fun makeLutCgImage(dimension: Int, cubeData: ByteArray): Bitmap? {
    val pixelsCount = dimension * dimension * dimension
    if (cubeData.size != pixelsCount * 4 * 4) {
        return null
    }
    val pixels = IntArray(pixelsCount)
    val cube = ByteBuffer.wrap(cubeData).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
    for (blue in 0 until dimension) {
        for (green in 0 until dimension) {
            for (red in 0 until dimension) {
                val cubeIndex = 4 * ((blue * dimension + green) * dimension + red)
                val pixelIndex = green * dimension * dimension + blue * dimension + red
                val components = IntArray(4) { component ->
                    minOf(maxOf((cube[cubeIndex + component] * 255f).roundToInt(), 0), 255)
                }
                pixels[pixelIndex] = (components[3] shl 24) or (components[0] shl 16) or
                    (components[1] shl 8) or components[2]
            }
        }
    }
    val bitmap = Bitmap.createBitmap(
        dimension * dimension,
        dimension,
        Bitmap.Config.ARGB_8888,
    )
    bitmap.isPremultiplied = false
    bitmap.setPixels(pixels, 0, dimension * dimension, 0, 0, dimension * dimension, dimension)
    return bitmap
}

private fun makeLutImage(dimension: Int, cubeData: ByteArray): Bitmap? {
    val cgImage = makeLutCgImage(dimension = dimension, cubeData = cubeData) ?: return null
    return cgImage
}

fun makeCubeData(entries: List<LutEntry>): ByteArray {
    val cube = FloatArray(entries.size * 4)
    var index = 0
    for (entry in entries) {
        cube[index++] = entry.red
        cube[index++] = entry.green
        cube[index++] = entry.blue
        cube[index++] = 1f
    }
    return floatArrayToByteArray(cube)
}

private fun floatArrayToByteArray(values: FloatArray): ByteArray {
    val buffer = ByteBuffer.allocate(values.size * 4).order(ByteOrder.LITTLE_ENDIAN)
    buffer.asFloatBuffer().put(values)
    return buffer.array()
}

class LutEffect : VideoEffect() {
    private var filter: CIColorCubeWithColorSpace? = null
    private val filterMetalPetal = MTIColorLookupFilter()

    fun setLut(
        lut: SettingsColorLut?,
        imageStorage: ImageStorage,
        onError: (String, String?) -> Unit,
    ) {
        loaderQueue.launch {
            try {
                loadLut(lut = lut, imageStorage = imageStorage)
            } catch (error: Exception) {
                val subTitle = when (error) {
                    SwiftCubeError.couldNotDecodeData -> "Not a text file"
                    SwiftCubeError.sizeMissing -> "Size missing"
                    is SwiftCubeError.sizeTooBig -> "Size ${error.size} too big"
                    SwiftCubeError.oneDimensionalLutNotSupported ->
                        "One dimensional LUT not supported"
                    is SwiftCubeError.unsupportedKey -> "Unsupported key ${error.key}"
                    SwiftCubeError.invalidType -> "Invalid type"
                    SwiftCubeError.typeMissing -> "Type missing"
                    is SwiftCubeError.invalidDataPoint -> "Invalid data point ${error.point}"
                    is SwiftCubeError.wrongNumberOfDataPoints ->
                        "Wrong number of data points ${error.count}"
                    is SwiftCubeError.invalidSyntax -> "Invalid syntax ${error.text}"
                    else -> "$error"
                }
                val title = when (lut?.type) {
                    SettingsColorLutType.bundled -> localized("Failed to load bundled file")
                    SettingsColorLutType.disk -> localized("Failed to load .png file")
                    SettingsColorLutType.diskCube -> localized("Failed to load .cube file")
                    null -> ""
                }
                mainScope.launch {
                    onError(title, subTitle)
                }
            }
        }
    }

    override fun isEnabled(): Boolean {
        return filter != null
    }

    override fun execute(image: Image, info: VideoEffectInfo): Image {
        return image
    }

    override fun executeMetalPetal(image: Image, info: VideoEffectInfo): Image {
        if (filterMetalPetal.inputColorLookupTable == null) {
            return image
        }
        filterMetalPetal.inputImage = image
        return filterMetalPetal.outputImage ?: image
    }

    private fun loadLut(lut: SettingsColorLut?, imageStorage: ImageStorage) {
        if (lut != null) {
            when (lut.type) {
                SettingsColorLutType.bundled -> loadBundledPngLut(lut = lut)
                SettingsColorLutType.disk -> loadDiskPngLut(lut = lut, imageStorage = imageStorage)
                SettingsColorLutType.diskCube -> loadDiskCubeLut(
                    lut = lut,
                    imageStorage = imageStorage,
                )
            }
        } else {
            processorPipelineQueue.launch {
                filter = null
                filterMetalPetal.inputColorLookupTable = null
            }
        }
    }

    private fun loadBundledPngLut(lut: SettingsColorLut) {
        Unit
    }

    private fun loadDiskPngLut(lut: SettingsColorLut, imageStorage: ImageStorage) {
        val data = imageStorage.makePath(id = lut.id).readBytes()
        val image = BitmapFactory.decodeByteArray(data, 0, data.size)
            ?: throw Exception(localized("Failed to create LUT image"))
        loadImageLut(image = image, componentsPerPixel = pngComponentsPerPixel(data))
    }

    private fun loadDiskCubeLut(lut: SettingsColorLut, imageStorage: ImageStorage) {
        val sc3dLut = lutEffectConvertCube(
            imageStorage.makePath(id = lut.id).readBytes(),
        )
        val filter = sc3dLut.ciFilter()
        val lutImage = makeLutImage(
            dimension = sc3dLut.size,
            cubeData = makeCubeData(sc3dLut.entries),
        )
        processorPipelineQueue.launch {
            this@LutEffect.filter = filter
            filterMetalPetal.inputColorLookupTable = lutImage
        }
    }

    private fun loadImageLut(image: Bitmap, componentsPerPixel: Int) {
        val (dimension, data) = lutEffectConvertLut(image = image, imageComponentsPerPixel = componentsPerPixel)
        val filter = CIColorCubeWithColorSpace()
        filter.cubeData = data
        filter.cubeDimension = dimension
        filter.colorSpace = CGColorSpaceCreateDeviceRGB()
        val lutImage = makeLutImage(dimension = dimension.toInt(), cubeData = data)
        processorPipelineQueue.launch {
            this@LutEffect.filter = filter
            filterMetalPetal.inputColorLookupTable = lutImage
        }
    }
}
