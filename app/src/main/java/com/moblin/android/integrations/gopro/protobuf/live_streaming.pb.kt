package com.moblin.android.integrations.gopro.protobuf

import com.moblin.android.platform.swiftprotobuf.BinaryDecoder
import com.moblin.android.platform.swiftprotobuf.BinaryEncodingVisitor
import com.moblin.android.platform.swiftprotobuf.Message
import com.moblin.android.platform.swiftprotobuf.merge

private const val _protobuf_package = "open_gopro"

enum class OpenGopro_EnumLens(val rawValue: Int) {
    lensWide(0),
    lensLinear(4),
    lensSuperview(3);

    fun default(): OpenGopro_EnumLens {
        return lensWide
    }

    companion object {
        fun fromRawValue(value: Int): OpenGopro_EnumLens? {
            return entries.firstOrNull { it.rawValue == value }
        }

        val _protobuf_nameMap: Map<String, Int> = mapOf(
            "LENS_WIDE" to 0,
            "LENS_SUPERVIEW" to 3,
            "LENS_LINEAR" to 4
        )
    }
}

enum class OpenGopro_EnumLiveStreamError(val rawValue: Int) {
    liveStreamErrorNone(0),
    liveStreamErrorNetwork(1),
    liveStreamErrorCreatestream(2),
    liveStreamErrorOutofmemory(3),
    liveStreamErrorInputstream(4),
    liveStreamErrorInternet(5),
    liveStreamErrorOsnetwork(6),
    liveStreamErrorSelectednetworktimeout(7),
    liveStreamErrorSslHandshake(8),
    liveStreamErrorCameraBlocked(9),
    liveStreamErrorUnknown(10),
    liveStreamErrorSdCardFull(40),
    liveStreamErrorSdCardRemoved(41);

    fun default(): OpenGopro_EnumLiveStreamError {
        return liveStreamErrorNone
    }

    companion object {
        fun fromRawValue(value: Int): OpenGopro_EnumLiveStreamError? {
            return entries.firstOrNull { it.rawValue == value }
        }

        val _protobuf_nameMap: Map<String, Int> = mapOf(
            "LIVE_STREAM_ERROR_NONE" to 0,
            "LIVE_STREAM_ERROR_NETWORK" to 1,
            "LIVE_STREAM_ERROR_CREATESTREAM" to 2,
            "LIVE_STREAM_ERROR_OUTOFMEMORY" to 3,
            "LIVE_STREAM_ERROR_INPUTSTREAM" to 4,
            "LIVE_STREAM_ERROR_INTERNET" to 5,
            "LIVE_STREAM_ERROR_OSNETWORK" to 6,
            "LIVE_STREAM_ERROR_SELECTEDNETWORKTIMEOUT" to 7,
            "LIVE_STREAM_ERROR_SSL_HANDSHAKE" to 8,
            "LIVE_STREAM_ERROR_CAMERA_BLOCKED" to 9,
            "LIVE_STREAM_ERROR_UNKNOWN" to 10,
            "LIVE_STREAM_ERROR_SD_CARD_FULL" to 40,
            "LIVE_STREAM_ERROR_SD_CARD_REMOVED" to 41
        )
    }
}

enum class OpenGopro_EnumLiveStreamStatus(val rawValue: Int) {
    liveStreamStateIdle(0),
    liveStreamStateConfig(1),
    liveStreamStateReady(2),
    liveStreamStateStreaming(3),
    liveStreamStateCompleteStayOn(4),
    liveStreamStateFailedStayOn(5),
    liveStreamStateReconnecting(6),
    liveStreamStateUnavailable(7);

    fun default(): OpenGopro_EnumLiveStreamStatus {
        return liveStreamStateIdle
    }

    companion object {
        fun fromRawValue(value: Int): OpenGopro_EnumLiveStreamStatus? {
            return entries.firstOrNull { it.rawValue == value }
        }

        val _protobuf_nameMap: Map<String, Int> = mapOf(
            "LIVE_STREAM_STATE_IDLE" to 0,
            "LIVE_STREAM_STATE_CONFIG" to 1,
            "LIVE_STREAM_STATE_READY" to 2,
            "LIVE_STREAM_STATE_STREAMING" to 3,
            "LIVE_STREAM_STATE_COMPLETE_STAY_ON" to 4,
            "LIVE_STREAM_STATE_FAILED_STAY_ON" to 5,
            "LIVE_STREAM_STATE_RECONNECTING" to 6,
            "LIVE_STREAM_STATE_UNAVAILABLE" to 7
        )
    }
}

