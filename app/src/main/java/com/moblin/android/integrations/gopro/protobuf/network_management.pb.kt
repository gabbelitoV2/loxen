package com.moblin.android.integrations.gopro.protobuf

enum class OpenGopro_EnumProvisioning(val rawValue: Int) {
    provisioningUnknown(0),
    provisioningNeverStarted(1),
    provisioningStarted(2),
    provisioningAbortedBySystem(3),
    provisioningCancelledByUser(4),
    provisioningSuccessNewAp(5),
    provisioningSuccessOldAp(6),
    provisioningErrorFailedToAssociate(7),
    provisioningErrorPasswordAuth(8),
    provisioningErrorEulaBlocking(9),
    provisioningErrorNoInternet(10),
    provisioningErrorUnsupportedType(11),
    ;

    companion object {
        fun fromRawValue(value: Int): OpenGopro_EnumProvisioning? =
            entries.firstOrNull { it.rawValue == value }

        val _protobuf_nameMap: Map<Int, String> = mapOf(
            0 to "PROVISIONING_UNKNOWN",
            1 to "PROVISIONING_NEVER_STARTED",
            2 to "PROVISIONING_STARTED",
            3 to "PROVISIONING_ABORTED_BY_SYSTEM",
            4 to "PROVISIONING_CANCELLED_BY_USER",
            5 to "PROVISIONING_SUCCESS_NEW_AP",
            6 to "PROVISIONING_SUCCESS_OLD_AP",
            7 to "PROVISIONING_ERROR_FAILED_TO_ASSOCIATE",
            8 to "PROVISIONING_ERROR_PASSWORD_AUTH",
            9 to "PROVISIONING_ERROR_EULA_BLOCKING",
            10 to "PROVISIONING_ERROR_NO_INTERNET",
            11 to "PROVISIONING_ERROR_UNSUPPORTED_TYPE",
        )
    }
}

enum class OpenGopro_EnumScanning(val rawValue: Int) {
    scanningUnknown(0),
    scanningNeverStarted(1),
    scanningStarted(2),
    scanningAbortedBySystem(3),
    scanningCancelledByUser(4),
    scanningSuccess(5),
    ;

    companion object {
        fun fromRawValue(value: Int): OpenGopro_EnumScanning? =
            entries.firstOrNull { it.rawValue == value }

        val _protobuf_nameMap: Map<Int, String> = mapOf(
            0 to "SCANNING_UNKNOWN",
            1 to "SCANNING_NEVER_STARTED",
            2 to "SCANNING_STARTED",
            3 to "SCANNING_ABORTED_BY_SYSTEM",
            4 to "SCANNING_CANCELLED_BY_USER",
            5 to "SCANNING_SUCCESS",
        )
    }
}

enum class OpenGopro_EnumScanEntryFlags(val rawValue: Int) {
    scanFlagOpen(0),
    scanFlagAuthenticated(1),
    scanFlagConfigured(2),
    scanFlagBestSsid(4),
    scanFlagAssociated(8),
    scanFlagUnsupportedType(16),
    ;

    companion object {
        fun fromRawValue(value: Int): OpenGopro_EnumScanEntryFlags? =
            entries.firstOrNull { it.rawValue == value }

        val _protobuf_nameMap: Map<Int, String> = mapOf(
            0 to "SCAN_FLAG_OPEN",
            1 to "SCAN_FLAG_AUTHENTICATED",
            2 to "SCAN_FLAG_CONFIGURED",
            4 to "SCAN_FLAG_BEST_SSID",
            8 to "SCAN_FLAG_ASSOCIATED",
            16 to "SCAN_FLAG_UNSUPPORTED_TYPE",
        )
    }
}

enum class OpenGopro_EnumPairingFinishState(val rawValue: Int) {
    success(0),
    failed(1),
    ;

    companion object {
        fun fromRawValue(value: Int): OpenGopro_EnumPairingFinishState? =
            entries.firstOrNull { it.rawValue == value }

        val _protobuf_nameMap: Map<Int, String> = mapOf(
            0 to "SUCCESS",
            1 to "FAILED",
        )
    }
}

class OpenGopro_NotifProvisioningState {
    var provisioningState: OpenGopro_EnumProvisioning
        get() = _provisioningState ?: OpenGopro_EnumProvisioning.provisioningUnknown
        set(value) {
            _provisioningState = value
        }

