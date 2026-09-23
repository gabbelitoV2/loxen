package com.moblin.android.videoeffects

import android.graphics.Bitmap
import com.moblin.android.localized
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.Bundle
import com.moblin.android.platform.coregraphics.CGColorSpaceCreateDeviceRGB
import com.moblin.android.platform.coregraphics.CGContext
import com.moblin.android.platform.coregraphics.CGImageAlphaInfo
import com.moblin.android.platform.coreimage.CIColorCubeWithColorSpace
import com.moblin.android.platform.coreimage.CIFilter
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIColorLookupFilter
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.metalpetal.MTKTextureLoader
import com.moblin.android.platform.simd.SIMD3
import com.moblin.android.platform.swiftcube.LutEntry
import com.moblin.android.platform.swiftcube.SC3DLut
import com.moblin.android.platform.swiftcube.SwiftCubeError
import com.moblin.android.platform.uikit.UIImage
import com.moblin.android.platform.uikit.cgImage
import com.moblin.android.platform.uikit.scale
import com.moblin.android.platform.uikit.size
import com.moblin.android.various.settings.SettingsColorLut
import com.moblin.android.various.settings.SettingsColorLutType
import com.moblin.android.various.storages.ImageStorage
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import kotlin.math.cbrt
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import com.moblin.android.platform.coregraphics.bitsPerComponent
import com.moblin.android.platform.coregraphics.bitsPerPixel
import com.moblin.android.platform.coregraphics.dataProvider

private val loaderQueue = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

fun interpolate3d(at: SIMD3, lut: List<SIMD3>, dimension: Int): SIMD3 {
    val point = at
    val maxIndex = (dimension - 1).toFloat()
    val x = min(max(point.x * maxIndex, 0f), maxIndex)
    val y = min(max(point.y * maxIndex, 0f), maxIndex)
    val z = min(max(point.z * maxIndex, 0f), maxIndex)
    val x0 = floor(x).toInt()
    val x1 = min(x0 + 1, dimension - 1)
    val y0 = floor(y).toInt()
    val y1 = min(y0 + 1, dimension - 1)
    val z0 = floor(z).toInt()
    val z1 = min(z0 + 1, dimension - 1)
    val xd = x - x0.toFloat()
    val yd = y - y0.toFloat()
    val zd = z - z0.toFloat()
    val c00 = lut[x0 * dimension * dimension + y0 * dimension + z0] * (1 - xd) +
        lut[x1 * dimension * dimension + y0 * dimension + z0] * xd
    val c01 = lut[x0 * dimension * dimension + y0 * dimension + z1] * (1 - xd) +
        lut[x1 * dimension * dimension + y0 * dimension + z1] * xd
    val c10 = lut[x0 * dimension * dimension + y1 * dimension + z0] * (1 - xd) +
        lut[x1 * dimension * dimension + y1 * dimension + z0] * xd
    val c11 = lut[x0 * dimension * dimension + y1 * dimension + z1] * (1 - xd) +
        lut[x1 * dimension * dimension + y1 * dimension + z1] * xd
    val c0 = c00 * (1 - yd) + c10 * yd
    val c1 = c01 * (1 - yd) + c11 * yd
    return c0 * (1 - zd) + c1 * zd
}