enum class OpenGopro_EnumRegisterLiveStreamStatus(val rawValue: Int) {
    registerLiveStreamStatusStatus(1),
    registerLiveStreamStatusError(2),
    registerLiveStreamStatusMode(3),
    registerLiveStreamStatusBitrate(4);

    fun default(): OpenGopro_EnumRegisterLiveStreamStatus {
        return registerLiveStreamStatusStatus
    }

    companion object {
        fun fromRawValue(value: Int): OpenGopro_EnumRegisterLiveStreamStatus? {
            return entries.firstOrNull { it.rawValue == value }
        }

        val _protobuf_nameMap: Map<String, Int> = mapOf(
            "REGISTER_LIVE_STREAM_STATUS_STATUS" to 1,
            "REGISTER_LIVE_STREAM_STATUS_ERROR" to 2,
            "REGISTER_LIVE_STREAM_STATUS_MODE" to 3,
            "REGISTER_LIVE_STREAM_STATUS_BITRATE" to 4
        )
    }
}

enum class OpenGopro_EnumWindowSize(val rawValue: Int) {
    windowSize480(4),
    windowSize720(7),
    windowSize1080(12);

    fun default(): OpenGopro_EnumWindowSize {
        return windowSize480
    }

    companion object {
        fun fromRawValue(value: Int): OpenGopro_EnumWindowSize? {
            return entries.firstOrNull { it.rawValue == value }
        }

        val _protobuf_nameMap: Map<String, Int> = mapOf(
            "WINDOW_SIZE_480" to 4,
            "WINDOW_SIZE_720" to 7,
            "WINDOW_SIZE_1080" to 12
        )
    }
}

class OpenGopro_NotifyLiveStreamStatus() : Message {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    private var _liveStreamStatus: OpenGopro_EnumLiveStreamStatus? = null
    var liveStreamStatus: OpenGopro_EnumLiveStreamStatus
        get() = _liveStreamStatus ?: OpenGopro_EnumLiveStreamStatus.liveStreamStateIdle
        set(value) { _liveStreamStatus = value }
    val hasLiveStreamStatus: Boolean
        get() = _liveStreamStatus != null
    fun clearLiveStreamStatus() { _liveStreamStatus = null }

    private var _liveStreamError: OpenGopro_EnumLiveStreamError? = null
    var liveStreamError: OpenGopro_EnumLiveStreamError
        get() = _liveStreamError ?: OpenGopro_EnumLiveStreamError.liveStreamErrorNone
        set(value) { _liveStreamError = value }
    val hasLiveStreamError: Boolean
        get() = _liveStreamError != null
    fun clearLiveStreamError() { _liveStreamError = null }

    private var _liveStreamEncode: Boolean? = null
    var liveStreamEncode: Boolean
        get() = _liveStreamEncode ?: false
        set(value) { _liveStreamEncode = value }
    val hasLiveStreamEncode: Boolean
        get() = _liveStreamEncode != null
    fun clearLiveStreamEncode() { _liveStreamEncode = null }

    private var _liveStreamBitrate: Int? = null
    var liveStreamBitrate: Int
        get() = _liveStreamBitrate ?: 0
        set(value) { _liveStreamBitrate = value }
    val hasLiveStreamBitrate: Boolean
        get() = _liveStreamBitrate != null
    fun clearLiveStreamBitrate() { _liveStreamBitrate = null }

    var liveStreamWindowSizeSupportedArray: MutableList<OpenGopro_EnumWindowSize> = mutableListOf()

    private var _liveStreamEncodeSupported: Boolean? = null
    var liveStreamEncodeSupported: Boolean
        get() = _liveStreamEncodeSupported ?: false
        set(value) { _liveStreamEncodeSupported = value }
    val hasLiveStreamEncodeSupported: Boolean
        get() = _liveStreamEncodeSupported != null
    fun clearLiveStreamEncodeSupported() { _liveStreamEncodeSupported = null }