    val hasProvisioningState: Boolean
        get() = _provisioningState != null

    fun clearProvisioningState() {
        _provisioningState = null
    }

    var unknownFields: ByteArray = ByteArray(0)

    private var _provisioningState: OpenGopro_EnumProvisioning? = null

    val isInitialized: Boolean
        get() = _provisioningState != null

    fun <D> decodeMessage(decoder: D) {
        Unit
    }

    fun <V> traverse(visitor: V) {
        Unit
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is OpenGopro_NotifProvisioningState) {
            return false
        }
        if (_provisioningState != other._provisioningState) {
            return false
        }
        if (!unknownFields.contentEquals(other.unknownFields)) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = _provisioningState?.hashCode() ?: 0
        result = 31 * result + unknownFields.contentHashCode()
        return result
    }

    companion object {
        const val protoMessageName: String = _protobuf_package + ".NotifProvisioningState"

        val _protobuf_nameMap: Map<Int, String> = mapOf(
            1 to "provisioning_state",
        )
    }
}

class OpenGopro_NotifStartScanning {
    var scanningState: OpenGopro_EnumScanning
        get() = _scanningState ?: OpenGopro_EnumScanning.scanningUnknown
        set(value) {
            _scanningState = value
        }

    val hasScanningState: Boolean
        get() = _scanningState != null

    fun clearScanningState() {
        _scanningState = null
    }

    var scanID: Int
        get() = _scanID ?: 0
        set(value) {
            _scanID = value
        }

    val hasScanID: Boolean
        get() = _scanID != null

    fun clearScanID() {
        _scanID = null
    }

    var totalEntries: Int
        get() = _totalEntries ?: 0
        set(value) {
            _totalEntries = value
        }

    val hasTotalEntries: Boolean
        get() = _totalEntries != null

    fun clearTotalEntries() {
        _totalEntries = null
    }

    var totalConfiguredSsid: Int
        get() = _totalConfiguredSsid ?: 0
        set(value) {
            _totalConfiguredSsid = value
        }

    val hasTotalConfiguredSsid: Boolean
        get() = _totalConfiguredSsid != null

    fun clearTotalConfiguredSsid() {
        _totalConfiguredSsid = null
    }

    var unknownFields: ByteArray = ByteArray(0)

    private var _scanningState: OpenGopro_EnumScanning? = null
    private var _scanID: Int? = null
    private var _totalEntries: Int? = null
    private var _totalConfiguredSsid: Int? = null

    val isInitialized: Boolean
        get() {
            if (_scanningState == null) {
                return false
            }
            if (_totalConfiguredSsid == null) {
                return false
            }
            return true
        }

    fun <D> decodeMessage(decoder: D) {
        Unit
    }

    fun <V> traverse(visitor: V) {
        Unit
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is OpenGopro_NotifStartScanning) {
            return false
        }
        if (_scanningState != other._scanningState) {
            return false
        }
        if (_scanID != other._scanID) {
            return false
        }
        if (_totalEntries != other._totalEntries) {
            return false
        }
        if (_totalConfiguredSsid != other._totalConfiguredSsid) {
            return false
        }
        if (!unknownFields.contentEquals(other.unknownFields)) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = _scanningState?.hashCode() ?: 0
        result = 31 * result + (_scanID?.hashCode() ?: 0)
        result = 31 * result + (_totalEntries?.hashCode() ?: 0)
        result = 31 * result + (_totalConfiguredSsid?.hashCode() ?: 0)
        result = 31 * result + unknownFields.contentHashCode()
        return result
    }

    companion object {
        const val protoMessageName: String = _protobuf_package + ".NotifStartScanning"

        val _protobuf_nameMap: Map<Int, String> = mapOf(
            1 to "scanning_state",
            2 to "scan_id",
            3 to "total_entries",
            4 to "total_configured_ssid",
        )
    }
}

class OpenGopro_RequestConnect {
    var ssid: String
        get() = _ssid ?: ""
        set(value) {
            _ssid = value
        }

    val hasSsid: Boolean
        get() = _ssid != null

    fun clearSsid() {
        _ssid = null
    }

    var unknownFields: ByteArray = ByteArray(0)

    private var _ssid: String? = null

