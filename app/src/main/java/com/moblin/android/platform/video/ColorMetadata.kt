package com.moblin.android.platform.video

import android.media.MediaFormat
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

const val kCVImageBufferColorPrimaries_EBU_3213 = "EBU_3213"
const val kCVImageBufferColorPrimaries_SMPTE_C = "SMPTE_C"
const val kCVImageBufferColorPrimaries_DCI_P3 = "DCI_P3"
const val kCVImageBufferTransferFunction_SMPTE_240M_1995 = "SMPTE_240M_1995"
const val kCVImageBufferTransferFunction_Linear = "Linear"
const val kCVImageBufferTransferFunction_sRGB = "IEC_sRGB"
const val kCVImageBufferTransferFunction_ITU_R_2020 = "ITU_R_2020"
const val kCVImageBufferTransferFunction_SMPTE_ST_2084_PQ = "SMPTE_ST_2084_PQ"
const val kCVImageBufferYCbCrMatrix_SMPTE_240M_1995 = "SMPTE_240M_1995"

data class ColorDescription(
    val primaries: String? = null,
    val transferFunction: String? = null,
    val yCbCrMatrix: String? = null,
    val fullRange: Boolean? = null,
) {
    val isEmpty: Boolean
        get() = primaries == null && transferFunction == null && yCbCrMatrix == null && fullRange == null

    internal fun primariesCode(): Int = ColorCodes.primariesCode(primaries)

    internal fun transferCode(): Int = ColorCodes.transferCode(transferFunction)

    internal fun matrixCode(): Int = ColorCodes.matrixCode(yCbCrMatrix)

    fun apply(format: MediaFormat) {
        primaries?.let { format.setString(kCVImageBufferColorPrimariesKey, it) }
        transferFunction?.let { format.setString(kCVImageBufferTransferFunctionKey, it) }
        yCbCrMatrix?.let { format.setString(kCVImageBufferYCbCrMatrixKey, it) }
        val standard = mediaFormatStandard()
        if (standard != null) {
            format.setInteger(MediaFormat.KEY_COLOR_STANDARD, standard)
        }
        val transfer = mediaFormatTransfer()
        if (transfer != null) {
            format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, transfer)
        }
        fullRange?.let {
            format.setInteger(
                MediaFormat.KEY_COLOR_RANGE,
                if (it) MediaFormat.COLOR_RANGE_FULL else MediaFormat.COLOR_RANGE_LIMITED,
            )
        }
    }

    fun mediaFormatStandard(): Int? {
        return when (yCbCrMatrix) {
            kCVImageBufferYCbCrMatrix_ITU_R_2020 -> MediaFormat.COLOR_STANDARD_BT2020
            kCVImageBufferYCbCrMatrix_ITU_R_601_4 -> if (primaries == kCVImageBufferColorPrimaries_EBU_3213) {
                MediaFormat.COLOR_STANDARD_BT601_PAL
            } else {
                MediaFormat.COLOR_STANDARD_BT601_NTSC
            }
            kCVImageBufferYCbCrMatrix_ITU_R_709_2 -> MediaFormat.COLOR_STANDARD_BT709
            else -> when (primaries) {
                kCVImageBufferColorPrimaries_ITU_R_2020 -> MediaFormat.COLOR_STANDARD_BT2020
                kCVImageBufferColorPrimaries_ITU_R_709_2 -> MediaFormat.COLOR_STANDARD_BT709
                kCVImageBufferColorPrimaries_EBU_3213 -> MediaFormat.COLOR_STANDARD_BT601_PAL
                kCVImageBufferColorPrimaries_SMPTE_C -> MediaFormat.COLOR_STANDARD_BT601_NTSC
                else -> null
            }
        }
    }

    fun mediaFormatTransfer(): Int? {
        return when (transferFunction) {
            null -> null
            kCVImageBufferTransferFunction_ITU_R_2100_HLG -> MediaFormat.COLOR_TRANSFER_HLG
            kCVImageBufferTransferFunction_SMPTE_ST_2084_PQ -> MediaFormat.COLOR_TRANSFER_ST2084
            kCVImageBufferTransferFunction_Linear -> MediaFormat.COLOR_TRANSFER_LINEAR
            else -> MediaFormat.COLOR_TRANSFER_SDR_VIDEO
        }
    }

    fun merged(other: ColorDescription): ColorDescription {
        return ColorDescription(
            primaries ?: other.primaries,
            transferFunction ?: other.transferFunction,
            yCbCrMatrix ?: other.yCbCrMatrix,
            fullRange ?: other.fullRange,
        )
    }

    companion object {
        fun fromAttachments(attachments: Map<String, Any>?, fullRange: Boolean? = null): ColorDescription {
            return ColorDescription(
                attachments?.get(kCVImageBufferColorPrimariesKey) as? String,
                attachments?.get(kCVImageBufferTransferFunctionKey) as? String,
                attachments?.get(kCVImageBufferYCbCrMatrixKey) as? String,
                fullRange,
            )
        }

        fun fromImageBuffer(imageBuffer: CVPixelBuffer): ColorDescription {
            val info = PixelFormats.info(imageBuffer.pixelFormatType)
            val fullRange = if (info?.isYCbCr == true) !info.isVideoRange else null
            return fromAttachments(imageBuffer.attachments, fullRange)
        }

        fun fromCodes(primaries: Int, transfer: Int, matrix: Int, fullRange: Boolean?): ColorDescription {
            return ColorDescription(
                ColorCodes.primaries(primaries),
                ColorCodes.transferFunction(transfer),
                ColorCodes.matrix(matrix),
                fullRange,
            )
        }

        fun fromMediaFormat(format: MediaFormat): ColorDescription {
            val range = readInteger(format, MediaFormat.KEY_COLOR_RANGE)
            val standard = readInteger(format, MediaFormat.KEY_COLOR_STANDARD)
            val transfer = readInteger(format, MediaFormat.KEY_COLOR_TRANSFER)
            return ColorDescription(
                readString(format, kCVImageBufferColorPrimariesKey) ?: when (standard) {
                    MediaFormat.COLOR_STANDARD_BT709 -> kCVImageBufferColorPrimaries_ITU_R_709_2
                    MediaFormat.COLOR_STANDARD_BT601_PAL -> kCVImageBufferColorPrimaries_EBU_3213
                    MediaFormat.COLOR_STANDARD_BT601_NTSC -> kCVImageBufferColorPrimaries_SMPTE_C
                    MediaFormat.COLOR_STANDARD_BT2020 -> kCVImageBufferColorPrimaries_ITU_R_2020
                    else -> null
                },
                readString(format, kCVImageBufferTransferFunctionKey) ?: when (transfer) {
                    MediaFormat.COLOR_TRANSFER_SDR_VIDEO -> kCVImageBufferTransferFunction_ITU_R_709_2
                    MediaFormat.COLOR_TRANSFER_HLG -> kCVImageBufferTransferFunction_ITU_R_2100_HLG
                    MediaFormat.COLOR_TRANSFER_ST2084 -> kCVImageBufferTransferFunction_SMPTE_ST_2084_PQ
                    MediaFormat.COLOR_TRANSFER_LINEAR -> kCVImageBufferTransferFunction_Linear
                    else -> null
                },
                readString(format, kCVImageBufferYCbCrMatrixKey) ?: when (standard) {
                    MediaFormat.COLOR_STANDARD_BT709 -> kCVImageBufferYCbCrMatrix_ITU_R_709_2
                    MediaFormat.COLOR_STANDARD_BT601_PAL, MediaFormat.COLOR_STANDARD_BT601_NTSC ->
                        kCVImageBufferYCbCrMatrix_ITU_R_601_4
                    MediaFormat.COLOR_STANDARD_BT2020 -> kCVImageBufferYCbCrMatrix_ITU_R_2020
                    else -> null
                },
                when (range) {
                    MediaFormat.COLOR_RANGE_FULL -> true
                    MediaFormat.COLOR_RANGE_LIMITED -> false
                    else -> null
                },
            )
        }

        private fun readInteger(format: MediaFormat, key: String): Int? {
            return try {
                if (format.containsKey(key)) format.getInteger(key) else null
            } catch (error: Exception) {
                null
            }
        }

        private fun readString(format: MediaFormat, key: String): String? {
            return try {
                if (format.containsKey(key)) format.getString(key) else null
            } catch (error: Exception) {
                null
            }
        }
    }
}

