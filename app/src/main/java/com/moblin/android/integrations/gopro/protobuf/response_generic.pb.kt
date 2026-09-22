package com.moblin.android.integrations.gopro.protobuf

private const val _protobuf_package = "open_gopro"

interface ProtobufAPIVersionCheck

interface ProtobufAPIVersion_2 : ProtobufAPIVersionCheck

object _GeneratedWithProtocGenSwiftVersion : ProtobufAPIVersionCheck {
    interface _2 : ProtobufAPIVersion_2

    interface Version : _2
}

enum class OpenGopro_EnumResultGeneric(val rawValue: Int) {
    resultUnknown(0),
    resultSuccess(1),
    resultIllFormed(2),
    resultNotSupported(3),
    resultArgumentOutOfBounds(4),
    resultArgumentInvalid(5),
    resultResourceNotAvailable(6),
    ;

    companion object {
        fun defaultValue(): OpenGopro_EnumResultGeneric = resultUnknown

        fun fromRawValue(value: Int): OpenGopro_EnumResultGeneric? =
            entries.firstOrNull { it.rawValue == value }

        val _protobuf_nameMap: Map<String, Int> = mapOf(
            "RESULT_UNKNOWN" to 0,
            "RESULT_SUCCESS" to 1,
            "RESULT_ILL_FORMED" to 2,
            "RESULT_NOT_SUPPORTED" to 3,
            "RESULT_ARGUMENT_OUT_OF_BOUNDS" to 4,
            "RESULT_ARGUMENT_INVALID" to 5,
            "RESULT_RESOURCE_NOT_AVAILABLE" to 6,
        )
    }
}

class OpenGopro_ResponseGeneric {
    var result: OpenGopro_EnumResultGeneric
        get() = _result ?: OpenGopro_EnumResultGeneric.resultUnknown
        set(value) {
            _result = value
        }

    val hasResult: Boolean
        get() = _result != null

    fun clearResult() {
        _result = null
    }

    var unknownFields: ByteArray = ByteArray(0)

    private var _result: OpenGopro_EnumResultGeneric? = null

    val isInitialized: Boolean
        get() = _result != null

    fun decodeMessage(decoder: Any) {
        Unit
    }

    fun traverse(visitor: Any) {
        Unit
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OpenGopro_ResponseGeneric) return false
        if (_result != other._result) return false
        if (!unknownFields.contentEquals(other.unknownFields)) return false
        return true
    }

    override fun hashCode(): Int {
        var hash = _result?.hashCode() ?: 0
        hash = 31 * hash + unknownFields.contentHashCode()
        return hash
    }

    companion object {
        const val protoMessageName: String = _protobuf_package + ".ResponseGeneric"

        val _protobuf_nameMap: Map<String, Int> = mapOf("result" to 1)
    }
}

class OpenGopro_Media {
    var folder: String
        get() = _folder ?: String()
        set(value) {
            _folder = value
        }

    val hasFolder: Boolean
        get() = _folder != null

    fun clearFolder() {
        _folder = null
    }

    var file: String
        get() = _file ?: String()
        set(value) {
            _file = value
        }

    val hasFile: Boolean
        get() = _file != null

    fun clearFile() {
        _file = null
    }

    var unknownFields: ByteArray = ByteArray(0)

    private var _folder: String? = null
    private var _file: String? = null

    fun decodeMessage(decoder: Any) {
        Unit
    }

    fun traverse(visitor: Any) {
        Unit
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OpenGopro_Media) return false
        if (_folder != other._folder) return false
        if (_file != other._file) return false
        if (!unknownFields.contentEquals(other.unknownFields)) return false
        return true
    }

    override fun hashCode(): Int {
        var hash = _folder?.hashCode() ?: 0
        hash = 31 * hash + (_file?.hashCode() ?: 0)
        hash = 31 * hash + unknownFields.contentHashCode()
        return hash
    }

    companion object {
        const val protoMessageName: String = _protobuf_package + ".Media"

        val _protobuf_nameMap: Map<String, Int> = mapOf(
            "folder" to 1,
            "file" to 2,
        )
    }
}