    val isInitialized: Boolean
        get() = _ssid != null

    fun <D> decodeMessage(decoder: D) {
        Unit
    }

    fun <V> traverse(visitor: V) {
        Unit
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is OpenGopro_RequestConnect) {
            return false
        }
        if (_ssid != other._ssid) {
            return false
        }
        if (!unknownFields.contentEquals(other.unknownFields)) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = _ssid?.hashCode() ?: 0
        result = 31 * result + unknownFields.contentHashCode()
        return result
    }

    companion object {
        const val protoMessageName: String = _protobuf_package + ".RequestConnect"

        val _protobuf_nameMap: Map<Int, String> = mapOf(
            1 to "ssid",
        )
    }
}

class OpenGopro_RequestConnectNew {
    var ssid: String
        get() = _ssid ?: ""
        set(value) {
            _ssid = value
        }

    val hasSsid: Boolean
        get() = _ssid != null

    fun clearSsid() {
        _ssid = null
    }

    var password: String
        get() = _password ?: ""
        set(value) {
            _password = value
        }

    val hasPassword: Boolean
        get() = _password != null

    fun clearPassword() {
        _password = null
    }

    var staticIp: ByteArray
        get() = _staticIp ?: ByteArray(0)
        set(value) {
            _staticIp = value
        }

    val hasStaticIp: Boolean
        get() = _staticIp != null

    fun clearStaticIp() {
        _staticIp = null
    }

    var gateway: ByteArray
        get() = _gateway ?: ByteArray(0)
        set(value) {
            _gateway = value
        }

    val hasGateway: Boolean
        get() = _gateway != null

    fun clearGateway() {
        _gateway = null
    }

    var subnet: ByteArray
        get() = _subnet ?: ByteArray(0)
        set(value) {
            _subnet = value
        }

    val hasSubnet: Boolean
        get() = _subnet != null

    fun clearSubnet() {
        _subnet = null
    }

    var dnsPrimary: ByteArray
        get() = _dnsPrimary ?: ByteArray(0)
        set(value) {
            _dnsPrimary = value
        }

    val hasDnsPrimary: Boolean
        get() = _dnsPrimary != null

    fun clearDnsPrimary() {
        _dnsPrimary = null
    }

    var dnsSecondary: ByteArray
        get() = _dnsSecondary ?: ByteArray(0)
        set(value) {
            _dnsSecondary = value
        }

    val hasDnsSecondary: Boolean
        get() = _dnsSecondary != null

    fun clearDnsSecondary() {
        _dnsSecondary = null
    }

    var bypassEulaCheck: Boolean
        get() = _bypassEulaCheck ?: false
        set(value) {
            _bypassEulaCheck = value
        }

    val hasBypassEulaCheck: Boolean
        get() = _bypassEulaCheck != null

    fun clearBypassEulaCheck() {
        _bypassEulaCheck = null
    }

    var unknownFields: ByteArray = ByteArray(0)

    private var _ssid: String? = null
    private var _password: String? = null
    private var _staticIp: ByteArray? = null
    private var _gateway: ByteArray? = null
    private var _subnet: ByteArray? = null
    private var _dnsPrimary: ByteArray? = null
    private var _dnsSecondary: ByteArray? = null
    private var _bypassEulaCheck: Boolean? = null

    val isInitialized: Boolean
        get() {
            if (_ssid == null) {
                return false
            }
            if (_password == null) {
                return false
            }
            return true
        }

    fun <D> decodeMessage(decoder: D) {
        Unit
    }

