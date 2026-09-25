package com.moblin.android.integrations.tesla.protobuf

import com.moblin.android.platform.swiftprotobuf.BinaryDecoder
import com.moblin.android.platform.swiftprotobuf.BinaryEncodingVisitor
import com.moblin.android.platform.swiftprotobuf.GeneratedMessage
import com.moblin.android.platform.swiftprotobuf.Google_Protobuf_Timestamp
import com.moblin.android.platform.swiftprotobuf.ProtobufList
import com.moblin.android.platform.swiftprotobuf.ProtobufOneofCase
import com.moblin.android.platform.swiftprotobuf.merge

enum class CarServer_MediaSourceType(val rawValue: Int) {
    none(0),
    am(1),
    fm(2),
    xm(3),
    slacker(5),
    localFiles(6),
    iPod(7),
    bluetooth(8),
    auxIn(9),
    dab(10),
    rdio(11),
    spotify(12),
    usradio(13),
    euradio(14),
    mediaFile(16),
    tuneIn(17),
    stingray(18),
    siriusXm(19),
    tidal(20),
    qqmusic(21),
    qqmusic2(22),
    ximalaya(23),
    onlineRadio(24),
    onlineRadio2(25),
    netEaseMusic(26),
    browser(28),
    theater(29),
    game(30),
    tutorial(31),
    toybox(32),
    recentsFavorites(33),
    homeApps(34),
    search(35),
    UNRECOGNIZED(-1),
    ;

    companion object {
        val allCases: List<CarServer_MediaSourceType> = listOf(
            none,
            am,
            fm,
            xm,
            slacker,
            localFiles,
            iPod,
            bluetooth,
            auxIn,
            dab,
            rdio,
            spotify,
            usradio,
            euradio,
            mediaFile,
            tuneIn,
            stingray,
            siriusXm,
            tidal,
            qqmusic,
            qqmusic2,
            ximalaya,
            onlineRadio,
            onlineRadio2,
            netEaseMusic,
            browser,
            theater,
            game,
            tutorial,
            toybox,
            recentsFavorites,
            homeApps,
            search,
        )

        fun fromRawValue(rawValue: Int): CarServer_MediaSourceType =
            when (rawValue) {
                0 -> none
                1 -> am
                2 -> fm
                3 -> xm
                5 -> slacker
                6 -> localFiles
                7 -> iPod
                8 -> bluetooth
                9 -> auxIn
                10 -> dab
                11 -> rdio
                12 -> spotify
                13 -> usradio
                14 -> euradio
                16 -> mediaFile
                17 -> tuneIn
                18 -> stingray
                19 -> siriusXm
                20 -> tidal
                21 -> qqmusic
                22 -> qqmusic2
                23 -> ximalaya
                24 -> onlineRadio
                25 -> onlineRadio2
                26 -> netEaseMusic
                28 -> browser
                29 -> theater
                30 -> game
                31 -> tutorial
                32 -> toybox
                33 -> recentsFavorites
                34 -> homeApps
                35 -> search
                else -> UNRECOGNIZED
            }
    }
}

class CarServer_VehicleData() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var chargeState: CarServer_ChargeState
        get() = this._chargeState ?: _protobufPending(3, { CarServer_ChargeState() }) { this._chargeState = it }
        set(value) {
            _protobufDropPending(3)
            this._chargeState = value.copy()
            _protobufMutated()
        }

    val hasChargeState: Boolean
        get() = this._chargeState != null

    fun clearChargeState() {
        _protobufDropPending(3)
        this._chargeState = null
        _protobufMutated()
    }

    var climateState: CarServer_ClimateState
        get() = this._climateState ?: _protobufPending(4, { CarServer_ClimateState() }) { this._climateState = it }
        set(value) {
            _protobufDropPending(4)
            this._climateState = value.copy()
            _protobufMutated()
        }

    val hasClimateState: Boolean
        get() = this._climateState != null

    fun clearClimateState() {
        _protobufDropPending(4)
        this._climateState = null
        _protobufMutated()
    }

    var driveState: CarServer_DriveState
        get() = this._driveState ?: _protobufPending(5, { CarServer_DriveState() }) { this._driveState = it }
        set(value) {
            _protobufDropPending(5)
            this._driveState = value.copy()
            _protobufMutated()
        }

    val hasDriveState: Boolean
        get() = this._driveState != null

    fun clearDriveState() {
        _protobufDropPending(5)
        this._driveState = null
        _protobufMutated()
    }

    var locationState: CarServer_LocationState
        get() = this._locationState ?: _protobufPending(8, { CarServer_LocationState() }) { this._locationState = it }
        set(value) {
            _protobufDropPending(8)
            this._locationState = value.copy()
            _protobufMutated()
        }

    val hasLocationState: Boolean
        get() = this._locationState != null

    fun clearLocationState() {
        _protobufDropPending(8)
        this._locationState = null
        _protobufMutated()
    }

    var closuresState: CarServer_ClosuresState
        get() = this._closuresState ?: _protobufPending(9, { CarServer_ClosuresState() }) { this._closuresState = it }
        set(value) {
            _protobufDropPending(9)
            this._closuresState = value.copy()
            _protobufMutated()
        }

    val hasClosuresState: Boolean
        get() = this._closuresState != null

    fun clearClosuresState() {
        _protobufDropPending(9)
        this._closuresState = null
        _protobufMutated()
    }

    var chargeScheduleState: CarServer_ChargeScheduleState
        get() = this._chargeScheduleState ?: _protobufPending(15, { CarServer_ChargeScheduleState() }) { this._chargeScheduleState = it }
        set(value) {
            _protobufDropPending(15)
            this._chargeScheduleState = value.copy()
            _protobufMutated()
        }

    val hasChargeScheduleState: Boolean
        get() = this._chargeScheduleState != null

    fun clearChargeScheduleState() {
        _protobufDropPending(15)
        this._chargeScheduleState = null
        _protobufMutated()
    }

    var preconditioningScheduleState: CarServer_PreconditioningScheduleState
        get() = this._preconditioningScheduleState ?: _protobufPending(16, { CarServer_PreconditioningScheduleState() }) { this._preconditioningScheduleState = it }
        set(value) {
            _protobufDropPending(16)
            this._preconditioningScheduleState = value.copy()
            _protobufMutated()
        }

    val hasPreconditioningScheduleState: Boolean
        get() = this._preconditioningScheduleState != null

    fun clearPreconditioningScheduleState() {
        _protobufDropPending(16)
        this._preconditioningScheduleState = null
        _protobufMutated()
    }

    var tirePressureState: CarServer_TirePressureState
        get() = this._tirePressureState ?: _protobufPending(19, { CarServer_TirePressureState() }) { this._tirePressureState = it }
        set(value) {
            _protobufDropPending(19)
            this._tirePressureState = value.copy()
            _protobufMutated()
        }

    val hasTirePressureState: Boolean
        get() = this._tirePressureState != null

    fun clearTirePressureState() {
        _protobufDropPending(19)
        this._tirePressureState = null
        _protobufMutated()
    }

    var mediaState: CarServer_MediaState
        get() = this._mediaState ?: _protobufPending(20, { CarServer_MediaState() }) { this._mediaState = it }
        set(value) {
            _protobufDropPending(20)
            this._mediaState = value.copy()
            _protobufMutated()
        }

    val hasMediaState: Boolean
        get() = this._mediaState != null

    fun clearMediaState() {
        _protobufDropPending(20)
        this._mediaState = null
        _protobufMutated()
    }

    var mediaDetailState: CarServer_MediaDetailState
        get() = this._mediaDetailState ?: _protobufPending(21, { CarServer_MediaDetailState() }) { this._mediaDetailState = it }
        set(value) {
            _protobufDropPending(21)
            this._mediaDetailState = value.copy()
            _protobufMutated()
        }

    val hasMediaDetailState: Boolean
        get() = this._mediaDetailState != null

    fun clearMediaDetailState() {
        _protobufDropPending(21)
        this._mediaDetailState = null
        _protobufMutated()
    }

    var softwareUpdateState: CarServer_SoftwareUpdateState
        get() = this._softwareUpdateState ?: _protobufPending(23, { CarServer_SoftwareUpdateState() }) { this._softwareUpdateState = it }
        set(value) {
            _protobufDropPending(23)
            this._softwareUpdateState = value.copy()
            _protobufMutated()
        }

    val hasSoftwareUpdateState: Boolean
        get() = this._softwareUpdateState != null

    fun clearSoftwareUpdateState() {
        _protobufDropPending(23)
        this._softwareUpdateState = null
        _protobufMutated()
    }

    var parentalControlsState: CarServer_ParentalControlsState
        get() = this._parentalControlsState ?: _protobufPending(24, { CarServer_ParentalControlsState() }) { this._parentalControlsState = it }
        set(value) {
            _protobufDropPending(24)
            this._parentalControlsState = value.copy()
            _protobufMutated()
        }

    val hasParentalControlsState: Boolean
        get() = this._parentalControlsState != null

    fun clearParentalControlsState() {
        _protobufDropPending(24)
        this._parentalControlsState = null
        _protobufMutated()
    }

    private var _chargeState: CarServer_ChargeState? = null
    private var _climateState: CarServer_ClimateState? = null
    private var _driveState: CarServer_DriveState? = null
    private var _locationState: CarServer_LocationState? = null
    private var _closuresState: CarServer_ClosuresState? = null
    private var _chargeScheduleState: CarServer_ChargeScheduleState? = null
    private var _preconditioningScheduleState: CarServer_PreconditioningScheduleState? = null
    private var _tirePressureState: CarServer_TirePressureState? = null
    private var _mediaState: CarServer_MediaState? = null
    private var _mediaDetailState: CarServer_MediaDetailState? = null
    private var _softwareUpdateState: CarServer_SoftwareUpdateState? = null
    private var _parentalControlsState: CarServer_ParentalControlsState? = null

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                3 -> decoder.decodeSingularMessageField(this._chargeState) { CarServer_ChargeState() }?.let {
                    _protobufDropPending(3)
                    this._chargeState = it
                }
                4 -> decoder.decodeSingularMessageField(this._climateState) { CarServer_ClimateState() }?.let {
                    _protobufDropPending(4)
                    this._climateState = it
                }
                5 -> decoder.decodeSingularMessageField(this._driveState) { CarServer_DriveState() }?.let {
                    _protobufDropPending(5)
                    this._driveState = it
                }
                8 -> decoder.decodeSingularMessageField(this._locationState) { CarServer_LocationState() }?.let {
                    _protobufDropPending(8)
                    this._locationState = it
                }
                9 -> decoder.decodeSingularMessageField(this._closuresState) { CarServer_ClosuresState() }?.let {
                    _protobufDropPending(9)
                    this._closuresState = it
                }
                15 -> decoder.decodeSingularMessageField(this._chargeScheduleState) { CarServer_ChargeScheduleState() }?.let {
                    _protobufDropPending(15)
                    this._chargeScheduleState = it
                }
                16 -> decoder.decodeSingularMessageField(this._preconditioningScheduleState) { CarServer_PreconditioningScheduleState() }?.let {
                    _protobufDropPending(16)
                    this._preconditioningScheduleState = it
                }
                19 -> decoder.decodeSingularMessageField(this._tirePressureState) { CarServer_TirePressureState() }?.let {
                    _protobufDropPending(19)
                    this._tirePressureState = it
                }
                20 -> decoder.decodeSingularMessageField(this._mediaState) { CarServer_MediaState() }?.let {
                    _protobufDropPending(20)
                    this._mediaState = it
                }
                21 -> decoder.decodeSingularMessageField(this._mediaDetailState) { CarServer_MediaDetailState() }?.let {
                    _protobufDropPending(21)
                    this._mediaDetailState = it
                }
                23 -> decoder.decodeSingularMessageField(this._softwareUpdateState) { CarServer_SoftwareUpdateState() }?.let {
                    _protobufDropPending(23)
                    this._softwareUpdateState = it
                }
                24 -> decoder.decodeSingularMessageField(this._parentalControlsState) { CarServer_ParentalControlsState() }?.let {
                    _protobufDropPending(24)
                    this._parentalControlsState = it
                }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        this._chargeState?.let {
            visitor.visitSingularMessageField(it, 3)
        }
        this._climateState?.let {
            visitor.visitSingularMessageField(it, 4)
        }
        this._driveState?.let {
            visitor.visitSingularMessageField(it, 5)
        }
        this._locationState?.let {
            visitor.visitSingularMessageField(it, 8)
        }
        this._closuresState?.let {
            visitor.visitSingularMessageField(it, 9)
        }
        this._chargeScheduleState?.let {
            visitor.visitSingularMessageField(it, 15)
        }
        this._preconditioningScheduleState?.let {
            visitor.visitSingularMessageField(it, 16)
        }
        this._tirePressureState?.let {
            visitor.visitSingularMessageField(it, 19)
        }
        this._mediaState?.let {
            visitor.visitSingularMessageField(it, 20)
        }
        this._mediaDetailState?.let {
            visitor.visitSingularMessageField(it, 21)
        }
        this._softwareUpdateState?.let {
            visitor.visitSingularMessageField(it, 23)
        }
        this._parentalControlsState?.let {
            visitor.visitSingularMessageField(it, 24)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_VehicleData) return false
        if (this._chargeState != other._chargeState) return false
        if (this._climateState != other._climateState) return false
        if (this._driveState != other._driveState) return false
        if (this._locationState != other._locationState) return false
        if (this._closuresState != other._closuresState) return false
        if (this._chargeScheduleState != other._chargeScheduleState) return false
        if (this._preconditioningScheduleState != other._preconditioningScheduleState) return false
        if (this._tirePressureState != other._tirePressureState) return false
        if (this._mediaState != other._mediaState) return false
        if (this._mediaDetailState != other._mediaDetailState) return false
        if (this._softwareUpdateState != other._softwareUpdateState) return false
        if (this._parentalControlsState != other._parentalControlsState) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._chargeState?.hashCode() ?: 0)
        hash = 31 * hash + (this._climateState?.hashCode() ?: 0)
        hash = 31 * hash + (this._driveState?.hashCode() ?: 0)
        hash = 31 * hash + (this._locationState?.hashCode() ?: 0)
        hash = 31 * hash + (this._closuresState?.hashCode() ?: 0)
        hash = 31 * hash + (this._chargeScheduleState?.hashCode() ?: 0)
        hash = 31 * hash + (this._preconditioningScheduleState?.hashCode() ?: 0)
        hash = 31 * hash + (this._tirePressureState?.hashCode() ?: 0)
        hash = 31 * hash + (this._mediaState?.hashCode() ?: 0)
        hash = 31 * hash + (this._mediaDetailState?.hashCode() ?: 0)
        hash = 31 * hash + (this._softwareUpdateState?.hashCode() ?: 0)
        hash = 31 * hash + (this._parentalControlsState?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_VehicleData {
        val result = CarServer_VehicleData()
        result._chargeState = this._chargeState?.copy()
        result._climateState = this._climateState?.copy()
        result._driveState = this._driveState?.copy()
        result._locationState = this._locationState?.copy()
        result._closuresState = this._closuresState?.copy()
        result._chargeScheduleState = this._chargeScheduleState?.copy()
        result._preconditioningScheduleState = this._preconditioningScheduleState?.copy()
        result._tirePressureState = this._tirePressureState?.copy()
        result._mediaState = this._mediaState?.copy()
        result._mediaDetailState = this._mediaDetailState?.copy()
        result._softwareUpdateState = this._softwareUpdateState?.copy()
        result._parentalControlsState = this._parentalControlsState?.copy()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.VehicleData"

        fun with(block: CarServer_VehicleData.() -> Unit): CarServer_VehicleData =
            CarServer_VehicleData().apply(block)
    }
}

class CarServer_ClosuresState() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var optionalDoorOpenDriverFront: CarServer_ClosuresState.OneOf_OptionalDoorOpenDriverFront?
        get() = this._optionalDoorOpenDriverFront
        set(value) {
            this._optionalDoorOpenDriverFront = value
            _protobufMutated()
        }

    var doorOpenDriverFront: Boolean
        get() = (this._optionalDoorOpenDriverFront as? CarServer_ClosuresState.OneOf_OptionalDoorOpenDriverFront.doorOpenDriverFront)?.value ?: false
        set(value) {
            this.optionalDoorOpenDriverFront = CarServer_ClosuresState.OneOf_OptionalDoorOpenDriverFront.doorOpenDriverFront(value)
        }

    var optionalDoorOpenDriverRear: CarServer_ClosuresState.OneOf_OptionalDoorOpenDriverRear?
        get() = this._optionalDoorOpenDriverRear
        set(value) {
            this._optionalDoorOpenDriverRear = value
            _protobufMutated()
        }

    var doorOpenDriverRear: Boolean
        get() = (this._optionalDoorOpenDriverRear as? CarServer_ClosuresState.OneOf_OptionalDoorOpenDriverRear.doorOpenDriverRear)?.value ?: false
        set(value) {
            this.optionalDoorOpenDriverRear = CarServer_ClosuresState.OneOf_OptionalDoorOpenDriverRear.doorOpenDriverRear(value)
        }

    var optionalDoorOpenPassengerFront: CarServer_ClosuresState.OneOf_OptionalDoorOpenPassengerFront?
        get() = this._optionalDoorOpenPassengerFront
        set(value) {
            this._optionalDoorOpenPassengerFront = value
            _protobufMutated()
        }

    var doorOpenPassengerFront: Boolean
        get() = (this._optionalDoorOpenPassengerFront as? CarServer_ClosuresState.OneOf_OptionalDoorOpenPassengerFront.doorOpenPassengerFront)?.value ?: false
        set(value) {
            this.optionalDoorOpenPassengerFront = CarServer_ClosuresState.OneOf_OptionalDoorOpenPassengerFront.doorOpenPassengerFront(value)
        }

    var optionalDoorOpenPassengerRear: CarServer_ClosuresState.OneOf_OptionalDoorOpenPassengerRear?
        get() = this._optionalDoorOpenPassengerRear
        set(value) {
            this._optionalDoorOpenPassengerRear = value
            _protobufMutated()
        }

    var doorOpenPassengerRear: Boolean
        get() = (this._optionalDoorOpenPassengerRear as? CarServer_ClosuresState.OneOf_OptionalDoorOpenPassengerRear.doorOpenPassengerRear)?.value ?: false
        set(value) {
            this.optionalDoorOpenPassengerRear = CarServer_ClosuresState.OneOf_OptionalDoorOpenPassengerRear.doorOpenPassengerRear(value)
        }

    var optionalDoorOpenTrunkFront: CarServer_ClosuresState.OneOf_OptionalDoorOpenTrunkFront?
        get() = this._optionalDoorOpenTrunkFront
        set(value) {
            this._optionalDoorOpenTrunkFront = value
            _protobufMutated()
        }

    var doorOpenTrunkFront: Boolean
        get() = (this._optionalDoorOpenTrunkFront as? CarServer_ClosuresState.OneOf_OptionalDoorOpenTrunkFront.doorOpenTrunkFront)?.value ?: false
        set(value) {
            this.optionalDoorOpenTrunkFront = CarServer_ClosuresState.OneOf_OptionalDoorOpenTrunkFront.doorOpenTrunkFront(value)
        }

    var optionalDoorOpenTrunkRear: CarServer_ClosuresState.OneOf_OptionalDoorOpenTrunkRear?
        get() = this._optionalDoorOpenTrunkRear
        set(value) {
            this._optionalDoorOpenTrunkRear = value
            _protobufMutated()
        }

    var doorOpenTrunkRear: Boolean
        get() = (this._optionalDoorOpenTrunkRear as? CarServer_ClosuresState.OneOf_OptionalDoorOpenTrunkRear.doorOpenTrunkRear)?.value ?: false
        set(value) {
            this.optionalDoorOpenTrunkRear = CarServer_ClosuresState.OneOf_OptionalDoorOpenTrunkRear.doorOpenTrunkRear(value)
        }

    var optionalWindowOpenDriverFront: CarServer_ClosuresState.OneOf_OptionalWindowOpenDriverFront?
        get() = this._optionalWindowOpenDriverFront
        set(value) {
            this._optionalWindowOpenDriverFront = value
            _protobufMutated()
        }

    var windowOpenDriverFront: Boolean
        get() = (this._optionalWindowOpenDriverFront as? CarServer_ClosuresState.OneOf_OptionalWindowOpenDriverFront.windowOpenDriverFront)?.value ?: false
        set(value) {
            this.optionalWindowOpenDriverFront = CarServer_ClosuresState.OneOf_OptionalWindowOpenDriverFront.windowOpenDriverFront(value)
        }

    var optionalWindowOpenPassengerFront: CarServer_ClosuresState.OneOf_OptionalWindowOpenPassengerFront?
        get() = this._optionalWindowOpenPassengerFront
        set(value) {
            this._optionalWindowOpenPassengerFront = value
            _protobufMutated()
        }

    var windowOpenPassengerFront: Boolean
        get() = (this._optionalWindowOpenPassengerFront as? CarServer_ClosuresState.OneOf_OptionalWindowOpenPassengerFront.windowOpenPassengerFront)?.value ?: false
        set(value) {
            this.optionalWindowOpenPassengerFront = CarServer_ClosuresState.OneOf_OptionalWindowOpenPassengerFront.windowOpenPassengerFront(value)
        }

    var optionalWindowOpenDriverRear: CarServer_ClosuresState.OneOf_OptionalWindowOpenDriverRear?
        get() = this._optionalWindowOpenDriverRear
        set(value) {
            this._optionalWindowOpenDriverRear = value
            _protobufMutated()
        }

    var windowOpenDriverRear: Boolean
        get() = (this._optionalWindowOpenDriverRear as? CarServer_ClosuresState.OneOf_OptionalWindowOpenDriverRear.windowOpenDriverRear)?.value ?: false
        set(value) {
            this.optionalWindowOpenDriverRear = CarServer_ClosuresState.OneOf_OptionalWindowOpenDriverRear.windowOpenDriverRear(value)
        }

    var optionalWindowOpenPassengerRear: CarServer_ClosuresState.OneOf_OptionalWindowOpenPassengerRear?
        get() = this._optionalWindowOpenPassengerRear
        set(value) {
            this._optionalWindowOpenPassengerRear = value
            _protobufMutated()
        }

    var windowOpenPassengerRear: Boolean
        get() = (this._optionalWindowOpenPassengerRear as? CarServer_ClosuresState.OneOf_OptionalWindowOpenPassengerRear.windowOpenPassengerRear)?.value ?: false
        set(value) {
            this.optionalWindowOpenPassengerRear = CarServer_ClosuresState.OneOf_OptionalWindowOpenPassengerRear.windowOpenPassengerRear(value)
        }

    var sunRoofState: CarServer_ClosuresState.SunRoofState
        get() = this._sunRoofState ?: _protobufPending(11, { CarServer_ClosuresState.SunRoofState() }) { this._sunRoofState = it }
        set(value) {
            _protobufDropPending(11)
            this._sunRoofState = value.copy()
            _protobufMutated()
        }

    val hasSunRoofState: Boolean
        get() = this._sunRoofState != null

    fun clearSunRoofState() {
        _protobufDropPending(11)
        this._sunRoofState = null
        _protobufMutated()
    }

    var optionalSunRoofPercentOpen: CarServer_ClosuresState.OneOf_OptionalSunRoofPercentOpen?
        get() = this._optionalSunRoofPercentOpen
        set(value) {
            this._optionalSunRoofPercentOpen = value
            _protobufMutated()
        }

    var sunRoofPercentOpen: Int
        get() = (this._optionalSunRoofPercentOpen as? CarServer_ClosuresState.OneOf_OptionalSunRoofPercentOpen.sunRoofPercentOpen)?.value ?: 0
        set(value) {
            this.optionalSunRoofPercentOpen = CarServer_ClosuresState.OneOf_OptionalSunRoofPercentOpen.sunRoofPercentOpen(value)
        }

    var optionalLocked: CarServer_ClosuresState.OneOf_OptionalLocked?
        get() = this._optionalLocked
        set(value) {
            this._optionalLocked = value
            _protobufMutated()
        }

    var locked: Boolean
        get() = (this._optionalLocked as? CarServer_ClosuresState.OneOf_OptionalLocked.locked)?.value ?: false
        set(value) {
            this.optionalLocked = CarServer_ClosuresState.OneOf_OptionalLocked.locked(value)
        }

    var optionalIsUserPresent: CarServer_ClosuresState.OneOf_OptionalIsUserPresent?
        get() = this._optionalIsUserPresent
        set(value) {
            this._optionalIsUserPresent = value
            _protobufMutated()
        }

    var isUserPresent: Boolean
        get() = (this._optionalIsUserPresent as? CarServer_ClosuresState.OneOf_OptionalIsUserPresent.isUserPresent)?.value ?: false
        set(value) {
            this.optionalIsUserPresent = CarServer_ClosuresState.OneOf_OptionalIsUserPresent.isUserPresent(value)
        }

    var centerDisplayState: CarServer_ClosuresState.DisplayState
        get() = this._centerDisplayState ?: _protobufPending(15, { CarServer_ClosuresState.DisplayState() }) { this._centerDisplayState = it }
        set(value) {
            _protobufDropPending(15)
            this._centerDisplayState = value.copy()
            _protobufMutated()
        }

    val hasCenterDisplayState: Boolean
        get() = this._centerDisplayState != null

    fun clearCenterDisplayState() {
        _protobufDropPending(15)
        this._centerDisplayState = null
        _protobufMutated()
    }

    var optionalRemoteStart: CarServer_ClosuresState.OneOf_OptionalRemoteStart?
        get() = this._optionalRemoteStart
        set(value) {
            this._optionalRemoteStart = value
            _protobufMutated()
        }

    var remoteStart: Boolean
        get() = (this._optionalRemoteStart as? CarServer_ClosuresState.OneOf_OptionalRemoteStart.remoteStart)?.value ?: false
        set(value) {
            this.optionalRemoteStart = CarServer_ClosuresState.OneOf_OptionalRemoteStart.remoteStart(value)
        }

    var optionalValetMode: CarServer_ClosuresState.OneOf_OptionalValetMode?
        get() = this._optionalValetMode
        set(value) {
            this._optionalValetMode = value
            _protobufMutated()
        }

    var valetMode: Boolean
        get() = (this._optionalValetMode as? CarServer_ClosuresState.OneOf_OptionalValetMode.valetMode)?.value ?: false
        set(value) {
            this.optionalValetMode = CarServer_ClosuresState.OneOf_OptionalValetMode.valetMode(value)
        }

    var optionalValetPinNeeded: CarServer_ClosuresState.OneOf_OptionalValetPinNeeded?
        get() = this._optionalValetPinNeeded
        set(value) {
            this._optionalValetPinNeeded = value
            _protobufMutated()
        }

    var valetPinNeeded: Boolean
        get() = (this._optionalValetPinNeeded as? CarServer_ClosuresState.OneOf_OptionalValetPinNeeded.valetPinNeeded)?.value ?: false
        set(value) {
            this.optionalValetPinNeeded = CarServer_ClosuresState.OneOf_OptionalValetPinNeeded.valetPinNeeded(value)
        }

    var sentryModeState: CarServer_ClosuresState.SentryModeState
        get() = this._sentryModeState ?: _protobufPending(19, { CarServer_ClosuresState.SentryModeState() }) { this._sentryModeState = it }
        set(value) {
            _protobufDropPending(19)
            this._sentryModeState = value.copy()
            _protobufMutated()
        }

    val hasSentryModeState: Boolean
        get() = this._sentryModeState != null

    fun clearSentryModeState() {
        _protobufDropPending(19)
        this._sentryModeState = null
        _protobufMutated()
    }

    var optionalSentryModeAvailable: CarServer_ClosuresState.OneOf_OptionalSentryModeAvailable?
        get() = this._optionalSentryModeAvailable
        set(value) {
            this._optionalSentryModeAvailable = value
            _protobufMutated()
        }

    var sentryModeAvailable: Boolean
        get() = (this._optionalSentryModeAvailable as? CarServer_ClosuresState.OneOf_OptionalSentryModeAvailable.sentryModeAvailable)?.value ?: false
        set(value) {
            this.optionalSentryModeAvailable = CarServer_ClosuresState.OneOf_OptionalSentryModeAvailable.sentryModeAvailable(value)
        }

    var speedLimitMode: CarServer_SpeedLimitMode
        get() = this._speedLimitMode ?: _protobufPending(22, { CarServer_SpeedLimitMode() }) { this._speedLimitMode = it }
        set(value) {
            _protobufDropPending(22)
            this._speedLimitMode = value.copy()
            _protobufMutated()
        }

    val hasSpeedLimitMode: Boolean
        get() = this._speedLimitMode != null

    fun clearSpeedLimitMode() {
        _protobufDropPending(22)
        this._speedLimitMode = null
        _protobufMutated()
    }

    var optionalTonneauState: CarServer_ClosuresState.OneOf_OptionalTonneauState?
        get() = this._optionalTonneauState
        set(value) {
            this._protobufStore_optionalTonneauState(value)
            _protobufMutated()
        }

    var tonneauState: VCSEC_ClosureState_E
        get() = (this._optionalTonneauState as? CarServer_ClosuresState.OneOf_OptionalTonneauState.tonneauState)?.value ?: VCSEC_ClosureState_E.closurestateClosed
        set(value) {
            this.optionalTonneauState = CarServer_ClosuresState.OneOf_OptionalTonneauState.tonneauState(value)
        }

    var optionalTonneauPercentOpen: CarServer_ClosuresState.OneOf_OptionalTonneauPercentOpen?
        get() = this._optionalTonneauPercentOpen
        set(value) {
            this._optionalTonneauPercentOpen = value
            _protobufMutated()
        }

    var tonneauPercentOpen: UInt
        get() = (this._optionalTonneauPercentOpen as? CarServer_ClosuresState.OneOf_OptionalTonneauPercentOpen.tonneauPercentOpen)?.value ?: 0u
        set(value) {
            this.optionalTonneauPercentOpen = CarServer_ClosuresState.OneOf_OptionalTonneauPercentOpen.tonneauPercentOpen(value)
        }

    var optionalTonneauInMotion: CarServer_ClosuresState.OneOf_OptionalTonneauInMotion?
        get() = this._optionalTonneauInMotion
        set(value) {
            this._optionalTonneauInMotion = value
            _protobufMutated()
        }

    var tonneauInMotion: Boolean
        get() = (this._optionalTonneauInMotion as? CarServer_ClosuresState.OneOf_OptionalTonneauInMotion.tonneauInMotion)?.value ?: false
        set(value) {
            this.optionalTonneauInMotion = CarServer_ClosuresState.OneOf_OptionalTonneauInMotion.tonneauInMotion(value)
        }

    var timestamp: Google_Protobuf_Timestamp
        get() = this._timestamp ?: _protobufPending(2000, { Google_Protobuf_Timestamp() }) { this._timestamp = it }
        set(value) {
            _protobufDropPending(2000)
            this._timestamp = value.copy()
            _protobufMutated()
        }

    val hasTimestamp: Boolean
        get() = this._timestamp != null

    fun clearTimestamp() {
        _protobufDropPending(2000)
        this._timestamp = null
        _protobufMutated()
    }

    sealed class OneOf_OptionalDoorOpenDriverFront(value: Any) : ProtobufOneofCase(value) {
        class doorOpenDriverFront(val value: Boolean) : OneOf_OptionalDoorOpenDriverFront(value)
    }

    sealed class OneOf_OptionalDoorOpenDriverRear(value: Any) : ProtobufOneofCase(value) {
        class doorOpenDriverRear(val value: Boolean) : OneOf_OptionalDoorOpenDriverRear(value)
    }

    sealed class OneOf_OptionalDoorOpenPassengerFront(value: Any) : ProtobufOneofCase(value) {
        class doorOpenPassengerFront(val value: Boolean) : OneOf_OptionalDoorOpenPassengerFront(value)
    }

    sealed class OneOf_OptionalDoorOpenPassengerRear(value: Any) : ProtobufOneofCase(value) {
        class doorOpenPassengerRear(val value: Boolean) : OneOf_OptionalDoorOpenPassengerRear(value)
    }

    sealed class OneOf_OptionalDoorOpenTrunkFront(value: Any) : ProtobufOneofCase(value) {
        class doorOpenTrunkFront(val value: Boolean) : OneOf_OptionalDoorOpenTrunkFront(value)
    }

    sealed class OneOf_OptionalDoorOpenTrunkRear(value: Any) : ProtobufOneofCase(value) {
        class doorOpenTrunkRear(val value: Boolean) : OneOf_OptionalDoorOpenTrunkRear(value)
    }

    sealed class OneOf_OptionalWindowOpenDriverFront(value: Any) : ProtobufOneofCase(value) {
        class windowOpenDriverFront(val value: Boolean) : OneOf_OptionalWindowOpenDriverFront(value)
    }

    sealed class OneOf_OptionalWindowOpenPassengerFront(value: Any) : ProtobufOneofCase(value) {
        class windowOpenPassengerFront(val value: Boolean) : OneOf_OptionalWindowOpenPassengerFront(value)
    }

    sealed class OneOf_OptionalWindowOpenDriverRear(value: Any) : ProtobufOneofCase(value) {
        class windowOpenDriverRear(val value: Boolean) : OneOf_OptionalWindowOpenDriverRear(value)
    }

    sealed class OneOf_OptionalWindowOpenPassengerRear(value: Any) : ProtobufOneofCase(value) {
        class windowOpenPassengerRear(val value: Boolean) : OneOf_OptionalWindowOpenPassengerRear(value)
    }

    sealed class OneOf_OptionalSunRoofPercentOpen(value: Any) : ProtobufOneofCase(value) {
        class sunRoofPercentOpen(val value: Int) : OneOf_OptionalSunRoofPercentOpen(value)
    }

    sealed class OneOf_OptionalLocked(value: Any) : ProtobufOneofCase(value) {
        class locked(val value: Boolean) : OneOf_OptionalLocked(value)
    }

    sealed class OneOf_OptionalIsUserPresent(value: Any) : ProtobufOneofCase(value) {
        class isUserPresent(val value: Boolean) : OneOf_OptionalIsUserPresent(value)
    }

    sealed class OneOf_OptionalRemoteStart(value: Any) : ProtobufOneofCase(value) {
        class remoteStart(val value: Boolean) : OneOf_OptionalRemoteStart(value)
    }

    sealed class OneOf_OptionalValetMode(value: Any) : ProtobufOneofCase(value) {
        class valetMode(val value: Boolean) : OneOf_OptionalValetMode(value)
    }

    sealed class OneOf_OptionalValetPinNeeded(value: Any) : ProtobufOneofCase(value) {
        class valetPinNeeded(val value: Boolean) : OneOf_OptionalValetPinNeeded(value)
    }

    sealed class OneOf_OptionalSentryModeAvailable(value: Any) : ProtobufOneofCase(value) {
        class sentryModeAvailable(val value: Boolean) : OneOf_OptionalSentryModeAvailable(value)
    }

    sealed class OneOf_OptionalTonneauState(value: Any) : ProtobufOneofCase(value) {
        class tonneauState(val value: VCSEC_ClosureState_E) : OneOf_OptionalTonneauState(value)
    }

    sealed class OneOf_OptionalTonneauPercentOpen(value: Any) : ProtobufOneofCase(value) {
        class tonneauPercentOpen(val value: UInt) : OneOf_OptionalTonneauPercentOpen(value)
    }

    sealed class OneOf_OptionalTonneauInMotion(value: Any) : ProtobufOneofCase(value) {
        class tonneauInMotion(val value: Boolean) : OneOf_OptionalTonneauInMotion(value)
    }

    class SunRoofState() : GeneratedMessage() {
        constructor(serializedBytes: ByteArray) : this() {
            merge(serializedBytes)
        }

        var type: CarServer_ClosuresState.SunRoofState.OneOf_Type?
            get() = this._type
            set(value) {
                this._protobufStore_type(this._protobufCopy_type(value))
                _protobufMutated()
            }

        var unknown: CarServer_Void
            get() = (this._type as? CarServer_ClosuresState.SunRoofState.OneOf_Type.unknown)?.value
                ?: _protobufPending(1, { CarServer_Void() }) { this._protobufStore_type(CarServer_ClosuresState.SunRoofState.OneOf_Type.unknown(it)) }
            set(value) {
                this.type = CarServer_ClosuresState.SunRoofState.OneOf_Type.unknown(value)
            }

        var calibrating: CarServer_Void
            get() = (this._type as? CarServer_ClosuresState.SunRoofState.OneOf_Type.calibrating)?.value
                ?: _protobufPending(2, { CarServer_Void() }) { this._protobufStore_type(CarServer_ClosuresState.SunRoofState.OneOf_Type.calibrating(it)) }
            set(value) {
                this.type = CarServer_ClosuresState.SunRoofState.OneOf_Type.calibrating(value)
            }

        var closed: CarServer_Void
            get() = (this._type as? CarServer_ClosuresState.SunRoofState.OneOf_Type.closed)?.value
                ?: _protobufPending(3, { CarServer_Void() }) { this._protobufStore_type(CarServer_ClosuresState.SunRoofState.OneOf_Type.closed(it)) }
            set(value) {
                this.type = CarServer_ClosuresState.SunRoofState.OneOf_Type.closed(value)
            }

        var `open`: CarServer_Void
            get() = (this._type as? CarServer_ClosuresState.SunRoofState.OneOf_Type.`open`)?.value
                ?: _protobufPending(4, { CarServer_Void() }) { this._protobufStore_type(CarServer_ClosuresState.SunRoofState.OneOf_Type.`open`(it)) }
            set(value) {
                this.type = CarServer_ClosuresState.SunRoofState.OneOf_Type.`open`(value)
            }

        var moving: CarServer_Void
            get() = (this._type as? CarServer_ClosuresState.SunRoofState.OneOf_Type.moving)?.value
                ?: _protobufPending(5, { CarServer_Void() }) { this._protobufStore_type(CarServer_ClosuresState.SunRoofState.OneOf_Type.moving(it)) }
            set(value) {
                this.type = CarServer_ClosuresState.SunRoofState.OneOf_Type.moving(value)
            }

        var vent: CarServer_Void
            get() = (this._type as? CarServer_ClosuresState.SunRoofState.OneOf_Type.vent)?.value
                ?: _protobufPending(6, { CarServer_Void() }) { this._protobufStore_type(CarServer_ClosuresState.SunRoofState.OneOf_Type.vent(it)) }
            set(value) {
                this.type = CarServer_ClosuresState.SunRoofState.OneOf_Type.vent(value)
            }

        sealed class OneOf_Type(value: Any) : ProtobufOneofCase(value) {
            class unknown(val value: CarServer_Void) : OneOf_Type(value)
            class calibrating(val value: CarServer_Void) : OneOf_Type(value)
            class closed(val value: CarServer_Void) : OneOf_Type(value)
            class `open`(val value: CarServer_Void) : OneOf_Type(value)
            class moving(val value: CarServer_Void) : OneOf_Type(value)
            class vent(val value: CarServer_Void) : OneOf_Type(value)
        }

        private var _type: CarServer_ClosuresState.SunRoofState.OneOf_Type? = null

        private fun _protobufStore_type(value: CarServer_ClosuresState.SunRoofState.OneOf_Type?) {
            _protobufDropPending(1)
            _protobufDropPending(2)
            _protobufDropPending(3)
            _protobufDropPending(4)
            _protobufDropPending(5)
            _protobufDropPending(6)
            this._type = value
        }

        private fun _protobufCopy_type(value: CarServer_ClosuresState.SunRoofState.OneOf_Type?): CarServer_ClosuresState.SunRoofState.OneOf_Type? =
            when (value) {
                is CarServer_ClosuresState.SunRoofState.OneOf_Type.unknown -> CarServer_ClosuresState.SunRoofState.OneOf_Type.unknown(value.value.copy())
                is CarServer_ClosuresState.SunRoofState.OneOf_Type.calibrating -> CarServer_ClosuresState.SunRoofState.OneOf_Type.calibrating(value.value.copy())
                is CarServer_ClosuresState.SunRoofState.OneOf_Type.closed -> CarServer_ClosuresState.SunRoofState.OneOf_Type.closed(value.value.copy())
                is CarServer_ClosuresState.SunRoofState.OneOf_Type.`open` -> CarServer_ClosuresState.SunRoofState.OneOf_Type.`open`(value.value.copy())
                is CarServer_ClosuresState.SunRoofState.OneOf_Type.moving -> CarServer_ClosuresState.SunRoofState.OneOf_Type.moving(value.value.copy())
                is CarServer_ClosuresState.SunRoofState.OneOf_Type.vent -> CarServer_ClosuresState.SunRoofState.OneOf_Type.vent(value.value.copy())
                else -> value
            }

        override fun decodeMessage(decoder: BinaryDecoder) {
            while (true) {
                when (decoder.nextFieldNumber() ?: return) {
                    1 -> decoder.decodeSingularMessageField((this._type as? CarServer_ClosuresState.SunRoofState.OneOf_Type.unknown)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ClosuresState.SunRoofState.OneOf_Type.unknown(it)) }
                    2 -> decoder.decodeSingularMessageField((this._type as? CarServer_ClosuresState.SunRoofState.OneOf_Type.calibrating)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ClosuresState.SunRoofState.OneOf_Type.calibrating(it)) }
                    3 -> decoder.decodeSingularMessageField((this._type as? CarServer_ClosuresState.SunRoofState.OneOf_Type.closed)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ClosuresState.SunRoofState.OneOf_Type.closed(it)) }
                    4 -> decoder.decodeSingularMessageField((this._type as? CarServer_ClosuresState.SunRoofState.OneOf_Type.`open`)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ClosuresState.SunRoofState.OneOf_Type.`open`(it)) }
                    5 -> decoder.decodeSingularMessageField((this._type as? CarServer_ClosuresState.SunRoofState.OneOf_Type.moving)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ClosuresState.SunRoofState.OneOf_Type.moving(it)) }
                    6 -> decoder.decodeSingularMessageField((this._type as? CarServer_ClosuresState.SunRoofState.OneOf_Type.vent)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ClosuresState.SunRoofState.OneOf_Type.vent(it)) }
                }
            }
        }

        override fun traverse(visitor: BinaryEncodingVisitor) {
            (this._type as? CarServer_ClosuresState.SunRoofState.OneOf_Type.unknown)?.let {
                visitor.visitSingularMessageField(it.value, 1)
            }
            (this._type as? CarServer_ClosuresState.SunRoofState.OneOf_Type.calibrating)?.let {
                visitor.visitSingularMessageField(it.value, 2)
            }
            (this._type as? CarServer_ClosuresState.SunRoofState.OneOf_Type.closed)?.let {
                visitor.visitSingularMessageField(it.value, 3)
            }
            (this._type as? CarServer_ClosuresState.SunRoofState.OneOf_Type.`open`)?.let {
                visitor.visitSingularMessageField(it.value, 4)
            }
            (this._type as? CarServer_ClosuresState.SunRoofState.OneOf_Type.moving)?.let {
                visitor.visitSingularMessageField(it.value, 5)
            }
            (this._type as? CarServer_ClosuresState.SunRoofState.OneOf_Type.vent)?.let {
                visitor.visitSingularMessageField(it.value, 6)
            }
            visitor.visitUnknown(unknownFields)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CarServer_ClosuresState.SunRoofState) return false
            if (this._type != other._type) return false
            return this.unknownFields.contentEquals(other.unknownFields)
        }

        override fun hashCode(): Int {
            var hash = 0
            hash = 31 * hash + (this._type?.hashCode() ?: 0)
            hash = 31 * hash + this.unknownFields.contentHashCode()
            return hash
        }

        override fun copy(): CarServer_ClosuresState.SunRoofState {
            val result = CarServer_ClosuresState.SunRoofState()
            result._type = this._protobufCopy_type(this._type)
            result.unknownFields = this.unknownFields
            return result
        }

        companion object {
            const val protoMessageName: String = "CarServer.ClosuresState.SunRoofState"

            fun with(block: CarServer_ClosuresState.SunRoofState.() -> Unit): CarServer_ClosuresState.SunRoofState =
                CarServer_ClosuresState.SunRoofState().apply(block)
        }
    }

    class DisplayState() : GeneratedMessage() {
        constructor(serializedBytes: ByteArray) : this() {
            merge(serializedBytes)
        }

        var type: CarServer_ClosuresState.DisplayState.OneOf_Type?
            get() = this._type
            set(value) {
                this._protobufStore_type(this._protobufCopy_type(value))
                _protobufMutated()
            }

        var off: CarServer_Void
            get() = (this._type as? CarServer_ClosuresState.DisplayState.OneOf_Type.off)?.value
                ?: _protobufPending(1, { CarServer_Void() }) { this._protobufStore_type(CarServer_ClosuresState.DisplayState.OneOf_Type.off(it)) }
            set(value) {
                this.type = CarServer_ClosuresState.DisplayState.OneOf_Type.off(value)
            }

        var dim: CarServer_Void
            get() = (this._type as? CarServer_ClosuresState.DisplayState.OneOf_Type.dim)?.value
                ?: _protobufPending(2, { CarServer_Void() }) { this._protobufStore_type(CarServer_ClosuresState.DisplayState.OneOf_Type.dim(it)) }
            set(value) {
                this.type = CarServer_ClosuresState.DisplayState.OneOf_Type.dim(value)
            }

        var accessory: CarServer_Void
            get() = (this._type as? CarServer_ClosuresState.DisplayState.OneOf_Type.accessory)?.value
                ?: _protobufPending(3, { CarServer_Void() }) { this._protobufStore_type(CarServer_ClosuresState.DisplayState.OneOf_Type.accessory(it)) }
            set(value) {
                this.type = CarServer_ClosuresState.DisplayState.OneOf_Type.accessory(value)
            }

        var on: CarServer_Void
            get() = (this._type as? CarServer_ClosuresState.DisplayState.OneOf_Type.on)?.value
                ?: _protobufPending(4, { CarServer_Void() }) { this._protobufStore_type(CarServer_ClosuresState.DisplayState.OneOf_Type.on(it)) }
            set(value) {
                this.type = CarServer_ClosuresState.DisplayState.OneOf_Type.on(value)
            }

        var driving: CarServer_Void
            get() = (this._type as? CarServer_ClosuresState.DisplayState.OneOf_Type.driving)?.value
                ?: _protobufPending(5, { CarServer_Void() }) { this._protobufStore_type(CarServer_ClosuresState.DisplayState.OneOf_Type.driving(it)) }
            set(value) {
                this.type = CarServer_ClosuresState.DisplayState.OneOf_Type.driving(value)
            }

        var charging: CarServer_Void
            get() = (this._type as? CarServer_ClosuresState.DisplayState.OneOf_Type.charging)?.value
                ?: _protobufPending(6, { CarServer_Void() }) { this._protobufStore_type(CarServer_ClosuresState.DisplayState.OneOf_Type.charging(it)) }
            set(value) {
                this.type = CarServer_ClosuresState.DisplayState.OneOf_Type.charging(value)
            }

        var lock: CarServer_Void
            get() = (this._type as? CarServer_ClosuresState.DisplayState.OneOf_Type.lock)?.value
                ?: _protobufPending(7, { CarServer_Void() }) { this._protobufStore_type(CarServer_ClosuresState.DisplayState.OneOf_Type.lock(it)) }
            set(value) {
                this.type = CarServer_ClosuresState.DisplayState.OneOf_Type.lock(value)
            }

        var sentry: CarServer_Void
            get() = (this._type as? CarServer_ClosuresState.DisplayState.OneOf_Type.sentry)?.value
                ?: _protobufPending(8, { CarServer_Void() }) { this._protobufStore_type(CarServer_ClosuresState.DisplayState.OneOf_Type.sentry(it)) }
            set(value) {
                this.type = CarServer_ClosuresState.DisplayState.OneOf_Type.sentry(value)
            }

        var dog: CarServer_Void
            get() = (this._type as? CarServer_ClosuresState.DisplayState.OneOf_Type.dog)?.value
                ?: _protobufPending(9, { CarServer_Void() }) { this._protobufStore_type(CarServer_ClosuresState.DisplayState.OneOf_Type.dog(it)) }
            set(value) {
                this.type = CarServer_ClosuresState.DisplayState.OneOf_Type.dog(value)
            }

        var entertainment: CarServer_Void
            get() = (this._type as? CarServer_ClosuresState.DisplayState.OneOf_Type.entertainment)?.value
                ?: _protobufPending(10, { CarServer_Void() }) { this._protobufStore_type(CarServer_ClosuresState.DisplayState.OneOf_Type.entertainment(it)) }
            set(value) {
                this.type = CarServer_ClosuresState.DisplayState.OneOf_Type.entertainment(value)
            }

        sealed class OneOf_Type(value: Any) : ProtobufOneofCase(value) {
            class off(val value: CarServer_Void) : OneOf_Type(value)
            class dim(val value: CarServer_Void) : OneOf_Type(value)
            class accessory(val value: CarServer_Void) : OneOf_Type(value)
            class on(val value: CarServer_Void) : OneOf_Type(value)
            class driving(val value: CarServer_Void) : OneOf_Type(value)
            class charging(val value: CarServer_Void) : OneOf_Type(value)
            class lock(val value: CarServer_Void) : OneOf_Type(value)
            class sentry(val value: CarServer_Void) : OneOf_Type(value)
            class dog(val value: CarServer_Void) : OneOf_Type(value)
            class entertainment(val value: CarServer_Void) : OneOf_Type(value)
        }

        private var _type: CarServer_ClosuresState.DisplayState.OneOf_Type? = null

        private fun _protobufStore_type(value: CarServer_ClosuresState.DisplayState.OneOf_Type?) {
            _protobufDropPending(1)
            _protobufDropPending(2)
            _protobufDropPending(3)
            _protobufDropPending(4)
            _protobufDropPending(5)
            _protobufDropPending(6)
            _protobufDropPending(7)
            _protobufDropPending(8)
            _protobufDropPending(9)
            _protobufDropPending(10)
            this._type = value
        }

        private fun _protobufCopy_type(value: CarServer_ClosuresState.DisplayState.OneOf_Type?): CarServer_ClosuresState.DisplayState.OneOf_Type? =
            when (value) {
                is CarServer_ClosuresState.DisplayState.OneOf_Type.off -> CarServer_ClosuresState.DisplayState.OneOf_Type.off(value.value.copy())
                is CarServer_ClosuresState.DisplayState.OneOf_Type.dim -> CarServer_ClosuresState.DisplayState.OneOf_Type.dim(value.value.copy())
                is CarServer_ClosuresState.DisplayState.OneOf_Type.accessory -> CarServer_ClosuresState.DisplayState.OneOf_Type.accessory(value.value.copy())
                is CarServer_ClosuresState.DisplayState.OneOf_Type.on -> CarServer_ClosuresState.DisplayState.OneOf_Type.on(value.value.copy())
                is CarServer_ClosuresState.DisplayState.OneOf_Type.driving -> CarServer_ClosuresState.DisplayState.OneOf_Type.driving(value.value.copy())
                is CarServer_ClosuresState.DisplayState.OneOf_Type.charging -> CarServer_ClosuresState.DisplayState.OneOf_Type.charging(value.value.copy())
                is CarServer_ClosuresState.DisplayState.OneOf_Type.lock -> CarServer_ClosuresState.DisplayState.OneOf_Type.lock(value.value.copy())
                is CarServer_ClosuresState.DisplayState.OneOf_Type.sentry -> CarServer_ClosuresState.DisplayState.OneOf_Type.sentry(value.value.copy())
                is CarServer_ClosuresState.DisplayState.OneOf_Type.dog -> CarServer_ClosuresState.DisplayState.OneOf_Type.dog(value.value.copy())
                is CarServer_ClosuresState.DisplayState.OneOf_Type.entertainment -> CarServer_ClosuresState.DisplayState.OneOf_Type.entertainment(value.value.copy())
                else -> value
            }

        override fun decodeMessage(decoder: BinaryDecoder) {
            while (true) {
                when (decoder.nextFieldNumber() ?: return) {
                    1 -> decoder.decodeSingularMessageField((this._type as? CarServer_ClosuresState.DisplayState.OneOf_Type.off)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ClosuresState.DisplayState.OneOf_Type.off(it)) }
                    2 -> decoder.decodeSingularMessageField((this._type as? CarServer_ClosuresState.DisplayState.OneOf_Type.dim)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ClosuresState.DisplayState.OneOf_Type.dim(it)) }
                    3 -> decoder.decodeSingularMessageField((this._type as? CarServer_ClosuresState.DisplayState.OneOf_Type.accessory)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ClosuresState.DisplayState.OneOf_Type.accessory(it)) }
                    4 -> decoder.decodeSingularMessageField((this._type as? CarServer_ClosuresState.DisplayState.OneOf_Type.on)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ClosuresState.DisplayState.OneOf_Type.on(it)) }
                    5 -> decoder.decodeSingularMessageField((this._type as? CarServer_ClosuresState.DisplayState.OneOf_Type.driving)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ClosuresState.DisplayState.OneOf_Type.driving(it)) }
                    6 -> decoder.decodeSingularMessageField((this._type as? CarServer_ClosuresState.DisplayState.OneOf_Type.charging)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ClosuresState.DisplayState.OneOf_Type.charging(it)) }
                    7 -> decoder.decodeSingularMessageField((this._type as? CarServer_ClosuresState.DisplayState.OneOf_Type.lock)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ClosuresState.DisplayState.OneOf_Type.lock(it)) }
                    8 -> decoder.decodeSingularMessageField((this._type as? CarServer_ClosuresState.DisplayState.OneOf_Type.sentry)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ClosuresState.DisplayState.OneOf_Type.sentry(it)) }
                    9 -> decoder.decodeSingularMessageField((this._type as? CarServer_ClosuresState.DisplayState.OneOf_Type.dog)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ClosuresState.DisplayState.OneOf_Type.dog(it)) }
                    10 -> decoder.decodeSingularMessageField((this._type as? CarServer_ClosuresState.DisplayState.OneOf_Type.entertainment)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ClosuresState.DisplayState.OneOf_Type.entertainment(it)) }
                }
            }
        }

        override fun traverse(visitor: BinaryEncodingVisitor) {
            (this._type as? CarServer_ClosuresState.DisplayState.OneOf_Type.off)?.let {
                visitor.visitSingularMessageField(it.value, 1)
            }
            (this._type as? CarServer_ClosuresState.DisplayState.OneOf_Type.dim)?.let {
                visitor.visitSingularMessageField(it.value, 2)
            }
            (this._type as? CarServer_ClosuresState.DisplayState.OneOf_Type.accessory)?.let {
                visitor.visitSingularMessageField(it.value, 3)
            }
            (this._type as? CarServer_ClosuresState.DisplayState.OneOf_Type.on)?.let {
                visitor.visitSingularMessageField(it.value, 4)
            }
            (this._type as? CarServer_ClosuresState.DisplayState.OneOf_Type.driving)?.let {
                visitor.visitSingularMessageField(it.value, 5)
            }
            (this._type as? CarServer_ClosuresState.DisplayState.OneOf_Type.charging)?.let {
                visitor.visitSingularMessageField(it.value, 6)
            }
            (this._type as? CarServer_ClosuresState.DisplayState.OneOf_Type.lock)?.let {
                visitor.visitSingularMessageField(it.value, 7)
            }
            (this._type as? CarServer_ClosuresState.DisplayState.OneOf_Type.sentry)?.let {
                visitor.visitSingularMessageField(it.value, 8)
            }
            (this._type as? CarServer_ClosuresState.DisplayState.OneOf_Type.dog)?.let {
                visitor.visitSingularMessageField(it.value, 9)
            }
            (this._type as? CarServer_ClosuresState.DisplayState.OneOf_Type.entertainment)?.let {
                visitor.visitSingularMessageField(it.value, 10)
            }
            visitor.visitUnknown(unknownFields)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CarServer_ClosuresState.DisplayState) return false
            if (this._type != other._type) return false
            return this.unknownFields.contentEquals(other.unknownFields)
        }

        override fun hashCode(): Int {
            var hash = 0
            hash = 31 * hash + (this._type?.hashCode() ?: 0)
            hash = 31 * hash + this.unknownFields.contentHashCode()
            return hash
        }

        override fun copy(): CarServer_ClosuresState.DisplayState {
            val result = CarServer_ClosuresState.DisplayState()
            result._type = this._protobufCopy_type(this._type)
            result.unknownFields = this.unknownFields
            return result
        }

        companion object {
            const val protoMessageName: String = "CarServer.ClosuresState.DisplayState"

            fun with(block: CarServer_ClosuresState.DisplayState.() -> Unit): CarServer_ClosuresState.DisplayState =
                CarServer_ClosuresState.DisplayState().apply(block)
        }
    }

    class SentryModeState() : GeneratedMessage() {
        constructor(serializedBytes: ByteArray) : this() {
            merge(serializedBytes)
        }

        var type: CarServer_ClosuresState.SentryModeState.OneOf_Type?
            get() = this._type
            set(value) {
                this._protobufStore_type(this._protobufCopy_type(value))
                _protobufMutated()
            }

        var off: CarServer_Void
            get() = (this._type as? CarServer_ClosuresState.SentryModeState.OneOf_Type.off)?.value
                ?: _protobufPending(1, { CarServer_Void() }) { this._protobufStore_type(CarServer_ClosuresState.SentryModeState.OneOf_Type.off(it)) }
            set(value) {
                this.type = CarServer_ClosuresState.SentryModeState.OneOf_Type.off(value)
            }

        var idle: CarServer_Void
            get() = (this._type as? CarServer_ClosuresState.SentryModeState.OneOf_Type.idle)?.value
                ?: _protobufPending(2, { CarServer_Void() }) { this._protobufStore_type(CarServer_ClosuresState.SentryModeState.OneOf_Type.idle(it)) }
            set(value) {
                this.type = CarServer_ClosuresState.SentryModeState.OneOf_Type.idle(value)
            }

        var armed: CarServer_Void
            get() = (this._type as? CarServer_ClosuresState.SentryModeState.OneOf_Type.armed)?.value
                ?: _protobufPending(3, { CarServer_Void() }) { this._protobufStore_type(CarServer_ClosuresState.SentryModeState.OneOf_Type.armed(it)) }
            set(value) {
                this.type = CarServer_ClosuresState.SentryModeState.OneOf_Type.armed(value)
            }

        var aware: CarServer_Void
            get() = (this._type as? CarServer_ClosuresState.SentryModeState.OneOf_Type.aware)?.value
                ?: _protobufPending(4, { CarServer_Void() }) { this._protobufStore_type(CarServer_ClosuresState.SentryModeState.OneOf_Type.aware(it)) }
            set(value) {
                this.type = CarServer_ClosuresState.SentryModeState.OneOf_Type.aware(value)
            }

        var panic: CarServer_Void
            get() = (this._type as? CarServer_ClosuresState.SentryModeState.OneOf_Type.panic)?.value
                ?: _protobufPending(5, { CarServer_Void() }) { this._protobufStore_type(CarServer_ClosuresState.SentryModeState.OneOf_Type.panic(it)) }
            set(value) {
                this.type = CarServer_ClosuresState.SentryModeState.OneOf_Type.panic(value)
            }

        var quiet: CarServer_Void
            get() = (this._type as? CarServer_ClosuresState.SentryModeState.OneOf_Type.quiet)?.value
                ?: _protobufPending(6, { CarServer_Void() }) { this._protobufStore_type(CarServer_ClosuresState.SentryModeState.OneOf_Type.quiet(it)) }
            set(value) {
                this.type = CarServer_ClosuresState.SentryModeState.OneOf_Type.quiet(value)
            }

        sealed class OneOf_Type(value: Any) : ProtobufOneofCase(value) {
            class off(val value: CarServer_Void) : OneOf_Type(value)
            class idle(val value: CarServer_Void) : OneOf_Type(value)
            class armed(val value: CarServer_Void) : OneOf_Type(value)
            class aware(val value: CarServer_Void) : OneOf_Type(value)
            class panic(val value: CarServer_Void) : OneOf_Type(value)
            class quiet(val value: CarServer_Void) : OneOf_Type(value)
        }

        private var _type: CarServer_ClosuresState.SentryModeState.OneOf_Type? = null

        private fun _protobufStore_type(value: CarServer_ClosuresState.SentryModeState.OneOf_Type?) {
            _protobufDropPending(1)
            _protobufDropPending(2)
            _protobufDropPending(3)
            _protobufDropPending(4)
            _protobufDropPending(5)
            _protobufDropPending(6)
            this._type = value
        }

        private fun _protobufCopy_type(value: CarServer_ClosuresState.SentryModeState.OneOf_Type?): CarServer_ClosuresState.SentryModeState.OneOf_Type? =
            when (value) {
                is CarServer_ClosuresState.SentryModeState.OneOf_Type.off -> CarServer_ClosuresState.SentryModeState.OneOf_Type.off(value.value.copy())
                is CarServer_ClosuresState.SentryModeState.OneOf_Type.idle -> CarServer_ClosuresState.SentryModeState.OneOf_Type.idle(value.value.copy())
                is CarServer_ClosuresState.SentryModeState.OneOf_Type.armed -> CarServer_ClosuresState.SentryModeState.OneOf_Type.armed(value.value.copy())
                is CarServer_ClosuresState.SentryModeState.OneOf_Type.aware -> CarServer_ClosuresState.SentryModeState.OneOf_Type.aware(value.value.copy())
                is CarServer_ClosuresState.SentryModeState.OneOf_Type.panic -> CarServer_ClosuresState.SentryModeState.OneOf_Type.panic(value.value.copy())
                is CarServer_ClosuresState.SentryModeState.OneOf_Type.quiet -> CarServer_ClosuresState.SentryModeState.OneOf_Type.quiet(value.value.copy())
                else -> value
            }

        override fun decodeMessage(decoder: BinaryDecoder) {
            while (true) {
                when (decoder.nextFieldNumber() ?: return) {
                    1 -> decoder.decodeSingularMessageField((this._type as? CarServer_ClosuresState.SentryModeState.OneOf_Type.off)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ClosuresState.SentryModeState.OneOf_Type.off(it)) }
                    2 -> decoder.decodeSingularMessageField((this._type as? CarServer_ClosuresState.SentryModeState.OneOf_Type.idle)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ClosuresState.SentryModeState.OneOf_Type.idle(it)) }
                    3 -> decoder.decodeSingularMessageField((this._type as? CarServer_ClosuresState.SentryModeState.OneOf_Type.armed)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ClosuresState.SentryModeState.OneOf_Type.armed(it)) }
                    4 -> decoder.decodeSingularMessageField((this._type as? CarServer_ClosuresState.SentryModeState.OneOf_Type.aware)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ClosuresState.SentryModeState.OneOf_Type.aware(it)) }
                    5 -> decoder.decodeSingularMessageField((this._type as? CarServer_ClosuresState.SentryModeState.OneOf_Type.panic)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ClosuresState.SentryModeState.OneOf_Type.panic(it)) }
                    6 -> decoder.decodeSingularMessageField((this._type as? CarServer_ClosuresState.SentryModeState.OneOf_Type.quiet)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ClosuresState.SentryModeState.OneOf_Type.quiet(it)) }
                }
            }
        }

        override fun traverse(visitor: BinaryEncodingVisitor) {
            (this._type as? CarServer_ClosuresState.SentryModeState.OneOf_Type.off)?.let {
                visitor.visitSingularMessageField(it.value, 1)
            }
            (this._type as? CarServer_ClosuresState.SentryModeState.OneOf_Type.idle)?.let {
                visitor.visitSingularMessageField(it.value, 2)
            }
            (this._type as? CarServer_ClosuresState.SentryModeState.OneOf_Type.armed)?.let {
                visitor.visitSingularMessageField(it.value, 3)
            }
            (this._type as? CarServer_ClosuresState.SentryModeState.OneOf_Type.aware)?.let {
                visitor.visitSingularMessageField(it.value, 4)
            }
            (this._type as? CarServer_ClosuresState.SentryModeState.OneOf_Type.panic)?.let {
                visitor.visitSingularMessageField(it.value, 5)
            }
            (this._type as? CarServer_ClosuresState.SentryModeState.OneOf_Type.quiet)?.let {
                visitor.visitSingularMessageField(it.value, 6)
            }
            visitor.visitUnknown(unknownFields)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CarServer_ClosuresState.SentryModeState) return false
            if (this._type != other._type) return false
            return this.unknownFields.contentEquals(other.unknownFields)
        }

        override fun hashCode(): Int {
            var hash = 0
            hash = 31 * hash + (this._type?.hashCode() ?: 0)
            hash = 31 * hash + this.unknownFields.contentHashCode()
            return hash
        }

        override fun copy(): CarServer_ClosuresState.SentryModeState {
            val result = CarServer_ClosuresState.SentryModeState()
            result._type = this._protobufCopy_type(this._type)
            result.unknownFields = this.unknownFields
            return result
        }

        companion object {
            const val protoMessageName: String = "CarServer.ClosuresState.SentryModeState"

            fun with(block: CarServer_ClosuresState.SentryModeState.() -> Unit): CarServer_ClosuresState.SentryModeState =
                CarServer_ClosuresState.SentryModeState().apply(block)
        }
    }

    private var _optionalDoorOpenDriverFront: CarServer_ClosuresState.OneOf_OptionalDoorOpenDriverFront? = null
    private var _optionalDoorOpenDriverRear: CarServer_ClosuresState.OneOf_OptionalDoorOpenDriverRear? = null
    private var _optionalDoorOpenPassengerFront: CarServer_ClosuresState.OneOf_OptionalDoorOpenPassengerFront? = null
    private var _optionalDoorOpenPassengerRear: CarServer_ClosuresState.OneOf_OptionalDoorOpenPassengerRear? = null
    private var _optionalDoorOpenTrunkFront: CarServer_ClosuresState.OneOf_OptionalDoorOpenTrunkFront? = null
    private var _optionalDoorOpenTrunkRear: CarServer_ClosuresState.OneOf_OptionalDoorOpenTrunkRear? = null
    private var _optionalWindowOpenDriverFront: CarServer_ClosuresState.OneOf_OptionalWindowOpenDriverFront? = null
    private var _optionalWindowOpenPassengerFront: CarServer_ClosuresState.OneOf_OptionalWindowOpenPassengerFront? = null
    private var _optionalWindowOpenDriverRear: CarServer_ClosuresState.OneOf_OptionalWindowOpenDriverRear? = null
    private var _optionalWindowOpenPassengerRear: CarServer_ClosuresState.OneOf_OptionalWindowOpenPassengerRear? = null
    private var _sunRoofState: CarServer_ClosuresState.SunRoofState? = null
    private var _optionalSunRoofPercentOpen: CarServer_ClosuresState.OneOf_OptionalSunRoofPercentOpen? = null
    private var _optionalLocked: CarServer_ClosuresState.OneOf_OptionalLocked? = null
    private var _optionalIsUserPresent: CarServer_ClosuresState.OneOf_OptionalIsUserPresent? = null
    private var _centerDisplayState: CarServer_ClosuresState.DisplayState? = null
    private var _optionalRemoteStart: CarServer_ClosuresState.OneOf_OptionalRemoteStart? = null
    private var _optionalValetMode: CarServer_ClosuresState.OneOf_OptionalValetMode? = null
    private var _optionalValetPinNeeded: CarServer_ClosuresState.OneOf_OptionalValetPinNeeded? = null
    private var _sentryModeState: CarServer_ClosuresState.SentryModeState? = null
    private var _optionalSentryModeAvailable: CarServer_ClosuresState.OneOf_OptionalSentryModeAvailable? = null
    private var _speedLimitMode: CarServer_SpeedLimitMode? = null
    private var _optionalTonneauState: CarServer_ClosuresState.OneOf_OptionalTonneauState? = null
    private var _optionalTonneauPercentOpen: CarServer_ClosuresState.OneOf_OptionalTonneauPercentOpen? = null
    private var _optionalTonneauInMotion: CarServer_ClosuresState.OneOf_OptionalTonneauInMotion? = null
    private var _timestamp: Google_Protobuf_Timestamp? = null

    private fun _protobufStore_optionalTonneauState(value: CarServer_ClosuresState.OneOf_OptionalTonneauState?) {
        _protobufForgetUnrecognized(23)
        this._optionalTonneauState = value
    }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                11 -> decoder.decodeSingularMessageField(this._sunRoofState) { CarServer_ClosuresState.SunRoofState() }?.let {
                    _protobufDropPending(11)
                    this._sunRoofState = it
                }
                15 -> decoder.decodeSingularMessageField(this._centerDisplayState) { CarServer_ClosuresState.DisplayState() }?.let {
                    _protobufDropPending(15)
                    this._centerDisplayState = it
                }
                19 -> decoder.decodeSingularMessageField(this._sentryModeState) { CarServer_ClosuresState.SentryModeState() }?.let {
                    _protobufDropPending(19)
                    this._sentryModeState = it
                }
                22 -> decoder.decodeSingularMessageField(this._speedLimitMode) { CarServer_SpeedLimitMode() }?.let {
                    _protobufDropPending(22)
                    this._speedLimitMode = it
                }
                23 -> decoder.decodeSingularOpenEnumField(VCSEC_ClosureState_E.UNRECOGNIZED) { VCSEC_ClosureState_E.fromRawValue(it) }
                    ?.let { this._protobufStore_optionalTonneauState(CarServer_ClosuresState.OneOf_OptionalTonneauState.tonneauState(it)) }
                24 -> decoder.decodeSingularUInt32Field()?.let { this._optionalTonneauPercentOpen = CarServer_ClosuresState.OneOf_OptionalTonneauPercentOpen.tonneauPercentOpen(it) }
                25 -> decoder.decodeSingularBoolField()?.let { this._optionalTonneauInMotion = CarServer_ClosuresState.OneOf_OptionalTonneauInMotion.tonneauInMotion(it) }
                101 -> decoder.decodeSingularBoolField()?.let { this._optionalDoorOpenDriverFront = CarServer_ClosuresState.OneOf_OptionalDoorOpenDriverFront.doorOpenDriverFront(it) }
                102 -> decoder.decodeSingularBoolField()?.let { this._optionalDoorOpenDriverRear = CarServer_ClosuresState.OneOf_OptionalDoorOpenDriverRear.doorOpenDriverRear(it) }
                103 -> decoder.decodeSingularBoolField()?.let { this._optionalDoorOpenPassengerFront = CarServer_ClosuresState.OneOf_OptionalDoorOpenPassengerFront.doorOpenPassengerFront(it) }
                104 -> decoder.decodeSingularBoolField()?.let { this._optionalDoorOpenPassengerRear = CarServer_ClosuresState.OneOf_OptionalDoorOpenPassengerRear.doorOpenPassengerRear(it) }
                105 -> decoder.decodeSingularBoolField()?.let { this._optionalDoorOpenTrunkFront = CarServer_ClosuresState.OneOf_OptionalDoorOpenTrunkFront.doorOpenTrunkFront(it) }
                106 -> decoder.decodeSingularBoolField()?.let { this._optionalDoorOpenTrunkRear = CarServer_ClosuresState.OneOf_OptionalDoorOpenTrunkRear.doorOpenTrunkRear(it) }
                107 -> decoder.decodeSingularBoolField()?.let { this._optionalWindowOpenDriverFront = CarServer_ClosuresState.OneOf_OptionalWindowOpenDriverFront.windowOpenDriverFront(it) }
                108 -> decoder.decodeSingularBoolField()?.let { this._optionalWindowOpenPassengerFront = CarServer_ClosuresState.OneOf_OptionalWindowOpenPassengerFront.windowOpenPassengerFront(it) }
                109 -> decoder.decodeSingularBoolField()?.let { this._optionalWindowOpenDriverRear = CarServer_ClosuresState.OneOf_OptionalWindowOpenDriverRear.windowOpenDriverRear(it) }
                110 -> decoder.decodeSingularBoolField()?.let { this._optionalWindowOpenPassengerRear = CarServer_ClosuresState.OneOf_OptionalWindowOpenPassengerRear.windowOpenPassengerRear(it) }
                112 -> decoder.decodeSingularInt32Field()?.let { this._optionalSunRoofPercentOpen = CarServer_ClosuresState.OneOf_OptionalSunRoofPercentOpen.sunRoofPercentOpen(it) }
                113 -> decoder.decodeSingularBoolField()?.let { this._optionalLocked = CarServer_ClosuresState.OneOf_OptionalLocked.locked(it) }
                114 -> decoder.decodeSingularBoolField()?.let { this._optionalIsUserPresent = CarServer_ClosuresState.OneOf_OptionalIsUserPresent.isUserPresent(it) }
                116 -> decoder.decodeSingularBoolField()?.let { this._optionalRemoteStart = CarServer_ClosuresState.OneOf_OptionalRemoteStart.remoteStart(it) }
                117 -> decoder.decodeSingularBoolField()?.let { this._optionalValetMode = CarServer_ClosuresState.OneOf_OptionalValetMode.valetMode(it) }
                118 -> decoder.decodeSingularBoolField()?.let { this._optionalValetPinNeeded = CarServer_ClosuresState.OneOf_OptionalValetPinNeeded.valetPinNeeded(it) }
                120 -> decoder.decodeSingularBoolField()?.let { this._optionalSentryModeAvailable = CarServer_ClosuresState.OneOf_OptionalSentryModeAvailable.sentryModeAvailable(it) }
                2000 -> decoder.decodeSingularMessageField(this._timestamp) { Google_Protobuf_Timestamp() }?.let {
                    _protobufDropPending(2000)
                    this._timestamp = it
                }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        this._sunRoofState?.let {
            visitor.visitSingularMessageField(it, 11)
        }
        this._centerDisplayState?.let {
            visitor.visitSingularMessageField(it, 15)
        }
        this._sentryModeState?.let {
            visitor.visitSingularMessageField(it, 19)
        }
        this._speedLimitMode?.let {
            visitor.visitSingularMessageField(it, 22)
        }
        (this._optionalTonneauState as? CarServer_ClosuresState.OneOf_OptionalTonneauState.tonneauState)?.let {
            if (it.value != VCSEC_ClosureState_E.UNRECOGNIZED) {
                visitor.visitSingularEnumField(it.value.rawValue, 23)
            }
        }
        (this._optionalTonneauPercentOpen as? CarServer_ClosuresState.OneOf_OptionalTonneauPercentOpen.tonneauPercentOpen)?.let {
            visitor.visitSingularUInt32Field(it.value, 24)
        }
        (this._optionalTonneauInMotion as? CarServer_ClosuresState.OneOf_OptionalTonneauInMotion.tonneauInMotion)?.let {
            visitor.visitSingularBoolField(it.value, 25)
        }
        (this._optionalDoorOpenDriverFront as? CarServer_ClosuresState.OneOf_OptionalDoorOpenDriverFront.doorOpenDriverFront)?.let {
            visitor.visitSingularBoolField(it.value, 101)
        }
        (this._optionalDoorOpenDriverRear as? CarServer_ClosuresState.OneOf_OptionalDoorOpenDriverRear.doorOpenDriverRear)?.let {
            visitor.visitSingularBoolField(it.value, 102)
        }
        (this._optionalDoorOpenPassengerFront as? CarServer_ClosuresState.OneOf_OptionalDoorOpenPassengerFront.doorOpenPassengerFront)?.let {
            visitor.visitSingularBoolField(it.value, 103)
        }
        (this._optionalDoorOpenPassengerRear as? CarServer_ClosuresState.OneOf_OptionalDoorOpenPassengerRear.doorOpenPassengerRear)?.let {
            visitor.visitSingularBoolField(it.value, 104)
        }
        (this._optionalDoorOpenTrunkFront as? CarServer_ClosuresState.OneOf_OptionalDoorOpenTrunkFront.doorOpenTrunkFront)?.let {
            visitor.visitSingularBoolField(it.value, 105)
        }
        (this._optionalDoorOpenTrunkRear as? CarServer_ClosuresState.OneOf_OptionalDoorOpenTrunkRear.doorOpenTrunkRear)?.let {
            visitor.visitSingularBoolField(it.value, 106)
        }
        (this._optionalWindowOpenDriverFront as? CarServer_ClosuresState.OneOf_OptionalWindowOpenDriverFront.windowOpenDriverFront)?.let {
            visitor.visitSingularBoolField(it.value, 107)
        }
        (this._optionalWindowOpenPassengerFront as? CarServer_ClosuresState.OneOf_OptionalWindowOpenPassengerFront.windowOpenPassengerFront)?.let {
            visitor.visitSingularBoolField(it.value, 108)
        }
        (this._optionalWindowOpenDriverRear as? CarServer_ClosuresState.OneOf_OptionalWindowOpenDriverRear.windowOpenDriverRear)?.let {
            visitor.visitSingularBoolField(it.value, 109)
        }
        (this._optionalWindowOpenPassengerRear as? CarServer_ClosuresState.OneOf_OptionalWindowOpenPassengerRear.windowOpenPassengerRear)?.let {
            visitor.visitSingularBoolField(it.value, 110)
        }
        (this._optionalSunRoofPercentOpen as? CarServer_ClosuresState.OneOf_OptionalSunRoofPercentOpen.sunRoofPercentOpen)?.let {
            visitor.visitSingularInt32Field(it.value, 112)
        }
        (this._optionalLocked as? CarServer_ClosuresState.OneOf_OptionalLocked.locked)?.let {
            visitor.visitSingularBoolField(it.value, 113)
        }
        (this._optionalIsUserPresent as? CarServer_ClosuresState.OneOf_OptionalIsUserPresent.isUserPresent)?.let {
            visitor.visitSingularBoolField(it.value, 114)
        }
        (this._optionalRemoteStart as? CarServer_ClosuresState.OneOf_OptionalRemoteStart.remoteStart)?.let {
            visitor.visitSingularBoolField(it.value, 116)
        }
        (this._optionalValetMode as? CarServer_ClosuresState.OneOf_OptionalValetMode.valetMode)?.let {
            visitor.visitSingularBoolField(it.value, 117)
        }
        (this._optionalValetPinNeeded as? CarServer_ClosuresState.OneOf_OptionalValetPinNeeded.valetPinNeeded)?.let {
            visitor.visitSingularBoolField(it.value, 118)
        }
        (this._optionalSentryModeAvailable as? CarServer_ClosuresState.OneOf_OptionalSentryModeAvailable.sentryModeAvailable)?.let {
            visitor.visitSingularBoolField(it.value, 120)
        }
        this._timestamp?.let {
            visitor.visitSingularMessageField(it, 2000)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_ClosuresState) return false
        if (this._optionalDoorOpenDriverFront != other._optionalDoorOpenDriverFront) return false
        if (this._optionalDoorOpenDriverRear != other._optionalDoorOpenDriverRear) return false
        if (this._optionalDoorOpenPassengerFront != other._optionalDoorOpenPassengerFront) return false
        if (this._optionalDoorOpenPassengerRear != other._optionalDoorOpenPassengerRear) return false
        if (this._optionalDoorOpenTrunkFront != other._optionalDoorOpenTrunkFront) return false
        if (this._optionalDoorOpenTrunkRear != other._optionalDoorOpenTrunkRear) return false
        if (this._optionalWindowOpenDriverFront != other._optionalWindowOpenDriverFront) return false
        if (this._optionalWindowOpenPassengerFront != other._optionalWindowOpenPassengerFront) return false
        if (this._optionalWindowOpenDriverRear != other._optionalWindowOpenDriverRear) return false
        if (this._optionalWindowOpenPassengerRear != other._optionalWindowOpenPassengerRear) return false
        if (this._sunRoofState != other._sunRoofState) return false
        if (this._optionalSunRoofPercentOpen != other._optionalSunRoofPercentOpen) return false
        if (this._optionalLocked != other._optionalLocked) return false
        if (this._optionalIsUserPresent != other._optionalIsUserPresent) return false
        if (this._centerDisplayState != other._centerDisplayState) return false
        if (this._optionalRemoteStart != other._optionalRemoteStart) return false
        if (this._optionalValetMode != other._optionalValetMode) return false
        if (this._optionalValetPinNeeded != other._optionalValetPinNeeded) return false
        if (this._sentryModeState != other._sentryModeState) return false
        if (this._optionalSentryModeAvailable != other._optionalSentryModeAvailable) return false
        if (this._speedLimitMode != other._speedLimitMode) return false
        if (this._optionalTonneauState != other._optionalTonneauState) return false
        if (this._optionalTonneauPercentOpen != other._optionalTonneauPercentOpen) return false
        if (this._optionalTonneauInMotion != other._optionalTonneauInMotion) return false
        if (this._timestamp != other._timestamp) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._optionalDoorOpenDriverFront?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalDoorOpenDriverRear?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalDoorOpenPassengerFront?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalDoorOpenPassengerRear?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalDoorOpenTrunkFront?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalDoorOpenTrunkRear?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalWindowOpenDriverFront?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalWindowOpenPassengerFront?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalWindowOpenDriverRear?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalWindowOpenPassengerRear?.hashCode() ?: 0)
        hash = 31 * hash + (this._sunRoofState?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalSunRoofPercentOpen?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalLocked?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalIsUserPresent?.hashCode() ?: 0)
        hash = 31 * hash + (this._centerDisplayState?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalRemoteStart?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalValetMode?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalValetPinNeeded?.hashCode() ?: 0)
        hash = 31 * hash + (this._sentryModeState?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalSentryModeAvailable?.hashCode() ?: 0)
        hash = 31 * hash + (this._speedLimitMode?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalTonneauState?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalTonneauPercentOpen?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalTonneauInMotion?.hashCode() ?: 0)
        hash = 31 * hash + (this._timestamp?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ClosuresState {
        val result = CarServer_ClosuresState()
        result._optionalDoorOpenDriverFront = this._optionalDoorOpenDriverFront
        result._optionalDoorOpenDriverRear = this._optionalDoorOpenDriverRear
        result._optionalDoorOpenPassengerFront = this._optionalDoorOpenPassengerFront
        result._optionalDoorOpenPassengerRear = this._optionalDoorOpenPassengerRear
        result._optionalDoorOpenTrunkFront = this._optionalDoorOpenTrunkFront
        result._optionalDoorOpenTrunkRear = this._optionalDoorOpenTrunkRear
        result._optionalWindowOpenDriverFront = this._optionalWindowOpenDriverFront
        result._optionalWindowOpenPassengerFront = this._optionalWindowOpenPassengerFront
        result._optionalWindowOpenDriverRear = this._optionalWindowOpenDriverRear
        result._optionalWindowOpenPassengerRear = this._optionalWindowOpenPassengerRear
        result._sunRoofState = this._sunRoofState?.copy()
        result._optionalSunRoofPercentOpen = this._optionalSunRoofPercentOpen
        result._optionalLocked = this._optionalLocked
        result._optionalIsUserPresent = this._optionalIsUserPresent
        result._centerDisplayState = this._centerDisplayState?.copy()
        result._optionalRemoteStart = this._optionalRemoteStart
        result._optionalValetMode = this._optionalValetMode
        result._optionalValetPinNeeded = this._optionalValetPinNeeded
        result._sentryModeState = this._sentryModeState?.copy()
        result._optionalSentryModeAvailable = this._optionalSentryModeAvailable
        result._speedLimitMode = this._speedLimitMode?.copy()
        result._optionalTonneauState = this._optionalTonneauState
        result._optionalTonneauPercentOpen = this._optionalTonneauPercentOpen
        result._optionalTonneauInMotion = this._optionalTonneauInMotion
        result._timestamp = this._timestamp?.copy()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ClosuresState"

        fun with(block: CarServer_ClosuresState.() -> Unit): CarServer_ClosuresState =
            CarServer_ClosuresState().apply(block)
    }
}

class CarServer_ChargeScheduleState() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var chargeSchedules: MutableList<CarServer_ChargeSchedule>
        get() = this._chargeSchedules
        set(value) {
            this._chargeSchedules = ProtobufList<CarServer_ChargeSchedule>(this) { it.copy() }.apply { addAll(value) }
            _protobufMutated()
        }

    var optionalChargeScheduleWindow: CarServer_ChargeScheduleState.OneOf_OptionalChargeScheduleWindow?
        get() = this._optionalChargeScheduleWindow
        set(value) {
            this._protobufStore_optionalChargeScheduleWindow(this._protobufCopy_optionalChargeScheduleWindow(value))
            _protobufMutated()
        }

    var chargeScheduleWindow: CarServer_ChargeSchedule
        get() = (this._optionalChargeScheduleWindow as? CarServer_ChargeScheduleState.OneOf_OptionalChargeScheduleWindow.chargeScheduleWindow)?.value
            ?: _protobufPending(2, { CarServer_ChargeSchedule() }) { this._protobufStore_optionalChargeScheduleWindow(CarServer_ChargeScheduleState.OneOf_OptionalChargeScheduleWindow.chargeScheduleWindow(it)) }
        set(value) {
            this.optionalChargeScheduleWindow = CarServer_ChargeScheduleState.OneOf_OptionalChargeScheduleWindow.chargeScheduleWindow(value)
        }

    var optionalChargeBuffer: CarServer_ChargeScheduleState.OneOf_OptionalChargeBuffer?
        get() = this._optionalChargeBuffer
        set(value) {
            this._optionalChargeBuffer = value
            _protobufMutated()
        }

    var chargeBuffer: Int
        get() = (this._optionalChargeBuffer as? CarServer_ChargeScheduleState.OneOf_OptionalChargeBuffer.chargeBuffer)?.value ?: 0
        set(value) {
            this.optionalChargeBuffer = CarServer_ChargeScheduleState.OneOf_OptionalChargeBuffer.chargeBuffer(value)
        }

    var optionalMaxNumChargeSchedules: CarServer_ChargeScheduleState.OneOf_OptionalMaxNumChargeSchedules?
        get() = this._optionalMaxNumChargeSchedules
        set(value) {
            this._optionalMaxNumChargeSchedules = value
            _protobufMutated()
        }

    var maxNumChargeSchedules: UInt
        get() = (this._optionalMaxNumChargeSchedules as? CarServer_ChargeScheduleState.OneOf_OptionalMaxNumChargeSchedules.maxNumChargeSchedules)?.value ?: 0u
        set(value) {
            this.optionalMaxNumChargeSchedules = CarServer_ChargeScheduleState.OneOf_OptionalMaxNumChargeSchedules.maxNumChargeSchedules(value)
        }

    var optionalNextSchedule: CarServer_ChargeScheduleState.OneOf_OptionalNextSchedule?
        get() = this._optionalNextSchedule
        set(value) {
            this._optionalNextSchedule = value
            _protobufMutated()
        }

    var nextSchedule: Boolean
        get() = (this._optionalNextSchedule as? CarServer_ChargeScheduleState.OneOf_OptionalNextSchedule.nextSchedule)?.value ?: false
        set(value) {
            this.optionalNextSchedule = CarServer_ChargeScheduleState.OneOf_OptionalNextSchedule.nextSchedule(value)
        }

    var optionalShowScheduleCompleteState: CarServer_ChargeScheduleState.OneOf_OptionalShowScheduleCompleteState?
        get() = this._optionalShowScheduleCompleteState
        set(value) {
            this._optionalShowScheduleCompleteState = value
            _protobufMutated()
        }

    var showScheduleCompleteState: Boolean
        get() = (this._optionalShowScheduleCompleteState as? CarServer_ChargeScheduleState.OneOf_OptionalShowScheduleCompleteState.showScheduleCompleteState)?.value ?: false
        set(value) {
            this.optionalShowScheduleCompleteState = CarServer_ChargeScheduleState.OneOf_OptionalShowScheduleCompleteState.showScheduleCompleteState(value)
        }

    var timestamp: Google_Protobuf_Timestamp
        get() = this._timestamp ?: _protobufPending(2000, { Google_Protobuf_Timestamp() }) { this._timestamp = it }
        set(value) {
            _protobufDropPending(2000)
            this._timestamp = value.copy()
            _protobufMutated()
        }

    val hasTimestamp: Boolean
        get() = this._timestamp != null

    fun clearTimestamp() {
        _protobufDropPending(2000)
        this._timestamp = null
        _protobufMutated()
    }

    sealed class OneOf_OptionalChargeScheduleWindow(value: Any) : ProtobufOneofCase(value) {
        class chargeScheduleWindow(val value: CarServer_ChargeSchedule) : OneOf_OptionalChargeScheduleWindow(value)
    }

    sealed class OneOf_OptionalChargeBuffer(value: Any) : ProtobufOneofCase(value) {
        class chargeBuffer(val value: Int) : OneOf_OptionalChargeBuffer(value)
    }

    sealed class OneOf_OptionalMaxNumChargeSchedules(value: Any) : ProtobufOneofCase(value) {
        class maxNumChargeSchedules(val value: UInt) : OneOf_OptionalMaxNumChargeSchedules(value)
    }

    sealed class OneOf_OptionalNextSchedule(value: Any) : ProtobufOneofCase(value) {
        class nextSchedule(val value: Boolean) : OneOf_OptionalNextSchedule(value)
    }

    sealed class OneOf_OptionalShowScheduleCompleteState(value: Any) : ProtobufOneofCase(value) {
        class showScheduleCompleteState(val value: Boolean) : OneOf_OptionalShowScheduleCompleteState(value)
    }

    private var _chargeSchedules: ProtobufList<CarServer_ChargeSchedule> = ProtobufList<CarServer_ChargeSchedule>(this) { it.copy() }
    private var _optionalChargeScheduleWindow: CarServer_ChargeScheduleState.OneOf_OptionalChargeScheduleWindow? = null
    private var _optionalChargeBuffer: CarServer_ChargeScheduleState.OneOf_OptionalChargeBuffer? = null
    private var _optionalMaxNumChargeSchedules: CarServer_ChargeScheduleState.OneOf_OptionalMaxNumChargeSchedules? = null
    private var _optionalNextSchedule: CarServer_ChargeScheduleState.OneOf_OptionalNextSchedule? = null
    private var _optionalShowScheduleCompleteState: CarServer_ChargeScheduleState.OneOf_OptionalShowScheduleCompleteState? = null
    private var _timestamp: Google_Protobuf_Timestamp? = null

    private fun _protobufStore_optionalChargeScheduleWindow(value: CarServer_ChargeScheduleState.OneOf_OptionalChargeScheduleWindow?) {
        _protobufDropPending(2)
        this._optionalChargeScheduleWindow = value
    }

    private fun _protobufCopy_optionalChargeScheduleWindow(value: CarServer_ChargeScheduleState.OneOf_OptionalChargeScheduleWindow?): CarServer_ChargeScheduleState.OneOf_OptionalChargeScheduleWindow? =
        when (value) {
            is CarServer_ChargeScheduleState.OneOf_OptionalChargeScheduleWindow.chargeScheduleWindow -> CarServer_ChargeScheduleState.OneOf_OptionalChargeScheduleWindow.chargeScheduleWindow(value.value.copy())
            else -> value
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeRepeatedMessageField(this._chargeSchedules) { CarServer_ChargeSchedule() }
                2 -> decoder.decodeSingularMessageField((this._optionalChargeScheduleWindow as? CarServer_ChargeScheduleState.OneOf_OptionalChargeScheduleWindow.chargeScheduleWindow)?.value) { CarServer_ChargeSchedule() }
                    ?.let { this._protobufStore_optionalChargeScheduleWindow(CarServer_ChargeScheduleState.OneOf_OptionalChargeScheduleWindow.chargeScheduleWindow(it)) }
                3 -> decoder.decodeSingularInt32Field()?.let { this._optionalChargeBuffer = CarServer_ChargeScheduleState.OneOf_OptionalChargeBuffer.chargeBuffer(it) }
                4 -> decoder.decodeSingularUInt32Field()?.let { this._optionalMaxNumChargeSchedules = CarServer_ChargeScheduleState.OneOf_OptionalMaxNumChargeSchedules.maxNumChargeSchedules(it) }
                5 -> decoder.decodeSingularBoolField()?.let { this._optionalNextSchedule = CarServer_ChargeScheduleState.OneOf_OptionalNextSchedule.nextSchedule(it) }
                6 -> decoder.decodeSingularBoolField()?.let { this._optionalShowScheduleCompleteState = CarServer_ChargeScheduleState.OneOf_OptionalShowScheduleCompleteState.showScheduleCompleteState(it) }
                2000 -> decoder.decodeSingularMessageField(this._timestamp) { Google_Protobuf_Timestamp() }?.let {
                    _protobufDropPending(2000)
                    this._timestamp = it
                }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this._chargeSchedules.isNotEmpty()) {
            visitor.visitRepeatedMessageField(this._chargeSchedules, 1)
        }
        (this._optionalChargeScheduleWindow as? CarServer_ChargeScheduleState.OneOf_OptionalChargeScheduleWindow.chargeScheduleWindow)?.let {
            visitor.visitSingularMessageField(it.value, 2)
        }
        (this._optionalChargeBuffer as? CarServer_ChargeScheduleState.OneOf_OptionalChargeBuffer.chargeBuffer)?.let {
            visitor.visitSingularInt32Field(it.value, 3)
        }
        (this._optionalMaxNumChargeSchedules as? CarServer_ChargeScheduleState.OneOf_OptionalMaxNumChargeSchedules.maxNumChargeSchedules)?.let {
            visitor.visitSingularUInt32Field(it.value, 4)
        }
        (this._optionalNextSchedule as? CarServer_ChargeScheduleState.OneOf_OptionalNextSchedule.nextSchedule)?.let {
            visitor.visitSingularBoolField(it.value, 5)
        }
        (this._optionalShowScheduleCompleteState as? CarServer_ChargeScheduleState.OneOf_OptionalShowScheduleCompleteState.showScheduleCompleteState)?.let {
            visitor.visitSingularBoolField(it.value, 6)
        }
        this._timestamp?.let {
            visitor.visitSingularMessageField(it, 2000)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_ChargeScheduleState) return false
        if (this._chargeSchedules != other._chargeSchedules) return false
        if (this._optionalChargeScheduleWindow != other._optionalChargeScheduleWindow) return false
        if (this._optionalChargeBuffer != other._optionalChargeBuffer) return false
        if (this._optionalMaxNumChargeSchedules != other._optionalMaxNumChargeSchedules) return false
        if (this._optionalNextSchedule != other._optionalNextSchedule) return false
        if (this._optionalShowScheduleCompleteState != other._optionalShowScheduleCompleteState) return false
        if (this._timestamp != other._timestamp) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this._chargeSchedules.hashCode()
        hash = 31 * hash + (this._optionalChargeScheduleWindow?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalChargeBuffer?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalMaxNumChargeSchedules?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalNextSchedule?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalShowScheduleCompleteState?.hashCode() ?: 0)
        hash = 31 * hash + (this._timestamp?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ChargeScheduleState {
        val result = CarServer_ChargeScheduleState()
        result._chargeSchedules.addAll(this._chargeSchedules)
        result._optionalChargeScheduleWindow = this._protobufCopy_optionalChargeScheduleWindow(this._optionalChargeScheduleWindow)
        result._optionalChargeBuffer = this._optionalChargeBuffer
        result._optionalMaxNumChargeSchedules = this._optionalMaxNumChargeSchedules
        result._optionalNextSchedule = this._optionalNextSchedule
        result._optionalShowScheduleCompleteState = this._optionalShowScheduleCompleteState
        result._timestamp = this._timestamp?.copy()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ChargeScheduleState"

        fun with(block: CarServer_ChargeScheduleState.() -> Unit): CarServer_ChargeScheduleState =
            CarServer_ChargeScheduleState().apply(block)
    }
}

class CarServer_PreconditioningScheduleState() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var preconditionSchedules: MutableList<CarServer_PreconditionSchedule>
        get() = this._preconditionSchedules
        set(value) {
            this._preconditionSchedules = ProtobufList<CarServer_PreconditionSchedule>(this) { it.copy() }.apply { addAll(value) }
            _protobufMutated()
        }

    var optionalPreconditioningScheduleWindow: CarServer_PreconditioningScheduleState.OneOf_OptionalPreconditioningScheduleWindow?
        get() = this._optionalPreconditioningScheduleWindow
        set(value) {
            this._protobufStore_optionalPreconditioningScheduleWindow(this._protobufCopy_optionalPreconditioningScheduleWindow(value))
            _protobufMutated()
        }

    var preconditioningScheduleWindow: CarServer_PreconditionSchedule
        get() = (this._optionalPreconditioningScheduleWindow as? CarServer_PreconditioningScheduleState.OneOf_OptionalPreconditioningScheduleWindow.preconditioningScheduleWindow)?.value
            ?: _protobufPending(2, { CarServer_PreconditionSchedule() }) { this._protobufStore_optionalPreconditioningScheduleWindow(CarServer_PreconditioningScheduleState.OneOf_OptionalPreconditioningScheduleWindow.preconditioningScheduleWindow(it)) }
        set(value) {
            this.optionalPreconditioningScheduleWindow = CarServer_PreconditioningScheduleState.OneOf_OptionalPreconditioningScheduleWindow.preconditioningScheduleWindow(value)
        }

    var optionalMaxNumPreconditionSchedules: CarServer_PreconditioningScheduleState.OneOf_OptionalMaxNumPreconditionSchedules?
        get() = this._optionalMaxNumPreconditionSchedules
        set(value) {
            this._optionalMaxNumPreconditionSchedules = value
            _protobufMutated()
        }

    var maxNumPreconditionSchedules: UInt
        get() = (this._optionalMaxNumPreconditionSchedules as? CarServer_PreconditioningScheduleState.OneOf_OptionalMaxNumPreconditionSchedules.maxNumPreconditionSchedules)?.value ?: 0u
        set(value) {
            this.optionalMaxNumPreconditionSchedules = CarServer_PreconditioningScheduleState.OneOf_OptionalMaxNumPreconditionSchedules.maxNumPreconditionSchedules(value)
        }

    var optionalNextSchedule: CarServer_PreconditioningScheduleState.OneOf_OptionalNextSchedule?
        get() = this._optionalNextSchedule
        set(value) {
            this._optionalNextSchedule = value
            _protobufMutated()
        }

    var nextSchedule: Boolean
        get() = (this._optionalNextSchedule as? CarServer_PreconditioningScheduleState.OneOf_OptionalNextSchedule.nextSchedule)?.value ?: false
        set(value) {
            this.optionalNextSchedule = CarServer_PreconditioningScheduleState.OneOf_OptionalNextSchedule.nextSchedule(value)
        }

    var timestamp: Google_Protobuf_Timestamp
        get() = this._timestamp ?: _protobufPending(2000, { Google_Protobuf_Timestamp() }) { this._timestamp = it }
        set(value) {
            _protobufDropPending(2000)
            this._timestamp = value.copy()
            _protobufMutated()
        }

    val hasTimestamp: Boolean
        get() = this._timestamp != null

    fun clearTimestamp() {
        _protobufDropPending(2000)
        this._timestamp = null
        _protobufMutated()
    }

    sealed class OneOf_OptionalPreconditioningScheduleWindow(value: Any) : ProtobufOneofCase(value) {
        class preconditioningScheduleWindow(val value: CarServer_PreconditionSchedule) : OneOf_OptionalPreconditioningScheduleWindow(value)
    }

    sealed class OneOf_OptionalMaxNumPreconditionSchedules(value: Any) : ProtobufOneofCase(value) {
        class maxNumPreconditionSchedules(val value: UInt) : OneOf_OptionalMaxNumPreconditionSchedules(value)
    }

    sealed class OneOf_OptionalNextSchedule(value: Any) : ProtobufOneofCase(value) {
        class nextSchedule(val value: Boolean) : OneOf_OptionalNextSchedule(value)
    }

    private var _preconditionSchedules: ProtobufList<CarServer_PreconditionSchedule> = ProtobufList<CarServer_PreconditionSchedule>(this) { it.copy() }
    private var _optionalPreconditioningScheduleWindow: CarServer_PreconditioningScheduleState.OneOf_OptionalPreconditioningScheduleWindow? = null
    private var _optionalMaxNumPreconditionSchedules: CarServer_PreconditioningScheduleState.OneOf_OptionalMaxNumPreconditionSchedules? = null
    private var _optionalNextSchedule: CarServer_PreconditioningScheduleState.OneOf_OptionalNextSchedule? = null
    private var _timestamp: Google_Protobuf_Timestamp? = null

    private fun _protobufStore_optionalPreconditioningScheduleWindow(value: CarServer_PreconditioningScheduleState.OneOf_OptionalPreconditioningScheduleWindow?) {
        _protobufDropPending(2)
        this._optionalPreconditioningScheduleWindow = value
    }

    private fun _protobufCopy_optionalPreconditioningScheduleWindow(value: CarServer_PreconditioningScheduleState.OneOf_OptionalPreconditioningScheduleWindow?): CarServer_PreconditioningScheduleState.OneOf_OptionalPreconditioningScheduleWindow? =
        when (value) {
            is CarServer_PreconditioningScheduleState.OneOf_OptionalPreconditioningScheduleWindow.preconditioningScheduleWindow -> CarServer_PreconditioningScheduleState.OneOf_OptionalPreconditioningScheduleWindow.preconditioningScheduleWindow(value.value.copy())
            else -> value
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeRepeatedMessageField(this._preconditionSchedules) { CarServer_PreconditionSchedule() }
                2 -> decoder.decodeSingularMessageField((this._optionalPreconditioningScheduleWindow as? CarServer_PreconditioningScheduleState.OneOf_OptionalPreconditioningScheduleWindow.preconditioningScheduleWindow)?.value) { CarServer_PreconditionSchedule() }
                    ?.let { this._protobufStore_optionalPreconditioningScheduleWindow(CarServer_PreconditioningScheduleState.OneOf_OptionalPreconditioningScheduleWindow.preconditioningScheduleWindow(it)) }
                3 -> decoder.decodeSingularUInt32Field()?.let { this._optionalMaxNumPreconditionSchedules = CarServer_PreconditioningScheduleState.OneOf_OptionalMaxNumPreconditionSchedules.maxNumPreconditionSchedules(it) }
                4 -> decoder.decodeSingularBoolField()?.let { this._optionalNextSchedule = CarServer_PreconditioningScheduleState.OneOf_OptionalNextSchedule.nextSchedule(it) }
                2000 -> decoder.decodeSingularMessageField(this._timestamp) { Google_Protobuf_Timestamp() }?.let {
                    _protobufDropPending(2000)
                    this._timestamp = it
                }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this._preconditionSchedules.isNotEmpty()) {
            visitor.visitRepeatedMessageField(this._preconditionSchedules, 1)
        }
        (this._optionalPreconditioningScheduleWindow as? CarServer_PreconditioningScheduleState.OneOf_OptionalPreconditioningScheduleWindow.preconditioningScheduleWindow)?.let {
            visitor.visitSingularMessageField(it.value, 2)
        }
        (this._optionalMaxNumPreconditionSchedules as? CarServer_PreconditioningScheduleState.OneOf_OptionalMaxNumPreconditionSchedules.maxNumPreconditionSchedules)?.let {
            visitor.visitSingularUInt32Field(it.value, 3)
        }
        (this._optionalNextSchedule as? CarServer_PreconditioningScheduleState.OneOf_OptionalNextSchedule.nextSchedule)?.let {
            visitor.visitSingularBoolField(it.value, 4)
        }
        this._timestamp?.let {
            visitor.visitSingularMessageField(it, 2000)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_PreconditioningScheduleState) return false
        if (this._preconditionSchedules != other._preconditionSchedules) return false
        if (this._optionalPreconditioningScheduleWindow != other._optionalPreconditioningScheduleWindow) return false
        if (this._optionalMaxNumPreconditionSchedules != other._optionalMaxNumPreconditionSchedules) return false
        if (this._optionalNextSchedule != other._optionalNextSchedule) return false
        if (this._timestamp != other._timestamp) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this._preconditionSchedules.hashCode()
        hash = 31 * hash + (this._optionalPreconditioningScheduleWindow?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalMaxNumPreconditionSchedules?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalNextSchedule?.hashCode() ?: 0)
        hash = 31 * hash + (this._timestamp?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_PreconditioningScheduleState {
        val result = CarServer_PreconditioningScheduleState()
        result._preconditionSchedules.addAll(this._preconditionSchedules)
        result._optionalPreconditioningScheduleWindow = this._protobufCopy_optionalPreconditioningScheduleWindow(this._optionalPreconditioningScheduleWindow)
        result._optionalMaxNumPreconditionSchedules = this._optionalMaxNumPreconditionSchedules
        result._optionalNextSchedule = this._optionalNextSchedule
        result._timestamp = this._timestamp?.copy()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.PreconditioningScheduleState"

        fun with(block: CarServer_PreconditioningScheduleState.() -> Unit): CarServer_PreconditioningScheduleState =
            CarServer_PreconditioningScheduleState().apply(block)
    }
}

class CarServer_SpeedLimitMode() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var optionalActive: CarServer_SpeedLimitMode.OneOf_OptionalActive?
        get() = this._optionalActive
        set(value) {
            this._optionalActive = value
            _protobufMutated()
        }

    var active: Boolean
        get() = (this._optionalActive as? CarServer_SpeedLimitMode.OneOf_OptionalActive.active)?.value ?: false
        set(value) {
            this.optionalActive = CarServer_SpeedLimitMode.OneOf_OptionalActive.active(value)
        }

    var optionalPinCodeSet: CarServer_SpeedLimitMode.OneOf_OptionalPinCodeSet?
        get() = this._optionalPinCodeSet
        set(value) {
            this._optionalPinCodeSet = value
            _protobufMutated()
        }

    var pinCodeSet: Boolean
        get() = (this._optionalPinCodeSet as? CarServer_SpeedLimitMode.OneOf_OptionalPinCodeSet.pinCodeSet)?.value ?: false
        set(value) {
            this.optionalPinCodeSet = CarServer_SpeedLimitMode.OneOf_OptionalPinCodeSet.pinCodeSet(value)
        }

    var optionalMaxLimitMph: CarServer_SpeedLimitMode.OneOf_OptionalMaxLimitMph?
        get() = this._optionalMaxLimitMph
        set(value) {
            this._optionalMaxLimitMph = value
            _protobufMutated()
        }

    var maxLimitMph: Float
        get() = (this._optionalMaxLimitMph as? CarServer_SpeedLimitMode.OneOf_OptionalMaxLimitMph.maxLimitMph)?.value ?: 0f
        set(value) {
            this.optionalMaxLimitMph = CarServer_SpeedLimitMode.OneOf_OptionalMaxLimitMph.maxLimitMph(value)
        }

    var optionalMinLimitMph: CarServer_SpeedLimitMode.OneOf_OptionalMinLimitMph?
        get() = this._optionalMinLimitMph
        set(value) {
            this._optionalMinLimitMph = value
            _protobufMutated()
        }

    var minLimitMph: Float
        get() = (this._optionalMinLimitMph as? CarServer_SpeedLimitMode.OneOf_OptionalMinLimitMph.minLimitMph)?.value ?: 0f
        set(value) {
            this.optionalMinLimitMph = CarServer_SpeedLimitMode.OneOf_OptionalMinLimitMph.minLimitMph(value)
        }

    var optionalCurrentLimitMph: CarServer_SpeedLimitMode.OneOf_OptionalCurrentLimitMph?
        get() = this._optionalCurrentLimitMph
        set(value) {
            this._optionalCurrentLimitMph = value
            _protobufMutated()
        }

    var currentLimitMph: Float
        get() = (this._optionalCurrentLimitMph as? CarServer_SpeedLimitMode.OneOf_OptionalCurrentLimitMph.currentLimitMph)?.value ?: 0f
        set(value) {
            this.optionalCurrentLimitMph = CarServer_SpeedLimitMode.OneOf_OptionalCurrentLimitMph.currentLimitMph(value)
        }

    sealed class OneOf_OptionalActive(value: Any) : ProtobufOneofCase(value) {
        class active(val value: Boolean) : OneOf_OptionalActive(value)
    }

    sealed class OneOf_OptionalPinCodeSet(value: Any) : ProtobufOneofCase(value) {
        class pinCodeSet(val value: Boolean) : OneOf_OptionalPinCodeSet(value)
    }

    sealed class OneOf_OptionalMaxLimitMph(value: Any) : ProtobufOneofCase(value) {
        class maxLimitMph(val value: Float) : OneOf_OptionalMaxLimitMph(value)
    }

    sealed class OneOf_OptionalMinLimitMph(value: Any) : ProtobufOneofCase(value) {
        class minLimitMph(val value: Float) : OneOf_OptionalMinLimitMph(value)
    }

    sealed class OneOf_OptionalCurrentLimitMph(value: Any) : ProtobufOneofCase(value) {
        class currentLimitMph(val value: Float) : OneOf_OptionalCurrentLimitMph(value)
    }

    private var _optionalActive: CarServer_SpeedLimitMode.OneOf_OptionalActive? = null
    private var _optionalPinCodeSet: CarServer_SpeedLimitMode.OneOf_OptionalPinCodeSet? = null
    private var _optionalMaxLimitMph: CarServer_SpeedLimitMode.OneOf_OptionalMaxLimitMph? = null
    private var _optionalMinLimitMph: CarServer_SpeedLimitMode.OneOf_OptionalMinLimitMph? = null
    private var _optionalCurrentLimitMph: CarServer_SpeedLimitMode.OneOf_OptionalCurrentLimitMph? = null

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                103 -> decoder.decodeSingularBoolField()?.let { this._optionalActive = CarServer_SpeedLimitMode.OneOf_OptionalActive.active(it) }
                104 -> decoder.decodeSingularBoolField()?.let { this._optionalPinCodeSet = CarServer_SpeedLimitMode.OneOf_OptionalPinCodeSet.pinCodeSet(it) }
                106 -> decoder.decodeSingularFloatField()?.let { this._optionalMaxLimitMph = CarServer_SpeedLimitMode.OneOf_OptionalMaxLimitMph.maxLimitMph(it) }
                107 -> decoder.decodeSingularFloatField()?.let { this._optionalMinLimitMph = CarServer_SpeedLimitMode.OneOf_OptionalMinLimitMph.minLimitMph(it) }
                108 -> decoder.decodeSingularFloatField()?.let { this._optionalCurrentLimitMph = CarServer_SpeedLimitMode.OneOf_OptionalCurrentLimitMph.currentLimitMph(it) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        (this._optionalActive as? CarServer_SpeedLimitMode.OneOf_OptionalActive.active)?.let {
            visitor.visitSingularBoolField(it.value, 103)
        }
        (this._optionalPinCodeSet as? CarServer_SpeedLimitMode.OneOf_OptionalPinCodeSet.pinCodeSet)?.let {
            visitor.visitSingularBoolField(it.value, 104)
        }
        (this._optionalMaxLimitMph as? CarServer_SpeedLimitMode.OneOf_OptionalMaxLimitMph.maxLimitMph)?.let {
            visitor.visitSingularFloatField(it.value, 106)
        }
        (this._optionalMinLimitMph as? CarServer_SpeedLimitMode.OneOf_OptionalMinLimitMph.minLimitMph)?.let {
            visitor.visitSingularFloatField(it.value, 107)
        }
        (this._optionalCurrentLimitMph as? CarServer_SpeedLimitMode.OneOf_OptionalCurrentLimitMph.currentLimitMph)?.let {
            visitor.visitSingularFloatField(it.value, 108)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_SpeedLimitMode) return false
        if (this._optionalActive != other._optionalActive) return false
        if (this._optionalPinCodeSet != other._optionalPinCodeSet) return false
        if (this._optionalMaxLimitMph != other._optionalMaxLimitMph) return false
        if (this._optionalMinLimitMph != other._optionalMinLimitMph) return false
        if (this._optionalCurrentLimitMph != other._optionalCurrentLimitMph) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._optionalActive?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalPinCodeSet?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalMaxLimitMph?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalMinLimitMph?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalCurrentLimitMph?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_SpeedLimitMode {
        val result = CarServer_SpeedLimitMode()
        result._optionalActive = this._optionalActive
        result._optionalPinCodeSet = this._optionalPinCodeSet
        result._optionalMaxLimitMph = this._optionalMaxLimitMph
        result._optionalMinLimitMph = this._optionalMinLimitMph
        result._optionalCurrentLimitMph = this._optionalCurrentLimitMph
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.SpeedLimitMode"

        fun with(block: CarServer_SpeedLimitMode.() -> Unit): CarServer_SpeedLimitMode =
            CarServer_SpeedLimitMode().apply(block)
    }
}

class CarServer_ParentalControlsSettings() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var optionalSpeedLimitEnabled: CarServer_ParentalControlsSettings.OneOf_OptionalSpeedLimitEnabled?
        get() = this._optionalSpeedLimitEnabled
        set(value) {
            this._optionalSpeedLimitEnabled = value
            _protobufMutated()
        }

    var speedLimitEnabled: Boolean
        get() = (this._optionalSpeedLimitEnabled as? CarServer_ParentalControlsSettings.OneOf_OptionalSpeedLimitEnabled.speedLimitEnabled)?.value ?: false
        set(value) {
            this.optionalSpeedLimitEnabled = CarServer_ParentalControlsSettings.OneOf_OptionalSpeedLimitEnabled.speedLimitEnabled(value)
        }

    var optionalMaxLimitMph: CarServer_ParentalControlsSettings.OneOf_OptionalMaxLimitMph?
        get() = this._optionalMaxLimitMph
        set(value) {
            this._optionalMaxLimitMph = value
            _protobufMutated()
        }

    var maxLimitMph: Float
        get() = (this._optionalMaxLimitMph as? CarServer_ParentalControlsSettings.OneOf_OptionalMaxLimitMph.maxLimitMph)?.value ?: 0f
        set(value) {
            this.optionalMaxLimitMph = CarServer_ParentalControlsSettings.OneOf_OptionalMaxLimitMph.maxLimitMph(value)
        }

    var optionalMinLimitMph: CarServer_ParentalControlsSettings.OneOf_OptionalMinLimitMph?
        get() = this._optionalMinLimitMph
        set(value) {
            this._optionalMinLimitMph = value
            _protobufMutated()
        }

    var minLimitMph: Float
        get() = (this._optionalMinLimitMph as? CarServer_ParentalControlsSettings.OneOf_OptionalMinLimitMph.minLimitMph)?.value ?: 0f
        set(value) {
            this.optionalMinLimitMph = CarServer_ParentalControlsSettings.OneOf_OptionalMinLimitMph.minLimitMph(value)
        }

    var optionalCurrentLimitMph: CarServer_ParentalControlsSettings.OneOf_OptionalCurrentLimitMph?
        get() = this._optionalCurrentLimitMph
        set(value) {
            this._optionalCurrentLimitMph = value
            _protobufMutated()
        }

    var currentLimitMph: Float
        get() = (this._optionalCurrentLimitMph as? CarServer_ParentalControlsSettings.OneOf_OptionalCurrentLimitMph.currentLimitMph)?.value ?: 0f
        set(value) {
            this.optionalCurrentLimitMph = CarServer_ParentalControlsSettings.OneOf_OptionalCurrentLimitMph.currentLimitMph(value)
        }

    var optionalChillAccelerationEnabled: CarServer_ParentalControlsSettings.OneOf_OptionalChillAccelerationEnabled?
        get() = this._optionalChillAccelerationEnabled
        set(value) {
            this._optionalChillAccelerationEnabled = value
            _protobufMutated()
        }

    var chillAccelerationEnabled: Boolean
        get() = (this._optionalChillAccelerationEnabled as? CarServer_ParentalControlsSettings.OneOf_OptionalChillAccelerationEnabled.chillAccelerationEnabled)?.value ?: false
        set(value) {
            this.optionalChillAccelerationEnabled = CarServer_ParentalControlsSettings.OneOf_OptionalChillAccelerationEnabled.chillAccelerationEnabled(value)
        }

    var optionalRequireSafetySettingsEnabled: CarServer_ParentalControlsSettings.OneOf_OptionalRequireSafetySettingsEnabled?
        get() = this._optionalRequireSafetySettingsEnabled
        set(value) {
            this._optionalRequireSafetySettingsEnabled = value
            _protobufMutated()
        }

    var requireSafetySettingsEnabled: Boolean
        get() = (this._optionalRequireSafetySettingsEnabled as? CarServer_ParentalControlsSettings.OneOf_OptionalRequireSafetySettingsEnabled.requireSafetySettingsEnabled)?.value ?: false
        set(value) {
            this.optionalRequireSafetySettingsEnabled = CarServer_ParentalControlsSettings.OneOf_OptionalRequireSafetySettingsEnabled.requireSafetySettingsEnabled(value)
        }

    var optionalCurfewEnabled: CarServer_ParentalControlsSettings.OneOf_OptionalCurfewEnabled?
        get() = this._optionalCurfewEnabled
        set(value) {
            this._optionalCurfewEnabled = value
            _protobufMutated()
        }

    var curfewEnabled: Boolean
        get() = (this._optionalCurfewEnabled as? CarServer_ParentalControlsSettings.OneOf_OptionalCurfewEnabled.curfewEnabled)?.value ?: false
        set(value) {
            this.optionalCurfewEnabled = CarServer_ParentalControlsSettings.OneOf_OptionalCurfewEnabled.curfewEnabled(value)
        }

    var optionalCurfewStartTime: CarServer_ParentalControlsSettings.OneOf_OptionalCurfewStartTime?
        get() = this._optionalCurfewStartTime
        set(value) {
            this._optionalCurfewStartTime = value
            _protobufMutated()
        }

    var curfewStartTime: Int
        get() = (this._optionalCurfewStartTime as? CarServer_ParentalControlsSettings.OneOf_OptionalCurfewStartTime.curfewStartTime)?.value ?: 0
        set(value) {
            this.optionalCurfewStartTime = CarServer_ParentalControlsSettings.OneOf_OptionalCurfewStartTime.curfewStartTime(value)
        }

    var optionalCurfewEndTime: CarServer_ParentalControlsSettings.OneOf_OptionalCurfewEndTime?
        get() = this._optionalCurfewEndTime
        set(value) {
            this._optionalCurfewEndTime = value
            _protobufMutated()
        }

    var curfewEndTime: Int
        get() = (this._optionalCurfewEndTime as? CarServer_ParentalControlsSettings.OneOf_OptionalCurfewEndTime.curfewEndTime)?.value ?: 0
        set(value) {
            this.optionalCurfewEndTime = CarServer_ParentalControlsSettings.OneOf_OptionalCurfewEndTime.curfewEndTime(value)
        }

    sealed class OneOf_OptionalSpeedLimitEnabled(value: Any) : ProtobufOneofCase(value) {
        class speedLimitEnabled(val value: Boolean) : OneOf_OptionalSpeedLimitEnabled(value)
    }

    sealed class OneOf_OptionalMaxLimitMph(value: Any) : ProtobufOneofCase(value) {
        class maxLimitMph(val value: Float) : OneOf_OptionalMaxLimitMph(value)
    }

    sealed class OneOf_OptionalMinLimitMph(value: Any) : ProtobufOneofCase(value) {
        class minLimitMph(val value: Float) : OneOf_OptionalMinLimitMph(value)
    }

    sealed class OneOf_OptionalCurrentLimitMph(value: Any) : ProtobufOneofCase(value) {
        class currentLimitMph(val value: Float) : OneOf_OptionalCurrentLimitMph(value)
    }

    sealed class OneOf_OptionalChillAccelerationEnabled(value: Any) : ProtobufOneofCase(value) {
        class chillAccelerationEnabled(val value: Boolean) : OneOf_OptionalChillAccelerationEnabled(value)
    }

    sealed class OneOf_OptionalRequireSafetySettingsEnabled(value: Any) : ProtobufOneofCase(value) {
        class requireSafetySettingsEnabled(val value: Boolean) : OneOf_OptionalRequireSafetySettingsEnabled(value)
    }

    sealed class OneOf_OptionalCurfewEnabled(value: Any) : ProtobufOneofCase(value) {
        class curfewEnabled(val value: Boolean) : OneOf_OptionalCurfewEnabled(value)
    }

    sealed class OneOf_OptionalCurfewStartTime(value: Any) : ProtobufOneofCase(value) {
        class curfewStartTime(val value: Int) : OneOf_OptionalCurfewStartTime(value)
    }

    sealed class OneOf_OptionalCurfewEndTime(value: Any) : ProtobufOneofCase(value) {
        class curfewEndTime(val value: Int) : OneOf_OptionalCurfewEndTime(value)
    }

    private var _optionalSpeedLimitEnabled: CarServer_ParentalControlsSettings.OneOf_OptionalSpeedLimitEnabled? = null
    private var _optionalMaxLimitMph: CarServer_ParentalControlsSettings.OneOf_OptionalMaxLimitMph? = null
    private var _optionalMinLimitMph: CarServer_ParentalControlsSettings.OneOf_OptionalMinLimitMph? = null
    private var _optionalCurrentLimitMph: CarServer_ParentalControlsSettings.OneOf_OptionalCurrentLimitMph? = null
    private var _optionalChillAccelerationEnabled: CarServer_ParentalControlsSettings.OneOf_OptionalChillAccelerationEnabled? = null
    private var _optionalRequireSafetySettingsEnabled: CarServer_ParentalControlsSettings.OneOf_OptionalRequireSafetySettingsEnabled? = null
    private var _optionalCurfewEnabled: CarServer_ParentalControlsSettings.OneOf_OptionalCurfewEnabled? = null
    private var _optionalCurfewStartTime: CarServer_ParentalControlsSettings.OneOf_OptionalCurfewStartTime? = null
    private var _optionalCurfewEndTime: CarServer_ParentalControlsSettings.OneOf_OptionalCurfewEndTime? = null

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularBoolField()?.let { this._optionalSpeedLimitEnabled = CarServer_ParentalControlsSettings.OneOf_OptionalSpeedLimitEnabled.speedLimitEnabled(it) }
                2 -> decoder.decodeSingularFloatField()?.let { this._optionalMaxLimitMph = CarServer_ParentalControlsSettings.OneOf_OptionalMaxLimitMph.maxLimitMph(it) }
                3 -> decoder.decodeSingularFloatField()?.let { this._optionalMinLimitMph = CarServer_ParentalControlsSettings.OneOf_OptionalMinLimitMph.minLimitMph(it) }
                4 -> decoder.decodeSingularFloatField()?.let { this._optionalCurrentLimitMph = CarServer_ParentalControlsSettings.OneOf_OptionalCurrentLimitMph.currentLimitMph(it) }
                5 -> decoder.decodeSingularBoolField()?.let { this._optionalChillAccelerationEnabled = CarServer_ParentalControlsSettings.OneOf_OptionalChillAccelerationEnabled.chillAccelerationEnabled(it) }
                6 -> decoder.decodeSingularBoolField()?.let { this._optionalRequireSafetySettingsEnabled = CarServer_ParentalControlsSettings.OneOf_OptionalRequireSafetySettingsEnabled.requireSafetySettingsEnabled(it) }
                7 -> decoder.decodeSingularBoolField()?.let { this._optionalCurfewEnabled = CarServer_ParentalControlsSettings.OneOf_OptionalCurfewEnabled.curfewEnabled(it) }
                8 -> decoder.decodeSingularInt32Field()?.let { this._optionalCurfewStartTime = CarServer_ParentalControlsSettings.OneOf_OptionalCurfewStartTime.curfewStartTime(it) }
                9 -> decoder.decodeSingularInt32Field()?.let { this._optionalCurfewEndTime = CarServer_ParentalControlsSettings.OneOf_OptionalCurfewEndTime.curfewEndTime(it) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        (this._optionalSpeedLimitEnabled as? CarServer_ParentalControlsSettings.OneOf_OptionalSpeedLimitEnabled.speedLimitEnabled)?.let {
            visitor.visitSingularBoolField(it.value, 1)
        }
        (this._optionalMaxLimitMph as? CarServer_ParentalControlsSettings.OneOf_OptionalMaxLimitMph.maxLimitMph)?.let {
            visitor.visitSingularFloatField(it.value, 2)
        }
        (this._optionalMinLimitMph as? CarServer_ParentalControlsSettings.OneOf_OptionalMinLimitMph.minLimitMph)?.let {
            visitor.visitSingularFloatField(it.value, 3)
        }
        (this._optionalCurrentLimitMph as? CarServer_ParentalControlsSettings.OneOf_OptionalCurrentLimitMph.currentLimitMph)?.let {
            visitor.visitSingularFloatField(it.value, 4)
        }
        (this._optionalChillAccelerationEnabled as? CarServer_ParentalControlsSettings.OneOf_OptionalChillAccelerationEnabled.chillAccelerationEnabled)?.let {
            visitor.visitSingularBoolField(it.value, 5)
        }
        (this._optionalRequireSafetySettingsEnabled as? CarServer_ParentalControlsSettings.OneOf_OptionalRequireSafetySettingsEnabled.requireSafetySettingsEnabled)?.let {
            visitor.visitSingularBoolField(it.value, 6)
        }
        (this._optionalCurfewEnabled as? CarServer_ParentalControlsSettings.OneOf_OptionalCurfewEnabled.curfewEnabled)?.let {
            visitor.visitSingularBoolField(it.value, 7)
        }
        (this._optionalCurfewStartTime as? CarServer_ParentalControlsSettings.OneOf_OptionalCurfewStartTime.curfewStartTime)?.let {
            visitor.visitSingularInt32Field(it.value, 8)
        }
        (this._optionalCurfewEndTime as? CarServer_ParentalControlsSettings.OneOf_OptionalCurfewEndTime.curfewEndTime)?.let {
            visitor.visitSingularInt32Field(it.value, 9)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_ParentalControlsSettings) return false
        if (this._optionalSpeedLimitEnabled != other._optionalSpeedLimitEnabled) return false
        if (this._optionalMaxLimitMph != other._optionalMaxLimitMph) return false
        if (this._optionalMinLimitMph != other._optionalMinLimitMph) return false
        if (this._optionalCurrentLimitMph != other._optionalCurrentLimitMph) return false
        if (this._optionalChillAccelerationEnabled != other._optionalChillAccelerationEnabled) return false
        if (this._optionalRequireSafetySettingsEnabled != other._optionalRequireSafetySettingsEnabled) return false
        if (this._optionalCurfewEnabled != other._optionalCurfewEnabled) return false
        if (this._optionalCurfewStartTime != other._optionalCurfewStartTime) return false
        if (this._optionalCurfewEndTime != other._optionalCurfewEndTime) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._optionalSpeedLimitEnabled?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalMaxLimitMph?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalMinLimitMph?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalCurrentLimitMph?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalChillAccelerationEnabled?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalRequireSafetySettingsEnabled?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalCurfewEnabled?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalCurfewStartTime?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalCurfewEndTime?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ParentalControlsSettings {
        val result = CarServer_ParentalControlsSettings()
        result._optionalSpeedLimitEnabled = this._optionalSpeedLimitEnabled
        result._optionalMaxLimitMph = this._optionalMaxLimitMph
        result._optionalMinLimitMph = this._optionalMinLimitMph
        result._optionalCurrentLimitMph = this._optionalCurrentLimitMph
        result._optionalChillAccelerationEnabled = this._optionalChillAccelerationEnabled
        result._optionalRequireSafetySettingsEnabled = this._optionalRequireSafetySettingsEnabled
        result._optionalCurfewEnabled = this._optionalCurfewEnabled
        result._optionalCurfewStartTime = this._optionalCurfewStartTime
        result._optionalCurfewEndTime = this._optionalCurfewEndTime
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ParentalControlsSettings"

        fun with(block: CarServer_ParentalControlsSettings.() -> Unit): CarServer_ParentalControlsSettings =
            CarServer_ParentalControlsSettings().apply(block)
    }
}

class CarServer_ParentalControlsState() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var timestamp: Google_Protobuf_Timestamp
        get() = this._timestamp ?: _protobufPending(1, { Google_Protobuf_Timestamp() }) { this._timestamp = it }
        set(value) {
            _protobufDropPending(1)
            this._timestamp = value.copy()
            _protobufMutated()
        }

    val hasTimestamp: Boolean
        get() = this._timestamp != null

    fun clearTimestamp() {
        _protobufDropPending(1)
        this._timestamp = null
        _protobufMutated()
    }

    var optionalParentalControlsActive: CarServer_ParentalControlsState.OneOf_OptionalParentalControlsActive?
        get() = this._optionalParentalControlsActive
        set(value) {
            this._optionalParentalControlsActive = value
            _protobufMutated()
        }

    var parentalControlsActive: Boolean
        get() = (this._optionalParentalControlsActive as? CarServer_ParentalControlsState.OneOf_OptionalParentalControlsActive.parentalControlsActive)?.value ?: false
        set(value) {
            this.optionalParentalControlsActive = CarServer_ParentalControlsState.OneOf_OptionalParentalControlsActive.parentalControlsActive(value)
        }

    var optionalParentalControlsPinSet: CarServer_ParentalControlsState.OneOf_OptionalParentalControlsPinSet?
        get() = this._optionalParentalControlsPinSet
        set(value) {
            this._optionalParentalControlsPinSet = value
            _protobufMutated()
        }

    var parentalControlsPinSet: Boolean
        get() = (this._optionalParentalControlsPinSet as? CarServer_ParentalControlsState.OneOf_OptionalParentalControlsPinSet.parentalControlsPinSet)?.value ?: false
        set(value) {
            this.optionalParentalControlsPinSet = CarServer_ParentalControlsState.OneOf_OptionalParentalControlsPinSet.parentalControlsPinSet(value)
        }

    var parentalControlsSettings: CarServer_ParentalControlsSettings
        get() = this._parentalControlsSettings ?: _protobufPending(4, { CarServer_ParentalControlsSettings() }) { this._parentalControlsSettings = it }
        set(value) {
            _protobufDropPending(4)
            this._parentalControlsSettings = value.copy()
            _protobufMutated()
        }

    val hasParentalControlsSettings: Boolean
        get() = this._parentalControlsSettings != null

    fun clearParentalControlsSettings() {
        _protobufDropPending(4)
        this._parentalControlsSettings = null
        _protobufMutated()
    }

    sealed class OneOf_OptionalParentalControlsActive(value: Any) : ProtobufOneofCase(value) {
        class parentalControlsActive(val value: Boolean) : OneOf_OptionalParentalControlsActive(value)
    }

    sealed class OneOf_OptionalParentalControlsPinSet(value: Any) : ProtobufOneofCase(value) {
        class parentalControlsPinSet(val value: Boolean) : OneOf_OptionalParentalControlsPinSet(value)
    }

    private var _timestamp: Google_Protobuf_Timestamp? = null
    private var _optionalParentalControlsActive: CarServer_ParentalControlsState.OneOf_OptionalParentalControlsActive? = null
    private var _optionalParentalControlsPinSet: CarServer_ParentalControlsState.OneOf_OptionalParentalControlsPinSet? = null
    private var _parentalControlsSettings: CarServer_ParentalControlsSettings? = null

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularMessageField(this._timestamp) { Google_Protobuf_Timestamp() }?.let {
                    _protobufDropPending(1)
                    this._timestamp = it
                }
                2 -> decoder.decodeSingularBoolField()?.let { this._optionalParentalControlsActive = CarServer_ParentalControlsState.OneOf_OptionalParentalControlsActive.parentalControlsActive(it) }
                3 -> decoder.decodeSingularBoolField()?.let { this._optionalParentalControlsPinSet = CarServer_ParentalControlsState.OneOf_OptionalParentalControlsPinSet.parentalControlsPinSet(it) }
                4 -> decoder.decodeSingularMessageField(this._parentalControlsSettings) { CarServer_ParentalControlsSettings() }?.let {
                    _protobufDropPending(4)
                    this._parentalControlsSettings = it
                }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        this._timestamp?.let {
            visitor.visitSingularMessageField(it, 1)
        }
        (this._optionalParentalControlsActive as? CarServer_ParentalControlsState.OneOf_OptionalParentalControlsActive.parentalControlsActive)?.let {
            visitor.visitSingularBoolField(it.value, 2)
        }
        (this._optionalParentalControlsPinSet as? CarServer_ParentalControlsState.OneOf_OptionalParentalControlsPinSet.parentalControlsPinSet)?.let {
            visitor.visitSingularBoolField(it.value, 3)
        }
        this._parentalControlsSettings?.let {
            visitor.visitSingularMessageField(it, 4)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_ParentalControlsState) return false
        if (this._timestamp != other._timestamp) return false
        if (this._optionalParentalControlsActive != other._optionalParentalControlsActive) return false
        if (this._optionalParentalControlsPinSet != other._optionalParentalControlsPinSet) return false
        if (this._parentalControlsSettings != other._parentalControlsSettings) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._timestamp?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalParentalControlsActive?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalParentalControlsPinSet?.hashCode() ?: 0)
        hash = 31 * hash + (this._parentalControlsSettings?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ParentalControlsState {
        val result = CarServer_ParentalControlsState()
        result._timestamp = this._timestamp?.copy()
        result._optionalParentalControlsActive = this._optionalParentalControlsActive
        result._optionalParentalControlsPinSet = this._optionalParentalControlsPinSet
        result._parentalControlsSettings = this._parentalControlsSettings?.copy()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ParentalControlsState"

        fun with(block: CarServer_ParentalControlsState.() -> Unit): CarServer_ParentalControlsState =
            CarServer_ParentalControlsState().apply(block)
    }
}

class CarServer_SoftwareUpdateState() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var status: CarServer_SoftwareUpdateState.SoftwareUpdateStatus
        get() = this._status ?: _protobufPending(1, { CarServer_SoftwareUpdateState.SoftwareUpdateStatus() }) { this._status = it }
        set(value) {
            _protobufDropPending(1)
            this._status = value.copy()
            _protobufMutated()
        }

    val hasStatus: Boolean
        get() = this._status != null

    fun clearStatus() {
        _protobufDropPending(1)
        this._status = null
        _protobufMutated()
    }

    var optionalScheduledTimeMs: CarServer_SoftwareUpdateState.OneOf_OptionalScheduledTimeMs?
        get() = this._optionalScheduledTimeMs
        set(value) {
            this._optionalScheduledTimeMs = value
            _protobufMutated()
        }

    var scheduledTimeMs: ULong
        get() = (this._optionalScheduledTimeMs as? CarServer_SoftwareUpdateState.OneOf_OptionalScheduledTimeMs.scheduledTimeMs)?.value ?: 0uL
        set(value) {
            this.optionalScheduledTimeMs = CarServer_SoftwareUpdateState.OneOf_OptionalScheduledTimeMs.scheduledTimeMs(value)
        }

    var optionalWarningTimeRemainingMs: CarServer_SoftwareUpdateState.OneOf_OptionalWarningTimeRemainingMs?
        get() = this._optionalWarningTimeRemainingMs
        set(value) {
            this._optionalWarningTimeRemainingMs = value
            _protobufMutated()
        }

    var warningTimeRemainingMs: ULong
        get() = (this._optionalWarningTimeRemainingMs as? CarServer_SoftwareUpdateState.OneOf_OptionalWarningTimeRemainingMs.warningTimeRemainingMs)?.value ?: 0uL
        set(value) {
            this.optionalWarningTimeRemainingMs = CarServer_SoftwareUpdateState.OneOf_OptionalWarningTimeRemainingMs.warningTimeRemainingMs(value)
        }

    var optionalExpectedDurationSec: CarServer_SoftwareUpdateState.OneOf_OptionalExpectedDurationSec?
        get() = this._optionalExpectedDurationSec
        set(value) {
            this._optionalExpectedDurationSec = value
            _protobufMutated()
        }

    var expectedDurationSec: UInt
        get() = (this._optionalExpectedDurationSec as? CarServer_SoftwareUpdateState.OneOf_OptionalExpectedDurationSec.expectedDurationSec)?.value ?: 0u
        set(value) {
            this.optionalExpectedDurationSec = CarServer_SoftwareUpdateState.OneOf_OptionalExpectedDurationSec.expectedDurationSec(value)
        }

    var optionalDownloadPerc: CarServer_SoftwareUpdateState.OneOf_OptionalDownloadPerc?
        get() = this._optionalDownloadPerc
        set(value) {
            this._optionalDownloadPerc = value
            _protobufMutated()
        }

    var downloadPerc: UInt
        get() = (this._optionalDownloadPerc as? CarServer_SoftwareUpdateState.OneOf_OptionalDownloadPerc.downloadPerc)?.value ?: 0u
        set(value) {
            this.optionalDownloadPerc = CarServer_SoftwareUpdateState.OneOf_OptionalDownloadPerc.downloadPerc(value)
        }

    var optionalInstallPerc: CarServer_SoftwareUpdateState.OneOf_OptionalInstallPerc?
        get() = this._optionalInstallPerc
        set(value) {
            this._optionalInstallPerc = value
            _protobufMutated()
        }

    var installPerc: UInt
        get() = (this._optionalInstallPerc as? CarServer_SoftwareUpdateState.OneOf_OptionalInstallPerc.installPerc)?.value ?: 0u
        set(value) {
            this.optionalInstallPerc = CarServer_SoftwareUpdateState.OneOf_OptionalInstallPerc.installPerc(value)
        }

    var optionalVersion: CarServer_SoftwareUpdateState.OneOf_OptionalVersion?
        get() = this._optionalVersion
        set(value) {
            this._optionalVersion = value
            _protobufMutated()
        }

    var version: String
        get() = (this._optionalVersion as? CarServer_SoftwareUpdateState.OneOf_OptionalVersion.version)?.value ?: ""
        set(value) {
            this.optionalVersion = CarServer_SoftwareUpdateState.OneOf_OptionalVersion.version(value)
        }

    var timestamp: Google_Protobuf_Timestamp
        get() = this._timestamp ?: _protobufPending(108, { Google_Protobuf_Timestamp() }) { this._timestamp = it }
        set(value) {
            _protobufDropPending(108)
            this._timestamp = value.copy()
            _protobufMutated()
        }

    val hasTimestamp: Boolean
        get() = this._timestamp != null

    fun clearTimestamp() {
        _protobufDropPending(108)
        this._timestamp = null
        _protobufMutated()
    }

    sealed class OneOf_OptionalScheduledTimeMs(value: Any) : ProtobufOneofCase(value) {
        class scheduledTimeMs(val value: ULong) : OneOf_OptionalScheduledTimeMs(value)
    }

    sealed class OneOf_OptionalWarningTimeRemainingMs(value: Any) : ProtobufOneofCase(value) {
        class warningTimeRemainingMs(val value: ULong) : OneOf_OptionalWarningTimeRemainingMs(value)
    }

    sealed class OneOf_OptionalExpectedDurationSec(value: Any) : ProtobufOneofCase(value) {
        class expectedDurationSec(val value: UInt) : OneOf_OptionalExpectedDurationSec(value)
    }

    sealed class OneOf_OptionalDownloadPerc(value: Any) : ProtobufOneofCase(value) {
        class downloadPerc(val value: UInt) : OneOf_OptionalDownloadPerc(value)
    }

    sealed class OneOf_OptionalInstallPerc(value: Any) : ProtobufOneofCase(value) {
        class installPerc(val value: UInt) : OneOf_OptionalInstallPerc(value)
    }

    sealed class OneOf_OptionalVersion(value: Any) : ProtobufOneofCase(value) {
        class version(val value: String) : OneOf_OptionalVersion(value)
    }

    class SoftwareUpdateStatus() : GeneratedMessage() {
        constructor(serializedBytes: ByteArray) : this() {
            merge(serializedBytes)
        }

        var type: CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type?
            get() = this._type
            set(value) {
                this._protobufStore_type(this._protobufCopy_type(value))
                _protobufMutated()
            }

        var unknown: CarServer_Void
            get() = (this._type as? CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.unknown)?.value
                ?: _protobufPending(1, { CarServer_Void() }) { this._protobufStore_type(CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.unknown(it)) }
            set(value) {
                this.type = CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.unknown(value)
            }

        var installing: CarServer_Void
            get() = (this._type as? CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.installing)?.value
                ?: _protobufPending(2, { CarServer_Void() }) { this._protobufStore_type(CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.installing(it)) }
            set(value) {
                this.type = CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.installing(value)
            }

        var scheduled: CarServer_Void
            get() = (this._type as? CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.scheduled)?.value
                ?: _protobufPending(3, { CarServer_Void() }) { this._protobufStore_type(CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.scheduled(it)) }
            set(value) {
                this.type = CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.scheduled(value)
            }

        var available: CarServer_Void
            get() = (this._type as? CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.available)?.value
                ?: _protobufPending(4, { CarServer_Void() }) { this._protobufStore_type(CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.available(it)) }
            set(value) {
                this.type = CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.available(value)
            }

        var downloadingWifiWait: CarServer_Void
            get() = (this._type as? CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.downloadingWifiWait)?.value
                ?: _protobufPending(5, { CarServer_Void() }) { this._protobufStore_type(CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.downloadingWifiWait(it)) }
            set(value) {
                this.type = CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.downloadingWifiWait(value)
            }

        var downloading: CarServer_Void
            get() = (this._type as? CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.downloading)?.value
                ?: _protobufPending(6, { CarServer_Void() }) { this._protobufStore_type(CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.downloading(it)) }
            set(value) {
                this.type = CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.downloading(value)
            }

        sealed class OneOf_Type(value: Any) : ProtobufOneofCase(value) {
            class unknown(val value: CarServer_Void) : OneOf_Type(value)
            class installing(val value: CarServer_Void) : OneOf_Type(value)
            class scheduled(val value: CarServer_Void) : OneOf_Type(value)
            class available(val value: CarServer_Void) : OneOf_Type(value)
            class downloadingWifiWait(val value: CarServer_Void) : OneOf_Type(value)
            class downloading(val value: CarServer_Void) : OneOf_Type(value)
        }

        private var _type: CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type? = null

        private fun _protobufStore_type(value: CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type?) {
            _protobufDropPending(1)
            _protobufDropPending(2)
            _protobufDropPending(3)
            _protobufDropPending(4)
            _protobufDropPending(5)
            _protobufDropPending(6)
            this._type = value
        }

        private fun _protobufCopy_type(value: CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type?): CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type? =
            when (value) {
                is CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.unknown -> CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.unknown(value.value.copy())
                is CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.installing -> CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.installing(value.value.copy())
                is CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.scheduled -> CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.scheduled(value.value.copy())
                is CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.available -> CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.available(value.value.copy())
                is CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.downloadingWifiWait -> CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.downloadingWifiWait(value.value.copy())
                is CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.downloading -> CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.downloading(value.value.copy())
                else -> value
            }

        override fun decodeMessage(decoder: BinaryDecoder) {
            while (true) {
                when (decoder.nextFieldNumber() ?: return) {
                    1 -> decoder.decodeSingularMessageField((this._type as? CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.unknown)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.unknown(it)) }
                    2 -> decoder.decodeSingularMessageField((this._type as? CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.installing)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.installing(it)) }
                    3 -> decoder.decodeSingularMessageField((this._type as? CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.scheduled)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.scheduled(it)) }
                    4 -> decoder.decodeSingularMessageField((this._type as? CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.available)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.available(it)) }
                    5 -> decoder.decodeSingularMessageField((this._type as? CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.downloadingWifiWait)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.downloadingWifiWait(it)) }
                    6 -> decoder.decodeSingularMessageField((this._type as? CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.downloading)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.downloading(it)) }
                }
            }
        }

        override fun traverse(visitor: BinaryEncodingVisitor) {
            (this._type as? CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.unknown)?.let {
                visitor.visitSingularMessageField(it.value, 1)
            }
            (this._type as? CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.installing)?.let {
                visitor.visitSingularMessageField(it.value, 2)
            }
            (this._type as? CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.scheduled)?.let {
                visitor.visitSingularMessageField(it.value, 3)
            }
            (this._type as? CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.available)?.let {
                visitor.visitSingularMessageField(it.value, 4)
            }
            (this._type as? CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.downloadingWifiWait)?.let {
                visitor.visitSingularMessageField(it.value, 5)
            }
            (this._type as? CarServer_SoftwareUpdateState.SoftwareUpdateStatus.OneOf_Type.downloading)?.let {
                visitor.visitSingularMessageField(it.value, 6)
            }
            visitor.visitUnknown(unknownFields)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CarServer_SoftwareUpdateState.SoftwareUpdateStatus) return false
            if (this._type != other._type) return false
            return this.unknownFields.contentEquals(other.unknownFields)
        }

        override fun hashCode(): Int {
            var hash = 0
            hash = 31 * hash + (this._type?.hashCode() ?: 0)
            hash = 31 * hash + this.unknownFields.contentHashCode()
            return hash
        }

        override fun copy(): CarServer_SoftwareUpdateState.SoftwareUpdateStatus {
            val result = CarServer_SoftwareUpdateState.SoftwareUpdateStatus()
            result._type = this._protobufCopy_type(this._type)
            result.unknownFields = this.unknownFields
            return result
        }

        companion object {
            const val protoMessageName: String = "CarServer.SoftwareUpdateState.SoftwareUpdateStatus"

            fun with(block: CarServer_SoftwareUpdateState.SoftwareUpdateStatus.() -> Unit): CarServer_SoftwareUpdateState.SoftwareUpdateStatus =
                CarServer_SoftwareUpdateState.SoftwareUpdateStatus().apply(block)
        }
    }

    private var _status: CarServer_SoftwareUpdateState.SoftwareUpdateStatus? = null
    private var _optionalScheduledTimeMs: CarServer_SoftwareUpdateState.OneOf_OptionalScheduledTimeMs? = null
    private var _optionalWarningTimeRemainingMs: CarServer_SoftwareUpdateState.OneOf_OptionalWarningTimeRemainingMs? = null
    private var _optionalExpectedDurationSec: CarServer_SoftwareUpdateState.OneOf_OptionalExpectedDurationSec? = null
    private var _optionalDownloadPerc: CarServer_SoftwareUpdateState.OneOf_OptionalDownloadPerc? = null
    private var _optionalInstallPerc: CarServer_SoftwareUpdateState.OneOf_OptionalInstallPerc? = null
    private var _optionalVersion: CarServer_SoftwareUpdateState.OneOf_OptionalVersion? = null
    private var _timestamp: Google_Protobuf_Timestamp? = null

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularMessageField(this._status) { CarServer_SoftwareUpdateState.SoftwareUpdateStatus() }?.let {
                    _protobufDropPending(1)
                    this._status = it
                }
                102 -> decoder.decodeSingularUInt64Field()?.let { this._optionalScheduledTimeMs = CarServer_SoftwareUpdateState.OneOf_OptionalScheduledTimeMs.scheduledTimeMs(it) }
                103 -> decoder.decodeSingularUInt64Field()?.let { this._optionalWarningTimeRemainingMs = CarServer_SoftwareUpdateState.OneOf_OptionalWarningTimeRemainingMs.warningTimeRemainingMs(it) }
                104 -> decoder.decodeSingularUInt32Field()?.let { this._optionalExpectedDurationSec = CarServer_SoftwareUpdateState.OneOf_OptionalExpectedDurationSec.expectedDurationSec(it) }
                105 -> decoder.decodeSingularUInt32Field()?.let { this._optionalDownloadPerc = CarServer_SoftwareUpdateState.OneOf_OptionalDownloadPerc.downloadPerc(it) }
                106 -> decoder.decodeSingularUInt32Field()?.let { this._optionalInstallPerc = CarServer_SoftwareUpdateState.OneOf_OptionalInstallPerc.installPerc(it) }
                107 -> decoder.decodeSingularStringField()?.let { this._optionalVersion = CarServer_SoftwareUpdateState.OneOf_OptionalVersion.version(it) }
                108 -> decoder.decodeSingularMessageField(this._timestamp) { Google_Protobuf_Timestamp() }?.let {
                    _protobufDropPending(108)
                    this._timestamp = it
                }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        this._status?.let {
            visitor.visitSingularMessageField(it, 1)
        }
        (this._optionalScheduledTimeMs as? CarServer_SoftwareUpdateState.OneOf_OptionalScheduledTimeMs.scheduledTimeMs)?.let {
            visitor.visitSingularUInt64Field(it.value, 102)
        }
        (this._optionalWarningTimeRemainingMs as? CarServer_SoftwareUpdateState.OneOf_OptionalWarningTimeRemainingMs.warningTimeRemainingMs)?.let {
            visitor.visitSingularUInt64Field(it.value, 103)
        }
        (this._optionalExpectedDurationSec as? CarServer_SoftwareUpdateState.OneOf_OptionalExpectedDurationSec.expectedDurationSec)?.let {
            visitor.visitSingularUInt32Field(it.value, 104)
        }
        (this._optionalDownloadPerc as? CarServer_SoftwareUpdateState.OneOf_OptionalDownloadPerc.downloadPerc)?.let {
            visitor.visitSingularUInt32Field(it.value, 105)
        }
        (this._optionalInstallPerc as? CarServer_SoftwareUpdateState.OneOf_OptionalInstallPerc.installPerc)?.let {
            visitor.visitSingularUInt32Field(it.value, 106)
        }
        (this._optionalVersion as? CarServer_SoftwareUpdateState.OneOf_OptionalVersion.version)?.let {
            visitor.visitSingularStringField(it.value, 107)
        }
        this._timestamp?.let {
            visitor.visitSingularMessageField(it, 108)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_SoftwareUpdateState) return false
        if (this._status != other._status) return false
        if (this._optionalScheduledTimeMs != other._optionalScheduledTimeMs) return false
        if (this._optionalWarningTimeRemainingMs != other._optionalWarningTimeRemainingMs) return false
        if (this._optionalExpectedDurationSec != other._optionalExpectedDurationSec) return false
        if (this._optionalDownloadPerc != other._optionalDownloadPerc) return false
        if (this._optionalInstallPerc != other._optionalInstallPerc) return false
        if (this._optionalVersion != other._optionalVersion) return false
        if (this._timestamp != other._timestamp) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._status?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalScheduledTimeMs?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalWarningTimeRemainingMs?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalExpectedDurationSec?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalDownloadPerc?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalInstallPerc?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalVersion?.hashCode() ?: 0)
        hash = 31 * hash + (this._timestamp?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_SoftwareUpdateState {
        val result = CarServer_SoftwareUpdateState()
        result._status = this._status?.copy()
        result._optionalScheduledTimeMs = this._optionalScheduledTimeMs
        result._optionalWarningTimeRemainingMs = this._optionalWarningTimeRemainingMs
        result._optionalExpectedDurationSec = this._optionalExpectedDurationSec
        result._optionalDownloadPerc = this._optionalDownloadPerc
        result._optionalInstallPerc = this._optionalInstallPerc
        result._optionalVersion = this._optionalVersion
        result._timestamp = this._timestamp?.copy()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.SoftwareUpdateState"

        fun with(block: CarServer_SoftwareUpdateState.() -> Unit): CarServer_SoftwareUpdateState =
            CarServer_SoftwareUpdateState().apply(block)
    }
}

class CarServer_DriveState() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var shiftState: CarServer_ShiftState
        get() = this._shiftState ?: _protobufPending(1, { CarServer_ShiftState() }) { this._shiftState = it }
        set(value) {
            _protobufDropPending(1)
            this._shiftState = value.copy()
            _protobufMutated()
        }

    val hasShiftState: Boolean
        get() = this._shiftState != null

    fun clearShiftState() {
        _protobufDropPending(1)
        this._shiftState = null
        _protobufMutated()
    }

    var optionalSpeed: CarServer_DriveState.OneOf_OptionalSpeed?
        get() = this._optionalSpeed
        set(value) {
            this._optionalSpeed = value
            _protobufMutated()
        }

    var speed: UInt
        get() = (this._optionalSpeed as? CarServer_DriveState.OneOf_OptionalSpeed.speed)?.value ?: 0u
        set(value) {
            this.optionalSpeed = CarServer_DriveState.OneOf_OptionalSpeed.speed(value)
        }

    var optionalPower: CarServer_DriveState.OneOf_OptionalPower?
        get() = this._optionalPower
        set(value) {
            this._optionalPower = value
            _protobufMutated()
        }

    var power: Int
        get() = (this._optionalPower as? CarServer_DriveState.OneOf_OptionalPower.power)?.value ?: 0
        set(value) {
            this.optionalPower = CarServer_DriveState.OneOf_OptionalPower.power(value)
        }

    var timestamp: Google_Protobuf_Timestamp
        get() = this._timestamp ?: _protobufPending(4, { Google_Protobuf_Timestamp() }) { this._timestamp = it }
        set(value) {
            _protobufDropPending(4)
            this._timestamp = value.copy()
            _protobufMutated()
        }

    val hasTimestamp: Boolean
        get() = this._timestamp != null

    fun clearTimestamp() {
        _protobufDropPending(4)
        this._timestamp = null
        _protobufMutated()
    }

    var optionalOdometerInHundredthsOfAMile: CarServer_DriveState.OneOf_OptionalOdometerInHundredthsOfAMile?
        get() = this._optionalOdometerInHundredthsOfAMile
        set(value) {
            this._optionalOdometerInHundredthsOfAMile = value
            _protobufMutated()
        }

    var odometerInHundredthsOfAMile: Int
        get() = (this._optionalOdometerInHundredthsOfAMile as? CarServer_DriveState.OneOf_OptionalOdometerInHundredthsOfAMile.odometerInHundredthsOfAMile)?.value ?: 0
        set(value) {
            this.optionalOdometerInHundredthsOfAMile = CarServer_DriveState.OneOf_OptionalOdometerInHundredthsOfAMile.odometerInHundredthsOfAMile(value)
        }

    var optionalSpeedFloat: CarServer_DriveState.OneOf_OptionalSpeedFloat?
        get() = this._optionalSpeedFloat
        set(value) {
            this._optionalSpeedFloat = value
            _protobufMutated()
        }

    var speedFloat: Float
        get() = (this._optionalSpeedFloat as? CarServer_DriveState.OneOf_OptionalSpeedFloat.speedFloat)?.value ?: 0f
        set(value) {
            this.optionalSpeedFloat = CarServer_DriveState.OneOf_OptionalSpeedFloat.speedFloat(value)
        }

    var optionalActiveRouteDestination: CarServer_DriveState.OneOf_OptionalActiveRouteDestination?
        get() = this._optionalActiveRouteDestination
        set(value) {
            this._optionalActiveRouteDestination = value
            _protobufMutated()
        }

    var activeRouteDestination: String
        get() = (this._optionalActiveRouteDestination as? CarServer_DriveState.OneOf_OptionalActiveRouteDestination.activeRouteDestination)?.value ?: ""
        set(value) {
            this.optionalActiveRouteDestination = CarServer_DriveState.OneOf_OptionalActiveRouteDestination.activeRouteDestination(value)
        }

    var optionalActiveRouteMinutesToArrival: CarServer_DriveState.OneOf_OptionalActiveRouteMinutesToArrival?
        get() = this._optionalActiveRouteMinutesToArrival
        set(value) {
            this._optionalActiveRouteMinutesToArrival = value
            _protobufMutated()
        }

    var activeRouteMinutesToArrival: Float
        get() = (this._optionalActiveRouteMinutesToArrival as? CarServer_DriveState.OneOf_OptionalActiveRouteMinutesToArrival.activeRouteMinutesToArrival)?.value ?: 0f
        set(value) {
            this.optionalActiveRouteMinutesToArrival = CarServer_DriveState.OneOf_OptionalActiveRouteMinutesToArrival.activeRouteMinutesToArrival(value)
        }

    var optionalActiveRouteMilesToArrival: CarServer_DriveState.OneOf_OptionalActiveRouteMilesToArrival?
        get() = this._optionalActiveRouteMilesToArrival
        set(value) {
            this._optionalActiveRouteMilesToArrival = value
            _protobufMutated()
        }

    var activeRouteMilesToArrival: Float
        get() = (this._optionalActiveRouteMilesToArrival as? CarServer_DriveState.OneOf_OptionalActiveRouteMilesToArrival.activeRouteMilesToArrival)?.value ?: 0f
        set(value) {
            this.optionalActiveRouteMilesToArrival = CarServer_DriveState.OneOf_OptionalActiveRouteMilesToArrival.activeRouteMilesToArrival(value)
        }

    var optionalActiveRouteTrafficMinutesDelay: CarServer_DriveState.OneOf_OptionalActiveRouteTrafficMinutesDelay?
        get() = this._optionalActiveRouteTrafficMinutesDelay
        set(value) {
            this._optionalActiveRouteTrafficMinutesDelay = value
            _protobufMutated()
        }

    var activeRouteTrafficMinutesDelay: Float
        get() = (this._optionalActiveRouteTrafficMinutesDelay as? CarServer_DriveState.OneOf_OptionalActiveRouteTrafficMinutesDelay.activeRouteTrafficMinutesDelay)?.value ?: 0f
        set(value) {
            this.optionalActiveRouteTrafficMinutesDelay = CarServer_DriveState.OneOf_OptionalActiveRouteTrafficMinutesDelay.activeRouteTrafficMinutesDelay(value)
        }

    var optionalActiveRouteEnergyAtArrival: CarServer_DriveState.OneOf_OptionalActiveRouteEnergyAtArrival?
        get() = this._optionalActiveRouteEnergyAtArrival
        set(value) {
            this._optionalActiveRouteEnergyAtArrival = value
            _protobufMutated()
        }

    var activeRouteEnergyAtArrival: Float
        get() = (this._optionalActiveRouteEnergyAtArrival as? CarServer_DriveState.OneOf_OptionalActiveRouteEnergyAtArrival.activeRouteEnergyAtArrival)?.value ?: 0f
        set(value) {
            this.optionalActiveRouteEnergyAtArrival = CarServer_DriveState.OneOf_OptionalActiveRouteEnergyAtArrival.activeRouteEnergyAtArrival(value)
        }

    var optionalLastRouteUpdate: CarServer_DriveState.OneOf_OptionalLastRouteUpdate?
        get() = this._optionalLastRouteUpdate
        set(value) {
            this._optionalLastRouteUpdate = value
            _protobufMutated()
        }

    var lastRouteUpdate: UInt
        get() = (this._optionalLastRouteUpdate as? CarServer_DriveState.OneOf_OptionalLastRouteUpdate.lastRouteUpdate)?.value ?: 0u
        set(value) {
            this.optionalLastRouteUpdate = CarServer_DriveState.OneOf_OptionalLastRouteUpdate.lastRouteUpdate(value)
        }

    var lastTrafficUpdate: Google_Protobuf_Timestamp
        get() = this._lastTrafficUpdate ?: _protobufPending(15, { Google_Protobuf_Timestamp() }) { this._lastTrafficUpdate = it }
        set(value) {
            _protobufDropPending(15)
            this._lastTrafficUpdate = value.copy()
            _protobufMutated()
        }

    val hasLastTrafficUpdate: Boolean
        get() = this._lastTrafficUpdate != null

    fun clearLastTrafficUpdate() {
        _protobufDropPending(15)
        this._lastTrafficUpdate = null
        _protobufMutated()
    }

    var activeRouteCoordinates: CarServer_LatLong
        get() = this._activeRouteCoordinates ?: _protobufPending(12, { CarServer_LatLong() }) { this._activeRouteCoordinates = it }
        set(value) {
            _protobufDropPending(12)
            this._activeRouteCoordinates = value.copy()
            _protobufMutated()
        }

    val hasActiveRouteCoordinates: Boolean
        get() = this._activeRouteCoordinates != null

    fun clearActiveRouteCoordinates() {
        _protobufDropPending(12)
        this._activeRouteCoordinates = null
        _protobufMutated()
    }

    sealed class OneOf_OptionalSpeed(value: Any) : ProtobufOneofCase(value) {
        class speed(val value: UInt) : OneOf_OptionalSpeed(value)
    }

    sealed class OneOf_OptionalPower(value: Any) : ProtobufOneofCase(value) {
        class power(val value: Int) : OneOf_OptionalPower(value)
    }

    sealed class OneOf_OptionalOdometerInHundredthsOfAMile(value: Any) : ProtobufOneofCase(value) {
        class odometerInHundredthsOfAMile(val value: Int) : OneOf_OptionalOdometerInHundredthsOfAMile(value)
    }

    sealed class OneOf_OptionalSpeedFloat(value: Any) : ProtobufOneofCase(value) {
        class speedFloat(val value: Float) : OneOf_OptionalSpeedFloat(value)
    }

    sealed class OneOf_OptionalActiveRouteDestination(value: Any) : ProtobufOneofCase(value) {
        class activeRouteDestination(val value: String) : OneOf_OptionalActiveRouteDestination(value)
    }

    sealed class OneOf_OptionalActiveRouteMinutesToArrival(value: Any) : ProtobufOneofCase(value) {
        class activeRouteMinutesToArrival(val value: Float) : OneOf_OptionalActiveRouteMinutesToArrival(value)
    }

    sealed class OneOf_OptionalActiveRouteMilesToArrival(value: Any) : ProtobufOneofCase(value) {
        class activeRouteMilesToArrival(val value: Float) : OneOf_OptionalActiveRouteMilesToArrival(value)
    }

    sealed class OneOf_OptionalActiveRouteTrafficMinutesDelay(value: Any) : ProtobufOneofCase(value) {
        class activeRouteTrafficMinutesDelay(val value: Float) : OneOf_OptionalActiveRouteTrafficMinutesDelay(value)
    }

    sealed class OneOf_OptionalActiveRouteEnergyAtArrival(value: Any) : ProtobufOneofCase(value) {
        class activeRouteEnergyAtArrival(val value: Float) : OneOf_OptionalActiveRouteEnergyAtArrival(value)
    }

    sealed class OneOf_OptionalLastRouteUpdate(value: Any) : ProtobufOneofCase(value) {
        class lastRouteUpdate(val value: UInt) : OneOf_OptionalLastRouteUpdate(value)
    }

    private var _shiftState: CarServer_ShiftState? = null
    private var _optionalSpeed: CarServer_DriveState.OneOf_OptionalSpeed? = null
    private var _optionalPower: CarServer_DriveState.OneOf_OptionalPower? = null
    private var _timestamp: Google_Protobuf_Timestamp? = null
    private var _optionalOdometerInHundredthsOfAMile: CarServer_DriveState.OneOf_OptionalOdometerInHundredthsOfAMile? = null
    private var _optionalSpeedFloat: CarServer_DriveState.OneOf_OptionalSpeedFloat? = null
    private var _optionalActiveRouteDestination: CarServer_DriveState.OneOf_OptionalActiveRouteDestination? = null
    private var _optionalActiveRouteMinutesToArrival: CarServer_DriveState.OneOf_OptionalActiveRouteMinutesToArrival? = null
    private var _optionalActiveRouteMilesToArrival: CarServer_DriveState.OneOf_OptionalActiveRouteMilesToArrival? = null
    private var _optionalActiveRouteTrafficMinutesDelay: CarServer_DriveState.OneOf_OptionalActiveRouteTrafficMinutesDelay? = null
    private var _optionalActiveRouteEnergyAtArrival: CarServer_DriveState.OneOf_OptionalActiveRouteEnergyAtArrival? = null
    private var _optionalLastRouteUpdate: CarServer_DriveState.OneOf_OptionalLastRouteUpdate? = null
    private var _lastTrafficUpdate: Google_Protobuf_Timestamp? = null
    private var _activeRouteCoordinates: CarServer_LatLong? = null

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularMessageField(this._shiftState) { CarServer_ShiftState() }?.let {
                    _protobufDropPending(1)
                    this._shiftState = it
                }
                4 -> decoder.decodeSingularMessageField(this._timestamp) { Google_Protobuf_Timestamp() }?.let {
                    _protobufDropPending(4)
                    this._timestamp = it
                }
                7 -> decoder.decodeSingularStringField()?.let { this._optionalActiveRouteDestination = CarServer_DriveState.OneOf_OptionalActiveRouteDestination.activeRouteDestination(it) }
                8 -> decoder.decodeSingularFloatField()?.let { this._optionalActiveRouteMinutesToArrival = CarServer_DriveState.OneOf_OptionalActiveRouteMinutesToArrival.activeRouteMinutesToArrival(it) }
                9 -> decoder.decodeSingularFloatField()?.let { this._optionalActiveRouteMilesToArrival = CarServer_DriveState.OneOf_OptionalActiveRouteMilesToArrival.activeRouteMilesToArrival(it) }
                10 -> decoder.decodeSingularFloatField()?.let { this._optionalActiveRouteTrafficMinutesDelay = CarServer_DriveState.OneOf_OptionalActiveRouteTrafficMinutesDelay.activeRouteTrafficMinutesDelay(it) }
                11 -> decoder.decodeSingularFloatField()?.let { this._optionalActiveRouteEnergyAtArrival = CarServer_DriveState.OneOf_OptionalActiveRouteEnergyAtArrival.activeRouteEnergyAtArrival(it) }
                12 -> decoder.decodeSingularMessageField(this._activeRouteCoordinates) { CarServer_LatLong() }?.let {
                    _protobufDropPending(12)
                    this._activeRouteCoordinates = it
                }
                14 -> decoder.decodeSingularUInt32Field()?.let { this._optionalLastRouteUpdate = CarServer_DriveState.OneOf_OptionalLastRouteUpdate.lastRouteUpdate(it) }
                15 -> decoder.decodeSingularMessageField(this._lastTrafficUpdate) { Google_Protobuf_Timestamp() }?.let {
                    _protobufDropPending(15)
                    this._lastTrafficUpdate = it
                }
                102 -> decoder.decodeSingularUInt32Field()?.let { this._optionalSpeed = CarServer_DriveState.OneOf_OptionalSpeed.speed(it) }
                103 -> decoder.decodeSingularInt32Field()?.let { this._optionalPower = CarServer_DriveState.OneOf_OptionalPower.power(it) }
                105 -> decoder.decodeSingularInt32Field()?.let { this._optionalOdometerInHundredthsOfAMile = CarServer_DriveState.OneOf_OptionalOdometerInHundredthsOfAMile.odometerInHundredthsOfAMile(it) }
                106 -> decoder.decodeSingularFloatField()?.let { this._optionalSpeedFloat = CarServer_DriveState.OneOf_OptionalSpeedFloat.speedFloat(it) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        this._shiftState?.let {
            visitor.visitSingularMessageField(it, 1)
        }
        this._timestamp?.let {
            visitor.visitSingularMessageField(it, 4)
        }
        (this._optionalActiveRouteDestination as? CarServer_DriveState.OneOf_OptionalActiveRouteDestination.activeRouteDestination)?.let {
            visitor.visitSingularStringField(it.value, 7)
        }
        (this._optionalActiveRouteMinutesToArrival as? CarServer_DriveState.OneOf_OptionalActiveRouteMinutesToArrival.activeRouteMinutesToArrival)?.let {
            visitor.visitSingularFloatField(it.value, 8)
        }
        (this._optionalActiveRouteMilesToArrival as? CarServer_DriveState.OneOf_OptionalActiveRouteMilesToArrival.activeRouteMilesToArrival)?.let {
            visitor.visitSingularFloatField(it.value, 9)
        }
        (this._optionalActiveRouteTrafficMinutesDelay as? CarServer_DriveState.OneOf_OptionalActiveRouteTrafficMinutesDelay.activeRouteTrafficMinutesDelay)?.let {
            visitor.visitSingularFloatField(it.value, 10)
        }
        (this._optionalActiveRouteEnergyAtArrival as? CarServer_DriveState.OneOf_OptionalActiveRouteEnergyAtArrival.activeRouteEnergyAtArrival)?.let {
            visitor.visitSingularFloatField(it.value, 11)
        }
        this._activeRouteCoordinates?.let {
            visitor.visitSingularMessageField(it, 12)
        }
        (this._optionalLastRouteUpdate as? CarServer_DriveState.OneOf_OptionalLastRouteUpdate.lastRouteUpdate)?.let {
            visitor.visitSingularUInt32Field(it.value, 14)
        }
        this._lastTrafficUpdate?.let {
            visitor.visitSingularMessageField(it, 15)
        }
        (this._optionalSpeed as? CarServer_DriveState.OneOf_OptionalSpeed.speed)?.let {
            visitor.visitSingularUInt32Field(it.value, 102)
        }
        (this._optionalPower as? CarServer_DriveState.OneOf_OptionalPower.power)?.let {
            visitor.visitSingularInt32Field(it.value, 103)
        }
        (this._optionalOdometerInHundredthsOfAMile as? CarServer_DriveState.OneOf_OptionalOdometerInHundredthsOfAMile.odometerInHundredthsOfAMile)?.let {
            visitor.visitSingularInt32Field(it.value, 105)
        }
        (this._optionalSpeedFloat as? CarServer_DriveState.OneOf_OptionalSpeedFloat.speedFloat)?.let {
            visitor.visitSingularFloatField(it.value, 106)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_DriveState) return false
        if (this._shiftState != other._shiftState) return false
        if (this._optionalSpeed != other._optionalSpeed) return false
        if (this._optionalPower != other._optionalPower) return false
        if (this._timestamp != other._timestamp) return false
        if (this._optionalOdometerInHundredthsOfAMile != other._optionalOdometerInHundredthsOfAMile) return false
        if (this._optionalSpeedFloat != other._optionalSpeedFloat) return false
        if (this._optionalActiveRouteDestination != other._optionalActiveRouteDestination) return false
        if (this._optionalActiveRouteMinutesToArrival != other._optionalActiveRouteMinutesToArrival) return false
        if (this._optionalActiveRouteMilesToArrival != other._optionalActiveRouteMilesToArrival) return false
        if (this._optionalActiveRouteTrafficMinutesDelay != other._optionalActiveRouteTrafficMinutesDelay) return false
        if (this._optionalActiveRouteEnergyAtArrival != other._optionalActiveRouteEnergyAtArrival) return false
        if (this._optionalLastRouteUpdate != other._optionalLastRouteUpdate) return false
        if (this._lastTrafficUpdate != other._lastTrafficUpdate) return false
        if (this._activeRouteCoordinates != other._activeRouteCoordinates) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._shiftState?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalSpeed?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalPower?.hashCode() ?: 0)
        hash = 31 * hash + (this._timestamp?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalOdometerInHundredthsOfAMile?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalSpeedFloat?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalActiveRouteDestination?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalActiveRouteMinutesToArrival?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalActiveRouteMilesToArrival?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalActiveRouteTrafficMinutesDelay?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalActiveRouteEnergyAtArrival?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalLastRouteUpdate?.hashCode() ?: 0)
        hash = 31 * hash + (this._lastTrafficUpdate?.hashCode() ?: 0)
        hash = 31 * hash + (this._activeRouteCoordinates?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_DriveState {
        val result = CarServer_DriveState()
        result._shiftState = this._shiftState?.copy()
        result._optionalSpeed = this._optionalSpeed
        result._optionalPower = this._optionalPower
        result._timestamp = this._timestamp?.copy()
        result._optionalOdometerInHundredthsOfAMile = this._optionalOdometerInHundredthsOfAMile
        result._optionalSpeedFloat = this._optionalSpeedFloat
        result._optionalActiveRouteDestination = this._optionalActiveRouteDestination
        result._optionalActiveRouteMinutesToArrival = this._optionalActiveRouteMinutesToArrival
        result._optionalActiveRouteMilesToArrival = this._optionalActiveRouteMilesToArrival
        result._optionalActiveRouteTrafficMinutesDelay = this._optionalActiveRouteTrafficMinutesDelay
        result._optionalActiveRouteEnergyAtArrival = this._optionalActiveRouteEnergyAtArrival
        result._optionalLastRouteUpdate = this._optionalLastRouteUpdate
        result._lastTrafficUpdate = this._lastTrafficUpdate?.copy()
        result._activeRouteCoordinates = this._activeRouteCoordinates?.copy()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.DriveState"

        fun with(block: CarServer_DriveState.() -> Unit): CarServer_DriveState =
            CarServer_DriveState().apply(block)
    }
}

class CarServer_ChargeState() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var chargingState: CarServer_ChargeState.ChargingState
        get() = this._chargingState ?: _protobufPending(1, { CarServer_ChargeState.ChargingState() }) { this._chargingState = it }
        set(value) {
            _protobufDropPending(1)
            this._chargingState = value.copy()
            _protobufMutated()
        }

    val hasChargingState: Boolean
        get() = this._chargingState != null

    fun clearChargingState() {
        _protobufDropPending(1)
        this._chargingState = null
        _protobufMutated()
    }

    var fastChargerType: CarServer_ChargeState.ChargerType
        get() = this._fastChargerType ?: _protobufPending(2, { CarServer_ChargeState.ChargerType() }) { this._fastChargerType = it }
        set(value) {
            _protobufDropPending(2)
            this._fastChargerType = value.copy()
            _protobufMutated()
        }

    val hasFastChargerType: Boolean
        get() = this._fastChargerType != null

    fun clearFastChargerType() {
        _protobufDropPending(2)
        this._fastChargerType = null
        _protobufMutated()
    }

    var fastChargerBrand: CarServer_ChargeState.ChargerBrand
        get() = this._fastChargerBrand ?: _protobufPending(3, { CarServer_ChargeState.ChargerBrand() }) { this._fastChargerBrand = it }
        set(value) {
            _protobufDropPending(3)
            this._fastChargerBrand = value.copy()
            _protobufMutated()
        }

    val hasFastChargerBrand: Boolean
        get() = this._fastChargerBrand != null

    fun clearFastChargerBrand() {
        _protobufDropPending(3)
        this._fastChargerBrand = null
        _protobufMutated()
    }

    var optionalChargeLimitSoc: CarServer_ChargeState.OneOf_OptionalChargeLimitSoc?
        get() = this._optionalChargeLimitSoc
        set(value) {
            this._optionalChargeLimitSoc = value
            _protobufMutated()
        }

    var chargeLimitSoc: Int
        get() = (this._optionalChargeLimitSoc as? CarServer_ChargeState.OneOf_OptionalChargeLimitSoc.chargeLimitSoc)?.value ?: 0
        set(value) {
            this.optionalChargeLimitSoc = CarServer_ChargeState.OneOf_OptionalChargeLimitSoc.chargeLimitSoc(value)
        }

    var optionalChargeLimitSocStd: CarServer_ChargeState.OneOf_OptionalChargeLimitSocStd?
        get() = this._optionalChargeLimitSocStd
        set(value) {
            this._optionalChargeLimitSocStd = value
            _protobufMutated()
        }

    var chargeLimitSocStd: Int
        get() = (this._optionalChargeLimitSocStd as? CarServer_ChargeState.OneOf_OptionalChargeLimitSocStd.chargeLimitSocStd)?.value ?: 0
        set(value) {
            this.optionalChargeLimitSocStd = CarServer_ChargeState.OneOf_OptionalChargeLimitSocStd.chargeLimitSocStd(value)
        }

    var optionalChargeLimitSocMin: CarServer_ChargeState.OneOf_OptionalChargeLimitSocMin?
        get() = this._optionalChargeLimitSocMin
        set(value) {
            this._optionalChargeLimitSocMin = value
            _protobufMutated()
        }

    var chargeLimitSocMin: Int
        get() = (this._optionalChargeLimitSocMin as? CarServer_ChargeState.OneOf_OptionalChargeLimitSocMin.chargeLimitSocMin)?.value ?: 0
        set(value) {
            this.optionalChargeLimitSocMin = CarServer_ChargeState.OneOf_OptionalChargeLimitSocMin.chargeLimitSocMin(value)
        }

    var optionalChargeLimitSocMax: CarServer_ChargeState.OneOf_OptionalChargeLimitSocMax?
        get() = this._optionalChargeLimitSocMax
        set(value) {
            this._optionalChargeLimitSocMax = value
            _protobufMutated()
        }

    var chargeLimitSocMax: Int
        get() = (this._optionalChargeLimitSocMax as? CarServer_ChargeState.OneOf_OptionalChargeLimitSocMax.chargeLimitSocMax)?.value ?: 0
        set(value) {
            this.optionalChargeLimitSocMax = CarServer_ChargeState.OneOf_OptionalChargeLimitSocMax.chargeLimitSocMax(value)
        }

    var optionalMaxRangeChargeCounter: CarServer_ChargeState.OneOf_OptionalMaxRangeChargeCounter?
        get() = this._optionalMaxRangeChargeCounter
        set(value) {
            this._optionalMaxRangeChargeCounter = value
            _protobufMutated()
        }

    var maxRangeChargeCounter: Int
        get() = (this._optionalMaxRangeChargeCounter as? CarServer_ChargeState.OneOf_OptionalMaxRangeChargeCounter.maxRangeChargeCounter)?.value ?: 0
        set(value) {
            this.optionalMaxRangeChargeCounter = CarServer_ChargeState.OneOf_OptionalMaxRangeChargeCounter.maxRangeChargeCounter(value)
        }

    var optionalFastChargerPresent: CarServer_ChargeState.OneOf_OptionalFastChargerPresent?
        get() = this._optionalFastChargerPresent
        set(value) {
            this._optionalFastChargerPresent = value
            _protobufMutated()
        }

    var fastChargerPresent: Boolean
        get() = (this._optionalFastChargerPresent as? CarServer_ChargeState.OneOf_OptionalFastChargerPresent.fastChargerPresent)?.value ?: false
        set(value) {
            this.optionalFastChargerPresent = CarServer_ChargeState.OneOf_OptionalFastChargerPresent.fastChargerPresent(value)
        }

    var optionalBatteryRange: CarServer_ChargeState.OneOf_OptionalBatteryRange?
        get() = this._optionalBatteryRange
        set(value) {
            this._optionalBatteryRange = value
            _protobufMutated()
        }

    var batteryRange: Float
        get() = (this._optionalBatteryRange as? CarServer_ChargeState.OneOf_OptionalBatteryRange.batteryRange)?.value ?: 0f
        set(value) {
            this.optionalBatteryRange = CarServer_ChargeState.OneOf_OptionalBatteryRange.batteryRange(value)
        }

    var optionalEstBatteryRange: CarServer_ChargeState.OneOf_OptionalEstBatteryRange?
        get() = this._optionalEstBatteryRange
        set(value) {
            this._optionalEstBatteryRange = value
            _protobufMutated()
        }

    var estBatteryRange: Float
        get() = (this._optionalEstBatteryRange as? CarServer_ChargeState.OneOf_OptionalEstBatteryRange.estBatteryRange)?.value ?: 0f
        set(value) {
            this.optionalEstBatteryRange = CarServer_ChargeState.OneOf_OptionalEstBatteryRange.estBatteryRange(value)
        }

    var optionalIdealBatteryRange: CarServer_ChargeState.OneOf_OptionalIdealBatteryRange?
        get() = this._optionalIdealBatteryRange
        set(value) {
            this._optionalIdealBatteryRange = value
            _protobufMutated()
        }

    var idealBatteryRange: Float
        get() = (this._optionalIdealBatteryRange as? CarServer_ChargeState.OneOf_OptionalIdealBatteryRange.idealBatteryRange)?.value ?: 0f
        set(value) {
            this.optionalIdealBatteryRange = CarServer_ChargeState.OneOf_OptionalIdealBatteryRange.idealBatteryRange(value)
        }

    var optionalBatteryLevel: CarServer_ChargeState.OneOf_OptionalBatteryLevel?
        get() = this._optionalBatteryLevel
        set(value) {
            this._optionalBatteryLevel = value
            _protobufMutated()
        }

    var batteryLevel: Int
        get() = (this._optionalBatteryLevel as? CarServer_ChargeState.OneOf_OptionalBatteryLevel.batteryLevel)?.value ?: 0
        set(value) {
            this.optionalBatteryLevel = CarServer_ChargeState.OneOf_OptionalBatteryLevel.batteryLevel(value)
        }

    var optionalUsableBatteryLevel: CarServer_ChargeState.OneOf_OptionalUsableBatteryLevel?
        get() = this._optionalUsableBatteryLevel
        set(value) {
            this._optionalUsableBatteryLevel = value
            _protobufMutated()
        }

    var usableBatteryLevel: Int
        get() = (this._optionalUsableBatteryLevel as? CarServer_ChargeState.OneOf_OptionalUsableBatteryLevel.usableBatteryLevel)?.value ?: 0
        set(value) {
            this.optionalUsableBatteryLevel = CarServer_ChargeState.OneOf_OptionalUsableBatteryLevel.usableBatteryLevel(value)
        }

    var optionalChargeEnergyAdded: CarServer_ChargeState.OneOf_OptionalChargeEnergyAdded?
        get() = this._optionalChargeEnergyAdded
        set(value) {
            this._optionalChargeEnergyAdded = value
            _protobufMutated()
        }

    var chargeEnergyAdded: Float
        get() = (this._optionalChargeEnergyAdded as? CarServer_ChargeState.OneOf_OptionalChargeEnergyAdded.chargeEnergyAdded)?.value ?: 0f
        set(value) {
            this.optionalChargeEnergyAdded = CarServer_ChargeState.OneOf_OptionalChargeEnergyAdded.chargeEnergyAdded(value)
        }

    var optionalChargeMilesAddedRated: CarServer_ChargeState.OneOf_OptionalChargeMilesAddedRated?
        get() = this._optionalChargeMilesAddedRated
        set(value) {
            this._optionalChargeMilesAddedRated = value
            _protobufMutated()
        }

    var chargeMilesAddedRated: Float
        get() = (this._optionalChargeMilesAddedRated as? CarServer_ChargeState.OneOf_OptionalChargeMilesAddedRated.chargeMilesAddedRated)?.value ?: 0f
        set(value) {
            this.optionalChargeMilesAddedRated = CarServer_ChargeState.OneOf_OptionalChargeMilesAddedRated.chargeMilesAddedRated(value)
        }

    var optionalChargeMilesAddedIdeal: CarServer_ChargeState.OneOf_OptionalChargeMilesAddedIdeal?
        get() = this._optionalChargeMilesAddedIdeal
        set(value) {
            this._optionalChargeMilesAddedIdeal = value
            _protobufMutated()
        }

    var chargeMilesAddedIdeal: Float
        get() = (this._optionalChargeMilesAddedIdeal as? CarServer_ChargeState.OneOf_OptionalChargeMilesAddedIdeal.chargeMilesAddedIdeal)?.value ?: 0f
        set(value) {
            this.optionalChargeMilesAddedIdeal = CarServer_ChargeState.OneOf_OptionalChargeMilesAddedIdeal.chargeMilesAddedIdeal(value)
        }

    var optionalChargerVoltage: CarServer_ChargeState.OneOf_OptionalChargerVoltage?
        get() = this._optionalChargerVoltage
        set(value) {
            this._optionalChargerVoltage = value
            _protobufMutated()
        }

    var chargerVoltage: Int
        get() = (this._optionalChargerVoltage as? CarServer_ChargeState.OneOf_OptionalChargerVoltage.chargerVoltage)?.value ?: 0
        set(value) {
            this.optionalChargerVoltage = CarServer_ChargeState.OneOf_OptionalChargerVoltage.chargerVoltage(value)
        }

    var optionalChargerPilotCurrent: CarServer_ChargeState.OneOf_OptionalChargerPilotCurrent?
        get() = this._optionalChargerPilotCurrent
        set(value) {
            this._optionalChargerPilotCurrent = value
            _protobufMutated()
        }

    var chargerPilotCurrent: Int
        get() = (this._optionalChargerPilotCurrent as? CarServer_ChargeState.OneOf_OptionalChargerPilotCurrent.chargerPilotCurrent)?.value ?: 0
        set(value) {
            this.optionalChargerPilotCurrent = CarServer_ChargeState.OneOf_OptionalChargerPilotCurrent.chargerPilotCurrent(value)
        }

    var optionalChargerActualCurrent: CarServer_ChargeState.OneOf_OptionalChargerActualCurrent?
        get() = this._optionalChargerActualCurrent
        set(value) {
            this._optionalChargerActualCurrent = value
            _protobufMutated()
        }

    var chargerActualCurrent: Int
        get() = (this._optionalChargerActualCurrent as? CarServer_ChargeState.OneOf_OptionalChargerActualCurrent.chargerActualCurrent)?.value ?: 0
        set(value) {
            this.optionalChargerActualCurrent = CarServer_ChargeState.OneOf_OptionalChargerActualCurrent.chargerActualCurrent(value)
        }

    var optionalChargerPower: CarServer_ChargeState.OneOf_OptionalChargerPower?
        get() = this._optionalChargerPower
        set(value) {
            this._optionalChargerPower = value
            _protobufMutated()
        }

    var chargerPower: Int
        get() = (this._optionalChargerPower as? CarServer_ChargeState.OneOf_OptionalChargerPower.chargerPower)?.value ?: 0
        set(value) {
            this.optionalChargerPower = CarServer_ChargeState.OneOf_OptionalChargerPower.chargerPower(value)
        }

    var optionalMinutesToFullCharge: CarServer_ChargeState.OneOf_OptionalMinutesToFullCharge?
        get() = this._optionalMinutesToFullCharge
        set(value) {
            this._optionalMinutesToFullCharge = value
            _protobufMutated()
        }

    var minutesToFullCharge: Int
        get() = (this._optionalMinutesToFullCharge as? CarServer_ChargeState.OneOf_OptionalMinutesToFullCharge.minutesToFullCharge)?.value ?: 0
        set(value) {
            this.optionalMinutesToFullCharge = CarServer_ChargeState.OneOf_OptionalMinutesToFullCharge.minutesToFullCharge(value)
        }

    var optionalMinutesToChargeLimit: CarServer_ChargeState.OneOf_OptionalMinutesToChargeLimit?
        get() = this._optionalMinutesToChargeLimit
        set(value) {
            this._optionalMinutesToChargeLimit = value
            _protobufMutated()
        }

    var minutesToChargeLimit: Int
        get() = (this._optionalMinutesToChargeLimit as? CarServer_ChargeState.OneOf_OptionalMinutesToChargeLimit.minutesToChargeLimit)?.value ?: 0
        set(value) {
            this.optionalMinutesToChargeLimit = CarServer_ChargeState.OneOf_OptionalMinutesToChargeLimit.minutesToChargeLimit(value)
        }

    var optionalTripCharging: CarServer_ChargeState.OneOf_OptionalTripCharging?
        get() = this._optionalTripCharging
        set(value) {
            this._optionalTripCharging = value
            _protobufMutated()
        }

    var tripCharging: Boolean
        get() = (this._optionalTripCharging as? CarServer_ChargeState.OneOf_OptionalTripCharging.tripCharging)?.value ?: false
        set(value) {
            this.optionalTripCharging = CarServer_ChargeState.OneOf_OptionalTripCharging.tripCharging(value)
        }

    var optionalChargeRateMph: CarServer_ChargeState.OneOf_OptionalChargeRateMph?
        get() = this._optionalChargeRateMph
        set(value) {
            this._optionalChargeRateMph = value
            _protobufMutated()
        }

    var chargeRateMph: Int
        get() = (this._optionalChargeRateMph as? CarServer_ChargeState.OneOf_OptionalChargeRateMph.chargeRateMph)?.value ?: 0
        set(value) {
            this.optionalChargeRateMph = CarServer_ChargeState.OneOf_OptionalChargeRateMph.chargeRateMph(value)
        }

    var optionalChargePortDoorOpen: CarServer_ChargeState.OneOf_OptionalChargePortDoorOpen?
        get() = this._optionalChargePortDoorOpen
        set(value) {
            this._optionalChargePortDoorOpen = value
            _protobufMutated()
        }

    var chargePortDoorOpen: Boolean
        get() = (this._optionalChargePortDoorOpen as? CarServer_ChargeState.OneOf_OptionalChargePortDoorOpen.chargePortDoorOpen)?.value ?: false
        set(value) {
            this.optionalChargePortDoorOpen = CarServer_ChargeState.OneOf_OptionalChargePortDoorOpen.chargePortDoorOpen(value)
        }

    var connChargeCable: CarServer_ChargeState.CableType
        get() = this._connChargeCable ?: _protobufPending(28, { CarServer_ChargeState.CableType() }) { this._connChargeCable = it }
        set(value) {
            _protobufDropPending(28)
            this._connChargeCable = value.copy()
            _protobufMutated()
        }

    val hasConnChargeCable: Boolean
        get() = this._connChargeCable != null

    fun clearConnChargeCable() {
        _protobufDropPending(28)
        this._connChargeCable = null
        _protobufMutated()
    }

    var optionalScheduledChargingStartTime: CarServer_ChargeState.OneOf_OptionalScheduledChargingStartTime?
        get() = this._optionalScheduledChargingStartTime
        set(value) {
            this._optionalScheduledChargingStartTime = value
            _protobufMutated()
        }

    var scheduledChargingStartTime: ULong
        get() = (this._optionalScheduledChargingStartTime as? CarServer_ChargeState.OneOf_OptionalScheduledChargingStartTime.scheduledChargingStartTime)?.value ?: 0uL
        set(value) {
            this.optionalScheduledChargingStartTime = CarServer_ChargeState.OneOf_OptionalScheduledChargingStartTime.scheduledChargingStartTime(value)
        }

    var optionalScheduledChargingPending: CarServer_ChargeState.OneOf_OptionalScheduledChargingPending?
        get() = this._optionalScheduledChargingPending
        set(value) {
            this._optionalScheduledChargingPending = value
            _protobufMutated()
        }

    var scheduledChargingPending: Boolean
        get() = (this._optionalScheduledChargingPending as? CarServer_ChargeState.OneOf_OptionalScheduledChargingPending.scheduledChargingPending)?.value ?: false
        set(value) {
            this.optionalScheduledChargingPending = CarServer_ChargeState.OneOf_OptionalScheduledChargingPending.scheduledChargingPending(value)
        }

    var scheduledDepartureTime: Google_Protobuf_Timestamp
        get() = this._scheduledDepartureTime ?: _protobufPending(31, { Google_Protobuf_Timestamp() }) { this._scheduledDepartureTime = it }
        set(value) {
            _protobufDropPending(31)
            this._scheduledDepartureTime = value.copy()
            _protobufMutated()
        }

    val hasScheduledDepartureTime: Boolean
        get() = this._scheduledDepartureTime != null

    fun clearScheduledDepartureTime() {
        _protobufDropPending(31)
        this._scheduledDepartureTime = null
        _protobufMutated()
    }

    var optionalUserChargeEnableRequest: CarServer_ChargeState.OneOf_OptionalUserChargeEnableRequest?
        get() = this._optionalUserChargeEnableRequest
        set(value) {
            this._optionalUserChargeEnableRequest = value
            _protobufMutated()
        }

    var userChargeEnableRequest: Boolean
        get() = (this._optionalUserChargeEnableRequest as? CarServer_ChargeState.OneOf_OptionalUserChargeEnableRequest.userChargeEnableRequest)?.value ?: false
        set(value) {
            this.optionalUserChargeEnableRequest = CarServer_ChargeState.OneOf_OptionalUserChargeEnableRequest.userChargeEnableRequest(value)
        }

    var optionalChargeEnableRequest: CarServer_ChargeState.OneOf_OptionalChargeEnableRequest?
        get() = this._optionalChargeEnableRequest
        set(value) {
            this._optionalChargeEnableRequest = value
            _protobufMutated()
        }

    var chargeEnableRequest: Boolean
        get() = (this._optionalChargeEnableRequest as? CarServer_ChargeState.OneOf_OptionalChargeEnableRequest.chargeEnableRequest)?.value ?: false
        set(value) {
            this.optionalChargeEnableRequest = CarServer_ChargeState.OneOf_OptionalChargeEnableRequest.chargeEnableRequest(value)
        }

    var optionalChargerPhases: CarServer_ChargeState.OneOf_OptionalChargerPhases?
        get() = this._optionalChargerPhases
        set(value) {
            this._optionalChargerPhases = value
            _protobufMutated()
        }

    var chargerPhases: Int
        get() = (this._optionalChargerPhases as? CarServer_ChargeState.OneOf_OptionalChargerPhases.chargerPhases)?.value ?: 0
        set(value) {
            this.optionalChargerPhases = CarServer_ChargeState.OneOf_OptionalChargerPhases.chargerPhases(value)
        }

    var chargePortLatch: CarServer_ChargePortLatchState
        get() = this._chargePortLatch ?: _protobufPending(35, { CarServer_ChargePortLatchState() }) { this._chargePortLatch = it }
        set(value) {
            _protobufDropPending(35)
            this._chargePortLatch = value.copy()
            _protobufMutated()
        }

    val hasChargePortLatch: Boolean
        get() = this._chargePortLatch != null

    fun clearChargePortLatch() {
        _protobufDropPending(35)
        this._chargePortLatch = null
        _protobufMutated()
    }

    var optionalChargePortColdWeatherMode: CarServer_ChargeState.OneOf_OptionalChargePortColdWeatherMode?
        get() = this._optionalChargePortColdWeatherMode
        set(value) {
            this._optionalChargePortColdWeatherMode = value
            _protobufMutated()
        }

    var chargePortColdWeatherMode: Boolean
        get() = (this._optionalChargePortColdWeatherMode as? CarServer_ChargeState.OneOf_OptionalChargePortColdWeatherMode.chargePortColdWeatherMode)?.value ?: false
        set(value) {
            this.optionalChargePortColdWeatherMode = CarServer_ChargeState.OneOf_OptionalChargePortColdWeatherMode.chargePortColdWeatherMode(value)
        }

    var optionalChargeCurrentRequest: CarServer_ChargeState.OneOf_OptionalChargeCurrentRequest?
        get() = this._optionalChargeCurrentRequest
        set(value) {
            this._optionalChargeCurrentRequest = value
            _protobufMutated()
        }

    var chargeCurrentRequest: Int
        get() = (this._optionalChargeCurrentRequest as? CarServer_ChargeState.OneOf_OptionalChargeCurrentRequest.chargeCurrentRequest)?.value ?: 0
        set(value) {
            this.optionalChargeCurrentRequest = CarServer_ChargeState.OneOf_OptionalChargeCurrentRequest.chargeCurrentRequest(value)
        }

    var optionalChargeCurrentRequestMax: CarServer_ChargeState.OneOf_OptionalChargeCurrentRequestMax?
        get() = this._optionalChargeCurrentRequestMax
        set(value) {
            this._optionalChargeCurrentRequestMax = value
            _protobufMutated()
        }

    var chargeCurrentRequestMax: Int
        get() = (this._optionalChargeCurrentRequestMax as? CarServer_ChargeState.OneOf_OptionalChargeCurrentRequestMax.chargeCurrentRequestMax)?.value ?: 0
        set(value) {
            this.optionalChargeCurrentRequestMax = CarServer_ChargeState.OneOf_OptionalChargeCurrentRequestMax.chargeCurrentRequestMax(value)
        }

    var optionalManagedChargingActive: CarServer_ChargeState.OneOf_OptionalManagedChargingActive?
        get() = this._optionalManagedChargingActive
        set(value) {
            this._optionalManagedChargingActive = value
            _protobufMutated()
        }

    var managedChargingActive: Boolean
        get() = (this._optionalManagedChargingActive as? CarServer_ChargeState.OneOf_OptionalManagedChargingActive.managedChargingActive)?.value ?: false
        set(value) {
            this.optionalManagedChargingActive = CarServer_ChargeState.OneOf_OptionalManagedChargingActive.managedChargingActive(value)
        }

    var optionalManagedChargingUserCanceled: CarServer_ChargeState.OneOf_OptionalManagedChargingUserCanceled?
        get() = this._optionalManagedChargingUserCanceled
        set(value) {
            this._optionalManagedChargingUserCanceled = value
            _protobufMutated()
        }

    var managedChargingUserCanceled: Boolean
        get() = (this._optionalManagedChargingUserCanceled as? CarServer_ChargeState.OneOf_OptionalManagedChargingUserCanceled.managedChargingUserCanceled)?.value ?: false
        set(value) {
            this.optionalManagedChargingUserCanceled = CarServer_ChargeState.OneOf_OptionalManagedChargingUserCanceled.managedChargingUserCanceled(value)
        }

    var optionalManagedChargingStartTime: CarServer_ChargeState.OneOf_OptionalManagedChargingStartTime?
        get() = this._optionalManagedChargingStartTime
        set(value) {
            this._optionalManagedChargingStartTime = value
            _protobufMutated()
        }

    var managedChargingStartTime: ULong
        get() = (this._optionalManagedChargingStartTime as? CarServer_ChargeState.OneOf_OptionalManagedChargingStartTime.managedChargingStartTime)?.value ?: 0uL
        set(value) {
            this.optionalManagedChargingStartTime = CarServer_ChargeState.OneOf_OptionalManagedChargingStartTime.managedChargingStartTime(value)
        }

    var timestamp: Google_Protobuf_Timestamp
        get() = this._timestamp ?: _protobufPending(44, { Google_Protobuf_Timestamp() }) { this._timestamp = it }
        set(value) {
            _protobufDropPending(44)
            this._timestamp = value.copy()
            _protobufMutated()
        }

    val hasTimestamp: Boolean
        get() = this._timestamp != null

    fun clearTimestamp() {
        _protobufDropPending(44)
        this._timestamp = null
        _protobufMutated()
    }

    var preconditioningTimes: CarServer_PreconditioningTimes
        get() = this._preconditioningTimes ?: _protobufPending(45, { CarServer_PreconditioningTimes() }) { this._preconditioningTimes = it }
        set(value) {
            _protobufDropPending(45)
            this._preconditioningTimes = value.copy()
            _protobufMutated()
        }

    val hasPreconditioningTimes: Boolean
        get() = this._preconditioningTimes != null

    fun clearPreconditioningTimes() {
        _protobufDropPending(45)
        this._preconditioningTimes = null
        _protobufMutated()
    }

    var offPeakChargingTimes: CarServer_OffPeakChargingTimes
        get() = this._offPeakChargingTimes ?: _protobufPending(46, { CarServer_OffPeakChargingTimes() }) { this._offPeakChargingTimes = it }
        set(value) {
            _protobufDropPending(46)
            this._offPeakChargingTimes = value.copy()
            _protobufMutated()
        }

    val hasOffPeakChargingTimes: Boolean
        get() = this._offPeakChargingTimes != null

    fun clearOffPeakChargingTimes() {
        _protobufDropPending(46)
        this._offPeakChargingTimes = null
        _protobufMutated()
    }

    var optionalOffPeakHoursEndTime: CarServer_ChargeState.OneOf_OptionalOffPeakHoursEndTime?
        get() = this._optionalOffPeakHoursEndTime
        set(value) {
            this._optionalOffPeakHoursEndTime = value
            _protobufMutated()
        }

    var offPeakHoursEndTime: UInt
        get() = (this._optionalOffPeakHoursEndTime as? CarServer_ChargeState.OneOf_OptionalOffPeakHoursEndTime.offPeakHoursEndTime)?.value ?: 0u
        set(value) {
            this.optionalOffPeakHoursEndTime = CarServer_ChargeState.OneOf_OptionalOffPeakHoursEndTime.offPeakHoursEndTime(value)
        }

    var optionalScheduledChargingMode: CarServer_ChargeState.OneOf_OptionalScheduledChargingMode?
        get() = this._optionalScheduledChargingMode
        set(value) {
            this._protobufStore_optionalScheduledChargingMode(value)
            _protobufMutated()
        }

    var scheduledChargingMode: CarServer_ChargeState.ScheduledChargingMode
        get() = (this._optionalScheduledChargingMode as? CarServer_ChargeState.OneOf_OptionalScheduledChargingMode.scheduledChargingMode)?.value ?: CarServer_ChargeState.ScheduledChargingMode.off
        set(value) {
            this.optionalScheduledChargingMode = CarServer_ChargeState.OneOf_OptionalScheduledChargingMode.scheduledChargingMode(value)
        }

    var optionalChargingAmps: CarServer_ChargeState.OneOf_OptionalChargingAmps?
        get() = this._optionalChargingAmps
        set(value) {
            this._optionalChargingAmps = value
            _protobufMutated()
        }

    var chargingAmps: Int
        get() = (this._optionalChargingAmps as? CarServer_ChargeState.OneOf_OptionalChargingAmps.chargingAmps)?.value ?: 0
        set(value) {
            this.optionalChargingAmps = CarServer_ChargeState.OneOf_OptionalChargingAmps.chargingAmps(value)
        }

    var optionalScheduledChargingStartTimeMinutes: CarServer_ChargeState.OneOf_OptionalScheduledChargingStartTimeMinutes?
        get() = this._optionalScheduledChargingStartTimeMinutes
        set(value) {
            this._optionalScheduledChargingStartTimeMinutes = value
            _protobufMutated()
        }

    var scheduledChargingStartTimeMinutes: UInt
        get() = (this._optionalScheduledChargingStartTimeMinutes as? CarServer_ChargeState.OneOf_OptionalScheduledChargingStartTimeMinutes.scheduledChargingStartTimeMinutes)?.value ?: 0u
        set(value) {
            this.optionalScheduledChargingStartTimeMinutes = CarServer_ChargeState.OneOf_OptionalScheduledChargingStartTimeMinutes.scheduledChargingStartTimeMinutes(value)
        }

    var optionalScheduledDepartureTimeMinutes: CarServer_ChargeState.OneOf_OptionalScheduledDepartureTimeMinutes?
        get() = this._optionalScheduledDepartureTimeMinutes
        set(value) {
            this._optionalScheduledDepartureTimeMinutes = value
            _protobufMutated()
        }

    var scheduledDepartureTimeMinutes: UInt
        get() = (this._optionalScheduledDepartureTimeMinutes as? CarServer_ChargeState.OneOf_OptionalScheduledDepartureTimeMinutes.scheduledDepartureTimeMinutes)?.value ?: 0u
        set(value) {
            this.optionalScheduledDepartureTimeMinutes = CarServer_ChargeState.OneOf_OptionalScheduledDepartureTimeMinutes.scheduledDepartureTimeMinutes(value)
        }

    var optionalPreconditioningEnabled: CarServer_ChargeState.OneOf_OptionalPreconditioningEnabled?
        get() = this._optionalPreconditioningEnabled
        set(value) {
            this._optionalPreconditioningEnabled = value
            _protobufMutated()
        }

    var preconditioningEnabled: Boolean
        get() = (this._optionalPreconditioningEnabled as? CarServer_ChargeState.OneOf_OptionalPreconditioningEnabled.preconditioningEnabled)?.value ?: false
        set(value) {
            this.optionalPreconditioningEnabled = CarServer_ChargeState.OneOf_OptionalPreconditioningEnabled.preconditioningEnabled(value)
        }

    var optionalScheduledChargingStartTimeApp: CarServer_ChargeState.OneOf_OptionalScheduledChargingStartTimeApp?
        get() = this._optionalScheduledChargingStartTimeApp
        set(value) {
            this._optionalScheduledChargingStartTimeApp = value
            _protobufMutated()
        }

    var scheduledChargingStartTimeApp: Int
        get() = (this._optionalScheduledChargingStartTimeApp as? CarServer_ChargeState.OneOf_OptionalScheduledChargingStartTimeApp.scheduledChargingStartTimeApp)?.value ?: 0
        set(value) {
            this.optionalScheduledChargingStartTimeApp = CarServer_ChargeState.OneOf_OptionalScheduledChargingStartTimeApp.scheduledChargingStartTimeApp(value)
        }

    var optionalSuperchargerSessionTripPlanner: CarServer_ChargeState.OneOf_OptionalSuperchargerSessionTripPlanner?
        get() = this._optionalSuperchargerSessionTripPlanner
        set(value) {
            this._optionalSuperchargerSessionTripPlanner = value
            _protobufMutated()
        }

    var superchargerSessionTripPlanner: Boolean
        get() = (this._optionalSuperchargerSessionTripPlanner as? CarServer_ChargeState.OneOf_OptionalSuperchargerSessionTripPlanner.superchargerSessionTripPlanner)?.value ?: false
        set(value) {
            this.optionalSuperchargerSessionTripPlanner = CarServer_ChargeState.OneOf_OptionalSuperchargerSessionTripPlanner.superchargerSessionTripPlanner(value)
        }

    var optionalChargePortColor: CarServer_ChargeState.OneOf_OptionalChargePortColor?
        get() = this._optionalChargePortColor
        set(value) {
            this._protobufStore_optionalChargePortColor(value)
            _protobufMutated()
        }

    var chargePortColor: CarServer_ChargeState.ChargePortColor_E
        get() = (this._optionalChargePortColor as? CarServer_ChargeState.OneOf_OptionalChargePortColor.chargePortColor)?.value ?: CarServer_ChargeState.ChargePortColor_E.chargePortColorOff
        set(value) {
            this.optionalChargePortColor = CarServer_ChargeState.OneOf_OptionalChargePortColor.chargePortColor(value)
        }

    var optionalChargeRateMphFloat: CarServer_ChargeState.OneOf_OptionalChargeRateMphFloat?
        get() = this._optionalChargeRateMphFloat
        set(value) {
            this._optionalChargeRateMphFloat = value
            _protobufMutated()
        }

    var chargeRateMphFloat: Float
        get() = (this._optionalChargeRateMphFloat as? CarServer_ChargeState.OneOf_OptionalChargeRateMphFloat.chargeRateMphFloat)?.value ?: 0f
        set(value) {
            this.optionalChargeRateMphFloat = CarServer_ChargeState.OneOf_OptionalChargeRateMphFloat.chargeRateMphFloat(value)
        }

    var optionalChargeLimitReason: CarServer_ChargeState.OneOf_OptionalChargeLimitReason?
        get() = this._optionalChargeLimitReason
        set(value) {
            this._protobufStore_optionalChargeLimitReason(value)
            _protobufMutated()
        }

    var chargeLimitReason: CarServer_ChargeState.ChargeLimitReason
        get() = (this._optionalChargeLimitReason as? CarServer_ChargeState.OneOf_OptionalChargeLimitReason.chargeLimitReason)?.value ?: CarServer_ChargeState.ChargeLimitReason.unknown
        set(value) {
            this.optionalChargeLimitReason = CarServer_ChargeState.OneOf_OptionalChargeLimitReason.chargeLimitReason(value)
        }

    var managedChargingState: CarServer_ManagedChargingState
        get() = this._managedChargingState ?: _protobufPending(158, { CarServer_ManagedChargingState() }) { this._managedChargingState = it }
        set(value) {
            _protobufDropPending(158)
            this._managedChargingState = value.copy()
            _protobufMutated()
        }

    val hasManagedChargingState: Boolean
        get() = this._managedChargingState != null

    fun clearManagedChargingState() {
        _protobufDropPending(158)
        this._managedChargingState = null
        _protobufMutated()
    }

    var optionalChargeCableUnlatched: CarServer_ChargeState.OneOf_OptionalChargeCableUnlatched?
        get() = this._optionalChargeCableUnlatched
        set(value) {
            this._optionalChargeCableUnlatched = value
            _protobufMutated()
        }

    var chargeCableUnlatched: Boolean
        get() = (this._optionalChargeCableUnlatched as? CarServer_ChargeState.OneOf_OptionalChargeCableUnlatched.chargeCableUnlatched)?.value ?: false
        set(value) {
            this.optionalChargeCableUnlatched = CarServer_ChargeState.OneOf_OptionalChargeCableUnlatched.chargeCableUnlatched(value)
        }

    var optionalOutletState: CarServer_ChargeState.OneOf_OptionalOutletState?
        get() = this._optionalOutletState
        set(value) {
            this._protobufStore_optionalOutletState(value)
            _protobufMutated()
        }

    var outletState: CarServer_ChargeState.OutletState
        get() = (this._optionalOutletState as? CarServer_ChargeState.OneOf_OptionalOutletState.outletState)?.value ?: CarServer_ChargeState.OutletState.off
        set(value) {
            this.optionalOutletState = CarServer_ChargeState.OneOf_OptionalOutletState.outletState(value)
        }

    var optionalPowerFeedState: CarServer_ChargeState.OneOf_OptionalPowerFeedState?
        get() = this._optionalPowerFeedState
        set(value) {
            this._protobufStore_optionalPowerFeedState(value)
            _protobufMutated()
        }

    var powerFeedState: CarServer_ChargeState.PowerFeedState
        get() = (this._optionalPowerFeedState as? CarServer_ChargeState.OneOf_OptionalPowerFeedState.powerFeedState)?.value ?: CarServer_ChargeState.PowerFeedState.off
        set(value) {
            this.optionalPowerFeedState = CarServer_ChargeState.OneOf_OptionalPowerFeedState.powerFeedState(value)
        }

    var optionOutletSocLimit: CarServer_ChargeState.OneOf_OptionOutletSocLimit?
        get() = this._optionOutletSocLimit
        set(value) {
            this._optionOutletSocLimit = value
            _protobufMutated()
        }

    var outletSocLimit: Int
        get() = (this._optionOutletSocLimit as? CarServer_ChargeState.OneOf_OptionOutletSocLimit.outletSocLimit)?.value ?: 0
        set(value) {
            this.optionOutletSocLimit = CarServer_ChargeState.OneOf_OptionOutletSocLimit.outletSocLimit(value)
        }

    var optionPowerFeedSocLimit: CarServer_ChargeState.OneOf_OptionPowerFeedSocLimit?
        get() = this._optionPowerFeedSocLimit
        set(value) {
            this._optionPowerFeedSocLimit = value
            _protobufMutated()
        }

    var powerFeedSocLimit: Int
        get() = (this._optionPowerFeedSocLimit as? CarServer_ChargeState.OneOf_OptionPowerFeedSocLimit.powerFeedSocLimit)?.value ?: 0
        set(value) {
            this.optionPowerFeedSocLimit = CarServer_ChargeState.OneOf_OptionPowerFeedSocLimit.powerFeedSocLimit(value)
        }

    var optionOutletTimeRemaining: CarServer_ChargeState.OneOf_OptionOutletTimeRemaining?
        get() = this._optionOutletTimeRemaining
        set(value) {
            this._optionOutletTimeRemaining = value
            _protobufMutated()
        }

    var outletTimeRemaining: Long
        get() = (this._optionOutletTimeRemaining as? CarServer_ChargeState.OneOf_OptionOutletTimeRemaining.outletTimeRemaining)?.value ?: 0L
        set(value) {
            this.optionOutletTimeRemaining = CarServer_ChargeState.OneOf_OptionOutletTimeRemaining.outletTimeRemaining(value)
        }

    var optionPowerFeedTimeRemaining: CarServer_ChargeState.OneOf_OptionPowerFeedTimeRemaining?
        get() = this._optionPowerFeedTimeRemaining
        set(value) {
            this._optionPowerFeedTimeRemaining = value
            _protobufMutated()
        }

    var powerFeedTimeRemaining: Long
        get() = (this._optionPowerFeedTimeRemaining as? CarServer_ChargeState.OneOf_OptionPowerFeedTimeRemaining.powerFeedTimeRemaining)?.value ?: 0L
        set(value) {
            this.optionPowerFeedTimeRemaining = CarServer_ChargeState.OneOf_OptionPowerFeedTimeRemaining.powerFeedTimeRemaining(value)
        }

    var optionalPowershareFeatureAllowed: CarServer_ChargeState.OneOf_OptionalPowershareFeatureAllowed?
        get() = this._optionalPowershareFeatureAllowed
        set(value) {
            this._optionalPowershareFeatureAllowed = value
            _protobufMutated()
        }

    var powershareFeatureAllowed: Boolean
        get() = (this._optionalPowershareFeatureAllowed as? CarServer_ChargeState.OneOf_OptionalPowershareFeatureAllowed.powershareFeatureAllowed)?.value ?: false
        set(value) {
            this.optionalPowershareFeatureAllowed = CarServer_ChargeState.OneOf_OptionalPowershareFeatureAllowed.powershareFeatureAllowed(value)
        }

    var optionalPowershareFeatureEnabled: CarServer_ChargeState.OneOf_OptionalPowershareFeatureEnabled?
        get() = this._optionalPowershareFeatureEnabled
        set(value) {
            this._optionalPowershareFeatureEnabled = value
            _protobufMutated()
        }

    var powershareFeatureEnabled: Boolean
        get() = (this._optionalPowershareFeatureEnabled as? CarServer_ChargeState.OneOf_OptionalPowershareFeatureEnabled.powershareFeatureEnabled)?.value ?: false
        set(value) {
            this.optionalPowershareFeatureEnabled = CarServer_ChargeState.OneOf_OptionalPowershareFeatureEnabled.powershareFeatureEnabled(value)
        }

    var optionalPowershareRequest: CarServer_ChargeState.OneOf_OptionalPowershareRequest?
        get() = this._optionalPowershareRequest
        set(value) {
            this._optionalPowershareRequest = value
            _protobufMutated()
        }

    var powershareRequest: Boolean
        get() = (this._optionalPowershareRequest as? CarServer_ChargeState.OneOf_OptionalPowershareRequest.powershareRequest)?.value ?: false
        set(value) {
            this.optionalPowershareRequest = CarServer_ChargeState.OneOf_OptionalPowershareRequest.powershareRequest(value)
        }

    var optionalPowershareType: CarServer_ChargeState.OneOf_OptionalPowershareType?
        get() = this._optionalPowershareType
        set(value) {
            this._protobufStore_optionalPowershareType(value)
            _protobufMutated()
        }

    var powershareType: CarServer_ChargeState.PowershareType
        get() = (this._optionalPowershareType as? CarServer_ChargeState.OneOf_OptionalPowershareType.powershareType)?.value ?: CarServer_ChargeState.PowershareType.none
        set(value) {
            this.optionalPowershareType = CarServer_ChargeState.OneOf_OptionalPowershareType.powershareType(value)
        }

    var optionalPowershareStatus: CarServer_ChargeState.OneOf_OptionalPowershareStatus?
        get() = this._optionalPowershareStatus
        set(value) {
            this._protobufStore_optionalPowershareStatus(value)
            _protobufMutated()
        }

    var powershareStatus: CarServer_ChargeState.PowershareStatus
        get() = (this._optionalPowershareStatus as? CarServer_ChargeState.OneOf_OptionalPowershareStatus.powershareStatus)?.value ?: CarServer_ChargeState.PowershareStatus.inactive
        set(value) {
            this.optionalPowershareStatus = CarServer_ChargeState.OneOf_OptionalPowershareStatus.powershareStatus(value)
        }

    var optionalPowershareStopReason: CarServer_ChargeState.OneOf_OptionalPowershareStopReason?
        get() = this._optionalPowershareStopReason
        set(value) {
            this._protobufStore_optionalPowershareStopReason(value)
            _protobufMutated()
        }

    var powershareStopReason: CarServer_ChargeState.PowershareStopReason
        get() = (this._optionalPowershareStopReason as? CarServer_ChargeState.OneOf_OptionalPowershareStopReason.powershareStopReason)?.value ?: CarServer_ChargeState.PowershareStopReason.none
        set(value) {
            this.optionalPowershareStopReason = CarServer_ChargeState.OneOf_OptionalPowershareStopReason.powershareStopReason(value)
        }

    var optionalPowershareInstantaneousLoadKw: CarServer_ChargeState.OneOf_OptionalPowershareInstantaneousLoadKw?
        get() = this._optionalPowershareInstantaneousLoadKw
        set(value) {
            this._optionalPowershareInstantaneousLoadKw = value
            _protobufMutated()
        }

    var powershareInstantaneousLoadKw: Float
        get() = (this._optionalPowershareInstantaneousLoadKw as? CarServer_ChargeState.OneOf_OptionalPowershareInstantaneousLoadKw.powershareInstantaneousLoadKw)?.value ?: 0f
        set(value) {
            this.optionalPowershareInstantaneousLoadKw = CarServer_ChargeState.OneOf_OptionalPowershareInstantaneousLoadKw.powershareInstantaneousLoadKw(value)
        }

    var optionalPowershareVehicleEnergyLeftHr: CarServer_ChargeState.OneOf_OptionalPowershareVehicleEnergyLeftHr?
        get() = this._optionalPowershareVehicleEnergyLeftHr
        set(value) {
            this._optionalPowershareVehicleEnergyLeftHr = value
            _protobufMutated()
        }

    var powershareVehicleEnergyLeftHr: Int
        get() = (this._optionalPowershareVehicleEnergyLeftHr as? CarServer_ChargeState.OneOf_OptionalPowershareVehicleEnergyLeftHr.powershareVehicleEnergyLeftHr)?.value ?: 0
        set(value) {
            this.optionalPowershareVehicleEnergyLeftHr = CarServer_ChargeState.OneOf_OptionalPowershareVehicleEnergyLeftHr.powershareVehicleEnergyLeftHr(value)
        }

    var optionalPowershareSocLimit: CarServer_ChargeState.OneOf_OptionalPowershareSocLimit?
        get() = this._optionalPowershareSocLimit
        set(value) {
            this._optionalPowershareSocLimit = value
            _protobufMutated()
        }

    var powershareSocLimit: Int
        get() = (this._optionalPowershareSocLimit as? CarServer_ChargeState.OneOf_OptionalPowershareSocLimit.powershareSocLimit)?.value ?: 0
        set(value) {
            this.optionalPowershareSocLimit = CarServer_ChargeState.OneOf_OptionalPowershareSocLimit.powershareSocLimit(value)
        }

    var optionalOneTimeSocLimit: CarServer_ChargeState.OneOf_OptionalOneTimeSocLimit?
        get() = this._optionalOneTimeSocLimit
        set(value) {
            this._optionalOneTimeSocLimit = value
            _protobufMutated()
        }

    var oneTimeSocLimit: Int
        get() = (this._optionalOneTimeSocLimit as? CarServer_ChargeState.OneOf_OptionalOneTimeSocLimit.oneTimeSocLimit)?.value ?: 0
        set(value) {
            this.optionalOneTimeSocLimit = CarServer_ChargeState.OneOf_OptionalOneTimeSocLimit.oneTimeSocLimit(value)
        }

    var optionalHomeLocation: CarServer_ChargeState.OneOf_OptionalHomeLocation?
        get() = this._optionalHomeLocation
        set(value) {
            this._protobufStore_optionalHomeLocation(this._protobufCopy_optionalHomeLocation(value))
            _protobufMutated()
        }

    var homeLocation: CarServer_LatLong
        get() = (this._optionalHomeLocation as? CarServer_ChargeState.OneOf_OptionalHomeLocation.homeLocation)?.value
            ?: _protobufPending(176, { CarServer_LatLong() }) { this._protobufStore_optionalHomeLocation(CarServer_ChargeState.OneOf_OptionalHomeLocation.homeLocation(it)) }
        set(value) {
            this.optionalHomeLocation = CarServer_ChargeState.OneOf_OptionalHomeLocation.homeLocation(value)
        }

    var optionalWorkLocation: CarServer_ChargeState.OneOf_OptionalWorkLocation?
        get() = this._optionalWorkLocation
        set(value) {
            this._protobufStore_optionalWorkLocation(this._protobufCopy_optionalWorkLocation(value))
            _protobufMutated()
        }

    var workLocation: CarServer_LatLong
        get() = (this._optionalWorkLocation as? CarServer_ChargeState.OneOf_OptionalWorkLocation.workLocation)?.value
            ?: _protobufPending(177, { CarServer_LatLong() }) { this._protobufStore_optionalWorkLocation(CarServer_ChargeState.OneOf_OptionalWorkLocation.workLocation(it)) }
        set(value) {
            this.optionalWorkLocation = CarServer_ChargeState.OneOf_OptionalWorkLocation.workLocation(value)
        }

    var optionalOutletMaxTimerMinutes: CarServer_ChargeState.OneOf_OptionalOutletMaxTimerMinutes?
        get() = this._optionalOutletMaxTimerMinutes
        set(value) {
            this._optionalOutletMaxTimerMinutes = value
            _protobufMutated()
        }

    var outletMaxTimerMinutes: Int
        get() = (this._optionalOutletMaxTimerMinutes as? CarServer_ChargeState.OneOf_OptionalOutletMaxTimerMinutes.outletMaxTimerMinutes)?.value ?: 0
        set(value) {
            this.optionalOutletMaxTimerMinutes = CarServer_ChargeState.OneOf_OptionalOutletMaxTimerMinutes.outletMaxTimerMinutes(value)
        }

    sealed class OneOf_OptionalChargeLimitSoc(value: Any) : ProtobufOneofCase(value) {
        class chargeLimitSoc(val value: Int) : OneOf_OptionalChargeLimitSoc(value)
    }

    sealed class OneOf_OptionalChargeLimitSocStd(value: Any) : ProtobufOneofCase(value) {
        class chargeLimitSocStd(val value: Int) : OneOf_OptionalChargeLimitSocStd(value)
    }

    sealed class OneOf_OptionalChargeLimitSocMin(value: Any) : ProtobufOneofCase(value) {
        class chargeLimitSocMin(val value: Int) : OneOf_OptionalChargeLimitSocMin(value)
    }

    sealed class OneOf_OptionalChargeLimitSocMax(value: Any) : ProtobufOneofCase(value) {
        class chargeLimitSocMax(val value: Int) : OneOf_OptionalChargeLimitSocMax(value)
    }

    sealed class OneOf_OptionalMaxRangeChargeCounter(value: Any) : ProtobufOneofCase(value) {
        class maxRangeChargeCounter(val value: Int) : OneOf_OptionalMaxRangeChargeCounter(value)
    }

    sealed class OneOf_OptionalFastChargerPresent(value: Any) : ProtobufOneofCase(value) {
        class fastChargerPresent(val value: Boolean) : OneOf_OptionalFastChargerPresent(value)
    }

    sealed class OneOf_OptionalBatteryRange(value: Any) : ProtobufOneofCase(value) {
        class batteryRange(val value: Float) : OneOf_OptionalBatteryRange(value)
    }

    sealed class OneOf_OptionalEstBatteryRange(value: Any) : ProtobufOneofCase(value) {
        class estBatteryRange(val value: Float) : OneOf_OptionalEstBatteryRange(value)
    }

    sealed class OneOf_OptionalIdealBatteryRange(value: Any) : ProtobufOneofCase(value) {
        class idealBatteryRange(val value: Float) : OneOf_OptionalIdealBatteryRange(value)
    }

    sealed class OneOf_OptionalBatteryLevel(value: Any) : ProtobufOneofCase(value) {
        class batteryLevel(val value: Int) : OneOf_OptionalBatteryLevel(value)
    }

    sealed class OneOf_OptionalUsableBatteryLevel(value: Any) : ProtobufOneofCase(value) {
        class usableBatteryLevel(val value: Int) : OneOf_OptionalUsableBatteryLevel(value)
    }

    sealed class OneOf_OptionalChargeEnergyAdded(value: Any) : ProtobufOneofCase(value) {
        class chargeEnergyAdded(val value: Float) : OneOf_OptionalChargeEnergyAdded(value)
    }

    sealed class OneOf_OptionalChargeMilesAddedRated(value: Any) : ProtobufOneofCase(value) {
        class chargeMilesAddedRated(val value: Float) : OneOf_OptionalChargeMilesAddedRated(value)
    }

    sealed class OneOf_OptionalChargeMilesAddedIdeal(value: Any) : ProtobufOneofCase(value) {
        class chargeMilesAddedIdeal(val value: Float) : OneOf_OptionalChargeMilesAddedIdeal(value)
    }

    sealed class OneOf_OptionalChargerVoltage(value: Any) : ProtobufOneofCase(value) {
        class chargerVoltage(val value: Int) : OneOf_OptionalChargerVoltage(value)
    }

    sealed class OneOf_OptionalChargerPilotCurrent(value: Any) : ProtobufOneofCase(value) {
        class chargerPilotCurrent(val value: Int) : OneOf_OptionalChargerPilotCurrent(value)
    }

    sealed class OneOf_OptionalChargerActualCurrent(value: Any) : ProtobufOneofCase(value) {
        class chargerActualCurrent(val value: Int) : OneOf_OptionalChargerActualCurrent(value)
    }

    sealed class OneOf_OptionalChargerPower(value: Any) : ProtobufOneofCase(value) {
        class chargerPower(val value: Int) : OneOf_OptionalChargerPower(value)
    }

    sealed class OneOf_OptionalMinutesToFullCharge(value: Any) : ProtobufOneofCase(value) {
        class minutesToFullCharge(val value: Int) : OneOf_OptionalMinutesToFullCharge(value)
    }

    sealed class OneOf_OptionalMinutesToChargeLimit(value: Any) : ProtobufOneofCase(value) {
        class minutesToChargeLimit(val value: Int) : OneOf_OptionalMinutesToChargeLimit(value)
    }

    sealed class OneOf_OptionalTripCharging(value: Any) : ProtobufOneofCase(value) {
        class tripCharging(val value: Boolean) : OneOf_OptionalTripCharging(value)
    }

    sealed class OneOf_OptionalChargeRateMph(value: Any) : ProtobufOneofCase(value) {
        class chargeRateMph(val value: Int) : OneOf_OptionalChargeRateMph(value)
    }

    sealed class OneOf_OptionalChargePortDoorOpen(value: Any) : ProtobufOneofCase(value) {
        class chargePortDoorOpen(val value: Boolean) : OneOf_OptionalChargePortDoorOpen(value)
    }

    sealed class OneOf_OptionalScheduledChargingStartTime(value: Any) : ProtobufOneofCase(value) {
        class scheduledChargingStartTime(val value: ULong) : OneOf_OptionalScheduledChargingStartTime(value)
    }

    sealed class OneOf_OptionalScheduledChargingPending(value: Any) : ProtobufOneofCase(value) {
        class scheduledChargingPending(val value: Boolean) : OneOf_OptionalScheduledChargingPending(value)
    }

    sealed class OneOf_OptionalUserChargeEnableRequest(value: Any) : ProtobufOneofCase(value) {
        class userChargeEnableRequest(val value: Boolean) : OneOf_OptionalUserChargeEnableRequest(value)
    }

    sealed class OneOf_OptionalChargeEnableRequest(value: Any) : ProtobufOneofCase(value) {
        class chargeEnableRequest(val value: Boolean) : OneOf_OptionalChargeEnableRequest(value)
    }

    sealed class OneOf_OptionalChargerPhases(value: Any) : ProtobufOneofCase(value) {
        class chargerPhases(val value: Int) : OneOf_OptionalChargerPhases(value)
    }

    sealed class OneOf_OptionalChargePortColdWeatherMode(value: Any) : ProtobufOneofCase(value) {
        class chargePortColdWeatherMode(val value: Boolean) : OneOf_OptionalChargePortColdWeatherMode(value)
    }

    sealed class OneOf_OptionalChargeCurrentRequest(value: Any) : ProtobufOneofCase(value) {
        class chargeCurrentRequest(val value: Int) : OneOf_OptionalChargeCurrentRequest(value)
    }

    sealed class OneOf_OptionalChargeCurrentRequestMax(value: Any) : ProtobufOneofCase(value) {
        class chargeCurrentRequestMax(val value: Int) : OneOf_OptionalChargeCurrentRequestMax(value)
    }

    sealed class OneOf_OptionalManagedChargingActive(value: Any) : ProtobufOneofCase(value) {
        class managedChargingActive(val value: Boolean) : OneOf_OptionalManagedChargingActive(value)
    }

    sealed class OneOf_OptionalManagedChargingUserCanceled(value: Any) : ProtobufOneofCase(value) {
        class managedChargingUserCanceled(val value: Boolean) : OneOf_OptionalManagedChargingUserCanceled(value)
    }

    sealed class OneOf_OptionalManagedChargingStartTime(value: Any) : ProtobufOneofCase(value) {
        class managedChargingStartTime(val value: ULong) : OneOf_OptionalManagedChargingStartTime(value)
    }

    sealed class OneOf_OptionalOffPeakHoursEndTime(value: Any) : ProtobufOneofCase(value) {
        class offPeakHoursEndTime(val value: UInt) : OneOf_OptionalOffPeakHoursEndTime(value)
    }

    sealed class OneOf_OptionalScheduledChargingMode(value: Any) : ProtobufOneofCase(value) {
        class scheduledChargingMode(val value: CarServer_ChargeState.ScheduledChargingMode) : OneOf_OptionalScheduledChargingMode(value)
    }

    sealed class OneOf_OptionalChargingAmps(value: Any) : ProtobufOneofCase(value) {
        class chargingAmps(val value: Int) : OneOf_OptionalChargingAmps(value)
    }

    sealed class OneOf_OptionalScheduledChargingStartTimeMinutes(value: Any) : ProtobufOneofCase(value) {
        class scheduledChargingStartTimeMinutes(val value: UInt) : OneOf_OptionalScheduledChargingStartTimeMinutes(value)
    }

    sealed class OneOf_OptionalScheduledDepartureTimeMinutes(value: Any) : ProtobufOneofCase(value) {
        class scheduledDepartureTimeMinutes(val value: UInt) : OneOf_OptionalScheduledDepartureTimeMinutes(value)
    }

    sealed class OneOf_OptionalPreconditioningEnabled(value: Any) : ProtobufOneofCase(value) {
        class preconditioningEnabled(val value: Boolean) : OneOf_OptionalPreconditioningEnabled(value)
    }

    sealed class OneOf_OptionalScheduledChargingStartTimeApp(value: Any) : ProtobufOneofCase(value) {
        class scheduledChargingStartTimeApp(val value: Int) : OneOf_OptionalScheduledChargingStartTimeApp(value)
    }

    sealed class OneOf_OptionalSuperchargerSessionTripPlanner(value: Any) : ProtobufOneofCase(value) {
        class superchargerSessionTripPlanner(val value: Boolean) : OneOf_OptionalSuperchargerSessionTripPlanner(value)
    }

    sealed class OneOf_OptionalChargePortColor(value: Any) : ProtobufOneofCase(value) {
        class chargePortColor(val value: CarServer_ChargeState.ChargePortColor_E) : OneOf_OptionalChargePortColor(value)
    }

    sealed class OneOf_OptionalChargeRateMphFloat(value: Any) : ProtobufOneofCase(value) {
        class chargeRateMphFloat(val value: Float) : OneOf_OptionalChargeRateMphFloat(value)
    }

    sealed class OneOf_OptionalChargeLimitReason(value: Any) : ProtobufOneofCase(value) {
        class chargeLimitReason(val value: CarServer_ChargeState.ChargeLimitReason) : OneOf_OptionalChargeLimitReason(value)
    }

    sealed class OneOf_OptionalChargeCableUnlatched(value: Any) : ProtobufOneofCase(value) {
        class chargeCableUnlatched(val value: Boolean) : OneOf_OptionalChargeCableUnlatched(value)
    }

    sealed class OneOf_OptionalOutletState(value: Any) : ProtobufOneofCase(value) {
        class outletState(val value: CarServer_ChargeState.OutletState) : OneOf_OptionalOutletState(value)
    }

    sealed class OneOf_OptionalPowerFeedState(value: Any) : ProtobufOneofCase(value) {
        class powerFeedState(val value: CarServer_ChargeState.PowerFeedState) : OneOf_OptionalPowerFeedState(value)
    }

    sealed class OneOf_OptionOutletSocLimit(value: Any) : ProtobufOneofCase(value) {
        class outletSocLimit(val value: Int) : OneOf_OptionOutletSocLimit(value)
    }

    sealed class OneOf_OptionPowerFeedSocLimit(value: Any) : ProtobufOneofCase(value) {
        class powerFeedSocLimit(val value: Int) : OneOf_OptionPowerFeedSocLimit(value)
    }

    sealed class OneOf_OptionOutletTimeRemaining(value: Any) : ProtobufOneofCase(value) {
        class outletTimeRemaining(val value: Long) : OneOf_OptionOutletTimeRemaining(value)
    }

    sealed class OneOf_OptionPowerFeedTimeRemaining(value: Any) : ProtobufOneofCase(value) {
        class powerFeedTimeRemaining(val value: Long) : OneOf_OptionPowerFeedTimeRemaining(value)
    }

    sealed class OneOf_OptionalPowershareFeatureAllowed(value: Any) : ProtobufOneofCase(value) {
        class powershareFeatureAllowed(val value: Boolean) : OneOf_OptionalPowershareFeatureAllowed(value)
    }

    sealed class OneOf_OptionalPowershareFeatureEnabled(value: Any) : ProtobufOneofCase(value) {
        class powershareFeatureEnabled(val value: Boolean) : OneOf_OptionalPowershareFeatureEnabled(value)
    }

    sealed class OneOf_OptionalPowershareRequest(value: Any) : ProtobufOneofCase(value) {
        class powershareRequest(val value: Boolean) : OneOf_OptionalPowershareRequest(value)
    }

    sealed class OneOf_OptionalPowershareType(value: Any) : ProtobufOneofCase(value) {
        class powershareType(val value: CarServer_ChargeState.PowershareType) : OneOf_OptionalPowershareType(value)
    }

    sealed class OneOf_OptionalPowershareStatus(value: Any) : ProtobufOneofCase(value) {
        class powershareStatus(val value: CarServer_ChargeState.PowershareStatus) : OneOf_OptionalPowershareStatus(value)
    }

    sealed class OneOf_OptionalPowershareStopReason(value: Any) : ProtobufOneofCase(value) {
        class powershareStopReason(val value: CarServer_ChargeState.PowershareStopReason) : OneOf_OptionalPowershareStopReason(value)
    }

    sealed class OneOf_OptionalPowershareInstantaneousLoadKw(value: Any) : ProtobufOneofCase(value) {
        class powershareInstantaneousLoadKw(val value: Float) : OneOf_OptionalPowershareInstantaneousLoadKw(value)
    }

    sealed class OneOf_OptionalPowershareVehicleEnergyLeftHr(value: Any) : ProtobufOneofCase(value) {
        class powershareVehicleEnergyLeftHr(val value: Int) : OneOf_OptionalPowershareVehicleEnergyLeftHr(value)
    }

    sealed class OneOf_OptionalPowershareSocLimit(value: Any) : ProtobufOneofCase(value) {
        class powershareSocLimit(val value: Int) : OneOf_OptionalPowershareSocLimit(value)
    }

    sealed class OneOf_OptionalOneTimeSocLimit(value: Any) : ProtobufOneofCase(value) {
        class oneTimeSocLimit(val value: Int) : OneOf_OptionalOneTimeSocLimit(value)
    }

    sealed class OneOf_OptionalHomeLocation(value: Any) : ProtobufOneofCase(value) {
        class homeLocation(val value: CarServer_LatLong) : OneOf_OptionalHomeLocation(value)
    }

    sealed class OneOf_OptionalWorkLocation(value: Any) : ProtobufOneofCase(value) {
        class workLocation(val value: CarServer_LatLong) : OneOf_OptionalWorkLocation(value)
    }

    sealed class OneOf_OptionalOutletMaxTimerMinutes(value: Any) : ProtobufOneofCase(value) {
        class outletMaxTimerMinutes(val value: Int) : OneOf_OptionalOutletMaxTimerMinutes(value)
    }

    enum class ScheduledChargingMode(val rawValue: Int) {
        off(0),
        startAt(1),
        departBy(2),
        UNRECOGNIZED(-1),
        ;

        companion object {
            val allCases: List<CarServer_ChargeState.ScheduledChargingMode> = listOf(
                off,
                startAt,
                departBy,
            )

            fun fromRawValue(rawValue: Int): CarServer_ChargeState.ScheduledChargingMode =
                when (rawValue) {
                    0 -> off
                    1 -> startAt
                    2 -> departBy
                    else -> UNRECOGNIZED
                }
        }
    }

    enum class ChargePortColor_E(val rawValue: Int) {
        chargePortColorOff(0),
        chargePortColorRed(1),
        chargePortColorGreen(2),
        chargePortColorBlue(3),
        chargePortColorWhite(4),
        chargePortColorFlashingGreen(5),
        chargePortColorFlashingAmber(6),
        chargePortColorAmber(7),
        chargePortColorRave(8),
        chargePortColorDebug(9),
        chargePortColorFlashingBlue(10),
        UNRECOGNIZED(-1),
        ;

        companion object {
            val allCases: List<CarServer_ChargeState.ChargePortColor_E> = listOf(
                chargePortColorOff,
                chargePortColorRed,
                chargePortColorGreen,
                chargePortColorBlue,
                chargePortColorWhite,
                chargePortColorFlashingGreen,
                chargePortColorFlashingAmber,
                chargePortColorAmber,
                chargePortColorRave,
                chargePortColorDebug,
                chargePortColorFlashingBlue,
            )

            fun fromRawValue(rawValue: Int): CarServer_ChargeState.ChargePortColor_E =
                when (rawValue) {
                    0 -> chargePortColorOff
                    1 -> chargePortColorRed
                    2 -> chargePortColorGreen
                    3 -> chargePortColorBlue
                    4 -> chargePortColorWhite
                    5 -> chargePortColorFlashingGreen
                    6 -> chargePortColorFlashingAmber
                    7 -> chargePortColorAmber
                    8 -> chargePortColorRave
                    9 -> chargePortColorDebug
                    10 -> chargePortColorFlashingBlue
                    else -> UNRECOGNIZED
                }
        }
    }

    enum class ChargeLimitReason(val rawValue: Int) {
        unknown(0),
        none(1),
        evse(2),
        battTempLow(3),
        highSoc(4),
        cabin(5),
        UNRECOGNIZED(-1),
        ;

        companion object {
            val allCases: List<CarServer_ChargeState.ChargeLimitReason> = listOf(
                unknown,
                none,
                evse,
                battTempLow,
                highSoc,
                cabin,
            )

            fun fromRawValue(rawValue: Int): CarServer_ChargeState.ChargeLimitReason =
                when (rawValue) {
                    0 -> unknown
                    1 -> none
                    2 -> evse
                    3 -> battTempLow
                    4 -> highSoc
                    5 -> cabin
                    else -> UNRECOGNIZED
                }
        }
    }

    enum class OutletState(val rawValue: Int) {
        off(0),
        cabinAndBed(1),
        cabin(2),
        UNRECOGNIZED(-1),
        ;

        companion object {
            val allCases: List<CarServer_ChargeState.OutletState> = listOf(
                off,
                cabinAndBed,
                cabin,
            )

            fun fromRawValue(rawValue: Int): CarServer_ChargeState.OutletState =
                when (rawValue) {
                    0 -> off
                    1 -> cabinAndBed
                    2 -> cabin
                    else -> UNRECOGNIZED
                }
        }
    }

    enum class PowerFeedState(val rawValue: Int) {
        off(0),
        cabinAndBed(1),
        cabin(2),
        UNRECOGNIZED(-1),
        ;

        companion object {
            val allCases: List<CarServer_ChargeState.PowerFeedState> = listOf(
                off,
                cabinAndBed,
                cabin,
            )

            fun fromRawValue(rawValue: Int): CarServer_ChargeState.PowerFeedState =
                when (rawValue) {
                    0 -> off
                    1 -> cabinAndBed
                    2 -> cabin
                    else -> UNRECOGNIZED
                }
        }
    }

    enum class PowershareStatus(val rawValue: Int) {
        inactive(0),
        init_(1),
        active(2),
        stopped(3),
        handshaking(4),
        activeReconnectingSoon(5),
        UNRECOGNIZED(-1),
        ;

        companion object {
            val allCases: List<CarServer_ChargeState.PowershareStatus> = listOf(
                inactive,
                init_,
                active,
                stopped,
                handshaking,
                activeReconnectingSoon,
            )

            fun fromRawValue(rawValue: Int): CarServer_ChargeState.PowershareStatus =
                when (rawValue) {
                    0 -> inactive
                    1 -> init_
                    2 -> active
                    3 -> stopped
                    4 -> handshaking
                    5 -> activeReconnectingSoon
                    else -> UNRECOGNIZED
                }
        }
    }

    enum class PowershareType(val rawValue: Int) {
        none(0),
        load(1),
        home(2),
        UNRECOGNIZED(-1),
        ;

        companion object {
            val allCases: List<CarServer_ChargeState.PowershareType> = listOf(
                none,
                load,
                home,
            )

            fun fromRawValue(rawValue: Int): CarServer_ChargeState.PowershareType =
                when (rawValue) {
                    0 -> none
                    1 -> load
                    2 -> home
                    else -> UNRECOGNIZED
                }
        }
    }

    enum class PowershareStopReason(val rawValue: Int) {
        none(0),
        soctooLow(1),
        retry(2),
        fault(3),
        user(4),
        reconnecting(5),
        authentication(6),
        UNRECOGNIZED(-1),
        ;

        companion object {
            val allCases: List<CarServer_ChargeState.PowershareStopReason> = listOf(
                none,
                soctooLow,
                retry,
                fault,
                user,
                reconnecting,
                authentication,
            )

            fun fromRawValue(rawValue: Int): CarServer_ChargeState.PowershareStopReason =
                when (rawValue) {
                    0 -> none
                    1 -> soctooLow
                    2 -> retry
                    3 -> fault
                    4 -> user
                    5 -> reconnecting
                    6 -> authentication
                    else -> UNRECOGNIZED
                }
        }
    }

    class CableType() : GeneratedMessage() {
        constructor(serializedBytes: ByteArray) : this() {
            merge(serializedBytes)
        }

        var type: CarServer_ChargeState.CableType.OneOf_Type?
            get() = this._type
            set(value) {
                this._protobufStore_type(this._protobufCopy_type(value))
                _protobufMutated()
            }

        var sna: CarServer_Void
            get() = (this._type as? CarServer_ChargeState.CableType.OneOf_Type.sna)?.value
                ?: _protobufPending(1, { CarServer_Void() }) { this._protobufStore_type(CarServer_ChargeState.CableType.OneOf_Type.sna(it)) }
            set(value) {
                this.type = CarServer_ChargeState.CableType.OneOf_Type.sna(value)
            }

        var iec: CarServer_Void
            get() = (this._type as? CarServer_ChargeState.CableType.OneOf_Type.iec)?.value
                ?: _protobufPending(2, { CarServer_Void() }) { this._protobufStore_type(CarServer_ChargeState.CableType.OneOf_Type.iec(it)) }
            set(value) {
                this.type = CarServer_ChargeState.CableType.OneOf_Type.iec(value)
            }

        var sae: CarServer_Void
            get() = (this._type as? CarServer_ChargeState.CableType.OneOf_Type.sae)?.value
                ?: _protobufPending(3, { CarServer_Void() }) { this._protobufStore_type(CarServer_ChargeState.CableType.OneOf_Type.sae(it)) }
            set(value) {
                this.type = CarServer_ChargeState.CableType.OneOf_Type.sae(value)
            }

        var gbAc: CarServer_Void
            get() = (this._type as? CarServer_ChargeState.CableType.OneOf_Type.gbAc)?.value
                ?: _protobufPending(4, { CarServer_Void() }) { this._protobufStore_type(CarServer_ChargeState.CableType.OneOf_Type.gbAc(it)) }
            set(value) {
                this.type = CarServer_ChargeState.CableType.OneOf_Type.gbAc(value)
            }

        var gbDc: CarServer_Void
            get() = (this._type as? CarServer_ChargeState.CableType.OneOf_Type.gbDc)?.value
                ?: _protobufPending(5, { CarServer_Void() }) { this._protobufStore_type(CarServer_ChargeState.CableType.OneOf_Type.gbDc(it)) }
            set(value) {
                this.type = CarServer_ChargeState.CableType.OneOf_Type.gbDc(value)
            }

        sealed class OneOf_Type(value: Any) : ProtobufOneofCase(value) {
            class sna(val value: CarServer_Void) : OneOf_Type(value)
            class iec(val value: CarServer_Void) : OneOf_Type(value)
            class sae(val value: CarServer_Void) : OneOf_Type(value)
            class gbAc(val value: CarServer_Void) : OneOf_Type(value)
            class gbDc(val value: CarServer_Void) : OneOf_Type(value)
        }

        private var _type: CarServer_ChargeState.CableType.OneOf_Type? = null

        private fun _protobufStore_type(value: CarServer_ChargeState.CableType.OneOf_Type?) {
            _protobufDropPending(1)
            _protobufDropPending(2)
            _protobufDropPending(3)
            _protobufDropPending(4)
            _protobufDropPending(5)
            this._type = value
        }

        private fun _protobufCopy_type(value: CarServer_ChargeState.CableType.OneOf_Type?): CarServer_ChargeState.CableType.OneOf_Type? =
            when (value) {
                is CarServer_ChargeState.CableType.OneOf_Type.sna -> CarServer_ChargeState.CableType.OneOf_Type.sna(value.value.copy())
                is CarServer_ChargeState.CableType.OneOf_Type.iec -> CarServer_ChargeState.CableType.OneOf_Type.iec(value.value.copy())
                is CarServer_ChargeState.CableType.OneOf_Type.sae -> CarServer_ChargeState.CableType.OneOf_Type.sae(value.value.copy())
                is CarServer_ChargeState.CableType.OneOf_Type.gbAc -> CarServer_ChargeState.CableType.OneOf_Type.gbAc(value.value.copy())
                is CarServer_ChargeState.CableType.OneOf_Type.gbDc -> CarServer_ChargeState.CableType.OneOf_Type.gbDc(value.value.copy())
                else -> value
            }

        override fun decodeMessage(decoder: BinaryDecoder) {
            while (true) {
                when (decoder.nextFieldNumber() ?: return) {
                    1 -> decoder.decodeSingularMessageField((this._type as? CarServer_ChargeState.CableType.OneOf_Type.sna)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ChargeState.CableType.OneOf_Type.sna(it)) }
                    2 -> decoder.decodeSingularMessageField((this._type as? CarServer_ChargeState.CableType.OneOf_Type.iec)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ChargeState.CableType.OneOf_Type.iec(it)) }
                    3 -> decoder.decodeSingularMessageField((this._type as? CarServer_ChargeState.CableType.OneOf_Type.sae)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ChargeState.CableType.OneOf_Type.sae(it)) }
                    4 -> decoder.decodeSingularMessageField((this._type as? CarServer_ChargeState.CableType.OneOf_Type.gbAc)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ChargeState.CableType.OneOf_Type.gbAc(it)) }
                    5 -> decoder.decodeSingularMessageField((this._type as? CarServer_ChargeState.CableType.OneOf_Type.gbDc)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ChargeState.CableType.OneOf_Type.gbDc(it)) }
                }
            }
        }

        override fun traverse(visitor: BinaryEncodingVisitor) {
            (this._type as? CarServer_ChargeState.CableType.OneOf_Type.sna)?.let {
                visitor.visitSingularMessageField(it.value, 1)
            }
            (this._type as? CarServer_ChargeState.CableType.OneOf_Type.iec)?.let {
                visitor.visitSingularMessageField(it.value, 2)
            }
            (this._type as? CarServer_ChargeState.CableType.OneOf_Type.sae)?.let {
                visitor.visitSingularMessageField(it.value, 3)
            }
            (this._type as? CarServer_ChargeState.CableType.OneOf_Type.gbAc)?.let {
                visitor.visitSingularMessageField(it.value, 4)
            }
            (this._type as? CarServer_ChargeState.CableType.OneOf_Type.gbDc)?.let {
                visitor.visitSingularMessageField(it.value, 5)
            }
            visitor.visitUnknown(unknownFields)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CarServer_ChargeState.CableType) return false
            if (this._type != other._type) return false
            return this.unknownFields.contentEquals(other.unknownFields)
        }

        override fun hashCode(): Int {
            var hash = 0
            hash = 31 * hash + (this._type?.hashCode() ?: 0)
            hash = 31 * hash + this.unknownFields.contentHashCode()
            return hash
        }

        override fun copy(): CarServer_ChargeState.CableType {
            val result = CarServer_ChargeState.CableType()
            result._type = this._protobufCopy_type(this._type)
            result.unknownFields = this.unknownFields
            return result
        }

        companion object {
            const val protoMessageName: String = "CarServer.ChargeState.CableType"

            fun with(block: CarServer_ChargeState.CableType.() -> Unit): CarServer_ChargeState.CableType =
                CarServer_ChargeState.CableType().apply(block)
        }
    }

    class ChargerType() : GeneratedMessage() {
        constructor(serializedBytes: ByteArray) : this() {
            merge(serializedBytes)
        }

        var type: CarServer_ChargeState.ChargerType.OneOf_Type?
            get() = this._type
            set(value) {
                this._protobufStore_type(this._protobufCopy_type(value))
                _protobufMutated()
            }

        var sna: CarServer_Void
            get() = (this._type as? CarServer_ChargeState.ChargerType.OneOf_Type.sna)?.value
                ?: _protobufPending(1, { CarServer_Void() }) { this._protobufStore_type(CarServer_ChargeState.ChargerType.OneOf_Type.sna(it)) }
            set(value) {
                this.type = CarServer_ChargeState.ChargerType.OneOf_Type.sna(value)
            }

        var supercharger: CarServer_Void
            get() = (this._type as? CarServer_ChargeState.ChargerType.OneOf_Type.supercharger)?.value
                ?: _protobufPending(2, { CarServer_Void() }) { this._protobufStore_type(CarServer_ChargeState.ChargerType.OneOf_Type.supercharger(it)) }
            set(value) {
                this.type = CarServer_ChargeState.ChargerType.OneOf_Type.supercharger(value)
            }

        var chademo: CarServer_Void
            get() = (this._type as? CarServer_ChargeState.ChargerType.OneOf_Type.chademo)?.value
                ?: _protobufPending(3, { CarServer_Void() }) { this._protobufStore_type(CarServer_ChargeState.ChargerType.OneOf_Type.chademo(it)) }
            set(value) {
                this.type = CarServer_ChargeState.ChargerType.OneOf_Type.chademo(value)
            }

        var gb: CarServer_Void
            get() = (this._type as? CarServer_ChargeState.ChargerType.OneOf_Type.gb)?.value
                ?: _protobufPending(4, { CarServer_Void() }) { this._protobufStore_type(CarServer_ChargeState.ChargerType.OneOf_Type.gb(it)) }
            set(value) {
                this.type = CarServer_ChargeState.ChargerType.OneOf_Type.gb(value)
            }

        var acsingleWireCan: CarServer_Void
            get() = (this._type as? CarServer_ChargeState.ChargerType.OneOf_Type.acsingleWireCan)?.value
                ?: _protobufPending(5, { CarServer_Void() }) { this._protobufStore_type(CarServer_ChargeState.ChargerType.OneOf_Type.acsingleWireCan(it)) }
            set(value) {
                this.type = CarServer_ChargeState.ChargerType.OneOf_Type.acsingleWireCan(value)
            }

        var combo: CarServer_Void
            get() = (this._type as? CarServer_ChargeState.ChargerType.OneOf_Type.combo)?.value
                ?: _protobufPending(6, { CarServer_Void() }) { this._protobufStore_type(CarServer_ChargeState.ChargerType.OneOf_Type.combo(it)) }
            set(value) {
                this.type = CarServer_ChargeState.ChargerType.OneOf_Type.combo(value)
            }

        var mcsingleWireCan: CarServer_Void
            get() = (this._type as? CarServer_ChargeState.ChargerType.OneOf_Type.mcsingleWireCan)?.value
                ?: _protobufPending(7, { CarServer_Void() }) { this._protobufStore_type(CarServer_ChargeState.ChargerType.OneOf_Type.mcsingleWireCan(it)) }
            set(value) {
                this.type = CarServer_ChargeState.ChargerType.OneOf_Type.mcsingleWireCan(value)
            }

        var other: CarServer_Void
            get() = (this._type as? CarServer_ChargeState.ChargerType.OneOf_Type.other)?.value
                ?: _protobufPending(8, { CarServer_Void() }) { this._protobufStore_type(CarServer_ChargeState.ChargerType.OneOf_Type.other(it)) }
            set(value) {
                this.type = CarServer_ChargeState.ChargerType.OneOf_Type.other(value)
            }

        var tesla: CarServer_Void
            get() = (this._type as? CarServer_ChargeState.ChargerType.OneOf_Type.tesla)?.value
                ?: _protobufPending(9, { CarServer_Void() }) { this._protobufStore_type(CarServer_ChargeState.ChargerType.OneOf_Type.tesla(it)) }
            set(value) {
                this.type = CarServer_ChargeState.ChargerType.OneOf_Type.tesla(value)
            }

        sealed class OneOf_Type(value: Any) : ProtobufOneofCase(value) {
            class sna(val value: CarServer_Void) : OneOf_Type(value)
            class supercharger(val value: CarServer_Void) : OneOf_Type(value)
            class chademo(val value: CarServer_Void) : OneOf_Type(value)
            class gb(val value: CarServer_Void) : OneOf_Type(value)
            class acsingleWireCan(val value: CarServer_Void) : OneOf_Type(value)
            class combo(val value: CarServer_Void) : OneOf_Type(value)
            class mcsingleWireCan(val value: CarServer_Void) : OneOf_Type(value)
            class other(val value: CarServer_Void) : OneOf_Type(value)
            class tesla(val value: CarServer_Void) : OneOf_Type(value)
        }

        private var _type: CarServer_ChargeState.ChargerType.OneOf_Type? = null

        private fun _protobufStore_type(value: CarServer_ChargeState.ChargerType.OneOf_Type?) {
            _protobufDropPending(1)
            _protobufDropPending(2)
            _protobufDropPending(3)
            _protobufDropPending(4)
            _protobufDropPending(5)
            _protobufDropPending(6)
            _protobufDropPending(7)
            _protobufDropPending(8)
            _protobufDropPending(9)
            this._type = value
        }

        private fun _protobufCopy_type(value: CarServer_ChargeState.ChargerType.OneOf_Type?): CarServer_ChargeState.ChargerType.OneOf_Type? =
            when (value) {
                is CarServer_ChargeState.ChargerType.OneOf_Type.sna -> CarServer_ChargeState.ChargerType.OneOf_Type.sna(value.value.copy())
                is CarServer_ChargeState.ChargerType.OneOf_Type.supercharger -> CarServer_ChargeState.ChargerType.OneOf_Type.supercharger(value.value.copy())
                is CarServer_ChargeState.ChargerType.OneOf_Type.chademo -> CarServer_ChargeState.ChargerType.OneOf_Type.chademo(value.value.copy())
                is CarServer_ChargeState.ChargerType.OneOf_Type.gb -> CarServer_ChargeState.ChargerType.OneOf_Type.gb(value.value.copy())
                is CarServer_ChargeState.ChargerType.OneOf_Type.acsingleWireCan -> CarServer_ChargeState.ChargerType.OneOf_Type.acsingleWireCan(value.value.copy())
                is CarServer_ChargeState.ChargerType.OneOf_Type.combo -> CarServer_ChargeState.ChargerType.OneOf_Type.combo(value.value.copy())
                is CarServer_ChargeState.ChargerType.OneOf_Type.mcsingleWireCan -> CarServer_ChargeState.ChargerType.OneOf_Type.mcsingleWireCan(value.value.copy())
                is CarServer_ChargeState.ChargerType.OneOf_Type.other -> CarServer_ChargeState.ChargerType.OneOf_Type.other(value.value.copy())
                is CarServer_ChargeState.ChargerType.OneOf_Type.tesla -> CarServer_ChargeState.ChargerType.OneOf_Type.tesla(value.value.copy())
                else -> value
            }

        override fun decodeMessage(decoder: BinaryDecoder) {
            while (true) {
                when (decoder.nextFieldNumber() ?: return) {
                    1 -> decoder.decodeSingularMessageField((this._type as? CarServer_ChargeState.ChargerType.OneOf_Type.sna)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ChargeState.ChargerType.OneOf_Type.sna(it)) }
                    2 -> decoder.decodeSingularMessageField((this._type as? CarServer_ChargeState.ChargerType.OneOf_Type.supercharger)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ChargeState.ChargerType.OneOf_Type.supercharger(it)) }
                    3 -> decoder.decodeSingularMessageField((this._type as? CarServer_ChargeState.ChargerType.OneOf_Type.chademo)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ChargeState.ChargerType.OneOf_Type.chademo(it)) }
                    4 -> decoder.decodeSingularMessageField((this._type as? CarServer_ChargeState.ChargerType.OneOf_Type.gb)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ChargeState.ChargerType.OneOf_Type.gb(it)) }
                    5 -> decoder.decodeSingularMessageField((this._type as? CarServer_ChargeState.ChargerType.OneOf_Type.acsingleWireCan)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ChargeState.ChargerType.OneOf_Type.acsingleWireCan(it)) }
                    6 -> decoder.decodeSingularMessageField((this._type as? CarServer_ChargeState.ChargerType.OneOf_Type.combo)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ChargeState.ChargerType.OneOf_Type.combo(it)) }
                    7 -> decoder.decodeSingularMessageField((this._type as? CarServer_ChargeState.ChargerType.OneOf_Type.mcsingleWireCan)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ChargeState.ChargerType.OneOf_Type.mcsingleWireCan(it)) }
                    8 -> decoder.decodeSingularMessageField((this._type as? CarServer_ChargeState.ChargerType.OneOf_Type.other)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ChargeState.ChargerType.OneOf_Type.other(it)) }
                    9 -> decoder.decodeSingularMessageField((this._type as? CarServer_ChargeState.ChargerType.OneOf_Type.tesla)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ChargeState.ChargerType.OneOf_Type.tesla(it)) }
                }
            }
        }

        override fun traverse(visitor: BinaryEncodingVisitor) {
            (this._type as? CarServer_ChargeState.ChargerType.OneOf_Type.sna)?.let {
                visitor.visitSingularMessageField(it.value, 1)
            }
            (this._type as? CarServer_ChargeState.ChargerType.OneOf_Type.supercharger)?.let {
                visitor.visitSingularMessageField(it.value, 2)
            }
            (this._type as? CarServer_ChargeState.ChargerType.OneOf_Type.chademo)?.let {
                visitor.visitSingularMessageField(it.value, 3)
            }
            (this._type as? CarServer_ChargeState.ChargerType.OneOf_Type.gb)?.let {
                visitor.visitSingularMessageField(it.value, 4)
            }
            (this._type as? CarServer_ChargeState.ChargerType.OneOf_Type.acsingleWireCan)?.let {
                visitor.visitSingularMessageField(it.value, 5)
            }
            (this._type as? CarServer_ChargeState.ChargerType.OneOf_Type.combo)?.let {
                visitor.visitSingularMessageField(it.value, 6)
            }
            (this._type as? CarServer_ChargeState.ChargerType.OneOf_Type.mcsingleWireCan)?.let {
                visitor.visitSingularMessageField(it.value, 7)
            }
            (this._type as? CarServer_ChargeState.ChargerType.OneOf_Type.other)?.let {
                visitor.visitSingularMessageField(it.value, 8)
            }
            (this._type as? CarServer_ChargeState.ChargerType.OneOf_Type.tesla)?.let {
                visitor.visitSingularMessageField(it.value, 9)
            }
            visitor.visitUnknown(unknownFields)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CarServer_ChargeState.ChargerType) return false
            if (this._type != other._type) return false
            return this.unknownFields.contentEquals(other.unknownFields)
        }

        override fun hashCode(): Int {
            var hash = 0
            hash = 31 * hash + (this._type?.hashCode() ?: 0)
            hash = 31 * hash + this.unknownFields.contentHashCode()
            return hash
        }

        override fun copy(): CarServer_ChargeState.ChargerType {
            val result = CarServer_ChargeState.ChargerType()
            result._type = this._protobufCopy_type(this._type)
            result.unknownFields = this.unknownFields
            return result
        }

        companion object {
            const val protoMessageName: String = "CarServer.ChargeState.ChargerType"

            fun with(block: CarServer_ChargeState.ChargerType.() -> Unit): CarServer_ChargeState.ChargerType =
                CarServer_ChargeState.ChargerType().apply(block)
        }
    }

    class ChargingState() : GeneratedMessage() {
        constructor(serializedBytes: ByteArray) : this() {
            merge(serializedBytes)
        }

        var type: CarServer_ChargeState.ChargingState.OneOf_Type?
            get() = this._type
            set(value) {
                this._protobufStore_type(this._protobufCopy_type(value))
                _protobufMutated()
            }

        var unknown: CarServer_Void
            get() = (this._type as? CarServer_ChargeState.ChargingState.OneOf_Type.unknown)?.value
                ?: _protobufPending(1, { CarServer_Void() }) { this._protobufStore_type(CarServer_ChargeState.ChargingState.OneOf_Type.unknown(it)) }
            set(value) {
                this.type = CarServer_ChargeState.ChargingState.OneOf_Type.unknown(value)
            }

        var disconnected: CarServer_Void
            get() = (this._type as? CarServer_ChargeState.ChargingState.OneOf_Type.disconnected)?.value
                ?: _protobufPending(2, { CarServer_Void() }) { this._protobufStore_type(CarServer_ChargeState.ChargingState.OneOf_Type.disconnected(it)) }
            set(value) {
                this.type = CarServer_ChargeState.ChargingState.OneOf_Type.disconnected(value)
            }

        var noPower: CarServer_Void
            get() = (this._type as? CarServer_ChargeState.ChargingState.OneOf_Type.noPower)?.value
                ?: _protobufPending(3, { CarServer_Void() }) { this._protobufStore_type(CarServer_ChargeState.ChargingState.OneOf_Type.noPower(it)) }
            set(value) {
                this.type = CarServer_ChargeState.ChargingState.OneOf_Type.noPower(value)
            }

        var starting: CarServer_Void
            get() = (this._type as? CarServer_ChargeState.ChargingState.OneOf_Type.starting)?.value
                ?: _protobufPending(4, { CarServer_Void() }) { this._protobufStore_type(CarServer_ChargeState.ChargingState.OneOf_Type.starting(it)) }
            set(value) {
                this.type = CarServer_ChargeState.ChargingState.OneOf_Type.starting(value)
            }

        var charging: CarServer_Void
            get() = (this._type as? CarServer_ChargeState.ChargingState.OneOf_Type.charging)?.value
                ?: _protobufPending(5, { CarServer_Void() }) { this._protobufStore_type(CarServer_ChargeState.ChargingState.OneOf_Type.charging(it)) }
            set(value) {
                this.type = CarServer_ChargeState.ChargingState.OneOf_Type.charging(value)
            }

        var complete: CarServer_Void
            get() = (this._type as? CarServer_ChargeState.ChargingState.OneOf_Type.complete)?.value
                ?: _protobufPending(6, { CarServer_Void() }) { this._protobufStore_type(CarServer_ChargeState.ChargingState.OneOf_Type.complete(it)) }
            set(value) {
                this.type = CarServer_ChargeState.ChargingState.OneOf_Type.complete(value)
            }

        var stopped: CarServer_Void
            get() = (this._type as? CarServer_ChargeState.ChargingState.OneOf_Type.stopped)?.value
                ?: _protobufPending(7, { CarServer_Void() }) { this._protobufStore_type(CarServer_ChargeState.ChargingState.OneOf_Type.stopped(it)) }
            set(value) {
                this.type = CarServer_ChargeState.ChargingState.OneOf_Type.stopped(value)
            }

        var calibrating: CarServer_Void
            get() = (this._type as? CarServer_ChargeState.ChargingState.OneOf_Type.calibrating)?.value
                ?: _protobufPending(8, { CarServer_Void() }) { this._protobufStore_type(CarServer_ChargeState.ChargingState.OneOf_Type.calibrating(it)) }
            set(value) {
                this.type = CarServer_ChargeState.ChargingState.OneOf_Type.calibrating(value)
            }

        sealed class OneOf_Type(value: Any) : ProtobufOneofCase(value) {
            class unknown(val value: CarServer_Void) : OneOf_Type(value)
            class disconnected(val value: CarServer_Void) : OneOf_Type(value)
            class noPower(val value: CarServer_Void) : OneOf_Type(value)
            class starting(val value: CarServer_Void) : OneOf_Type(value)
            class charging(val value: CarServer_Void) : OneOf_Type(value)
            class complete(val value: CarServer_Void) : OneOf_Type(value)
            class stopped(val value: CarServer_Void) : OneOf_Type(value)
            class calibrating(val value: CarServer_Void) : OneOf_Type(value)
        }

        private var _type: CarServer_ChargeState.ChargingState.OneOf_Type? = null

        private fun _protobufStore_type(value: CarServer_ChargeState.ChargingState.OneOf_Type?) {
            _protobufDropPending(1)
            _protobufDropPending(2)
            _protobufDropPending(3)
            _protobufDropPending(4)
            _protobufDropPending(5)
            _protobufDropPending(6)
            _protobufDropPending(7)
            _protobufDropPending(8)
            this._type = value
        }

        private fun _protobufCopy_type(value: CarServer_ChargeState.ChargingState.OneOf_Type?): CarServer_ChargeState.ChargingState.OneOf_Type? =
            when (value) {
                is CarServer_ChargeState.ChargingState.OneOf_Type.unknown -> CarServer_ChargeState.ChargingState.OneOf_Type.unknown(value.value.copy())
                is CarServer_ChargeState.ChargingState.OneOf_Type.disconnected -> CarServer_ChargeState.ChargingState.OneOf_Type.disconnected(value.value.copy())
                is CarServer_ChargeState.ChargingState.OneOf_Type.noPower -> CarServer_ChargeState.ChargingState.OneOf_Type.noPower(value.value.copy())
                is CarServer_ChargeState.ChargingState.OneOf_Type.starting -> CarServer_ChargeState.ChargingState.OneOf_Type.starting(value.value.copy())
                is CarServer_ChargeState.ChargingState.OneOf_Type.charging -> CarServer_ChargeState.ChargingState.OneOf_Type.charging(value.value.copy())
                is CarServer_ChargeState.ChargingState.OneOf_Type.complete -> CarServer_ChargeState.ChargingState.OneOf_Type.complete(value.value.copy())
                is CarServer_ChargeState.ChargingState.OneOf_Type.stopped -> CarServer_ChargeState.ChargingState.OneOf_Type.stopped(value.value.copy())
                is CarServer_ChargeState.ChargingState.OneOf_Type.calibrating -> CarServer_ChargeState.ChargingState.OneOf_Type.calibrating(value.value.copy())
                else -> value
            }

        override fun decodeMessage(decoder: BinaryDecoder) {
            while (true) {
                when (decoder.nextFieldNumber() ?: return) {
                    1 -> decoder.decodeSingularMessageField((this._type as? CarServer_ChargeState.ChargingState.OneOf_Type.unknown)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ChargeState.ChargingState.OneOf_Type.unknown(it)) }
                    2 -> decoder.decodeSingularMessageField((this._type as? CarServer_ChargeState.ChargingState.OneOf_Type.disconnected)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ChargeState.ChargingState.OneOf_Type.disconnected(it)) }
                    3 -> decoder.decodeSingularMessageField((this._type as? CarServer_ChargeState.ChargingState.OneOf_Type.noPower)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ChargeState.ChargingState.OneOf_Type.noPower(it)) }
                    4 -> decoder.decodeSingularMessageField((this._type as? CarServer_ChargeState.ChargingState.OneOf_Type.starting)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ChargeState.ChargingState.OneOf_Type.starting(it)) }
                    5 -> decoder.decodeSingularMessageField((this._type as? CarServer_ChargeState.ChargingState.OneOf_Type.charging)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ChargeState.ChargingState.OneOf_Type.charging(it)) }
                    6 -> decoder.decodeSingularMessageField((this._type as? CarServer_ChargeState.ChargingState.OneOf_Type.complete)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ChargeState.ChargingState.OneOf_Type.complete(it)) }
                    7 -> decoder.decodeSingularMessageField((this._type as? CarServer_ChargeState.ChargingState.OneOf_Type.stopped)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ChargeState.ChargingState.OneOf_Type.stopped(it)) }
                    8 -> decoder.decodeSingularMessageField((this._type as? CarServer_ChargeState.ChargingState.OneOf_Type.calibrating)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ChargeState.ChargingState.OneOf_Type.calibrating(it)) }
                }
            }
        }

        override fun traverse(visitor: BinaryEncodingVisitor) {
            (this._type as? CarServer_ChargeState.ChargingState.OneOf_Type.unknown)?.let {
                visitor.visitSingularMessageField(it.value, 1)
            }
            (this._type as? CarServer_ChargeState.ChargingState.OneOf_Type.disconnected)?.let {
                visitor.visitSingularMessageField(it.value, 2)
            }
            (this._type as? CarServer_ChargeState.ChargingState.OneOf_Type.noPower)?.let {
                visitor.visitSingularMessageField(it.value, 3)
            }
            (this._type as? CarServer_ChargeState.ChargingState.OneOf_Type.starting)?.let {
                visitor.visitSingularMessageField(it.value, 4)
            }
            (this._type as? CarServer_ChargeState.ChargingState.OneOf_Type.charging)?.let {
                visitor.visitSingularMessageField(it.value, 5)
            }
            (this._type as? CarServer_ChargeState.ChargingState.OneOf_Type.complete)?.let {
                visitor.visitSingularMessageField(it.value, 6)
            }
            (this._type as? CarServer_ChargeState.ChargingState.OneOf_Type.stopped)?.let {
                visitor.visitSingularMessageField(it.value, 7)
            }
            (this._type as? CarServer_ChargeState.ChargingState.OneOf_Type.calibrating)?.let {
                visitor.visitSingularMessageField(it.value, 8)
            }
            visitor.visitUnknown(unknownFields)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CarServer_ChargeState.ChargingState) return false
            if (this._type != other._type) return false
            return this.unknownFields.contentEquals(other.unknownFields)
        }

        override fun hashCode(): Int {
            var hash = 0
            hash = 31 * hash + (this._type?.hashCode() ?: 0)
            hash = 31 * hash + this.unknownFields.contentHashCode()
            return hash
        }

        override fun copy(): CarServer_ChargeState.ChargingState {
            val result = CarServer_ChargeState.ChargingState()
            result._type = this._protobufCopy_type(this._type)
            result.unknownFields = this.unknownFields
            return result
        }

        companion object {
            const val protoMessageName: String = "CarServer.ChargeState.ChargingState"

            fun with(block: CarServer_ChargeState.ChargingState.() -> Unit): CarServer_ChargeState.ChargingState =
                CarServer_ChargeState.ChargingState().apply(block)
        }
    }

    class ChargerBrand() : GeneratedMessage() {
        constructor(serializedBytes: ByteArray) : this() {
            merge(serializedBytes)
        }

        var type: CarServer_ChargeState.ChargerBrand.OneOf_Type?
            get() = this._type
            set(value) {
                this._protobufStore_type(this._protobufCopy_type(value))
                _protobufMutated()
            }

        var tesla: CarServer_Void
            get() = (this._type as? CarServer_ChargeState.ChargerBrand.OneOf_Type.tesla)?.value
                ?: _protobufPending(1, { CarServer_Void() }) { this._protobufStore_type(CarServer_ChargeState.ChargerBrand.OneOf_Type.tesla(it)) }
            set(value) {
                this.type = CarServer_ChargeState.ChargerBrand.OneOf_Type.tesla(value)
            }

        var sna: CarServer_Void
            get() = (this._type as? CarServer_ChargeState.ChargerBrand.OneOf_Type.sna)?.value
                ?: _protobufPending(2, { CarServer_Void() }) { this._protobufStore_type(CarServer_ChargeState.ChargerBrand.OneOf_Type.sna(it)) }
            set(value) {
                this.type = CarServer_ChargeState.ChargerBrand.OneOf_Type.sna(value)
            }

        sealed class OneOf_Type(value: Any) : ProtobufOneofCase(value) {
            class tesla(val value: CarServer_Void) : OneOf_Type(value)
            class sna(val value: CarServer_Void) : OneOf_Type(value)
        }

        private var _type: CarServer_ChargeState.ChargerBrand.OneOf_Type? = null

        private fun _protobufStore_type(value: CarServer_ChargeState.ChargerBrand.OneOf_Type?) {
            _protobufDropPending(1)
            _protobufDropPending(2)
            this._type = value
        }

        private fun _protobufCopy_type(value: CarServer_ChargeState.ChargerBrand.OneOf_Type?): CarServer_ChargeState.ChargerBrand.OneOf_Type? =
            when (value) {
                is CarServer_ChargeState.ChargerBrand.OneOf_Type.tesla -> CarServer_ChargeState.ChargerBrand.OneOf_Type.tesla(value.value.copy())
                is CarServer_ChargeState.ChargerBrand.OneOf_Type.sna -> CarServer_ChargeState.ChargerBrand.OneOf_Type.sna(value.value.copy())
                else -> value
            }

        override fun decodeMessage(decoder: BinaryDecoder) {
            while (true) {
                when (decoder.nextFieldNumber() ?: return) {
                    1 -> decoder.decodeSingularMessageField((this._type as? CarServer_ChargeState.ChargerBrand.OneOf_Type.tesla)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ChargeState.ChargerBrand.OneOf_Type.tesla(it)) }
                    2 -> decoder.decodeSingularMessageField((this._type as? CarServer_ChargeState.ChargerBrand.OneOf_Type.sna)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ChargeState.ChargerBrand.OneOf_Type.sna(it)) }
                }
            }
        }

        override fun traverse(visitor: BinaryEncodingVisitor) {
            (this._type as? CarServer_ChargeState.ChargerBrand.OneOf_Type.tesla)?.let {
                visitor.visitSingularMessageField(it.value, 1)
            }
            (this._type as? CarServer_ChargeState.ChargerBrand.OneOf_Type.sna)?.let {
                visitor.visitSingularMessageField(it.value, 2)
            }
            visitor.visitUnknown(unknownFields)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CarServer_ChargeState.ChargerBrand) return false
            if (this._type != other._type) return false
            return this.unknownFields.contentEquals(other.unknownFields)
        }

        override fun hashCode(): Int {
            var hash = 0
            hash = 31 * hash + (this._type?.hashCode() ?: 0)
            hash = 31 * hash + this.unknownFields.contentHashCode()
            return hash
        }

        override fun copy(): CarServer_ChargeState.ChargerBrand {
            val result = CarServer_ChargeState.ChargerBrand()
            result._type = this._protobufCopy_type(this._type)
            result.unknownFields = this.unknownFields
            return result
        }

        companion object {
            const val protoMessageName: String = "CarServer.ChargeState.ChargerBrand"

            fun with(block: CarServer_ChargeState.ChargerBrand.() -> Unit): CarServer_ChargeState.ChargerBrand =
                CarServer_ChargeState.ChargerBrand().apply(block)
        }
    }

    private var _chargingState: CarServer_ChargeState.ChargingState? = null
    private var _fastChargerType: CarServer_ChargeState.ChargerType? = null
    private var _fastChargerBrand: CarServer_ChargeState.ChargerBrand? = null
    private var _optionalChargeLimitSoc: CarServer_ChargeState.OneOf_OptionalChargeLimitSoc? = null
    private var _optionalChargeLimitSocStd: CarServer_ChargeState.OneOf_OptionalChargeLimitSocStd? = null
    private var _optionalChargeLimitSocMin: CarServer_ChargeState.OneOf_OptionalChargeLimitSocMin? = null
    private var _optionalChargeLimitSocMax: CarServer_ChargeState.OneOf_OptionalChargeLimitSocMax? = null
    private var _optionalMaxRangeChargeCounter: CarServer_ChargeState.OneOf_OptionalMaxRangeChargeCounter? = null
    private var _optionalFastChargerPresent: CarServer_ChargeState.OneOf_OptionalFastChargerPresent? = null
    private var _optionalBatteryRange: CarServer_ChargeState.OneOf_OptionalBatteryRange? = null
    private var _optionalEstBatteryRange: CarServer_ChargeState.OneOf_OptionalEstBatteryRange? = null
    private var _optionalIdealBatteryRange: CarServer_ChargeState.OneOf_OptionalIdealBatteryRange? = null
    private var _optionalBatteryLevel: CarServer_ChargeState.OneOf_OptionalBatteryLevel? = null
    private var _optionalUsableBatteryLevel: CarServer_ChargeState.OneOf_OptionalUsableBatteryLevel? = null
    private var _optionalChargeEnergyAdded: CarServer_ChargeState.OneOf_OptionalChargeEnergyAdded? = null
    private var _optionalChargeMilesAddedRated: CarServer_ChargeState.OneOf_OptionalChargeMilesAddedRated? = null
    private var _optionalChargeMilesAddedIdeal: CarServer_ChargeState.OneOf_OptionalChargeMilesAddedIdeal? = null
    private var _optionalChargerVoltage: CarServer_ChargeState.OneOf_OptionalChargerVoltage? = null
    private var _optionalChargerPilotCurrent: CarServer_ChargeState.OneOf_OptionalChargerPilotCurrent? = null
    private var _optionalChargerActualCurrent: CarServer_ChargeState.OneOf_OptionalChargerActualCurrent? = null
    private var _optionalChargerPower: CarServer_ChargeState.OneOf_OptionalChargerPower? = null
    private var _optionalMinutesToFullCharge: CarServer_ChargeState.OneOf_OptionalMinutesToFullCharge? = null
    private var _optionalMinutesToChargeLimit: CarServer_ChargeState.OneOf_OptionalMinutesToChargeLimit? = null
    private var _optionalTripCharging: CarServer_ChargeState.OneOf_OptionalTripCharging? = null
    private var _optionalChargeRateMph: CarServer_ChargeState.OneOf_OptionalChargeRateMph? = null
    private var _optionalChargePortDoorOpen: CarServer_ChargeState.OneOf_OptionalChargePortDoorOpen? = null
    private var _connChargeCable: CarServer_ChargeState.CableType? = null
    private var _optionalScheduledChargingStartTime: CarServer_ChargeState.OneOf_OptionalScheduledChargingStartTime? = null
    private var _optionalScheduledChargingPending: CarServer_ChargeState.OneOf_OptionalScheduledChargingPending? = null
    private var _scheduledDepartureTime: Google_Protobuf_Timestamp? = null
    private var _optionalUserChargeEnableRequest: CarServer_ChargeState.OneOf_OptionalUserChargeEnableRequest? = null
    private var _optionalChargeEnableRequest: CarServer_ChargeState.OneOf_OptionalChargeEnableRequest? = null
    private var _optionalChargerPhases: CarServer_ChargeState.OneOf_OptionalChargerPhases? = null
    private var _chargePortLatch: CarServer_ChargePortLatchState? = null
    private var _optionalChargePortColdWeatherMode: CarServer_ChargeState.OneOf_OptionalChargePortColdWeatherMode? = null
    private var _optionalChargeCurrentRequest: CarServer_ChargeState.OneOf_OptionalChargeCurrentRequest? = null
    private var _optionalChargeCurrentRequestMax: CarServer_ChargeState.OneOf_OptionalChargeCurrentRequestMax? = null
    private var _optionalManagedChargingActive: CarServer_ChargeState.OneOf_OptionalManagedChargingActive? = null
    private var _optionalManagedChargingUserCanceled: CarServer_ChargeState.OneOf_OptionalManagedChargingUserCanceled? = null
    private var _optionalManagedChargingStartTime: CarServer_ChargeState.OneOf_OptionalManagedChargingStartTime? = null
    private var _timestamp: Google_Protobuf_Timestamp? = null
    private var _preconditioningTimes: CarServer_PreconditioningTimes? = null
    private var _offPeakChargingTimes: CarServer_OffPeakChargingTimes? = null
    private var _optionalOffPeakHoursEndTime: CarServer_ChargeState.OneOf_OptionalOffPeakHoursEndTime? = null
    private var _optionalScheduledChargingMode: CarServer_ChargeState.OneOf_OptionalScheduledChargingMode? = null
    private var _optionalChargingAmps: CarServer_ChargeState.OneOf_OptionalChargingAmps? = null
    private var _optionalScheduledChargingStartTimeMinutes: CarServer_ChargeState.OneOf_OptionalScheduledChargingStartTimeMinutes? = null
    private var _optionalScheduledDepartureTimeMinutes: CarServer_ChargeState.OneOf_OptionalScheduledDepartureTimeMinutes? = null
    private var _optionalPreconditioningEnabled: CarServer_ChargeState.OneOf_OptionalPreconditioningEnabled? = null
    private var _optionalScheduledChargingStartTimeApp: CarServer_ChargeState.OneOf_OptionalScheduledChargingStartTimeApp? = null
    private var _optionalSuperchargerSessionTripPlanner: CarServer_ChargeState.OneOf_OptionalSuperchargerSessionTripPlanner? = null
    private var _optionalChargePortColor: CarServer_ChargeState.OneOf_OptionalChargePortColor? = null
    private var _optionalChargeRateMphFloat: CarServer_ChargeState.OneOf_OptionalChargeRateMphFloat? = null
    private var _optionalChargeLimitReason: CarServer_ChargeState.OneOf_OptionalChargeLimitReason? = null
    private var _managedChargingState: CarServer_ManagedChargingState? = null
    private var _optionalChargeCableUnlatched: CarServer_ChargeState.OneOf_OptionalChargeCableUnlatched? = null
    private var _optionalOutletState: CarServer_ChargeState.OneOf_OptionalOutletState? = null
    private var _optionalPowerFeedState: CarServer_ChargeState.OneOf_OptionalPowerFeedState? = null
    private var _optionOutletSocLimit: CarServer_ChargeState.OneOf_OptionOutletSocLimit? = null
    private var _optionPowerFeedSocLimit: CarServer_ChargeState.OneOf_OptionPowerFeedSocLimit? = null
    private var _optionOutletTimeRemaining: CarServer_ChargeState.OneOf_OptionOutletTimeRemaining? = null
    private var _optionPowerFeedTimeRemaining: CarServer_ChargeState.OneOf_OptionPowerFeedTimeRemaining? = null
    private var _optionalPowershareFeatureAllowed: CarServer_ChargeState.OneOf_OptionalPowershareFeatureAllowed? = null
    private var _optionalPowershareFeatureEnabled: CarServer_ChargeState.OneOf_OptionalPowershareFeatureEnabled? = null
    private var _optionalPowershareRequest: CarServer_ChargeState.OneOf_OptionalPowershareRequest? = null
    private var _optionalPowershareType: CarServer_ChargeState.OneOf_OptionalPowershareType? = null
    private var _optionalPowershareStatus: CarServer_ChargeState.OneOf_OptionalPowershareStatus? = null
    private var _optionalPowershareStopReason: CarServer_ChargeState.OneOf_OptionalPowershareStopReason? = null
    private var _optionalPowershareInstantaneousLoadKw: CarServer_ChargeState.OneOf_OptionalPowershareInstantaneousLoadKw? = null
    private var _optionalPowershareVehicleEnergyLeftHr: CarServer_ChargeState.OneOf_OptionalPowershareVehicleEnergyLeftHr? = null
    private var _optionalPowershareSocLimit: CarServer_ChargeState.OneOf_OptionalPowershareSocLimit? = null
    private var _optionalOneTimeSocLimit: CarServer_ChargeState.OneOf_OptionalOneTimeSocLimit? = null
    private var _optionalHomeLocation: CarServer_ChargeState.OneOf_OptionalHomeLocation? = null
    private var _optionalWorkLocation: CarServer_ChargeState.OneOf_OptionalWorkLocation? = null
    private var _optionalOutletMaxTimerMinutes: CarServer_ChargeState.OneOf_OptionalOutletMaxTimerMinutes? = null

    private fun _protobufStore_optionalScheduledChargingMode(value: CarServer_ChargeState.OneOf_OptionalScheduledChargingMode?) {
        _protobufForgetUnrecognized(148)
        this._optionalScheduledChargingMode = value
    }

    private fun _protobufStore_optionalChargePortColor(value: CarServer_ChargeState.OneOf_OptionalChargePortColor?) {
        _protobufForgetUnrecognized(155)
        this._optionalChargePortColor = value
    }

    private fun _protobufStore_optionalChargeLimitReason(value: CarServer_ChargeState.OneOf_OptionalChargeLimitReason?) {
        _protobufForgetUnrecognized(157)
        this._optionalChargeLimitReason = value
    }

    private fun _protobufStore_optionalOutletState(value: CarServer_ChargeState.OneOf_OptionalOutletState?) {
        _protobufForgetUnrecognized(160)
        this._optionalOutletState = value
    }

    private fun _protobufStore_optionalPowerFeedState(value: CarServer_ChargeState.OneOf_OptionalPowerFeedState?) {
        _protobufForgetUnrecognized(161)
        this._optionalPowerFeedState = value
    }

    private fun _protobufStore_optionalPowershareType(value: CarServer_ChargeState.OneOf_OptionalPowershareType?) {
        _protobufForgetUnrecognized(169)
        this._optionalPowershareType = value
    }

    private fun _protobufStore_optionalPowershareStatus(value: CarServer_ChargeState.OneOf_OptionalPowershareStatus?) {
        _protobufForgetUnrecognized(170)
        this._optionalPowershareStatus = value
    }

    private fun _protobufStore_optionalPowershareStopReason(value: CarServer_ChargeState.OneOf_OptionalPowershareStopReason?) {
        _protobufForgetUnrecognized(171)
        this._optionalPowershareStopReason = value
    }

    private fun _protobufStore_optionalHomeLocation(value: CarServer_ChargeState.OneOf_OptionalHomeLocation?) {
        _protobufDropPending(176)
        this._optionalHomeLocation = value
    }

    private fun _protobufCopy_optionalHomeLocation(value: CarServer_ChargeState.OneOf_OptionalHomeLocation?): CarServer_ChargeState.OneOf_OptionalHomeLocation? =
        when (value) {
            is CarServer_ChargeState.OneOf_OptionalHomeLocation.homeLocation -> CarServer_ChargeState.OneOf_OptionalHomeLocation.homeLocation(value.value.copy())
            else -> value
        }

    private fun _protobufStore_optionalWorkLocation(value: CarServer_ChargeState.OneOf_OptionalWorkLocation?) {
        _protobufDropPending(177)
        this._optionalWorkLocation = value
    }

    private fun _protobufCopy_optionalWorkLocation(value: CarServer_ChargeState.OneOf_OptionalWorkLocation?): CarServer_ChargeState.OneOf_OptionalWorkLocation? =
        when (value) {
            is CarServer_ChargeState.OneOf_OptionalWorkLocation.workLocation -> CarServer_ChargeState.OneOf_OptionalWorkLocation.workLocation(value.value.copy())
            else -> value
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularMessageField(this._chargingState) { CarServer_ChargeState.ChargingState() }?.let {
                    _protobufDropPending(1)
                    this._chargingState = it
                }
                2 -> decoder.decodeSingularMessageField(this._fastChargerType) { CarServer_ChargeState.ChargerType() }?.let {
                    _protobufDropPending(2)
                    this._fastChargerType = it
                }
                3 -> decoder.decodeSingularMessageField(this._fastChargerBrand) { CarServer_ChargeState.ChargerBrand() }?.let {
                    _protobufDropPending(3)
                    this._fastChargerBrand = it
                }
                28 -> decoder.decodeSingularMessageField(this._connChargeCable) { CarServer_ChargeState.CableType() }?.let {
                    _protobufDropPending(28)
                    this._connChargeCable = it
                }
                31 -> decoder.decodeSingularMessageField(this._scheduledDepartureTime) { Google_Protobuf_Timestamp() }?.let {
                    _protobufDropPending(31)
                    this._scheduledDepartureTime = it
                }
                35 -> decoder.decodeSingularMessageField(this._chargePortLatch) { CarServer_ChargePortLatchState() }?.let {
                    _protobufDropPending(35)
                    this._chargePortLatch = it
                }
                44 -> decoder.decodeSingularMessageField(this._timestamp) { Google_Protobuf_Timestamp() }?.let {
                    _protobufDropPending(44)
                    this._timestamp = it
                }
                45 -> decoder.decodeSingularMessageField(this._preconditioningTimes) { CarServer_PreconditioningTimes() }?.let {
                    _protobufDropPending(45)
                    this._preconditioningTimes = it
                }
                46 -> decoder.decodeSingularMessageField(this._offPeakChargingTimes) { CarServer_OffPeakChargingTimes() }?.let {
                    _protobufDropPending(46)
                    this._offPeakChargingTimes = it
                }
                104 -> decoder.decodeSingularInt32Field()?.let { this._optionalChargeLimitSoc = CarServer_ChargeState.OneOf_OptionalChargeLimitSoc.chargeLimitSoc(it) }
                105 -> decoder.decodeSingularInt32Field()?.let { this._optionalChargeLimitSocStd = CarServer_ChargeState.OneOf_OptionalChargeLimitSocStd.chargeLimitSocStd(it) }
                106 -> decoder.decodeSingularInt32Field()?.let { this._optionalChargeLimitSocMin = CarServer_ChargeState.OneOf_OptionalChargeLimitSocMin.chargeLimitSocMin(it) }
                107 -> decoder.decodeSingularInt32Field()?.let { this._optionalChargeLimitSocMax = CarServer_ChargeState.OneOf_OptionalChargeLimitSocMax.chargeLimitSocMax(it) }
                109 -> decoder.decodeSingularInt32Field()?.let { this._optionalMaxRangeChargeCounter = CarServer_ChargeState.OneOf_OptionalMaxRangeChargeCounter.maxRangeChargeCounter(it) }
                110 -> decoder.decodeSingularBoolField()?.let { this._optionalFastChargerPresent = CarServer_ChargeState.OneOf_OptionalFastChargerPresent.fastChargerPresent(it) }
                111 -> decoder.decodeSingularFloatField()?.let { this._optionalBatteryRange = CarServer_ChargeState.OneOf_OptionalBatteryRange.batteryRange(it) }
                112 -> decoder.decodeSingularFloatField()?.let { this._optionalEstBatteryRange = CarServer_ChargeState.OneOf_OptionalEstBatteryRange.estBatteryRange(it) }
                113 -> decoder.decodeSingularFloatField()?.let { this._optionalIdealBatteryRange = CarServer_ChargeState.OneOf_OptionalIdealBatteryRange.idealBatteryRange(it) }
                114 -> decoder.decodeSingularInt32Field()?.let { this._optionalBatteryLevel = CarServer_ChargeState.OneOf_OptionalBatteryLevel.batteryLevel(it) }
                115 -> decoder.decodeSingularInt32Field()?.let { this._optionalUsableBatteryLevel = CarServer_ChargeState.OneOf_OptionalUsableBatteryLevel.usableBatteryLevel(it) }
                116 -> decoder.decodeSingularFloatField()?.let { this._optionalChargeEnergyAdded = CarServer_ChargeState.OneOf_OptionalChargeEnergyAdded.chargeEnergyAdded(it) }
                117 -> decoder.decodeSingularFloatField()?.let { this._optionalChargeMilesAddedRated = CarServer_ChargeState.OneOf_OptionalChargeMilesAddedRated.chargeMilesAddedRated(it) }
                118 -> decoder.decodeSingularFloatField()?.let { this._optionalChargeMilesAddedIdeal = CarServer_ChargeState.OneOf_OptionalChargeMilesAddedIdeal.chargeMilesAddedIdeal(it) }
                119 -> decoder.decodeSingularInt32Field()?.let { this._optionalChargerVoltage = CarServer_ChargeState.OneOf_OptionalChargerVoltage.chargerVoltage(it) }
                120 -> decoder.decodeSingularInt32Field()?.let { this._optionalChargerPilotCurrent = CarServer_ChargeState.OneOf_OptionalChargerPilotCurrent.chargerPilotCurrent(it) }
                121 -> decoder.decodeSingularInt32Field()?.let { this._optionalChargerActualCurrent = CarServer_ChargeState.OneOf_OptionalChargerActualCurrent.chargerActualCurrent(it) }
                122 -> decoder.decodeSingularInt32Field()?.let { this._optionalChargerPower = CarServer_ChargeState.OneOf_OptionalChargerPower.chargerPower(it) }
                123 -> decoder.decodeSingularInt32Field()?.let { this._optionalMinutesToFullCharge = CarServer_ChargeState.OneOf_OptionalMinutesToFullCharge.minutesToFullCharge(it) }
                125 -> decoder.decodeSingularBoolField()?.let { this._optionalTripCharging = CarServer_ChargeState.OneOf_OptionalTripCharging.tripCharging(it) }
                126 -> decoder.decodeSingularInt32Field()?.let { this._optionalChargeRateMph = CarServer_ChargeState.OneOf_OptionalChargeRateMph.chargeRateMph(it) }
                127 -> decoder.decodeSingularBoolField()?.let { this._optionalChargePortDoorOpen = CarServer_ChargeState.OneOf_OptionalChargePortDoorOpen.chargePortDoorOpen(it) }
                129 -> decoder.decodeSingularUInt64Field()?.let { this._optionalScheduledChargingStartTime = CarServer_ChargeState.OneOf_OptionalScheduledChargingStartTime.scheduledChargingStartTime(it) }
                130 -> decoder.decodeSingularBoolField()?.let { this._optionalScheduledChargingPending = CarServer_ChargeState.OneOf_OptionalScheduledChargingPending.scheduledChargingPending(it) }
                132 -> decoder.decodeSingularBoolField()?.let { this._optionalUserChargeEnableRequest = CarServer_ChargeState.OneOf_OptionalUserChargeEnableRequest.userChargeEnableRequest(it) }
                133 -> decoder.decodeSingularBoolField()?.let { this._optionalChargeEnableRequest = CarServer_ChargeState.OneOf_OptionalChargeEnableRequest.chargeEnableRequest(it) }
                134 -> decoder.decodeSingularInt32Field()?.let { this._optionalChargerPhases = CarServer_ChargeState.OneOf_OptionalChargerPhases.chargerPhases(it) }
                136 -> decoder.decodeSingularBoolField()?.let { this._optionalChargePortColdWeatherMode = CarServer_ChargeState.OneOf_OptionalChargePortColdWeatherMode.chargePortColdWeatherMode(it) }
                137 -> decoder.decodeSingularInt32Field()?.let { this._optionalChargeCurrentRequest = CarServer_ChargeState.OneOf_OptionalChargeCurrentRequest.chargeCurrentRequest(it) }
                138 -> decoder.decodeSingularInt32Field()?.let { this._optionalChargeCurrentRequestMax = CarServer_ChargeState.OneOf_OptionalChargeCurrentRequestMax.chargeCurrentRequestMax(it) }
                139 -> decoder.decodeSingularBoolField()?.let { this._optionalManagedChargingActive = CarServer_ChargeState.OneOf_OptionalManagedChargingActive.managedChargingActive(it) }
                140 -> decoder.decodeSingularBoolField()?.let { this._optionalManagedChargingUserCanceled = CarServer_ChargeState.OneOf_OptionalManagedChargingUserCanceled.managedChargingUserCanceled(it) }
                141 -> decoder.decodeSingularUInt64Field()?.let { this._optionalManagedChargingStartTime = CarServer_ChargeState.OneOf_OptionalManagedChargingStartTime.managedChargingStartTime(it) }
                142 -> decoder.decodeSingularInt32Field()?.let { this._optionalMinutesToChargeLimit = CarServer_ChargeState.OneOf_OptionalMinutesToChargeLimit.minutesToChargeLimit(it) }
                147 -> decoder.decodeSingularUInt32Field()?.let { this._optionalOffPeakHoursEndTime = CarServer_ChargeState.OneOf_OptionalOffPeakHoursEndTime.offPeakHoursEndTime(it) }
                148 -> decoder.decodeSingularOpenEnumField(CarServer_ChargeState.ScheduledChargingMode.UNRECOGNIZED) { CarServer_ChargeState.ScheduledChargingMode.fromRawValue(it) }
                    ?.let { this._protobufStore_optionalScheduledChargingMode(CarServer_ChargeState.OneOf_OptionalScheduledChargingMode.scheduledChargingMode(it)) }
                149 -> decoder.decodeSingularInt32Field()?.let { this._optionalChargingAmps = CarServer_ChargeState.OneOf_OptionalChargingAmps.chargingAmps(it) }
                150 -> decoder.decodeSingularUInt32Field()?.let { this._optionalScheduledChargingStartTimeMinutes = CarServer_ChargeState.OneOf_OptionalScheduledChargingStartTimeMinutes.scheduledChargingStartTimeMinutes(it) }
                151 -> decoder.decodeSingularUInt32Field()?.let { this._optionalScheduledDepartureTimeMinutes = CarServer_ChargeState.OneOf_OptionalScheduledDepartureTimeMinutes.scheduledDepartureTimeMinutes(it) }
                152 -> decoder.decodeSingularBoolField()?.let { this._optionalPreconditioningEnabled = CarServer_ChargeState.OneOf_OptionalPreconditioningEnabled.preconditioningEnabled(it) }
                153 -> decoder.decodeSingularSInt32Field()?.let { this._optionalScheduledChargingStartTimeApp = CarServer_ChargeState.OneOf_OptionalScheduledChargingStartTimeApp.scheduledChargingStartTimeApp(it) }
                154 -> decoder.decodeSingularBoolField()?.let { this._optionalSuperchargerSessionTripPlanner = CarServer_ChargeState.OneOf_OptionalSuperchargerSessionTripPlanner.superchargerSessionTripPlanner(it) }
                155 -> decoder.decodeSingularOpenEnumField(CarServer_ChargeState.ChargePortColor_E.UNRECOGNIZED) { CarServer_ChargeState.ChargePortColor_E.fromRawValue(it) }
                    ?.let { this._protobufStore_optionalChargePortColor(CarServer_ChargeState.OneOf_OptionalChargePortColor.chargePortColor(it)) }
                156 -> decoder.decodeSingularFloatField()?.let { this._optionalChargeRateMphFloat = CarServer_ChargeState.OneOf_OptionalChargeRateMphFloat.chargeRateMphFloat(it) }
                157 -> decoder.decodeSingularOpenEnumField(CarServer_ChargeState.ChargeLimitReason.UNRECOGNIZED) { CarServer_ChargeState.ChargeLimitReason.fromRawValue(it) }
                    ?.let { this._protobufStore_optionalChargeLimitReason(CarServer_ChargeState.OneOf_OptionalChargeLimitReason.chargeLimitReason(it)) }
                158 -> decoder.decodeSingularMessageField(this._managedChargingState) { CarServer_ManagedChargingState() }?.let {
                    _protobufDropPending(158)
                    this._managedChargingState = it
                }
                159 -> decoder.decodeSingularBoolField()?.let { this._optionalChargeCableUnlatched = CarServer_ChargeState.OneOf_OptionalChargeCableUnlatched.chargeCableUnlatched(it) }
                160 -> decoder.decodeSingularOpenEnumField(CarServer_ChargeState.OutletState.UNRECOGNIZED) { CarServer_ChargeState.OutletState.fromRawValue(it) }
                    ?.let { this._protobufStore_optionalOutletState(CarServer_ChargeState.OneOf_OptionalOutletState.outletState(it)) }
                161 -> decoder.decodeSingularOpenEnumField(CarServer_ChargeState.PowerFeedState.UNRECOGNIZED) { CarServer_ChargeState.PowerFeedState.fromRawValue(it) }
                    ?.let { this._protobufStore_optionalPowerFeedState(CarServer_ChargeState.OneOf_OptionalPowerFeedState.powerFeedState(it)) }
                162 -> decoder.decodeSingularInt32Field()?.let { this._optionOutletSocLimit = CarServer_ChargeState.OneOf_OptionOutletSocLimit.outletSocLimit(it) }
                163 -> decoder.decodeSingularInt32Field()?.let { this._optionPowerFeedSocLimit = CarServer_ChargeState.OneOf_OptionPowerFeedSocLimit.powerFeedSocLimit(it) }
                164 -> decoder.decodeSingularInt64Field()?.let { this._optionOutletTimeRemaining = CarServer_ChargeState.OneOf_OptionOutletTimeRemaining.outletTimeRemaining(it) }
                165 -> decoder.decodeSingularInt64Field()?.let { this._optionPowerFeedTimeRemaining = CarServer_ChargeState.OneOf_OptionPowerFeedTimeRemaining.powerFeedTimeRemaining(it) }
                166 -> decoder.decodeSingularBoolField()?.let { this._optionalPowershareFeatureAllowed = CarServer_ChargeState.OneOf_OptionalPowershareFeatureAllowed.powershareFeatureAllowed(it) }
                167 -> decoder.decodeSingularBoolField()?.let { this._optionalPowershareFeatureEnabled = CarServer_ChargeState.OneOf_OptionalPowershareFeatureEnabled.powershareFeatureEnabled(it) }
                168 -> decoder.decodeSingularBoolField()?.let { this._optionalPowershareRequest = CarServer_ChargeState.OneOf_OptionalPowershareRequest.powershareRequest(it) }
                169 -> decoder.decodeSingularOpenEnumField(CarServer_ChargeState.PowershareType.UNRECOGNIZED) { CarServer_ChargeState.PowershareType.fromRawValue(it) }
                    ?.let { this._protobufStore_optionalPowershareType(CarServer_ChargeState.OneOf_OptionalPowershareType.powershareType(it)) }
                170 -> decoder.decodeSingularOpenEnumField(CarServer_ChargeState.PowershareStatus.UNRECOGNIZED) { CarServer_ChargeState.PowershareStatus.fromRawValue(it) }
                    ?.let { this._protobufStore_optionalPowershareStatus(CarServer_ChargeState.OneOf_OptionalPowershareStatus.powershareStatus(it)) }
                171 -> decoder.decodeSingularOpenEnumField(CarServer_ChargeState.PowershareStopReason.UNRECOGNIZED) { CarServer_ChargeState.PowershareStopReason.fromRawValue(it) }
                    ?.let { this._protobufStore_optionalPowershareStopReason(CarServer_ChargeState.OneOf_OptionalPowershareStopReason.powershareStopReason(it)) }
                172 -> decoder.decodeSingularFloatField()?.let { this._optionalPowershareInstantaneousLoadKw = CarServer_ChargeState.OneOf_OptionalPowershareInstantaneousLoadKw.powershareInstantaneousLoadKw(it) }
                173 -> decoder.decodeSingularInt32Field()?.let { this._optionalPowershareVehicleEnergyLeftHr = CarServer_ChargeState.OneOf_OptionalPowershareVehicleEnergyLeftHr.powershareVehicleEnergyLeftHr(it) }
                174 -> decoder.decodeSingularInt32Field()?.let { this._optionalPowershareSocLimit = CarServer_ChargeState.OneOf_OptionalPowershareSocLimit.powershareSocLimit(it) }
                175 -> decoder.decodeSingularInt32Field()?.let { this._optionalOneTimeSocLimit = CarServer_ChargeState.OneOf_OptionalOneTimeSocLimit.oneTimeSocLimit(it) }
                176 -> decoder.decodeSingularMessageField((this._optionalHomeLocation as? CarServer_ChargeState.OneOf_OptionalHomeLocation.homeLocation)?.value) { CarServer_LatLong() }
                    ?.let { this._protobufStore_optionalHomeLocation(CarServer_ChargeState.OneOf_OptionalHomeLocation.homeLocation(it)) }
                177 -> decoder.decodeSingularMessageField((this._optionalWorkLocation as? CarServer_ChargeState.OneOf_OptionalWorkLocation.workLocation)?.value) { CarServer_LatLong() }
                    ?.let { this._protobufStore_optionalWorkLocation(CarServer_ChargeState.OneOf_OptionalWorkLocation.workLocation(it)) }
                178 -> decoder.decodeSingularInt32Field()?.let { this._optionalOutletMaxTimerMinutes = CarServer_ChargeState.OneOf_OptionalOutletMaxTimerMinutes.outletMaxTimerMinutes(it) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        this._chargingState?.let {
            visitor.visitSingularMessageField(it, 1)
        }
        this._fastChargerType?.let {
            visitor.visitSingularMessageField(it, 2)
        }
        this._fastChargerBrand?.let {
            visitor.visitSingularMessageField(it, 3)
        }
        this._connChargeCable?.let {
            visitor.visitSingularMessageField(it, 28)
        }
        this._scheduledDepartureTime?.let {
            visitor.visitSingularMessageField(it, 31)
        }
        this._chargePortLatch?.let {
            visitor.visitSingularMessageField(it, 35)
        }
        this._timestamp?.let {
            visitor.visitSingularMessageField(it, 44)
        }
        this._preconditioningTimes?.let {
            visitor.visitSingularMessageField(it, 45)
        }
        this._offPeakChargingTimes?.let {
            visitor.visitSingularMessageField(it, 46)
        }
        (this._optionalChargeLimitSoc as? CarServer_ChargeState.OneOf_OptionalChargeLimitSoc.chargeLimitSoc)?.let {
            visitor.visitSingularInt32Field(it.value, 104)
        }
        (this._optionalChargeLimitSocStd as? CarServer_ChargeState.OneOf_OptionalChargeLimitSocStd.chargeLimitSocStd)?.let {
            visitor.visitSingularInt32Field(it.value, 105)
        }
        (this._optionalChargeLimitSocMin as? CarServer_ChargeState.OneOf_OptionalChargeLimitSocMin.chargeLimitSocMin)?.let {
            visitor.visitSingularInt32Field(it.value, 106)
        }
        (this._optionalChargeLimitSocMax as? CarServer_ChargeState.OneOf_OptionalChargeLimitSocMax.chargeLimitSocMax)?.let {
            visitor.visitSingularInt32Field(it.value, 107)
        }
        (this._optionalMaxRangeChargeCounter as? CarServer_ChargeState.OneOf_OptionalMaxRangeChargeCounter.maxRangeChargeCounter)?.let {
            visitor.visitSingularInt32Field(it.value, 109)
        }
        (this._optionalFastChargerPresent as? CarServer_ChargeState.OneOf_OptionalFastChargerPresent.fastChargerPresent)?.let {
            visitor.visitSingularBoolField(it.value, 110)
        }
        (this._optionalBatteryRange as? CarServer_ChargeState.OneOf_OptionalBatteryRange.batteryRange)?.let {
            visitor.visitSingularFloatField(it.value, 111)
        }
        (this._optionalEstBatteryRange as? CarServer_ChargeState.OneOf_OptionalEstBatteryRange.estBatteryRange)?.let {
            visitor.visitSingularFloatField(it.value, 112)
        }
        (this._optionalIdealBatteryRange as? CarServer_ChargeState.OneOf_OptionalIdealBatteryRange.idealBatteryRange)?.let {
            visitor.visitSingularFloatField(it.value, 113)
        }
        (this._optionalBatteryLevel as? CarServer_ChargeState.OneOf_OptionalBatteryLevel.batteryLevel)?.let {
            visitor.visitSingularInt32Field(it.value, 114)
        }
        (this._optionalUsableBatteryLevel as? CarServer_ChargeState.OneOf_OptionalUsableBatteryLevel.usableBatteryLevel)?.let {
            visitor.visitSingularInt32Field(it.value, 115)
        }
        (this._optionalChargeEnergyAdded as? CarServer_ChargeState.OneOf_OptionalChargeEnergyAdded.chargeEnergyAdded)?.let {
            visitor.visitSingularFloatField(it.value, 116)
        }
        (this._optionalChargeMilesAddedRated as? CarServer_ChargeState.OneOf_OptionalChargeMilesAddedRated.chargeMilesAddedRated)?.let {
            visitor.visitSingularFloatField(it.value, 117)
        }
        (this._optionalChargeMilesAddedIdeal as? CarServer_ChargeState.OneOf_OptionalChargeMilesAddedIdeal.chargeMilesAddedIdeal)?.let {
            visitor.visitSingularFloatField(it.value, 118)
        }
        (this._optionalChargerVoltage as? CarServer_ChargeState.OneOf_OptionalChargerVoltage.chargerVoltage)?.let {
            visitor.visitSingularInt32Field(it.value, 119)
        }
        (this._optionalChargerPilotCurrent as? CarServer_ChargeState.OneOf_OptionalChargerPilotCurrent.chargerPilotCurrent)?.let {
            visitor.visitSingularInt32Field(it.value, 120)
        }
        (this._optionalChargerActualCurrent as? CarServer_ChargeState.OneOf_OptionalChargerActualCurrent.chargerActualCurrent)?.let {
            visitor.visitSingularInt32Field(it.value, 121)
        }
        (this._optionalChargerPower as? CarServer_ChargeState.OneOf_OptionalChargerPower.chargerPower)?.let {
            visitor.visitSingularInt32Field(it.value, 122)
        }
        (this._optionalMinutesToFullCharge as? CarServer_ChargeState.OneOf_OptionalMinutesToFullCharge.minutesToFullCharge)?.let {
            visitor.visitSingularInt32Field(it.value, 123)
        }
        (this._optionalTripCharging as? CarServer_ChargeState.OneOf_OptionalTripCharging.tripCharging)?.let {
            visitor.visitSingularBoolField(it.value, 125)
        }
        (this._optionalChargeRateMph as? CarServer_ChargeState.OneOf_OptionalChargeRateMph.chargeRateMph)?.let {
            visitor.visitSingularInt32Field(it.value, 126)
        }
        (this._optionalChargePortDoorOpen as? CarServer_ChargeState.OneOf_OptionalChargePortDoorOpen.chargePortDoorOpen)?.let {
            visitor.visitSingularBoolField(it.value, 127)
        }
        (this._optionalScheduledChargingStartTime as? CarServer_ChargeState.OneOf_OptionalScheduledChargingStartTime.scheduledChargingStartTime)?.let {
            visitor.visitSingularUInt64Field(it.value, 129)
        }
        (this._optionalScheduledChargingPending as? CarServer_ChargeState.OneOf_OptionalScheduledChargingPending.scheduledChargingPending)?.let {
            visitor.visitSingularBoolField(it.value, 130)
        }
        (this._optionalUserChargeEnableRequest as? CarServer_ChargeState.OneOf_OptionalUserChargeEnableRequest.userChargeEnableRequest)?.let {
            visitor.visitSingularBoolField(it.value, 132)
        }
        (this._optionalChargeEnableRequest as? CarServer_ChargeState.OneOf_OptionalChargeEnableRequest.chargeEnableRequest)?.let {
            visitor.visitSingularBoolField(it.value, 133)
        }
        (this._optionalChargerPhases as? CarServer_ChargeState.OneOf_OptionalChargerPhases.chargerPhases)?.let {
            visitor.visitSingularInt32Field(it.value, 134)
        }
        (this._optionalChargePortColdWeatherMode as? CarServer_ChargeState.OneOf_OptionalChargePortColdWeatherMode.chargePortColdWeatherMode)?.let {
            visitor.visitSingularBoolField(it.value, 136)
        }
        (this._optionalChargeCurrentRequest as? CarServer_ChargeState.OneOf_OptionalChargeCurrentRequest.chargeCurrentRequest)?.let {
            visitor.visitSingularInt32Field(it.value, 137)
        }
        (this._optionalChargeCurrentRequestMax as? CarServer_ChargeState.OneOf_OptionalChargeCurrentRequestMax.chargeCurrentRequestMax)?.let {
            visitor.visitSingularInt32Field(it.value, 138)
        }
        (this._optionalManagedChargingActive as? CarServer_ChargeState.OneOf_OptionalManagedChargingActive.managedChargingActive)?.let {
            visitor.visitSingularBoolField(it.value, 139)
        }
        (this._optionalManagedChargingUserCanceled as? CarServer_ChargeState.OneOf_OptionalManagedChargingUserCanceled.managedChargingUserCanceled)?.let {
            visitor.visitSingularBoolField(it.value, 140)
        }
        (this._optionalManagedChargingStartTime as? CarServer_ChargeState.OneOf_OptionalManagedChargingStartTime.managedChargingStartTime)?.let {
            visitor.visitSingularUInt64Field(it.value, 141)
        }
        (this._optionalMinutesToChargeLimit as? CarServer_ChargeState.OneOf_OptionalMinutesToChargeLimit.minutesToChargeLimit)?.let {
            visitor.visitSingularInt32Field(it.value, 142)
        }
        (this._optionalOffPeakHoursEndTime as? CarServer_ChargeState.OneOf_OptionalOffPeakHoursEndTime.offPeakHoursEndTime)?.let {
            visitor.visitSingularUInt32Field(it.value, 147)
        }
        (this._optionalScheduledChargingMode as? CarServer_ChargeState.OneOf_OptionalScheduledChargingMode.scheduledChargingMode)?.let {
            if (it.value != CarServer_ChargeState.ScheduledChargingMode.UNRECOGNIZED) {
                visitor.visitSingularEnumField(it.value.rawValue, 148)
            }
        }
        (this._optionalChargingAmps as? CarServer_ChargeState.OneOf_OptionalChargingAmps.chargingAmps)?.let {
            visitor.visitSingularInt32Field(it.value, 149)
        }
        (this._optionalScheduledChargingStartTimeMinutes as? CarServer_ChargeState.OneOf_OptionalScheduledChargingStartTimeMinutes.scheduledChargingStartTimeMinutes)?.let {
            visitor.visitSingularUInt32Field(it.value, 150)
        }
        (this._optionalScheduledDepartureTimeMinutes as? CarServer_ChargeState.OneOf_OptionalScheduledDepartureTimeMinutes.scheduledDepartureTimeMinutes)?.let {
            visitor.visitSingularUInt32Field(it.value, 151)
        }
        (this._optionalPreconditioningEnabled as? CarServer_ChargeState.OneOf_OptionalPreconditioningEnabled.preconditioningEnabled)?.let {
            visitor.visitSingularBoolField(it.value, 152)
        }
        (this._optionalScheduledChargingStartTimeApp as? CarServer_ChargeState.OneOf_OptionalScheduledChargingStartTimeApp.scheduledChargingStartTimeApp)?.let {
            visitor.visitSingularSInt32Field(it.value, 153)
        }
        (this._optionalSuperchargerSessionTripPlanner as? CarServer_ChargeState.OneOf_OptionalSuperchargerSessionTripPlanner.superchargerSessionTripPlanner)?.let {
            visitor.visitSingularBoolField(it.value, 154)
        }
        (this._optionalChargePortColor as? CarServer_ChargeState.OneOf_OptionalChargePortColor.chargePortColor)?.let {
            if (it.value != CarServer_ChargeState.ChargePortColor_E.UNRECOGNIZED) {
                visitor.visitSingularEnumField(it.value.rawValue, 155)
            }
        }
        (this._optionalChargeRateMphFloat as? CarServer_ChargeState.OneOf_OptionalChargeRateMphFloat.chargeRateMphFloat)?.let {
            visitor.visitSingularFloatField(it.value, 156)
        }
        (this._optionalChargeLimitReason as? CarServer_ChargeState.OneOf_OptionalChargeLimitReason.chargeLimitReason)?.let {
            if (it.value != CarServer_ChargeState.ChargeLimitReason.UNRECOGNIZED) {
                visitor.visitSingularEnumField(it.value.rawValue, 157)
            }
        }
        this._managedChargingState?.let {
            visitor.visitSingularMessageField(it, 158)
        }
        (this._optionalChargeCableUnlatched as? CarServer_ChargeState.OneOf_OptionalChargeCableUnlatched.chargeCableUnlatched)?.let {
            visitor.visitSingularBoolField(it.value, 159)
        }
        (this._optionalOutletState as? CarServer_ChargeState.OneOf_OptionalOutletState.outletState)?.let {
            if (it.value != CarServer_ChargeState.OutletState.UNRECOGNIZED) {
                visitor.visitSingularEnumField(it.value.rawValue, 160)
            }
        }
        (this._optionalPowerFeedState as? CarServer_ChargeState.OneOf_OptionalPowerFeedState.powerFeedState)?.let {
            if (it.value != CarServer_ChargeState.PowerFeedState.UNRECOGNIZED) {
                visitor.visitSingularEnumField(it.value.rawValue, 161)
            }
        }
        (this._optionOutletSocLimit as? CarServer_ChargeState.OneOf_OptionOutletSocLimit.outletSocLimit)?.let {
            visitor.visitSingularInt32Field(it.value, 162)
        }
        (this._optionPowerFeedSocLimit as? CarServer_ChargeState.OneOf_OptionPowerFeedSocLimit.powerFeedSocLimit)?.let {
            visitor.visitSingularInt32Field(it.value, 163)
        }
        (this._optionOutletTimeRemaining as? CarServer_ChargeState.OneOf_OptionOutletTimeRemaining.outletTimeRemaining)?.let {
            visitor.visitSingularInt64Field(it.value, 164)
        }
        (this._optionPowerFeedTimeRemaining as? CarServer_ChargeState.OneOf_OptionPowerFeedTimeRemaining.powerFeedTimeRemaining)?.let {
            visitor.visitSingularInt64Field(it.value, 165)
        }
        (this._optionalPowershareFeatureAllowed as? CarServer_ChargeState.OneOf_OptionalPowershareFeatureAllowed.powershareFeatureAllowed)?.let {
            visitor.visitSingularBoolField(it.value, 166)
        }
        (this._optionalPowershareFeatureEnabled as? CarServer_ChargeState.OneOf_OptionalPowershareFeatureEnabled.powershareFeatureEnabled)?.let {
            visitor.visitSingularBoolField(it.value, 167)
        }
        (this._optionalPowershareRequest as? CarServer_ChargeState.OneOf_OptionalPowershareRequest.powershareRequest)?.let {
            visitor.visitSingularBoolField(it.value, 168)
        }
        (this._optionalPowershareType as? CarServer_ChargeState.OneOf_OptionalPowershareType.powershareType)?.let {
            if (it.value != CarServer_ChargeState.PowershareType.UNRECOGNIZED) {
                visitor.visitSingularEnumField(it.value.rawValue, 169)
            }
        }
        (this._optionalPowershareStatus as? CarServer_ChargeState.OneOf_OptionalPowershareStatus.powershareStatus)?.let {
            if (it.value != CarServer_ChargeState.PowershareStatus.UNRECOGNIZED) {
                visitor.visitSingularEnumField(it.value.rawValue, 170)
            }
        }
        (this._optionalPowershareStopReason as? CarServer_ChargeState.OneOf_OptionalPowershareStopReason.powershareStopReason)?.let {
            if (it.value != CarServer_ChargeState.PowershareStopReason.UNRECOGNIZED) {
                visitor.visitSingularEnumField(it.value.rawValue, 171)
            }
        }
        (this._optionalPowershareInstantaneousLoadKw as? CarServer_ChargeState.OneOf_OptionalPowershareInstantaneousLoadKw.powershareInstantaneousLoadKw)?.let {
            visitor.visitSingularFloatField(it.value, 172)
        }
        (this._optionalPowershareVehicleEnergyLeftHr as? CarServer_ChargeState.OneOf_OptionalPowershareVehicleEnergyLeftHr.powershareVehicleEnergyLeftHr)?.let {
            visitor.visitSingularInt32Field(it.value, 173)
        }
        (this._optionalPowershareSocLimit as? CarServer_ChargeState.OneOf_OptionalPowershareSocLimit.powershareSocLimit)?.let {
            visitor.visitSingularInt32Field(it.value, 174)
        }
        (this._optionalOneTimeSocLimit as? CarServer_ChargeState.OneOf_OptionalOneTimeSocLimit.oneTimeSocLimit)?.let {
            visitor.visitSingularInt32Field(it.value, 175)
        }
        (this._optionalHomeLocation as? CarServer_ChargeState.OneOf_OptionalHomeLocation.homeLocation)?.let {
            visitor.visitSingularMessageField(it.value, 176)
        }
        (this._optionalWorkLocation as? CarServer_ChargeState.OneOf_OptionalWorkLocation.workLocation)?.let {
            visitor.visitSingularMessageField(it.value, 177)
        }
        (this._optionalOutletMaxTimerMinutes as? CarServer_ChargeState.OneOf_OptionalOutletMaxTimerMinutes.outletMaxTimerMinutes)?.let {
            visitor.visitSingularInt32Field(it.value, 178)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_ChargeState) return false
        if (this._chargingState != other._chargingState) return false
        if (this._fastChargerType != other._fastChargerType) return false
        if (this._fastChargerBrand != other._fastChargerBrand) return false
        if (this._optionalChargeLimitSoc != other._optionalChargeLimitSoc) return false
        if (this._optionalChargeLimitSocStd != other._optionalChargeLimitSocStd) return false
        if (this._optionalChargeLimitSocMin != other._optionalChargeLimitSocMin) return false
        if (this._optionalChargeLimitSocMax != other._optionalChargeLimitSocMax) return false
        if (this._optionalMaxRangeChargeCounter != other._optionalMaxRangeChargeCounter) return false
        if (this._optionalFastChargerPresent != other._optionalFastChargerPresent) return false
        if (this._optionalBatteryRange != other._optionalBatteryRange) return false
        if (this._optionalEstBatteryRange != other._optionalEstBatteryRange) return false
        if (this._optionalIdealBatteryRange != other._optionalIdealBatteryRange) return false
        if (this._optionalBatteryLevel != other._optionalBatteryLevel) return false
        if (this._optionalUsableBatteryLevel != other._optionalUsableBatteryLevel) return false
        if (this._optionalChargeEnergyAdded != other._optionalChargeEnergyAdded) return false
        if (this._optionalChargeMilesAddedRated != other._optionalChargeMilesAddedRated) return false
        if (this._optionalChargeMilesAddedIdeal != other._optionalChargeMilesAddedIdeal) return false
        if (this._optionalChargerVoltage != other._optionalChargerVoltage) return false
        if (this._optionalChargerPilotCurrent != other._optionalChargerPilotCurrent) return false
        if (this._optionalChargerActualCurrent != other._optionalChargerActualCurrent) return false
        if (this._optionalChargerPower != other._optionalChargerPower) return false
        if (this._optionalMinutesToFullCharge != other._optionalMinutesToFullCharge) return false
        if (this._optionalMinutesToChargeLimit != other._optionalMinutesToChargeLimit) return false
        if (this._optionalTripCharging != other._optionalTripCharging) return false
        if (this._optionalChargeRateMph != other._optionalChargeRateMph) return false
        if (this._optionalChargePortDoorOpen != other._optionalChargePortDoorOpen) return false
        if (this._connChargeCable != other._connChargeCable) return false
        if (this._optionalScheduledChargingStartTime != other._optionalScheduledChargingStartTime) return false
        if (this._optionalScheduledChargingPending != other._optionalScheduledChargingPending) return false
        if (this._scheduledDepartureTime != other._scheduledDepartureTime) return false
        if (this._optionalUserChargeEnableRequest != other._optionalUserChargeEnableRequest) return false
        if (this._optionalChargeEnableRequest != other._optionalChargeEnableRequest) return false
        if (this._optionalChargerPhases != other._optionalChargerPhases) return false
        if (this._chargePortLatch != other._chargePortLatch) return false
        if (this._optionalChargePortColdWeatherMode != other._optionalChargePortColdWeatherMode) return false
        if (this._optionalChargeCurrentRequest != other._optionalChargeCurrentRequest) return false
        if (this._optionalChargeCurrentRequestMax != other._optionalChargeCurrentRequestMax) return false
        if (this._optionalManagedChargingActive != other._optionalManagedChargingActive) return false
        if (this._optionalManagedChargingUserCanceled != other._optionalManagedChargingUserCanceled) return false
        if (this._optionalManagedChargingStartTime != other._optionalManagedChargingStartTime) return false
        if (this._timestamp != other._timestamp) return false
        if (this._preconditioningTimes != other._preconditioningTimes) return false
        if (this._offPeakChargingTimes != other._offPeakChargingTimes) return false
        if (this._optionalOffPeakHoursEndTime != other._optionalOffPeakHoursEndTime) return false
        if (this._optionalScheduledChargingMode != other._optionalScheduledChargingMode) return false
        if (this._optionalChargingAmps != other._optionalChargingAmps) return false
        if (this._optionalScheduledChargingStartTimeMinutes != other._optionalScheduledChargingStartTimeMinutes) return false
        if (this._optionalScheduledDepartureTimeMinutes != other._optionalScheduledDepartureTimeMinutes) return false
        if (this._optionalPreconditioningEnabled != other._optionalPreconditioningEnabled) return false
        if (this._optionalScheduledChargingStartTimeApp != other._optionalScheduledChargingStartTimeApp) return false
        if (this._optionalSuperchargerSessionTripPlanner != other._optionalSuperchargerSessionTripPlanner) return false
        if (this._optionalChargePortColor != other._optionalChargePortColor) return false
        if (this._optionalChargeRateMphFloat != other._optionalChargeRateMphFloat) return false
        if (this._optionalChargeLimitReason != other._optionalChargeLimitReason) return false
        if (this._managedChargingState != other._managedChargingState) return false
        if (this._optionalChargeCableUnlatched != other._optionalChargeCableUnlatched) return false
        if (this._optionalOutletState != other._optionalOutletState) return false
        if (this._optionalPowerFeedState != other._optionalPowerFeedState) return false
        if (this._optionOutletSocLimit != other._optionOutletSocLimit) return false
        if (this._optionPowerFeedSocLimit != other._optionPowerFeedSocLimit) return false
        if (this._optionOutletTimeRemaining != other._optionOutletTimeRemaining) return false
        if (this._optionPowerFeedTimeRemaining != other._optionPowerFeedTimeRemaining) return false
        if (this._optionalPowershareFeatureAllowed != other._optionalPowershareFeatureAllowed) return false
        if (this._optionalPowershareFeatureEnabled != other._optionalPowershareFeatureEnabled) return false
        if (this._optionalPowershareRequest != other._optionalPowershareRequest) return false
        if (this._optionalPowershareType != other._optionalPowershareType) return false
        if (this._optionalPowershareStatus != other._optionalPowershareStatus) return false
        if (this._optionalPowershareStopReason != other._optionalPowershareStopReason) return false
        if (this._optionalPowershareInstantaneousLoadKw != other._optionalPowershareInstantaneousLoadKw) return false
        if (this._optionalPowershareVehicleEnergyLeftHr != other._optionalPowershareVehicleEnergyLeftHr) return false
        if (this._optionalPowershareSocLimit != other._optionalPowershareSocLimit) return false
        if (this._optionalOneTimeSocLimit != other._optionalOneTimeSocLimit) return false
        if (this._optionalHomeLocation != other._optionalHomeLocation) return false
        if (this._optionalWorkLocation != other._optionalWorkLocation) return false
        if (this._optionalOutletMaxTimerMinutes != other._optionalOutletMaxTimerMinutes) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._chargingState?.hashCode() ?: 0)
        hash = 31 * hash + (this._fastChargerType?.hashCode() ?: 0)
        hash = 31 * hash + (this._fastChargerBrand?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalChargeLimitSoc?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalChargeLimitSocStd?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalChargeLimitSocMin?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalChargeLimitSocMax?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalMaxRangeChargeCounter?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalFastChargerPresent?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalBatteryRange?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalEstBatteryRange?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalIdealBatteryRange?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalBatteryLevel?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalUsableBatteryLevel?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalChargeEnergyAdded?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalChargeMilesAddedRated?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalChargeMilesAddedIdeal?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalChargerVoltage?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalChargerPilotCurrent?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalChargerActualCurrent?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalChargerPower?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalMinutesToFullCharge?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalMinutesToChargeLimit?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalTripCharging?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalChargeRateMph?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalChargePortDoorOpen?.hashCode() ?: 0)
        hash = 31 * hash + (this._connChargeCable?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalScheduledChargingStartTime?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalScheduledChargingPending?.hashCode() ?: 0)
        hash = 31 * hash + (this._scheduledDepartureTime?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalUserChargeEnableRequest?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalChargeEnableRequest?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalChargerPhases?.hashCode() ?: 0)
        hash = 31 * hash + (this._chargePortLatch?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalChargePortColdWeatherMode?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalChargeCurrentRequest?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalChargeCurrentRequestMax?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalManagedChargingActive?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalManagedChargingUserCanceled?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalManagedChargingStartTime?.hashCode() ?: 0)
        hash = 31 * hash + (this._timestamp?.hashCode() ?: 0)
        hash = 31 * hash + (this._preconditioningTimes?.hashCode() ?: 0)
        hash = 31 * hash + (this._offPeakChargingTimes?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalOffPeakHoursEndTime?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalScheduledChargingMode?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalChargingAmps?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalScheduledChargingStartTimeMinutes?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalScheduledDepartureTimeMinutes?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalPreconditioningEnabled?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalScheduledChargingStartTimeApp?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalSuperchargerSessionTripPlanner?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalChargePortColor?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalChargeRateMphFloat?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalChargeLimitReason?.hashCode() ?: 0)
        hash = 31 * hash + (this._managedChargingState?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalChargeCableUnlatched?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalOutletState?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalPowerFeedState?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionOutletSocLimit?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionPowerFeedSocLimit?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionOutletTimeRemaining?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionPowerFeedTimeRemaining?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalPowershareFeatureAllowed?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalPowershareFeatureEnabled?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalPowershareRequest?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalPowershareType?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalPowershareStatus?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalPowershareStopReason?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalPowershareInstantaneousLoadKw?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalPowershareVehicleEnergyLeftHr?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalPowershareSocLimit?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalOneTimeSocLimit?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalHomeLocation?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalWorkLocation?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalOutletMaxTimerMinutes?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ChargeState {
        val result = CarServer_ChargeState()
        result._chargingState = this._chargingState?.copy()
        result._fastChargerType = this._fastChargerType?.copy()
        result._fastChargerBrand = this._fastChargerBrand?.copy()
        result._optionalChargeLimitSoc = this._optionalChargeLimitSoc
        result._optionalChargeLimitSocStd = this._optionalChargeLimitSocStd
        result._optionalChargeLimitSocMin = this._optionalChargeLimitSocMin
        result._optionalChargeLimitSocMax = this._optionalChargeLimitSocMax
        result._optionalMaxRangeChargeCounter = this._optionalMaxRangeChargeCounter
        result._optionalFastChargerPresent = this._optionalFastChargerPresent
        result._optionalBatteryRange = this._optionalBatteryRange
        result._optionalEstBatteryRange = this._optionalEstBatteryRange
        result._optionalIdealBatteryRange = this._optionalIdealBatteryRange
        result._optionalBatteryLevel = this._optionalBatteryLevel
        result._optionalUsableBatteryLevel = this._optionalUsableBatteryLevel
        result._optionalChargeEnergyAdded = this._optionalChargeEnergyAdded
        result._optionalChargeMilesAddedRated = this._optionalChargeMilesAddedRated
        result._optionalChargeMilesAddedIdeal = this._optionalChargeMilesAddedIdeal
        result._optionalChargerVoltage = this._optionalChargerVoltage
        result._optionalChargerPilotCurrent = this._optionalChargerPilotCurrent
        result._optionalChargerActualCurrent = this._optionalChargerActualCurrent
        result._optionalChargerPower = this._optionalChargerPower
        result._optionalMinutesToFullCharge = this._optionalMinutesToFullCharge
        result._optionalMinutesToChargeLimit = this._optionalMinutesToChargeLimit
        result._optionalTripCharging = this._optionalTripCharging
        result._optionalChargeRateMph = this._optionalChargeRateMph
        result._optionalChargePortDoorOpen = this._optionalChargePortDoorOpen
        result._connChargeCable = this._connChargeCable?.copy()
        result._optionalScheduledChargingStartTime = this._optionalScheduledChargingStartTime
        result._optionalScheduledChargingPending = this._optionalScheduledChargingPending
        result._scheduledDepartureTime = this._scheduledDepartureTime?.copy()
        result._optionalUserChargeEnableRequest = this._optionalUserChargeEnableRequest
        result._optionalChargeEnableRequest = this._optionalChargeEnableRequest
        result._optionalChargerPhases = this._optionalChargerPhases
        result._chargePortLatch = this._chargePortLatch?.copy()
        result._optionalChargePortColdWeatherMode = this._optionalChargePortColdWeatherMode
        result._optionalChargeCurrentRequest = this._optionalChargeCurrentRequest
        result._optionalChargeCurrentRequestMax = this._optionalChargeCurrentRequestMax
        result._optionalManagedChargingActive = this._optionalManagedChargingActive
        result._optionalManagedChargingUserCanceled = this._optionalManagedChargingUserCanceled
        result._optionalManagedChargingStartTime = this._optionalManagedChargingStartTime
        result._timestamp = this._timestamp?.copy()
        result._preconditioningTimes = this._preconditioningTimes?.copy()
        result._offPeakChargingTimes = this._offPeakChargingTimes?.copy()
        result._optionalOffPeakHoursEndTime = this._optionalOffPeakHoursEndTime
        result._optionalScheduledChargingMode = this._optionalScheduledChargingMode
        result._optionalChargingAmps = this._optionalChargingAmps
        result._optionalScheduledChargingStartTimeMinutes = this._optionalScheduledChargingStartTimeMinutes
        result._optionalScheduledDepartureTimeMinutes = this._optionalScheduledDepartureTimeMinutes
        result._optionalPreconditioningEnabled = this._optionalPreconditioningEnabled
        result._optionalScheduledChargingStartTimeApp = this._optionalScheduledChargingStartTimeApp
        result._optionalSuperchargerSessionTripPlanner = this._optionalSuperchargerSessionTripPlanner
        result._optionalChargePortColor = this._optionalChargePortColor
        result._optionalChargeRateMphFloat = this._optionalChargeRateMphFloat
        result._optionalChargeLimitReason = this._optionalChargeLimitReason
        result._managedChargingState = this._managedChargingState?.copy()
        result._optionalChargeCableUnlatched = this._optionalChargeCableUnlatched
        result._optionalOutletState = this._optionalOutletState
        result._optionalPowerFeedState = this._optionalPowerFeedState
        result._optionOutletSocLimit = this._optionOutletSocLimit
        result._optionPowerFeedSocLimit = this._optionPowerFeedSocLimit
        result._optionOutletTimeRemaining = this._optionOutletTimeRemaining
        result._optionPowerFeedTimeRemaining = this._optionPowerFeedTimeRemaining
        result._optionalPowershareFeatureAllowed = this._optionalPowershareFeatureAllowed
        result._optionalPowershareFeatureEnabled = this._optionalPowershareFeatureEnabled
        result._optionalPowershareRequest = this._optionalPowershareRequest
        result._optionalPowershareType = this._optionalPowershareType
        result._optionalPowershareStatus = this._optionalPowershareStatus
        result._optionalPowershareStopReason = this._optionalPowershareStopReason
        result._optionalPowershareInstantaneousLoadKw = this._optionalPowershareInstantaneousLoadKw
        result._optionalPowershareVehicleEnergyLeftHr = this._optionalPowershareVehicleEnergyLeftHr
        result._optionalPowershareSocLimit = this._optionalPowershareSocLimit
        result._optionalOneTimeSocLimit = this._optionalOneTimeSocLimit
        result._optionalHomeLocation = this._protobufCopy_optionalHomeLocation(this._optionalHomeLocation)
        result._optionalWorkLocation = this._protobufCopy_optionalWorkLocation(this._optionalWorkLocation)
        result._optionalOutletMaxTimerMinutes = this._optionalOutletMaxTimerMinutes
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ChargeState"

        fun with(block: CarServer_ChargeState.() -> Unit): CarServer_ChargeState =
            CarServer_ChargeState().apply(block)
    }
}

class CarServer_ManagedChargingState() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var chargeOnSolarState: CarServer_ChargeOnSolarState
        get() = this._chargeOnSolarState ?: _protobufPending(1, { CarServer_ChargeOnSolarState() }) { this._chargeOnSolarState = it }
        set(value) {
            _protobufDropPending(1)
            this._chargeOnSolarState = value.copy()
            _protobufMutated()
        }

    val hasChargeOnSolarState: Boolean
        get() = this._chargeOnSolarState != null

    fun clearChargeOnSolarState() {
        _protobufDropPending(1)
        this._chargeOnSolarState = null
        _protobufMutated()
    }

    var optionalChargeOnSolarGatewayDin: CarServer_ManagedChargingState.OneOf_OptionalChargeOnSolarGatewayDin?
        get() = this._optionalChargeOnSolarGatewayDin
        set(value) {
            this._optionalChargeOnSolarGatewayDin = value
            _protobufMutated()
        }

    var chargeOnSolarGatewayDin: String
        get() = (this._optionalChargeOnSolarGatewayDin as? CarServer_ManagedChargingState.OneOf_OptionalChargeOnSolarGatewayDin.chargeOnSolarGatewayDin)?.value ?: ""
        set(value) {
            this.optionalChargeOnSolarGatewayDin = CarServer_ManagedChargingState.OneOf_OptionalChargeOnSolarGatewayDin.chargeOnSolarGatewayDin(value)
        }

    var optionalTeslaElectricAssetID: CarServer_ManagedChargingState.OneOf_OptionalTeslaElectricAssetID?
        get() = this._optionalTeslaElectricAssetID
        set(value) {
            this._optionalTeslaElectricAssetID = value
            _protobufMutated()
        }

    var teslaElectricAssetID: String
        get() = (this._optionalTeslaElectricAssetID as? CarServer_ManagedChargingState.OneOf_OptionalTeslaElectricAssetID.teslaElectricAssetID)?.value ?: ""
        set(value) {
            this.optionalTeslaElectricAssetID = CarServer_ManagedChargingState.OneOf_OptionalTeslaElectricAssetID.teslaElectricAssetID(value)
        }

    var optionalMinutesToLowerLimit: CarServer_ManagedChargingState.OneOf_OptionalMinutesToLowerLimit?
        get() = this._optionalMinutesToLowerLimit
        set(value) {
            this._optionalMinutesToLowerLimit = value
            _protobufMutated()
        }

    var minutesToLowerLimit: Int
        get() = (this._optionalMinutesToLowerLimit as? CarServer_ManagedChargingState.OneOf_OptionalMinutesToLowerLimit.minutesToLowerLimit)?.value ?: 0
        set(value) {
            this.optionalMinutesToLowerLimit = CarServer_ManagedChargingState.OneOf_OptionalMinutesToLowerLimit.minutesToLowerLimit(value)
        }

    sealed class OneOf_OptionalChargeOnSolarGatewayDin(value: Any) : ProtobufOneofCase(value) {
        class chargeOnSolarGatewayDin(val value: String) : OneOf_OptionalChargeOnSolarGatewayDin(value)
    }

    sealed class OneOf_OptionalTeslaElectricAssetID(value: Any) : ProtobufOneofCase(value) {
        class teslaElectricAssetID(val value: String) : OneOf_OptionalTeslaElectricAssetID(value)
    }

    sealed class OneOf_OptionalMinutesToLowerLimit(value: Any) : ProtobufOneofCase(value) {
        class minutesToLowerLimit(val value: Int) : OneOf_OptionalMinutesToLowerLimit(value)
    }

    private var _chargeOnSolarState: CarServer_ChargeOnSolarState? = null
    private var _optionalChargeOnSolarGatewayDin: CarServer_ManagedChargingState.OneOf_OptionalChargeOnSolarGatewayDin? = null
    private var _optionalTeslaElectricAssetID: CarServer_ManagedChargingState.OneOf_OptionalTeslaElectricAssetID? = null
    private var _optionalMinutesToLowerLimit: CarServer_ManagedChargingState.OneOf_OptionalMinutesToLowerLimit? = null

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularMessageField(this._chargeOnSolarState) { CarServer_ChargeOnSolarState() }?.let {
                    _protobufDropPending(1)
                    this._chargeOnSolarState = it
                }
                2 -> decoder.decodeSingularStringField()?.let { this._optionalChargeOnSolarGatewayDin = CarServer_ManagedChargingState.OneOf_OptionalChargeOnSolarGatewayDin.chargeOnSolarGatewayDin(it) }
                3 -> decoder.decodeSingularStringField()?.let { this._optionalTeslaElectricAssetID = CarServer_ManagedChargingState.OneOf_OptionalTeslaElectricAssetID.teslaElectricAssetID(it) }
                4 -> decoder.decodeSingularInt32Field()?.let { this._optionalMinutesToLowerLimit = CarServer_ManagedChargingState.OneOf_OptionalMinutesToLowerLimit.minutesToLowerLimit(it) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        this._chargeOnSolarState?.let {
            visitor.visitSingularMessageField(it, 1)
        }
        (this._optionalChargeOnSolarGatewayDin as? CarServer_ManagedChargingState.OneOf_OptionalChargeOnSolarGatewayDin.chargeOnSolarGatewayDin)?.let {
            visitor.visitSingularStringField(it.value, 2)
        }
        (this._optionalTeslaElectricAssetID as? CarServer_ManagedChargingState.OneOf_OptionalTeslaElectricAssetID.teslaElectricAssetID)?.let {
            visitor.visitSingularStringField(it.value, 3)
        }
        (this._optionalMinutesToLowerLimit as? CarServer_ManagedChargingState.OneOf_OptionalMinutesToLowerLimit.minutesToLowerLimit)?.let {
            visitor.visitSingularInt32Field(it.value, 4)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_ManagedChargingState) return false
        if (this._chargeOnSolarState != other._chargeOnSolarState) return false
        if (this._optionalChargeOnSolarGatewayDin != other._optionalChargeOnSolarGatewayDin) return false
        if (this._optionalTeslaElectricAssetID != other._optionalTeslaElectricAssetID) return false
        if (this._optionalMinutesToLowerLimit != other._optionalMinutesToLowerLimit) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._chargeOnSolarState?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalChargeOnSolarGatewayDin?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalTeslaElectricAssetID?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalMinutesToLowerLimit?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ManagedChargingState {
        val result = CarServer_ManagedChargingState()
        result._chargeOnSolarState = this._chargeOnSolarState?.copy()
        result._optionalChargeOnSolarGatewayDin = this._optionalChargeOnSolarGatewayDin
        result._optionalTeslaElectricAssetID = this._optionalTeslaElectricAssetID
        result._optionalMinutesToLowerLimit = this._optionalMinutesToLowerLimit
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ManagedChargingState"

        fun with(block: CarServer_ManagedChargingState.() -> Unit): CarServer_ManagedChargingState =
            CarServer_ManagedChargingState().apply(block)
    }
}

class CarServer_ChargeOnSolarState() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var state: CarServer_ChargeOnSolarState.OneOf_State?
        get() = this._state
        set(value) {
            this._protobufStore_state(this._protobufCopy_state(value))
            _protobufMutated()
        }

    var notAllowed: CarServer_ChargeOnSolarStateNotAllowed
        get() = (this._state as? CarServer_ChargeOnSolarState.OneOf_State.notAllowed)?.value
            ?: _protobufPending(1, { CarServer_ChargeOnSolarStateNotAllowed() }) { this._protobufStore_state(CarServer_ChargeOnSolarState.OneOf_State.notAllowed(it)) }
        set(value) {
            this.state = CarServer_ChargeOnSolarState.OneOf_State.notAllowed(value)
        }

    var noChargeRecommended: CarServer_ChargeOnSolarStateNoChargeRecommended
        get() = (this._state as? CarServer_ChargeOnSolarState.OneOf_State.noChargeRecommended)?.value
            ?: _protobufPending(2, { CarServer_ChargeOnSolarStateNoChargeRecommended() }) { this._protobufStore_state(CarServer_ChargeOnSolarState.OneOf_State.noChargeRecommended(it)) }
        set(value) {
            this.state = CarServer_ChargeOnSolarState.OneOf_State.noChargeRecommended(value)
        }

    var chargingOnExcessSolar: CarServer_ChargeOnSolarStateChargingOnExcessSolar
        get() = (this._state as? CarServer_ChargeOnSolarState.OneOf_State.chargingOnExcessSolar)?.value
            ?: _protobufPending(3, { CarServer_ChargeOnSolarStateChargingOnExcessSolar() }) { this._protobufStore_state(CarServer_ChargeOnSolarState.OneOf_State.chargingOnExcessSolar(it)) }
        set(value) {
            this.state = CarServer_ChargeOnSolarState.OneOf_State.chargingOnExcessSolar(value)
        }

    var chargingOnAnything: CarServer_ChargeOnSolarStateChargingOnAnything
        get() = (this._state as? CarServer_ChargeOnSolarState.OneOf_State.chargingOnAnything)?.value
            ?: _protobufPending(4, { CarServer_ChargeOnSolarStateChargingOnAnything() }) { this._protobufStore_state(CarServer_ChargeOnSolarState.OneOf_State.chargingOnAnything(it)) }
        set(value) {
            this.state = CarServer_ChargeOnSolarState.OneOf_State.chargingOnAnything(value)
        }

    var userDisabled: CarServer_ChargeOnSolarStateUserDisabled
        get() = (this._state as? CarServer_ChargeOnSolarState.OneOf_State.userDisabled)?.value
            ?: _protobufPending(6, { CarServer_ChargeOnSolarStateUserDisabled() }) { this._protobufStore_state(CarServer_ChargeOnSolarState.OneOf_State.userDisabled(it)) }
        set(value) {
            this.state = CarServer_ChargeOnSolarState.OneOf_State.userDisabled(value)
        }

    var waitingForServer: CarServer_ChargeOnSolarStateWaitingForServer
        get() = (this._state as? CarServer_ChargeOnSolarState.OneOf_State.waitingForServer)?.value
            ?: _protobufPending(7, { CarServer_ChargeOnSolarStateWaitingForServer() }) { this._protobufStore_state(CarServer_ChargeOnSolarState.OneOf_State.waitingForServer(it)) }
        set(value) {
            this.state = CarServer_ChargeOnSolarState.OneOf_State.waitingForServer(value)
        }

    var error: CarServer_ChargeOnSolarStateError
        get() = (this._state as? CarServer_ChargeOnSolarState.OneOf_State.error)?.value
            ?: _protobufPending(8, { CarServer_ChargeOnSolarStateError() }) { this._protobufStore_state(CarServer_ChargeOnSolarState.OneOf_State.error(it)) }
        set(value) {
            this.state = CarServer_ChargeOnSolarState.OneOf_State.error(value)
        }

    var userStopped: CarServer_ChargeOnSolarStateUserStopped
        get() = (this._state as? CarServer_ChargeOnSolarState.OneOf_State.userStopped)?.value
            ?: _protobufPending(9, { CarServer_ChargeOnSolarStateUserStopped() }) { this._protobufStore_state(CarServer_ChargeOnSolarState.OneOf_State.userStopped(it)) }
        set(value) {
            this.state = CarServer_ChargeOnSolarState.OneOf_State.userStopped(value)
        }

    sealed class OneOf_State(value: Any) : ProtobufOneofCase(value) {
        class notAllowed(val value: CarServer_ChargeOnSolarStateNotAllowed) : OneOf_State(value)
        class noChargeRecommended(val value: CarServer_ChargeOnSolarStateNoChargeRecommended) : OneOf_State(value)
        class chargingOnExcessSolar(val value: CarServer_ChargeOnSolarStateChargingOnExcessSolar) : OneOf_State(value)
        class chargingOnAnything(val value: CarServer_ChargeOnSolarStateChargingOnAnything) : OneOf_State(value)
        class userDisabled(val value: CarServer_ChargeOnSolarStateUserDisabled) : OneOf_State(value)
        class waitingForServer(val value: CarServer_ChargeOnSolarStateWaitingForServer) : OneOf_State(value)
        class error(val value: CarServer_ChargeOnSolarStateError) : OneOf_State(value)
        class userStopped(val value: CarServer_ChargeOnSolarStateUserStopped) : OneOf_State(value)
    }

    private var _state: CarServer_ChargeOnSolarState.OneOf_State? = null

    private fun _protobufStore_state(value: CarServer_ChargeOnSolarState.OneOf_State?) {
        _protobufDropPending(1)
        _protobufDropPending(2)
        _protobufDropPending(3)
        _protobufDropPending(4)
        _protobufDropPending(6)
        _protobufDropPending(7)
        _protobufDropPending(8)
        _protobufDropPending(9)
        this._state = value
    }

    private fun _protobufCopy_state(value: CarServer_ChargeOnSolarState.OneOf_State?): CarServer_ChargeOnSolarState.OneOf_State? =
        when (value) {
            is CarServer_ChargeOnSolarState.OneOf_State.notAllowed -> CarServer_ChargeOnSolarState.OneOf_State.notAllowed(value.value.copy())
            is CarServer_ChargeOnSolarState.OneOf_State.noChargeRecommended -> CarServer_ChargeOnSolarState.OneOf_State.noChargeRecommended(value.value.copy())
            is CarServer_ChargeOnSolarState.OneOf_State.chargingOnExcessSolar -> CarServer_ChargeOnSolarState.OneOf_State.chargingOnExcessSolar(value.value.copy())
            is CarServer_ChargeOnSolarState.OneOf_State.chargingOnAnything -> CarServer_ChargeOnSolarState.OneOf_State.chargingOnAnything(value.value.copy())
            is CarServer_ChargeOnSolarState.OneOf_State.userDisabled -> CarServer_ChargeOnSolarState.OneOf_State.userDisabled(value.value.copy())
            is CarServer_ChargeOnSolarState.OneOf_State.waitingForServer -> CarServer_ChargeOnSolarState.OneOf_State.waitingForServer(value.value.copy())
            is CarServer_ChargeOnSolarState.OneOf_State.error -> CarServer_ChargeOnSolarState.OneOf_State.error(value.value.copy())
            is CarServer_ChargeOnSolarState.OneOf_State.userStopped -> CarServer_ChargeOnSolarState.OneOf_State.userStopped(value.value.copy())
            else -> value
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularMessageField((this._state as? CarServer_ChargeOnSolarState.OneOf_State.notAllowed)?.value) { CarServer_ChargeOnSolarStateNotAllowed() }
                    ?.let { this._protobufStore_state(CarServer_ChargeOnSolarState.OneOf_State.notAllowed(it)) }
                2 -> decoder.decodeSingularMessageField((this._state as? CarServer_ChargeOnSolarState.OneOf_State.noChargeRecommended)?.value) { CarServer_ChargeOnSolarStateNoChargeRecommended() }
                    ?.let { this._protobufStore_state(CarServer_ChargeOnSolarState.OneOf_State.noChargeRecommended(it)) }
                3 -> decoder.decodeSingularMessageField((this._state as? CarServer_ChargeOnSolarState.OneOf_State.chargingOnExcessSolar)?.value) { CarServer_ChargeOnSolarStateChargingOnExcessSolar() }
                    ?.let { this._protobufStore_state(CarServer_ChargeOnSolarState.OneOf_State.chargingOnExcessSolar(it)) }
                4 -> decoder.decodeSingularMessageField((this._state as? CarServer_ChargeOnSolarState.OneOf_State.chargingOnAnything)?.value) { CarServer_ChargeOnSolarStateChargingOnAnything() }
                    ?.let { this._protobufStore_state(CarServer_ChargeOnSolarState.OneOf_State.chargingOnAnything(it)) }
                6 -> decoder.decodeSingularMessageField((this._state as? CarServer_ChargeOnSolarState.OneOf_State.userDisabled)?.value) { CarServer_ChargeOnSolarStateUserDisabled() }
                    ?.let { this._protobufStore_state(CarServer_ChargeOnSolarState.OneOf_State.userDisabled(it)) }
                7 -> decoder.decodeSingularMessageField((this._state as? CarServer_ChargeOnSolarState.OneOf_State.waitingForServer)?.value) { CarServer_ChargeOnSolarStateWaitingForServer() }
                    ?.let { this._protobufStore_state(CarServer_ChargeOnSolarState.OneOf_State.waitingForServer(it)) }
                8 -> decoder.decodeSingularMessageField((this._state as? CarServer_ChargeOnSolarState.OneOf_State.error)?.value) { CarServer_ChargeOnSolarStateError() }
                    ?.let { this._protobufStore_state(CarServer_ChargeOnSolarState.OneOf_State.error(it)) }
                9 -> decoder.decodeSingularMessageField((this._state as? CarServer_ChargeOnSolarState.OneOf_State.userStopped)?.value) { CarServer_ChargeOnSolarStateUserStopped() }
                    ?.let { this._protobufStore_state(CarServer_ChargeOnSolarState.OneOf_State.userStopped(it)) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        (this._state as? CarServer_ChargeOnSolarState.OneOf_State.notAllowed)?.let {
            visitor.visitSingularMessageField(it.value, 1)
        }
        (this._state as? CarServer_ChargeOnSolarState.OneOf_State.noChargeRecommended)?.let {
            visitor.visitSingularMessageField(it.value, 2)
        }
        (this._state as? CarServer_ChargeOnSolarState.OneOf_State.chargingOnExcessSolar)?.let {
            visitor.visitSingularMessageField(it.value, 3)
        }
        (this._state as? CarServer_ChargeOnSolarState.OneOf_State.chargingOnAnything)?.let {
            visitor.visitSingularMessageField(it.value, 4)
        }
        (this._state as? CarServer_ChargeOnSolarState.OneOf_State.userDisabled)?.let {
            visitor.visitSingularMessageField(it.value, 6)
        }
        (this._state as? CarServer_ChargeOnSolarState.OneOf_State.waitingForServer)?.let {
            visitor.visitSingularMessageField(it.value, 7)
        }
        (this._state as? CarServer_ChargeOnSolarState.OneOf_State.error)?.let {
            visitor.visitSingularMessageField(it.value, 8)
        }
        (this._state as? CarServer_ChargeOnSolarState.OneOf_State.userStopped)?.let {
            visitor.visitSingularMessageField(it.value, 9)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_ChargeOnSolarState) return false
        if (this._state != other._state) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._state?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ChargeOnSolarState {
        val result = CarServer_ChargeOnSolarState()
        result._state = this._protobufCopy_state(this._state)
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ChargeOnSolarState"

        fun with(block: CarServer_ChargeOnSolarState.() -> Unit): CarServer_ChargeOnSolarState =
            CarServer_ChargeOnSolarState().apply(block)
    }
}

class CarServer_ChargeOnSolarStateNotAllowed() : GeneratedMessage() {
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
        if (other !is CarServer_ChargeOnSolarStateNotAllowed) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ChargeOnSolarStateNotAllowed {
        val result = CarServer_ChargeOnSolarStateNotAllowed()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ChargeOnSolarStateNotAllowed"

        fun with(block: CarServer_ChargeOnSolarStateNotAllowed.() -> Unit): CarServer_ChargeOnSolarStateNotAllowed =
            CarServer_ChargeOnSolarStateNotAllowed().apply(block)
    }
}

class CarServer_ChargeOnSolarStateNoChargeRecommended() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var reason: ManagedCharging_ChargeOnSolarNoChargeReason = ManagedCharging_ChargeOnSolarNoChargeReason.invalid
        set(value) {
            field = value
            _protobufForgetUnrecognized(1)
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularOpenEnumField(ManagedCharging_ChargeOnSolarNoChargeReason.UNRECOGNIZED) { ManagedCharging_ChargeOnSolarNoChargeReason.fromRawValue(it) }
                    ?.let { this.reason = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.reason != ManagedCharging_ChargeOnSolarNoChargeReason.invalid && this.reason != ManagedCharging_ChargeOnSolarNoChargeReason.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.reason.rawValue, 1)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_ChargeOnSolarStateNoChargeRecommended) return false
        if (this.reason != other.reason) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.reason.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ChargeOnSolarStateNoChargeRecommended {
        val result = CarServer_ChargeOnSolarStateNoChargeRecommended()
        result.reason = this.reason
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ChargeOnSolarStateNoChargeRecommended"

        fun with(block: CarServer_ChargeOnSolarStateNoChargeRecommended.() -> Unit): CarServer_ChargeOnSolarStateNoChargeRecommended =
            CarServer_ChargeOnSolarStateNoChargeRecommended().apply(block)
    }
}

class CarServer_ChargeOnSolarStateChargingOnExcessSolar() : GeneratedMessage() {
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
        if (other !is CarServer_ChargeOnSolarStateChargingOnExcessSolar) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ChargeOnSolarStateChargingOnExcessSolar {
        val result = CarServer_ChargeOnSolarStateChargingOnExcessSolar()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ChargeOnSolarStateChargingOnExcessSolar"

        fun with(block: CarServer_ChargeOnSolarStateChargingOnExcessSolar.() -> Unit): CarServer_ChargeOnSolarStateChargingOnExcessSolar =
            CarServer_ChargeOnSolarStateChargingOnExcessSolar().apply(block)
    }
}

class CarServer_ChargeOnSolarStateChargingOnAnything() : GeneratedMessage() {
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
        if (other !is CarServer_ChargeOnSolarStateChargingOnAnything) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ChargeOnSolarStateChargingOnAnything {
        val result = CarServer_ChargeOnSolarStateChargingOnAnything()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ChargeOnSolarStateChargingOnAnything"

        fun with(block: CarServer_ChargeOnSolarStateChargingOnAnything.() -> Unit): CarServer_ChargeOnSolarStateChargingOnAnything =
            CarServer_ChargeOnSolarStateChargingOnAnything().apply(block)
    }
}

class CarServer_ChargeOnSolarStateUserDisabled() : GeneratedMessage() {
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
        if (other !is CarServer_ChargeOnSolarStateUserDisabled) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ChargeOnSolarStateUserDisabled {
        val result = CarServer_ChargeOnSolarStateUserDisabled()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ChargeOnSolarStateUserDisabled"

        fun with(block: CarServer_ChargeOnSolarStateUserDisabled.() -> Unit): CarServer_ChargeOnSolarStateUserDisabled =
            CarServer_ChargeOnSolarStateUserDisabled().apply(block)
    }
}

class CarServer_ChargeOnSolarStateWaitingForServer() : GeneratedMessage() {
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
        if (other !is CarServer_ChargeOnSolarStateWaitingForServer) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ChargeOnSolarStateWaitingForServer {
        val result = CarServer_ChargeOnSolarStateWaitingForServer()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ChargeOnSolarStateWaitingForServer"

        fun with(block: CarServer_ChargeOnSolarStateWaitingForServer.() -> Unit): CarServer_ChargeOnSolarStateWaitingForServer =
            CarServer_ChargeOnSolarStateWaitingForServer().apply(block)
    }
}

class CarServer_ChargeOnSolarStateError() : GeneratedMessage() {
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
        if (other !is CarServer_ChargeOnSolarStateError) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ChargeOnSolarStateError {
        val result = CarServer_ChargeOnSolarStateError()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ChargeOnSolarStateError"

        fun with(block: CarServer_ChargeOnSolarStateError.() -> Unit): CarServer_ChargeOnSolarStateError =
            CarServer_ChargeOnSolarStateError().apply(block)
    }
}

class CarServer_ChargeOnSolarStateUserStopped() : GeneratedMessage() {
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
        if (other !is CarServer_ChargeOnSolarStateUserStopped) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ChargeOnSolarStateUserStopped {
        val result = CarServer_ChargeOnSolarStateUserStopped()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ChargeOnSolarStateUserStopped"

        fun with(block: CarServer_ChargeOnSolarStateUserStopped.() -> Unit): CarServer_ChargeOnSolarStateUserStopped =
            CarServer_ChargeOnSolarStateUserStopped().apply(block)
    }
}

class CarServer_LocationState() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var optionalLatitude: CarServer_LocationState.OneOf_OptionalLatitude?
        get() = this._optionalLatitude
        set(value) {
            this._optionalLatitude = value
            _protobufMutated()
        }

    var latitude: Float
        get() = (this._optionalLatitude as? CarServer_LocationState.OneOf_OptionalLatitude.latitude)?.value ?: 0f
        set(value) {
            this.optionalLatitude = CarServer_LocationState.OneOf_OptionalLatitude.latitude(value)
        }

    var optionalLongitude: CarServer_LocationState.OneOf_OptionalLongitude?
        get() = this._optionalLongitude
        set(value) {
            this._optionalLongitude = value
            _protobufMutated()
        }

    var longitude: Float
        get() = (this._optionalLongitude as? CarServer_LocationState.OneOf_OptionalLongitude.longitude)?.value ?: 0f
        set(value) {
            this.optionalLongitude = CarServer_LocationState.OneOf_OptionalLongitude.longitude(value)
        }

    var optionalHeading: CarServer_LocationState.OneOf_OptionalHeading?
        get() = this._optionalHeading
        set(value) {
            this._optionalHeading = value
            _protobufMutated()
        }

    var heading: UInt
        get() = (this._optionalHeading as? CarServer_LocationState.OneOf_OptionalHeading.heading)?.value ?: 0u
        set(value) {
            this.optionalHeading = CarServer_LocationState.OneOf_OptionalHeading.heading(value)
        }

    var optionalGpsAsOf: CarServer_LocationState.OneOf_OptionalGpsAsOf?
        get() = this._optionalGpsAsOf
        set(value) {
            this._optionalGpsAsOf = value
            _protobufMutated()
        }

    var gpsAsOf: ULong
        get() = (this._optionalGpsAsOf as? CarServer_LocationState.OneOf_OptionalGpsAsOf.gpsAsOf)?.value ?: 0uL
        set(value) {
            this.optionalGpsAsOf = CarServer_LocationState.OneOf_OptionalGpsAsOf.gpsAsOf(value)
        }

    var optionalNativeLocationSupported: CarServer_LocationState.OneOf_OptionalNativeLocationSupported?
        get() = this._optionalNativeLocationSupported
        set(value) {
            this._optionalNativeLocationSupported = value
            _protobufMutated()
        }

    var nativeLocationSupported: Boolean
        get() = (this._optionalNativeLocationSupported as? CarServer_LocationState.OneOf_OptionalNativeLocationSupported.nativeLocationSupported)?.value ?: false
        set(value) {
            this.optionalNativeLocationSupported = CarServer_LocationState.OneOf_OptionalNativeLocationSupported.nativeLocationSupported(value)
        }

    var optionalNativeLatitude: CarServer_LocationState.OneOf_OptionalNativeLatitude?
        get() = this._optionalNativeLatitude
        set(value) {
            this._optionalNativeLatitude = value
            _protobufMutated()
        }

    var nativeLatitude: Float
        get() = (this._optionalNativeLatitude as? CarServer_LocationState.OneOf_OptionalNativeLatitude.nativeLatitude)?.value ?: 0f
        set(value) {
            this.optionalNativeLatitude = CarServer_LocationState.OneOf_OptionalNativeLatitude.nativeLatitude(value)
        }

    var optionalNativeLongitude: CarServer_LocationState.OneOf_OptionalNativeLongitude?
        get() = this._optionalNativeLongitude
        set(value) {
            this._optionalNativeLongitude = value
            _protobufMutated()
        }

    var nativeLongitude: Float
        get() = (this._optionalNativeLongitude as? CarServer_LocationState.OneOf_OptionalNativeLongitude.nativeLongitude)?.value ?: 0f
        set(value) {
            this.optionalNativeLongitude = CarServer_LocationState.OneOf_OptionalNativeLongitude.nativeLongitude(value)
        }

    var nativeType: CarServer_LocationState.GPSCoordinateType
        get() = this._nativeType ?: _protobufPending(8, { CarServer_LocationState.GPSCoordinateType() }) { this._nativeType = it }
        set(value) {
            _protobufDropPending(8)
            this._nativeType = value.copy()
            _protobufMutated()
        }

    val hasNativeType: Boolean
        get() = this._nativeType != null

    fun clearNativeType() {
        _protobufDropPending(8)
        this._nativeType = null
        _protobufMutated()
    }

    var optionalCorrectedLatitude: CarServer_LocationState.OneOf_OptionalCorrectedLatitude?
        get() = this._optionalCorrectedLatitude
        set(value) {
            this._optionalCorrectedLatitude = value
            _protobufMutated()
        }

    var correctedLatitude: Float
        get() = (this._optionalCorrectedLatitude as? CarServer_LocationState.OneOf_OptionalCorrectedLatitude.correctedLatitude)?.value ?: 0f
        set(value) {
            this.optionalCorrectedLatitude = CarServer_LocationState.OneOf_OptionalCorrectedLatitude.correctedLatitude(value)
        }

    var optionalCorrectedLongitude: CarServer_LocationState.OneOf_OptionalCorrectedLongitude?
        get() = this._optionalCorrectedLongitude
        set(value) {
            this._optionalCorrectedLongitude = value
            _protobufMutated()
        }

    var correctedLongitude: Float
        get() = (this._optionalCorrectedLongitude as? CarServer_LocationState.OneOf_OptionalCorrectedLongitude.correctedLongitude)?.value ?: 0f
        set(value) {
            this.optionalCorrectedLongitude = CarServer_LocationState.OneOf_OptionalCorrectedLongitude.correctedLongitude(value)
        }

    var timestamp: Google_Protobuf_Timestamp
        get() = this._timestamp ?: _protobufPending(11, { Google_Protobuf_Timestamp() }) { this._timestamp = it }
        set(value) {
            _protobufDropPending(11)
            this._timestamp = value.copy()
            _protobufMutated()
        }

    val hasTimestamp: Boolean
        get() = this._timestamp != null

    fun clearTimestamp() {
        _protobufDropPending(11)
        this._timestamp = null
        _protobufMutated()
    }

    var optionalHomelinkNearby: CarServer_LocationState.OneOf_OptionalHomelinkNearby?
        get() = this._optionalHomelinkNearby
        set(value) {
            this._optionalHomelinkNearby = value
            _protobufMutated()
        }

    var homelinkNearby: Boolean
        get() = (this._optionalHomelinkNearby as? CarServer_LocationState.OneOf_OptionalHomelinkNearby.homelinkNearby)?.value ?: false
        set(value) {
            this.optionalHomelinkNearby = CarServer_LocationState.OneOf_OptionalHomelinkNearby.homelinkNearby(value)
        }

    var optionalLocationName: CarServer_LocationState.OneOf_OptionalLocationName?
        get() = this._optionalLocationName
        set(value) {
            this._optionalLocationName = value
            _protobufMutated()
        }

    var locationName: String
        get() = (this._optionalLocationName as? CarServer_LocationState.OneOf_OptionalLocationName.locationName)?.value ?: ""
        set(value) {
            this.optionalLocationName = CarServer_LocationState.OneOf_OptionalLocationName.locationName(value)
        }

    var optionalGeoLatitude: CarServer_LocationState.OneOf_OptionalGeoLatitude?
        get() = this._optionalGeoLatitude
        set(value) {
            this._optionalGeoLatitude = value
            _protobufMutated()
        }

    var geoLatitude: Float
        get() = (this._optionalGeoLatitude as? CarServer_LocationState.OneOf_OptionalGeoLatitude.geoLatitude)?.value ?: 0f
        set(value) {
            this.optionalGeoLatitude = CarServer_LocationState.OneOf_OptionalGeoLatitude.geoLatitude(value)
        }

    var optionalGeoLongitude: CarServer_LocationState.OneOf_OptionalGeoLongitude?
        get() = this._optionalGeoLongitude
        set(value) {
            this._optionalGeoLongitude = value
            _protobufMutated()
        }

    var geoLongitude: Float
        get() = (this._optionalGeoLongitude as? CarServer_LocationState.OneOf_OptionalGeoLongitude.geoLongitude)?.value ?: 0f
        set(value) {
            this.optionalGeoLongitude = CarServer_LocationState.OneOf_OptionalGeoLongitude.geoLongitude(value)
        }

    var optionalGeoHeading: CarServer_LocationState.OneOf_OptionalGeoHeading?
        get() = this._optionalGeoHeading
        set(value) {
            this._optionalGeoHeading = value
            _protobufMutated()
        }

    var geoHeading: Float
        get() = (this._optionalGeoHeading as? CarServer_LocationState.OneOf_OptionalGeoHeading.geoHeading)?.value ?: 0f
        set(value) {
            this.optionalGeoHeading = CarServer_LocationState.OneOf_OptionalGeoHeading.geoHeading(value)
        }

    var optionalGeoElevation: CarServer_LocationState.OneOf_OptionalGeoElevation?
        get() = this._optionalGeoElevation
        set(value) {
            this._optionalGeoElevation = value
            _protobufMutated()
        }

    var geoElevation: Float
        get() = (this._optionalGeoElevation as? CarServer_LocationState.OneOf_OptionalGeoElevation.geoElevation)?.value ?: 0f
        set(value) {
            this.optionalGeoElevation = CarServer_LocationState.OneOf_OptionalGeoElevation.geoElevation(value)
        }

    var optionalGeoAccuracy: CarServer_LocationState.OneOf_OptionalGeoAccuracy?
        get() = this._optionalGeoAccuracy
        set(value) {
            this._optionalGeoAccuracy = value
            _protobufMutated()
        }

    var geoAccuracy: Float
        get() = (this._optionalGeoAccuracy as? CarServer_LocationState.OneOf_OptionalGeoAccuracy.geoAccuracy)?.value ?: 0f
        set(value) {
            this.optionalGeoAccuracy = CarServer_LocationState.OneOf_OptionalGeoAccuracy.geoAccuracy(value)
        }

    var optionalEstimatedGpsValid: CarServer_LocationState.OneOf_OptionalEstimatedGpsValid?
        get() = this._optionalEstimatedGpsValid
        set(value) {
            this._optionalEstimatedGpsValid = value
            _protobufMutated()
        }

    var estimatedGpsValid: Boolean
        get() = (this._optionalEstimatedGpsValid as? CarServer_LocationState.OneOf_OptionalEstimatedGpsValid.estimatedGpsValid)?.value ?: false
        set(value) {
            this.optionalEstimatedGpsValid = CarServer_LocationState.OneOf_OptionalEstimatedGpsValid.estimatedGpsValid(value)
        }

    var optionalEstimatedToRawDistance: CarServer_LocationState.OneOf_OptionalEstimatedToRawDistance?
        get() = this._optionalEstimatedToRawDistance
        set(value) {
            this._optionalEstimatedToRawDistance = value
            _protobufMutated()
        }

    var estimatedToRawDistance: Float
        get() = (this._optionalEstimatedToRawDistance as? CarServer_LocationState.OneOf_OptionalEstimatedToRawDistance.estimatedToRawDistance)?.value ?: 0f
        set(value) {
            this.optionalEstimatedToRawDistance = CarServer_LocationState.OneOf_OptionalEstimatedToRawDistance.estimatedToRawDistance(value)
        }

    sealed class OneOf_OptionalLatitude(value: Any) : ProtobufOneofCase(value) {
        class latitude(val value: Float) : OneOf_OptionalLatitude(value)
    }

    sealed class OneOf_OptionalLongitude(value: Any) : ProtobufOneofCase(value) {
        class longitude(val value: Float) : OneOf_OptionalLongitude(value)
    }

    sealed class OneOf_OptionalHeading(value: Any) : ProtobufOneofCase(value) {
        class heading(val value: UInt) : OneOf_OptionalHeading(value)
    }

    sealed class OneOf_OptionalGpsAsOf(value: Any) : ProtobufOneofCase(value) {
        class gpsAsOf(val value: ULong) : OneOf_OptionalGpsAsOf(value)
    }

    sealed class OneOf_OptionalNativeLocationSupported(value: Any) : ProtobufOneofCase(value) {
        class nativeLocationSupported(val value: Boolean) : OneOf_OptionalNativeLocationSupported(value)
    }

    sealed class OneOf_OptionalNativeLatitude(value: Any) : ProtobufOneofCase(value) {
        class nativeLatitude(val value: Float) : OneOf_OptionalNativeLatitude(value)
    }

    sealed class OneOf_OptionalNativeLongitude(value: Any) : ProtobufOneofCase(value) {
        class nativeLongitude(val value: Float) : OneOf_OptionalNativeLongitude(value)
    }

    sealed class OneOf_OptionalCorrectedLatitude(value: Any) : ProtobufOneofCase(value) {
        class correctedLatitude(val value: Float) : OneOf_OptionalCorrectedLatitude(value)
    }

    sealed class OneOf_OptionalCorrectedLongitude(value: Any) : ProtobufOneofCase(value) {
        class correctedLongitude(val value: Float) : OneOf_OptionalCorrectedLongitude(value)
    }

    sealed class OneOf_OptionalHomelinkNearby(value: Any) : ProtobufOneofCase(value) {
        class homelinkNearby(val value: Boolean) : OneOf_OptionalHomelinkNearby(value)
    }

    sealed class OneOf_OptionalLocationName(value: Any) : ProtobufOneofCase(value) {
        class locationName(val value: String) : OneOf_OptionalLocationName(value)
    }

    sealed class OneOf_OptionalGeoLatitude(value: Any) : ProtobufOneofCase(value) {
        class geoLatitude(val value: Float) : OneOf_OptionalGeoLatitude(value)
    }

    sealed class OneOf_OptionalGeoLongitude(value: Any) : ProtobufOneofCase(value) {
        class geoLongitude(val value: Float) : OneOf_OptionalGeoLongitude(value)
    }

    sealed class OneOf_OptionalGeoHeading(value: Any) : ProtobufOneofCase(value) {
        class geoHeading(val value: Float) : OneOf_OptionalGeoHeading(value)
    }

    sealed class OneOf_OptionalGeoElevation(value: Any) : ProtobufOneofCase(value) {
        class geoElevation(val value: Float) : OneOf_OptionalGeoElevation(value)
    }

    sealed class OneOf_OptionalGeoAccuracy(value: Any) : ProtobufOneofCase(value) {
        class geoAccuracy(val value: Float) : OneOf_OptionalGeoAccuracy(value)
    }

    sealed class OneOf_OptionalEstimatedGpsValid(value: Any) : ProtobufOneofCase(value) {
        class estimatedGpsValid(val value: Boolean) : OneOf_OptionalEstimatedGpsValid(value)
    }

    sealed class OneOf_OptionalEstimatedToRawDistance(value: Any) : ProtobufOneofCase(value) {
        class estimatedToRawDistance(val value: Float) : OneOf_OptionalEstimatedToRawDistance(value)
    }

    class GPSCoordinateType() : GeneratedMessage() {
        constructor(serializedBytes: ByteArray) : this() {
            merge(serializedBytes)
        }

        var type: CarServer_LocationState.GPSCoordinateType.OneOf_Type?
            get() = this._type
            set(value) {
                this._protobufStore_type(this._protobufCopy_type(value))
                _protobufMutated()
            }

        var gcj: CarServer_Void
            get() = (this._type as? CarServer_LocationState.GPSCoordinateType.OneOf_Type.gcj)?.value
                ?: _protobufPending(1, { CarServer_Void() }) { this._protobufStore_type(CarServer_LocationState.GPSCoordinateType.OneOf_Type.gcj(it)) }
            set(value) {
                this.type = CarServer_LocationState.GPSCoordinateType.OneOf_Type.gcj(value)
            }

        var wgs: CarServer_Void
            get() = (this._type as? CarServer_LocationState.GPSCoordinateType.OneOf_Type.wgs)?.value
                ?: _protobufPending(2, { CarServer_Void() }) { this._protobufStore_type(CarServer_LocationState.GPSCoordinateType.OneOf_Type.wgs(it)) }
            set(value) {
                this.type = CarServer_LocationState.GPSCoordinateType.OneOf_Type.wgs(value)
            }

        sealed class OneOf_Type(value: Any) : ProtobufOneofCase(value) {
            class gcj(val value: CarServer_Void) : OneOf_Type(value)
            class wgs(val value: CarServer_Void) : OneOf_Type(value)
        }

        private var _type: CarServer_LocationState.GPSCoordinateType.OneOf_Type? = null

        private fun _protobufStore_type(value: CarServer_LocationState.GPSCoordinateType.OneOf_Type?) {
            _protobufDropPending(1)
            _protobufDropPending(2)
            this._type = value
        }

        private fun _protobufCopy_type(value: CarServer_LocationState.GPSCoordinateType.OneOf_Type?): CarServer_LocationState.GPSCoordinateType.OneOf_Type? =
            when (value) {
                is CarServer_LocationState.GPSCoordinateType.OneOf_Type.gcj -> CarServer_LocationState.GPSCoordinateType.OneOf_Type.gcj(value.value.copy())
                is CarServer_LocationState.GPSCoordinateType.OneOf_Type.wgs -> CarServer_LocationState.GPSCoordinateType.OneOf_Type.wgs(value.value.copy())
                else -> value
            }

        override fun decodeMessage(decoder: BinaryDecoder) {
            while (true) {
                when (decoder.nextFieldNumber() ?: return) {
                    1 -> decoder.decodeSingularMessageField((this._type as? CarServer_LocationState.GPSCoordinateType.OneOf_Type.gcj)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_LocationState.GPSCoordinateType.OneOf_Type.gcj(it)) }
                    2 -> decoder.decodeSingularMessageField((this._type as? CarServer_LocationState.GPSCoordinateType.OneOf_Type.wgs)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_LocationState.GPSCoordinateType.OneOf_Type.wgs(it)) }
                }
            }
        }

        override fun traverse(visitor: BinaryEncodingVisitor) {
            (this._type as? CarServer_LocationState.GPSCoordinateType.OneOf_Type.gcj)?.let {
                visitor.visitSingularMessageField(it.value, 1)
            }
            (this._type as? CarServer_LocationState.GPSCoordinateType.OneOf_Type.wgs)?.let {
                visitor.visitSingularMessageField(it.value, 2)
            }
            visitor.visitUnknown(unknownFields)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CarServer_LocationState.GPSCoordinateType) return false
            if (this._type != other._type) return false
            return this.unknownFields.contentEquals(other.unknownFields)
        }

        override fun hashCode(): Int {
            var hash = 0
            hash = 31 * hash + (this._type?.hashCode() ?: 0)
            hash = 31 * hash + this.unknownFields.contentHashCode()
            return hash
        }

        override fun copy(): CarServer_LocationState.GPSCoordinateType {
            val result = CarServer_LocationState.GPSCoordinateType()
            result._type = this._protobufCopy_type(this._type)
            result.unknownFields = this.unknownFields
            return result
        }

        companion object {
            const val protoMessageName: String = "CarServer.LocationState.GPSCoordinateType"

            fun with(block: CarServer_LocationState.GPSCoordinateType.() -> Unit): CarServer_LocationState.GPSCoordinateType =
                CarServer_LocationState.GPSCoordinateType().apply(block)
        }
    }

    private var _optionalLatitude: CarServer_LocationState.OneOf_OptionalLatitude? = null
    private var _optionalLongitude: CarServer_LocationState.OneOf_OptionalLongitude? = null
    private var _optionalHeading: CarServer_LocationState.OneOf_OptionalHeading? = null
    private var _optionalGpsAsOf: CarServer_LocationState.OneOf_OptionalGpsAsOf? = null
    private var _optionalNativeLocationSupported: CarServer_LocationState.OneOf_OptionalNativeLocationSupported? = null
    private var _optionalNativeLatitude: CarServer_LocationState.OneOf_OptionalNativeLatitude? = null
    private var _optionalNativeLongitude: CarServer_LocationState.OneOf_OptionalNativeLongitude? = null
    private var _nativeType: CarServer_LocationState.GPSCoordinateType? = null
    private var _optionalCorrectedLatitude: CarServer_LocationState.OneOf_OptionalCorrectedLatitude? = null
    private var _optionalCorrectedLongitude: CarServer_LocationState.OneOf_OptionalCorrectedLongitude? = null
    private var _timestamp: Google_Protobuf_Timestamp? = null
    private var _optionalHomelinkNearby: CarServer_LocationState.OneOf_OptionalHomelinkNearby? = null
    private var _optionalLocationName: CarServer_LocationState.OneOf_OptionalLocationName? = null
    private var _optionalGeoLatitude: CarServer_LocationState.OneOf_OptionalGeoLatitude? = null
    private var _optionalGeoLongitude: CarServer_LocationState.OneOf_OptionalGeoLongitude? = null
    private var _optionalGeoHeading: CarServer_LocationState.OneOf_OptionalGeoHeading? = null
    private var _optionalGeoElevation: CarServer_LocationState.OneOf_OptionalGeoElevation? = null
    private var _optionalGeoAccuracy: CarServer_LocationState.OneOf_OptionalGeoAccuracy? = null
    private var _optionalEstimatedGpsValid: CarServer_LocationState.OneOf_OptionalEstimatedGpsValid? = null
    private var _optionalEstimatedToRawDistance: CarServer_LocationState.OneOf_OptionalEstimatedToRawDistance? = null

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                8 -> decoder.decodeSingularMessageField(this._nativeType) { CarServer_LocationState.GPSCoordinateType() }?.let {
                    _protobufDropPending(8)
                    this._nativeType = it
                }
                11 -> decoder.decodeSingularMessageField(this._timestamp) { Google_Protobuf_Timestamp() }?.let {
                    _protobufDropPending(11)
                    this._timestamp = it
                }
                101 -> decoder.decodeSingularFloatField()?.let { this._optionalLatitude = CarServer_LocationState.OneOf_OptionalLatitude.latitude(it) }
                102 -> decoder.decodeSingularFloatField()?.let { this._optionalLongitude = CarServer_LocationState.OneOf_OptionalLongitude.longitude(it) }
                103 -> decoder.decodeSingularUInt32Field()?.let { this._optionalHeading = CarServer_LocationState.OneOf_OptionalHeading.heading(it) }
                104 -> decoder.decodeSingularUInt64Field()?.let { this._optionalGpsAsOf = CarServer_LocationState.OneOf_OptionalGpsAsOf.gpsAsOf(it) }
                105 -> decoder.decodeSingularBoolField()?.let { this._optionalNativeLocationSupported = CarServer_LocationState.OneOf_OptionalNativeLocationSupported.nativeLocationSupported(it) }
                106 -> decoder.decodeSingularFloatField()?.let { this._optionalNativeLatitude = CarServer_LocationState.OneOf_OptionalNativeLatitude.nativeLatitude(it) }
                107 -> decoder.decodeSingularFloatField()?.let { this._optionalNativeLongitude = CarServer_LocationState.OneOf_OptionalNativeLongitude.nativeLongitude(it) }
                109 -> decoder.decodeSingularFloatField()?.let { this._optionalCorrectedLatitude = CarServer_LocationState.OneOf_OptionalCorrectedLatitude.correctedLatitude(it) }
                110 -> decoder.decodeSingularFloatField()?.let { this._optionalCorrectedLongitude = CarServer_LocationState.OneOf_OptionalCorrectedLongitude.correctedLongitude(it) }
                112 -> decoder.decodeSingularBoolField()?.let { this._optionalHomelinkNearby = CarServer_LocationState.OneOf_OptionalHomelinkNearby.homelinkNearby(it) }
                113 -> decoder.decodeSingularStringField()?.let { this._optionalLocationName = CarServer_LocationState.OneOf_OptionalLocationName.locationName(it) }
                114 -> decoder.decodeSingularFloatField()?.let { this._optionalGeoLatitude = CarServer_LocationState.OneOf_OptionalGeoLatitude.geoLatitude(it) }
                115 -> decoder.decodeSingularFloatField()?.let { this._optionalGeoLongitude = CarServer_LocationState.OneOf_OptionalGeoLongitude.geoLongitude(it) }
                116 -> decoder.decodeSingularFloatField()?.let { this._optionalGeoHeading = CarServer_LocationState.OneOf_OptionalGeoHeading.geoHeading(it) }
                117 -> decoder.decodeSingularFloatField()?.let { this._optionalGeoElevation = CarServer_LocationState.OneOf_OptionalGeoElevation.geoElevation(it) }
                118 -> decoder.decodeSingularFloatField()?.let { this._optionalGeoAccuracy = CarServer_LocationState.OneOf_OptionalGeoAccuracy.geoAccuracy(it) }
                119 -> decoder.decodeSingularBoolField()?.let { this._optionalEstimatedGpsValid = CarServer_LocationState.OneOf_OptionalEstimatedGpsValid.estimatedGpsValid(it) }
                120 -> decoder.decodeSingularFloatField()?.let { this._optionalEstimatedToRawDistance = CarServer_LocationState.OneOf_OptionalEstimatedToRawDistance.estimatedToRawDistance(it) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        this._nativeType?.let {
            visitor.visitSingularMessageField(it, 8)
        }
        this._timestamp?.let {
            visitor.visitSingularMessageField(it, 11)
        }
        (this._optionalLatitude as? CarServer_LocationState.OneOf_OptionalLatitude.latitude)?.let {
            visitor.visitSingularFloatField(it.value, 101)
        }
        (this._optionalLongitude as? CarServer_LocationState.OneOf_OptionalLongitude.longitude)?.let {
            visitor.visitSingularFloatField(it.value, 102)
        }
        (this._optionalHeading as? CarServer_LocationState.OneOf_OptionalHeading.heading)?.let {
            visitor.visitSingularUInt32Field(it.value, 103)
        }
        (this._optionalGpsAsOf as? CarServer_LocationState.OneOf_OptionalGpsAsOf.gpsAsOf)?.let {
            visitor.visitSingularUInt64Field(it.value, 104)
        }
        (this._optionalNativeLocationSupported as? CarServer_LocationState.OneOf_OptionalNativeLocationSupported.nativeLocationSupported)?.let {
            visitor.visitSingularBoolField(it.value, 105)
        }
        (this._optionalNativeLatitude as? CarServer_LocationState.OneOf_OptionalNativeLatitude.nativeLatitude)?.let {
            visitor.visitSingularFloatField(it.value, 106)
        }
        (this._optionalNativeLongitude as? CarServer_LocationState.OneOf_OptionalNativeLongitude.nativeLongitude)?.let {
            visitor.visitSingularFloatField(it.value, 107)
        }
        (this._optionalCorrectedLatitude as? CarServer_LocationState.OneOf_OptionalCorrectedLatitude.correctedLatitude)?.let {
            visitor.visitSingularFloatField(it.value, 109)
        }
        (this._optionalCorrectedLongitude as? CarServer_LocationState.OneOf_OptionalCorrectedLongitude.correctedLongitude)?.let {
            visitor.visitSingularFloatField(it.value, 110)
        }
        (this._optionalHomelinkNearby as? CarServer_LocationState.OneOf_OptionalHomelinkNearby.homelinkNearby)?.let {
            visitor.visitSingularBoolField(it.value, 112)
        }
        (this._optionalLocationName as? CarServer_LocationState.OneOf_OptionalLocationName.locationName)?.let {
            visitor.visitSingularStringField(it.value, 113)
        }
        (this._optionalGeoLatitude as? CarServer_LocationState.OneOf_OptionalGeoLatitude.geoLatitude)?.let {
            visitor.visitSingularFloatField(it.value, 114)
        }
        (this._optionalGeoLongitude as? CarServer_LocationState.OneOf_OptionalGeoLongitude.geoLongitude)?.let {
            visitor.visitSingularFloatField(it.value, 115)
        }
        (this._optionalGeoHeading as? CarServer_LocationState.OneOf_OptionalGeoHeading.geoHeading)?.let {
            visitor.visitSingularFloatField(it.value, 116)
        }
        (this._optionalGeoElevation as? CarServer_LocationState.OneOf_OptionalGeoElevation.geoElevation)?.let {
            visitor.visitSingularFloatField(it.value, 117)
        }
        (this._optionalGeoAccuracy as? CarServer_LocationState.OneOf_OptionalGeoAccuracy.geoAccuracy)?.let {
            visitor.visitSingularFloatField(it.value, 118)
        }
        (this._optionalEstimatedGpsValid as? CarServer_LocationState.OneOf_OptionalEstimatedGpsValid.estimatedGpsValid)?.let {
            visitor.visitSingularBoolField(it.value, 119)
        }
        (this._optionalEstimatedToRawDistance as? CarServer_LocationState.OneOf_OptionalEstimatedToRawDistance.estimatedToRawDistance)?.let {
            visitor.visitSingularFloatField(it.value, 120)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_LocationState) return false
        if (this._optionalLatitude != other._optionalLatitude) return false
        if (this._optionalLongitude != other._optionalLongitude) return false
        if (this._optionalHeading != other._optionalHeading) return false
        if (this._optionalGpsAsOf != other._optionalGpsAsOf) return false
        if (this._optionalNativeLocationSupported != other._optionalNativeLocationSupported) return false
        if (this._optionalNativeLatitude != other._optionalNativeLatitude) return false
        if (this._optionalNativeLongitude != other._optionalNativeLongitude) return false
        if (this._nativeType != other._nativeType) return false
        if (this._optionalCorrectedLatitude != other._optionalCorrectedLatitude) return false
        if (this._optionalCorrectedLongitude != other._optionalCorrectedLongitude) return false
        if (this._timestamp != other._timestamp) return false
        if (this._optionalHomelinkNearby != other._optionalHomelinkNearby) return false
        if (this._optionalLocationName != other._optionalLocationName) return false
        if (this._optionalGeoLatitude != other._optionalGeoLatitude) return false
        if (this._optionalGeoLongitude != other._optionalGeoLongitude) return false
        if (this._optionalGeoHeading != other._optionalGeoHeading) return false
        if (this._optionalGeoElevation != other._optionalGeoElevation) return false
        if (this._optionalGeoAccuracy != other._optionalGeoAccuracy) return false
        if (this._optionalEstimatedGpsValid != other._optionalEstimatedGpsValid) return false
        if (this._optionalEstimatedToRawDistance != other._optionalEstimatedToRawDistance) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._optionalLatitude?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalLongitude?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalHeading?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalGpsAsOf?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalNativeLocationSupported?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalNativeLatitude?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalNativeLongitude?.hashCode() ?: 0)
        hash = 31 * hash + (this._nativeType?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalCorrectedLatitude?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalCorrectedLongitude?.hashCode() ?: 0)
        hash = 31 * hash + (this._timestamp?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalHomelinkNearby?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalLocationName?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalGeoLatitude?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalGeoLongitude?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalGeoHeading?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalGeoElevation?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalGeoAccuracy?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalEstimatedGpsValid?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalEstimatedToRawDistance?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_LocationState {
        val result = CarServer_LocationState()
        result._optionalLatitude = this._optionalLatitude
        result._optionalLongitude = this._optionalLongitude
        result._optionalHeading = this._optionalHeading
        result._optionalGpsAsOf = this._optionalGpsAsOf
        result._optionalNativeLocationSupported = this._optionalNativeLocationSupported
        result._optionalNativeLatitude = this._optionalNativeLatitude
        result._optionalNativeLongitude = this._optionalNativeLongitude
        result._nativeType = this._nativeType?.copy()
        result._optionalCorrectedLatitude = this._optionalCorrectedLatitude
        result._optionalCorrectedLongitude = this._optionalCorrectedLongitude
        result._timestamp = this._timestamp?.copy()
        result._optionalHomelinkNearby = this._optionalHomelinkNearby
        result._optionalLocationName = this._optionalLocationName
        result._optionalGeoLatitude = this._optionalGeoLatitude
        result._optionalGeoLongitude = this._optionalGeoLongitude
        result._optionalGeoHeading = this._optionalGeoHeading
        result._optionalGeoElevation = this._optionalGeoElevation
        result._optionalGeoAccuracy = this._optionalGeoAccuracy
        result._optionalEstimatedGpsValid = this._optionalEstimatedGpsValid
        result._optionalEstimatedToRawDistance = this._optionalEstimatedToRawDistance
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.LocationState"

        fun with(block: CarServer_LocationState.() -> Unit): CarServer_LocationState =
            CarServer_LocationState().apply(block)
    }
}

class CarServer_VehicleState() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var guestMode: CarServer_VehicleState.GuestMode
        get() = this._guestMode ?: _protobufPending(74, { CarServer_VehicleState.GuestMode() }) { this._guestMode = it }
        set(value) {
            _protobufDropPending(74)
            this._guestMode = value.copy()
            _protobufMutated()
        }

    val hasGuestMode: Boolean
        get() = this._guestMode != null

    fun clearGuestMode() {
        _protobufDropPending(74)
        this._guestMode = null
        _protobufMutated()
    }

    class GuestMode() : GeneratedMessage() {
        constructor(serializedBytes: ByteArray) : this() {
            merge(serializedBytes)
        }

        var guestModeActive: Boolean = false
            set(value) {
                field = value
                _protobufMutated()
            }

        override fun decodeMessage(decoder: BinaryDecoder) {
            while (true) {
                when (decoder.nextFieldNumber() ?: return) {
                    1 -> decoder.decodeSingularBoolField()?.let { this.guestModeActive = it }
                }
            }
        }

        override fun traverse(visitor: BinaryEncodingVisitor) {
            if (this.guestModeActive) {
                visitor.visitSingularBoolField(this.guestModeActive, 1)
            }
            visitor.visitUnknown(unknownFields)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CarServer_VehicleState.GuestMode) return false
            if (this.guestModeActive != other.guestModeActive) return false
            return this.unknownFields.contentEquals(other.unknownFields)
        }

        override fun hashCode(): Int {
            var hash = 0
            hash = 31 * hash + this.guestModeActive.hashCode()
            hash = 31 * hash + this.unknownFields.contentHashCode()
            return hash
        }

        override fun copy(): CarServer_VehicleState.GuestMode {
            val result = CarServer_VehicleState.GuestMode()
            result.guestModeActive = this.guestModeActive
            result.unknownFields = this.unknownFields
            return result
        }

        companion object {
            const val protoMessageName: String = "CarServer.VehicleState.GuestMode"

            fun with(block: CarServer_VehicleState.GuestMode.() -> Unit): CarServer_VehicleState.GuestMode =
                CarServer_VehicleState.GuestMode().apply(block)
        }
    }

    private var _guestMode: CarServer_VehicleState.GuestMode? = null

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                74 -> decoder.decodeSingularMessageField(this._guestMode) { CarServer_VehicleState.GuestMode() }?.let {
                    _protobufDropPending(74)
                    this._guestMode = it
                }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        this._guestMode?.let {
            visitor.visitSingularMessageField(it, 74)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_VehicleState) return false
        if (this._guestMode != other._guestMode) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._guestMode?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_VehicleState {
        val result = CarServer_VehicleState()
        result._guestMode = this._guestMode?.copy()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.VehicleState"

        fun with(block: CarServer_VehicleState.() -> Unit): CarServer_VehicleState =
            CarServer_VehicleState().apply(block)
    }
}

class CarServer_ClimateState() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var optionalInsideTempCelsius: CarServer_ClimateState.OneOf_OptionalInsideTempCelsius?
        get() = this._optionalInsideTempCelsius
        set(value) {
            this._optionalInsideTempCelsius = value
            _protobufMutated()
        }

    var insideTempCelsius: Float
        get() = (this._optionalInsideTempCelsius as? CarServer_ClimateState.OneOf_OptionalInsideTempCelsius.insideTempCelsius)?.value ?: 0f
        set(value) {
            this.optionalInsideTempCelsius = CarServer_ClimateState.OneOf_OptionalInsideTempCelsius.insideTempCelsius(value)
        }

    var optionalOutsideTempCelsius: CarServer_ClimateState.OneOf_OptionalOutsideTempCelsius?
        get() = this._optionalOutsideTempCelsius
        set(value) {
            this._optionalOutsideTempCelsius = value
            _protobufMutated()
        }

    var outsideTempCelsius: Float
        get() = (this._optionalOutsideTempCelsius as? CarServer_ClimateState.OneOf_OptionalOutsideTempCelsius.outsideTempCelsius)?.value ?: 0f
        set(value) {
            this.optionalOutsideTempCelsius = CarServer_ClimateState.OneOf_OptionalOutsideTempCelsius.outsideTempCelsius(value)
        }

    var optionalDriverTempSetting: CarServer_ClimateState.OneOf_OptionalDriverTempSetting?
        get() = this._optionalDriverTempSetting
        set(value) {
            this._optionalDriverTempSetting = value
            _protobufMutated()
        }

    var driverTempSetting: Float
        get() = (this._optionalDriverTempSetting as? CarServer_ClimateState.OneOf_OptionalDriverTempSetting.driverTempSetting)?.value ?: 0f
        set(value) {
            this.optionalDriverTempSetting = CarServer_ClimateState.OneOf_OptionalDriverTempSetting.driverTempSetting(value)
        }

    var optionalPassengerTempSetting: CarServer_ClimateState.OneOf_OptionalPassengerTempSetting?
        get() = this._optionalPassengerTempSetting
        set(value) {
            this._optionalPassengerTempSetting = value
            _protobufMutated()
        }

    var passengerTempSetting: Float
        get() = (this._optionalPassengerTempSetting as? CarServer_ClimateState.OneOf_OptionalPassengerTempSetting.passengerTempSetting)?.value ?: 0f
        set(value) {
            this.optionalPassengerTempSetting = CarServer_ClimateState.OneOf_OptionalPassengerTempSetting.passengerTempSetting(value)
        }

    var optionalLeftTempDirection: CarServer_ClimateState.OneOf_OptionalLeftTempDirection?
        get() = this._optionalLeftTempDirection
        set(value) {
            this._optionalLeftTempDirection = value
            _protobufMutated()
        }

    var leftTempDirection: Int
        get() = (this._optionalLeftTempDirection as? CarServer_ClimateState.OneOf_OptionalLeftTempDirection.leftTempDirection)?.value ?: 0
        set(value) {
            this.optionalLeftTempDirection = CarServer_ClimateState.OneOf_OptionalLeftTempDirection.leftTempDirection(value)
        }

    var optionalRightTempDirection: CarServer_ClimateState.OneOf_OptionalRightTempDirection?
        get() = this._optionalRightTempDirection
        set(value) {
            this._optionalRightTempDirection = value
            _protobufMutated()
        }

    var rightTempDirection: Int
        get() = (this._optionalRightTempDirection as? CarServer_ClimateState.OneOf_OptionalRightTempDirection.rightTempDirection)?.value ?: 0
        set(value) {
            this.optionalRightTempDirection = CarServer_ClimateState.OneOf_OptionalRightTempDirection.rightTempDirection(value)
        }

    var optionalIsFrontDefrosterOn: CarServer_ClimateState.OneOf_OptionalIsFrontDefrosterOn?
        get() = this._optionalIsFrontDefrosterOn
        set(value) {
            this._optionalIsFrontDefrosterOn = value
            _protobufMutated()
        }

    var isFrontDefrosterOn: Boolean
        get() = (this._optionalIsFrontDefrosterOn as? CarServer_ClimateState.OneOf_OptionalIsFrontDefrosterOn.isFrontDefrosterOn)?.value ?: false
        set(value) {
            this.optionalIsFrontDefrosterOn = CarServer_ClimateState.OneOf_OptionalIsFrontDefrosterOn.isFrontDefrosterOn(value)
        }

    var optionalIsRearDefrosterOn: CarServer_ClimateState.OneOf_OptionalIsRearDefrosterOn?
        get() = this._optionalIsRearDefrosterOn
        set(value) {
            this._optionalIsRearDefrosterOn = value
            _protobufMutated()
        }

    var isRearDefrosterOn: Boolean
        get() = (this._optionalIsRearDefrosterOn as? CarServer_ClimateState.OneOf_OptionalIsRearDefrosterOn.isRearDefrosterOn)?.value ?: false
        set(value) {
            this.optionalIsRearDefrosterOn = CarServer_ClimateState.OneOf_OptionalIsRearDefrosterOn.isRearDefrosterOn(value)
        }

    var optionalFanStatus: CarServer_ClimateState.OneOf_OptionalFanStatus?
        get() = this._optionalFanStatus
        set(value) {
            this._optionalFanStatus = value
            _protobufMutated()
        }

    var fanStatus: Int
        get() = (this._optionalFanStatus as? CarServer_ClimateState.OneOf_OptionalFanStatus.fanStatus)?.value ?: 0
        set(value) {
            this.optionalFanStatus = CarServer_ClimateState.OneOf_OptionalFanStatus.fanStatus(value)
        }

    var optionalIsClimateOn: CarServer_ClimateState.OneOf_OptionalIsClimateOn?
        get() = this._optionalIsClimateOn
        set(value) {
            this._optionalIsClimateOn = value
            _protobufMutated()
        }

    var isClimateOn: Boolean
        get() = (this._optionalIsClimateOn as? CarServer_ClimateState.OneOf_OptionalIsClimateOn.isClimateOn)?.value ?: false
        set(value) {
            this.optionalIsClimateOn = CarServer_ClimateState.OneOf_OptionalIsClimateOn.isClimateOn(value)
        }

    var optionalMinAvailTempCelsius: CarServer_ClimateState.OneOf_OptionalMinAvailTempCelsius?
        get() = this._optionalMinAvailTempCelsius
        set(value) {
            this._optionalMinAvailTempCelsius = value
            _protobufMutated()
        }

    var minAvailTempCelsius: Float
        get() = (this._optionalMinAvailTempCelsius as? CarServer_ClimateState.OneOf_OptionalMinAvailTempCelsius.minAvailTempCelsius)?.value ?: 0f
        set(value) {
            this.optionalMinAvailTempCelsius = CarServer_ClimateState.OneOf_OptionalMinAvailTempCelsius.minAvailTempCelsius(value)
        }

    var optionalMaxAvailTempCelsius: CarServer_ClimateState.OneOf_OptionalMaxAvailTempCelsius?
        get() = this._optionalMaxAvailTempCelsius
        set(value) {
            this._optionalMaxAvailTempCelsius = value
            _protobufMutated()
        }

    var maxAvailTempCelsius: Float
        get() = (this._optionalMaxAvailTempCelsius as? CarServer_ClimateState.OneOf_OptionalMaxAvailTempCelsius.maxAvailTempCelsius)?.value ?: 0f
        set(value) {
            this.optionalMaxAvailTempCelsius = CarServer_ClimateState.OneOf_OptionalMaxAvailTempCelsius.maxAvailTempCelsius(value)
        }

    var optionalSeatHeaterLeft: CarServer_ClimateState.OneOf_OptionalSeatHeaterLeft?
        get() = this._optionalSeatHeaterLeft
        set(value) {
            this._optionalSeatHeaterLeft = value
            _protobufMutated()
        }

    var seatHeaterLeft: Int
        get() = (this._optionalSeatHeaterLeft as? CarServer_ClimateState.OneOf_OptionalSeatHeaterLeft.seatHeaterLeft)?.value ?: 0
        set(value) {
            this.optionalSeatHeaterLeft = CarServer_ClimateState.OneOf_OptionalSeatHeaterLeft.seatHeaterLeft(value)
        }

    var optionalSeatHeaterRight: CarServer_ClimateState.OneOf_OptionalSeatHeaterRight?
        get() = this._optionalSeatHeaterRight
        set(value) {
            this._optionalSeatHeaterRight = value
            _protobufMutated()
        }

    var seatHeaterRight: Int
        get() = (this._optionalSeatHeaterRight as? CarServer_ClimateState.OneOf_OptionalSeatHeaterRight.seatHeaterRight)?.value ?: 0
        set(value) {
            this.optionalSeatHeaterRight = CarServer_ClimateState.OneOf_OptionalSeatHeaterRight.seatHeaterRight(value)
        }

    var optionalSeatHeaterRearLeft: CarServer_ClimateState.OneOf_OptionalSeatHeaterRearLeft?
        get() = this._optionalSeatHeaterRearLeft
        set(value) {
            this._optionalSeatHeaterRearLeft = value
            _protobufMutated()
        }

    var seatHeaterRearLeft: Int
        get() = (this._optionalSeatHeaterRearLeft as? CarServer_ClimateState.OneOf_OptionalSeatHeaterRearLeft.seatHeaterRearLeft)?.value ?: 0
        set(value) {
            this.optionalSeatHeaterRearLeft = CarServer_ClimateState.OneOf_OptionalSeatHeaterRearLeft.seatHeaterRearLeft(value)
        }

    var optionalSeatHeaterRearRight: CarServer_ClimateState.OneOf_OptionalSeatHeaterRearRight?
        get() = this._optionalSeatHeaterRearRight
        set(value) {
            this._optionalSeatHeaterRearRight = value
            _protobufMutated()
        }

    var seatHeaterRearRight: Int
        get() = (this._optionalSeatHeaterRearRight as? CarServer_ClimateState.OneOf_OptionalSeatHeaterRearRight.seatHeaterRearRight)?.value ?: 0
        set(value) {
            this.optionalSeatHeaterRearRight = CarServer_ClimateState.OneOf_OptionalSeatHeaterRearRight.seatHeaterRearRight(value)
        }

    var optionalSeatHeaterRearCenter: CarServer_ClimateState.OneOf_OptionalSeatHeaterRearCenter?
        get() = this._optionalSeatHeaterRearCenter
        set(value) {
            this._optionalSeatHeaterRearCenter = value
            _protobufMutated()
        }

    var seatHeaterRearCenter: Int
        get() = (this._optionalSeatHeaterRearCenter as? CarServer_ClimateState.OneOf_OptionalSeatHeaterRearCenter.seatHeaterRearCenter)?.value ?: 0
        set(value) {
            this.optionalSeatHeaterRearCenter = CarServer_ClimateState.OneOf_OptionalSeatHeaterRearCenter.seatHeaterRearCenter(value)
        }

    var optionalSeatHeaterRearRightBack: CarServer_ClimateState.OneOf_OptionalSeatHeaterRearRightBack?
        get() = this._optionalSeatHeaterRearRightBack
        set(value) {
            this._optionalSeatHeaterRearRightBack = value
            _protobufMutated()
        }

    var seatHeaterRearRightBack: Int
        get() = (this._optionalSeatHeaterRearRightBack as? CarServer_ClimateState.OneOf_OptionalSeatHeaterRearRightBack.seatHeaterRearRightBack)?.value ?: 0
        set(value) {
            this.optionalSeatHeaterRearRightBack = CarServer_ClimateState.OneOf_OptionalSeatHeaterRearRightBack.seatHeaterRearRightBack(value)
        }

    var optionalSeatHeaterRearLeftBack: CarServer_ClimateState.OneOf_OptionalSeatHeaterRearLeftBack?
        get() = this._optionalSeatHeaterRearLeftBack
        set(value) {
            this._optionalSeatHeaterRearLeftBack = value
            _protobufMutated()
        }

    var seatHeaterRearLeftBack: Int
        get() = (this._optionalSeatHeaterRearLeftBack as? CarServer_ClimateState.OneOf_OptionalSeatHeaterRearLeftBack.seatHeaterRearLeftBack)?.value ?: 0
        set(value) {
            this.optionalSeatHeaterRearLeftBack = CarServer_ClimateState.OneOf_OptionalSeatHeaterRearLeftBack.seatHeaterRearLeftBack(value)
        }

    var optionalSeatHeaterThirdRowRight: CarServer_ClimateState.OneOf_OptionalSeatHeaterThirdRowRight?
        get() = this._optionalSeatHeaterThirdRowRight
        set(value) {
            this._optionalSeatHeaterThirdRowRight = value
            _protobufMutated()
        }

    var seatHeaterThirdRowRight: Int
        get() = (this._optionalSeatHeaterThirdRowRight as? CarServer_ClimateState.OneOf_OptionalSeatHeaterThirdRowRight.seatHeaterThirdRowRight)?.value ?: 0
        set(value) {
            this.optionalSeatHeaterThirdRowRight = CarServer_ClimateState.OneOf_OptionalSeatHeaterThirdRowRight.seatHeaterThirdRowRight(value)
        }

    var optionalSeatHeaterThirdRowLeft: CarServer_ClimateState.OneOf_OptionalSeatHeaterThirdRowLeft?
        get() = this._optionalSeatHeaterThirdRowLeft
        set(value) {
            this._optionalSeatHeaterThirdRowLeft = value
            _protobufMutated()
        }

    var seatHeaterThirdRowLeft: Int
        get() = (this._optionalSeatHeaterThirdRowLeft as? CarServer_ClimateState.OneOf_OptionalSeatHeaterThirdRowLeft.seatHeaterThirdRowLeft)?.value ?: 0
        set(value) {
            this.optionalSeatHeaterThirdRowLeft = CarServer_ClimateState.OneOf_OptionalSeatHeaterThirdRowLeft.seatHeaterThirdRowLeft(value)
        }

    var optionalBatteryHeater: CarServer_ClimateState.OneOf_OptionalBatteryHeater?
        get() = this._optionalBatteryHeater
        set(value) {
            this._optionalBatteryHeater = value
            _protobufMutated()
        }

    var batteryHeater: Boolean
        get() = (this._optionalBatteryHeater as? CarServer_ClimateState.OneOf_OptionalBatteryHeater.batteryHeater)?.value ?: false
        set(value) {
            this.optionalBatteryHeater = CarServer_ClimateState.OneOf_OptionalBatteryHeater.batteryHeater(value)
        }

    var optionalBatteryHeaterNoPower: CarServer_ClimateState.OneOf_OptionalBatteryHeaterNoPower?
        get() = this._optionalBatteryHeaterNoPower
        set(value) {
            this._optionalBatteryHeaterNoPower = value
            _protobufMutated()
        }

    var batteryHeaterNoPower: Boolean
        get() = (this._optionalBatteryHeaterNoPower as? CarServer_ClimateState.OneOf_OptionalBatteryHeaterNoPower.batteryHeaterNoPower)?.value ?: false
        set(value) {
            this.optionalBatteryHeaterNoPower = CarServer_ClimateState.OneOf_OptionalBatteryHeaterNoPower.batteryHeaterNoPower(value)
        }

    var optionalSteeringWheelHeater: CarServer_ClimateState.OneOf_OptionalSteeringWheelHeater?
        get() = this._optionalSteeringWheelHeater
        set(value) {
            this._optionalSteeringWheelHeater = value
            _protobufMutated()
        }

    var steeringWheelHeater: Boolean
        get() = (this._optionalSteeringWheelHeater as? CarServer_ClimateState.OneOf_OptionalSteeringWheelHeater.steeringWheelHeater)?.value ?: false
        set(value) {
            this.optionalSteeringWheelHeater = CarServer_ClimateState.OneOf_OptionalSteeringWheelHeater.steeringWheelHeater(value)
        }

    var optionalWiperBladeHeater: CarServer_ClimateState.OneOf_OptionalWiperBladeHeater?
        get() = this._optionalWiperBladeHeater
        set(value) {
            this._optionalWiperBladeHeater = value
            _protobufMutated()
        }

    var wiperBladeHeater: Boolean
        get() = (this._optionalWiperBladeHeater as? CarServer_ClimateState.OneOf_OptionalWiperBladeHeater.wiperBladeHeater)?.value ?: false
        set(value) {
            this.optionalWiperBladeHeater = CarServer_ClimateState.OneOf_OptionalWiperBladeHeater.wiperBladeHeater(value)
        }

    var optionalSideMirrorHeaters: CarServer_ClimateState.OneOf_OptionalSideMirrorHeaters?
        get() = this._optionalSideMirrorHeaters
        set(value) {
            this._optionalSideMirrorHeaters = value
            _protobufMutated()
        }

    var sideMirrorHeaters: Boolean
        get() = (this._optionalSideMirrorHeaters as? CarServer_ClimateState.OneOf_OptionalSideMirrorHeaters.sideMirrorHeaters)?.value ?: false
        set(value) {
            this.optionalSideMirrorHeaters = CarServer_ClimateState.OneOf_OptionalSideMirrorHeaters.sideMirrorHeaters(value)
        }

    var optionalIsPreconditioning: CarServer_ClimateState.OneOf_OptionalIsPreconditioning?
        get() = this._optionalIsPreconditioning
        set(value) {
            this._optionalIsPreconditioning = value
            _protobufMutated()
        }

    var isPreconditioning: Boolean
        get() = (this._optionalIsPreconditioning as? CarServer_ClimateState.OneOf_OptionalIsPreconditioning.isPreconditioning)?.value ?: false
        set(value) {
            this.optionalIsPreconditioning = CarServer_ClimateState.OneOf_OptionalIsPreconditioning.isPreconditioning(value)
        }

    var optionalRemoteHeaterControlEnabled: CarServer_ClimateState.OneOf_OptionalRemoteHeaterControlEnabled?
        get() = this._optionalRemoteHeaterControlEnabled
        set(value) {
            this._optionalRemoteHeaterControlEnabled = value
            _protobufMutated()
        }

    var remoteHeaterControlEnabled: Boolean
        get() = (this._optionalRemoteHeaterControlEnabled as? CarServer_ClimateState.OneOf_OptionalRemoteHeaterControlEnabled.remoteHeaterControlEnabled)?.value ?: false
        set(value) {
            this.optionalRemoteHeaterControlEnabled = CarServer_ClimateState.OneOf_OptionalRemoteHeaterControlEnabled.remoteHeaterControlEnabled(value)
        }

    var climateKeeperMode: CarServer_ClimateState.ClimateKeeperMode
        get() = this._climateKeeperMode ?: _protobufPending(30, { CarServer_ClimateState.ClimateKeeperMode() }) { this._climateKeeperMode = it }
        set(value) {
            _protobufDropPending(30)
            this._climateKeeperMode = value.copy()
            _protobufMutated()
        }

    val hasClimateKeeperMode: Boolean
        get() = this._climateKeeperMode != null

    fun clearClimateKeeperMode() {
        _protobufDropPending(30)
        this._climateKeeperMode = null
        _protobufMutated()
    }

    var timestamp: Google_Protobuf_Timestamp
        get() = this._timestamp ?: _protobufPending(33, { Google_Protobuf_Timestamp() }) { this._timestamp = it }
        set(value) {
            _protobufDropPending(33)
            this._timestamp = value.copy()
            _protobufMutated()
        }

    val hasTimestamp: Boolean
        get() = this._timestamp != null

    fun clearTimestamp() {
        _protobufDropPending(33)
        this._timestamp = null
        _protobufMutated()
    }

    var optionalBioweaponModeOn: CarServer_ClimateState.OneOf_OptionalBioweaponModeOn?
        get() = this._optionalBioweaponModeOn
        set(value) {
            this._optionalBioweaponModeOn = value
            _protobufMutated()
        }

    var bioweaponModeOn: Boolean
        get() = (this._optionalBioweaponModeOn as? CarServer_ClimateState.OneOf_OptionalBioweaponModeOn.bioweaponModeOn)?.value ?: false
        set(value) {
            this.optionalBioweaponModeOn = CarServer_ClimateState.OneOf_OptionalBioweaponModeOn.bioweaponModeOn(value)
        }

    var defrostMode: CarServer_ClimateState.DefrostMode
        get() = this._defrostMode ?: _protobufPending(35, { CarServer_ClimateState.DefrostMode() }) { this._defrostMode = it }
        set(value) {
            _protobufDropPending(35)
            this._defrostMode = value.copy()
            _protobufMutated()
        }

    val hasDefrostMode: Boolean
        get() = this._defrostMode != null

    fun clearDefrostMode() {
        _protobufDropPending(35)
        this._defrostMode = null
        _protobufMutated()
    }

    var optionalIsAutoConditioningOn: CarServer_ClimateState.OneOf_OptionalIsAutoConditioningOn?
        get() = this._optionalIsAutoConditioningOn
        set(value) {
            this._optionalIsAutoConditioningOn = value
            _protobufMutated()
        }

    var isAutoConditioningOn: Boolean
        get() = (this._optionalIsAutoConditioningOn as? CarServer_ClimateState.OneOf_OptionalIsAutoConditioningOn.isAutoConditioningOn)?.value ?: false
        set(value) {
            this.optionalIsAutoConditioningOn = CarServer_ClimateState.OneOf_OptionalIsAutoConditioningOn.isAutoConditioningOn(value)
        }

    var optionalAutoSeatClimateLeft: CarServer_ClimateState.OneOf_OptionalAutoSeatClimateLeft?
        get() = this._optionalAutoSeatClimateLeft
        set(value) {
            this._optionalAutoSeatClimateLeft = value
            _protobufMutated()
        }

    var autoSeatClimateLeft: Boolean
        get() = (this._optionalAutoSeatClimateLeft as? CarServer_ClimateState.OneOf_OptionalAutoSeatClimateLeft.autoSeatClimateLeft)?.value ?: false
        set(value) {
            this.optionalAutoSeatClimateLeft = CarServer_ClimateState.OneOf_OptionalAutoSeatClimateLeft.autoSeatClimateLeft(value)
        }

    var optionalAutoSeatClimateRight: CarServer_ClimateState.OneOf_OptionalAutoSeatClimateRight?
        get() = this._optionalAutoSeatClimateRight
        set(value) {
            this._optionalAutoSeatClimateRight = value
            _protobufMutated()
        }

    var autoSeatClimateRight: Boolean
        get() = (this._optionalAutoSeatClimateRight as? CarServer_ClimateState.OneOf_OptionalAutoSeatClimateRight.autoSeatClimateRight)?.value ?: false
        set(value) {
            this.optionalAutoSeatClimateRight = CarServer_ClimateState.OneOf_OptionalAutoSeatClimateRight.autoSeatClimateRight(value)
        }

    var optionalSeatFanFrontLeft: CarServer_ClimateState.OneOf_OptionalSeatFanFrontLeft?
        get() = this._optionalSeatFanFrontLeft
        set(value) {
            this._optionalSeatFanFrontLeft = value
            _protobufMutated()
        }

    var seatFanFrontLeft: Int
        get() = (this._optionalSeatFanFrontLeft as? CarServer_ClimateState.OneOf_OptionalSeatFanFrontLeft.seatFanFrontLeft)?.value ?: 0
        set(value) {
            this.optionalSeatFanFrontLeft = CarServer_ClimateState.OneOf_OptionalSeatFanFrontLeft.seatFanFrontLeft(value)
        }

    var optionalSeatFanFrontRight: CarServer_ClimateState.OneOf_OptionalSeatFanFrontRight?
        get() = this._optionalSeatFanFrontRight
        set(value) {
            this._optionalSeatFanFrontRight = value
            _protobufMutated()
        }

    var seatFanFrontRight: Int
        get() = (this._optionalSeatFanFrontRight as? CarServer_ClimateState.OneOf_OptionalSeatFanFrontRight.seatFanFrontRight)?.value ?: 0
        set(value) {
            this.optionalSeatFanFrontRight = CarServer_ClimateState.OneOf_OptionalSeatFanFrontRight.seatFanFrontRight(value)
        }

    var optionalAllowCabinOverheatProtection: CarServer_ClimateState.OneOf_OptionalAllowCabinOverheatProtection?
        get() = this._optionalAllowCabinOverheatProtection
        set(value) {
            this._optionalAllowCabinOverheatProtection = value
            _protobufMutated()
        }

    var allowCabinOverheatProtection: Boolean
        get() = (this._optionalAllowCabinOverheatProtection as? CarServer_ClimateState.OneOf_OptionalAllowCabinOverheatProtection.allowCabinOverheatProtection)?.value ?: false
        set(value) {
            this.optionalAllowCabinOverheatProtection = CarServer_ClimateState.OneOf_OptionalAllowCabinOverheatProtection.allowCabinOverheatProtection(value)
        }

    var optionalSupportsFanOnlyCabinOverheatProtection: CarServer_ClimateState.OneOf_OptionalSupportsFanOnlyCabinOverheatProtection?
        get() = this._optionalSupportsFanOnlyCabinOverheatProtection
        set(value) {
            this._optionalSupportsFanOnlyCabinOverheatProtection = value
            _protobufMutated()
        }

    var supportsFanOnlyCabinOverheatProtection: Boolean
        get() = (this._optionalSupportsFanOnlyCabinOverheatProtection as? CarServer_ClimateState.OneOf_OptionalSupportsFanOnlyCabinOverheatProtection.supportsFanOnlyCabinOverheatProtection)?.value ?: false
        set(value) {
            this.optionalSupportsFanOnlyCabinOverheatProtection = CarServer_ClimateState.OneOf_OptionalSupportsFanOnlyCabinOverheatProtection.supportsFanOnlyCabinOverheatProtection(value)
        }

    var optionalCabinOverheatProtection: CarServer_ClimateState.OneOf_OptionalCabinOverheatProtection?
        get() = this._optionalCabinOverheatProtection
        set(value) {
            this._protobufStore_optionalCabinOverheatProtection(value)
            _protobufMutated()
        }

    var cabinOverheatProtection: CarServer_ClimateState.CabinOverheatProtection_E
        get() = (this._optionalCabinOverheatProtection as? CarServer_ClimateState.OneOf_OptionalCabinOverheatProtection.cabinOverheatProtection)?.value ?: CarServer_ClimateState.CabinOverheatProtection_E.cabinOverheatProtectionOff
        set(value) {
            this.optionalCabinOverheatProtection = CarServer_ClimateState.OneOf_OptionalCabinOverheatProtection.cabinOverheatProtection(value)
        }

    var optionalCabinOverheatProtectionActivelyCooling: CarServer_ClimateState.OneOf_OptionalCabinOverheatProtectionActivelyCooling?
        get() = this._optionalCabinOverheatProtectionActivelyCooling
        set(value) {
            this._optionalCabinOverheatProtectionActivelyCooling = value
            _protobufMutated()
        }

    var cabinOverheatProtectionActivelyCooling: Boolean
        get() = (this._optionalCabinOverheatProtectionActivelyCooling as? CarServer_ClimateState.OneOf_OptionalCabinOverheatProtectionActivelyCooling.cabinOverheatProtectionActivelyCooling)?.value ?: false
        set(value) {
            this.optionalCabinOverheatProtectionActivelyCooling = CarServer_ClimateState.OneOf_OptionalCabinOverheatProtectionActivelyCooling.cabinOverheatProtectionActivelyCooling(value)
        }

    var optionalCopActivationTemperature: CarServer_ClimateState.OneOf_OptionalCopActivationTemperature?
        get() = this._optionalCopActivationTemperature
        set(value) {
            this._protobufStore_optionalCopActivationTemperature(value)
            _protobufMutated()
        }

    var copActivationTemperature: CarServer_ClimateState.CopActivationTemp
        get() = (this._optionalCopActivationTemperature as? CarServer_ClimateState.OneOf_OptionalCopActivationTemperature.copActivationTemperature)?.value ?: CarServer_ClimateState.CopActivationTemp.unspecified
        set(value) {
            this.optionalCopActivationTemperature = CarServer_ClimateState.OneOf_OptionalCopActivationTemperature.copActivationTemperature(value)
        }

    var optionalAutoSteeringWheelHeat: CarServer_ClimateState.OneOf_OptionalAutoSteeringWheelHeat?
        get() = this._optionalAutoSteeringWheelHeat
        set(value) {
            this._optionalAutoSteeringWheelHeat = value
            _protobufMutated()
        }

    var autoSteeringWheelHeat: Boolean
        get() = (this._optionalAutoSteeringWheelHeat as? CarServer_ClimateState.OneOf_OptionalAutoSteeringWheelHeat.autoSteeringWheelHeat)?.value ?: false
        set(value) {
            this.optionalAutoSteeringWheelHeat = CarServer_ClimateState.OneOf_OptionalAutoSteeringWheelHeat.autoSteeringWheelHeat(value)
        }

    var optionalSteeringWheelHeatLevel: CarServer_ClimateState.OneOf_OptionalSteeringWheelHeatLevel?
        get() = this._optionalSteeringWheelHeatLevel
        set(value) {
            this._protobufStore_optionalSteeringWheelHeatLevel(value)
            _protobufMutated()
        }

    var steeringWheelHeatLevel: CarServer_StwHeatLevel
        get() = (this._optionalSteeringWheelHeatLevel as? CarServer_ClimateState.OneOf_OptionalSteeringWheelHeatLevel.steeringWheelHeatLevel)?.value ?: CarServer_StwHeatLevel.unknown
        set(value) {
            this.optionalSteeringWheelHeatLevel = CarServer_ClimateState.OneOf_OptionalSteeringWheelHeatLevel.steeringWheelHeatLevel(value)
        }

    var optionalHvacAutoRequest: CarServer_ClimateState.OneOf_OptionalHvacAutoRequest?
        get() = this._optionalHvacAutoRequest
        set(value) {
            this._protobufStore_optionalHvacAutoRequest(value)
            _protobufMutated()
        }

    var hvacAutoRequest: CarServer_ClimateState.HvacAutoRequest
        get() = (this._optionalHvacAutoRequest as? CarServer_ClimateState.OneOf_OptionalHvacAutoRequest.hvacAutoRequest)?.value ?: CarServer_ClimateState.HvacAutoRequest.on
        set(value) {
            this.optionalHvacAutoRequest = CarServer_ClimateState.OneOf_OptionalHvacAutoRequest.hvacAutoRequest(value)
        }

    var optionalCopNotRunningReason: CarServer_ClimateState.OneOf_OptionalCopNotRunningReason?
        get() = this._optionalCopNotRunningReason
        set(value) {
            this._protobufStore_optionalCopNotRunningReason(value)
            _protobufMutated()
        }

    var copNotRunningReason: CarServer_ClimateState.COPNotRunningReason
        get() = (this._optionalCopNotRunningReason as? CarServer_ClimateState.OneOf_OptionalCopNotRunningReason.copNotRunningReason)?.value ?: CarServer_ClimateState.COPNotRunningReason.noReason
        set(value) {
            this.optionalCopNotRunningReason = CarServer_ClimateState.OneOf_OptionalCopNotRunningReason.copNotRunningReason(value)
        }

    sealed class OneOf_OptionalInsideTempCelsius(value: Any) : ProtobufOneofCase(value) {
        class insideTempCelsius(val value: Float) : OneOf_OptionalInsideTempCelsius(value)
    }

    sealed class OneOf_OptionalOutsideTempCelsius(value: Any) : ProtobufOneofCase(value) {
        class outsideTempCelsius(val value: Float) : OneOf_OptionalOutsideTempCelsius(value)
    }

    sealed class OneOf_OptionalDriverTempSetting(value: Any) : ProtobufOneofCase(value) {
        class driverTempSetting(val value: Float) : OneOf_OptionalDriverTempSetting(value)
    }

    sealed class OneOf_OptionalPassengerTempSetting(value: Any) : ProtobufOneofCase(value) {
        class passengerTempSetting(val value: Float) : OneOf_OptionalPassengerTempSetting(value)
    }

    sealed class OneOf_OptionalLeftTempDirection(value: Any) : ProtobufOneofCase(value) {
        class leftTempDirection(val value: Int) : OneOf_OptionalLeftTempDirection(value)
    }

    sealed class OneOf_OptionalRightTempDirection(value: Any) : ProtobufOneofCase(value) {
        class rightTempDirection(val value: Int) : OneOf_OptionalRightTempDirection(value)
    }

    sealed class OneOf_OptionalIsFrontDefrosterOn(value: Any) : ProtobufOneofCase(value) {
        class isFrontDefrosterOn(val value: Boolean) : OneOf_OptionalIsFrontDefrosterOn(value)
    }

    sealed class OneOf_OptionalIsRearDefrosterOn(value: Any) : ProtobufOneofCase(value) {
        class isRearDefrosterOn(val value: Boolean) : OneOf_OptionalIsRearDefrosterOn(value)
    }

    sealed class OneOf_OptionalFanStatus(value: Any) : ProtobufOneofCase(value) {
        class fanStatus(val value: Int) : OneOf_OptionalFanStatus(value)
    }

    sealed class OneOf_OptionalIsClimateOn(value: Any) : ProtobufOneofCase(value) {
        class isClimateOn(val value: Boolean) : OneOf_OptionalIsClimateOn(value)
    }

    sealed class OneOf_OptionalMinAvailTempCelsius(value: Any) : ProtobufOneofCase(value) {
        class minAvailTempCelsius(val value: Float) : OneOf_OptionalMinAvailTempCelsius(value)
    }

    sealed class OneOf_OptionalMaxAvailTempCelsius(value: Any) : ProtobufOneofCase(value) {
        class maxAvailTempCelsius(val value: Float) : OneOf_OptionalMaxAvailTempCelsius(value)
    }

    sealed class OneOf_OptionalSeatHeaterLeft(value: Any) : ProtobufOneofCase(value) {
        class seatHeaterLeft(val value: Int) : OneOf_OptionalSeatHeaterLeft(value)
    }

    sealed class OneOf_OptionalSeatHeaterRight(value: Any) : ProtobufOneofCase(value) {
        class seatHeaterRight(val value: Int) : OneOf_OptionalSeatHeaterRight(value)
    }

    sealed class OneOf_OptionalSeatHeaterRearLeft(value: Any) : ProtobufOneofCase(value) {
        class seatHeaterRearLeft(val value: Int) : OneOf_OptionalSeatHeaterRearLeft(value)
    }

    sealed class OneOf_OptionalSeatHeaterRearRight(value: Any) : ProtobufOneofCase(value) {
        class seatHeaterRearRight(val value: Int) : OneOf_OptionalSeatHeaterRearRight(value)
    }

    sealed class OneOf_OptionalSeatHeaterRearCenter(value: Any) : ProtobufOneofCase(value) {
        class seatHeaterRearCenter(val value: Int) : OneOf_OptionalSeatHeaterRearCenter(value)
    }

    sealed class OneOf_OptionalSeatHeaterRearRightBack(value: Any) : ProtobufOneofCase(value) {
        class seatHeaterRearRightBack(val value: Int) : OneOf_OptionalSeatHeaterRearRightBack(value)
    }

    sealed class OneOf_OptionalSeatHeaterRearLeftBack(value: Any) : ProtobufOneofCase(value) {
        class seatHeaterRearLeftBack(val value: Int) : OneOf_OptionalSeatHeaterRearLeftBack(value)
    }

    sealed class OneOf_OptionalSeatHeaterThirdRowRight(value: Any) : ProtobufOneofCase(value) {
        class seatHeaterThirdRowRight(val value: Int) : OneOf_OptionalSeatHeaterThirdRowRight(value)
    }

    sealed class OneOf_OptionalSeatHeaterThirdRowLeft(value: Any) : ProtobufOneofCase(value) {
        class seatHeaterThirdRowLeft(val value: Int) : OneOf_OptionalSeatHeaterThirdRowLeft(value)
    }

    sealed class OneOf_OptionalBatteryHeater(value: Any) : ProtobufOneofCase(value) {
        class batteryHeater(val value: Boolean) : OneOf_OptionalBatteryHeater(value)
    }

    sealed class OneOf_OptionalBatteryHeaterNoPower(value: Any) : ProtobufOneofCase(value) {
        class batteryHeaterNoPower(val value: Boolean) : OneOf_OptionalBatteryHeaterNoPower(value)
    }

    sealed class OneOf_OptionalSteeringWheelHeater(value: Any) : ProtobufOneofCase(value) {
        class steeringWheelHeater(val value: Boolean) : OneOf_OptionalSteeringWheelHeater(value)
    }

    sealed class OneOf_OptionalWiperBladeHeater(value: Any) : ProtobufOneofCase(value) {
        class wiperBladeHeater(val value: Boolean) : OneOf_OptionalWiperBladeHeater(value)
    }

    sealed class OneOf_OptionalSideMirrorHeaters(value: Any) : ProtobufOneofCase(value) {
        class sideMirrorHeaters(val value: Boolean) : OneOf_OptionalSideMirrorHeaters(value)
    }

    sealed class OneOf_OptionalIsPreconditioning(value: Any) : ProtobufOneofCase(value) {
        class isPreconditioning(val value: Boolean) : OneOf_OptionalIsPreconditioning(value)
    }

    sealed class OneOf_OptionalRemoteHeaterControlEnabled(value: Any) : ProtobufOneofCase(value) {
        class remoteHeaterControlEnabled(val value: Boolean) : OneOf_OptionalRemoteHeaterControlEnabled(value)
    }

    sealed class OneOf_OptionalBioweaponModeOn(value: Any) : ProtobufOneofCase(value) {
        class bioweaponModeOn(val value: Boolean) : OneOf_OptionalBioweaponModeOn(value)
    }

    sealed class OneOf_OptionalIsAutoConditioningOn(value: Any) : ProtobufOneofCase(value) {
        class isAutoConditioningOn(val value: Boolean) : OneOf_OptionalIsAutoConditioningOn(value)
    }

    sealed class OneOf_OptionalAutoSeatClimateLeft(value: Any) : ProtobufOneofCase(value) {
        class autoSeatClimateLeft(val value: Boolean) : OneOf_OptionalAutoSeatClimateLeft(value)
    }

    sealed class OneOf_OptionalAutoSeatClimateRight(value: Any) : ProtobufOneofCase(value) {
        class autoSeatClimateRight(val value: Boolean) : OneOf_OptionalAutoSeatClimateRight(value)
    }

    sealed class OneOf_OptionalSeatFanFrontLeft(value: Any) : ProtobufOneofCase(value) {
        class seatFanFrontLeft(val value: Int) : OneOf_OptionalSeatFanFrontLeft(value)
    }

    sealed class OneOf_OptionalSeatFanFrontRight(value: Any) : ProtobufOneofCase(value) {
        class seatFanFrontRight(val value: Int) : OneOf_OptionalSeatFanFrontRight(value)
    }

    sealed class OneOf_OptionalAllowCabinOverheatProtection(value: Any) : ProtobufOneofCase(value) {
        class allowCabinOverheatProtection(val value: Boolean) : OneOf_OptionalAllowCabinOverheatProtection(value)
    }

    sealed class OneOf_OptionalSupportsFanOnlyCabinOverheatProtection(value: Any) : ProtobufOneofCase(value) {
        class supportsFanOnlyCabinOverheatProtection(val value: Boolean) : OneOf_OptionalSupportsFanOnlyCabinOverheatProtection(value)
    }

    sealed class OneOf_OptionalCabinOverheatProtection(value: Any) : ProtobufOneofCase(value) {
        class cabinOverheatProtection(val value: CarServer_ClimateState.CabinOverheatProtection_E) : OneOf_OptionalCabinOverheatProtection(value)
    }

    sealed class OneOf_OptionalCabinOverheatProtectionActivelyCooling(value: Any) : ProtobufOneofCase(value) {
        class cabinOverheatProtectionActivelyCooling(val value: Boolean) : OneOf_OptionalCabinOverheatProtectionActivelyCooling(value)
    }

    sealed class OneOf_OptionalCopActivationTemperature(value: Any) : ProtobufOneofCase(value) {
        class copActivationTemperature(val value: CarServer_ClimateState.CopActivationTemp) : OneOf_OptionalCopActivationTemperature(value)
    }

    sealed class OneOf_OptionalAutoSteeringWheelHeat(value: Any) : ProtobufOneofCase(value) {
        class autoSteeringWheelHeat(val value: Boolean) : OneOf_OptionalAutoSteeringWheelHeat(value)
    }

    sealed class OneOf_OptionalSteeringWheelHeatLevel(value: Any) : ProtobufOneofCase(value) {
        class steeringWheelHeatLevel(val value: CarServer_StwHeatLevel) : OneOf_OptionalSteeringWheelHeatLevel(value)
    }

    sealed class OneOf_OptionalHvacAutoRequest(value: Any) : ProtobufOneofCase(value) {
        class hvacAutoRequest(val value: CarServer_ClimateState.HvacAutoRequest) : OneOf_OptionalHvacAutoRequest(value)
    }

    sealed class OneOf_OptionalCopNotRunningReason(value: Any) : ProtobufOneofCase(value) {
        class copNotRunningReason(val value: CarServer_ClimateState.COPNotRunningReason) : OneOf_OptionalCopNotRunningReason(value)
    }

    enum class HvacAutoRequest(val rawValue: Int) {
        on(0),
        `override`(1),
        UNRECOGNIZED(-1),
        ;

        companion object {
            val allCases: List<CarServer_ClimateState.HvacAutoRequest> = listOf(
                on,
                `override`,
            )

            fun fromRawValue(rawValue: Int): CarServer_ClimateState.HvacAutoRequest =
                when (rawValue) {
                    0 -> on
                    1 -> `override`
                    else -> UNRECOGNIZED
                }
        }
    }

    enum class CabinOverheatProtection_E(val rawValue: Int) {
        cabinOverheatProtectionOff(0),
        cabinOverheatProtectionOn(1),
        cabinOverheatProtectionFanOnly(2),
        UNRECOGNIZED(-1),
        ;

        companion object {
            val allCases: List<CarServer_ClimateState.CabinOverheatProtection_E> = listOf(
                cabinOverheatProtectionOff,
                cabinOverheatProtectionOn,
                cabinOverheatProtectionFanOnly,
            )

            fun fromRawValue(rawValue: Int): CarServer_ClimateState.CabinOverheatProtection_E =
                when (rawValue) {
                    0 -> cabinOverheatProtectionOff
                    1 -> cabinOverheatProtectionOn
                    2 -> cabinOverheatProtectionFanOnly
                    else -> UNRECOGNIZED
                }
        }
    }

    enum class SeatHeaterLevel_E(val rawValue: Int) {
        seatHeaterLevelOff(0),
        seatHeaterLevelLow(1),
        seatHeaterLevelMed(2),
        seatHeaterLevelHigh(3),
        UNRECOGNIZED(-1),
        ;

        companion object {
            val allCases: List<CarServer_ClimateState.SeatHeaterLevel_E> = listOf(
                seatHeaterLevelOff,
                seatHeaterLevelLow,
                seatHeaterLevelMed,
                seatHeaterLevelHigh,
            )

            fun fromRawValue(rawValue: Int): CarServer_ClimateState.SeatHeaterLevel_E =
                when (rawValue) {
                    0 -> seatHeaterLevelOff
                    1 -> seatHeaterLevelLow
                    2 -> seatHeaterLevelMed
                    3 -> seatHeaterLevelHigh
                    else -> UNRECOGNIZED
                }
        }
    }

    enum class SeatCoolingLevel_E(val rawValue: Int) {
        seatCoolingLevelOff(0),
        seatCoolingLevelLow(1),
        seatCoolingLevelMed(2),
        seatCoolingLevelHigh(3),
        UNRECOGNIZED(-1),
        ;

        companion object {
            val allCases: List<CarServer_ClimateState.SeatCoolingLevel_E> = listOf(
                seatCoolingLevelOff,
                seatCoolingLevelLow,
                seatCoolingLevelMed,
                seatCoolingLevelHigh,
            )

            fun fromRawValue(rawValue: Int): CarServer_ClimateState.SeatCoolingLevel_E =
                when (rawValue) {
                    0 -> seatCoolingLevelOff
                    1 -> seatCoolingLevelLow
                    2 -> seatCoolingLevelMed
                    3 -> seatCoolingLevelHigh
                    else -> UNRECOGNIZED
                }
        }
    }

    enum class CopActivationTemp(val rawValue: Int) {
        unspecified(0),
        low(1),
        medium(2),
        high(3),
        UNRECOGNIZED(-1),
        ;

        companion object {
            val allCases: List<CarServer_ClimateState.CopActivationTemp> = listOf(
                unspecified,
                low,
                medium,
                high,
            )

            fun fromRawValue(rawValue: Int): CarServer_ClimateState.CopActivationTemp =
                when (rawValue) {
                    0 -> unspecified
                    1 -> low
                    2 -> medium
                    3 -> high
                    else -> UNRECOGNIZED
                }
        }
    }

    enum class COPNotRunningReason(val rawValue: Int) {
        noReason(0),
        userInteraction(1),
        energyConsumptionReached(2),
        timeout(3),
        lowSolarLoad(4),
        fault(5),
        cabinBelowThreshold(6),
        UNRECOGNIZED(-1),
        ;

        companion object {
            val allCases: List<CarServer_ClimateState.COPNotRunningReason> = listOf(
                noReason,
                userInteraction,
                energyConsumptionReached,
                timeout,
                lowSolarLoad,
                fault,
                cabinBelowThreshold,
            )

            fun fromRawValue(rawValue: Int): CarServer_ClimateState.COPNotRunningReason =
                when (rawValue) {
                    0 -> noReason
                    1 -> userInteraction
                    2 -> energyConsumptionReached
                    3 -> timeout
                    4 -> lowSolarLoad
                    5 -> fault
                    6 -> cabinBelowThreshold
                    else -> UNRECOGNIZED
                }
        }
    }

    class ClimateKeeperMode() : GeneratedMessage() {
        constructor(serializedBytes: ByteArray) : this() {
            merge(serializedBytes)
        }

        var type: CarServer_ClimateState.ClimateKeeperMode.OneOf_Type?
            get() = this._type
            set(value) {
                this._protobufStore_type(this._protobufCopy_type(value))
                _protobufMutated()
            }

        var unknown: CarServer_Void
            get() = (this._type as? CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.unknown)?.value
                ?: _protobufPending(1, { CarServer_Void() }) { this._protobufStore_type(CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.unknown(it)) }
            set(value) {
                this.type = CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.unknown(value)
            }

        var off: CarServer_Void
            get() = (this._type as? CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.off)?.value
                ?: _protobufPending(2, { CarServer_Void() }) { this._protobufStore_type(CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.off(it)) }
            set(value) {
                this.type = CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.off(value)
            }

        var on: CarServer_Void
            get() = (this._type as? CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.on)?.value
                ?: _protobufPending(3, { CarServer_Void() }) { this._protobufStore_type(CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.on(it)) }
            set(value) {
                this.type = CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.on(value)
            }

        var dog: CarServer_Void
            get() = (this._type as? CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.dog)?.value
                ?: _protobufPending(4, { CarServer_Void() }) { this._protobufStore_type(CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.dog(it)) }
            set(value) {
                this.type = CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.dog(value)
            }

        var party: CarServer_Void
            get() = (this._type as? CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.party)?.value
                ?: _protobufPending(5, { CarServer_Void() }) { this._protobufStore_type(CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.party(it)) }
            set(value) {
                this.type = CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.party(value)
            }

        sealed class OneOf_Type(value: Any) : ProtobufOneofCase(value) {
            class unknown(val value: CarServer_Void) : OneOf_Type(value)
            class off(val value: CarServer_Void) : OneOf_Type(value)
            class on(val value: CarServer_Void) : OneOf_Type(value)
            class dog(val value: CarServer_Void) : OneOf_Type(value)
            class party(val value: CarServer_Void) : OneOf_Type(value)
        }

        private var _type: CarServer_ClimateState.ClimateKeeperMode.OneOf_Type? = null

        private fun _protobufStore_type(value: CarServer_ClimateState.ClimateKeeperMode.OneOf_Type?) {
            _protobufDropPending(1)
            _protobufDropPending(2)
            _protobufDropPending(3)
            _protobufDropPending(4)
            _protobufDropPending(5)
            this._type = value
        }

        private fun _protobufCopy_type(value: CarServer_ClimateState.ClimateKeeperMode.OneOf_Type?): CarServer_ClimateState.ClimateKeeperMode.OneOf_Type? =
            when (value) {
                is CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.unknown -> CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.unknown(value.value.copy())
                is CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.off -> CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.off(value.value.copy())
                is CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.on -> CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.on(value.value.copy())
                is CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.dog -> CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.dog(value.value.copy())
                is CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.party -> CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.party(value.value.copy())
                else -> value
            }

        override fun decodeMessage(decoder: BinaryDecoder) {
            while (true) {
                when (decoder.nextFieldNumber() ?: return) {
                    1 -> decoder.decodeSingularMessageField((this._type as? CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.unknown)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.unknown(it)) }
                    2 -> decoder.decodeSingularMessageField((this._type as? CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.off)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.off(it)) }
                    3 -> decoder.decodeSingularMessageField((this._type as? CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.on)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.on(it)) }
                    4 -> decoder.decodeSingularMessageField((this._type as? CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.dog)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.dog(it)) }
                    5 -> decoder.decodeSingularMessageField((this._type as? CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.party)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.party(it)) }
                }
            }
        }

        override fun traverse(visitor: BinaryEncodingVisitor) {
            (this._type as? CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.unknown)?.let {
                visitor.visitSingularMessageField(it.value, 1)
            }
            (this._type as? CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.off)?.let {
                visitor.visitSingularMessageField(it.value, 2)
            }
            (this._type as? CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.on)?.let {
                visitor.visitSingularMessageField(it.value, 3)
            }
            (this._type as? CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.dog)?.let {
                visitor.visitSingularMessageField(it.value, 4)
            }
            (this._type as? CarServer_ClimateState.ClimateKeeperMode.OneOf_Type.party)?.let {
                visitor.visitSingularMessageField(it.value, 5)
            }
            visitor.visitUnknown(unknownFields)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CarServer_ClimateState.ClimateKeeperMode) return false
            if (this._type != other._type) return false
            return this.unknownFields.contentEquals(other.unknownFields)
        }

        override fun hashCode(): Int {
            var hash = 0
            hash = 31 * hash + (this._type?.hashCode() ?: 0)
            hash = 31 * hash + this.unknownFields.contentHashCode()
            return hash
        }

        override fun copy(): CarServer_ClimateState.ClimateKeeperMode {
            val result = CarServer_ClimateState.ClimateKeeperMode()
            result._type = this._protobufCopy_type(this._type)
            result.unknownFields = this.unknownFields
            return result
        }

        companion object {
            const val protoMessageName: String = "CarServer.ClimateState.ClimateKeeperMode"

            fun with(block: CarServer_ClimateState.ClimateKeeperMode.() -> Unit): CarServer_ClimateState.ClimateKeeperMode =
                CarServer_ClimateState.ClimateKeeperMode().apply(block)
        }
    }

    class DefrostMode() : GeneratedMessage() {
        constructor(serializedBytes: ByteArray) : this() {
            merge(serializedBytes)
        }

        var type: CarServer_ClimateState.DefrostMode.OneOf_Type?
            get() = this._type
            set(value) {
                this._protobufStore_type(this._protobufCopy_type(value))
                _protobufMutated()
            }

        var off: CarServer_Void
            get() = (this._type as? CarServer_ClimateState.DefrostMode.OneOf_Type.off)?.value
                ?: _protobufPending(1, { CarServer_Void() }) { this._protobufStore_type(CarServer_ClimateState.DefrostMode.OneOf_Type.off(it)) }
            set(value) {
                this.type = CarServer_ClimateState.DefrostMode.OneOf_Type.off(value)
            }

        var normal: CarServer_Void
            get() = (this._type as? CarServer_ClimateState.DefrostMode.OneOf_Type.normal)?.value
                ?: _protobufPending(2, { CarServer_Void() }) { this._protobufStore_type(CarServer_ClimateState.DefrostMode.OneOf_Type.normal(it)) }
            set(value) {
                this.type = CarServer_ClimateState.DefrostMode.OneOf_Type.normal(value)
            }

        var max: CarServer_Void
            get() = (this._type as? CarServer_ClimateState.DefrostMode.OneOf_Type.max)?.value
                ?: _protobufPending(3, { CarServer_Void() }) { this._protobufStore_type(CarServer_ClimateState.DefrostMode.OneOf_Type.max(it)) }
            set(value) {
                this.type = CarServer_ClimateState.DefrostMode.OneOf_Type.max(value)
            }

        sealed class OneOf_Type(value: Any) : ProtobufOneofCase(value) {
            class off(val value: CarServer_Void) : OneOf_Type(value)
            class normal(val value: CarServer_Void) : OneOf_Type(value)
            class max(val value: CarServer_Void) : OneOf_Type(value)
        }

        private var _type: CarServer_ClimateState.DefrostMode.OneOf_Type? = null

        private fun _protobufStore_type(value: CarServer_ClimateState.DefrostMode.OneOf_Type?) {
            _protobufDropPending(1)
            _protobufDropPending(2)
            _protobufDropPending(3)
            this._type = value
        }

        private fun _protobufCopy_type(value: CarServer_ClimateState.DefrostMode.OneOf_Type?): CarServer_ClimateState.DefrostMode.OneOf_Type? =
            when (value) {
                is CarServer_ClimateState.DefrostMode.OneOf_Type.off -> CarServer_ClimateState.DefrostMode.OneOf_Type.off(value.value.copy())
                is CarServer_ClimateState.DefrostMode.OneOf_Type.normal -> CarServer_ClimateState.DefrostMode.OneOf_Type.normal(value.value.copy())
                is CarServer_ClimateState.DefrostMode.OneOf_Type.max -> CarServer_ClimateState.DefrostMode.OneOf_Type.max(value.value.copy())
                else -> value
            }

        override fun decodeMessage(decoder: BinaryDecoder) {
            while (true) {
                when (decoder.nextFieldNumber() ?: return) {
                    1 -> decoder.decodeSingularMessageField((this._type as? CarServer_ClimateState.DefrostMode.OneOf_Type.off)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ClimateState.DefrostMode.OneOf_Type.off(it)) }
                    2 -> decoder.decodeSingularMessageField((this._type as? CarServer_ClimateState.DefrostMode.OneOf_Type.normal)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ClimateState.DefrostMode.OneOf_Type.normal(it)) }
                    3 -> decoder.decodeSingularMessageField((this._type as? CarServer_ClimateState.DefrostMode.OneOf_Type.max)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_ClimateState.DefrostMode.OneOf_Type.max(it)) }
                }
            }
        }

        override fun traverse(visitor: BinaryEncodingVisitor) {
            (this._type as? CarServer_ClimateState.DefrostMode.OneOf_Type.off)?.let {
                visitor.visitSingularMessageField(it.value, 1)
            }
            (this._type as? CarServer_ClimateState.DefrostMode.OneOf_Type.normal)?.let {
                visitor.visitSingularMessageField(it.value, 2)
            }
            (this._type as? CarServer_ClimateState.DefrostMode.OneOf_Type.max)?.let {
                visitor.visitSingularMessageField(it.value, 3)
            }
            visitor.visitUnknown(unknownFields)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CarServer_ClimateState.DefrostMode) return false
            if (this._type != other._type) return false
            return this.unknownFields.contentEquals(other.unknownFields)
        }

        override fun hashCode(): Int {
            var hash = 0
            hash = 31 * hash + (this._type?.hashCode() ?: 0)
            hash = 31 * hash + this.unknownFields.contentHashCode()
            return hash
        }

        override fun copy(): CarServer_ClimateState.DefrostMode {
            val result = CarServer_ClimateState.DefrostMode()
            result._type = this._protobufCopy_type(this._type)
            result.unknownFields = this.unknownFields
            return result
        }

        companion object {
            const val protoMessageName: String = "CarServer.ClimateState.DefrostMode"

            fun with(block: CarServer_ClimateState.DefrostMode.() -> Unit): CarServer_ClimateState.DefrostMode =
                CarServer_ClimateState.DefrostMode().apply(block)
        }
    }

    private var _optionalInsideTempCelsius: CarServer_ClimateState.OneOf_OptionalInsideTempCelsius? = null
    private var _optionalOutsideTempCelsius: CarServer_ClimateState.OneOf_OptionalOutsideTempCelsius? = null
    private var _optionalDriverTempSetting: CarServer_ClimateState.OneOf_OptionalDriverTempSetting? = null
    private var _optionalPassengerTempSetting: CarServer_ClimateState.OneOf_OptionalPassengerTempSetting? = null
    private var _optionalLeftTempDirection: CarServer_ClimateState.OneOf_OptionalLeftTempDirection? = null
    private var _optionalRightTempDirection: CarServer_ClimateState.OneOf_OptionalRightTempDirection? = null
    private var _optionalIsFrontDefrosterOn: CarServer_ClimateState.OneOf_OptionalIsFrontDefrosterOn? = null
    private var _optionalIsRearDefrosterOn: CarServer_ClimateState.OneOf_OptionalIsRearDefrosterOn? = null
    private var _optionalFanStatus: CarServer_ClimateState.OneOf_OptionalFanStatus? = null
    private var _optionalIsClimateOn: CarServer_ClimateState.OneOf_OptionalIsClimateOn? = null
    private var _optionalMinAvailTempCelsius: CarServer_ClimateState.OneOf_OptionalMinAvailTempCelsius? = null
    private var _optionalMaxAvailTempCelsius: CarServer_ClimateState.OneOf_OptionalMaxAvailTempCelsius? = null
    private var _optionalSeatHeaterLeft: CarServer_ClimateState.OneOf_OptionalSeatHeaterLeft? = null
    private var _optionalSeatHeaterRight: CarServer_ClimateState.OneOf_OptionalSeatHeaterRight? = null
    private var _optionalSeatHeaterRearLeft: CarServer_ClimateState.OneOf_OptionalSeatHeaterRearLeft? = null
    private var _optionalSeatHeaterRearRight: CarServer_ClimateState.OneOf_OptionalSeatHeaterRearRight? = null
    private var _optionalSeatHeaterRearCenter: CarServer_ClimateState.OneOf_OptionalSeatHeaterRearCenter? = null
    private var _optionalSeatHeaterRearRightBack: CarServer_ClimateState.OneOf_OptionalSeatHeaterRearRightBack? = null
    private var _optionalSeatHeaterRearLeftBack: CarServer_ClimateState.OneOf_OptionalSeatHeaterRearLeftBack? = null
    private var _optionalSeatHeaterThirdRowRight: CarServer_ClimateState.OneOf_OptionalSeatHeaterThirdRowRight? = null
    private var _optionalSeatHeaterThirdRowLeft: CarServer_ClimateState.OneOf_OptionalSeatHeaterThirdRowLeft? = null
    private var _optionalBatteryHeater: CarServer_ClimateState.OneOf_OptionalBatteryHeater? = null
    private var _optionalBatteryHeaterNoPower: CarServer_ClimateState.OneOf_OptionalBatteryHeaterNoPower? = null
    private var _optionalSteeringWheelHeater: CarServer_ClimateState.OneOf_OptionalSteeringWheelHeater? = null
    private var _optionalWiperBladeHeater: CarServer_ClimateState.OneOf_OptionalWiperBladeHeater? = null
    private var _optionalSideMirrorHeaters: CarServer_ClimateState.OneOf_OptionalSideMirrorHeaters? = null
    private var _optionalIsPreconditioning: CarServer_ClimateState.OneOf_OptionalIsPreconditioning? = null
    private var _optionalRemoteHeaterControlEnabled: CarServer_ClimateState.OneOf_OptionalRemoteHeaterControlEnabled? = null
    private var _climateKeeperMode: CarServer_ClimateState.ClimateKeeperMode? = null
    private var _timestamp: Google_Protobuf_Timestamp? = null
    private var _optionalBioweaponModeOn: CarServer_ClimateState.OneOf_OptionalBioweaponModeOn? = null
    private var _defrostMode: CarServer_ClimateState.DefrostMode? = null
    private var _optionalIsAutoConditioningOn: CarServer_ClimateState.OneOf_OptionalIsAutoConditioningOn? = null
    private var _optionalAutoSeatClimateLeft: CarServer_ClimateState.OneOf_OptionalAutoSeatClimateLeft? = null
    private var _optionalAutoSeatClimateRight: CarServer_ClimateState.OneOf_OptionalAutoSeatClimateRight? = null
    private var _optionalSeatFanFrontLeft: CarServer_ClimateState.OneOf_OptionalSeatFanFrontLeft? = null
    private var _optionalSeatFanFrontRight: CarServer_ClimateState.OneOf_OptionalSeatFanFrontRight? = null
    private var _optionalAllowCabinOverheatProtection: CarServer_ClimateState.OneOf_OptionalAllowCabinOverheatProtection? = null
    private var _optionalSupportsFanOnlyCabinOverheatProtection: CarServer_ClimateState.OneOf_OptionalSupportsFanOnlyCabinOverheatProtection? = null
    private var _optionalCabinOverheatProtection: CarServer_ClimateState.OneOf_OptionalCabinOverheatProtection? = null
    private var _optionalCabinOverheatProtectionActivelyCooling: CarServer_ClimateState.OneOf_OptionalCabinOverheatProtectionActivelyCooling? = null
    private var _optionalCopActivationTemperature: CarServer_ClimateState.OneOf_OptionalCopActivationTemperature? = null
    private var _optionalAutoSteeringWheelHeat: CarServer_ClimateState.OneOf_OptionalAutoSteeringWheelHeat? = null
    private var _optionalSteeringWheelHeatLevel: CarServer_ClimateState.OneOf_OptionalSteeringWheelHeatLevel? = null
    private var _optionalHvacAutoRequest: CarServer_ClimateState.OneOf_OptionalHvacAutoRequest? = null
    private var _optionalCopNotRunningReason: CarServer_ClimateState.OneOf_OptionalCopNotRunningReason? = null

    private fun _protobufStore_optionalCabinOverheatProtection(value: CarServer_ClimateState.OneOf_OptionalCabinOverheatProtection?) {
        _protobufForgetUnrecognized(143)
        this._optionalCabinOverheatProtection = value
    }

    private fun _protobufStore_optionalCopActivationTemperature(value: CarServer_ClimateState.OneOf_OptionalCopActivationTemperature?) {
        _protobufForgetUnrecognized(146)
        this._optionalCopActivationTemperature = value
    }

    private fun _protobufStore_optionalSteeringWheelHeatLevel(value: CarServer_ClimateState.OneOf_OptionalSteeringWheelHeatLevel?) {
        _protobufForgetUnrecognized(148)
        this._optionalSteeringWheelHeatLevel = value
    }

    private fun _protobufStore_optionalHvacAutoRequest(value: CarServer_ClimateState.OneOf_OptionalHvacAutoRequest?) {
        _protobufForgetUnrecognized(150)
        this._optionalHvacAutoRequest = value
    }

    private fun _protobufStore_optionalCopNotRunningReason(value: CarServer_ClimateState.OneOf_OptionalCopNotRunningReason?) {
        _protobufForgetUnrecognized(151)
        this._optionalCopNotRunningReason = value
    }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                30 -> decoder.decodeSingularMessageField(this._climateKeeperMode) { CarServer_ClimateState.ClimateKeeperMode() }?.let {
                    _protobufDropPending(30)
                    this._climateKeeperMode = it
                }
                33 -> decoder.decodeSingularMessageField(this._timestamp) { Google_Protobuf_Timestamp() }?.let {
                    _protobufDropPending(33)
                    this._timestamp = it
                }
                35 -> decoder.decodeSingularMessageField(this._defrostMode) { CarServer_ClimateState.DefrostMode() }?.let {
                    _protobufDropPending(35)
                    this._defrostMode = it
                }
                101 -> decoder.decodeSingularFloatField()?.let { this._optionalInsideTempCelsius = CarServer_ClimateState.OneOf_OptionalInsideTempCelsius.insideTempCelsius(it) }
                102 -> decoder.decodeSingularFloatField()?.let { this._optionalOutsideTempCelsius = CarServer_ClimateState.OneOf_OptionalOutsideTempCelsius.outsideTempCelsius(it) }
                103 -> decoder.decodeSingularFloatField()?.let { this._optionalDriverTempSetting = CarServer_ClimateState.OneOf_OptionalDriverTempSetting.driverTempSetting(it) }
                104 -> decoder.decodeSingularFloatField()?.let { this._optionalPassengerTempSetting = CarServer_ClimateState.OneOf_OptionalPassengerTempSetting.passengerTempSetting(it) }
                105 -> decoder.decodeSingularInt32Field()?.let { this._optionalLeftTempDirection = CarServer_ClimateState.OneOf_OptionalLeftTempDirection.leftTempDirection(it) }
                106 -> decoder.decodeSingularInt32Field()?.let { this._optionalRightTempDirection = CarServer_ClimateState.OneOf_OptionalRightTempDirection.rightTempDirection(it) }
                107 -> decoder.decodeSingularBoolField()?.let { this._optionalIsFrontDefrosterOn = CarServer_ClimateState.OneOf_OptionalIsFrontDefrosterOn.isFrontDefrosterOn(it) }
                108 -> decoder.decodeSingularBoolField()?.let { this._optionalIsRearDefrosterOn = CarServer_ClimateState.OneOf_OptionalIsRearDefrosterOn.isRearDefrosterOn(it) }
                109 -> decoder.decodeSingularInt32Field()?.let { this._optionalFanStatus = CarServer_ClimateState.OneOf_OptionalFanStatus.fanStatus(it) }
                110 -> decoder.decodeSingularBoolField()?.let { this._optionalIsClimateOn = CarServer_ClimateState.OneOf_OptionalIsClimateOn.isClimateOn(it) }
                111 -> decoder.decodeSingularFloatField()?.let { this._optionalMinAvailTempCelsius = CarServer_ClimateState.OneOf_OptionalMinAvailTempCelsius.minAvailTempCelsius(it) }
                112 -> decoder.decodeSingularFloatField()?.let { this._optionalMaxAvailTempCelsius = CarServer_ClimateState.OneOf_OptionalMaxAvailTempCelsius.maxAvailTempCelsius(it) }
                113 -> decoder.decodeSingularInt32Field()?.let { this._optionalSeatHeaterLeft = CarServer_ClimateState.OneOf_OptionalSeatHeaterLeft.seatHeaterLeft(it) }
                114 -> decoder.decodeSingularInt32Field()?.let { this._optionalSeatHeaterRight = CarServer_ClimateState.OneOf_OptionalSeatHeaterRight.seatHeaterRight(it) }
                115 -> decoder.decodeSingularInt32Field()?.let { this._optionalSeatHeaterRearLeft = CarServer_ClimateState.OneOf_OptionalSeatHeaterRearLeft.seatHeaterRearLeft(it) }
                116 -> decoder.decodeSingularInt32Field()?.let { this._optionalSeatHeaterRearRight = CarServer_ClimateState.OneOf_OptionalSeatHeaterRearRight.seatHeaterRearRight(it) }
                117 -> decoder.decodeSingularInt32Field()?.let { this._optionalSeatHeaterRearCenter = CarServer_ClimateState.OneOf_OptionalSeatHeaterRearCenter.seatHeaterRearCenter(it) }
                118 -> decoder.decodeSingularInt32Field()?.let { this._optionalSeatHeaterRearRightBack = CarServer_ClimateState.OneOf_OptionalSeatHeaterRearRightBack.seatHeaterRearRightBack(it) }
                119 -> decoder.decodeSingularInt32Field()?.let { this._optionalSeatHeaterRearLeftBack = CarServer_ClimateState.OneOf_OptionalSeatHeaterRearLeftBack.seatHeaterRearLeftBack(it) }
                120 -> decoder.decodeSingularInt32Field()?.let { this._optionalSeatHeaterThirdRowRight = CarServer_ClimateState.OneOf_OptionalSeatHeaterThirdRowRight.seatHeaterThirdRowRight(it) }
                121 -> decoder.decodeSingularInt32Field()?.let { this._optionalSeatHeaterThirdRowLeft = CarServer_ClimateState.OneOf_OptionalSeatHeaterThirdRowLeft.seatHeaterThirdRowLeft(it) }
                122 -> decoder.decodeSingularBoolField()?.let { this._optionalBatteryHeater = CarServer_ClimateState.OneOf_OptionalBatteryHeater.batteryHeater(it) }
                123 -> decoder.decodeSingularBoolField()?.let { this._optionalBatteryHeaterNoPower = CarServer_ClimateState.OneOf_OptionalBatteryHeaterNoPower.batteryHeaterNoPower(it) }
                125 -> decoder.decodeSingularBoolField()?.let { this._optionalSteeringWheelHeater = CarServer_ClimateState.OneOf_OptionalSteeringWheelHeater.steeringWheelHeater(it) }
                126 -> decoder.decodeSingularBoolField()?.let { this._optionalWiperBladeHeater = CarServer_ClimateState.OneOf_OptionalWiperBladeHeater.wiperBladeHeater(it) }
                127 -> decoder.decodeSingularBoolField()?.let { this._optionalSideMirrorHeaters = CarServer_ClimateState.OneOf_OptionalSideMirrorHeaters.sideMirrorHeaters(it) }
                128 -> decoder.decodeSingularBoolField()?.let { this._optionalIsPreconditioning = CarServer_ClimateState.OneOf_OptionalIsPreconditioning.isPreconditioning(it) }
                129 -> decoder.decodeSingularBoolField()?.let { this._optionalRemoteHeaterControlEnabled = CarServer_ClimateState.OneOf_OptionalRemoteHeaterControlEnabled.remoteHeaterControlEnabled(it) }
                134 -> decoder.decodeSingularBoolField()?.let { this._optionalBioweaponModeOn = CarServer_ClimateState.OneOf_OptionalBioweaponModeOn.bioweaponModeOn(it) }
                136 -> decoder.decodeSingularBoolField()?.let { this._optionalIsAutoConditioningOn = CarServer_ClimateState.OneOf_OptionalIsAutoConditioningOn.isAutoConditioningOn(it) }
                137 -> decoder.decodeSingularBoolField()?.let { this._optionalAutoSeatClimateLeft = CarServer_ClimateState.OneOf_OptionalAutoSeatClimateLeft.autoSeatClimateLeft(it) }
                138 -> decoder.decodeSingularBoolField()?.let { this._optionalAutoSeatClimateRight = CarServer_ClimateState.OneOf_OptionalAutoSeatClimateRight.autoSeatClimateRight(it) }
                139 -> decoder.decodeSingularInt32Field()?.let { this._optionalSeatFanFrontLeft = CarServer_ClimateState.OneOf_OptionalSeatFanFrontLeft.seatFanFrontLeft(it) }
                140 -> decoder.decodeSingularInt32Field()?.let { this._optionalSeatFanFrontRight = CarServer_ClimateState.OneOf_OptionalSeatFanFrontRight.seatFanFrontRight(it) }
                141 -> decoder.decodeSingularBoolField()?.let { this._optionalAllowCabinOverheatProtection = CarServer_ClimateState.OneOf_OptionalAllowCabinOverheatProtection.allowCabinOverheatProtection(it) }
                142 -> decoder.decodeSingularBoolField()?.let { this._optionalSupportsFanOnlyCabinOverheatProtection = CarServer_ClimateState.OneOf_OptionalSupportsFanOnlyCabinOverheatProtection.supportsFanOnlyCabinOverheatProtection(it) }
                143 -> decoder.decodeSingularOpenEnumField(CarServer_ClimateState.CabinOverheatProtection_E.UNRECOGNIZED) { CarServer_ClimateState.CabinOverheatProtection_E.fromRawValue(it) }
                    ?.let { this._protobufStore_optionalCabinOverheatProtection(CarServer_ClimateState.OneOf_OptionalCabinOverheatProtection.cabinOverheatProtection(it)) }
                144 -> decoder.decodeSingularBoolField()?.let { this._optionalCabinOverheatProtectionActivelyCooling = CarServer_ClimateState.OneOf_OptionalCabinOverheatProtectionActivelyCooling.cabinOverheatProtectionActivelyCooling(it) }
                146 -> decoder.decodeSingularOpenEnumField(CarServer_ClimateState.CopActivationTemp.UNRECOGNIZED) { CarServer_ClimateState.CopActivationTemp.fromRawValue(it) }
                    ?.let { this._protobufStore_optionalCopActivationTemperature(CarServer_ClimateState.OneOf_OptionalCopActivationTemperature.copActivationTemperature(it)) }
                147 -> decoder.decodeSingularBoolField()?.let { this._optionalAutoSteeringWheelHeat = CarServer_ClimateState.OneOf_OptionalAutoSteeringWheelHeat.autoSteeringWheelHeat(it) }
                148 -> decoder.decodeSingularOpenEnumField(CarServer_StwHeatLevel.UNRECOGNIZED) { CarServer_StwHeatLevel.fromRawValue(it) }
                    ?.let { this._protobufStore_optionalSteeringWheelHeatLevel(CarServer_ClimateState.OneOf_OptionalSteeringWheelHeatLevel.steeringWheelHeatLevel(it)) }
                150 -> decoder.decodeSingularOpenEnumField(CarServer_ClimateState.HvacAutoRequest.UNRECOGNIZED) { CarServer_ClimateState.HvacAutoRequest.fromRawValue(it) }
                    ?.let { this._protobufStore_optionalHvacAutoRequest(CarServer_ClimateState.OneOf_OptionalHvacAutoRequest.hvacAutoRequest(it)) }
                151 -> decoder.decodeSingularOpenEnumField(CarServer_ClimateState.COPNotRunningReason.UNRECOGNIZED) { CarServer_ClimateState.COPNotRunningReason.fromRawValue(it) }
                    ?.let { this._protobufStore_optionalCopNotRunningReason(CarServer_ClimateState.OneOf_OptionalCopNotRunningReason.copNotRunningReason(it)) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        this._climateKeeperMode?.let {
            visitor.visitSingularMessageField(it, 30)
        }
        this._timestamp?.let {
            visitor.visitSingularMessageField(it, 33)
        }
        this._defrostMode?.let {
            visitor.visitSingularMessageField(it, 35)
        }
        (this._optionalInsideTempCelsius as? CarServer_ClimateState.OneOf_OptionalInsideTempCelsius.insideTempCelsius)?.let {
            visitor.visitSingularFloatField(it.value, 101)
        }
        (this._optionalOutsideTempCelsius as? CarServer_ClimateState.OneOf_OptionalOutsideTempCelsius.outsideTempCelsius)?.let {
            visitor.visitSingularFloatField(it.value, 102)
        }
        (this._optionalDriverTempSetting as? CarServer_ClimateState.OneOf_OptionalDriverTempSetting.driverTempSetting)?.let {
            visitor.visitSingularFloatField(it.value, 103)
        }
        (this._optionalPassengerTempSetting as? CarServer_ClimateState.OneOf_OptionalPassengerTempSetting.passengerTempSetting)?.let {
            visitor.visitSingularFloatField(it.value, 104)
        }
        (this._optionalLeftTempDirection as? CarServer_ClimateState.OneOf_OptionalLeftTempDirection.leftTempDirection)?.let {
            visitor.visitSingularInt32Field(it.value, 105)
        }
        (this._optionalRightTempDirection as? CarServer_ClimateState.OneOf_OptionalRightTempDirection.rightTempDirection)?.let {
            visitor.visitSingularInt32Field(it.value, 106)
        }
        (this._optionalIsFrontDefrosterOn as? CarServer_ClimateState.OneOf_OptionalIsFrontDefrosterOn.isFrontDefrosterOn)?.let {
            visitor.visitSingularBoolField(it.value, 107)
        }
        (this._optionalIsRearDefrosterOn as? CarServer_ClimateState.OneOf_OptionalIsRearDefrosterOn.isRearDefrosterOn)?.let {
            visitor.visitSingularBoolField(it.value, 108)
        }
        (this._optionalFanStatus as? CarServer_ClimateState.OneOf_OptionalFanStatus.fanStatus)?.let {
            visitor.visitSingularInt32Field(it.value, 109)
        }
        (this._optionalIsClimateOn as? CarServer_ClimateState.OneOf_OptionalIsClimateOn.isClimateOn)?.let {
            visitor.visitSingularBoolField(it.value, 110)
        }
        (this._optionalMinAvailTempCelsius as? CarServer_ClimateState.OneOf_OptionalMinAvailTempCelsius.minAvailTempCelsius)?.let {
            visitor.visitSingularFloatField(it.value, 111)
        }
        (this._optionalMaxAvailTempCelsius as? CarServer_ClimateState.OneOf_OptionalMaxAvailTempCelsius.maxAvailTempCelsius)?.let {
            visitor.visitSingularFloatField(it.value, 112)
        }
        (this._optionalSeatHeaterLeft as? CarServer_ClimateState.OneOf_OptionalSeatHeaterLeft.seatHeaterLeft)?.let {
            visitor.visitSingularInt32Field(it.value, 113)
        }
        (this._optionalSeatHeaterRight as? CarServer_ClimateState.OneOf_OptionalSeatHeaterRight.seatHeaterRight)?.let {
            visitor.visitSingularInt32Field(it.value, 114)
        }
        (this._optionalSeatHeaterRearLeft as? CarServer_ClimateState.OneOf_OptionalSeatHeaterRearLeft.seatHeaterRearLeft)?.let {
            visitor.visitSingularInt32Field(it.value, 115)
        }
        (this._optionalSeatHeaterRearRight as? CarServer_ClimateState.OneOf_OptionalSeatHeaterRearRight.seatHeaterRearRight)?.let {
            visitor.visitSingularInt32Field(it.value, 116)
        }
        (this._optionalSeatHeaterRearCenter as? CarServer_ClimateState.OneOf_OptionalSeatHeaterRearCenter.seatHeaterRearCenter)?.let {
            visitor.visitSingularInt32Field(it.value, 117)
        }
        (this._optionalSeatHeaterRearRightBack as? CarServer_ClimateState.OneOf_OptionalSeatHeaterRearRightBack.seatHeaterRearRightBack)?.let {
            visitor.visitSingularInt32Field(it.value, 118)
        }
        (this._optionalSeatHeaterRearLeftBack as? CarServer_ClimateState.OneOf_OptionalSeatHeaterRearLeftBack.seatHeaterRearLeftBack)?.let {
            visitor.visitSingularInt32Field(it.value, 119)
        }
        (this._optionalSeatHeaterThirdRowRight as? CarServer_ClimateState.OneOf_OptionalSeatHeaterThirdRowRight.seatHeaterThirdRowRight)?.let {
            visitor.visitSingularInt32Field(it.value, 120)
        }
        (this._optionalSeatHeaterThirdRowLeft as? CarServer_ClimateState.OneOf_OptionalSeatHeaterThirdRowLeft.seatHeaterThirdRowLeft)?.let {
            visitor.visitSingularInt32Field(it.value, 121)
        }
        (this._optionalBatteryHeater as? CarServer_ClimateState.OneOf_OptionalBatteryHeater.batteryHeater)?.let {
            visitor.visitSingularBoolField(it.value, 122)
        }
        (this._optionalBatteryHeaterNoPower as? CarServer_ClimateState.OneOf_OptionalBatteryHeaterNoPower.batteryHeaterNoPower)?.let {
            visitor.visitSingularBoolField(it.value, 123)
        }
        (this._optionalSteeringWheelHeater as? CarServer_ClimateState.OneOf_OptionalSteeringWheelHeater.steeringWheelHeater)?.let {
            visitor.visitSingularBoolField(it.value, 125)
        }
        (this._optionalWiperBladeHeater as? CarServer_ClimateState.OneOf_OptionalWiperBladeHeater.wiperBladeHeater)?.let {
            visitor.visitSingularBoolField(it.value, 126)
        }
        (this._optionalSideMirrorHeaters as? CarServer_ClimateState.OneOf_OptionalSideMirrorHeaters.sideMirrorHeaters)?.let {
            visitor.visitSingularBoolField(it.value, 127)
        }
        (this._optionalIsPreconditioning as? CarServer_ClimateState.OneOf_OptionalIsPreconditioning.isPreconditioning)?.let {
            visitor.visitSingularBoolField(it.value, 128)
        }
        (this._optionalRemoteHeaterControlEnabled as? CarServer_ClimateState.OneOf_OptionalRemoteHeaterControlEnabled.remoteHeaterControlEnabled)?.let {
            visitor.visitSingularBoolField(it.value, 129)
        }
        (this._optionalBioweaponModeOn as? CarServer_ClimateState.OneOf_OptionalBioweaponModeOn.bioweaponModeOn)?.let {
            visitor.visitSingularBoolField(it.value, 134)
        }
        (this._optionalIsAutoConditioningOn as? CarServer_ClimateState.OneOf_OptionalIsAutoConditioningOn.isAutoConditioningOn)?.let {
            visitor.visitSingularBoolField(it.value, 136)
        }
        (this._optionalAutoSeatClimateLeft as? CarServer_ClimateState.OneOf_OptionalAutoSeatClimateLeft.autoSeatClimateLeft)?.let {
            visitor.visitSingularBoolField(it.value, 137)
        }
        (this._optionalAutoSeatClimateRight as? CarServer_ClimateState.OneOf_OptionalAutoSeatClimateRight.autoSeatClimateRight)?.let {
            visitor.visitSingularBoolField(it.value, 138)
        }
        (this._optionalSeatFanFrontLeft as? CarServer_ClimateState.OneOf_OptionalSeatFanFrontLeft.seatFanFrontLeft)?.let {
            visitor.visitSingularInt32Field(it.value, 139)
        }
        (this._optionalSeatFanFrontRight as? CarServer_ClimateState.OneOf_OptionalSeatFanFrontRight.seatFanFrontRight)?.let {
            visitor.visitSingularInt32Field(it.value, 140)
        }
        (this._optionalAllowCabinOverheatProtection as? CarServer_ClimateState.OneOf_OptionalAllowCabinOverheatProtection.allowCabinOverheatProtection)?.let {
            visitor.visitSingularBoolField(it.value, 141)
        }
        (this._optionalSupportsFanOnlyCabinOverheatProtection as? CarServer_ClimateState.OneOf_OptionalSupportsFanOnlyCabinOverheatProtection.supportsFanOnlyCabinOverheatProtection)?.let {
            visitor.visitSingularBoolField(it.value, 142)
        }
        (this._optionalCabinOverheatProtection as? CarServer_ClimateState.OneOf_OptionalCabinOverheatProtection.cabinOverheatProtection)?.let {
            if (it.value != CarServer_ClimateState.CabinOverheatProtection_E.UNRECOGNIZED) {
                visitor.visitSingularEnumField(it.value.rawValue, 143)
            }
        }
        (this._optionalCabinOverheatProtectionActivelyCooling as? CarServer_ClimateState.OneOf_OptionalCabinOverheatProtectionActivelyCooling.cabinOverheatProtectionActivelyCooling)?.let {
            visitor.visitSingularBoolField(it.value, 144)
        }
        (this._optionalCopActivationTemperature as? CarServer_ClimateState.OneOf_OptionalCopActivationTemperature.copActivationTemperature)?.let {
            if (it.value != CarServer_ClimateState.CopActivationTemp.UNRECOGNIZED) {
                visitor.visitSingularEnumField(it.value.rawValue, 146)
            }
        }
        (this._optionalAutoSteeringWheelHeat as? CarServer_ClimateState.OneOf_OptionalAutoSteeringWheelHeat.autoSteeringWheelHeat)?.let {
            visitor.visitSingularBoolField(it.value, 147)
        }
        (this._optionalSteeringWheelHeatLevel as? CarServer_ClimateState.OneOf_OptionalSteeringWheelHeatLevel.steeringWheelHeatLevel)?.let {
            if (it.value != CarServer_StwHeatLevel.UNRECOGNIZED) {
                visitor.visitSingularEnumField(it.value.rawValue, 148)
            }
        }
        (this._optionalHvacAutoRequest as? CarServer_ClimateState.OneOf_OptionalHvacAutoRequest.hvacAutoRequest)?.let {
            if (it.value != CarServer_ClimateState.HvacAutoRequest.UNRECOGNIZED) {
                visitor.visitSingularEnumField(it.value.rawValue, 150)
            }
        }
        (this._optionalCopNotRunningReason as? CarServer_ClimateState.OneOf_OptionalCopNotRunningReason.copNotRunningReason)?.let {
            if (it.value != CarServer_ClimateState.COPNotRunningReason.UNRECOGNIZED) {
                visitor.visitSingularEnumField(it.value.rawValue, 151)
            }
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_ClimateState) return false
        if (this._optionalInsideTempCelsius != other._optionalInsideTempCelsius) return false
        if (this._optionalOutsideTempCelsius != other._optionalOutsideTempCelsius) return false
        if (this._optionalDriverTempSetting != other._optionalDriverTempSetting) return false
        if (this._optionalPassengerTempSetting != other._optionalPassengerTempSetting) return false
        if (this._optionalLeftTempDirection != other._optionalLeftTempDirection) return false
        if (this._optionalRightTempDirection != other._optionalRightTempDirection) return false
        if (this._optionalIsFrontDefrosterOn != other._optionalIsFrontDefrosterOn) return false
        if (this._optionalIsRearDefrosterOn != other._optionalIsRearDefrosterOn) return false
        if (this._optionalFanStatus != other._optionalFanStatus) return false
        if (this._optionalIsClimateOn != other._optionalIsClimateOn) return false
        if (this._optionalMinAvailTempCelsius != other._optionalMinAvailTempCelsius) return false
        if (this._optionalMaxAvailTempCelsius != other._optionalMaxAvailTempCelsius) return false
        if (this._optionalSeatHeaterLeft != other._optionalSeatHeaterLeft) return false
        if (this._optionalSeatHeaterRight != other._optionalSeatHeaterRight) return false
        if (this._optionalSeatHeaterRearLeft != other._optionalSeatHeaterRearLeft) return false
        if (this._optionalSeatHeaterRearRight != other._optionalSeatHeaterRearRight) return false
        if (this._optionalSeatHeaterRearCenter != other._optionalSeatHeaterRearCenter) return false
        if (this._optionalSeatHeaterRearRightBack != other._optionalSeatHeaterRearRightBack) return false
        if (this._optionalSeatHeaterRearLeftBack != other._optionalSeatHeaterRearLeftBack) return false
        if (this._optionalSeatHeaterThirdRowRight != other._optionalSeatHeaterThirdRowRight) return false
        if (this._optionalSeatHeaterThirdRowLeft != other._optionalSeatHeaterThirdRowLeft) return false
        if (this._optionalBatteryHeater != other._optionalBatteryHeater) return false
        if (this._optionalBatteryHeaterNoPower != other._optionalBatteryHeaterNoPower) return false
        if (this._optionalSteeringWheelHeater != other._optionalSteeringWheelHeater) return false
        if (this._optionalWiperBladeHeater != other._optionalWiperBladeHeater) return false
        if (this._optionalSideMirrorHeaters != other._optionalSideMirrorHeaters) return false
        if (this._optionalIsPreconditioning != other._optionalIsPreconditioning) return false
        if (this._optionalRemoteHeaterControlEnabled != other._optionalRemoteHeaterControlEnabled) return false
        if (this._climateKeeperMode != other._climateKeeperMode) return false
        if (this._timestamp != other._timestamp) return false
        if (this._optionalBioweaponModeOn != other._optionalBioweaponModeOn) return false
        if (this._defrostMode != other._defrostMode) return false
        if (this._optionalIsAutoConditioningOn != other._optionalIsAutoConditioningOn) return false
        if (this._optionalAutoSeatClimateLeft != other._optionalAutoSeatClimateLeft) return false
        if (this._optionalAutoSeatClimateRight != other._optionalAutoSeatClimateRight) return false
        if (this._optionalSeatFanFrontLeft != other._optionalSeatFanFrontLeft) return false
        if (this._optionalSeatFanFrontRight != other._optionalSeatFanFrontRight) return false
        if (this._optionalAllowCabinOverheatProtection != other._optionalAllowCabinOverheatProtection) return false
        if (this._optionalSupportsFanOnlyCabinOverheatProtection != other._optionalSupportsFanOnlyCabinOverheatProtection) return false
        if (this._optionalCabinOverheatProtection != other._optionalCabinOverheatProtection) return false
        if (this._optionalCabinOverheatProtectionActivelyCooling != other._optionalCabinOverheatProtectionActivelyCooling) return false
        if (this._optionalCopActivationTemperature != other._optionalCopActivationTemperature) return false
        if (this._optionalAutoSteeringWheelHeat != other._optionalAutoSteeringWheelHeat) return false
        if (this._optionalSteeringWheelHeatLevel != other._optionalSteeringWheelHeatLevel) return false
        if (this._optionalHvacAutoRequest != other._optionalHvacAutoRequest) return false
        if (this._optionalCopNotRunningReason != other._optionalCopNotRunningReason) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._optionalInsideTempCelsius?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalOutsideTempCelsius?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalDriverTempSetting?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalPassengerTempSetting?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalLeftTempDirection?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalRightTempDirection?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalIsFrontDefrosterOn?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalIsRearDefrosterOn?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalFanStatus?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalIsClimateOn?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalMinAvailTempCelsius?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalMaxAvailTempCelsius?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalSeatHeaterLeft?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalSeatHeaterRight?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalSeatHeaterRearLeft?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalSeatHeaterRearRight?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalSeatHeaterRearCenter?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalSeatHeaterRearRightBack?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalSeatHeaterRearLeftBack?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalSeatHeaterThirdRowRight?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalSeatHeaterThirdRowLeft?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalBatteryHeater?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalBatteryHeaterNoPower?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalSteeringWheelHeater?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalWiperBladeHeater?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalSideMirrorHeaters?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalIsPreconditioning?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalRemoteHeaterControlEnabled?.hashCode() ?: 0)
        hash = 31 * hash + (this._climateKeeperMode?.hashCode() ?: 0)
        hash = 31 * hash + (this._timestamp?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalBioweaponModeOn?.hashCode() ?: 0)
        hash = 31 * hash + (this._defrostMode?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalIsAutoConditioningOn?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalAutoSeatClimateLeft?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalAutoSeatClimateRight?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalSeatFanFrontLeft?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalSeatFanFrontRight?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalAllowCabinOverheatProtection?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalSupportsFanOnlyCabinOverheatProtection?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalCabinOverheatProtection?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalCabinOverheatProtectionActivelyCooling?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalCopActivationTemperature?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalAutoSteeringWheelHeat?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalSteeringWheelHeatLevel?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalHvacAutoRequest?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalCopNotRunningReason?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ClimateState {
        val result = CarServer_ClimateState()
        result._optionalInsideTempCelsius = this._optionalInsideTempCelsius
        result._optionalOutsideTempCelsius = this._optionalOutsideTempCelsius
        result._optionalDriverTempSetting = this._optionalDriverTempSetting
        result._optionalPassengerTempSetting = this._optionalPassengerTempSetting
        result._optionalLeftTempDirection = this._optionalLeftTempDirection
        result._optionalRightTempDirection = this._optionalRightTempDirection
        result._optionalIsFrontDefrosterOn = this._optionalIsFrontDefrosterOn
        result._optionalIsRearDefrosterOn = this._optionalIsRearDefrosterOn
        result._optionalFanStatus = this._optionalFanStatus
        result._optionalIsClimateOn = this._optionalIsClimateOn
        result._optionalMinAvailTempCelsius = this._optionalMinAvailTempCelsius
        result._optionalMaxAvailTempCelsius = this._optionalMaxAvailTempCelsius
        result._optionalSeatHeaterLeft = this._optionalSeatHeaterLeft
        result._optionalSeatHeaterRight = this._optionalSeatHeaterRight
        result._optionalSeatHeaterRearLeft = this._optionalSeatHeaterRearLeft
        result._optionalSeatHeaterRearRight = this._optionalSeatHeaterRearRight
        result._optionalSeatHeaterRearCenter = this._optionalSeatHeaterRearCenter
        result._optionalSeatHeaterRearRightBack = this._optionalSeatHeaterRearRightBack
        result._optionalSeatHeaterRearLeftBack = this._optionalSeatHeaterRearLeftBack
        result._optionalSeatHeaterThirdRowRight = this._optionalSeatHeaterThirdRowRight
        result._optionalSeatHeaterThirdRowLeft = this._optionalSeatHeaterThirdRowLeft
        result._optionalBatteryHeater = this._optionalBatteryHeater
        result._optionalBatteryHeaterNoPower = this._optionalBatteryHeaterNoPower
        result._optionalSteeringWheelHeater = this._optionalSteeringWheelHeater
        result._optionalWiperBladeHeater = this._optionalWiperBladeHeater
        result._optionalSideMirrorHeaters = this._optionalSideMirrorHeaters
        result._optionalIsPreconditioning = this._optionalIsPreconditioning
        result._optionalRemoteHeaterControlEnabled = this._optionalRemoteHeaterControlEnabled
        result._climateKeeperMode = this._climateKeeperMode?.copy()
        result._timestamp = this._timestamp?.copy()
        result._optionalBioweaponModeOn = this._optionalBioweaponModeOn
        result._defrostMode = this._defrostMode?.copy()
        result._optionalIsAutoConditioningOn = this._optionalIsAutoConditioningOn
        result._optionalAutoSeatClimateLeft = this._optionalAutoSeatClimateLeft
        result._optionalAutoSeatClimateRight = this._optionalAutoSeatClimateRight
        result._optionalSeatFanFrontLeft = this._optionalSeatFanFrontLeft
        result._optionalSeatFanFrontRight = this._optionalSeatFanFrontRight
        result._optionalAllowCabinOverheatProtection = this._optionalAllowCabinOverheatProtection
        result._optionalSupportsFanOnlyCabinOverheatProtection = this._optionalSupportsFanOnlyCabinOverheatProtection
        result._optionalCabinOverheatProtection = this._optionalCabinOverheatProtection
        result._optionalCabinOverheatProtectionActivelyCooling = this._optionalCabinOverheatProtectionActivelyCooling
        result._optionalCopActivationTemperature = this._optionalCopActivationTemperature
        result._optionalAutoSteeringWheelHeat = this._optionalAutoSteeringWheelHeat
        result._optionalSteeringWheelHeatLevel = this._optionalSteeringWheelHeatLevel
        result._optionalHvacAutoRequest = this._optionalHvacAutoRequest
        result._optionalCopNotRunningReason = this._optionalCopNotRunningReason
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ClimateState"

        fun with(block: CarServer_ClimateState.() -> Unit): CarServer_ClimateState =
            CarServer_ClimateState().apply(block)
    }
}

class CarServer_TirePressureState() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var timestamp: Google_Protobuf_Timestamp
        get() = this._timestamp ?: _protobufPending(1, { Google_Protobuf_Timestamp() }) { this._timestamp = it }
        set(value) {
            _protobufDropPending(1)
            this._timestamp = value.copy()
            _protobufMutated()
        }

    val hasTimestamp: Boolean
        get() = this._timestamp != null

    fun clearTimestamp() {
        _protobufDropPending(1)
        this._timestamp = null
        _protobufMutated()
    }

    var optionalTpmsPressureFl: CarServer_TirePressureState.OneOf_OptionalTpmsPressureFl?
        get() = this._optionalTpmsPressureFl
        set(value) {
            this._optionalTpmsPressureFl = value
            _protobufMutated()
        }

    var tpmsPressureFl: Float
        get() = (this._optionalTpmsPressureFl as? CarServer_TirePressureState.OneOf_OptionalTpmsPressureFl.tpmsPressureFl)?.value ?: 0f
        set(value) {
            this.optionalTpmsPressureFl = CarServer_TirePressureState.OneOf_OptionalTpmsPressureFl.tpmsPressureFl(value)
        }

    var optionalTpmsPressureFr: CarServer_TirePressureState.OneOf_OptionalTpmsPressureFr?
        get() = this._optionalTpmsPressureFr
        set(value) {
            this._optionalTpmsPressureFr = value
            _protobufMutated()
        }

    var tpmsPressureFr: Float
        get() = (this._optionalTpmsPressureFr as? CarServer_TirePressureState.OneOf_OptionalTpmsPressureFr.tpmsPressureFr)?.value ?: 0f
        set(value) {
            this.optionalTpmsPressureFr = CarServer_TirePressureState.OneOf_OptionalTpmsPressureFr.tpmsPressureFr(value)
        }

    var optionalTpmsPressureRl: CarServer_TirePressureState.OneOf_OptionalTpmsPressureRl?
        get() = this._optionalTpmsPressureRl
        set(value) {
            this._optionalTpmsPressureRl = value
            _protobufMutated()
        }

    var tpmsPressureRl: Float
        get() = (this._optionalTpmsPressureRl as? CarServer_TirePressureState.OneOf_OptionalTpmsPressureRl.tpmsPressureRl)?.value ?: 0f
        set(value) {
            this.optionalTpmsPressureRl = CarServer_TirePressureState.OneOf_OptionalTpmsPressureRl.tpmsPressureRl(value)
        }

    var optionalTpmsPressureRr: CarServer_TirePressureState.OneOf_OptionalTpmsPressureRr?
        get() = this._optionalTpmsPressureRr
        set(value) {
            this._optionalTpmsPressureRr = value
            _protobufMutated()
        }

    var tpmsPressureRr: Float
        get() = (this._optionalTpmsPressureRr as? CarServer_TirePressureState.OneOf_OptionalTpmsPressureRr.tpmsPressureRr)?.value ?: 0f
        set(value) {
            this.optionalTpmsPressureRr = CarServer_TirePressureState.OneOf_OptionalTpmsPressureRr.tpmsPressureRr(value)
        }

    var tpmsLastSeenPressureTimeFl: Google_Protobuf_Timestamp
        get() = this._tpmsLastSeenPressureTimeFl ?: _protobufPending(6, { Google_Protobuf_Timestamp() }) { this._tpmsLastSeenPressureTimeFl = it }
        set(value) {
            _protobufDropPending(6)
            this._tpmsLastSeenPressureTimeFl = value.copy()
            _protobufMutated()
        }

    val hasTpmsLastSeenPressureTimeFl: Boolean
        get() = this._tpmsLastSeenPressureTimeFl != null

    fun clearTpmsLastSeenPressureTimeFl() {
        _protobufDropPending(6)
        this._tpmsLastSeenPressureTimeFl = null
        _protobufMutated()
    }

    var tpmsLastSeenPressureTimeFr: Google_Protobuf_Timestamp
        get() = this._tpmsLastSeenPressureTimeFr ?: _protobufPending(7, { Google_Protobuf_Timestamp() }) { this._tpmsLastSeenPressureTimeFr = it }
        set(value) {
            _protobufDropPending(7)
            this._tpmsLastSeenPressureTimeFr = value.copy()
            _protobufMutated()
        }

    val hasTpmsLastSeenPressureTimeFr: Boolean
        get() = this._tpmsLastSeenPressureTimeFr != null

    fun clearTpmsLastSeenPressureTimeFr() {
        _protobufDropPending(7)
        this._tpmsLastSeenPressureTimeFr = null
        _protobufMutated()
    }

    var tpmsLastSeenPressureTimeRl: Google_Protobuf_Timestamp
        get() = this._tpmsLastSeenPressureTimeRl ?: _protobufPending(8, { Google_Protobuf_Timestamp() }) { this._tpmsLastSeenPressureTimeRl = it }
        set(value) {
            _protobufDropPending(8)
            this._tpmsLastSeenPressureTimeRl = value.copy()
            _protobufMutated()
        }

    val hasTpmsLastSeenPressureTimeRl: Boolean
        get() = this._tpmsLastSeenPressureTimeRl != null

    fun clearTpmsLastSeenPressureTimeRl() {
        _protobufDropPending(8)
        this._tpmsLastSeenPressureTimeRl = null
        _protobufMutated()
    }

    var tpmsLastSeenPressureTimeRr: Google_Protobuf_Timestamp
        get() = this._tpmsLastSeenPressureTimeRr ?: _protobufPending(9, { Google_Protobuf_Timestamp() }) { this._tpmsLastSeenPressureTimeRr = it }
        set(value) {
            _protobufDropPending(9)
            this._tpmsLastSeenPressureTimeRr = value.copy()
            _protobufMutated()
        }

    val hasTpmsLastSeenPressureTimeRr: Boolean
        get() = this._tpmsLastSeenPressureTimeRr != null

    fun clearTpmsLastSeenPressureTimeRr() {
        _protobufDropPending(9)
        this._tpmsLastSeenPressureTimeRr = null
        _protobufMutated()
    }

    var optionalTpmsHardWarningFl: CarServer_TirePressureState.OneOf_OptionalTpmsHardWarningFl?
        get() = this._optionalTpmsHardWarningFl
        set(value) {
            this._optionalTpmsHardWarningFl = value
            _protobufMutated()
        }

    var tpmsHardWarningFl: Boolean
        get() = (this._optionalTpmsHardWarningFl as? CarServer_TirePressureState.OneOf_OptionalTpmsHardWarningFl.tpmsHardWarningFl)?.value ?: false
        set(value) {
            this.optionalTpmsHardWarningFl = CarServer_TirePressureState.OneOf_OptionalTpmsHardWarningFl.tpmsHardWarningFl(value)
        }

    var optionalTpmsHardWarningFr: CarServer_TirePressureState.OneOf_OptionalTpmsHardWarningFr?
        get() = this._optionalTpmsHardWarningFr
        set(value) {
            this._optionalTpmsHardWarningFr = value
            _protobufMutated()
        }

    var tpmsHardWarningFr: Boolean
        get() = (this._optionalTpmsHardWarningFr as? CarServer_TirePressureState.OneOf_OptionalTpmsHardWarningFr.tpmsHardWarningFr)?.value ?: false
        set(value) {
            this.optionalTpmsHardWarningFr = CarServer_TirePressureState.OneOf_OptionalTpmsHardWarningFr.tpmsHardWarningFr(value)
        }

    var optionalTpmsHardWarningRl: CarServer_TirePressureState.OneOf_OptionalTpmsHardWarningRl?
        get() = this._optionalTpmsHardWarningRl
        set(value) {
            this._optionalTpmsHardWarningRl = value
            _protobufMutated()
        }

    var tpmsHardWarningRl: Boolean
        get() = (this._optionalTpmsHardWarningRl as? CarServer_TirePressureState.OneOf_OptionalTpmsHardWarningRl.tpmsHardWarningRl)?.value ?: false
        set(value) {
            this.optionalTpmsHardWarningRl = CarServer_TirePressureState.OneOf_OptionalTpmsHardWarningRl.tpmsHardWarningRl(value)
        }

    var optionalTpmsHardWarningRr: CarServer_TirePressureState.OneOf_OptionalTpmsHardWarningRr?
        get() = this._optionalTpmsHardWarningRr
        set(value) {
            this._optionalTpmsHardWarningRr = value
            _protobufMutated()
        }

    var tpmsHardWarningRr: Boolean
        get() = (this._optionalTpmsHardWarningRr as? CarServer_TirePressureState.OneOf_OptionalTpmsHardWarningRr.tpmsHardWarningRr)?.value ?: false
        set(value) {
            this.optionalTpmsHardWarningRr = CarServer_TirePressureState.OneOf_OptionalTpmsHardWarningRr.tpmsHardWarningRr(value)
        }

    var optionalTpmsSoftWarningFl: CarServer_TirePressureState.OneOf_OptionalTpmsSoftWarningFl?
        get() = this._optionalTpmsSoftWarningFl
        set(value) {
            this._optionalTpmsSoftWarningFl = value
            _protobufMutated()
        }

    var tpmsSoftWarningFl: Boolean
        get() = (this._optionalTpmsSoftWarningFl as? CarServer_TirePressureState.OneOf_OptionalTpmsSoftWarningFl.tpmsSoftWarningFl)?.value ?: false
        set(value) {
            this.optionalTpmsSoftWarningFl = CarServer_TirePressureState.OneOf_OptionalTpmsSoftWarningFl.tpmsSoftWarningFl(value)
        }

    var optionalTpmsSoftWarningFr: CarServer_TirePressureState.OneOf_OptionalTpmsSoftWarningFr?
        get() = this._optionalTpmsSoftWarningFr
        set(value) {
            this._optionalTpmsSoftWarningFr = value
            _protobufMutated()
        }

    var tpmsSoftWarningFr: Boolean
        get() = (this._optionalTpmsSoftWarningFr as? CarServer_TirePressureState.OneOf_OptionalTpmsSoftWarningFr.tpmsSoftWarningFr)?.value ?: false
        set(value) {
            this.optionalTpmsSoftWarningFr = CarServer_TirePressureState.OneOf_OptionalTpmsSoftWarningFr.tpmsSoftWarningFr(value)
        }

    var optionalTpmsSoftWarningRl: CarServer_TirePressureState.OneOf_OptionalTpmsSoftWarningRl?
        get() = this._optionalTpmsSoftWarningRl
        set(value) {
            this._optionalTpmsSoftWarningRl = value
            _protobufMutated()
        }

    var tpmsSoftWarningRl: Boolean
        get() = (this._optionalTpmsSoftWarningRl as? CarServer_TirePressureState.OneOf_OptionalTpmsSoftWarningRl.tpmsSoftWarningRl)?.value ?: false
        set(value) {
            this.optionalTpmsSoftWarningRl = CarServer_TirePressureState.OneOf_OptionalTpmsSoftWarningRl.tpmsSoftWarningRl(value)
        }

    var optionalTpmsSoftWarningRr: CarServer_TirePressureState.OneOf_OptionalTpmsSoftWarningRr?
        get() = this._optionalTpmsSoftWarningRr
        set(value) {
            this._optionalTpmsSoftWarningRr = value
            _protobufMutated()
        }

    var tpmsSoftWarningRr: Boolean
        get() = (this._optionalTpmsSoftWarningRr as? CarServer_TirePressureState.OneOf_OptionalTpmsSoftWarningRr.tpmsSoftWarningRr)?.value ?: false
        set(value) {
            this.optionalTpmsSoftWarningRr = CarServer_TirePressureState.OneOf_OptionalTpmsSoftWarningRr.tpmsSoftWarningRr(value)
        }

    var optionalTpmsRcpFrontValue: CarServer_TirePressureState.OneOf_OptionalTpmsRcpFrontValue?
        get() = this._optionalTpmsRcpFrontValue
        set(value) {
            this._optionalTpmsRcpFrontValue = value
            _protobufMutated()
        }

    var tpmsRcpFrontValue: Float
        get() = (this._optionalTpmsRcpFrontValue as? CarServer_TirePressureState.OneOf_OptionalTpmsRcpFrontValue.tpmsRcpFrontValue)?.value ?: 0f
        set(value) {
            this.optionalTpmsRcpFrontValue = CarServer_TirePressureState.OneOf_OptionalTpmsRcpFrontValue.tpmsRcpFrontValue(value)
        }

    var optionalTpmsRcpRearValue: CarServer_TirePressureState.OneOf_OptionalTpmsRcpRearValue?
        get() = this._optionalTpmsRcpRearValue
        set(value) {
            this._optionalTpmsRcpRearValue = value
            _protobufMutated()
        }

    var tpmsRcpRearValue: Float
        get() = (this._optionalTpmsRcpRearValue as? CarServer_TirePressureState.OneOf_OptionalTpmsRcpRearValue.tpmsRcpRearValue)?.value ?: 0f
        set(value) {
            this.optionalTpmsRcpRearValue = CarServer_TirePressureState.OneOf_OptionalTpmsRcpRearValue.tpmsRcpRearValue(value)
        }

    sealed class OneOf_OptionalTpmsPressureFl(value: Any) : ProtobufOneofCase(value) {
        class tpmsPressureFl(val value: Float) : OneOf_OptionalTpmsPressureFl(value)
    }

    sealed class OneOf_OptionalTpmsPressureFr(value: Any) : ProtobufOneofCase(value) {
        class tpmsPressureFr(val value: Float) : OneOf_OptionalTpmsPressureFr(value)
    }

    sealed class OneOf_OptionalTpmsPressureRl(value: Any) : ProtobufOneofCase(value) {
        class tpmsPressureRl(val value: Float) : OneOf_OptionalTpmsPressureRl(value)
    }

    sealed class OneOf_OptionalTpmsPressureRr(value: Any) : ProtobufOneofCase(value) {
        class tpmsPressureRr(val value: Float) : OneOf_OptionalTpmsPressureRr(value)
    }

    sealed class OneOf_OptionalTpmsHardWarningFl(value: Any) : ProtobufOneofCase(value) {
        class tpmsHardWarningFl(val value: Boolean) : OneOf_OptionalTpmsHardWarningFl(value)
    }

    sealed class OneOf_OptionalTpmsHardWarningFr(value: Any) : ProtobufOneofCase(value) {
        class tpmsHardWarningFr(val value: Boolean) : OneOf_OptionalTpmsHardWarningFr(value)
    }

    sealed class OneOf_OptionalTpmsHardWarningRl(value: Any) : ProtobufOneofCase(value) {
        class tpmsHardWarningRl(val value: Boolean) : OneOf_OptionalTpmsHardWarningRl(value)
    }

    sealed class OneOf_OptionalTpmsHardWarningRr(value: Any) : ProtobufOneofCase(value) {
        class tpmsHardWarningRr(val value: Boolean) : OneOf_OptionalTpmsHardWarningRr(value)
    }

    sealed class OneOf_OptionalTpmsSoftWarningFl(value: Any) : ProtobufOneofCase(value) {
        class tpmsSoftWarningFl(val value: Boolean) : OneOf_OptionalTpmsSoftWarningFl(value)
    }

    sealed class OneOf_OptionalTpmsSoftWarningFr(value: Any) : ProtobufOneofCase(value) {
        class tpmsSoftWarningFr(val value: Boolean) : OneOf_OptionalTpmsSoftWarningFr(value)
    }

    sealed class OneOf_OptionalTpmsSoftWarningRl(value: Any) : ProtobufOneofCase(value) {
        class tpmsSoftWarningRl(val value: Boolean) : OneOf_OptionalTpmsSoftWarningRl(value)
    }

    sealed class OneOf_OptionalTpmsSoftWarningRr(value: Any) : ProtobufOneofCase(value) {
        class tpmsSoftWarningRr(val value: Boolean) : OneOf_OptionalTpmsSoftWarningRr(value)
    }

    sealed class OneOf_OptionalTpmsRcpFrontValue(value: Any) : ProtobufOneofCase(value) {
        class tpmsRcpFrontValue(val value: Float) : OneOf_OptionalTpmsRcpFrontValue(value)
    }

    sealed class OneOf_OptionalTpmsRcpRearValue(value: Any) : ProtobufOneofCase(value) {
        class tpmsRcpRearValue(val value: Float) : OneOf_OptionalTpmsRcpRearValue(value)
    }

    private var _timestamp: Google_Protobuf_Timestamp? = null
    private var _optionalTpmsPressureFl: CarServer_TirePressureState.OneOf_OptionalTpmsPressureFl? = null
    private var _optionalTpmsPressureFr: CarServer_TirePressureState.OneOf_OptionalTpmsPressureFr? = null
    private var _optionalTpmsPressureRl: CarServer_TirePressureState.OneOf_OptionalTpmsPressureRl? = null
    private var _optionalTpmsPressureRr: CarServer_TirePressureState.OneOf_OptionalTpmsPressureRr? = null
    private var _tpmsLastSeenPressureTimeFl: Google_Protobuf_Timestamp? = null
    private var _tpmsLastSeenPressureTimeFr: Google_Protobuf_Timestamp? = null
    private var _tpmsLastSeenPressureTimeRl: Google_Protobuf_Timestamp? = null
    private var _tpmsLastSeenPressureTimeRr: Google_Protobuf_Timestamp? = null
    private var _optionalTpmsHardWarningFl: CarServer_TirePressureState.OneOf_OptionalTpmsHardWarningFl? = null
    private var _optionalTpmsHardWarningFr: CarServer_TirePressureState.OneOf_OptionalTpmsHardWarningFr? = null
    private var _optionalTpmsHardWarningRl: CarServer_TirePressureState.OneOf_OptionalTpmsHardWarningRl? = null
    private var _optionalTpmsHardWarningRr: CarServer_TirePressureState.OneOf_OptionalTpmsHardWarningRr? = null
    private var _optionalTpmsSoftWarningFl: CarServer_TirePressureState.OneOf_OptionalTpmsSoftWarningFl? = null
    private var _optionalTpmsSoftWarningFr: CarServer_TirePressureState.OneOf_OptionalTpmsSoftWarningFr? = null
    private var _optionalTpmsSoftWarningRl: CarServer_TirePressureState.OneOf_OptionalTpmsSoftWarningRl? = null
    private var _optionalTpmsSoftWarningRr: CarServer_TirePressureState.OneOf_OptionalTpmsSoftWarningRr? = null
    private var _optionalTpmsRcpFrontValue: CarServer_TirePressureState.OneOf_OptionalTpmsRcpFrontValue? = null
    private var _optionalTpmsRcpRearValue: CarServer_TirePressureState.OneOf_OptionalTpmsRcpRearValue? = null

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularMessageField(this._timestamp) { Google_Protobuf_Timestamp() }?.let {
                    _protobufDropPending(1)
                    this._timestamp = it
                }
                2 -> decoder.decodeSingularFloatField()?.let { this._optionalTpmsPressureFl = CarServer_TirePressureState.OneOf_OptionalTpmsPressureFl.tpmsPressureFl(it) }
                3 -> decoder.decodeSingularFloatField()?.let { this._optionalTpmsPressureFr = CarServer_TirePressureState.OneOf_OptionalTpmsPressureFr.tpmsPressureFr(it) }
                4 -> decoder.decodeSingularFloatField()?.let { this._optionalTpmsPressureRl = CarServer_TirePressureState.OneOf_OptionalTpmsPressureRl.tpmsPressureRl(it) }
                5 -> decoder.decodeSingularFloatField()?.let { this._optionalTpmsPressureRr = CarServer_TirePressureState.OneOf_OptionalTpmsPressureRr.tpmsPressureRr(it) }
                6 -> decoder.decodeSingularMessageField(this._tpmsLastSeenPressureTimeFl) { Google_Protobuf_Timestamp() }?.let {
                    _protobufDropPending(6)
                    this._tpmsLastSeenPressureTimeFl = it
                }
                7 -> decoder.decodeSingularMessageField(this._tpmsLastSeenPressureTimeFr) { Google_Protobuf_Timestamp() }?.let {
                    _protobufDropPending(7)
                    this._tpmsLastSeenPressureTimeFr = it
                }
                8 -> decoder.decodeSingularMessageField(this._tpmsLastSeenPressureTimeRl) { Google_Protobuf_Timestamp() }?.let {
                    _protobufDropPending(8)
                    this._tpmsLastSeenPressureTimeRl = it
                }
                9 -> decoder.decodeSingularMessageField(this._tpmsLastSeenPressureTimeRr) { Google_Protobuf_Timestamp() }?.let {
                    _protobufDropPending(9)
                    this._tpmsLastSeenPressureTimeRr = it
                }
                10 -> decoder.decodeSingularBoolField()?.let { this._optionalTpmsHardWarningFl = CarServer_TirePressureState.OneOf_OptionalTpmsHardWarningFl.tpmsHardWarningFl(it) }
                11 -> decoder.decodeSingularBoolField()?.let { this._optionalTpmsHardWarningFr = CarServer_TirePressureState.OneOf_OptionalTpmsHardWarningFr.tpmsHardWarningFr(it) }
                12 -> decoder.decodeSingularBoolField()?.let { this._optionalTpmsHardWarningRl = CarServer_TirePressureState.OneOf_OptionalTpmsHardWarningRl.tpmsHardWarningRl(it) }
                13 -> decoder.decodeSingularBoolField()?.let { this._optionalTpmsHardWarningRr = CarServer_TirePressureState.OneOf_OptionalTpmsHardWarningRr.tpmsHardWarningRr(it) }
                14 -> decoder.decodeSingularBoolField()?.let { this._optionalTpmsSoftWarningFl = CarServer_TirePressureState.OneOf_OptionalTpmsSoftWarningFl.tpmsSoftWarningFl(it) }
                15 -> decoder.decodeSingularBoolField()?.let { this._optionalTpmsSoftWarningFr = CarServer_TirePressureState.OneOf_OptionalTpmsSoftWarningFr.tpmsSoftWarningFr(it) }
                16 -> decoder.decodeSingularBoolField()?.let { this._optionalTpmsSoftWarningRl = CarServer_TirePressureState.OneOf_OptionalTpmsSoftWarningRl.tpmsSoftWarningRl(it) }
                17 -> decoder.decodeSingularBoolField()?.let { this._optionalTpmsSoftWarningRr = CarServer_TirePressureState.OneOf_OptionalTpmsSoftWarningRr.tpmsSoftWarningRr(it) }
                18 -> decoder.decodeSingularFloatField()?.let { this._optionalTpmsRcpFrontValue = CarServer_TirePressureState.OneOf_OptionalTpmsRcpFrontValue.tpmsRcpFrontValue(it) }
                19 -> decoder.decodeSingularFloatField()?.let { this._optionalTpmsRcpRearValue = CarServer_TirePressureState.OneOf_OptionalTpmsRcpRearValue.tpmsRcpRearValue(it) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        this._timestamp?.let {
            visitor.visitSingularMessageField(it, 1)
        }
        (this._optionalTpmsPressureFl as? CarServer_TirePressureState.OneOf_OptionalTpmsPressureFl.tpmsPressureFl)?.let {
            visitor.visitSingularFloatField(it.value, 2)
        }
        (this._optionalTpmsPressureFr as? CarServer_TirePressureState.OneOf_OptionalTpmsPressureFr.tpmsPressureFr)?.let {
            visitor.visitSingularFloatField(it.value, 3)
        }
        (this._optionalTpmsPressureRl as? CarServer_TirePressureState.OneOf_OptionalTpmsPressureRl.tpmsPressureRl)?.let {
            visitor.visitSingularFloatField(it.value, 4)
        }
        (this._optionalTpmsPressureRr as? CarServer_TirePressureState.OneOf_OptionalTpmsPressureRr.tpmsPressureRr)?.let {
            visitor.visitSingularFloatField(it.value, 5)
        }
        this._tpmsLastSeenPressureTimeFl?.let {
            visitor.visitSingularMessageField(it, 6)
        }
        this._tpmsLastSeenPressureTimeFr?.let {
            visitor.visitSingularMessageField(it, 7)
        }
        this._tpmsLastSeenPressureTimeRl?.let {
            visitor.visitSingularMessageField(it, 8)
        }
        this._tpmsLastSeenPressureTimeRr?.let {
            visitor.visitSingularMessageField(it, 9)
        }
        (this._optionalTpmsHardWarningFl as? CarServer_TirePressureState.OneOf_OptionalTpmsHardWarningFl.tpmsHardWarningFl)?.let {
            visitor.visitSingularBoolField(it.value, 10)
        }
        (this._optionalTpmsHardWarningFr as? CarServer_TirePressureState.OneOf_OptionalTpmsHardWarningFr.tpmsHardWarningFr)?.let {
            visitor.visitSingularBoolField(it.value, 11)
        }
        (this._optionalTpmsHardWarningRl as? CarServer_TirePressureState.OneOf_OptionalTpmsHardWarningRl.tpmsHardWarningRl)?.let {
            visitor.visitSingularBoolField(it.value, 12)
        }
        (this._optionalTpmsHardWarningRr as? CarServer_TirePressureState.OneOf_OptionalTpmsHardWarningRr.tpmsHardWarningRr)?.let {
            visitor.visitSingularBoolField(it.value, 13)
        }
        (this._optionalTpmsSoftWarningFl as? CarServer_TirePressureState.OneOf_OptionalTpmsSoftWarningFl.tpmsSoftWarningFl)?.let {
            visitor.visitSingularBoolField(it.value, 14)
        }
        (this._optionalTpmsSoftWarningFr as? CarServer_TirePressureState.OneOf_OptionalTpmsSoftWarningFr.tpmsSoftWarningFr)?.let {
            visitor.visitSingularBoolField(it.value, 15)
        }
        (this._optionalTpmsSoftWarningRl as? CarServer_TirePressureState.OneOf_OptionalTpmsSoftWarningRl.tpmsSoftWarningRl)?.let {
            visitor.visitSingularBoolField(it.value, 16)
        }
        (this._optionalTpmsSoftWarningRr as? CarServer_TirePressureState.OneOf_OptionalTpmsSoftWarningRr.tpmsSoftWarningRr)?.let {
            visitor.visitSingularBoolField(it.value, 17)
        }
        (this._optionalTpmsRcpFrontValue as? CarServer_TirePressureState.OneOf_OptionalTpmsRcpFrontValue.tpmsRcpFrontValue)?.let {
            visitor.visitSingularFloatField(it.value, 18)
        }
        (this._optionalTpmsRcpRearValue as? CarServer_TirePressureState.OneOf_OptionalTpmsRcpRearValue.tpmsRcpRearValue)?.let {
            visitor.visitSingularFloatField(it.value, 19)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_TirePressureState) return false
        if (this._timestamp != other._timestamp) return false
        if (this._optionalTpmsPressureFl != other._optionalTpmsPressureFl) return false
        if (this._optionalTpmsPressureFr != other._optionalTpmsPressureFr) return false
        if (this._optionalTpmsPressureRl != other._optionalTpmsPressureRl) return false
        if (this._optionalTpmsPressureRr != other._optionalTpmsPressureRr) return false
        if (this._tpmsLastSeenPressureTimeFl != other._tpmsLastSeenPressureTimeFl) return false
        if (this._tpmsLastSeenPressureTimeFr != other._tpmsLastSeenPressureTimeFr) return false
        if (this._tpmsLastSeenPressureTimeRl != other._tpmsLastSeenPressureTimeRl) return false
        if (this._tpmsLastSeenPressureTimeRr != other._tpmsLastSeenPressureTimeRr) return false
        if (this._optionalTpmsHardWarningFl != other._optionalTpmsHardWarningFl) return false
        if (this._optionalTpmsHardWarningFr != other._optionalTpmsHardWarningFr) return false
        if (this._optionalTpmsHardWarningRl != other._optionalTpmsHardWarningRl) return false
        if (this._optionalTpmsHardWarningRr != other._optionalTpmsHardWarningRr) return false
        if (this._optionalTpmsSoftWarningFl != other._optionalTpmsSoftWarningFl) return false
        if (this._optionalTpmsSoftWarningFr != other._optionalTpmsSoftWarningFr) return false
        if (this._optionalTpmsSoftWarningRl != other._optionalTpmsSoftWarningRl) return false
        if (this._optionalTpmsSoftWarningRr != other._optionalTpmsSoftWarningRr) return false
        if (this._optionalTpmsRcpFrontValue != other._optionalTpmsRcpFrontValue) return false
        if (this._optionalTpmsRcpRearValue != other._optionalTpmsRcpRearValue) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._timestamp?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalTpmsPressureFl?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalTpmsPressureFr?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalTpmsPressureRl?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalTpmsPressureRr?.hashCode() ?: 0)
        hash = 31 * hash + (this._tpmsLastSeenPressureTimeFl?.hashCode() ?: 0)
        hash = 31 * hash + (this._tpmsLastSeenPressureTimeFr?.hashCode() ?: 0)
        hash = 31 * hash + (this._tpmsLastSeenPressureTimeRl?.hashCode() ?: 0)
        hash = 31 * hash + (this._tpmsLastSeenPressureTimeRr?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalTpmsHardWarningFl?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalTpmsHardWarningFr?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalTpmsHardWarningRl?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalTpmsHardWarningRr?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalTpmsSoftWarningFl?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalTpmsSoftWarningFr?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalTpmsSoftWarningRl?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalTpmsSoftWarningRr?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalTpmsRcpFrontValue?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalTpmsRcpRearValue?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_TirePressureState {
        val result = CarServer_TirePressureState()
        result._timestamp = this._timestamp?.copy()
        result._optionalTpmsPressureFl = this._optionalTpmsPressureFl
        result._optionalTpmsPressureFr = this._optionalTpmsPressureFr
        result._optionalTpmsPressureRl = this._optionalTpmsPressureRl
        result._optionalTpmsPressureRr = this._optionalTpmsPressureRr
        result._tpmsLastSeenPressureTimeFl = this._tpmsLastSeenPressureTimeFl?.copy()
        result._tpmsLastSeenPressureTimeFr = this._tpmsLastSeenPressureTimeFr?.copy()
        result._tpmsLastSeenPressureTimeRl = this._tpmsLastSeenPressureTimeRl?.copy()
        result._tpmsLastSeenPressureTimeRr = this._tpmsLastSeenPressureTimeRr?.copy()
        result._optionalTpmsHardWarningFl = this._optionalTpmsHardWarningFl
        result._optionalTpmsHardWarningFr = this._optionalTpmsHardWarningFr
        result._optionalTpmsHardWarningRl = this._optionalTpmsHardWarningRl
        result._optionalTpmsHardWarningRr = this._optionalTpmsHardWarningRr
        result._optionalTpmsSoftWarningFl = this._optionalTpmsSoftWarningFl
        result._optionalTpmsSoftWarningFr = this._optionalTpmsSoftWarningFr
        result._optionalTpmsSoftWarningRl = this._optionalTpmsSoftWarningRl
        result._optionalTpmsSoftWarningRr = this._optionalTpmsSoftWarningRr
        result._optionalTpmsRcpFrontValue = this._optionalTpmsRcpFrontValue
        result._optionalTpmsRcpRearValue = this._optionalTpmsRcpRearValue
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.TirePressureState"

        fun with(block: CarServer_TirePressureState.() -> Unit): CarServer_TirePressureState =
            CarServer_TirePressureState().apply(block)
    }
}

class CarServer_MediaState() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var timestamp: Google_Protobuf_Timestamp
        get() = this._timestamp ?: _protobufPending(1, { Google_Protobuf_Timestamp() }) { this._timestamp = it }
        set(value) {
            _protobufDropPending(1)
            this._timestamp = value.copy()
            _protobufMutated()
        }

    val hasTimestamp: Boolean
        get() = this._timestamp != null

    fun clearTimestamp() {
        _protobufDropPending(1)
        this._timestamp = null
        _protobufMutated()
    }

    var optionalRemoteControlEnabled: CarServer_MediaState.OneOf_OptionalRemoteControlEnabled?
        get() = this._optionalRemoteControlEnabled
        set(value) {
            this._optionalRemoteControlEnabled = value
            _protobufMutated()
        }

    var remoteControlEnabled: Boolean
        get() = (this._optionalRemoteControlEnabled as? CarServer_MediaState.OneOf_OptionalRemoteControlEnabled.remoteControlEnabled)?.value ?: false
        set(value) {
            this.optionalRemoteControlEnabled = CarServer_MediaState.OneOf_OptionalRemoteControlEnabled.remoteControlEnabled(value)
        }

    var optionalNowPlayingArtist: CarServer_MediaState.OneOf_OptionalNowPlayingArtist?
        get() = this._optionalNowPlayingArtist
        set(value) {
            this._optionalNowPlayingArtist = value
            _protobufMutated()
        }

    var nowPlayingArtist: String
        get() = (this._optionalNowPlayingArtist as? CarServer_MediaState.OneOf_OptionalNowPlayingArtist.nowPlayingArtist)?.value ?: ""
        set(value) {
            this.optionalNowPlayingArtist = CarServer_MediaState.OneOf_OptionalNowPlayingArtist.nowPlayingArtist(value)
        }

    var optionalNowPlayingTitle: CarServer_MediaState.OneOf_OptionalNowPlayingTitle?
        get() = this._optionalNowPlayingTitle
        set(value) {
            this._optionalNowPlayingTitle = value
            _protobufMutated()
        }

    var nowPlayingTitle: String
        get() = (this._optionalNowPlayingTitle as? CarServer_MediaState.OneOf_OptionalNowPlayingTitle.nowPlayingTitle)?.value ?: ""
        set(value) {
            this.optionalNowPlayingTitle = CarServer_MediaState.OneOf_OptionalNowPlayingTitle.nowPlayingTitle(value)
        }

    var optionalAudioVolume: CarServer_MediaState.OneOf_OptionalAudioVolume?
        get() = this._optionalAudioVolume
        set(value) {
            this._optionalAudioVolume = value
            _protobufMutated()
        }

    var audioVolume: Float
        get() = (this._optionalAudioVolume as? CarServer_MediaState.OneOf_OptionalAudioVolume.audioVolume)?.value ?: 0f
        set(value) {
            this.optionalAudioVolume = CarServer_MediaState.OneOf_OptionalAudioVolume.audioVolume(value)
        }

    var optionalAudioVolumeIncrement: CarServer_MediaState.OneOf_OptionalAudioVolumeIncrement?
        get() = this._optionalAudioVolumeIncrement
        set(value) {
            this._optionalAudioVolumeIncrement = value
            _protobufMutated()
        }

    var audioVolumeIncrement: Float
        get() = (this._optionalAudioVolumeIncrement as? CarServer_MediaState.OneOf_OptionalAudioVolumeIncrement.audioVolumeIncrement)?.value ?: 0f
        set(value) {
            this.optionalAudioVolumeIncrement = CarServer_MediaState.OneOf_OptionalAudioVolumeIncrement.audioVolumeIncrement(value)
        }

    var optionalAudioVolumeMax: CarServer_MediaState.OneOf_OptionalAudioVolumeMax?
        get() = this._optionalAudioVolumeMax
        set(value) {
            this._optionalAudioVolumeMax = value
            _protobufMutated()
        }

    var audioVolumeMax: Float
        get() = (this._optionalAudioVolumeMax as? CarServer_MediaState.OneOf_OptionalAudioVolumeMax.audioVolumeMax)?.value ?: 0f
        set(value) {
            this.optionalAudioVolumeMax = CarServer_MediaState.OneOf_OptionalAudioVolumeMax.audioVolumeMax(value)
        }

    var optionalNowPlayingSource: CarServer_MediaState.OneOf_OptionalNowPlayingSource?
        get() = this._optionalNowPlayingSource
        set(value) {
            this._protobufStore_optionalNowPlayingSource(value)
            _protobufMutated()
        }

    var nowPlayingSource: CarServer_MediaSourceType
        get() = (this._optionalNowPlayingSource as? CarServer_MediaState.OneOf_OptionalNowPlayingSource.nowPlayingSource)?.value ?: CarServer_MediaSourceType.none
        set(value) {
            this.optionalNowPlayingSource = CarServer_MediaState.OneOf_OptionalNowPlayingSource.nowPlayingSource(value)
        }

    var optionalMediaPlaybackStatus: CarServer_MediaState.OneOf_OptionalMediaPlaybackStatus?
        get() = this._optionalMediaPlaybackStatus
        set(value) {
            this._protobufStore_optionalMediaPlaybackStatus(value)
            _protobufMutated()
        }

    var mediaPlaybackStatus: CarServer_MediaPlaybackStatus
        get() = (this._optionalMediaPlaybackStatus as? CarServer_MediaState.OneOf_OptionalMediaPlaybackStatus.mediaPlaybackStatus)?.value ?: CarServer_MediaPlaybackStatus.stopped
        set(value) {
            this.optionalMediaPlaybackStatus = CarServer_MediaState.OneOf_OptionalMediaPlaybackStatus.mediaPlaybackStatus(value)
        }

    sealed class OneOf_OptionalRemoteControlEnabled(value: Any) : ProtobufOneofCase(value) {
        class remoteControlEnabled(val value: Boolean) : OneOf_OptionalRemoteControlEnabled(value)
    }

    sealed class OneOf_OptionalNowPlayingArtist(value: Any) : ProtobufOneofCase(value) {
        class nowPlayingArtist(val value: String) : OneOf_OptionalNowPlayingArtist(value)
    }

    sealed class OneOf_OptionalNowPlayingTitle(value: Any) : ProtobufOneofCase(value) {
        class nowPlayingTitle(val value: String) : OneOf_OptionalNowPlayingTitle(value)
    }

    sealed class OneOf_OptionalAudioVolume(value: Any) : ProtobufOneofCase(value) {
        class audioVolume(val value: Float) : OneOf_OptionalAudioVolume(value)
    }

    sealed class OneOf_OptionalAudioVolumeIncrement(value: Any) : ProtobufOneofCase(value) {
        class audioVolumeIncrement(val value: Float) : OneOf_OptionalAudioVolumeIncrement(value)
    }

    sealed class OneOf_OptionalAudioVolumeMax(value: Any) : ProtobufOneofCase(value) {
        class audioVolumeMax(val value: Float) : OneOf_OptionalAudioVolumeMax(value)
    }

    sealed class OneOf_OptionalNowPlayingSource(value: Any) : ProtobufOneofCase(value) {
        class nowPlayingSource(val value: CarServer_MediaSourceType) : OneOf_OptionalNowPlayingSource(value)
    }

    sealed class OneOf_OptionalMediaPlaybackStatus(value: Any) : ProtobufOneofCase(value) {
        class mediaPlaybackStatus(val value: CarServer_MediaPlaybackStatus) : OneOf_OptionalMediaPlaybackStatus(value)
    }

    private var _timestamp: Google_Protobuf_Timestamp? = null
    private var _optionalRemoteControlEnabled: CarServer_MediaState.OneOf_OptionalRemoteControlEnabled? = null
    private var _optionalNowPlayingArtist: CarServer_MediaState.OneOf_OptionalNowPlayingArtist? = null
    private var _optionalNowPlayingTitle: CarServer_MediaState.OneOf_OptionalNowPlayingTitle? = null
    private var _optionalAudioVolume: CarServer_MediaState.OneOf_OptionalAudioVolume? = null
    private var _optionalAudioVolumeIncrement: CarServer_MediaState.OneOf_OptionalAudioVolumeIncrement? = null
    private var _optionalAudioVolumeMax: CarServer_MediaState.OneOf_OptionalAudioVolumeMax? = null
    private var _optionalNowPlayingSource: CarServer_MediaState.OneOf_OptionalNowPlayingSource? = null
    private var _optionalMediaPlaybackStatus: CarServer_MediaState.OneOf_OptionalMediaPlaybackStatus? = null

    private fun _protobufStore_optionalNowPlayingSource(value: CarServer_MediaState.OneOf_OptionalNowPlayingSource?) {
        _protobufForgetUnrecognized(8)
        this._optionalNowPlayingSource = value
    }

    private fun _protobufStore_optionalMediaPlaybackStatus(value: CarServer_MediaState.OneOf_OptionalMediaPlaybackStatus?) {
        _protobufForgetUnrecognized(9)
        this._optionalMediaPlaybackStatus = value
    }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularMessageField(this._timestamp) { Google_Protobuf_Timestamp() }?.let {
                    _protobufDropPending(1)
                    this._timestamp = it
                }
                2 -> decoder.decodeSingularBoolField()?.let { this._optionalRemoteControlEnabled = CarServer_MediaState.OneOf_OptionalRemoteControlEnabled.remoteControlEnabled(it) }
                3 -> decoder.decodeSingularStringField()?.let { this._optionalNowPlayingArtist = CarServer_MediaState.OneOf_OptionalNowPlayingArtist.nowPlayingArtist(it) }
                4 -> decoder.decodeSingularStringField()?.let { this._optionalNowPlayingTitle = CarServer_MediaState.OneOf_OptionalNowPlayingTitle.nowPlayingTitle(it) }
                5 -> decoder.decodeSingularFloatField()?.let { this._optionalAudioVolume = CarServer_MediaState.OneOf_OptionalAudioVolume.audioVolume(it) }
                6 -> decoder.decodeSingularFloatField()?.let { this._optionalAudioVolumeIncrement = CarServer_MediaState.OneOf_OptionalAudioVolumeIncrement.audioVolumeIncrement(it) }
                7 -> decoder.decodeSingularFloatField()?.let { this._optionalAudioVolumeMax = CarServer_MediaState.OneOf_OptionalAudioVolumeMax.audioVolumeMax(it) }
                8 -> decoder.decodeSingularOpenEnumField(CarServer_MediaSourceType.UNRECOGNIZED) { CarServer_MediaSourceType.fromRawValue(it) }
                    ?.let { this._protobufStore_optionalNowPlayingSource(CarServer_MediaState.OneOf_OptionalNowPlayingSource.nowPlayingSource(it)) }
                9 -> decoder.decodeSingularOpenEnumField(CarServer_MediaPlaybackStatus.UNRECOGNIZED) { CarServer_MediaPlaybackStatus.fromRawValue(it) }
                    ?.let { this._protobufStore_optionalMediaPlaybackStatus(CarServer_MediaState.OneOf_OptionalMediaPlaybackStatus.mediaPlaybackStatus(it)) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        this._timestamp?.let {
            visitor.visitSingularMessageField(it, 1)
        }
        (this._optionalRemoteControlEnabled as? CarServer_MediaState.OneOf_OptionalRemoteControlEnabled.remoteControlEnabled)?.let {
            visitor.visitSingularBoolField(it.value, 2)
        }
        (this._optionalNowPlayingArtist as? CarServer_MediaState.OneOf_OptionalNowPlayingArtist.nowPlayingArtist)?.let {
            visitor.visitSingularStringField(it.value, 3)
        }
        (this._optionalNowPlayingTitle as? CarServer_MediaState.OneOf_OptionalNowPlayingTitle.nowPlayingTitle)?.let {
            visitor.visitSingularStringField(it.value, 4)
        }
        (this._optionalAudioVolume as? CarServer_MediaState.OneOf_OptionalAudioVolume.audioVolume)?.let {
            visitor.visitSingularFloatField(it.value, 5)
        }
        (this._optionalAudioVolumeIncrement as? CarServer_MediaState.OneOf_OptionalAudioVolumeIncrement.audioVolumeIncrement)?.let {
            visitor.visitSingularFloatField(it.value, 6)
        }
        (this._optionalAudioVolumeMax as? CarServer_MediaState.OneOf_OptionalAudioVolumeMax.audioVolumeMax)?.let {
            visitor.visitSingularFloatField(it.value, 7)
        }
        (this._optionalNowPlayingSource as? CarServer_MediaState.OneOf_OptionalNowPlayingSource.nowPlayingSource)?.let {
            if (it.value != CarServer_MediaSourceType.UNRECOGNIZED) {
                visitor.visitSingularEnumField(it.value.rawValue, 8)
            }
        }
        (this._optionalMediaPlaybackStatus as? CarServer_MediaState.OneOf_OptionalMediaPlaybackStatus.mediaPlaybackStatus)?.let {
            if (it.value != CarServer_MediaPlaybackStatus.UNRECOGNIZED) {
                visitor.visitSingularEnumField(it.value.rawValue, 9)
            }
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_MediaState) return false
        if (this._timestamp != other._timestamp) return false
        if (this._optionalRemoteControlEnabled != other._optionalRemoteControlEnabled) return false
        if (this._optionalNowPlayingArtist != other._optionalNowPlayingArtist) return false
        if (this._optionalNowPlayingTitle != other._optionalNowPlayingTitle) return false
        if (this._optionalAudioVolume != other._optionalAudioVolume) return false
        if (this._optionalAudioVolumeIncrement != other._optionalAudioVolumeIncrement) return false
        if (this._optionalAudioVolumeMax != other._optionalAudioVolumeMax) return false
        if (this._optionalNowPlayingSource != other._optionalNowPlayingSource) return false
        if (this._optionalMediaPlaybackStatus != other._optionalMediaPlaybackStatus) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._timestamp?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalRemoteControlEnabled?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalNowPlayingArtist?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalNowPlayingTitle?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalAudioVolume?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalAudioVolumeIncrement?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalAudioVolumeMax?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalNowPlayingSource?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalMediaPlaybackStatus?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_MediaState {
        val result = CarServer_MediaState()
        result._timestamp = this._timestamp?.copy()
        result._optionalRemoteControlEnabled = this._optionalRemoteControlEnabled
        result._optionalNowPlayingArtist = this._optionalNowPlayingArtist
        result._optionalNowPlayingTitle = this._optionalNowPlayingTitle
        result._optionalAudioVolume = this._optionalAudioVolume
        result._optionalAudioVolumeIncrement = this._optionalAudioVolumeIncrement
        result._optionalAudioVolumeMax = this._optionalAudioVolumeMax
        result._optionalNowPlayingSource = this._optionalNowPlayingSource
        result._optionalMediaPlaybackStatus = this._optionalMediaPlaybackStatus
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.MediaState"

        fun with(block: CarServer_MediaState.() -> Unit): CarServer_MediaState =
            CarServer_MediaState().apply(block)
    }
}

class CarServer_MediaDetailState() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var timestamp: Google_Protobuf_Timestamp
        get() = this._timestamp ?: _protobufPending(1, { Google_Protobuf_Timestamp() }) { this._timestamp = it }
        set(value) {
            _protobufDropPending(1)
            this._timestamp = value.copy()
            _protobufMutated()
        }

    val hasTimestamp: Boolean
        get() = this._timestamp != null

    fun clearTimestamp() {
        _protobufDropPending(1)
        this._timestamp = null
        _protobufMutated()
    }

    var optionalNowPlayingDuration: CarServer_MediaDetailState.OneOf_OptionalNowPlayingDuration?
        get() = this._optionalNowPlayingDuration
        set(value) {
            this._optionalNowPlayingDuration = value
            _protobufMutated()
        }

    var nowPlayingDuration: Int
        get() = (this._optionalNowPlayingDuration as? CarServer_MediaDetailState.OneOf_OptionalNowPlayingDuration.nowPlayingDuration)?.value ?: 0
        set(value) {
            this.optionalNowPlayingDuration = CarServer_MediaDetailState.OneOf_OptionalNowPlayingDuration.nowPlayingDuration(value)
        }

    var optionalNowPlayingElapsed: CarServer_MediaDetailState.OneOf_OptionalNowPlayingElapsed?
        get() = this._optionalNowPlayingElapsed
        set(value) {
            this._optionalNowPlayingElapsed = value
            _protobufMutated()
        }

    var nowPlayingElapsed: Int
        get() = (this._optionalNowPlayingElapsed as? CarServer_MediaDetailState.OneOf_OptionalNowPlayingElapsed.nowPlayingElapsed)?.value ?: 0
        set(value) {
            this.optionalNowPlayingElapsed = CarServer_MediaDetailState.OneOf_OptionalNowPlayingElapsed.nowPlayingElapsed(value)
        }

    var optionalNowPlayingSourceString: CarServer_MediaDetailState.OneOf_OptionalNowPlayingSourceString?
        get() = this._optionalNowPlayingSourceString
        set(value) {
            this._optionalNowPlayingSourceString = value
            _protobufMutated()
        }

    var nowPlayingSourceString: String
        get() = (this._optionalNowPlayingSourceString as? CarServer_MediaDetailState.OneOf_OptionalNowPlayingSourceString.nowPlayingSourceString)?.value ?: ""
        set(value) {
            this.optionalNowPlayingSourceString = CarServer_MediaDetailState.OneOf_OptionalNowPlayingSourceString.nowPlayingSourceString(value)
        }

    var optionalNowPlayingAlbum: CarServer_MediaDetailState.OneOf_OptionalNowPlayingAlbum?
        get() = this._optionalNowPlayingAlbum
        set(value) {
            this._optionalNowPlayingAlbum = value
            _protobufMutated()
        }

    var nowPlayingAlbum: String
        get() = (this._optionalNowPlayingAlbum as? CarServer_MediaDetailState.OneOf_OptionalNowPlayingAlbum.nowPlayingAlbum)?.value ?: ""
        set(value) {
            this.optionalNowPlayingAlbum = CarServer_MediaDetailState.OneOf_OptionalNowPlayingAlbum.nowPlayingAlbum(value)
        }

    var optionalNowPlayingStation: CarServer_MediaDetailState.OneOf_OptionalNowPlayingStation?
        get() = this._optionalNowPlayingStation
        set(value) {
            this._optionalNowPlayingStation = value
            _protobufMutated()
        }

    var nowPlayingStation: String
        get() = (this._optionalNowPlayingStation as? CarServer_MediaDetailState.OneOf_OptionalNowPlayingStation.nowPlayingStation)?.value ?: ""
        set(value) {
            this.optionalNowPlayingStation = CarServer_MediaDetailState.OneOf_OptionalNowPlayingStation.nowPlayingStation(value)
        }

    var optionalA2DpSourceName: CarServer_MediaDetailState.OneOf_OptionalA2DpSourceName?
        get() = this._optionalA2DpSourceName
        set(value) {
            this._optionalA2DpSourceName = value
            _protobufMutated()
        }

    var a2DpSourceName: String
        get() = (this._optionalA2DpSourceName as? CarServer_MediaDetailState.OneOf_OptionalA2DpSourceName.a2DpSourceName)?.value ?: ""
        set(value) {
            this.optionalA2DpSourceName = CarServer_MediaDetailState.OneOf_OptionalA2DpSourceName.a2DpSourceName(value)
        }

    sealed class OneOf_OptionalNowPlayingDuration(value: Any) : ProtobufOneofCase(value) {
        class nowPlayingDuration(val value: Int) : OneOf_OptionalNowPlayingDuration(value)
    }

    sealed class OneOf_OptionalNowPlayingElapsed(value: Any) : ProtobufOneofCase(value) {
        class nowPlayingElapsed(val value: Int) : OneOf_OptionalNowPlayingElapsed(value)
    }

    sealed class OneOf_OptionalNowPlayingSourceString(value: Any) : ProtobufOneofCase(value) {
        class nowPlayingSourceString(val value: String) : OneOf_OptionalNowPlayingSourceString(value)
    }

    sealed class OneOf_OptionalNowPlayingAlbum(value: Any) : ProtobufOneofCase(value) {
        class nowPlayingAlbum(val value: String) : OneOf_OptionalNowPlayingAlbum(value)
    }

    sealed class OneOf_OptionalNowPlayingStation(value: Any) : ProtobufOneofCase(value) {
        class nowPlayingStation(val value: String) : OneOf_OptionalNowPlayingStation(value)
    }

    sealed class OneOf_OptionalA2DpSourceName(value: Any) : ProtobufOneofCase(value) {
        class a2DpSourceName(val value: String) : OneOf_OptionalA2DpSourceName(value)
    }

    private var _timestamp: Google_Protobuf_Timestamp? = null
    private var _optionalNowPlayingDuration: CarServer_MediaDetailState.OneOf_OptionalNowPlayingDuration? = null
    private var _optionalNowPlayingElapsed: CarServer_MediaDetailState.OneOf_OptionalNowPlayingElapsed? = null
    private var _optionalNowPlayingSourceString: CarServer_MediaDetailState.OneOf_OptionalNowPlayingSourceString? = null
    private var _optionalNowPlayingAlbum: CarServer_MediaDetailState.OneOf_OptionalNowPlayingAlbum? = null
    private var _optionalNowPlayingStation: CarServer_MediaDetailState.OneOf_OptionalNowPlayingStation? = null
    private var _optionalA2DpSourceName: CarServer_MediaDetailState.OneOf_OptionalA2DpSourceName? = null

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularMessageField(this._timestamp) { Google_Protobuf_Timestamp() }?.let {
                    _protobufDropPending(1)
                    this._timestamp = it
                }
                2 -> decoder.decodeSingularInt32Field()?.let { this._optionalNowPlayingDuration = CarServer_MediaDetailState.OneOf_OptionalNowPlayingDuration.nowPlayingDuration(it) }
                3 -> decoder.decodeSingularInt32Field()?.let { this._optionalNowPlayingElapsed = CarServer_MediaDetailState.OneOf_OptionalNowPlayingElapsed.nowPlayingElapsed(it) }
                4 -> decoder.decodeSingularStringField()?.let { this._optionalNowPlayingSourceString = CarServer_MediaDetailState.OneOf_OptionalNowPlayingSourceString.nowPlayingSourceString(it) }
                5 -> decoder.decodeSingularStringField()?.let { this._optionalNowPlayingAlbum = CarServer_MediaDetailState.OneOf_OptionalNowPlayingAlbum.nowPlayingAlbum(it) }
                6 -> decoder.decodeSingularStringField()?.let { this._optionalNowPlayingStation = CarServer_MediaDetailState.OneOf_OptionalNowPlayingStation.nowPlayingStation(it) }
                7 -> decoder.decodeSingularStringField()?.let { this._optionalA2DpSourceName = CarServer_MediaDetailState.OneOf_OptionalA2DpSourceName.a2DpSourceName(it) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        this._timestamp?.let {
            visitor.visitSingularMessageField(it, 1)
        }
        (this._optionalNowPlayingDuration as? CarServer_MediaDetailState.OneOf_OptionalNowPlayingDuration.nowPlayingDuration)?.let {
            visitor.visitSingularInt32Field(it.value, 2)
        }
        (this._optionalNowPlayingElapsed as? CarServer_MediaDetailState.OneOf_OptionalNowPlayingElapsed.nowPlayingElapsed)?.let {
            visitor.visitSingularInt32Field(it.value, 3)
        }
        (this._optionalNowPlayingSourceString as? CarServer_MediaDetailState.OneOf_OptionalNowPlayingSourceString.nowPlayingSourceString)?.let {
            visitor.visitSingularStringField(it.value, 4)
        }
        (this._optionalNowPlayingAlbum as? CarServer_MediaDetailState.OneOf_OptionalNowPlayingAlbum.nowPlayingAlbum)?.let {
            visitor.visitSingularStringField(it.value, 5)
        }
        (this._optionalNowPlayingStation as? CarServer_MediaDetailState.OneOf_OptionalNowPlayingStation.nowPlayingStation)?.let {
            visitor.visitSingularStringField(it.value, 6)
        }
        (this._optionalA2DpSourceName as? CarServer_MediaDetailState.OneOf_OptionalA2DpSourceName.a2DpSourceName)?.let {
            visitor.visitSingularStringField(it.value, 7)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_MediaDetailState) return false
        if (this._timestamp != other._timestamp) return false
        if (this._optionalNowPlayingDuration != other._optionalNowPlayingDuration) return false
        if (this._optionalNowPlayingElapsed != other._optionalNowPlayingElapsed) return false
        if (this._optionalNowPlayingSourceString != other._optionalNowPlayingSourceString) return false
        if (this._optionalNowPlayingAlbum != other._optionalNowPlayingAlbum) return false
        if (this._optionalNowPlayingStation != other._optionalNowPlayingStation) return false
        if (this._optionalA2DpSourceName != other._optionalA2DpSourceName) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._timestamp?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalNowPlayingDuration?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalNowPlayingElapsed?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalNowPlayingSourceString?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalNowPlayingAlbum?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalNowPlayingStation?.hashCode() ?: 0)
        hash = 31 * hash + (this._optionalA2DpSourceName?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_MediaDetailState {
        val result = CarServer_MediaDetailState()
        result._timestamp = this._timestamp?.copy()
        result._optionalNowPlayingDuration = this._optionalNowPlayingDuration
        result._optionalNowPlayingElapsed = this._optionalNowPlayingElapsed
        result._optionalNowPlayingSourceString = this._optionalNowPlayingSourceString
        result._optionalNowPlayingAlbum = this._optionalNowPlayingAlbum
        result._optionalNowPlayingStation = this._optionalNowPlayingStation
        result._optionalA2DpSourceName = this._optionalA2DpSourceName
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.MediaDetailState"

        fun with(block: CarServer_MediaDetailState.() -> Unit): CarServer_MediaDetailState =
            CarServer_MediaDetailState().apply(block)
    }
}

class CarServer_ShiftState() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var type: CarServer_ShiftState.OneOf_Type?
        get() = this._type
        set(value) {
            this._protobufStore_type(this._protobufCopy_type(value))
            _protobufMutated()
        }

    var invalid: CarServer_Void
        get() = (this._type as? CarServer_ShiftState.OneOf_Type.invalid)?.value
            ?: _protobufPending(1, { CarServer_Void() }) { this._protobufStore_type(CarServer_ShiftState.OneOf_Type.invalid(it)) }
        set(value) {
            this.type = CarServer_ShiftState.OneOf_Type.invalid(value)
        }

    var p: CarServer_Void
        get() = (this._type as? CarServer_ShiftState.OneOf_Type.p)?.value
            ?: _protobufPending(2, { CarServer_Void() }) { this._protobufStore_type(CarServer_ShiftState.OneOf_Type.p(it)) }
        set(value) {
            this.type = CarServer_ShiftState.OneOf_Type.p(value)
        }

    var r: CarServer_Void
        get() = (this._type as? CarServer_ShiftState.OneOf_Type.r)?.value
            ?: _protobufPending(3, { CarServer_Void() }) { this._protobufStore_type(CarServer_ShiftState.OneOf_Type.r(it)) }
        set(value) {
            this.type = CarServer_ShiftState.OneOf_Type.r(value)
        }

    var n: CarServer_Void
        get() = (this._type as? CarServer_ShiftState.OneOf_Type.n)?.value
            ?: _protobufPending(4, { CarServer_Void() }) { this._protobufStore_type(CarServer_ShiftState.OneOf_Type.n(it)) }
        set(value) {
            this.type = CarServer_ShiftState.OneOf_Type.n(value)
        }

    var d: CarServer_Void
        get() = (this._type as? CarServer_ShiftState.OneOf_Type.d)?.value
            ?: _protobufPending(5, { CarServer_Void() }) { this._protobufStore_type(CarServer_ShiftState.OneOf_Type.d(it)) }
        set(value) {
            this.type = CarServer_ShiftState.OneOf_Type.d(value)
        }

    var sna: CarServer_Void
        get() = (this._type as? CarServer_ShiftState.OneOf_Type.sna)?.value
            ?: _protobufPending(6, { CarServer_Void() }) { this._protobufStore_type(CarServer_ShiftState.OneOf_Type.sna(it)) }
        set(value) {
            this.type = CarServer_ShiftState.OneOf_Type.sna(value)
        }

    sealed class OneOf_Type(value: Any) : ProtobufOneofCase(value) {
        class invalid(val value: CarServer_Void) : OneOf_Type(value)
        class p(val value: CarServer_Void) : OneOf_Type(value)
        class r(val value: CarServer_Void) : OneOf_Type(value)
        class n(val value: CarServer_Void) : OneOf_Type(value)
        class d(val value: CarServer_Void) : OneOf_Type(value)
        class sna(val value: CarServer_Void) : OneOf_Type(value)
    }

    private var _type: CarServer_ShiftState.OneOf_Type? = null

    private fun _protobufStore_type(value: CarServer_ShiftState.OneOf_Type?) {
        _protobufDropPending(1)
        _protobufDropPending(2)
        _protobufDropPending(3)
        _protobufDropPending(4)
        _protobufDropPending(5)
        _protobufDropPending(6)
        this._type = value
    }

    private fun _protobufCopy_type(value: CarServer_ShiftState.OneOf_Type?): CarServer_ShiftState.OneOf_Type? =
        when (value) {
            is CarServer_ShiftState.OneOf_Type.invalid -> CarServer_ShiftState.OneOf_Type.invalid(value.value.copy())
            is CarServer_ShiftState.OneOf_Type.p -> CarServer_ShiftState.OneOf_Type.p(value.value.copy())
            is CarServer_ShiftState.OneOf_Type.r -> CarServer_ShiftState.OneOf_Type.r(value.value.copy())
            is CarServer_ShiftState.OneOf_Type.n -> CarServer_ShiftState.OneOf_Type.n(value.value.copy())
            is CarServer_ShiftState.OneOf_Type.d -> CarServer_ShiftState.OneOf_Type.d(value.value.copy())
            is CarServer_ShiftState.OneOf_Type.sna -> CarServer_ShiftState.OneOf_Type.sna(value.value.copy())
            else -> value
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularMessageField((this._type as? CarServer_ShiftState.OneOf_Type.invalid)?.value) { CarServer_Void() }
                    ?.let { this._protobufStore_type(CarServer_ShiftState.OneOf_Type.invalid(it)) }
                2 -> decoder.decodeSingularMessageField((this._type as? CarServer_ShiftState.OneOf_Type.p)?.value) { CarServer_Void() }
                    ?.let { this._protobufStore_type(CarServer_ShiftState.OneOf_Type.p(it)) }
                3 -> decoder.decodeSingularMessageField((this._type as? CarServer_ShiftState.OneOf_Type.r)?.value) { CarServer_Void() }
                    ?.let { this._protobufStore_type(CarServer_ShiftState.OneOf_Type.r(it)) }
                4 -> decoder.decodeSingularMessageField((this._type as? CarServer_ShiftState.OneOf_Type.n)?.value) { CarServer_Void() }
                    ?.let { this._protobufStore_type(CarServer_ShiftState.OneOf_Type.n(it)) }
                5 -> decoder.decodeSingularMessageField((this._type as? CarServer_ShiftState.OneOf_Type.d)?.value) { CarServer_Void() }
                    ?.let { this._protobufStore_type(CarServer_ShiftState.OneOf_Type.d(it)) }
                6 -> decoder.decodeSingularMessageField((this._type as? CarServer_ShiftState.OneOf_Type.sna)?.value) { CarServer_Void() }
                    ?.let { this._protobufStore_type(CarServer_ShiftState.OneOf_Type.sna(it)) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        (this._type as? CarServer_ShiftState.OneOf_Type.invalid)?.let {
            visitor.visitSingularMessageField(it.value, 1)
        }
        (this._type as? CarServer_ShiftState.OneOf_Type.p)?.let {
            visitor.visitSingularMessageField(it.value, 2)
        }
        (this._type as? CarServer_ShiftState.OneOf_Type.r)?.let {
            visitor.visitSingularMessageField(it.value, 3)
        }
        (this._type as? CarServer_ShiftState.OneOf_Type.n)?.let {
            visitor.visitSingularMessageField(it.value, 4)
        }
        (this._type as? CarServer_ShiftState.OneOf_Type.d)?.let {
            visitor.visitSingularMessageField(it.value, 5)
        }
        (this._type as? CarServer_ShiftState.OneOf_Type.sna)?.let {
            visitor.visitSingularMessageField(it.value, 6)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_ShiftState) return false
        if (this._type != other._type) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._type?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ShiftState {
        val result = CarServer_ShiftState()
        result._type = this._protobufCopy_type(this._type)
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ShiftState"

        fun with(block: CarServer_ShiftState.() -> Unit): CarServer_ShiftState =
            CarServer_ShiftState().apply(block)
    }
}