fun convertLutTo64(bigLut: List<SIMD3>, bigDimension: Int): List<SIMD3> {
    val newPoints = List(64) { it / (64 - 1).toDouble() }
    val lut64 = MutableList(64 * 64 * 64) { SIMD3(0f, 0f, 0f) }
    for (i in 0 until 64) {
        for (j in 0 until 64) {
            for (k in 0 until 64) {
                val point = SIMD3(newPoints[i], newPoints[j], newPoints[k])
                lut64[i * 64 * 64 + j * 64 + k] = interpolate3d(
                    at = point,
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

fun lutEffectConvertLut(image: Bitmap): Pair<Float, ByteArray> {
    val width = image.size.width * image.scale
    val height = image.size.height * image.scale
    val dimension = cbrt(width * height).toInt()
    if (!(dimension > 0 && width.toInt() % dimension == 0 && height.toInt() % dimension == 0)) {
        throw LutLoadError(localized("LUT image is not a cube"))
    }
    if (dimension * dimension * dimension != (width * height).toInt()) {
        throw LutLoadError(localized("LUT image is not a cube"))
    }
    val cgImage = image.cgImage
    val data = cgImage.dataProvider?.data
        ?: throw LutLoadError(localized("Failed to get LUT data"))
    val length = data.size
    val pixels: FloatArray
    if (cgImage.bitsPerComponent == 8) {
        pixels = FloatArray(length) { ((data[it].toInt() and 0xFF) / 255.0).toFloat() }
    } else if (cgImage.bitsPerComponent == 16) {
        val count = length / 2
        pixels = FloatArray(count) {
            val value = (data[2 * it].toInt() and 0xFF) or ((data[2 * it + 1].toInt() and 0xFF) shl 8)
            (value / 65535.0).toFloat()
        }
    } else {
        throw LutLoadError(localized("LUT image is not 8 or 16 bits per pixel component"))
    }
    val numberOfPixels = (width * height).toInt()
    val numberOutputOfComponents = numberOfPixels * 4
    val cube = FloatArray(numberOutputOfComponents)
    val componentsPerPixel = cgImage.bitsPerPixel / cgImage.bitsPerComponent
    if (!(componentsPerPixel == 3 || componentsPerPixel == 4)) {
        throw LutLoadError(localized("LUT image is not 3 or 4 components per pixel"))
    }
    val hasAlpha = componentsPerPixel == 4
    val rows = height.toInt() / dimension
    val columns = width.toInt() / dimension
    var cubeIndex = 0
    for (row in 0 until rows) {
        for (column in 0 until columns) {
            for (lr in 0 until dimension) {
                val rowStrides = width.toInt() * (row * dimension + lr) * componentsPerPixel
                val columnStrides = column * dimension * componentsPerPixel
                var index = rowStrides + columnStrides
                for (n in 0 until dimension) {
                    cube[cubeIndex] = pixels[index]
                    cubeIndex += 1
                    index += 1
                    cube[cubeIndex] = pixels[index]
                    cubeIndex += 1
                    index += 1
                    cube[cubeIndex] = pixels[index]
                    cubeIndex += 1
                    index += 1
                    if (hasAlpha) {
                        cube[cubeIndex] = pixels[index]
                        index += 1
                    } else {
                        cube[cubeIndex] = 1.0f
                    }
                    cubeIndex += 1
                }
            }
        }
    }
    if (dimension <= 64) {
        return Pair(dimension.toFloat(), lutFloatsToData(cube))
    }
    val bigLut = (0 until numberOutputOfComponents step 4).map { index ->
        SIMD3(cube[index], cube[index + 1], cube[index + 2])
    }
    val lut64 = convertLutTo64(bigLut = bigLut, bigDimension = dimension)
    return Pair(64f, makeCubeData(lut64.map { entry -> LutEntry(red = entry.x, green = entry.y, blue = entry.z) }))
}

fun makeLutCgImage(dimension: Int, cubeData: ByteArray): Bitmap? {
    val pixelsCount = dimension * dimension * dimension
    if (cubeData.size != pixelsCount * 4 * 4) {
        return null
    }
    val cube = lutDataToFloats(cubeData)
    val pixels = ByteArray(pixelsCount * 4)
    for (blue in 0 until dimension) {
        for (green in 0 until dimension) {
            for (red in 0 until dimension) {
                val cubeIndex = 4 * ((blue * dimension + green) * dimension + red)
                val pixelIndex = 4 * (green * dimension * dimension + blue * dimension + red)
                for (component in 0 until 4) {
                    val rounded = floor(cube[cubeIndex + component] * 255f + 0.5f)
                    pixels[pixelIndex + component] = min(max(rounded, 0f), 255f).toInt().toByte()
                }
            }
        }
    }
    val context = CGContext(
        data = pixels,
        width = dimension * dimension,
        height = dimension,
        bitsPerComponent = 8,
        bytesPerRow = dimension * dimension * 4,
        space = CGColorSpaceCreateDeviceRGB(),
        bitmapInfo = CGImageAlphaInfo.premultipliedLast.rawValue,
    )
    return context?.makeImage()
}

private fun makeLutImage(dimension: Int, cubeData: ByteArray): MTIImage? {
    val cgImage = makeLutCgImage(dimension = dimension, cubeData = cubeData) ?: return null
    return MTIImage(cgImage = cgImage, options = mapOf(MTKTextureLoader.Option.SRGB to false), isOpaque = true)
}

fun makeCubeData(entries: List<LutEntry>): ByteArray {
    val cube = FloatArray(entries.size * 4)
    var index = 0
    for (entry in entries) {
        cube[index] = entry.red
        index += 1
        cube[index] = entry.green
        index += 1
        cube[index] = entry.blue
        index += 1
        cube[index] = 1f
        index += 1
    }
    return lutFloatsToData(cube)
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
            } catch (error: Throwable) {
                val subTitle = when (error) {
                    is SwiftCubeError.couldNotDecodeData -> "Not a text file"
                    is SwiftCubeError.sizeMissing -> "Size missing"
                    is SwiftCubeError.sizeTooBig -> "Size ${error.size} too big"
                    is SwiftCubeError.oneDimensionalLutNotSupported -> "One dimensional LUT not supported"
                    is SwiftCubeError.unsupportedKey -> "Unsupported key ${error.key}"
                    is SwiftCubeError.invalidType -> "Invalid type"
                    is SwiftCubeError.typeMissing -> "Type missing"
                    is SwiftCubeError.invalidDataPoint -> "Invalid data point ${error.point}"
                    is SwiftCubeError.wrongNumberOfDataPoints -> "Wrong number of data points ${error.count}"
                    is SwiftCubeError.invalidSyntax -> "Invalid syntax ${error.text}"
                    else -> "$error"
                }
                val title = when (lut?.type) {
                    SettingsColorLutType.bundled -> localized("Failed to load bundled file")
                    SettingsColorLutType.disk -> localized("Failed to load .png file")
                    SettingsColorLutType.diskCube -> localized("Failed to load .cube file")
                    else -> ""
                }
                CoroutineScope(Dispatchers.Main.immediate).launch {
                    onError(title, subTitle)
                }
            }
        }
    }

    override fun isEnabled(): Boolean {
        return filter != null
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        filter?.inputImage = image
        return filter?.outputImage ?: image
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
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
                SettingsColorLutType.diskCube -> loadDiskCubeLut(lut = lut, imageStorage = imageStorage)
            }
        } else {
            processorPipelineQueue.launch {
                filter = null
                filterMetalPetal.inputColorLookupTable = null
            }
        }
    }

    private fun loadBundledPngLut(lut: SettingsColorLut) {
        val path = Bundle.url("LUTs.bundle/${lut.name}.png") ?: return
        val image = UIImage(contentsOfFile = path) ?: return
        loadImageLut(image = image)
    }

    private fun loadDiskPngLut(lut: SettingsColorLut, imageStorage: ImageStorage) {
        val data = imageStorage.makePath(id = lut.id).readBytes()
        val image = UIImage(data = data) ?: throw LutLoadError(localized("Failed to create LUT image"))
        loadImageLut(image = image)
    }

    private fun loadDiskCubeLut(lut: SettingsColorLut, imageStorage: ImageStorage) {
        val sc3dLut = lutEffectConvertCube(data = imageStorage.makePath(id = lut.id).readBytes())
        val filter = sc3dLut.ciFilter()
        val lutImage = makeLutImage(dimension = sc3dLut.size, cubeData = makeCubeData(sc3dLut.entries))
        processorPipelineQueue.launch {
            this@LutEffect.filter = filter
            filterMetalPetal.inputColorLookupTable = lutImage
        }
    }

    private fun loadImageLut(image: Bitmap) {
        val (dimension, data) = lutEffectConvertLut(image = image)
        val filter = CIFilter.colorCubeWithColorSpace()
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

private class LutLoadError(message: String) : Exception(message) {
    override fun toString(): String = message ?: ""
}

private fun lutFloatsToData(values: FloatArray): ByteArray {
    val buffer = ByteBuffer.allocate(values.size * 4).order(ByteOrder.LITTLE_ENDIAN)
    for (value in values) {
        buffer.putFloat(value)
    }
    return buffer.array()
}

private fun lutDataToFloats(data: ByteArray): FloatArray {
    val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
    return FloatArray(data.size / 4) { buffer.getFloat() }
}