    fun <V> traverse(visitor: V) {
        Unit
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is OpenGopro_RequestConnectNew) {
            return false
        }
        if (_ssid != other._ssid) {
            return false
        }
        if (_password != other._password) {
            return false
        }
        if (!byteArraysEqual(_staticIp, other._staticIp)) {
            return false
        }
        if (!byteArraysEqual(_gateway, other._gateway)) {
            return false
        }
        if (!byteArraysEqual(_subnet, other._subnet)) {
            return false
        }
        if (!byteArraysEqual(_dnsPrimary, other._dnsPrimary)) {
            return false
        }
        if (!byteArraysEqual(_dnsSecondary, other._dnsSecondary)) {
            return false
        }
        if (_bypassEulaCheck != other._bypassEulaCheck) {
            return false
        }
        if (!unknownFields.contentEquals(other.unknownFields)) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = _ssid?.hashCode() ?: 0
        result = 31 * result + (_password?.hashCode() ?: 0)
        result = 31 * result + (_staticIp?.contentHashCode() ?: 0)
        result = 31 * result + (_gateway?.contentHashCode() ?: 0)
        result = 31 * result + (_subnet?.contentHashCode() ?: 0)
        result = 31 * result + (_dnsPrimary?.contentHashCode() ?: 0)
        result = 31 * result + (_dnsSecondary?.contentHashCode() ?: 0)
        result = 31 * result + (_bypassEulaCheck?.hashCode() ?: 0)
        result = 31 * result + unknownFields.contentHashCode()
        return result
    }

    companion object {
        const val protoMessageName: String = _protobuf_package + ".RequestConnectNew"

        val _protobuf_nameMap: Map<Int, String> = mapOf(
            1 to "ssid",
            2 to "password",
            3 to "static_ip",
            4 to "gateway",
            5 to "subnet",
            6 to "dns_primary",
            7 to "dns_secondary",
            10 to "bypass_eula_check",
        )
    }
}

class OpenGopro_RequestGetApEntries {
    var startIndex: Int
        get() = _startIndex ?: 0
        set(value) {
            _startIndex = value
        }

    val hasStartIndex: Boolean
        get() = _startIndex != null

    fun clearStartIndex() {
        _startIndex = null
    }

    var maxEntries: Int
        get() = _maxEntries ?: 0
        set(value) {
            _maxEntries = value
        }

    val hasMaxEntries: Boolean
        get() = _maxEntries != null

    fun clearMaxEntries() {
        _maxEntries = null
    }

    var scanID: Int
        get() = _scanID ?: 0
        set(value) {
            _scanID = value
        }

    val hasScanID: Boolean
        get() = _scanID != null

    fun clearScanID() {
        _scanID = null
    }

    var unknownFields: ByteArray = ByteArray(0)

    private var _startIndex: Int? = null
    private var _maxEntries: Int? = null
    private var _scanID: Int? = null

    val isInitialized: Boolean
        get() {
            if (_startIndex == null) {
                return false
            }
            if (_maxEntries == null) {
                return false
            }
            if (_scanID == null) {
                return false
            }
            return true
        }

    fun <D> decodeMessage(decoder: D) {
        Unit
    }

    fun <V> traverse(visitor: V) {
        Unit
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is OpenGopro_RequestGetApEntries) {
            return false
        }
        if (_startIndex != other._startIndex) {
            return false
        }
        if (_maxEntries != other._maxEntries) {
            return false
        }
        if (_scanID != other._scanID) {
            return false
        }
        if (!unknownFields.contentEquals(other.unknownFields)) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = _startIndex?.hashCode() ?: 0
        result = 31 * result + (_maxEntries?.hashCode() ?: 0)
        result = 31 * result + (_scanID?.hashCode() ?: 0)
        result = 31 * result + unknownFields.contentHashCode()
        return result
    }

    companion object {
        const val protoMessageName: String = _protobuf_package + ".RequestGetApEntries"

        val _protobuf_nameMap: Map<Int, String> = mapOf(
            1 to "start_index",
            2 to "max_entries",
            3 to "scan_id",
        )
    }
}

class OpenGopro_RequestReleaseNetwork {
    var unknownFields: ByteArray = ByteArray(0)

    val isInitialized: Boolean
        get() = true

    fun <D> decodeMessage(decoder: D) {
        Unit
    }

    fun <V> traverse(visitor: V) {
        Unit
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is OpenGopro_RequestReleaseNetwork) {
            return false
        }
        if (!unknownFields.contentEquals(other.unknownFields)) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        return unknownFields.contentHashCode()
    }

    companion object {
        const val protoMessageName: String = _protobuf_package + ".RequestReleaseNetwork"

        val _protobuf_nameMap: Map<Int, String> = emptyMap()
    }
}

class OpenGopro_RequestStartScan {
    var unknownFields: ByteArray = ByteArray(0)

    val isInitialized: Boolean
        get() = true

    fun <D> decodeMessage(decoder: D) {
        Unit
    }

