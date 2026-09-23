package com.moblin.android.media.haishinkit.mpeg.hevc

import android.media.MediaFormat
import android.util.Log
import com.moblin.android.common.various.atoms
import com.moblin.android.media.haishinkit.util.ByteReader
import java.nio.ByteBuffer

private const val kCMFormatDescriptionBridgeError_InvalidParameter = -12712

class MpegTsVideoConfigHevc(hvcC: ByteArray) {
    private val hvcC: ByteArray = hvcC

    companion object {
        private const val TAG = "MpegTsVideoConfigHevc"

        fun getHvcC(formatDescription: MediaFormat): ByteArray? {
            val atoms = formatDescription.atoms()
            if (atoms != null) {
                return atoms["hvcC"]
            }
            return null
        }

        fun create(formatDescription: MediaFormat): MpegTsVideoConfigHevc? {
            val data = getHvcC(formatDescription) ?: return null
            return MpegTsVideoConfigHevc(data)
        }
    }

    var videoParameterSet: ByteArray? = null
    var sequenceParameterSet: ByteArray? = null
    var pictureParameterSet: ByteArray? = null

    init {
        val reader = ByteReader(hvcC)
        try {
            reader.readBytes(22)
            val numberOfArrays = reader.readUInt8().toInt()
            for (i in 0 until numberOfArrays) {
                val header = reader.readUInt8().toInt()
                val nalUnitType = HevcNalUnitType.fromRawValue((header and 0b0011_1111).toUByte())
                    ?: HevcNalUnitType.unspec
                val numNalus = reader.readUInt16().toInt()
                for (j in 0 until numNalus) {
                    val length = reader.readUInt16().toInt()
                    val data = reader.readBytes(length)
                    when (nalUnitType) {
                        HevcNalUnitType.vps -> videoParameterSet = data
                        HevcNalUnitType.sps -> sequenceParameterSet = data
                        HevcNalUnitType.pps -> pictureParameterSet = data
                        else -> Unit
                    }
                }
            }
        } catch (e: Exception) {
            Log.i(TAG, "Failed to parse hvcC")
        }
    }

    fun makeFormatDescription(formatDescriptionOut: Array<MediaFormat?>): Int {
        if (videoParameterSet == null || sequenceParameterSet == null || pictureParameterSet == null) {
            return kCMFormatDescriptionBridgeError_InvalidParameter
        }
        val csd0 = ByteBuffer.allocate(hvcC.size + 8)
        csd0.putInt(hvcC.size + 8)
        csd0.put('h'.code.toByte())
        csd0.put('v'.code.toByte())
        csd0.put('c'.code.toByte())
        csd0.put('C'.code.toByte())
        csd0.put(hvcC)
        csd0.rewind()
        val format = MediaFormat()
        format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_VIDEO_HEVC)
        format.setByteBuffer("csd-0", csd0)
        formatDescriptionOut[0] = format
        return 0
    }
}
