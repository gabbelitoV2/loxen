package com.moblin.android.integrations.tesla.protobuf

import com.moblin.android.platform.swiftprotobuf.BinaryDecoder
import com.moblin.android.platform.swiftprotobuf.BinaryEncodingVisitor
import com.moblin.android.platform.swiftprotobuf.GeneratedMessage
import com.moblin.android.platform.swiftprotobuf.ProtobufOneofCase
import com.moblin.android.platform.swiftprotobuf.merge
import com.moblin.android.platform.swiftprotobuf.protobufHash

enum class CarServer_Invalid(val rawValue: Int) {
    invalid(0),
    UNRECOGNIZED(-1),
    ;

    companion object {
        val allCases: List<CarServer_Invalid> = listOf(
            invalid,
        )

        fun fromRawValue(rawValue: Int): CarServer_Invalid =
            when (rawValue) {
                0 -> invalid
                else -> UNRECOGNIZED
            }
    }
}

enum class CarServer_MediaPlaybackStatus(val rawValue: Int) {
    stopped(0),
    playing(1),
    paused(2),
    UNRECOGNIZED(-1),
    ;

    companion object {
        val allCases: List<CarServer_MediaPlaybackStatus> = listOf(
            stopped,
            playing,
            paused,
        )

        fun fromRawValue(rawValue: Int): CarServer_MediaPlaybackStatus =
            when (rawValue) {
                0 -> stopped
                1 -> playing
                2 -> paused
                else -> UNRECOGNIZED
            }
    }
}

enum class CarServer_StwHeatLevel(val rawValue: Int) {
    unknown(0),
    off(1),
    low(2),
    high(3),
    UNRECOGNIZED(-1),
    ;

    companion object {
        val allCases: List<CarServer_StwHeatLevel> = listOf(
            unknown,
            off,
            low,
            high,
        )

        fun fromRawValue(rawValue: Int): CarServer_StwHeatLevel =
            when (rawValue) {
                0 -> unknown
                1 -> off
                2 -> low
                3 -> high
                else -> UNRECOGNIZED
            }
    }
}

class CarServer_Void() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (decoder.nextFieldNumber() != null) {
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_Void) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_Void {
        val result = CarServer_Void()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.Void"

        fun with(block: CarServer_Void.() -> Unit): CarServer_Void =
            CarServer_Void().apply(block)
    }
}

class CarServer_LatLong() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var latitude: Float = 0f
        set(value) {
            field = value
            _protobufMutated()
        }

    var longitude: Float = 0f
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularFloatField()?.let { this.latitude = it }
                2 -> decoder.decodeSingularFloatField()?.let { this.longitude = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.latitude.toRawBits() != 0) {
            visitor.visitSingularFloatField(this.latitude, 1)
        }
        if (this.longitude.toRawBits() != 0) {
            visitor.visitSingularFloatField(this.longitude, 2)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_LatLong) return false
        if (this.latitude != other.latitude) return false
        if (this.longitude != other.longitude) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + protobufHash(this.latitude)
        hash = 31 * hash + protobufHash(this.longitude)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_LatLong {
        val result = CarServer_LatLong()
        result.latitude = this.latitude
        result.longitude = this.longitude
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.LatLong"

        fun with(block: CarServer_LatLong.() -> Unit): CarServer_LatLong =
            CarServer_LatLong().apply(block)
    }
}