    fun <V> traverse(visitor: V) {
        Unit
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is OpenGopro_RequestStartScan) {
            return false
        }
        if (!unknownFields.contentEquals(other.unknownFields)) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        return unknownFields.contentHashCode()
    }

    companion object {
        const val protoMessageName: String = _protobuf_package + ".RequestStartScan"

        val _protobuf_nameMap: Map<Int, String> = emptyMap()
    }
}

class OpenGopro_ResponseConnect {
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

    var provisioningState: OpenGopro_EnumProvisioning
        get() = _provisioningState ?: OpenGopro_EnumProvisioning.provisioningUnknown
        set(value) {
            _provisioningState = value
        }

    val hasProvisioningState: Boolean
        get() = _provisioningState != null

    fun clearProvisioningState() {
        _provisioningState = null
    }

    var timeoutSeconds: Int
        get() = _timeoutSeconds ?: 0
        set(value) {
            _timeoutSeconds = value
        }

    val hasTimeoutSeconds: Boolean
        get() = _timeoutSeconds != null

    fun clearTimeoutSeconds() {
        _timeoutSeconds = null
    }

    var unknownFields: ByteArray = ByteArray(0)

    private var _result: OpenGopro_EnumResultGeneric? = null
    private var _provisioningState: OpenGopro_EnumProvisioning? = null
    private var _timeoutSeconds: Int? = null

    val isInitialized: Boolean
        get() {
            if (_result == null) {
                return false
            }
            if (_provisioningState == null) {
                return false
            }
            if (_timeoutSeconds == null) {
                return false
            }
            return true
        }

    fun <D> decodeMessage(decoder: D) {
        Unit
    }

    fun <V> traverse(visitor: V) {
        Unit
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is OpenGopro_ResponseConnect) {
            return false
        }
        if (_result != other._result) {
            return false
        }
        if (_provisioningState != other._provisioningState) {
            return false
        }
        if (_timeoutSeconds != other._timeoutSeconds) {
            return false
        }
        if (!unknownFields.contentEquals(other.unknownFields)) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = _result?.hashCode() ?: 0
        result = 31 * result + (_provisioningState?.hashCode() ?: 0)
        result = 31 * result + (_timeoutSeconds?.hashCode() ?: 0)
        result = 31 * result + unknownFields.contentHashCode()
        return result
    }

    companion object {
        const val protoMessageName: String = _protobuf_package + ".ResponseConnect"

        val _protobuf_nameMap: Map<Int, String> = mapOf(
            1 to "result",
            2 to "provisioning_state",
            3 to "timeout_seconds",
        )
    }
}

class OpenGopro_ResponseConnectNew {
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

    var provisioningState: OpenGopro_EnumProvisioning
        get() = _provisioningState ?: OpenGopro_EnumProvisioning.provisioningUnknown
        set(value) {
            _provisioningState = value
        }

    val hasProvisioningState: Boolean
        get() = _provisioningState != null

    fun clearProvisioningState() {
        _provisioningState = null
    }

    var timeoutSeconds: Int
        get() = _timeoutSeconds ?: 0
        set(value) {
            _timeoutSeconds = value
        }

    val hasTimeoutSeconds: Boolean
        get() = _timeoutSeconds != null

    fun clearTimeoutSeconds() {
        _timeoutSeconds = null
    }

    var unknownFields: ByteArray = ByteArray(0)

    private var _result: OpenGopro_EnumResultGeneric? = null
    private var _provisioningState: OpenGopro_EnumProvisioning? = null
    private var _timeoutSeconds: Int? = null

    val isInitialized: Boolean
        get() {
            if (_result == null) {
                return false
            }
            if (_provisioningState == null) {
                return false
            }
            if (_timeoutSeconds == null) {
                return false
            }
            return true
        }

    fun <D> decodeMessage(decoder: D) {
        Unit
    }

    fun <V> traverse(visitor: V) {
        Unit
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is OpenGopro_ResponseConnectNew) {
            return false
        }
        if (_result != other._result) {
            return false
        }
        if (_provisioningState != other._provisioningState) {
            return false
        }
        if (_timeoutSeconds != other._timeoutSeconds) {
            return false
        }
        if (!unknownFields.contentEquals(other.unknownFields)) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = _result?.hashCode() ?: 0
        result = 31 * result + (_provisioningState?.hashCode() ?: 0)
        result = 31 * result + (_timeoutSeconds?.hashCode() ?: 0)
        result = 31 * result + unknownFields.contentHashCode()
        return result
    }

    companion object {
        const val protoMessageName: String = _protobuf_package + ".ResponseConnectNew"

        val _protobuf_nameMap: Map<Int, String> = mapOf(
            1 to "result",
            2 to "provisioning_state",
            3 to "timeout_seconds",
        )
    }
}

