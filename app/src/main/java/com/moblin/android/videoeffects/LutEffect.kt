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
import com.moblin.android.platform.simd.SIMD4
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

fun interpolate3d(at: SIMD3, dimension: Int, lut: (Int, Int, Int) -> SIMD3): SIMD3 {
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
    val c00 = lut(x0, y0, z0) * (1 - xd) + lut(x1, y0, z0) * xd
    val c01 = lut(x0, y0, z1) * (1 - xd) + lut(x1, y0, z1) * xd
    val c10 = lut(x0, y1, z0) * (1 - xd) + lut(x1, y1, z0) * xd
    val c11 = lut(x0, y1, z1) * (1 - xd) + lut(x1, y1, z1) * xd
    val c0 = c00 * (1 - yd) + c10 * yd
    val c1 = c01 * (1 - yd) + c11 * yd
    return c0 * (1 - zd) + c1 * zd
}

fun makeLutCube(dimension: Int, lut: (Int, Int, Int) -> SIMD3): Pair<Float, ByteArray> {
    val newDimension = min(dimension, 64)
    val cube = MutableList(newDimension * newDimension * newDimension) { SIMD4.zero }
    var index = 0
    for (blue in 0 until newDimension) {
        for (green in 0 until newDimension) {
            for (red in 0 until newDimension) {
                val entry: SIMD3
                if (newDimension == dimension) {
                    entry = lut(red, green, blue)
                } else {
                    val point = SIMD3(red.toFloat(), green.toFloat(), blue.toFloat()) / (newDimension - 1).toFloat()
                    entry = interpolate3d(at = point, dimension = dimension, lut = lut)
                }
                cube[index] = SIMD4(entry.x, entry.y, entry.z, 1f)
                index += 1
            }
        }
    }
    val values = FloatArray(cube.size * 4)
    var valueIndex = 0
    for (entry in cube) {
        values[valueIndex] = entry.x
        valueIndex += 1
        values[valueIndex] = entry.y
        valueIndex += 1
        values[valueIndex] = entry.z
        valueIndex += 1
        values[valueIndex] = entry.w
        valueIndex += 1
    }
    return Pair(newDimension.toFloat(), lutFloatsToData(values))
}

fun lutEffectConvertCube(data: ByteArray): Pair<Float, ByteArray> {
    val lut = SC3DLut(fileData = data)
    val dimension = lut.size
    return makeLutCube(dimension = dimension) { red, green, blue ->
        val entry = lut.entries[(blue * dimension + green) * dimension + red]
        SIMD3(entry.red, entry.green, entry.blue)
    }
}

fun lutEffectConvertLut(image: Bitmap): Pair<Float, ByteArray> {
    val width = (image.size.width * image.scale).toInt()
    val height = (image.size.height * image.scale).toInt()
    val dimension = cbrt((width * height).toDouble()).toInt()
    if (!(dimension > 0 && width % dimension == 0 && height % dimension == 0)) {
        throw LutLoadError(localized("LUT image is not a cube"))
    }
    if (dimension * dimension * dimension != width * height) {
        throw LutLoadError(localized("LUT image is not a cube"))
    }
    val cgImage = image.cgImage
    val data = cgImage.dataProvider?.data
        ?: throw LutLoadError(localized("Failed to get LUT pixels"))
    val bytes = data
    if (!(cgImage.bitsPerComponent == 8 || cgImage.bitsPerComponent == 16)) {
        throw LutLoadError(localized("LUT image is not 8 or 16 bits per pixel component"))
    }
    val componentsPerPixel = cgImage.bitsPerPixel / cgImage.bitsPerComponent
    if (!(componentsPerPixel == 3 || componentsPerPixel == 4)) {
        throw LutLoadError(localized("LUT image is not 3 or 4 components per pixel"))
    }
    if (data.size < width * height * cgImage.bitsPerPixel / 8) {
        throw LutLoadError(localized("Failed to get LUT pixels"))
    }
    fun component(index: Int): Float {
        return if (cgImage.bitsPerComponent == 8) {
            (bytes[index].toInt() and 0xFF) / 255f
        } else {
            val value = (bytes[2 * index].toInt() and 0xFF) or ((bytes[2 * index + 1].toInt() and 0xFF) shl 8)
            value / 65535f
        }
    }
    val columns = width / dimension
    return makeLutCube(dimension = dimension) { red, green, blue ->
        val row = blue / columns * dimension + green
        val column = blue % columns * dimension + red
        val index = (row * width + column) * componentsPerPixel
        SIMD3(component(index), component(index + 1), component(index + 2))
    }
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
        loadCube(lutEffectConvertCube(data = imageStorage.makePath(id = lut.id).readBytes()))
    }

    private fun loadImageLut(image: Bitmap) {
        loadCube(lutEffectConvertLut(image = image))
    }

    private fun loadCube(cube: Pair<Float, ByteArray>) {
        val (dimension, data) = cube
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