    private var _liveStreamMaxLensUnsupported: Boolean? = null
    var liveStreamMaxLensUnsupported: Boolean
        get() = _liveStreamMaxLensUnsupported ?: false
        set(value) { _liveStreamMaxLensUnsupported = value }
    val hasLiveStreamMaxLensUnsupported: Boolean
        get() = _liveStreamMaxLensUnsupported != null
    fun clearLiveStreamMaxLensUnsupported() { _liveStreamMaxLensUnsupported = null }

    private var _liveStreamMinimumStreamBitrate: Int? = null
    var liveStreamMinimumStreamBitrate: Int
        get() = _liveStreamMinimumStreamBitrate ?: 0
        set(value) { _liveStreamMinimumStreamBitrate = value }
    val hasLiveStreamMinimumStreamBitrate: Boolean
        get() = _liveStreamMinimumStreamBitrate != null
    fun clearLiveStreamMinimumStreamBitrate() { _liveStreamMinimumStreamBitrate = null }

    private var _liveStreamMaximumStreamBitrate: Int? = null
    var liveStreamMaximumStreamBitrate: Int
        get() = _liveStreamMaximumStreamBitrate ?: 0
        set(value) { _liveStreamMaximumStreamBitrate = value }
    val hasLiveStreamMaximumStreamBitrate: Boolean
        get() = _liveStreamMaximumStreamBitrate != null
    fun clearLiveStreamMaximumStreamBitrate() { _liveStreamMaximumStreamBitrate = null }

    private var _liveStreamLensSupported: Boolean? = null
    var liveStreamLensSupported: Boolean
        get() = _liveStreamLensSupported ?: false
        set(value) { _liveStreamLensSupported = value }
    val hasLiveStreamLensSupported: Boolean
        get() = _liveStreamLensSupported != null
    fun clearLiveStreamLensSupported() { _liveStreamLensSupported = null }

    var liveStreamLensSupportedArray: MutableList<OpenGopro_EnumLens> = mutableListOf()

    private var _liveStreamProtuneSupported: Boolean? = null
    var liveStreamProtuneSupported: Boolean
        get() = _liveStreamProtuneSupported ?: false
        set(value) { _liveStreamProtuneSupported = value }
    val hasLiveStreamProtuneSupported: Boolean
        get() = _liveStreamProtuneSupported != null
    fun clearLiveStreamProtuneSupported() { _liveStreamProtuneSupported = null }