class OpenGopro_ResponseGetApEntries {
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

    var scanID: Int
        get() = _scanID ?: 0
        set(value) {
            _scanID = value
        }

    val hasScanID: Boolean
        get() = _scanID != null

    fun clearScanID() {
        _scanID = null
    }

    var entries: MutableList<ScanEntry> = mutableListOf()

    var unknownFields: ByteArray = ByteArray(0)

    class ScanEntry {
        var ssid: String
            get() = _ssid ?: ""
            set(value) {
                _ssid = value
            }

        val hasSsid: Boolean
            get() = _ssid != null

        fun clearSsid() {
            _ssid = null
        }

        var signalStrengthBars: Int
            get() = _signalStrengthBars ?: 0
            set(value) {
                _signalStrengthBars = value
            }

        val hasSignalStrengthBars: Boolean
            get() = _signalStrengthBars != null

        fun clearSignalStrengthBars() {
            _signalStrengthBars = null
        }

        var signalFrequencyMhz: Int
            get() = _signalFrequencyMhz ?: 0
            set(value) {
                _signalFrequencyMhz = value
            }

        val hasSignalFrequencyMhz: Boolean
            get() = _signalFrequencyMhz != null

        fun clearSignalFrequencyMhz() {
            _signalFrequencyMhz = null
        }

        var scanEntryFlags: Int
            get() = _scanEntryFlags ?: 0
            set(value) {
                _scanEntryFlags = value
            }

        val hasScanEntryFlags: Boolean
            get() = _scanEntryFlags != null

        fun clearScanEntryFlags() {
            _scanEntryFlags = null
        }

        var unknownFields: ByteArray = ByteArray(0)

        private var _ssid: String? = null
        private var _signalStrengthBars: Int? = null
        private var _signalFrequencyMhz: Int? = null
        private var _scanEntryFlags: Int? = null

        val isInitialized: Boolean
            get() {
                if (_ssid == null) {
                    return false
                }
                if (_signalStrengthBars == null) {
                    return false
                }
                if (_signalFrequencyMhz == null) {
                    return false
                }
                if (_scanEntryFlags == null) {
                    return false
                }
                return true
            }

        fun <D> decodeMessage(decoder: D) {
            Unit
        }

        fun <V> traverse(visitor: V) {
            Unit
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other !is ScanEntry) {
                return false
            }
            if (_ssid != other._ssid) {
                return false
            }
            if (_signalStrengthBars != other._signalStrengthBars) {
                return false
            }
            if (_signalFrequencyMhz != other._signalFrequencyMhz) {
                return false
            }
            if (_scanEntryFlags != other._scanEntryFlags) {
                return false
            }
            if (!unknownFields.contentEquals(other.unknownFields)) {
                return false
            }
            return true
        }

        override fun hashCode(): Int {
            var result = _ssid?.hashCode() ?: 0
            result = 31 * result + (_signalStrengthBars?.hashCode() ?: 0)
            result = 31 * result + (_signalFrequencyMhz?.hashCode() ?: 0)
            result = 31 * result + (_scanEntryFlags?.hashCode() ?: 0)
            result = 31 * result + unknownFields.contentHashCode()
            return result
        }