class CarServer_ChargePortLatchState() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var type: CarServer_ChargePortLatchState.OneOf_Type?
        get() = this._type
        set(value) {
            this._protobufStore_type(this._protobufCopy_type(value))
            _protobufMutated()
        }

    var sna: CarServer_Void
        get() = (this._type as? CarServer_ChargePortLatchState.OneOf_Type.sna)?.value
            ?: _protobufPending(1, { CarServer_Void() }) { this._protobufStore_type(CarServer_ChargePortLatchState.OneOf_Type.sna(it)) }
        set(value) {
            this.type = CarServer_ChargePortLatchState.OneOf_Type.sna(value)
        }

    var disengaged: CarServer_Void
        get() = (this._type as? CarServer_ChargePortLatchState.OneOf_Type.disengaged)?.value
            ?: _protobufPending(2, { CarServer_Void() }) { this._protobufStore_type(CarServer_ChargePortLatchState.OneOf_Type.disengaged(it)) }
        set(value) {
            this.type = CarServer_ChargePortLatchState.OneOf_Type.disengaged(value)
        }

    var engaged: CarServer_Void
        get() = (this._type as? CarServer_ChargePortLatchState.OneOf_Type.engaged)?.value
            ?: _protobufPending(3, { CarServer_Void() }) { this._protobufStore_type(CarServer_ChargePortLatchState.OneOf_Type.engaged(it)) }
        set(value) {
            this.type = CarServer_ChargePortLatchState.OneOf_Type.engaged(value)
        }

    var blocking: CarServer_Void
        get() = (this._type as? CarServer_ChargePortLatchState.OneOf_Type.blocking)?.value
            ?: _protobufPending(4, { CarServer_Void() }) { this._protobufStore_type(CarServer_ChargePortLatchState.OneOf_Type.blocking(it)) }
        set(value) {
            this.type = CarServer_ChargePortLatchState.OneOf_Type.blocking(value)
        }

    sealed class OneOf_Type(value: Any) : ProtobufOneofCase(value) {
        class sna(val value: CarServer_Void) : OneOf_Type(value)
        class disengaged(val value: CarServer_Void) : OneOf_Type(value)
        class engaged(val value: CarServer_Void) : OneOf_Type(value)
        class blocking(val value: CarServer_Void) : OneOf_Type(value)
    }

    private var _type: CarServer_ChargePortLatchState.OneOf_Type? = null

    private fun _protobufStore_type(value: CarServer_ChargePortLatchState.OneOf_Type?) {
        _protobufDropPending(1)
        _protobufDropPending(2)
        _protobufDropPending(3)
        _protobufDropPending(4)
        this._type = value
    }

    private fun _protobufCopy_type(value: CarServer_ChargePortLatchState.OneOf_Type?): CarServer_ChargePortLatchState.OneOf_Type? =
        when (value) {
            is CarServer_ChargePortLatchState.OneOf_Type.sna -> CarServer_ChargePortLatchState.OneOf_Type.sna(value.value.copy())
            is CarServer_ChargePortLatchState.OneOf_Type.disengaged -> CarServer_ChargePortLatchState.OneOf_Type.disengaged(value.value.copy())
            is CarServer_ChargePortLatchState.OneOf_Type.engaged -> CarServer_ChargePortLatchState.OneOf_Type.engaged(value.value.copy())
            is CarServer_ChargePortLatchState.OneOf_Type.blocking -> CarServer_ChargePortLatchState.OneOf_Type.blocking(value.value.copy())
            else -> value
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularMessageField((this._type as? CarServer_ChargePortLatchState.OneOf_Type.sna)?.value) { CarServer_Void() }
                    ?.let { this._protobufStore_type(CarServer_ChargePortLatchState.OneOf_Type.sna(it)) }
                2 -> decoder.decodeSingularMessageField((this._type as? CarServer_ChargePortLatchState.OneOf_Type.disengaged)?.value) { CarServer_Void() }
                    ?.let { this._protobufStore_type(CarServer_ChargePortLatchState.OneOf_Type.disengaged(it)) }
                3 -> decoder.decodeSingularMessageField((this._type as? CarServer_ChargePortLatchState.OneOf_Type.engaged)?.value) { CarServer_Void() }
                    ?.let { this._protobufStore_type(CarServer_ChargePortLatchState.OneOf_Type.engaged(it)) }
                4 -> decoder.decodeSingularMessageField((this._type as? CarServer_ChargePortLatchState.OneOf_Type.blocking)?.value) { CarServer_Void() }
                    ?.let { this._protobufStore_type(CarServer_ChargePortLatchState.OneOf_Type.blocking(it)) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        (this._type as? CarServer_ChargePortLatchState.OneOf_Type.sna)?.let {
            visitor.visitSingularMessageField(it.value, 1)
        }
        (this._type as? CarServer_ChargePortLatchState.OneOf_Type.disengaged)?.let {
            visitor.visitSingularMessageField(it.value, 2)
        }
        (this._type as? CarServer_ChargePortLatchState.OneOf_Type.engaged)?.let {
            visitor.visitSingularMessageField(it.value, 3)
        }
        (this._type as? CarServer_ChargePortLatchState.OneOf_Type.blocking)?.let {
            visitor.visitSingularMessageField(it.value, 4)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_ChargePortLatchState) return false
        if (this._type != other._type) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._type?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ChargePortLatchState {
        val result = CarServer_ChargePortLatchState()
        result._type = this._protobufCopy_type(this._type)
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ChargePortLatchState"

        fun with(block: CarServer_ChargePortLatchState.() -> Unit): CarServer_ChargePortLatchState =
            CarServer_ChargePortLatchState().apply(block)
    }
}

class CarServer_PreconditioningTimes() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var times: CarServer_PreconditioningTimes.OneOf_Times?
        get() = this._times
        set(value) {
            this._protobufStore_times(this._protobufCopy_times(value))
            _protobufMutated()
        }

    var allWeek: CarServer_Void
        get() = (this._times as? CarServer_PreconditioningTimes.OneOf_Times.allWeek)?.value
            ?: _protobufPending(1, { CarServer_Void() }) { this._protobufStore_times(CarServer_PreconditioningTimes.OneOf_Times.allWeek(it)) }
        set(value) {
            this.times = CarServer_PreconditioningTimes.OneOf_Times.allWeek(value)
        }

    var weekdays: CarServer_Void
        get() = (this._times as? CarServer_PreconditioningTimes.OneOf_Times.weekdays)?.value
            ?: _protobufPending(2, { CarServer_Void() }) { this._protobufStore_times(CarServer_PreconditioningTimes.OneOf_Times.weekdays(it)) }
        set(value) {
            this.times = CarServer_PreconditioningTimes.OneOf_Times.weekdays(value)
        }

    sealed class OneOf_Times(value: Any) : ProtobufOneofCase(value) {
        class allWeek(val value: CarServer_Void) : OneOf_Times(value)
        class weekdays(val value: CarServer_Void) : OneOf_Times(value)
    }

    private var _times: CarServer_PreconditioningTimes.OneOf_Times? = null

    private fun _protobufStore_times(value: CarServer_PreconditioningTimes.OneOf_Times?) {
        _protobufDropPending(1)
        _protobufDropPending(2)
        this._times = value
    }

    private fun _protobufCopy_times(value: CarServer_PreconditioningTimes.OneOf_Times?): CarServer_PreconditioningTimes.OneOf_Times? =
        when (value) {
            is CarServer_PreconditioningTimes.OneOf_Times.allWeek -> CarServer_PreconditioningTimes.OneOf_Times.allWeek(value.value.copy())
            is CarServer_PreconditioningTimes.OneOf_Times.weekdays -> CarServer_PreconditioningTimes.OneOf_Times.weekdays(value.value.copy())
            else -> value
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularMessageField((this._times as? CarServer_PreconditioningTimes.OneOf_Times.allWeek)?.value) { CarServer_Void() }
                    ?.let { this._protobufStore_times(CarServer_PreconditioningTimes.OneOf_Times.allWeek(it)) }
                2 -> decoder.decodeSingularMessageField((this._times as? CarServer_PreconditioningTimes.OneOf_Times.weekdays)?.value) { CarServer_Void() }
                    ?.let { this._protobufStore_times(CarServer_PreconditioningTimes.OneOf_Times.weekdays(it)) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        (this._times as? CarServer_PreconditioningTimes.OneOf_Times.allWeek)?.let {
            visitor.visitSingularMessageField(it.value, 1)
        }
        (this._times as? CarServer_PreconditioningTimes.OneOf_Times.weekdays)?.let {
            visitor.visitSingularMessageField(it.value, 2)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_PreconditioningTimes) return false
        if (this._times != other._times) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._times?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_PreconditioningTimes {
        val result = CarServer_PreconditioningTimes()
        result._times = this._protobufCopy_times(this._times)
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.PreconditioningTimes"

        fun with(block: CarServer_PreconditioningTimes.() -> Unit): CarServer_PreconditioningTimes =
            CarServer_PreconditioningTimes().apply(block)
    }
}

class CarServer_OffPeakChargingTimes() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var times: CarServer_OffPeakChargingTimes.OneOf_Times?
        get() = this._times
        set(value) {
            this._protobufStore_times(this._protobufCopy_times(value))
            _protobufMutated()
        }

    var allWeek: CarServer_Void
        get() = (this._times as? CarServer_OffPeakChargingTimes.OneOf_Times.allWeek)?.value
            ?: _protobufPending(1, { CarServer_Void() }) { this._protobufStore_times(CarServer_OffPeakChargingTimes.OneOf_Times.allWeek(it)) }
        set(value) {
            this.times = CarServer_OffPeakChargingTimes.OneOf_Times.allWeek(value)
        }

    var weekdays: CarServer_Void
        get() = (this._times as? CarServer_OffPeakChargingTimes.OneOf_Times.weekdays)?.value
            ?: _protobufPending(2, { CarServer_Void() }) { this._protobufStore_times(CarServer_OffPeakChargingTimes.OneOf_Times.weekdays(it)) }
        set(value) {
            this.times = CarServer_OffPeakChargingTimes.OneOf_Times.weekdays(value)
        }

    sealed class OneOf_Times(value: Any) : ProtobufOneofCase(value) {
        class allWeek(val value: CarServer_Void) : OneOf_Times(value)
        class weekdays(val value: CarServer_Void) : OneOf_Times(value)
    }

    private var _times: CarServer_OffPeakChargingTimes.OneOf_Times? = null

    private fun _protobufStore_times(value: CarServer_OffPeakChargingTimes.OneOf_Times?) {
        _protobufDropPending(1)
        _protobufDropPending(2)
        this._times = value
    }

    private fun _protobufCopy_times(value: CarServer_OffPeakChargingTimes.OneOf_Times?): CarServer_OffPeakChargingTimes.OneOf_Times? =
        when (value) {
            is CarServer_OffPeakChargingTimes.OneOf_Times.allWeek -> CarServer_OffPeakChargingTimes.OneOf_Times.allWeek(value.value.copy())
            is CarServer_OffPeakChargingTimes.OneOf_Times.weekdays -> CarServer_OffPeakChargingTimes.OneOf_Times.weekdays(value.value.copy())
            else -> value
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularMessageField((this._times as? CarServer_OffPeakChargingTimes.OneOf_Times.allWeek)?.value) { CarServer_Void() }
                    ?.let { this._protobufStore_times(CarServer_OffPeakChargingTimes.OneOf_Times.allWeek(it)) }
                2 -> decoder.decodeSingularMessageField((this._times as? CarServer_OffPeakChargingTimes.OneOf_Times.weekdays)?.value) { CarServer_Void() }
                    ?.let { this._protobufStore_times(CarServer_OffPeakChargingTimes.OneOf_Times.weekdays(it)) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        (this._times as? CarServer_OffPeakChargingTimes.OneOf_Times.allWeek)?.let {
            visitor.visitSingularMessageField(it.value, 1)
        }
        (this._times as? CarServer_OffPeakChargingTimes.OneOf_Times.weekdays)?.let {
            visitor.visitSingularMessageField(it.value, 2)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_OffPeakChargingTimes) return false
        if (this._times != other._times) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._times?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_OffPeakChargingTimes {
        val result = CarServer_OffPeakChargingTimes()
        result._times = this._protobufCopy_times(this._times)
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.OffPeakChargingTimes"

        fun with(block: CarServer_OffPeakChargingTimes.() -> Unit): CarServer_OffPeakChargingTimes =
            CarServer_OffPeakChargingTimes().apply(block)
    }
}

class CarServer_ChargeSchedule() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var id: ULong = 0uL
        set(value) {
            field = value
            _protobufMutated()
        }

    var name: String = ""
        set(value) {
            field = value
            _protobufMutated()
        }

    var daysOfWeek: Int = 0
        set(value) {
            field = value
            _protobufMutated()
        }

    var startEnabled: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    var startTime: Int = 0
        set(value) {
            field = value
            _protobufMutated()
        }

    var endEnabled: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    var endTime: Int = 0
        set(value) {
            field = value
            _protobufMutated()
        }

    var oneTime: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    var enabled: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    var latitude: Float = 0f
        set(value) {
            field = value
            _protobufMutated()
        }

    var longitude: Float = 0f
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularUInt64Field()?.let { this.id = it }
                2 -> decoder.decodeSingularStringField()?.let { this.name = it }
                3 -> decoder.decodeSingularInt32Field()?.let { this.daysOfWeek = it }
                4 -> decoder.decodeSingularBoolField()?.let { this.startEnabled = it }
                5 -> decoder.decodeSingularInt32Field()?.let { this.startTime = it }
                6 -> decoder.decodeSingularBoolField()?.let { this.endEnabled = it }
                7 -> decoder.decodeSingularInt32Field()?.let { this.endTime = it }
                8 -> decoder.decodeSingularBoolField()?.let { this.oneTime = it }
                9 -> decoder.decodeSingularBoolField()?.let { this.enabled = it }
                10 -> decoder.decodeSingularFloatField()?.let { this.latitude = it }
                11 -> decoder.decodeSingularFloatField()?.let { this.longitude = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.id != 0uL) {
            visitor.visitSingularUInt64Field(this.id, 1)
        }
        if (this.name.isNotEmpty()) {
            visitor.visitSingularStringField(this.name, 2)
        }
        if (this.daysOfWeek != 0) {
            visitor.visitSingularInt32Field(this.daysOfWeek, 3)
        }
        if (this.startEnabled) {
            visitor.visitSingularBoolField(this.startEnabled, 4)
        }
        if (this.startTime != 0) {
            visitor.visitSingularInt32Field(this.startTime, 5)
        }
        if (this.endEnabled) {
            visitor.visitSingularBoolField(this.endEnabled, 6)
        }
        if (this.endTime != 0) {
            visitor.visitSingularInt32Field(this.endTime, 7)
        }
        if (this.oneTime) {
            visitor.visitSingularBoolField(this.oneTime, 8)
        }
        if (this.enabled) {
            visitor.visitSingularBoolField(this.enabled, 9)
        }
        if (this.latitude.toRawBits() != 0) {
            visitor.visitSingularFloatField(this.latitude, 10)
        }
        if (this.longitude.toRawBits() != 0) {
            visitor.visitSingularFloatField(this.longitude, 11)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_ChargeSchedule) return false
        if (this.id != other.id) return false
        if (this.name != other.name) return false
        if (this.daysOfWeek != other.daysOfWeek) return false
        if (this.startEnabled != other.startEnabled) return false
        if (this.startTime != other.startTime) return false
        if (this.endEnabled != other.endEnabled) return false
        if (this.endTime != other.endTime) return false
        if (this.oneTime != other.oneTime) return false
        if (this.enabled != other.enabled) return false
        if (this.latitude != other.latitude) return false
        if (this.longitude != other.longitude) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.id.hashCode()
        hash = 31 * hash + this.name.hashCode()
        hash = 31 * hash + this.daysOfWeek.hashCode()
        hash = 31 * hash + this.startEnabled.hashCode()
        hash = 31 * hash + this.startTime.hashCode()
        hash = 31 * hash + this.endEnabled.hashCode()
        hash = 31 * hash + this.endTime.hashCode()
        hash = 31 * hash + this.oneTime.hashCode()
        hash = 31 * hash + this.enabled.hashCode()
        hash = 31 * hash + protobufHash(this.latitude)
        hash = 31 * hash + protobufHash(this.longitude)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ChargeSchedule {
        val result = CarServer_ChargeSchedule()
        result.id = this.id
        result.name = this.name
        result.daysOfWeek = this.daysOfWeek
        result.startEnabled = this.startEnabled
        result.startTime = this.startTime
        result.endEnabled = this.endEnabled
        result.endTime = this.endTime
        result.oneTime = this.oneTime
        result.enabled = this.enabled
        result.latitude = this.latitude
        result.longitude = this.longitude
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ChargeSchedule"

        fun with(block: CarServer_ChargeSchedule.() -> Unit): CarServer_ChargeSchedule =
            CarServer_ChargeSchedule().apply(block)
    }
}

class CarServer_PreconditionSchedule() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var id: ULong = 0uL
        set(value) {
            field = value
            _protobufMutated()
        }

    var name: String = ""
        set(value) {
            field = value
            _protobufMutated()
        }

    var daysOfWeek: Int = 0
        set(value) {
            field = value
            _protobufMutated()
        }

    var preconditionTime: Int = 0
        set(value) {
            field = value
            _protobufMutated()
        }

    var oneTime: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    var enabled: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    var latitude: Float = 0f
        set(value) {
            field = value
            _protobufMutated()
        }

    var longitude: Float = 0f
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularUInt64Field()?.let { this.id = it }
                2 -> decoder.decodeSingularStringField()?.let { this.name = it }
                3 -> decoder.decodeSingularInt32Field()?.let { this.daysOfWeek = it }
                4 -> decoder.decodeSingularInt32Field()?.let { this.preconditionTime = it }
                5 -> decoder.decodeSingularBoolField()?.let { this.oneTime = it }
                6 -> decoder.decodeSingularBoolField()?.let { this.enabled = it }
                7 -> decoder.decodeSingularFloatField()?.let { this.latitude = it }
                8 -> decoder.decodeSingularFloatField()?.let { this.longitude = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.id != 0uL) {
            visitor.visitSingularUInt64Field(this.id, 1)
        }
        if (this.name.isNotEmpty()) {
            visitor.visitSingularStringField(this.name, 2)
        }
        if (this.daysOfWeek != 0) {
            visitor.visitSingularInt32Field(this.daysOfWeek, 3)
        }
        if (this.preconditionTime != 0) {
            visitor.visitSingularInt32Field(this.preconditionTime, 4)
        }
        if (this.oneTime) {
            visitor.visitSingularBoolField(this.oneTime, 5)
        }
        if (this.enabled) {
            visitor.visitSingularBoolField(this.enabled, 6)
        }
        if (this.latitude.toRawBits() != 0) {
            visitor.visitSingularFloatField(this.latitude, 7)
        }
        if (this.longitude.toRawBits() != 0) {
            visitor.visitSingularFloatField(this.longitude, 8)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_PreconditionSchedule) return false
        if (this.id != other.id) return false
        if (this.name != other.name) return false
        if (this.daysOfWeek != other.daysOfWeek) return false
        if (this.preconditionTime != other.preconditionTime) return false
        if (this.oneTime != other.oneTime) return false
        if (this.enabled != other.enabled) return false
        if (this.latitude != other.latitude) return false
        if (this.longitude != other.longitude) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.id.hashCode()
        hash = 31 * hash + this.name.hashCode()
        hash = 31 * hash + this.daysOfWeek.hashCode()
        hash = 31 * hash + this.preconditionTime.hashCode()
        hash = 31 * hash + this.oneTime.hashCode()
        hash = 31 * hash + this.enabled.hashCode()
        hash = 31 * hash + protobufHash(this.latitude)
        hash = 31 * hash + protobufHash(this.longitude)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_PreconditionSchedule {
        val result = CarServer_PreconditionSchedule()
        result.id = this.id
        result.name = this.name
        result.daysOfWeek = this.daysOfWeek
        result.preconditionTime = this.preconditionTime
        result.oneTime = this.oneTime
        result.enabled = this.enabled
        result.latitude = this.latitude
        result.longitude = this.longitude
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.PreconditionSchedule"

        fun with(block: CarServer_PreconditionSchedule.() -> Unit): CarServer_PreconditionSchedule =
            CarServer_PreconditionSchedule().apply(block)
    }
}
