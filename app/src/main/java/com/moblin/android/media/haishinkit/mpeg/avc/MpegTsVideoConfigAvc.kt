package com.moblin.android.media.haishinkit.mpeg.avc

import android.media.MediaFormat
import android.util.Log
import com.moblin.android.media.haishinkit.util.ByteReader
import java.nio.ByteBuffer

class MpegTsVideoConfigAvc {
    companion object {
        private const val TAG = "MpegTsVideoConfigAvc"

        fun getAvcC(formatDescription: MediaFormat): ByteArray? {
            return null
        }

        fun fromFormatDescription(formatDescription: MediaFormat): MpegTsVideoConfigAvc? {
            val data = getAvcC(formatDescription) ?: return null
            return MpegTsVideoConfigAvc(data)
        }

        val reserveNumOfSequenceParameterSets: UByte = 0xE0u.toUByte()
    }

    var sequenceParameterSet: ByteArray? = null
    var pictureParameterSet: ByteArray? = null

    constructor(avcC: ByteArray) {
        val reader = ByteReader(avcC)
        runCatching {
            reader.readBytes(5)
            val numOfSequenceParameterSetsWithReserved = reader.readUInt8()
            val numOfSequenceParameterSets = numOfSequenceParameterSetsWithReserved and
                reserveNumOfSequenceParameterSets.inv()
            repeat(numOfSequenceParameterSets.toInt()) {
                val length = reader.readUInt16().toInt()
                sequenceParameterSet = reader.readBytes(length)
            }
            val numPictureParameterSets = reader.readUInt8().toInt()
            repeat(numPictureParameterSets) {
                val length = reader.readUInt16().toInt()
                pictureParameterSet = reader.readBytes(length)
            }
        }.onFailure {
            Log.i(TAG, "Failed to parse avcC")
        }
    }

    fun makeFormatDescription(): MediaFormat? {
        val pps = pictureParameterSet ?: return null
        val sps = sequenceParameterSet ?: return null
        val format = MediaFormat()
        format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_VIDEO_AVC)
        format.setByteBuffer("csd-0", ByteBuffer.wrap(sps))
        format.setByteBuffer("csd-1", ByteBuffer.wrap(pps))
        return format
    }
}