    override var unknownFields: ByteArray = ByteArray(0)

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            val fieldNumber = decoder.nextFieldNumber() ?: break
            when (fieldNumber) {
                1 -> decoder.decodeSingularEnumField { OpenGopro_EnumLiveStreamStatus.fromRawValue(it) }
                    ?.let { _liveStreamStatus = it }
                2 -> decoder.decodeSingularEnumField { OpenGopro_EnumLiveStreamError.fromRawValue(it) }
                    ?.let { _liveStreamError = it }
                3 -> decoder.decodeSingularBoolField()?.let { _liveStreamEncode = it }
                4 -> decoder.decodeSingularInt32Field()?.let { _liveStreamBitrate = it }
                5 -> decoder.decodeRepeatedEnumField(liveStreamWindowSizeSupportedArray) {
                    OpenGopro_EnumWindowSize.fromRawValue(it)
                }
                6 -> decoder.decodeSingularBoolField()?.let { _liveStreamEncodeSupported = it }
                7 -> decoder.decodeSingularBoolField()?.let { _liveStreamMaxLensUnsupported = it }
                8 -> decoder.decodeSingularInt32Field()?.let { _liveStreamMinimumStreamBitrate = it }
                9 -> decoder.decodeSingularInt32Field()?.let { _liveStreamMaximumStreamBitrate = it }
                10 -> decoder.decodeSingularBoolField()?.let { _liveStreamLensSupported = it }
                11 -> decoder.decodeRepeatedEnumField(liveStreamLensSupportedArray) {
                    OpenGopro_EnumLens.fromRawValue(it)
                }
                13 -> decoder.decodeSingularBoolField()?.let { _liveStreamProtuneSupported = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        _liveStreamStatus?.let { visitor.visitSingularEnumField(it.rawValue, 1) }
        _liveStreamError?.let { visitor.visitSingularEnumField(it.rawValue, 2) }
        _liveStreamEncode?.let { visitor.visitSingularBoolField(it, 3) }
        _liveStreamBitrate?.let { visitor.visitSingularInt32Field(it, 4) }
        if (liveStreamWindowSizeSupportedArray.isNotEmpty()) {
            visitor.visitRepeatedEnumField(liveStreamWindowSizeSupportedArray.map { it.rawValue }, 5)
        }
        _liveStreamEncodeSupported?.let { visitor.visitSingularBoolField(it, 6) }
        _liveStreamMaxLensUnsupported?.let { visitor.visitSingularBoolField(it, 7) }
        _liveStreamMinimumStreamBitrate?.let { visitor.visitSingularInt32Field(it, 8) }
        _liveStreamMaximumStreamBitrate?.let { visitor.visitSingularInt32Field(it, 9) }
        _liveStreamLensSupported?.let { visitor.visitSingularBoolField(it, 10) }
        if (liveStreamLensSupportedArray.isNotEmpty()) {
            visitor.visitRepeatedEnumField(liveStreamLensSupportedArray.map { it.rawValue }, 11)
        }
        _liveStreamProtuneSupported?.let { visitor.visitSingularBoolField(it, 13) }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is OpenGopro_NotifyLiveStreamStatus) {
            return false
        }
        if (_liveStreamStatus != other._liveStreamStatus) {
            return false
        }
        if (_liveStreamError != other._liveStreamError) {
            return false
        }
        if (_liveStreamEncode != other._liveStreamEncode) {
            return false
        }
        if (_liveStreamBitrate != other._liveStreamBitrate) {
            return false
        }
        if (liveStreamWindowSizeSupportedArray != other.liveStreamWindowSizeSupportedArray) {
            return false
        }
        if (_liveStreamEncodeSupported != other._liveStreamEncodeSupported) {
            return false
        }
        if (_liveStreamMaxLensUnsupported != other._liveStreamMaxLensUnsupported) {
            return false
        }
        if (_liveStreamMinimumStreamBitrate != other._liveStreamMinimumStreamBitrate) {
            return false
        }
        if (_liveStreamMaximumStreamBitrate != other._liveStreamMaximumStreamBitrate) {
            return false
        }
        if (_liveStreamLensSupported != other._liveStreamLensSupported) {
            return false
        }
        if (liveStreamLensSupportedArray != other.liveStreamLensSupportedArray) {
            return false
        }
        if (_liveStreamProtuneSupported != other._liveStreamProtuneSupported) {
            return false
        }
        if (!unknownFields.contentEquals(other.unknownFields)) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = _liveStreamStatus?.hashCode() ?: 0
        result = 31 * result + (_liveStreamError?.hashCode() ?: 0)
        result = 31 * result + (_liveStreamEncode?.hashCode() ?: 0)
        result = 31 * result + (_liveStreamBitrate ?: 0)
        result = 31 * result + liveStreamWindowSizeSupportedArray.hashCode()
        result = 31 * result + (_liveStreamEncodeSupported?.hashCode() ?: 0)
        result = 31 * result + (_liveStreamMaxLensUnsupported?.hashCode() ?: 0)
        result = 31 * result + (_liveStreamMinimumStreamBitrate ?: 0)
        result = 31 * result + (_liveStreamMaximumStreamBitrate ?: 0)
        result = 31 * result + (_liveStreamLensSupported?.hashCode() ?: 0)
        result = 31 * result + liveStreamLensSupportedArray.hashCode()
        result = 31 * result + (_liveStreamProtuneSupported?.hashCode() ?: 0)
        result = 31 * result + unknownFields.contentHashCode()
        return result
    }