internal object ColorCodes {
    const val unspecified = 2

    fun primariesCode(primaries: String?): Int {
        return when (primaries) {
            kCVImageBufferColorPrimaries_ITU_R_709_2 -> 1
            kCVImageBufferColorPrimaries_EBU_3213 -> 5
            kCVImageBufferColorPrimaries_SMPTE_C -> 6
            kCVImageBufferColorPrimaries_ITU_R_2020 -> 9
            kCVImageBufferColorPrimaries_DCI_P3 -> 11
            kCVImageBufferColorPrimaries_P3_D65 -> 12
            else -> unspecified
        }
    }

    fun transferCode(transferFunction: String?): Int {
        return when (transferFunction) {
            kCVImageBufferTransferFunction_ITU_R_709_2 -> 1
            kCVImageBufferTransferFunction_SMPTE_240M_1995 -> 7
            kCVImageBufferTransferFunction_Linear -> 8
            kCVImageBufferTransferFunction_sRGB -> 13
            kCVImageBufferTransferFunction_ITU_R_2020 -> 14
            kCVImageBufferTransferFunction_SMPTE_ST_2084_PQ -> 16
            kCVImageBufferTransferFunction_ITU_R_2100_HLG -> 18
            else -> unspecified
        }
    }

    fun matrixCode(matrix: String?): Int {
        return when (matrix) {
            kCVImageBufferYCbCrMatrix_ITU_R_709_2 -> 1
            kCVImageBufferYCbCrMatrix_ITU_R_601_4 -> 6
            kCVImageBufferYCbCrMatrix_SMPTE_240M_1995 -> 7
            kCVImageBufferYCbCrMatrix_ITU_R_2020 -> 9
            else -> unspecified
        }
    }