        companion object {
            const val protoMessageName: String =
                OpenGopro_ResponseGetApEntries.protoMessageName + ".ScanEntry"

            val _protobuf_nameMap: Map<Int, String> = mapOf(
                1 to "ssid",
                2 to "signal_strength_bars",
                4 to "signal_frequency_mhz",
                5 to "scan_entry_flags",
            )
        }
    }

    private var _result: OpenGopro_EnumResultGeneric? = null
    private var _scanID: Int? = null

    val isInitialized: Boolean
        get() {
            if (_result == null) {
                return false
            }
            if (_scanID == null) {
                return false
            }
            if (!entries.all { it.isInitialized }) {
                return false
            }
            return true
        }

    fun <D> decodeMessage(decoder: D) {
        Unit
    }

    fun <V> traverse(visitor: V) {
        Unit
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is OpenGopro_ResponseGetApEntries) {
            return false
        }
        if (_result != other._result) {
            return false
        }
        if (_scanID != other._scanID) {
            return false
        }
        if (entries != other.entries) {
            return false
        }
        if (!unknownFields.contentEquals(other.unknownFields)) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = _result?.hashCode() ?: 0
        result = 31 * result + (_scanID?.hashCode() ?: 0)
        result = 31 * result + entries.hashCode()
        result = 31 * result + unknownFields.contentHashCode()
        return result
    }

    companion object {
        const val protoMessageName: String = _protobuf_package + ".ResponseGetApEntries"

        val _protobuf_nameMap: Map<Int, String> = mapOf(
            1 to "result",
            2 to "scan_id",
            3 to "entries",
        )
    }
}

class OpenGopro_ResponseStartScanning {
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

    var scanningState: OpenGopro_EnumScanning
        get() = _scanningState ?: OpenGopro_EnumScanning.scanningUnknown
        set(value) {
            _scanningState = value
        }

    val hasScanningState: Boolean
        get() = _scanningState != null

    fun clearScanningState() {
        _scanningState = null
    }

    var unknownFields: ByteArray = ByteArray(0)

    private var _result: OpenGopro_EnumResultGeneric? = null
    private var _scanningState: OpenGopro_EnumScanning? = null

    val isInitialized: Boolean
        get() {
            if (_result == null) {
                return false
            }
            if (_scanningState == null) {
                return false
            }
            return true
        }

    fun <D> decodeMessage(decoder: D) {
        Unit
    }

    fun <V> traverse(visitor: V) {
        Unit
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is OpenGopro_ResponseStartScanning) {
            return false
        }
        if (_result != other._result) {
            return false
        }
        if (_scanningState != other._scanningState) {
            return false
        }
        if (!unknownFields.contentEquals(other.unknownFields)) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = _result?.hashCode() ?: 0
        result = 31 * result + (_scanningState?.hashCode() ?: 0)
        result = 31 * result + unknownFields.contentHashCode()
        return result
    }

    companion object {
        const val protoMessageName: String = _protobuf_package + ".ResponseStartScanning"

        val _protobuf_nameMap: Map<Int, String> = mapOf(
            1 to "result",
            2 to "scanning_state",
        )
    }
}

class OpenGopro_RequestPairingFinish {
    var result: OpenGopro_EnumPairingFinishState
        get() = _result ?: OpenGopro_EnumPairingFinishState.success
        set(value) {
            _result = value
        }

    val hasResult: Boolean
        get() = _result != null

    fun clearResult() {
        _result = null
    }

    var phoneName: String
        get() = _phoneName ?: ""
        set(value) {
            _phoneName = value
        }

    val hasPhoneName: Boolean
        get() = _phoneName != null

    fun clearPhoneName() {
        _phoneName = null
    }

    var unknownFields: ByteArray = ByteArray(0)

    private var _result: OpenGopro_EnumPairingFinishState? = null
    private var _phoneName: String? = null

    val isInitialized: Boolean
        get() {
            if (_result == null) {
                return false
            }
            if (_phoneName == null) {
                return false
            }
            return true
        }

    fun <D> decodeMessage(decoder: D) {
        Unit
    }

    fun <V> traverse(visitor: V) {
        Unit
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is OpenGopro_RequestPairingFinish) {
            return false
        }
        if (_result != other._result) {
            return false
        }
        if (_phoneName != other._phoneName) {
            return false
        }
        if (!unknownFields.contentEquals(other.unknownFields)) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = _result?.hashCode() ?: 0
        result = 31 * result + (_phoneName?.hashCode() ?: 0)
        result = 31 * result + unknownFields.contentHashCode()
        return result
    }

    companion object {
        const val protoMessageName: String = _protobuf_package + ".RequestPairingFinish"

        val _protobuf_nameMap: Map<Int, String> = mapOf(
            1 to "result",
            2 to "phoneName",
        )
    }
}

private const val _protobuf_package = "open_gopro"

private fun byteArraysEqual(a: ByteArray?, b: ByteArray?): Boolean {
    if (a == null) {
        return b == null
    }
    if (b == null) {
        return false
    }
    return a.contentEquals(b)
}