    companion object {
        const val protoMessageName: String = _protobuf_package + ".NotifyLiveStreamStatus"

        val _protobuf_nameMap: Map<String, Int> = mapOf(
            "live_stream_status" to 1,
            "live_stream_error" to 2,
            "live_stream_encode" to 3,
            "live_stream_bitrate" to 4,
            "live_stream_window_size_supported_array" to 5,
            "live_stream_encode_supported" to 6,
            "live_stream_max_lens_unsupported" to 7,
            "live_stream_minimum_stream_bitrate" to 8,
            "live_stream_maximum_stream_bitrate" to 9,
            "live_stream_lens_supported" to 10,
            "live_stream_lens_supported_array" to 11,
            "live_stream_protune_supported" to 13
        )
    }
}

class OpenGopro_RequestGetLiveStreamStatus : Message {
    var registerLiveStreamStatus: MutableList<OpenGopro_EnumRegisterLiveStreamStatus> = mutableListOf()

    var unregisterLiveStreamStatus: MutableList<OpenGopro_EnumRegisterLiveStreamStatus> = mutableListOf()

    override var unknownFields: ByteArray = ByteArray(0)

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            val fieldNumber = decoder.nextFieldNumber() ?: break
            when (fieldNumber) {
                1 -> decoder.decodeRepeatedEnumField(registerLiveStreamStatus) {
                    OpenGopro_EnumRegisterLiveStreamStatus.fromRawValue(it)
                }
                2 -> decoder.decodeRepeatedEnumField(unregisterLiveStreamStatus) {
                    OpenGopro_EnumRegisterLiveStreamStatus.fromRawValue(it)
                }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (registerLiveStreamStatus.isNotEmpty()) {
            visitor.visitRepeatedEnumField(registerLiveStreamStatus.map { it.rawValue }, 1)
        }
        if (unregisterLiveStreamStatus.isNotEmpty()) {
            visitor.visitRepeatedEnumField(unregisterLiveStreamStatus.map { it.rawValue }, 2)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is OpenGopro_RequestGetLiveStreamStatus) {
            return false
        }
        if (registerLiveStreamStatus != other.registerLiveStreamStatus) {
            return false
        }
        if (unregisterLiveStreamStatus != other.unregisterLiveStreamStatus) {
            return false
        }
        if (!unknownFields.contentEquals(other.unknownFields)) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = registerLiveStreamStatus.hashCode()
        result = 31 * result + unregisterLiveStreamStatus.hashCode()
        result = 31 * result + unknownFields.contentHashCode()
        return result
    }

    companion object {
        const val protoMessageName: String = _protobuf_package + ".RequestGetLiveStreamStatus"

        val _protobuf_nameMap: Map<String, Int> = mapOf(
            "register_live_stream_status" to 1,
            "unregister_live_stream_status" to 2
        )
    }
}

class OpenGopro_RequestSetLiveStreamMode : Message {
    private var _url: String? = null
    var url: String
        get() = _url ?: String()
        set(value) { _url = value }
    val hasURL: Boolean
        get() = _url != null
    fun clearURL() { _url = null }

    private var _encode: Boolean? = null
    var encode: Boolean
        get() = _encode ?: false
        set(value) { _encode = value }
    val hasEncode: Boolean
        get() = _encode != null
    fun clearEncode() { _encode = null }

    private var _windowSize: OpenGopro_EnumWindowSize? = null
    var windowSize: OpenGopro_EnumWindowSize
        get() = _windowSize ?: OpenGopro_EnumWindowSize.windowSize480
        set(value) { _windowSize = value }
    val hasWindowSize: Boolean
        get() = _windowSize != null
    fun clearWindowSize() { _windowSize = null }

    private var _cert: ByteArray? = null
    var cert: ByteArray
        get() = _cert ?: ByteArray(0)
        set(value) { _cert = value }
    val hasCert: Boolean
        get() = _cert != null
    fun clearCert() { _cert = null }

    private var _minimumBitrate: Int? = null
    var minimumBitrate: Int
        get() = _minimumBitrate ?: 0
        set(value) { _minimumBitrate = value }
    val hasMinimumBitrate: Boolean
        get() = _minimumBitrate != null
    fun clearMinimumBitrate() { _minimumBitrate = null }

    private var _maximumBitrate: Int? = null
    var maximumBitrate: Int
        get() = _maximumBitrate ?: 0
        set(value) { _maximumBitrate = value }
    val hasMaximumBitrate: Boolean
        get() = _maximumBitrate != null
    fun clearMaximumBitrate() { _maximumBitrate = null }