    fun primaries(code: Int): String? {
        return when (code) {
            1 -> kCVImageBufferColorPrimaries_ITU_R_709_2
            5 -> kCVImageBufferColorPrimaries_EBU_3213
            6 -> kCVImageBufferColorPrimaries_SMPTE_C
            9 -> kCVImageBufferColorPrimaries_ITU_R_2020
            11 -> kCVImageBufferColorPrimaries_DCI_P3
            12 -> kCVImageBufferColorPrimaries_P3_D65
            else -> null
        }
    }

    fun transferFunction(code: Int): String? {
        return when (code) {
            1, 6 -> kCVImageBufferTransferFunction_ITU_R_709_2
            7 -> kCVImageBufferTransferFunction_SMPTE_240M_1995
            8 -> kCVImageBufferTransferFunction_Linear
            13 -> kCVImageBufferTransferFunction_sRGB
            14, 15 -> kCVImageBufferTransferFunction_ITU_R_2020
            16 -> kCVImageBufferTransferFunction_SMPTE_ST_2084_PQ
            18 -> kCVImageBufferTransferFunction_ITU_R_2100_HLG
            else -> null
        }
    }

    fun matrix(code: Int): String? {
        return when (code) {
            1 -> kCVImageBufferYCbCrMatrix_ITU_R_709_2
            5, 6 -> kCVImageBufferYCbCrMatrix_ITU_R_601_4
            7 -> kCVImageBufferYCbCrMatrix_SMPTE_240M_1995
            9 -> kCVImageBufferYCbCrMatrix_ITU_R_2020
            else -> null
        }
    }
}