    private var _startingBitrate: Int? = null
    var startingBitrate: Int
        get() = _startingBitrate ?: 0
        set(value) { _startingBitrate = value }
    val hasStartingBitrate: Boolean
        get() = _startingBitrate != null
    fun clearStartingBitrate() { _startingBitrate = null }

    private var _lens: OpenGopro_EnumLens? = null
    var lens: OpenGopro_EnumLens
        get() = _lens ?: OpenGopro_EnumLens.lensWide
        set(value) { _lens = value }
    val hasLens: Boolean
        get() = _lens != null
    fun clearLens() { _lens = null }

    override var unknownFields: ByteArray = ByteArray(0)

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            val fieldNumber = decoder.nextFieldNumber() ?: break
            when (fieldNumber) {
                1 -> decoder.decodeSingularStringField()?.let { _url = it }
                2 -> decoder.decodeSingularBoolField()?.let { _encode = it }
                3 -> decoder.decodeSingularEnumField { OpenGopro_EnumWindowSize.fromRawValue(it) }
                    ?.let { _windowSize = it }
                6 -> decoder.decodeSingularBytesField()?.let { _cert = it }
                7 -> decoder.decodeSingularInt32Field()?.let { _minimumBitrate = it }
                8 -> decoder.decodeSingularInt32Field()?.let { _maximumBitrate = it }
                9 -> decoder.decodeSingularInt32Field()?.let { _startingBitrate = it }
                10 -> decoder.decodeSingularEnumField { OpenGopro_EnumLens.fromRawValue(it) }
                    ?.let { _lens = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        _url?.let { visitor.visitSingularStringField(it, 1) }
        _encode?.let { visitor.visitSingularBoolField(it, 2) }
        _windowSize?.let { visitor.visitSingularEnumField(it.rawValue, 3) }
        _cert?.let { visitor.visitSingularBytesField(it, 6) }
        _minimumBitrate?.let { visitor.visitSingularInt32Field(it, 7) }
        _maximumBitrate?.let { visitor.visitSingularInt32Field(it, 8) }
        _startingBitrate?.let { visitor.visitSingularInt32Field(it, 9) }
        _lens?.let { visitor.visitSingularEnumField(it.rawValue, 10) }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is OpenGopro_RequestSetLiveStreamMode) {
            return false
        }
        if (_url != other._url) {
            return false
        }
        if (_encode != other._encode) {
            return false
        }
        if (_windowSize != other._windowSize) {
            return false
        }
        if ((_cert == null) != (other._cert == null)) {
            return false
        }
        if (_cert != null && other._cert != null && !_cert!!.contentEquals(other._cert!!)) {
            return false
        }
        if (_minimumBitrate != other._minimumBitrate) {
            return false
        }
        if (_maximumBitrate != other._maximumBitrate) {
            return false
        }
        if (_startingBitrate != other._startingBitrate) {
            return false
        }
        if (_lens != other._lens) {
            return false
        }
        if (!unknownFields.contentEquals(other.unknownFields)) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = _url?.hashCode() ?: 0
        result = 31 * result + (_encode?.hashCode() ?: 0)
        result = 31 * result + (_windowSize?.hashCode() ?: 0)
        result = 31 * result + (_cert?.contentHashCode() ?: 0)
        result = 31 * result + (_minimumBitrate ?: 0)
        result = 31 * result + (_maximumBitrate ?: 0)
        result = 31 * result + (_startingBitrate ?: 0)
        result = 31 * result + (_lens?.hashCode() ?: 0)
        result = 31 * result + unknownFields.contentHashCode()
        return result
    }

    companion object {
        const val protoMessageName: String = _protobuf_package + ".RequestSetLiveStreamMode"

        val _protobuf_nameMap: Map<String, Int> = mapOf(
            "url" to 1,
            "encode" to 2,
            "window_size" to 3,
            "cert" to 6,
            "minimum_bitrate" to 7,
            "maximum_bitrate" to 8,
            "starting_bitrate" to 9,
            "lens" to 10
        )
    }
}