internal class YCbCrCoding(val kr: Double, val kb: Double, val bits: Int, val videoRange: Boolean) {
    private val kg = 1 - kr - kb
    private val scale = (1 shl (bits - 8)).toDouble()
    val maximumCode = (1 shl bits) - 1
    private val yOffset = if (videoRange) 16 * scale else 0.0
    private val yScale = if (videoRange) 219 * scale else maximumCode.toDouble()
    private val cOffset = (1 shl (bits - 1)).toDouble()
    private val cScale = if (videoRange) 224 * scale else maximumCode.toDouble()

    fun luma(red: Double, green: Double, blue: Double): Double {
        return yOffset + yScale * (kr * red + kg * green + kb * blue)
    }

    fun cb(red: Double, green: Double, blue: Double): Double {
        val y = kr * red + kg * green + kb * blue
        return cOffset + cScale * (blue - y) / (2 * (1 - kb))
    }

    fun cr(red: Double, green: Double, blue: Double): Double {
        val y = kr * red + kg * green + kb * blue
        return cOffset + cScale * (red - y) / (2 * (1 - kr))
    }

    fun decode(y: Double, cb: Double, cr: Double, output: DoubleArray) {
        val luma = (y - yOffset) / yScale
        val pb = (cb - cOffset) / cScale
        val pr = (cr - cOffset) / cScale
        val red = luma + 2 * (1 - kr) * pr
        val blue = luma + 2 * (1 - kb) * pb
        output[0] = red
        output[1] = (luma - kr * red - kb * blue) / kg
        output[2] = blue
    }

    fun code(value: Double): Int {
        return value.roundToInt().coerceIn(0, maximumCode)
    }

    fun sameAs(other: YCbCrCoding): Boolean {
        return bits == other.bits && videoRange == other.videoRange && abs(kr - other.kr) < 1e-9 &&
            abs(kb - other.kb) < 1e-9
    }

    companion object {
        fun coefficients(matrix: String?): Pair<Double, Double> {
            return when (matrix) {
                kCVImageBufferYCbCrMatrix_ITU_R_601_4 -> Pair(0.299, 0.114)
                kCVImageBufferYCbCrMatrix_ITU_R_2020 -> Pair(0.2627, 0.0593)
                kCVImageBufferYCbCrMatrix_SMPTE_240M_1995 -> Pair(0.212, 0.087)
                else -> Pair(0.2126, 0.0722)
            }
        }

        fun make(matrix: String?, bits: Int, videoRange: Boolean): YCbCrCoding {
            val (kr, kb) = coefficients(matrix)
            return YCbCrCoding(kr, kb, bits, videoRange)
        }

        fun forBuffer(buffer: CVPixelBuffer): YCbCrCoding? {
            val info = PixelFormats.info(buffer.pixelFormatType) ?: return null
            if (!info.isYCbCr) {
                return null
            }
            val matrix = buffer.attachments[kCVImageBufferYCbCrMatrixKey] as? String
            return make(matrix, info.bitsPerComponent, info.isVideoRange)
        }
    }
}

internal object TransferFunctions {
    fun srgbToLinear(value: Double): Double {
        val magnitude = abs(value)
        val linear = if (magnitude <= 0.04045) magnitude / 12.92 else ((magnitude + 0.055) / 1.055).pow(2.4)
        return if (value < 0) -linear else linear
    }

    fun linearToSrgb(value: Double): Double {
        val magnitude = abs(value)
        val encoded = if (magnitude <= 0.0031308) magnitude * 12.92 else 1.055 * magnitude.pow(1 / 2.4) - 0.055
        return if (value < 0) -encoded else encoded
    }
}
