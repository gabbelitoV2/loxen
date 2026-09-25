package com.moblin.android.integrations.tesla.protobuf

import com.moblin.android.platform.swiftprotobuf.BinaryDecoder
import com.moblin.android.platform.swiftprotobuf.BinaryEncodingVisitor
import com.moblin.android.platform.swiftprotobuf.GeneratedMessage
import com.moblin.android.platform.swiftprotobuf.Google_Protobuf_Timestamp
import com.moblin.android.platform.swiftprotobuf.ProtobufList
import com.moblin.android.platform.swiftprotobuf.ProtobufOneofCase
import com.moblin.android.platform.swiftprotobuf.merge
import com.moblin.android.platform.swiftprotobuf.protobufHash

enum class CarServer_OperationStatus_E(val rawValue: Int) {
    operationstatusOk(0),
    rror(1),
    UNRECOGNIZED(-1),
    ;

    companion object {
        val allCases: List<CarServer_OperationStatus_E> = listOf(
            operationstatusOk,
            rror,
        )

        fun fromRawValue(rawValue: Int): CarServer_OperationStatus_E =
            when (rawValue) {
                0 -> operationstatusOk
                1 -> rror
                else -> UNRECOGNIZED
            }
    }
}

class CarServer_Action() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var actionMsg: CarServer_Action.OneOf_ActionMsg?
        get() = this._actionMsg
        set(value) {
            this._protobufStore_actionMsg(this._protobufCopy_actionMsg(value))
            _protobufMutated()
        }

    var vehicleAction: CarServer_VehicleAction
        get() = (this._actionMsg as? CarServer_Action.OneOf_ActionMsg.vehicleAction)?.value
            ?: _protobufPending(2, { CarServer_VehicleAction() }) { this._protobufStore_actionMsg(CarServer_Action.OneOf_ActionMsg.vehicleAction(it)) }
        set(value) {
            this.actionMsg = CarServer_Action.OneOf_ActionMsg.vehicleAction(value)
        }

    sealed class OneOf_ActionMsg(value: Any) : ProtobufOneofCase(value) {
        class vehicleAction(val value: CarServer_VehicleAction) : OneOf_ActionMsg(value)
    }

    private var _actionMsg: CarServer_Action.OneOf_ActionMsg? = null

    private fun _protobufStore_actionMsg(value: CarServer_Action.OneOf_ActionMsg?) {
        _protobufDropPending(2)
        this._actionMsg = value
    }

    private fun _protobufCopy_actionMsg(value: CarServer_Action.OneOf_ActionMsg?): CarServer_Action.OneOf_ActionMsg? =
        when (value) {
            is CarServer_Action.OneOf_ActionMsg.vehicleAction -> CarServer_Action.OneOf_ActionMsg.vehicleAction(value.value.copy())
            else -> value
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                2 -> decoder.decodeSingularMessageField((this._actionMsg as? CarServer_Action.OneOf_ActionMsg.vehicleAction)?.value) { CarServer_VehicleAction() }
                    ?.let { this._protobufStore_actionMsg(CarServer_Action.OneOf_ActionMsg.vehicleAction(it)) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        (this._actionMsg as? CarServer_Action.OneOf_ActionMsg.vehicleAction)?.let {
            visitor.visitSingularMessageField(it.value, 2)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_Action) return false
        if (this._actionMsg != other._actionMsg) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._actionMsg?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_Action {
        val result = CarServer_Action()
        result._actionMsg = this._protobufCopy_actionMsg(this._actionMsg)
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.Action"

        fun with(block: CarServer_Action.() -> Unit): CarServer_Action =
            CarServer_Action().apply(block)
    }
}

class CarServer_VehicleAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var vehicleActionMsg: CarServer_VehicleAction.OneOf_VehicleActionMsg?
        get() = this._vehicleActionMsg
        set(value) {
            this._protobufStore_vehicleActionMsg(this._protobufCopy_vehicleActionMsg(value))
            _protobufMutated()
        }

    var getVehicleData: CarServer_GetVehicleData
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.getVehicleData)?.value
            ?: _protobufPending(1, { CarServer_GetVehicleData() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.getVehicleData(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.getVehicleData(value)
        }

    var chargingSetLimitAction: CarServer_ChargingSetLimitAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.chargingSetLimitAction)?.value
            ?: _protobufPending(5, { CarServer_ChargingSetLimitAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.chargingSetLimitAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.chargingSetLimitAction(value)
        }

    var chargingStartStopAction: CarServer_ChargingStartStopAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.chargingStartStopAction)?.value
            ?: _protobufPending(6, { CarServer_ChargingStartStopAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.chargingStartStopAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.chargingStartStopAction(value)
        }

    var drivingClearSpeedLimitPinAction: CarServer_DrivingClearSpeedLimitPinAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingClearSpeedLimitPinAction)?.value
            ?: _protobufPending(7, { CarServer_DrivingClearSpeedLimitPinAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingClearSpeedLimitPinAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingClearSpeedLimitPinAction(value)
        }

    var drivingSetSpeedLimitAction: CarServer_DrivingSetSpeedLimitAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingSetSpeedLimitAction)?.value
            ?: _protobufPending(8, { CarServer_DrivingSetSpeedLimitAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingSetSpeedLimitAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingSetSpeedLimitAction(value)
        }

    var drivingSpeedLimitAction: CarServer_DrivingSpeedLimitAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingSpeedLimitAction)?.value
            ?: _protobufPending(9, { CarServer_DrivingSpeedLimitAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingSpeedLimitAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingSpeedLimitAction(value)
        }

    var hvacAutoAction: CarServer_HvacAutoAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacAutoAction)?.value
            ?: _protobufPending(10, { CarServer_HvacAutoAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacAutoAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacAutoAction(value)
        }

    var hvacSetPreconditioningMaxAction: CarServer_HvacSetPreconditioningMaxAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSetPreconditioningMaxAction)?.value
            ?: _protobufPending(12, { CarServer_HvacSetPreconditioningMaxAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSetPreconditioningMaxAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSetPreconditioningMaxAction(value)
        }

    var hvacSteeringWheelHeaterAction: CarServer_HvacSteeringWheelHeaterAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSteeringWheelHeaterAction)?.value
            ?: _protobufPending(13, { CarServer_HvacSteeringWheelHeaterAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSteeringWheelHeaterAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSteeringWheelHeaterAction(value)
        }

    var hvacTemperatureAdjustmentAction: CarServer_HvacTemperatureAdjustmentAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacTemperatureAdjustmentAction)?.value
            ?: _protobufPending(14, { CarServer_HvacTemperatureAdjustmentAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacTemperatureAdjustmentAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacTemperatureAdjustmentAction(value)
        }

    var mediaPlayAction: CarServer_MediaPlayAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaPlayAction)?.value
            ?: _protobufPending(15, { CarServer_MediaPlayAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaPlayAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaPlayAction(value)
        }

    var mediaUpdateVolume: CarServer_MediaUpdateVolume
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaUpdateVolume)?.value
            ?: _protobufPending(16, { CarServer_MediaUpdateVolume() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaUpdateVolume(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaUpdateVolume(value)
        }

    var mediaNextFavorite: CarServer_MediaNextFavorite
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaNextFavorite)?.value
            ?: _protobufPending(17, { CarServer_MediaNextFavorite() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaNextFavorite(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaNextFavorite(value)
        }

    var mediaPreviousFavorite: CarServer_MediaPreviousFavorite
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaPreviousFavorite)?.value
            ?: _protobufPending(18, { CarServer_MediaPreviousFavorite() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaPreviousFavorite(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaPreviousFavorite(value)
        }

    var mediaNextTrack: CarServer_MediaNextTrack
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaNextTrack)?.value
            ?: _protobufPending(19, { CarServer_MediaNextTrack() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaNextTrack(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaNextTrack(value)
        }

    var mediaPreviousTrack: CarServer_MediaPreviousTrack
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaPreviousTrack)?.value
            ?: _protobufPending(20, { CarServer_MediaPreviousTrack() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaPreviousTrack(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaPreviousTrack(value)
        }

    var getNearbyChargingSites: CarServer_GetNearbyChargingSites
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.getNearbyChargingSites)?.value
            ?: _protobufPending(23, { CarServer_GetNearbyChargingSites() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.getNearbyChargingSites(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.getNearbyChargingSites(value)
        }

    var vehicleControlCancelSoftwareUpdateAction: CarServer_VehicleControlCancelSoftwareUpdateAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlCancelSoftwareUpdateAction)?.value
            ?: _protobufPending(25, { CarServer_VehicleControlCancelSoftwareUpdateAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlCancelSoftwareUpdateAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlCancelSoftwareUpdateAction(value)
        }

    var vehicleControlFlashLightsAction: CarServer_VehicleControlFlashLightsAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlFlashLightsAction)?.value
            ?: _protobufPending(26, { CarServer_VehicleControlFlashLightsAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlFlashLightsAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlFlashLightsAction(value)
        }

    var vehicleControlHonkHornAction: CarServer_VehicleControlHonkHornAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlHonkHornAction)?.value
            ?: _protobufPending(27, { CarServer_VehicleControlHonkHornAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlHonkHornAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlHonkHornAction(value)
        }

    var vehicleControlResetValetPinAction: CarServer_VehicleControlResetValetPinAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlResetValetPinAction)?.value
            ?: _protobufPending(28, { CarServer_VehicleControlResetValetPinAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlResetValetPinAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlResetValetPinAction(value)
        }

    var vehicleControlScheduleSoftwareUpdateAction: CarServer_VehicleControlScheduleSoftwareUpdateAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlScheduleSoftwareUpdateAction)?.value
            ?: _protobufPending(29, { CarServer_VehicleControlScheduleSoftwareUpdateAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlScheduleSoftwareUpdateAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlScheduleSoftwareUpdateAction(value)
        }

    var vehicleControlSetSentryModeAction: CarServer_VehicleControlSetSentryModeAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSetSentryModeAction)?.value
            ?: _protobufPending(30, { CarServer_VehicleControlSetSentryModeAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSetSentryModeAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSetSentryModeAction(value)
        }

    var vehicleControlSetValetModeAction: CarServer_VehicleControlSetValetModeAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSetValetModeAction)?.value
            ?: _protobufPending(31, { CarServer_VehicleControlSetValetModeAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSetValetModeAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSetValetModeAction(value)
        }

    var vehicleControlSunroofOpenCloseAction: CarServer_VehicleControlSunroofOpenCloseAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSunroofOpenCloseAction)?.value
            ?: _protobufPending(32, { CarServer_VehicleControlSunroofOpenCloseAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSunroofOpenCloseAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSunroofOpenCloseAction(value)
        }

    var vehicleControlTriggerHomelinkAction: CarServer_VehicleControlTriggerHomelinkAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlTriggerHomelinkAction)?.value
            ?: _protobufPending(33, { CarServer_VehicleControlTriggerHomelinkAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlTriggerHomelinkAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlTriggerHomelinkAction(value)
        }

    var vehicleControlWindowAction: CarServer_VehicleControlWindowAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlWindowAction)?.value
            ?: _protobufPending(34, { CarServer_VehicleControlWindowAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlWindowAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlWindowAction(value)
        }

    var hvacBioweaponModeAction: CarServer_HvacBioweaponModeAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacBioweaponModeAction)?.value
            ?: _protobufPending(35, { CarServer_HvacBioweaponModeAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacBioweaponModeAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacBioweaponModeAction(value)
        }

    var hvacSeatHeaterActions: CarServer_HvacSeatHeaterActions
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSeatHeaterActions)?.value
            ?: _protobufPending(36, { CarServer_HvacSeatHeaterActions() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSeatHeaterActions(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSeatHeaterActions(value)
        }

    var scheduledChargingAction: CarServer_ScheduledChargingAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.scheduledChargingAction)?.value
            ?: _protobufPending(41, { CarServer_ScheduledChargingAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.scheduledChargingAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.scheduledChargingAction(value)
        }

    var scheduledDepartureAction: CarServer_ScheduledDepartureAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.scheduledDepartureAction)?.value
            ?: _protobufPending(42, { CarServer_ScheduledDepartureAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.scheduledDepartureAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.scheduledDepartureAction(value)
        }

    var setChargingAmpsAction: CarServer_SetChargingAmpsAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.setChargingAmpsAction)?.value
            ?: _protobufPending(43, { CarServer_SetChargingAmpsAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.setChargingAmpsAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.setChargingAmpsAction(value)
        }

    var hvacClimateKeeperAction: CarServer_HvacClimateKeeperAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacClimateKeeperAction)?.value
            ?: _protobufPending(44, { CarServer_HvacClimateKeeperAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacClimateKeeperAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacClimateKeeperAction(value)
        }

    var ping: CarServer_Ping
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.ping)?.value
            ?: _protobufPending(46, { CarServer_Ping() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.ping(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.ping(value)
        }

    var autoSeatClimateAction: CarServer_AutoSeatClimateAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.autoSeatClimateAction)?.value
            ?: _protobufPending(48, { CarServer_AutoSeatClimateAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.autoSeatClimateAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.autoSeatClimateAction(value)
        }

    var hvacSeatCoolerActions: CarServer_HvacSeatCoolerActions
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSeatCoolerActions)?.value
            ?: _protobufPending(49, { CarServer_HvacSeatCoolerActions() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSeatCoolerActions(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSeatCoolerActions(value)
        }

    var setCabinOverheatProtectionAction: CarServer_SetCabinOverheatProtectionAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.setCabinOverheatProtectionAction)?.value
            ?: _protobufPending(50, { CarServer_SetCabinOverheatProtectionAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.setCabinOverheatProtectionAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.setCabinOverheatProtectionAction(value)
        }

    var setVehicleNameAction: CarServer_SetVehicleNameAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.setVehicleNameAction)?.value
            ?: _protobufPending(54, { CarServer_SetVehicleNameAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.setVehicleNameAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.setVehicleNameAction(value)
        }

    var chargePortDoorClose: CarServer_ChargePortDoorClose
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.chargePortDoorClose)?.value
            ?: _protobufPending(61, { CarServer_ChargePortDoorClose() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.chargePortDoorClose(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.chargePortDoorClose(value)
        }

    var chargePortDoorOpen: CarServer_ChargePortDoorOpen
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.chargePortDoorOpen)?.value
            ?: _protobufPending(62, { CarServer_ChargePortDoorOpen() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.chargePortDoorOpen(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.chargePortDoorOpen(value)
        }

    var guestModeAction: CarServer_VehicleState.GuestMode
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.guestModeAction)?.value
            ?: _protobufPending(65, { CarServer_VehicleState.GuestMode() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.guestModeAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.guestModeAction(value)
        }

    var setCopTempAction: CarServer_SetCopTempAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.setCopTempAction)?.value
            ?: _protobufPending(66, { CarServer_SetCopTempAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.setCopTempAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.setCopTempAction(value)
        }

    var eraseUserDataAction: CarServer_EraseUserDataAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.eraseUserDataAction)?.value
            ?: _protobufPending(72, { CarServer_EraseUserDataAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.eraseUserDataAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.eraseUserDataAction(value)
        }

    var vehicleControlSetPinToDriveAction: CarServer_VehicleControlSetPinToDriveAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSetPinToDriveAction)?.value
            ?: _protobufPending(77, { CarServer_VehicleControlSetPinToDriveAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSetPinToDriveAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSetPinToDriveAction(value)
        }

    var vehicleControlResetPinToDriveAction: CarServer_VehicleControlResetPinToDriveAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlResetPinToDriveAction)?.value
            ?: _protobufPending(78, { CarServer_VehicleControlResetPinToDriveAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlResetPinToDriveAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlResetPinToDriveAction(value)
        }

    var drivingClearSpeedLimitPinAdminAction: CarServer_DrivingClearSpeedLimitPinAdminAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingClearSpeedLimitPinAdminAction)?.value
            ?: _protobufPending(79, { CarServer_DrivingClearSpeedLimitPinAdminAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingClearSpeedLimitPinAdminAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingClearSpeedLimitPinAdminAction(value)
        }

    var vehicleControlResetPinToDriveAdminAction: CarServer_VehicleControlResetPinToDriveAdminAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlResetPinToDriveAdminAction)?.value
            ?: _protobufPending(89, { CarServer_VehicleControlResetPinToDriveAdminAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlResetPinToDriveAdminAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlResetPinToDriveAdminAction(value)
        }

    var addChargeScheduleAction: CarServer_ChargeSchedule
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.addChargeScheduleAction)?.value
            ?: _protobufPending(97, { CarServer_ChargeSchedule() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.addChargeScheduleAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.addChargeScheduleAction(value)
        }

    var removeChargeScheduleAction: CarServer_RemoveChargeScheduleAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.removeChargeScheduleAction)?.value
            ?: _protobufPending(98, { CarServer_RemoveChargeScheduleAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.removeChargeScheduleAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.removeChargeScheduleAction(value)
        }

    var addPreconditionScheduleAction: CarServer_PreconditionSchedule
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.addPreconditionScheduleAction)?.value
            ?: _protobufPending(99, { CarServer_PreconditionSchedule() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.addPreconditionScheduleAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.addPreconditionScheduleAction(value)
        }

    var removePreconditionScheduleAction: CarServer_RemovePreconditionScheduleAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.removePreconditionScheduleAction)?.value
            ?: _protobufPending(100, { CarServer_RemovePreconditionScheduleAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.removePreconditionScheduleAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.removePreconditionScheduleAction(value)
        }

    var batchRemovePreconditionSchedulesAction: CarServer_BatchRemovePreconditionSchedulesAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.batchRemovePreconditionSchedulesAction)?.value
            ?: _protobufPending(107, { CarServer_BatchRemovePreconditionSchedulesAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.batchRemovePreconditionSchedulesAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.batchRemovePreconditionSchedulesAction(value)
        }

    var batchRemoveChargeSchedulesAction: CarServer_BatchRemoveChargeSchedulesAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.batchRemoveChargeSchedulesAction)?.value
            ?: _protobufPending(108, { CarServer_BatchRemoveChargeSchedulesAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.batchRemoveChargeSchedulesAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.batchRemoveChargeSchedulesAction(value)
        }

    var parentalControlsClearPinAction: CarServer_ParentalControlsClearPinAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsClearPinAction)?.value
            ?: _protobufPending(109, { CarServer_ParentalControlsClearPinAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsClearPinAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsClearPinAction(value)
        }

    var parentalControlsClearPinAdminAction: CarServer_ParentalControlsClearPinAdminAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsClearPinAdminAction)?.value
            ?: _protobufPending(110, { CarServer_ParentalControlsClearPinAdminAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsClearPinAdminAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsClearPinAdminAction(value)
        }

    var parentalControlsAction: CarServer_ParentalControlsAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsAction)?.value
            ?: _protobufPending(111, { CarServer_ParentalControlsAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsAction(value)
        }

    var parentalControlsEnableSettingsAction: CarServer_ParentalControlsEnableSettingsAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsEnableSettingsAction)?.value
            ?: _protobufPending(112, { CarServer_ParentalControlsEnableSettingsAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsEnableSettingsAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsEnableSettingsAction(value)
        }

    var parentalControlsSetSpeedLimitAction: CarServer_ParentalControlsSetSpeedLimitAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsSetSpeedLimitAction)?.value
            ?: _protobufPending(113, { CarServer_ParentalControlsSetSpeedLimitAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsSetSpeedLimitAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsSetSpeedLimitAction(value)
        }

    var setLowPowerModeAction: CarServer_SetLowPowerModeAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.setLowPowerModeAction)?.value
            ?: _protobufPending(130, { CarServer_SetLowPowerModeAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.setLowPowerModeAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.setLowPowerModeAction(value)
        }

    var setKeepAccessoryPowerModeAction: CarServer_SetKeepAccessoryPowerModeAction
        get() = (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.setKeepAccessoryPowerModeAction)?.value
            ?: _protobufPending(138, { CarServer_SetKeepAccessoryPowerModeAction() }) { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.setKeepAccessoryPowerModeAction(it)) }
        set(value) {
            this.vehicleActionMsg = CarServer_VehicleAction.OneOf_VehicleActionMsg.setKeepAccessoryPowerModeAction(value)
        }

    sealed class OneOf_VehicleActionMsg(value: Any) : ProtobufOneofCase(value) {
        class getVehicleData(val value: CarServer_GetVehicleData) : OneOf_VehicleActionMsg(value)
        class chargingSetLimitAction(val value: CarServer_ChargingSetLimitAction) : OneOf_VehicleActionMsg(value)
        class chargingStartStopAction(val value: CarServer_ChargingStartStopAction) : OneOf_VehicleActionMsg(value)
        class drivingClearSpeedLimitPinAction(val value: CarServer_DrivingClearSpeedLimitPinAction) : OneOf_VehicleActionMsg(value)
        class drivingSetSpeedLimitAction(val value: CarServer_DrivingSetSpeedLimitAction) : OneOf_VehicleActionMsg(value)
        class drivingSpeedLimitAction(val value: CarServer_DrivingSpeedLimitAction) : OneOf_VehicleActionMsg(value)
        class hvacAutoAction(val value: CarServer_HvacAutoAction) : OneOf_VehicleActionMsg(value)
        class hvacSetPreconditioningMaxAction(val value: CarServer_HvacSetPreconditioningMaxAction) : OneOf_VehicleActionMsg(value)
        class hvacSteeringWheelHeaterAction(val value: CarServer_HvacSteeringWheelHeaterAction) : OneOf_VehicleActionMsg(value)
        class hvacTemperatureAdjustmentAction(val value: CarServer_HvacTemperatureAdjustmentAction) : OneOf_VehicleActionMsg(value)
        class mediaPlayAction(val value: CarServer_MediaPlayAction) : OneOf_VehicleActionMsg(value)
        class mediaUpdateVolume(val value: CarServer_MediaUpdateVolume) : OneOf_VehicleActionMsg(value)
        class mediaNextFavorite(val value: CarServer_MediaNextFavorite) : OneOf_VehicleActionMsg(value)
        class mediaPreviousFavorite(val value: CarServer_MediaPreviousFavorite) : OneOf_VehicleActionMsg(value)
        class mediaNextTrack(val value: CarServer_MediaNextTrack) : OneOf_VehicleActionMsg(value)
        class mediaPreviousTrack(val value: CarServer_MediaPreviousTrack) : OneOf_VehicleActionMsg(value)
        class getNearbyChargingSites(val value: CarServer_GetNearbyChargingSites) : OneOf_VehicleActionMsg(value)
        class vehicleControlCancelSoftwareUpdateAction(val value: CarServer_VehicleControlCancelSoftwareUpdateAction) : OneOf_VehicleActionMsg(value)
        class vehicleControlFlashLightsAction(val value: CarServer_VehicleControlFlashLightsAction) : OneOf_VehicleActionMsg(value)
        class vehicleControlHonkHornAction(val value: CarServer_VehicleControlHonkHornAction) : OneOf_VehicleActionMsg(value)
        class vehicleControlResetValetPinAction(val value: CarServer_VehicleControlResetValetPinAction) : OneOf_VehicleActionMsg(value)
        class vehicleControlScheduleSoftwareUpdateAction(val value: CarServer_VehicleControlScheduleSoftwareUpdateAction) : OneOf_VehicleActionMsg(value)
        class vehicleControlSetSentryModeAction(val value: CarServer_VehicleControlSetSentryModeAction) : OneOf_VehicleActionMsg(value)
        class vehicleControlSetValetModeAction(val value: CarServer_VehicleControlSetValetModeAction) : OneOf_VehicleActionMsg(value)
        class vehicleControlSunroofOpenCloseAction(val value: CarServer_VehicleControlSunroofOpenCloseAction) : OneOf_VehicleActionMsg(value)
        class vehicleControlTriggerHomelinkAction(val value: CarServer_VehicleControlTriggerHomelinkAction) : OneOf_VehicleActionMsg(value)
        class vehicleControlWindowAction(val value: CarServer_VehicleControlWindowAction) : OneOf_VehicleActionMsg(value)
        class hvacBioweaponModeAction(val value: CarServer_HvacBioweaponModeAction) : OneOf_VehicleActionMsg(value)
        class hvacSeatHeaterActions(val value: CarServer_HvacSeatHeaterActions) : OneOf_VehicleActionMsg(value)
        class scheduledChargingAction(val value: CarServer_ScheduledChargingAction) : OneOf_VehicleActionMsg(value)
        class scheduledDepartureAction(val value: CarServer_ScheduledDepartureAction) : OneOf_VehicleActionMsg(value)
        class setChargingAmpsAction(val value: CarServer_SetChargingAmpsAction) : OneOf_VehicleActionMsg(value)
        class hvacClimateKeeperAction(val value: CarServer_HvacClimateKeeperAction) : OneOf_VehicleActionMsg(value)
        class ping(val value: CarServer_Ping) : OneOf_VehicleActionMsg(value)
        class autoSeatClimateAction(val value: CarServer_AutoSeatClimateAction) : OneOf_VehicleActionMsg(value)
        class hvacSeatCoolerActions(val value: CarServer_HvacSeatCoolerActions) : OneOf_VehicleActionMsg(value)
        class setCabinOverheatProtectionAction(val value: CarServer_SetCabinOverheatProtectionAction) : OneOf_VehicleActionMsg(value)
        class setVehicleNameAction(val value: CarServer_SetVehicleNameAction) : OneOf_VehicleActionMsg(value)
        class chargePortDoorClose(val value: CarServer_ChargePortDoorClose) : OneOf_VehicleActionMsg(value)
        class chargePortDoorOpen(val value: CarServer_ChargePortDoorOpen) : OneOf_VehicleActionMsg(value)
        class guestModeAction(val value: CarServer_VehicleState.GuestMode) : OneOf_VehicleActionMsg(value)
        class setCopTempAction(val value: CarServer_SetCopTempAction) : OneOf_VehicleActionMsg(value)
        class eraseUserDataAction(val value: CarServer_EraseUserDataAction) : OneOf_VehicleActionMsg(value)
        class vehicleControlSetPinToDriveAction(val value: CarServer_VehicleControlSetPinToDriveAction) : OneOf_VehicleActionMsg(value)
        class vehicleControlResetPinToDriveAction(val value: CarServer_VehicleControlResetPinToDriveAction) : OneOf_VehicleActionMsg(value)
        class drivingClearSpeedLimitPinAdminAction(val value: CarServer_DrivingClearSpeedLimitPinAdminAction) : OneOf_VehicleActionMsg(value)
        class vehicleControlResetPinToDriveAdminAction(val value: CarServer_VehicleControlResetPinToDriveAdminAction) : OneOf_VehicleActionMsg(value)
        class addChargeScheduleAction(val value: CarServer_ChargeSchedule) : OneOf_VehicleActionMsg(value)
        class removeChargeScheduleAction(val value: CarServer_RemoveChargeScheduleAction) : OneOf_VehicleActionMsg(value)
        class addPreconditionScheduleAction(val value: CarServer_PreconditionSchedule) : OneOf_VehicleActionMsg(value)
        class removePreconditionScheduleAction(val value: CarServer_RemovePreconditionScheduleAction) : OneOf_VehicleActionMsg(value)
        class batchRemovePreconditionSchedulesAction(val value: CarServer_BatchRemovePreconditionSchedulesAction) : OneOf_VehicleActionMsg(value)
        class batchRemoveChargeSchedulesAction(val value: CarServer_BatchRemoveChargeSchedulesAction) : OneOf_VehicleActionMsg(value)
        class parentalControlsClearPinAction(val value: CarServer_ParentalControlsClearPinAction) : OneOf_VehicleActionMsg(value)
        class parentalControlsClearPinAdminAction(val value: CarServer_ParentalControlsClearPinAdminAction) : OneOf_VehicleActionMsg(value)
        class parentalControlsAction(val value: CarServer_ParentalControlsAction) : OneOf_VehicleActionMsg(value)
        class parentalControlsEnableSettingsAction(val value: CarServer_ParentalControlsEnableSettingsAction) : OneOf_VehicleActionMsg(value)
        class parentalControlsSetSpeedLimitAction(val value: CarServer_ParentalControlsSetSpeedLimitAction) : OneOf_VehicleActionMsg(value)
        class setLowPowerModeAction(val value: CarServer_SetLowPowerModeAction) : OneOf_VehicleActionMsg(value)
        class setKeepAccessoryPowerModeAction(val value: CarServer_SetKeepAccessoryPowerModeAction) : OneOf_VehicleActionMsg(value)
    }

    private var _vehicleActionMsg: CarServer_VehicleAction.OneOf_VehicleActionMsg? = null

    private fun _protobufStore_vehicleActionMsg(value: CarServer_VehicleAction.OneOf_VehicleActionMsg?) {
        _protobufDropPending(1)
        _protobufDropPending(5)
        _protobufDropPending(6)
        _protobufDropPending(7)
        _protobufDropPending(8)
        _protobufDropPending(9)
        _protobufDropPending(10)
        _protobufDropPending(12)
        _protobufDropPending(13)
        _protobufDropPending(14)
        _protobufDropPending(15)
        _protobufDropPending(16)
        _protobufDropPending(17)
        _protobufDropPending(18)
        _protobufDropPending(19)
        _protobufDropPending(20)
        _protobufDropPending(23)
        _protobufDropPending(25)
        _protobufDropPending(26)
        _protobufDropPending(27)
        _protobufDropPending(28)
        _protobufDropPending(29)
        _protobufDropPending(30)
        _protobufDropPending(31)
        _protobufDropPending(32)
        _protobufDropPending(33)
        _protobufDropPending(34)
        _protobufDropPending(35)
        _protobufDropPending(36)
        _protobufDropPending(41)
        _protobufDropPending(42)
        _protobufDropPending(43)
        _protobufDropPending(44)
        _protobufDropPending(46)
        _protobufDropPending(48)
        _protobufDropPending(49)
        _protobufDropPending(50)
        _protobufDropPending(54)
        _protobufDropPending(61)
        _protobufDropPending(62)
        _protobufDropPending(65)
        _protobufDropPending(66)
        _protobufDropPending(72)
        _protobufDropPending(77)
        _protobufDropPending(78)
        _protobufDropPending(79)
        _protobufDropPending(89)
        _protobufDropPending(97)
        _protobufDropPending(98)
        _protobufDropPending(99)
        _protobufDropPending(100)
        _protobufDropPending(107)
        _protobufDropPending(108)
        _protobufDropPending(109)
        _protobufDropPending(110)
        _protobufDropPending(111)
        _protobufDropPending(112)
        _protobufDropPending(113)
        _protobufDropPending(130)
        _protobufDropPending(138)
        this._vehicleActionMsg = value
    }

    private fun _protobufCopy_vehicleActionMsg(value: CarServer_VehicleAction.OneOf_VehicleActionMsg?): CarServer_VehicleAction.OneOf_VehicleActionMsg? =
        when (value) {
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.getVehicleData -> CarServer_VehicleAction.OneOf_VehicleActionMsg.getVehicleData(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.chargingSetLimitAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.chargingSetLimitAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.chargingStartStopAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.chargingStartStopAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingClearSpeedLimitPinAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingClearSpeedLimitPinAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingSetSpeedLimitAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingSetSpeedLimitAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingSpeedLimitAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingSpeedLimitAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacAutoAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacAutoAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSetPreconditioningMaxAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSetPreconditioningMaxAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSteeringWheelHeaterAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSteeringWheelHeaterAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacTemperatureAdjustmentAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacTemperatureAdjustmentAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaPlayAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaPlayAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaUpdateVolume -> CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaUpdateVolume(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaNextFavorite -> CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaNextFavorite(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaPreviousFavorite -> CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaPreviousFavorite(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaNextTrack -> CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaNextTrack(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaPreviousTrack -> CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaPreviousTrack(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.getNearbyChargingSites -> CarServer_VehicleAction.OneOf_VehicleActionMsg.getNearbyChargingSites(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlCancelSoftwareUpdateAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlCancelSoftwareUpdateAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlFlashLightsAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlFlashLightsAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlHonkHornAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlHonkHornAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlResetValetPinAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlResetValetPinAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlScheduleSoftwareUpdateAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlScheduleSoftwareUpdateAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSetSentryModeAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSetSentryModeAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSetValetModeAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSetValetModeAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSunroofOpenCloseAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSunroofOpenCloseAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlTriggerHomelinkAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlTriggerHomelinkAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlWindowAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlWindowAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacBioweaponModeAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacBioweaponModeAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSeatHeaterActions -> CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSeatHeaterActions(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.scheduledChargingAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.scheduledChargingAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.scheduledDepartureAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.scheduledDepartureAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.setChargingAmpsAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.setChargingAmpsAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacClimateKeeperAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacClimateKeeperAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.ping -> CarServer_VehicleAction.OneOf_VehicleActionMsg.ping(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.autoSeatClimateAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.autoSeatClimateAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSeatCoolerActions -> CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSeatCoolerActions(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.setCabinOverheatProtectionAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.setCabinOverheatProtectionAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.setVehicleNameAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.setVehicleNameAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.chargePortDoorClose -> CarServer_VehicleAction.OneOf_VehicleActionMsg.chargePortDoorClose(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.chargePortDoorOpen -> CarServer_VehicleAction.OneOf_VehicleActionMsg.chargePortDoorOpen(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.guestModeAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.guestModeAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.setCopTempAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.setCopTempAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.eraseUserDataAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.eraseUserDataAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSetPinToDriveAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSetPinToDriveAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlResetPinToDriveAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlResetPinToDriveAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingClearSpeedLimitPinAdminAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingClearSpeedLimitPinAdminAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlResetPinToDriveAdminAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlResetPinToDriveAdminAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.addChargeScheduleAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.addChargeScheduleAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.removeChargeScheduleAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.removeChargeScheduleAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.addPreconditionScheduleAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.addPreconditionScheduleAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.removePreconditionScheduleAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.removePreconditionScheduleAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.batchRemovePreconditionSchedulesAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.batchRemovePreconditionSchedulesAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.batchRemoveChargeSchedulesAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.batchRemoveChargeSchedulesAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsClearPinAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsClearPinAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsClearPinAdminAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsClearPinAdminAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsEnableSettingsAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsEnableSettingsAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsSetSpeedLimitAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsSetSpeedLimitAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.setLowPowerModeAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.setLowPowerModeAction(value.value.copy())
            is CarServer_VehicleAction.OneOf_VehicleActionMsg.setKeepAccessoryPowerModeAction -> CarServer_VehicleAction.OneOf_VehicleActionMsg.setKeepAccessoryPowerModeAction(value.value.copy())
            else -> value
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.getVehicleData)?.value) { CarServer_GetVehicleData() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.getVehicleData(it)) }
                5 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.chargingSetLimitAction)?.value) { CarServer_ChargingSetLimitAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.chargingSetLimitAction(it)) }
                6 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.chargingStartStopAction)?.value) { CarServer_ChargingStartStopAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.chargingStartStopAction(it)) }
                7 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingClearSpeedLimitPinAction)?.value) { CarServer_DrivingClearSpeedLimitPinAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingClearSpeedLimitPinAction(it)) }
                8 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingSetSpeedLimitAction)?.value) { CarServer_DrivingSetSpeedLimitAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingSetSpeedLimitAction(it)) }
                9 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingSpeedLimitAction)?.value) { CarServer_DrivingSpeedLimitAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingSpeedLimitAction(it)) }
                10 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacAutoAction)?.value) { CarServer_HvacAutoAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacAutoAction(it)) }
                12 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSetPreconditioningMaxAction)?.value) { CarServer_HvacSetPreconditioningMaxAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSetPreconditioningMaxAction(it)) }
                13 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSteeringWheelHeaterAction)?.value) { CarServer_HvacSteeringWheelHeaterAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSteeringWheelHeaterAction(it)) }
                14 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacTemperatureAdjustmentAction)?.value) { CarServer_HvacTemperatureAdjustmentAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacTemperatureAdjustmentAction(it)) }
                15 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaPlayAction)?.value) { CarServer_MediaPlayAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaPlayAction(it)) }
                16 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaUpdateVolume)?.value) { CarServer_MediaUpdateVolume() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaUpdateVolume(it)) }
                17 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaNextFavorite)?.value) { CarServer_MediaNextFavorite() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaNextFavorite(it)) }
                18 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaPreviousFavorite)?.value) { CarServer_MediaPreviousFavorite() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaPreviousFavorite(it)) }
                19 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaNextTrack)?.value) { CarServer_MediaNextTrack() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaNextTrack(it)) }
                20 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaPreviousTrack)?.value) { CarServer_MediaPreviousTrack() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaPreviousTrack(it)) }
                23 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.getNearbyChargingSites)?.value) { CarServer_GetNearbyChargingSites() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.getNearbyChargingSites(it)) }
                25 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlCancelSoftwareUpdateAction)?.value) { CarServer_VehicleControlCancelSoftwareUpdateAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlCancelSoftwareUpdateAction(it)) }
                26 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlFlashLightsAction)?.value) { CarServer_VehicleControlFlashLightsAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlFlashLightsAction(it)) }
                27 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlHonkHornAction)?.value) { CarServer_VehicleControlHonkHornAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlHonkHornAction(it)) }
                28 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlResetValetPinAction)?.value) { CarServer_VehicleControlResetValetPinAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlResetValetPinAction(it)) }
                29 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlScheduleSoftwareUpdateAction)?.value) { CarServer_VehicleControlScheduleSoftwareUpdateAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlScheduleSoftwareUpdateAction(it)) }
                30 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSetSentryModeAction)?.value) { CarServer_VehicleControlSetSentryModeAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSetSentryModeAction(it)) }
                31 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSetValetModeAction)?.value) { CarServer_VehicleControlSetValetModeAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSetValetModeAction(it)) }
                32 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSunroofOpenCloseAction)?.value) { CarServer_VehicleControlSunroofOpenCloseAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSunroofOpenCloseAction(it)) }
                33 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlTriggerHomelinkAction)?.value) { CarServer_VehicleControlTriggerHomelinkAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlTriggerHomelinkAction(it)) }
                34 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlWindowAction)?.value) { CarServer_VehicleControlWindowAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlWindowAction(it)) }
                35 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacBioweaponModeAction)?.value) { CarServer_HvacBioweaponModeAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacBioweaponModeAction(it)) }
                36 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSeatHeaterActions)?.value) { CarServer_HvacSeatHeaterActions() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSeatHeaterActions(it)) }
                41 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.scheduledChargingAction)?.value) { CarServer_ScheduledChargingAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.scheduledChargingAction(it)) }
                42 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.scheduledDepartureAction)?.value) { CarServer_ScheduledDepartureAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.scheduledDepartureAction(it)) }
                43 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.setChargingAmpsAction)?.value) { CarServer_SetChargingAmpsAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.setChargingAmpsAction(it)) }
                44 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacClimateKeeperAction)?.value) { CarServer_HvacClimateKeeperAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacClimateKeeperAction(it)) }
                46 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.ping)?.value) { CarServer_Ping() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.ping(it)) }
                48 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.autoSeatClimateAction)?.value) { CarServer_AutoSeatClimateAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.autoSeatClimateAction(it)) }
                49 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSeatCoolerActions)?.value) { CarServer_HvacSeatCoolerActions() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSeatCoolerActions(it)) }
                50 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.setCabinOverheatProtectionAction)?.value) { CarServer_SetCabinOverheatProtectionAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.setCabinOverheatProtectionAction(it)) }
                54 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.setVehicleNameAction)?.value) { CarServer_SetVehicleNameAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.setVehicleNameAction(it)) }
                61 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.chargePortDoorClose)?.value) { CarServer_ChargePortDoorClose() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.chargePortDoorClose(it)) }
                62 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.chargePortDoorOpen)?.value) { CarServer_ChargePortDoorOpen() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.chargePortDoorOpen(it)) }
                65 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.guestModeAction)?.value) { CarServer_VehicleState.GuestMode() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.guestModeAction(it)) }
                66 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.setCopTempAction)?.value) { CarServer_SetCopTempAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.setCopTempAction(it)) }
                72 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.eraseUserDataAction)?.value) { CarServer_EraseUserDataAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.eraseUserDataAction(it)) }
                77 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSetPinToDriveAction)?.value) { CarServer_VehicleControlSetPinToDriveAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSetPinToDriveAction(it)) }
                78 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlResetPinToDriveAction)?.value) { CarServer_VehicleControlResetPinToDriveAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlResetPinToDriveAction(it)) }
                79 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingClearSpeedLimitPinAdminAction)?.value) { CarServer_DrivingClearSpeedLimitPinAdminAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingClearSpeedLimitPinAdminAction(it)) }
                89 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlResetPinToDriveAdminAction)?.value) { CarServer_VehicleControlResetPinToDriveAdminAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlResetPinToDriveAdminAction(it)) }
                97 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.addChargeScheduleAction)?.value) { CarServer_ChargeSchedule() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.addChargeScheduleAction(it)) }
                98 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.removeChargeScheduleAction)?.value) { CarServer_RemoveChargeScheduleAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.removeChargeScheduleAction(it)) }
                99 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.addPreconditionScheduleAction)?.value) { CarServer_PreconditionSchedule() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.addPreconditionScheduleAction(it)) }
                100 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.removePreconditionScheduleAction)?.value) { CarServer_RemovePreconditionScheduleAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.removePreconditionScheduleAction(it)) }
                107 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.batchRemovePreconditionSchedulesAction)?.value) { CarServer_BatchRemovePreconditionSchedulesAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.batchRemovePreconditionSchedulesAction(it)) }
                108 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.batchRemoveChargeSchedulesAction)?.value) { CarServer_BatchRemoveChargeSchedulesAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.batchRemoveChargeSchedulesAction(it)) }
                109 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsClearPinAction)?.value) { CarServer_ParentalControlsClearPinAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsClearPinAction(it)) }
                110 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsClearPinAdminAction)?.value) { CarServer_ParentalControlsClearPinAdminAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsClearPinAdminAction(it)) }
                111 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsAction)?.value) { CarServer_ParentalControlsAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsAction(it)) }
                112 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsEnableSettingsAction)?.value) { CarServer_ParentalControlsEnableSettingsAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsEnableSettingsAction(it)) }
                113 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsSetSpeedLimitAction)?.value) { CarServer_ParentalControlsSetSpeedLimitAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsSetSpeedLimitAction(it)) }
                130 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.setLowPowerModeAction)?.value) { CarServer_SetLowPowerModeAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.setLowPowerModeAction(it)) }
                138 -> decoder.decodeSingularMessageField((this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.setKeepAccessoryPowerModeAction)?.value) { CarServer_SetKeepAccessoryPowerModeAction() }
                    ?.let { this._protobufStore_vehicleActionMsg(CarServer_VehicleAction.OneOf_VehicleActionMsg.setKeepAccessoryPowerModeAction(it)) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.getVehicleData)?.let {
            visitor.visitSingularMessageField(it.value, 1)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.chargingSetLimitAction)?.let {
            visitor.visitSingularMessageField(it.value, 5)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.chargingStartStopAction)?.let {
            visitor.visitSingularMessageField(it.value, 6)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingClearSpeedLimitPinAction)?.let {
            visitor.visitSingularMessageField(it.value, 7)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingSetSpeedLimitAction)?.let {
            visitor.visitSingularMessageField(it.value, 8)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingSpeedLimitAction)?.let {
            visitor.visitSingularMessageField(it.value, 9)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacAutoAction)?.let {
            visitor.visitSingularMessageField(it.value, 10)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSetPreconditioningMaxAction)?.let {
            visitor.visitSingularMessageField(it.value, 12)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSteeringWheelHeaterAction)?.let {
            visitor.visitSingularMessageField(it.value, 13)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacTemperatureAdjustmentAction)?.let {
            visitor.visitSingularMessageField(it.value, 14)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaPlayAction)?.let {
            visitor.visitSingularMessageField(it.value, 15)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaUpdateVolume)?.let {
            visitor.visitSingularMessageField(it.value, 16)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaNextFavorite)?.let {
            visitor.visitSingularMessageField(it.value, 17)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaPreviousFavorite)?.let {
            visitor.visitSingularMessageField(it.value, 18)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaNextTrack)?.let {
            visitor.visitSingularMessageField(it.value, 19)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.mediaPreviousTrack)?.let {
            visitor.visitSingularMessageField(it.value, 20)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.getNearbyChargingSites)?.let {
            visitor.visitSingularMessageField(it.value, 23)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlCancelSoftwareUpdateAction)?.let {
            visitor.visitSingularMessageField(it.value, 25)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlFlashLightsAction)?.let {
            visitor.visitSingularMessageField(it.value, 26)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlHonkHornAction)?.let {
            visitor.visitSingularMessageField(it.value, 27)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlResetValetPinAction)?.let {
            visitor.visitSingularMessageField(it.value, 28)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlScheduleSoftwareUpdateAction)?.let {
            visitor.visitSingularMessageField(it.value, 29)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSetSentryModeAction)?.let {
            visitor.visitSingularMessageField(it.value, 30)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSetValetModeAction)?.let {
            visitor.visitSingularMessageField(it.value, 31)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSunroofOpenCloseAction)?.let {
            visitor.visitSingularMessageField(it.value, 32)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlTriggerHomelinkAction)?.let {
            visitor.visitSingularMessageField(it.value, 33)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlWindowAction)?.let {
            visitor.visitSingularMessageField(it.value, 34)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacBioweaponModeAction)?.let {
            visitor.visitSingularMessageField(it.value, 35)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSeatHeaterActions)?.let {
            visitor.visitSingularMessageField(it.value, 36)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.scheduledChargingAction)?.let {
            visitor.visitSingularMessageField(it.value, 41)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.scheduledDepartureAction)?.let {
            visitor.visitSingularMessageField(it.value, 42)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.setChargingAmpsAction)?.let {
            visitor.visitSingularMessageField(it.value, 43)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacClimateKeeperAction)?.let {
            visitor.visitSingularMessageField(it.value, 44)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.ping)?.let {
            visitor.visitSingularMessageField(it.value, 46)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.autoSeatClimateAction)?.let {
            visitor.visitSingularMessageField(it.value, 48)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.hvacSeatCoolerActions)?.let {
            visitor.visitSingularMessageField(it.value, 49)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.setCabinOverheatProtectionAction)?.let {
            visitor.visitSingularMessageField(it.value, 50)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.setVehicleNameAction)?.let {
            visitor.visitSingularMessageField(it.value, 54)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.chargePortDoorClose)?.let {
            visitor.visitSingularMessageField(it.value, 61)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.chargePortDoorOpen)?.let {
            visitor.visitSingularMessageField(it.value, 62)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.guestModeAction)?.let {
            visitor.visitSingularMessageField(it.value, 65)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.setCopTempAction)?.let {
            visitor.visitSingularMessageField(it.value, 66)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.eraseUserDataAction)?.let {
            visitor.visitSingularMessageField(it.value, 72)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlSetPinToDriveAction)?.let {
            visitor.visitSingularMessageField(it.value, 77)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlResetPinToDriveAction)?.let {
            visitor.visitSingularMessageField(it.value, 78)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.drivingClearSpeedLimitPinAdminAction)?.let {
            visitor.visitSingularMessageField(it.value, 79)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.vehicleControlResetPinToDriveAdminAction)?.let {
            visitor.visitSingularMessageField(it.value, 89)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.addChargeScheduleAction)?.let {
            visitor.visitSingularMessageField(it.value, 97)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.removeChargeScheduleAction)?.let {
            visitor.visitSingularMessageField(it.value, 98)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.addPreconditionScheduleAction)?.let {
            visitor.visitSingularMessageField(it.value, 99)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.removePreconditionScheduleAction)?.let {
            visitor.visitSingularMessageField(it.value, 100)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.batchRemovePreconditionSchedulesAction)?.let {
            visitor.visitSingularMessageField(it.value, 107)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.batchRemoveChargeSchedulesAction)?.let {
            visitor.visitSingularMessageField(it.value, 108)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsClearPinAction)?.let {
            visitor.visitSingularMessageField(it.value, 109)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsClearPinAdminAction)?.let {
            visitor.visitSingularMessageField(it.value, 110)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsAction)?.let {
            visitor.visitSingularMessageField(it.value, 111)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsEnableSettingsAction)?.let {
            visitor.visitSingularMessageField(it.value, 112)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.parentalControlsSetSpeedLimitAction)?.let {
            visitor.visitSingularMessageField(it.value, 113)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.setLowPowerModeAction)?.let {
            visitor.visitSingularMessageField(it.value, 130)
        }
        (this._vehicleActionMsg as? CarServer_VehicleAction.OneOf_VehicleActionMsg.setKeepAccessoryPowerModeAction)?.let {
            visitor.visitSingularMessageField(it.value, 138)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_VehicleAction) return false
        if (this._vehicleActionMsg != other._vehicleActionMsg) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._vehicleActionMsg?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_VehicleAction {
        val result = CarServer_VehicleAction()
        result._vehicleActionMsg = this._protobufCopy_vehicleActionMsg(this._vehicleActionMsg)
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.VehicleAction"

        fun with(block: CarServer_VehicleAction.() -> Unit): CarServer_VehicleAction =
            CarServer_VehicleAction().apply(block)
    }
}

class CarServer_GetVehicleData() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var getChargeState: CarServer_GetChargeState
        get() = this._getChargeState ?: _protobufPending(2, { CarServer_GetChargeState() }) { this._getChargeState = it }
        set(value) {
            _protobufDropPending(2)
            this._getChargeState = value.copy()
            _protobufMutated()
        }

    val hasGetChargeState: Boolean
        get() = this._getChargeState != null

    fun clearGetChargeState() {
        _protobufDropPending(2)
        this._getChargeState = null
        _protobufMutated()
    }

    var getClimateState: CarServer_GetClimateState
        get() = this._getClimateState ?: _protobufPending(3, { CarServer_GetClimateState() }) { this._getClimateState = it }
        set(value) {
            _protobufDropPending(3)
            this._getClimateState = value.copy()
            _protobufMutated()
        }

    val hasGetClimateState: Boolean
        get() = this._getClimateState != null

    fun clearGetClimateState() {
        _protobufDropPending(3)
        this._getClimateState = null
        _protobufMutated()
    }

    var getDriveState: CarServer_GetDriveState
        get() = this._getDriveState ?: _protobufPending(4, { CarServer_GetDriveState() }) { this._getDriveState = it }
        set(value) {
            _protobufDropPending(4)
            this._getDriveState = value.copy()
            _protobufMutated()
        }

    val hasGetDriveState: Boolean
        get() = this._getDriveState != null

    fun clearGetDriveState() {
        _protobufDropPending(4)
        this._getDriveState = null
        _protobufMutated()
    }

    var getLocationState: CarServer_GetLocationState
        get() = this._getLocationState ?: _protobufPending(7, { CarServer_GetLocationState() }) { this._getLocationState = it }
        set(value) {
            _protobufDropPending(7)
            this._getLocationState = value.copy()
            _protobufMutated()
        }

    val hasGetLocationState: Boolean
        get() = this._getLocationState != null

    fun clearGetLocationState() {
        _protobufDropPending(7)
        this._getLocationState = null
        _protobufMutated()
    }

    var getClosuresState: CarServer_GetClosuresState
        get() = this._getClosuresState ?: _protobufPending(8, { CarServer_GetClosuresState() }) { this._getClosuresState = it }
        set(value) {
            _protobufDropPending(8)
            this._getClosuresState = value.copy()
            _protobufMutated()
        }

    val hasGetClosuresState: Boolean
        get() = this._getClosuresState != null

    fun clearGetClosuresState() {
        _protobufDropPending(8)
        this._getClosuresState = null
        _protobufMutated()
    }

    var getChargeScheduleState: CarServer_GetChargeScheduleState
        get() = this._getChargeScheduleState ?: _protobufPending(10, { CarServer_GetChargeScheduleState() }) { this._getChargeScheduleState = it }
        set(value) {
            _protobufDropPending(10)
            this._getChargeScheduleState = value.copy()
            _protobufMutated()
        }

    val hasGetChargeScheduleState: Boolean
        get() = this._getChargeScheduleState != null

    fun clearGetChargeScheduleState() {
        _protobufDropPending(10)
        this._getChargeScheduleState = null
        _protobufMutated()
    }

    var getPreconditioningScheduleState: CarServer_GetPreconditioningScheduleState
        get() = this._getPreconditioningScheduleState ?: _protobufPending(11, { CarServer_GetPreconditioningScheduleState() }) { this._getPreconditioningScheduleState = it }
        set(value) {
            _protobufDropPending(11)
            this._getPreconditioningScheduleState = value.copy()
            _protobufMutated()
        }

    val hasGetPreconditioningScheduleState: Boolean
        get() = this._getPreconditioningScheduleState != null

    fun clearGetPreconditioningScheduleState() {
        _protobufDropPending(11)
        this._getPreconditioningScheduleState = null
        _protobufMutated()
    }

    var getTirePressureState: CarServer_GetTirePressureState
        get() = this._getTirePressureState ?: _protobufPending(14, { CarServer_GetTirePressureState() }) { this._getTirePressureState = it }
        set(value) {
            _protobufDropPending(14)
            this._getTirePressureState = value.copy()
            _protobufMutated()
        }

    val hasGetTirePressureState: Boolean
        get() = this._getTirePressureState != null

    fun clearGetTirePressureState() {
        _protobufDropPending(14)
        this._getTirePressureState = null
        _protobufMutated()
    }

    var getMediaState: CarServer_GetMediaState
        get() = this._getMediaState ?: _protobufPending(15, { CarServer_GetMediaState() }) { this._getMediaState = it }
        set(value) {
            _protobufDropPending(15)
            this._getMediaState = value.copy()
            _protobufMutated()
        }

    val hasGetMediaState: Boolean
        get() = this._getMediaState != null

    fun clearGetMediaState() {
        _protobufDropPending(15)
        this._getMediaState = null
        _protobufMutated()
    }

    var getMediaDetailState: CarServer_GetMediaDetailState
        get() = this._getMediaDetailState ?: _protobufPending(16, { CarServer_GetMediaDetailState() }) { this._getMediaDetailState = it }
        set(value) {
            _protobufDropPending(16)
            this._getMediaDetailState = value.copy()
            _protobufMutated()
        }

    val hasGetMediaDetailState: Boolean
        get() = this._getMediaDetailState != null

    fun clearGetMediaDetailState() {
        _protobufDropPending(16)
        this._getMediaDetailState = null
        _protobufMutated()
    }

    var getSoftwareUpdateState: CarServer_GetSoftwareUpdateState
        get() = this._getSoftwareUpdateState ?: _protobufPending(17, { CarServer_GetSoftwareUpdateState() }) { this._getSoftwareUpdateState = it }
        set(value) {
            _protobufDropPending(17)
            this._getSoftwareUpdateState = value.copy()
            _protobufMutated()
        }

    val hasGetSoftwareUpdateState: Boolean
        get() = this._getSoftwareUpdateState != null

    fun clearGetSoftwareUpdateState() {
        _protobufDropPending(17)
        this._getSoftwareUpdateState = null
        _protobufMutated()
    }

    var getParentalControlsState: CarServer_GetParentalControlsState
        get() = this._getParentalControlsState ?: _protobufPending(19, { CarServer_GetParentalControlsState() }) { this._getParentalControlsState = it }
        set(value) {
            _protobufDropPending(19)
            this._getParentalControlsState = value.copy()
            _protobufMutated()
        }

    val hasGetParentalControlsState: Boolean
        get() = this._getParentalControlsState != null

    fun clearGetParentalControlsState() {
        _protobufDropPending(19)
        this._getParentalControlsState = null
        _protobufMutated()
    }

    private var _getChargeState: CarServer_GetChargeState? = null
    private var _getClimateState: CarServer_GetClimateState? = null
    private var _getDriveState: CarServer_GetDriveState? = null
    private var _getLocationState: CarServer_GetLocationState? = null
    private var _getClosuresState: CarServer_GetClosuresState? = null
    private var _getChargeScheduleState: CarServer_GetChargeScheduleState? = null
    private var _getPreconditioningScheduleState: CarServer_GetPreconditioningScheduleState? = null
    private var _getTirePressureState: CarServer_GetTirePressureState? = null
    private var _getMediaState: CarServer_GetMediaState? = null
    private var _getMediaDetailState: CarServer_GetMediaDetailState? = null
    private var _getSoftwareUpdateState: CarServer_GetSoftwareUpdateState? = null
    private var _getParentalControlsState: CarServer_GetParentalControlsState? = null

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                2 -> decoder.decodeSingularMessageField(this._getChargeState) { CarServer_GetChargeState() }?.let {
                    _protobufDropPending(2)
                    this._getChargeState = it
                }
                3 -> decoder.decodeSingularMessageField(this._getClimateState) { CarServer_GetClimateState() }?.let {
                    _protobufDropPending(3)
                    this._getClimateState = it
                }
                4 -> decoder.decodeSingularMessageField(this._getDriveState) { CarServer_GetDriveState() }?.let {
                    _protobufDropPending(4)
                    this._getDriveState = it
                }
                7 -> decoder.decodeSingularMessageField(this._getLocationState) { CarServer_GetLocationState() }?.let {
                    _protobufDropPending(7)
                    this._getLocationState = it
                }
                8 -> decoder.decodeSingularMessageField(this._getClosuresState) { CarServer_GetClosuresState() }?.let {
                    _protobufDropPending(8)
                    this._getClosuresState = it
                }
                10 -> decoder.decodeSingularMessageField(this._getChargeScheduleState) { CarServer_GetChargeScheduleState() }?.let {
                    _protobufDropPending(10)
                    this._getChargeScheduleState = it
                }
                11 -> decoder.decodeSingularMessageField(this._getPreconditioningScheduleState) { CarServer_GetPreconditioningScheduleState() }?.let {
                    _protobufDropPending(11)
                    this._getPreconditioningScheduleState = it
                }
                14 -> decoder.decodeSingularMessageField(this._getTirePressureState) { CarServer_GetTirePressureState() }?.let {
                    _protobufDropPending(14)
                    this._getTirePressureState = it
                }
                15 -> decoder.decodeSingularMessageField(this._getMediaState) { CarServer_GetMediaState() }?.let {
                    _protobufDropPending(15)
                    this._getMediaState = it
                }
                16 -> decoder.decodeSingularMessageField(this._getMediaDetailState) { CarServer_GetMediaDetailState() }?.let {
                    _protobufDropPending(16)
                    this._getMediaDetailState = it
                }
                17 -> decoder.decodeSingularMessageField(this._getSoftwareUpdateState) { CarServer_GetSoftwareUpdateState() }?.let {
                    _protobufDropPending(17)
                    this._getSoftwareUpdateState = it
                }
                19 -> decoder.decodeSingularMessageField(this._getParentalControlsState) { CarServer_GetParentalControlsState() }?.let {
                    _protobufDropPending(19)
                    this._getParentalControlsState = it
                }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        this._getChargeState?.let {
            visitor.visitSingularMessageField(it, 2)
        }
        this._getClimateState?.let {
            visitor.visitSingularMessageField(it, 3)
        }
        this._getDriveState?.let {
            visitor.visitSingularMessageField(it, 4)
        }
        this._getLocationState?.let {
            visitor.visitSingularMessageField(it, 7)
        }
        this._getClosuresState?.let {
            visitor.visitSingularMessageField(it, 8)
        }
        this._getChargeScheduleState?.let {
            visitor.visitSingularMessageField(it, 10)
        }
        this._getPreconditioningScheduleState?.let {
            visitor.visitSingularMessageField(it, 11)
        }
        this._getTirePressureState?.let {
            visitor.visitSingularMessageField(it, 14)
        }
        this._getMediaState?.let {
            visitor.visitSingularMessageField(it, 15)
        }
        this._getMediaDetailState?.let {
            visitor.visitSingularMessageField(it, 16)
        }
        this._getSoftwareUpdateState?.let {
            visitor.visitSingularMessageField(it, 17)
        }
        this._getParentalControlsState?.let {
            visitor.visitSingularMessageField(it, 19)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_GetVehicleData) return false
        if (this._getChargeState != other._getChargeState) return false
        if (this._getClimateState != other._getClimateState) return false
        if (this._getDriveState != other._getDriveState) return false
        if (this._getLocationState != other._getLocationState) return false
        if (this._getClosuresState != other._getClosuresState) return false
        if (this._getChargeScheduleState != other._getChargeScheduleState) return false
        if (this._getPreconditioningScheduleState != other._getPreconditioningScheduleState) return false
        if (this._getTirePressureState != other._getTirePressureState) return false
        if (this._getMediaState != other._getMediaState) return false
        if (this._getMediaDetailState != other._getMediaDetailState) return false
        if (this._getSoftwareUpdateState != other._getSoftwareUpdateState) return false
        if (this._getParentalControlsState != other._getParentalControlsState) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._getChargeState?.hashCode() ?: 0)
        hash = 31 * hash + (this._getClimateState?.hashCode() ?: 0)
        hash = 31 * hash + (this._getDriveState?.hashCode() ?: 0)
        hash = 31 * hash + (this._getLocationState?.hashCode() ?: 0)
        hash = 31 * hash + (this._getClosuresState?.hashCode() ?: 0)
        hash = 31 * hash + (this._getChargeScheduleState?.hashCode() ?: 0)
        hash = 31 * hash + (this._getPreconditioningScheduleState?.hashCode() ?: 0)
        hash = 31 * hash + (this._getTirePressureState?.hashCode() ?: 0)
        hash = 31 * hash + (this._getMediaState?.hashCode() ?: 0)
        hash = 31 * hash + (this._getMediaDetailState?.hashCode() ?: 0)
        hash = 31 * hash + (this._getSoftwareUpdateState?.hashCode() ?: 0)
        hash = 31 * hash + (this._getParentalControlsState?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_GetVehicleData {
        val result = CarServer_GetVehicleData()
        result._getChargeState = this._getChargeState?.copy()
        result._getClimateState = this._getClimateState?.copy()
        result._getDriveState = this._getDriveState?.copy()
        result._getLocationState = this._getLocationState?.copy()
        result._getClosuresState = this._getClosuresState?.copy()
        result._getChargeScheduleState = this._getChargeScheduleState?.copy()
        result._getPreconditioningScheduleState = this._getPreconditioningScheduleState?.copy()
        result._getTirePressureState = this._getTirePressureState?.copy()
        result._getMediaState = this._getMediaState?.copy()
        result._getMediaDetailState = this._getMediaDetailState?.copy()
        result._getSoftwareUpdateState = this._getSoftwareUpdateState?.copy()
        result._getParentalControlsState = this._getParentalControlsState?.copy()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.GetVehicleData"

        fun with(block: CarServer_GetVehicleData.() -> Unit): CarServer_GetVehicleData =
            CarServer_GetVehicleData().apply(block)
    }
}

class CarServer_GetTirePressureState() : GeneratedMessage() {
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
        if (other !is CarServer_GetTirePressureState) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_GetTirePressureState {
        val result = CarServer_GetTirePressureState()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.GetTirePressureState"

        fun with(block: CarServer_GetTirePressureState.() -> Unit): CarServer_GetTirePressureState =
            CarServer_GetTirePressureState().apply(block)
    }
}

class CarServer_GetMediaState() : GeneratedMessage() {
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
        if (other !is CarServer_GetMediaState) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_GetMediaState {
        val result = CarServer_GetMediaState()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.GetMediaState"

        fun with(block: CarServer_GetMediaState.() -> Unit): CarServer_GetMediaState =
            CarServer_GetMediaState().apply(block)
    }
}

class CarServer_GetMediaDetailState() : GeneratedMessage() {
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
        if (other !is CarServer_GetMediaDetailState) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_GetMediaDetailState {
        val result = CarServer_GetMediaDetailState()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.GetMediaDetailState"

        fun with(block: CarServer_GetMediaDetailState.() -> Unit): CarServer_GetMediaDetailState =
            CarServer_GetMediaDetailState().apply(block)
    }
}

class CarServer_GetSoftwareUpdateState() : GeneratedMessage() {
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
        if (other !is CarServer_GetSoftwareUpdateState) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_GetSoftwareUpdateState {
        val result = CarServer_GetSoftwareUpdateState()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.GetSoftwareUpdateState"

        fun with(block: CarServer_GetSoftwareUpdateState.() -> Unit): CarServer_GetSoftwareUpdateState =
            CarServer_GetSoftwareUpdateState().apply(block)
    }
}

class CarServer_GetChargeState() : GeneratedMessage() {
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
        if (other !is CarServer_GetChargeState) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_GetChargeState {
        val result = CarServer_GetChargeState()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.GetChargeState"

        fun with(block: CarServer_GetChargeState.() -> Unit): CarServer_GetChargeState =
            CarServer_GetChargeState().apply(block)
    }
}

class CarServer_GetClimateState() : GeneratedMessage() {
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
        if (other !is CarServer_GetClimateState) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_GetClimateState {
        val result = CarServer_GetClimateState()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.GetClimateState"

        fun with(block: CarServer_GetClimateState.() -> Unit): CarServer_GetClimateState =
            CarServer_GetClimateState().apply(block)
    }
}

class CarServer_GetDriveState() : GeneratedMessage() {
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
        if (other !is CarServer_GetDriveState) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_GetDriveState {
        val result = CarServer_GetDriveState()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.GetDriveState"

        fun with(block: CarServer_GetDriveState.() -> Unit): CarServer_GetDriveState =
            CarServer_GetDriveState().apply(block)
    }
}

class CarServer_GetLocationState() : GeneratedMessage() {
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
        if (other !is CarServer_GetLocationState) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_GetLocationState {
        val result = CarServer_GetLocationState()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.GetLocationState"

        fun with(block: CarServer_GetLocationState.() -> Unit): CarServer_GetLocationState =
            CarServer_GetLocationState().apply(block)
    }
}

class CarServer_GetClosuresState() : GeneratedMessage() {
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
        if (other !is CarServer_GetClosuresState) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_GetClosuresState {
        val result = CarServer_GetClosuresState()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.GetClosuresState"

        fun with(block: CarServer_GetClosuresState.() -> Unit): CarServer_GetClosuresState =
            CarServer_GetClosuresState().apply(block)
    }
}

class CarServer_GetChargeScheduleState() : GeneratedMessage() {
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
        if (other !is CarServer_GetChargeScheduleState) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_GetChargeScheduleState {
        val result = CarServer_GetChargeScheduleState()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.GetChargeScheduleState"

        fun with(block: CarServer_GetChargeScheduleState.() -> Unit): CarServer_GetChargeScheduleState =
            CarServer_GetChargeScheduleState().apply(block)
    }
}

class CarServer_GetPreconditioningScheduleState() : GeneratedMessage() {
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
        if (other !is CarServer_GetPreconditioningScheduleState) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_GetPreconditioningScheduleState {
        val result = CarServer_GetPreconditioningScheduleState()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.GetPreconditioningScheduleState"

        fun with(block: CarServer_GetPreconditioningScheduleState.() -> Unit): CarServer_GetPreconditioningScheduleState =
            CarServer_GetPreconditioningScheduleState().apply(block)
    }
}

class CarServer_GetParentalControlsState() : GeneratedMessage() {
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
        if (other !is CarServer_GetParentalControlsState) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_GetParentalControlsState {
        val result = CarServer_GetParentalControlsState()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.GetParentalControlsState"

        fun with(block: CarServer_GetParentalControlsState.() -> Unit): CarServer_GetParentalControlsState =
            CarServer_GetParentalControlsState().apply(block)
    }
}

class CarServer_EraseUserDataAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var reason: String = ""
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularStringField()?.let { this.reason = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.reason.isNotEmpty()) {
            visitor.visitSingularStringField(this.reason, 1)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_EraseUserDataAction) return false
        if (this.reason != other.reason) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.reason.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_EraseUserDataAction {
        val result = CarServer_EraseUserDataAction()
        result.reason = this.reason
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.EraseUserDataAction"

        fun with(block: CarServer_EraseUserDataAction.() -> Unit): CarServer_EraseUserDataAction =
            CarServer_EraseUserDataAction().apply(block)
    }
}

class CarServer_Response() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var actionStatus: CarServer_ActionStatus
        get() = this._actionStatus ?: _protobufPending(1, { CarServer_ActionStatus() }) { this._actionStatus = it }
        set(value) {
            _protobufDropPending(1)
            this._actionStatus = value.copy()
            _protobufMutated()
        }

    val hasActionStatus: Boolean
        get() = this._actionStatus != null

    fun clearActionStatus() {
        _protobufDropPending(1)
        this._actionStatus = null
        _protobufMutated()
    }

    var responseMsg: CarServer_Response.OneOf_ResponseMsg?
        get() = this._responseMsg
        set(value) {
            this._protobufStore_responseMsg(this._protobufCopy_responseMsg(value))
            _protobufMutated()
        }

    var vehicleData: CarServer_VehicleData
        get() = (this._responseMsg as? CarServer_Response.OneOf_ResponseMsg.vehicleData)?.value
            ?: _protobufPending(2, { CarServer_VehicleData() }) { this._protobufStore_responseMsg(CarServer_Response.OneOf_ResponseMsg.vehicleData(it)) }
        set(value) {
            this.responseMsg = CarServer_Response.OneOf_ResponseMsg.vehicleData(value)
        }

    var getSessionInfoResponse: Signatures_SessionInfo
        get() = (this._responseMsg as? CarServer_Response.OneOf_ResponseMsg.getSessionInfoResponse)?.value
            ?: _protobufPending(3, { Signatures_SessionInfo() }) { this._protobufStore_responseMsg(CarServer_Response.OneOf_ResponseMsg.getSessionInfoResponse(it)) }
        set(value) {
            this.responseMsg = CarServer_Response.OneOf_ResponseMsg.getSessionInfoResponse(value)
        }

    var getNearbyChargingSites: CarServer_NearbyChargingSites
        get() = (this._responseMsg as? CarServer_Response.OneOf_ResponseMsg.getNearbyChargingSites)?.value
            ?: _protobufPending(5, { CarServer_NearbyChargingSites() }) { this._protobufStore_responseMsg(CarServer_Response.OneOf_ResponseMsg.getNearbyChargingSites(it)) }
        set(value) {
            this.responseMsg = CarServer_Response.OneOf_ResponseMsg.getNearbyChargingSites(value)
        }

    var ping: CarServer_Ping
        get() = (this._responseMsg as? CarServer_Response.OneOf_ResponseMsg.ping)?.value
            ?: _protobufPending(9, { CarServer_Ping() }) { this._protobufStore_responseMsg(CarServer_Response.OneOf_ResponseMsg.ping(it)) }
        set(value) {
            this.responseMsg = CarServer_Response.OneOf_ResponseMsg.ping(value)
        }

    sealed class OneOf_ResponseMsg(value: Any) : ProtobufOneofCase(value) {
        class vehicleData(val value: CarServer_VehicleData) : OneOf_ResponseMsg(value)
        class getSessionInfoResponse(val value: Signatures_SessionInfo) : OneOf_ResponseMsg(value)
        class getNearbyChargingSites(val value: CarServer_NearbyChargingSites) : OneOf_ResponseMsg(value)
        class ping(val value: CarServer_Ping) : OneOf_ResponseMsg(value)
    }

    private var _actionStatus: CarServer_ActionStatus? = null
    private var _responseMsg: CarServer_Response.OneOf_ResponseMsg? = null

    private fun _protobufStore_responseMsg(value: CarServer_Response.OneOf_ResponseMsg?) {
        _protobufDropPending(2)
        _protobufDropPending(3)
        _protobufDropPending(5)
        _protobufDropPending(9)
        this._responseMsg = value
    }

    private fun _protobufCopy_responseMsg(value: CarServer_Response.OneOf_ResponseMsg?): CarServer_Response.OneOf_ResponseMsg? =
        when (value) {
            is CarServer_Response.OneOf_ResponseMsg.vehicleData -> CarServer_Response.OneOf_ResponseMsg.vehicleData(value.value.copy())
            is CarServer_Response.OneOf_ResponseMsg.getSessionInfoResponse -> CarServer_Response.OneOf_ResponseMsg.getSessionInfoResponse(value.value.copy())
            is CarServer_Response.OneOf_ResponseMsg.getNearbyChargingSites -> CarServer_Response.OneOf_ResponseMsg.getNearbyChargingSites(value.value.copy())
            is CarServer_Response.OneOf_ResponseMsg.ping -> CarServer_Response.OneOf_ResponseMsg.ping(value.value.copy())
            else -> value
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularMessageField(this._actionStatus) { CarServer_ActionStatus() }?.let {
                    _protobufDropPending(1)
                    this._actionStatus = it
                }
                2 -> decoder.decodeSingularMessageField((this._responseMsg as? CarServer_Response.OneOf_ResponseMsg.vehicleData)?.value) { CarServer_VehicleData() }
                    ?.let { this._protobufStore_responseMsg(CarServer_Response.OneOf_ResponseMsg.vehicleData(it)) }
                3 -> decoder.decodeSingularMessageField((this._responseMsg as? CarServer_Response.OneOf_ResponseMsg.getSessionInfoResponse)?.value) { Signatures_SessionInfo() }
                    ?.let { this._protobufStore_responseMsg(CarServer_Response.OneOf_ResponseMsg.getSessionInfoResponse(it)) }
                5 -> decoder.decodeSingularMessageField((this._responseMsg as? CarServer_Response.OneOf_ResponseMsg.getNearbyChargingSites)?.value) { CarServer_NearbyChargingSites() }
                    ?.let { this._protobufStore_responseMsg(CarServer_Response.OneOf_ResponseMsg.getNearbyChargingSites(it)) }
                9 -> decoder.decodeSingularMessageField((this._responseMsg as? CarServer_Response.OneOf_ResponseMsg.ping)?.value) { CarServer_Ping() }
                    ?.let { this._protobufStore_responseMsg(CarServer_Response.OneOf_ResponseMsg.ping(it)) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        this._actionStatus?.let {
            visitor.visitSingularMessageField(it, 1)
        }
        (this._responseMsg as? CarServer_Response.OneOf_ResponseMsg.vehicleData)?.let {
            visitor.visitSingularMessageField(it.value, 2)
        }
        (this._responseMsg as? CarServer_Response.OneOf_ResponseMsg.getSessionInfoResponse)?.let {
            visitor.visitSingularMessageField(it.value, 3)
        }
        (this._responseMsg as? CarServer_Response.OneOf_ResponseMsg.getNearbyChargingSites)?.let {
            visitor.visitSingularMessageField(it.value, 5)
        }
        (this._responseMsg as? CarServer_Response.OneOf_ResponseMsg.ping)?.let {
            visitor.visitSingularMessageField(it.value, 9)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_Response) return false
        if (this._actionStatus != other._actionStatus) return false
        if (this._responseMsg != other._responseMsg) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._actionStatus?.hashCode() ?: 0)
        hash = 31 * hash + (this._responseMsg?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_Response {
        val result = CarServer_Response()
        result._actionStatus = this._actionStatus?.copy()
        result._responseMsg = this._protobufCopy_responseMsg(this._responseMsg)
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.Response"

        fun with(block: CarServer_Response.() -> Unit): CarServer_Response =
            CarServer_Response().apply(block)
    }
}

class CarServer_ActionStatus() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var result: CarServer_OperationStatus_E = CarServer_OperationStatus_E.operationstatusOk
        set(value) {
            field = value
            _protobufForgetUnrecognized(1)
            _protobufMutated()
        }

    var resultReason: CarServer_ResultReason
        get() = this._resultReason ?: _protobufPending(2, { CarServer_ResultReason() }) { this._resultReason = it }
        set(value) {
            _protobufDropPending(2)
            this._resultReason = value.copy()
            _protobufMutated()
        }

    val hasResultReason: Boolean
        get() = this._resultReason != null

    fun clearResultReason() {
        _protobufDropPending(2)
        this._resultReason = null
        _protobufMutated()
    }

    private var _resultReason: CarServer_ResultReason? = null

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularOpenEnumField(CarServer_OperationStatus_E.UNRECOGNIZED) { CarServer_OperationStatus_E.fromRawValue(it) }
                    ?.let { this.result = it }
                2 -> decoder.decodeSingularMessageField(this._resultReason) { CarServer_ResultReason() }?.let {
                    _protobufDropPending(2)
                    this._resultReason = it
                }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.result != CarServer_OperationStatus_E.operationstatusOk && this.result != CarServer_OperationStatus_E.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.result.rawValue, 1)
        }
        this._resultReason?.let {
            visitor.visitSingularMessageField(it, 2)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_ActionStatus) return false
        if (this.result != other.result) return false
        if (this._resultReason != other._resultReason) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.result.hashCode()
        hash = 31 * hash + (this._resultReason?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ActionStatus {
        val result = CarServer_ActionStatus()
        result.result = this.result
        result._resultReason = this._resultReason?.copy()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ActionStatus"

        fun with(block: CarServer_ActionStatus.() -> Unit): CarServer_ActionStatus =
            CarServer_ActionStatus().apply(block)
    }
}

class CarServer_ResultReason() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var reason: CarServer_ResultReason.OneOf_Reason?
        get() = this._reason
        set(value) {
            this._reason = value
            _protobufMutated()
        }

    var plainText: String
        get() = (this._reason as? CarServer_ResultReason.OneOf_Reason.plainText)?.value ?: ""
        set(value) {
            this.reason = CarServer_ResultReason.OneOf_Reason.plainText(value)
        }

    sealed class OneOf_Reason(value: Any) : ProtobufOneofCase(value) {
        class plainText(val value: String) : OneOf_Reason(value)
    }

    private var _reason: CarServer_ResultReason.OneOf_Reason? = null

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularStringField()?.let { this._reason = CarServer_ResultReason.OneOf_Reason.plainText(it) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        (this._reason as? CarServer_ResultReason.OneOf_Reason.plainText)?.let {
            visitor.visitSingularStringField(it.value, 1)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_ResultReason) return false
        if (this._reason != other._reason) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._reason?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ResultReason {
        val result = CarServer_ResultReason()
        result._reason = this._reason
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ResultReason"

        fun with(block: CarServer_ResultReason.() -> Unit): CarServer_ResultReason =
            CarServer_ResultReason().apply(block)
    }
}

class CarServer_EncryptedData() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var fieldNumber: Int = 0
        set(value) {
            field = value
            _protobufMutated()
        }

    var ciphertext: ByteArray = ByteArray(0)
        set(value) {
            field = value
            _protobufMutated()
        }

    var tag: ByteArray = ByteArray(0)
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularInt32Field()?.let { this.fieldNumber = it }
                2 -> decoder.decodeSingularBytesField()?.let { this.ciphertext = it }
                3 -> decoder.decodeSingularBytesField()?.let { this.tag = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.fieldNumber != 0) {
            visitor.visitSingularInt32Field(this.fieldNumber, 1)
        }
        if (this.ciphertext.isNotEmpty()) {
            visitor.visitSingularBytesField(this.ciphertext, 2)
        }
        if (this.tag.isNotEmpty()) {
            visitor.visitSingularBytesField(this.tag, 3)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_EncryptedData) return false
        if (this.fieldNumber != other.fieldNumber) return false
        if (!this.ciphertext.contentEquals(other.ciphertext)) return false
        if (!this.tag.contentEquals(other.tag)) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.fieldNumber.hashCode()
        hash = 31 * hash + this.ciphertext.contentHashCode()
        hash = 31 * hash + this.tag.contentHashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_EncryptedData {
        val result = CarServer_EncryptedData()
        result.fieldNumber = this.fieldNumber
        result.ciphertext = this.ciphertext
        result.tag = this.tag
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.EncryptedData"

        fun with(block: CarServer_EncryptedData.() -> Unit): CarServer_EncryptedData =
            CarServer_EncryptedData().apply(block)
    }
}

class CarServer_ChargingSetLimitAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var percent: Int = 0
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularInt32Field()?.let { this.percent = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.percent != 0) {
            visitor.visitSingularInt32Field(this.percent, 1)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_ChargingSetLimitAction) return false
        if (this.percent != other.percent) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.percent.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ChargingSetLimitAction {
        val result = CarServer_ChargingSetLimitAction()
        result.percent = this.percent
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ChargingSetLimitAction"

        fun with(block: CarServer_ChargingSetLimitAction.() -> Unit): CarServer_ChargingSetLimitAction =
            CarServer_ChargingSetLimitAction().apply(block)
    }
}

class CarServer_ChargingStartStopAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var chargingAction: CarServer_ChargingStartStopAction.OneOf_ChargingAction?
        get() = this._chargingAction
        set(value) {
            this._protobufStore_chargingAction(this._protobufCopy_chargingAction(value))
            _protobufMutated()
        }

    var unknown: CarServer_Void
        get() = (this._chargingAction as? CarServer_ChargingStartStopAction.OneOf_ChargingAction.unknown)?.value
            ?: _protobufPending(1, { CarServer_Void() }) { this._protobufStore_chargingAction(CarServer_ChargingStartStopAction.OneOf_ChargingAction.unknown(it)) }
        set(value) {
            this.chargingAction = CarServer_ChargingStartStopAction.OneOf_ChargingAction.unknown(value)
        }

    var start: CarServer_Void
        get() = (this._chargingAction as? CarServer_ChargingStartStopAction.OneOf_ChargingAction.start)?.value
            ?: _protobufPending(2, { CarServer_Void() }) { this._protobufStore_chargingAction(CarServer_ChargingStartStopAction.OneOf_ChargingAction.start(it)) }
        set(value) {
            this.chargingAction = CarServer_ChargingStartStopAction.OneOf_ChargingAction.start(value)
        }

    var startStandard: CarServer_Void
        get() = (this._chargingAction as? CarServer_ChargingStartStopAction.OneOf_ChargingAction.startStandard)?.value
            ?: _protobufPending(3, { CarServer_Void() }) { this._protobufStore_chargingAction(CarServer_ChargingStartStopAction.OneOf_ChargingAction.startStandard(it)) }
        set(value) {
            this.chargingAction = CarServer_ChargingStartStopAction.OneOf_ChargingAction.startStandard(value)
        }

    var startMaxRange: CarServer_Void
        get() = (this._chargingAction as? CarServer_ChargingStartStopAction.OneOf_ChargingAction.startMaxRange)?.value
            ?: _protobufPending(4, { CarServer_Void() }) { this._protobufStore_chargingAction(CarServer_ChargingStartStopAction.OneOf_ChargingAction.startMaxRange(it)) }
        set(value) {
            this.chargingAction = CarServer_ChargingStartStopAction.OneOf_ChargingAction.startMaxRange(value)
        }

    var stop: CarServer_Void
        get() = (this._chargingAction as? CarServer_ChargingStartStopAction.OneOf_ChargingAction.stop)?.value
            ?: _protobufPending(5, { CarServer_Void() }) { this._protobufStore_chargingAction(CarServer_ChargingStartStopAction.OneOf_ChargingAction.stop(it)) }
        set(value) {
            this.chargingAction = CarServer_ChargingStartStopAction.OneOf_ChargingAction.stop(value)
        }

    sealed class OneOf_ChargingAction(value: Any) : ProtobufOneofCase(value) {
        class unknown(val value: CarServer_Void) : OneOf_ChargingAction(value)
        class start(val value: CarServer_Void) : OneOf_ChargingAction(value)
        class startStandard(val value: CarServer_Void) : OneOf_ChargingAction(value)
        class startMaxRange(val value: CarServer_Void) : OneOf_ChargingAction(value)
        class stop(val value: CarServer_Void) : OneOf_ChargingAction(value)
    }

    private var _chargingAction: CarServer_ChargingStartStopAction.OneOf_ChargingAction? = null

    private fun _protobufStore_chargingAction(value: CarServer_ChargingStartStopAction.OneOf_ChargingAction?) {
        _protobufDropPending(1)
        _protobufDropPending(2)
        _protobufDropPending(3)
        _protobufDropPending(4)
        _protobufDropPending(5)
        this._chargingAction = value
    }

    private fun _protobufCopy_chargingAction(value: CarServer_ChargingStartStopAction.OneOf_ChargingAction?): CarServer_ChargingStartStopAction.OneOf_ChargingAction? =
        when (value) {
            is CarServer_ChargingStartStopAction.OneOf_ChargingAction.unknown -> CarServer_ChargingStartStopAction.OneOf_ChargingAction.unknown(value.value.copy())
            is CarServer_ChargingStartStopAction.OneOf_ChargingAction.start -> CarServer_ChargingStartStopAction.OneOf_ChargingAction.start(value.value.copy())
            is CarServer_ChargingStartStopAction.OneOf_ChargingAction.startStandard -> CarServer_ChargingStartStopAction.OneOf_ChargingAction.startStandard(value.value.copy())
            is CarServer_ChargingStartStopAction.OneOf_ChargingAction.startMaxRange -> CarServer_ChargingStartStopAction.OneOf_ChargingAction.startMaxRange(value.value.copy())
            is CarServer_ChargingStartStopAction.OneOf_ChargingAction.stop -> CarServer_ChargingStartStopAction.OneOf_ChargingAction.stop(value.value.copy())
            else -> value
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularMessageField((this._chargingAction as? CarServer_ChargingStartStopAction.OneOf_ChargingAction.unknown)?.value) { CarServer_Void() }
                    ?.let { this._protobufStore_chargingAction(CarServer_ChargingStartStopAction.OneOf_ChargingAction.unknown(it)) }
                2 -> decoder.decodeSingularMessageField((this._chargingAction as? CarServer_ChargingStartStopAction.OneOf_ChargingAction.start)?.value) { CarServer_Void() }
                    ?.let { this._protobufStore_chargingAction(CarServer_ChargingStartStopAction.OneOf_ChargingAction.start(it)) }
                3 -> decoder.decodeSingularMessageField((this._chargingAction as? CarServer_ChargingStartStopAction.OneOf_ChargingAction.startStandard)?.value) { CarServer_Void() }
                    ?.let { this._protobufStore_chargingAction(CarServer_ChargingStartStopAction.OneOf_ChargingAction.startStandard(it)) }
                4 -> decoder.decodeSingularMessageField((this._chargingAction as? CarServer_ChargingStartStopAction.OneOf_ChargingAction.startMaxRange)?.value) { CarServer_Void() }
                    ?.let { this._protobufStore_chargingAction(CarServer_ChargingStartStopAction.OneOf_ChargingAction.startMaxRange(it)) }
                5 -> decoder.decodeSingularMessageField((this._chargingAction as? CarServer_ChargingStartStopAction.OneOf_ChargingAction.stop)?.value) { CarServer_Void() }
                    ?.let { this._protobufStore_chargingAction(CarServer_ChargingStartStopAction.OneOf_ChargingAction.stop(it)) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        (this._chargingAction as? CarServer_ChargingStartStopAction.OneOf_ChargingAction.unknown)?.let {
            visitor.visitSingularMessageField(it.value, 1)
        }
        (this._chargingAction as? CarServer_ChargingStartStopAction.OneOf_ChargingAction.start)?.let {
            visitor.visitSingularMessageField(it.value, 2)
        }
        (this._chargingAction as? CarServer_ChargingStartStopAction.OneOf_ChargingAction.startStandard)?.let {
            visitor.visitSingularMessageField(it.value, 3)
        }
        (this._chargingAction as? CarServer_ChargingStartStopAction.OneOf_ChargingAction.startMaxRange)?.let {
            visitor.visitSingularMessageField(it.value, 4)
        }
        (this._chargingAction as? CarServer_ChargingStartStopAction.OneOf_ChargingAction.stop)?.let {
            visitor.visitSingularMessageField(it.value, 5)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_ChargingStartStopAction) return false
        if (this._chargingAction != other._chargingAction) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._chargingAction?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ChargingStartStopAction {
        val result = CarServer_ChargingStartStopAction()
        result._chargingAction = this._protobufCopy_chargingAction(this._chargingAction)
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ChargingStartStopAction"

        fun with(block: CarServer_ChargingStartStopAction.() -> Unit): CarServer_ChargingStartStopAction =
            CarServer_ChargingStartStopAction().apply(block)
    }
}

class CarServer_DrivingClearSpeedLimitPinAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var pin: String = ""
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularStringField()?.let { this.pin = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.pin.isNotEmpty()) {
            visitor.visitSingularStringField(this.pin, 1)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_DrivingClearSpeedLimitPinAction) return false
        if (this.pin != other.pin) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.pin.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_DrivingClearSpeedLimitPinAction {
        val result = CarServer_DrivingClearSpeedLimitPinAction()
        result.pin = this.pin
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.DrivingClearSpeedLimitPinAction"

        fun with(block: CarServer_DrivingClearSpeedLimitPinAction.() -> Unit): CarServer_DrivingClearSpeedLimitPinAction =
            CarServer_DrivingClearSpeedLimitPinAction().apply(block)
    }
}

class CarServer_DrivingClearSpeedLimitPinAdminAction() : GeneratedMessage() {
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
        if (other !is CarServer_DrivingClearSpeedLimitPinAdminAction) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_DrivingClearSpeedLimitPinAdminAction {
        val result = CarServer_DrivingClearSpeedLimitPinAdminAction()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.DrivingClearSpeedLimitPinAdminAction"

        fun with(block: CarServer_DrivingClearSpeedLimitPinAdminAction.() -> Unit): CarServer_DrivingClearSpeedLimitPinAdminAction =
            CarServer_DrivingClearSpeedLimitPinAdminAction().apply(block)
    }
}

class CarServer_DrivingSetSpeedLimitAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var limitMph: Double = 0.0
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularDoubleField()?.let { this.limitMph = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.limitMph.toRawBits() != 0L) {
            visitor.visitSingularDoubleField(this.limitMph, 1)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_DrivingSetSpeedLimitAction) return false
        if (this.limitMph != other.limitMph) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + protobufHash(this.limitMph)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_DrivingSetSpeedLimitAction {
        val result = CarServer_DrivingSetSpeedLimitAction()
        result.limitMph = this.limitMph
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.DrivingSetSpeedLimitAction"

        fun with(block: CarServer_DrivingSetSpeedLimitAction.() -> Unit): CarServer_DrivingSetSpeedLimitAction =
            CarServer_DrivingSetSpeedLimitAction().apply(block)
    }
}

class CarServer_DrivingSpeedLimitAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var activate: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    var pin: String = ""
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularBoolField()?.let { this.activate = it }
                2 -> decoder.decodeSingularStringField()?.let { this.pin = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.activate) {
            visitor.visitSingularBoolField(this.activate, 1)
        }
        if (this.pin.isNotEmpty()) {
            visitor.visitSingularStringField(this.pin, 2)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_DrivingSpeedLimitAction) return false
        if (this.activate != other.activate) return false
        if (this.pin != other.pin) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.activate.hashCode()
        hash = 31 * hash + this.pin.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_DrivingSpeedLimitAction {
        val result = CarServer_DrivingSpeedLimitAction()
        result.activate = this.activate
        result.pin = this.pin
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.DrivingSpeedLimitAction"

        fun with(block: CarServer_DrivingSpeedLimitAction.() -> Unit): CarServer_DrivingSpeedLimitAction =
            CarServer_DrivingSpeedLimitAction().apply(block)
    }
}

class CarServer_HvacAutoAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var powerOn: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    var manualOverride: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularBoolField()?.let { this.powerOn = it }
                2 -> decoder.decodeSingularBoolField()?.let { this.manualOverride = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.powerOn) {
            visitor.visitSingularBoolField(this.powerOn, 1)
        }
        if (this.manualOverride) {
            visitor.visitSingularBoolField(this.manualOverride, 2)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_HvacAutoAction) return false
        if (this.powerOn != other.powerOn) return false
        if (this.manualOverride != other.manualOverride) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.powerOn.hashCode()
        hash = 31 * hash + this.manualOverride.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_HvacAutoAction {
        val result = CarServer_HvacAutoAction()
        result.powerOn = this.powerOn
        result.manualOverride = this.manualOverride
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.HvacAutoAction"

        fun with(block: CarServer_HvacAutoAction.() -> Unit): CarServer_HvacAutoAction =
            CarServer_HvacAutoAction().apply(block)
    }
}

class CarServer_HvacSeatHeaterActions() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var hvacSeatHeaterAction: MutableList<CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction>
        get() = this._hvacSeatHeaterAction
        set(value) {
            this._hvacSeatHeaterAction = ProtobufList<CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction>(this) { it.copy() }.apply { addAll(value) }
            _protobufMutated()
        }

    class HvacSeatHeaterAction() : GeneratedMessage() {
        constructor(serializedBytes: ByteArray) : this() {
            merge(serializedBytes)
        }

        var seatHeaterLevel: CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel?
            get() = this._seatHeaterLevel
            set(value) {
                this._protobufStore_seatHeaterLevel(this._protobufCopy_seatHeaterLevel(value))
                _protobufMutated()
            }

        var seatHeaterUnknown: CarServer_Void
            get() = (this._seatHeaterLevel as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterUnknown)?.value
                ?: _protobufPending(1, { CarServer_Void() }) { this._protobufStore_seatHeaterLevel(CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterUnknown(it)) }
            set(value) {
                this.seatHeaterLevel = CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterUnknown(value)
            }

        var seatHeaterOff: CarServer_Void
            get() = (this._seatHeaterLevel as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterOff)?.value
                ?: _protobufPending(2, { CarServer_Void() }) { this._protobufStore_seatHeaterLevel(CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterOff(it)) }
            set(value) {
                this.seatHeaterLevel = CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterOff(value)
            }

        var seatHeaterLow: CarServer_Void
            get() = (this._seatHeaterLevel as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterLow)?.value
                ?: _protobufPending(3, { CarServer_Void() }) { this._protobufStore_seatHeaterLevel(CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterLow(it)) }
            set(value) {
                this.seatHeaterLevel = CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterLow(value)
            }

        var seatHeaterMed: CarServer_Void
            get() = (this._seatHeaterLevel as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterMed)?.value
                ?: _protobufPending(4, { CarServer_Void() }) { this._protobufStore_seatHeaterLevel(CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterMed(it)) }
            set(value) {
                this.seatHeaterLevel = CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterMed(value)
            }

        var seatHeaterHigh: CarServer_Void
            get() = (this._seatHeaterLevel as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterHigh)?.value
                ?: _protobufPending(5, { CarServer_Void() }) { this._protobufStore_seatHeaterLevel(CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterHigh(it)) }
            set(value) {
                this.seatHeaterLevel = CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterHigh(value)
            }

        var seatPosition: CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition?
            get() = this._seatPosition
            set(value) {
                this._protobufStore_seatPosition(this._protobufCopy_seatPosition(value))
                _protobufMutated()
            }

        var carSeatUnknown: CarServer_Void
            get() = (this._seatPosition as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatUnknown)?.value
                ?: _protobufPending(6, { CarServer_Void() }) { this._protobufStore_seatPosition(CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatUnknown(it)) }
            set(value) {
                this.seatPosition = CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatUnknown(value)
            }

        var carSeatFrontLeft: CarServer_Void
            get() = (this._seatPosition as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatFrontLeft)?.value
                ?: _protobufPending(7, { CarServer_Void() }) { this._protobufStore_seatPosition(CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatFrontLeft(it)) }
            set(value) {
                this.seatPosition = CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatFrontLeft(value)
            }

        var carSeatFrontRight: CarServer_Void
            get() = (this._seatPosition as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatFrontRight)?.value
                ?: _protobufPending(8, { CarServer_Void() }) { this._protobufStore_seatPosition(CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatFrontRight(it)) }
            set(value) {
                this.seatPosition = CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatFrontRight(value)
            }

        var carSeatRearLeft: CarServer_Void
            get() = (this._seatPosition as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearLeft)?.value
                ?: _protobufPending(9, { CarServer_Void() }) { this._protobufStore_seatPosition(CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearLeft(it)) }
            set(value) {
                this.seatPosition = CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearLeft(value)
            }

        var carSeatRearLeftBack: CarServer_Void
            get() = (this._seatPosition as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearLeftBack)?.value
                ?: _protobufPending(10, { CarServer_Void() }) { this._protobufStore_seatPosition(CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearLeftBack(it)) }
            set(value) {
                this.seatPosition = CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearLeftBack(value)
            }

        var carSeatRearCenter: CarServer_Void
            get() = (this._seatPosition as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearCenter)?.value
                ?: _protobufPending(11, { CarServer_Void() }) { this._protobufStore_seatPosition(CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearCenter(it)) }
            set(value) {
                this.seatPosition = CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearCenter(value)
            }

        var carSeatRearRight: CarServer_Void
            get() = (this._seatPosition as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearRight)?.value
                ?: _protobufPending(12, { CarServer_Void() }) { this._protobufStore_seatPosition(CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearRight(it)) }
            set(value) {
                this.seatPosition = CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearRight(value)
            }

        var carSeatRearRightBack: CarServer_Void
            get() = (this._seatPosition as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearRightBack)?.value
                ?: _protobufPending(13, { CarServer_Void() }) { this._protobufStore_seatPosition(CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearRightBack(it)) }
            set(value) {
                this.seatPosition = CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearRightBack(value)
            }

        var carSeatThirdRowLeft: CarServer_Void
            get() = (this._seatPosition as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatThirdRowLeft)?.value
                ?: _protobufPending(14, { CarServer_Void() }) { this._protobufStore_seatPosition(CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatThirdRowLeft(it)) }
            set(value) {
                this.seatPosition = CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatThirdRowLeft(value)
            }

        var carSeatThirdRowRight: CarServer_Void
            get() = (this._seatPosition as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatThirdRowRight)?.value
                ?: _protobufPending(15, { CarServer_Void() }) { this._protobufStore_seatPosition(CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatThirdRowRight(it)) }
            set(value) {
                this.seatPosition = CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatThirdRowRight(value)
            }

        sealed class OneOf_SeatHeaterLevel(value: Any) : ProtobufOneofCase(value) {
            class seatHeaterUnknown(val value: CarServer_Void) : OneOf_SeatHeaterLevel(value)
            class seatHeaterOff(val value: CarServer_Void) : OneOf_SeatHeaterLevel(value)
            class seatHeaterLow(val value: CarServer_Void) : OneOf_SeatHeaterLevel(value)
            class seatHeaterMed(val value: CarServer_Void) : OneOf_SeatHeaterLevel(value)
            class seatHeaterHigh(val value: CarServer_Void) : OneOf_SeatHeaterLevel(value)
        }

        sealed class OneOf_SeatPosition(value: Any) : ProtobufOneofCase(value) {
            class carSeatUnknown(val value: CarServer_Void) : OneOf_SeatPosition(value)
            class carSeatFrontLeft(val value: CarServer_Void) : OneOf_SeatPosition(value)
            class carSeatFrontRight(val value: CarServer_Void) : OneOf_SeatPosition(value)
            class carSeatRearLeft(val value: CarServer_Void) : OneOf_SeatPosition(value)
            class carSeatRearLeftBack(val value: CarServer_Void) : OneOf_SeatPosition(value)
            class carSeatRearCenter(val value: CarServer_Void) : OneOf_SeatPosition(value)
            class carSeatRearRight(val value: CarServer_Void) : OneOf_SeatPosition(value)
            class carSeatRearRightBack(val value: CarServer_Void) : OneOf_SeatPosition(value)
            class carSeatThirdRowLeft(val value: CarServer_Void) : OneOf_SeatPosition(value)
            class carSeatThirdRowRight(val value: CarServer_Void) : OneOf_SeatPosition(value)
        }

        private var _seatHeaterLevel: CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel? = null
        private var _seatPosition: CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition? = null

        private fun _protobufStore_seatHeaterLevel(value: CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel?) {
            _protobufDropPending(1)
            _protobufDropPending(2)
            _protobufDropPending(3)
            _protobufDropPending(4)
            _protobufDropPending(5)
            this._seatHeaterLevel = value
        }

        private fun _protobufCopy_seatHeaterLevel(value: CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel?): CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel? =
            when (value) {
                is CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterUnknown -> CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterUnknown(value.value.copy())
                is CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterOff -> CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterOff(value.value.copy())
                is CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterLow -> CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterLow(value.value.copy())
                is CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterMed -> CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterMed(value.value.copy())
                is CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterHigh -> CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterHigh(value.value.copy())
                else -> value
            }

        private fun _protobufStore_seatPosition(value: CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition?) {
            _protobufDropPending(6)
            _protobufDropPending(7)
            _protobufDropPending(8)
            _protobufDropPending(9)
            _protobufDropPending(10)
            _protobufDropPending(11)
            _protobufDropPending(12)
            _protobufDropPending(13)
            _protobufDropPending(14)
            _protobufDropPending(15)
            this._seatPosition = value
        }

        private fun _protobufCopy_seatPosition(value: CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition?): CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition? =
            when (value) {
                is CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatUnknown -> CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatUnknown(value.value.copy())
                is CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatFrontLeft -> CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatFrontLeft(value.value.copy())
                is CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatFrontRight -> CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatFrontRight(value.value.copy())
                is CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearLeft -> CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearLeft(value.value.copy())
                is CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearLeftBack -> CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearLeftBack(value.value.copy())
                is CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearCenter -> CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearCenter(value.value.copy())
                is CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearRight -> CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearRight(value.value.copy())
                is CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearRightBack -> CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearRightBack(value.value.copy())
                is CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatThirdRowLeft -> CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatThirdRowLeft(value.value.copy())
                is CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatThirdRowRight -> CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatThirdRowRight(value.value.copy())
                else -> value
            }

        override fun decodeMessage(decoder: BinaryDecoder) {
            while (true) {
                when (decoder.nextFieldNumber() ?: return) {
                    1 -> decoder.decodeSingularMessageField((this._seatHeaterLevel as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterUnknown)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_seatHeaterLevel(CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterUnknown(it)) }
                    2 -> decoder.decodeSingularMessageField((this._seatHeaterLevel as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterOff)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_seatHeaterLevel(CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterOff(it)) }
                    3 -> decoder.decodeSingularMessageField((this._seatHeaterLevel as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterLow)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_seatHeaterLevel(CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterLow(it)) }
                    4 -> decoder.decodeSingularMessageField((this._seatHeaterLevel as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterMed)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_seatHeaterLevel(CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterMed(it)) }
                    5 -> decoder.decodeSingularMessageField((this._seatHeaterLevel as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterHigh)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_seatHeaterLevel(CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterHigh(it)) }
                    6 -> decoder.decodeSingularMessageField((this._seatPosition as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatUnknown)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_seatPosition(CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatUnknown(it)) }
                    7 -> decoder.decodeSingularMessageField((this._seatPosition as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatFrontLeft)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_seatPosition(CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatFrontLeft(it)) }
                    8 -> decoder.decodeSingularMessageField((this._seatPosition as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatFrontRight)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_seatPosition(CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatFrontRight(it)) }
                    9 -> decoder.decodeSingularMessageField((this._seatPosition as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearLeft)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_seatPosition(CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearLeft(it)) }
                    10 -> decoder.decodeSingularMessageField((this._seatPosition as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearLeftBack)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_seatPosition(CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearLeftBack(it)) }
                    11 -> decoder.decodeSingularMessageField((this._seatPosition as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearCenter)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_seatPosition(CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearCenter(it)) }
                    12 -> decoder.decodeSingularMessageField((this._seatPosition as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearRight)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_seatPosition(CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearRight(it)) }
                    13 -> decoder.decodeSingularMessageField((this._seatPosition as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearRightBack)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_seatPosition(CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearRightBack(it)) }
                    14 -> decoder.decodeSingularMessageField((this._seatPosition as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatThirdRowLeft)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_seatPosition(CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatThirdRowLeft(it)) }
                    15 -> decoder.decodeSingularMessageField((this._seatPosition as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatThirdRowRight)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_seatPosition(CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatThirdRowRight(it)) }
                }
            }
        }

        override fun traverse(visitor: BinaryEncodingVisitor) {
            (this._seatHeaterLevel as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterUnknown)?.let {
                visitor.visitSingularMessageField(it.value, 1)
            }
            (this._seatHeaterLevel as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterOff)?.let {
                visitor.visitSingularMessageField(it.value, 2)
            }
            (this._seatHeaterLevel as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterLow)?.let {
                visitor.visitSingularMessageField(it.value, 3)
            }
            (this._seatHeaterLevel as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterMed)?.let {
                visitor.visitSingularMessageField(it.value, 4)
            }
            (this._seatHeaterLevel as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterHigh)?.let {
                visitor.visitSingularMessageField(it.value, 5)
            }
            (this._seatPosition as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatUnknown)?.let {
                visitor.visitSingularMessageField(it.value, 6)
            }
            (this._seatPosition as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatFrontLeft)?.let {
                visitor.visitSingularMessageField(it.value, 7)
            }
            (this._seatPosition as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatFrontRight)?.let {
                visitor.visitSingularMessageField(it.value, 8)
            }
            (this._seatPosition as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearLeft)?.let {
                visitor.visitSingularMessageField(it.value, 9)
            }
            (this._seatPosition as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearLeftBack)?.let {
                visitor.visitSingularMessageField(it.value, 10)
            }
            (this._seatPosition as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearCenter)?.let {
                visitor.visitSingularMessageField(it.value, 11)
            }
            (this._seatPosition as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearRight)?.let {
                visitor.visitSingularMessageField(it.value, 12)
            }
            (this._seatPosition as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatRearRightBack)?.let {
                visitor.visitSingularMessageField(it.value, 13)
            }
            (this._seatPosition as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatThirdRowLeft)?.let {
                visitor.visitSingularMessageField(it.value, 14)
            }
            (this._seatPosition as? CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatPosition.carSeatThirdRowRight)?.let {
                visitor.visitSingularMessageField(it.value, 15)
            }
            visitor.visitUnknown(unknownFields)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction) return false
            if (this._seatHeaterLevel != other._seatHeaterLevel) return false
            if (this._seatPosition != other._seatPosition) return false
            return this.unknownFields.contentEquals(other.unknownFields)
        }

        override fun hashCode(): Int {
            var hash = 0
            hash = 31 * hash + (this._seatHeaterLevel?.hashCode() ?: 0)
            hash = 31 * hash + (this._seatPosition?.hashCode() ?: 0)
            hash = 31 * hash + this.unknownFields.contentHashCode()
            return hash
        }

        override fun copy(): CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction {
            val result = CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction()
            result._seatHeaterLevel = this._protobufCopy_seatHeaterLevel(this._seatHeaterLevel)
            result._seatPosition = this._protobufCopy_seatPosition(this._seatPosition)
            result.unknownFields = this.unknownFields
            return result
        }

        companion object {
            const val protoMessageName: String = "CarServer.HvacSeatHeaterActions.HvacSeatHeaterAction"

            fun with(block: CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.() -> Unit): CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction =
                CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction().apply(block)
        }
    }

    private var _hvacSeatHeaterAction: ProtobufList<CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction> = ProtobufList<CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction>(this) { it.copy() }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeRepeatedMessageField(this._hvacSeatHeaterAction) { CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction() }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this._hvacSeatHeaterAction.isNotEmpty()) {
            visitor.visitRepeatedMessageField(this._hvacSeatHeaterAction, 1)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_HvacSeatHeaterActions) return false
        if (this._hvacSeatHeaterAction != other._hvacSeatHeaterAction) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this._hvacSeatHeaterAction.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_HvacSeatHeaterActions {
        val result = CarServer_HvacSeatHeaterActions()
        result._hvacSeatHeaterAction.addAll(this._hvacSeatHeaterAction)
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.HvacSeatHeaterActions"

        fun with(block: CarServer_HvacSeatHeaterActions.() -> Unit): CarServer_HvacSeatHeaterActions =
            CarServer_HvacSeatHeaterActions().apply(block)
    }
}

class CarServer_HvacSeatCoolerActions() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var hvacSeatCoolerAction: MutableList<CarServer_HvacSeatCoolerActions.HvacSeatCoolerAction>
        get() = this._hvacSeatCoolerAction
        set(value) {
            this._hvacSeatCoolerAction = ProtobufList<CarServer_HvacSeatCoolerActions.HvacSeatCoolerAction>(this) { it.copy() }.apply { addAll(value) }
            _protobufMutated()
        }

    enum class HvacSeatCoolerLevel_E(val rawValue: Int) {
        hvacSeatCoolerLevelUnknown(0),
        hvacSeatCoolerLevelOff(1),
        hvacSeatCoolerLevelLow(2),
        hvacSeatCoolerLevelMed(3),
        hvacSeatCoolerLevelHigh(4),
        UNRECOGNIZED(-1),
        ;

        companion object {
            val allCases: List<CarServer_HvacSeatCoolerActions.HvacSeatCoolerLevel_E> = listOf(
                hvacSeatCoolerLevelUnknown,
                hvacSeatCoolerLevelOff,
                hvacSeatCoolerLevelLow,
                hvacSeatCoolerLevelMed,
                hvacSeatCoolerLevelHigh,
            )

            fun fromRawValue(rawValue: Int): CarServer_HvacSeatCoolerActions.HvacSeatCoolerLevel_E =
                when (rawValue) {
                    0 -> hvacSeatCoolerLevelUnknown
                    1 -> hvacSeatCoolerLevelOff
                    2 -> hvacSeatCoolerLevelLow
                    3 -> hvacSeatCoolerLevelMed
                    4 -> hvacSeatCoolerLevelHigh
                    else -> UNRECOGNIZED
                }
        }
    }

    enum class HvacSeatCoolerPosition_E(val rawValue: Int) {
        hvacSeatCoolerPositionUnknown(0),
        hvacSeatCoolerPositionFrontLeft(1),
        hvacSeatCoolerPositionFrontRight(2),
        UNRECOGNIZED(-1),
        ;

        companion object {
            val allCases: List<CarServer_HvacSeatCoolerActions.HvacSeatCoolerPosition_E> = listOf(
                hvacSeatCoolerPositionUnknown,
                hvacSeatCoolerPositionFrontLeft,
                hvacSeatCoolerPositionFrontRight,
            )

            fun fromRawValue(rawValue: Int): CarServer_HvacSeatCoolerActions.HvacSeatCoolerPosition_E =
                when (rawValue) {
                    0 -> hvacSeatCoolerPositionUnknown
                    1 -> hvacSeatCoolerPositionFrontLeft
                    2 -> hvacSeatCoolerPositionFrontRight
                    else -> UNRECOGNIZED
                }
        }
    }

    class HvacSeatCoolerAction() : GeneratedMessage() {
        constructor(serializedBytes: ByteArray) : this() {
            merge(serializedBytes)
        }

        var seatCoolerLevel: CarServer_HvacSeatCoolerActions.HvacSeatCoolerLevel_E = CarServer_HvacSeatCoolerActions.HvacSeatCoolerLevel_E.hvacSeatCoolerLevelUnknown
            set(value) {
                field = value
                _protobufForgetUnrecognized(1)
                _protobufMutated()
            }

        var seatPosition: CarServer_HvacSeatCoolerActions.HvacSeatCoolerPosition_E = CarServer_HvacSeatCoolerActions.HvacSeatCoolerPosition_E.hvacSeatCoolerPositionUnknown
            set(value) {
                field = value
                _protobufForgetUnrecognized(2)
                _protobufMutated()
            }

        override fun decodeMessage(decoder: BinaryDecoder) {
            while (true) {
                when (decoder.nextFieldNumber() ?: return) {
                    1 -> decoder.decodeSingularOpenEnumField(CarServer_HvacSeatCoolerActions.HvacSeatCoolerLevel_E.UNRECOGNIZED) { CarServer_HvacSeatCoolerActions.HvacSeatCoolerLevel_E.fromRawValue(it) }
                        ?.let { this.seatCoolerLevel = it }
                    2 -> decoder.decodeSingularOpenEnumField(CarServer_HvacSeatCoolerActions.HvacSeatCoolerPosition_E.UNRECOGNIZED) { CarServer_HvacSeatCoolerActions.HvacSeatCoolerPosition_E.fromRawValue(it) }
                        ?.let { this.seatPosition = it }
                }
            }
        }

        override fun traverse(visitor: BinaryEncodingVisitor) {
            if (this.seatCoolerLevel != CarServer_HvacSeatCoolerActions.HvacSeatCoolerLevel_E.hvacSeatCoolerLevelUnknown && this.seatCoolerLevel != CarServer_HvacSeatCoolerActions.HvacSeatCoolerLevel_E.UNRECOGNIZED) {
                visitor.visitSingularEnumField(this.seatCoolerLevel.rawValue, 1)
            }
            if (this.seatPosition != CarServer_HvacSeatCoolerActions.HvacSeatCoolerPosition_E.hvacSeatCoolerPositionUnknown && this.seatPosition != CarServer_HvacSeatCoolerActions.HvacSeatCoolerPosition_E.UNRECOGNIZED) {
                visitor.visitSingularEnumField(this.seatPosition.rawValue, 2)
            }
            visitor.visitUnknown(unknownFields)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CarServer_HvacSeatCoolerActions.HvacSeatCoolerAction) return false
            if (this.seatCoolerLevel != other.seatCoolerLevel) return false
            if (this.seatPosition != other.seatPosition) return false
            return this.unknownFields.contentEquals(other.unknownFields)
        }

        override fun hashCode(): Int {
            var hash = 0
            hash = 31 * hash + this.seatCoolerLevel.hashCode()
            hash = 31 * hash + this.seatPosition.hashCode()
            hash = 31 * hash + this.unknownFields.contentHashCode()
            return hash
        }

        override fun copy(): CarServer_HvacSeatCoolerActions.HvacSeatCoolerAction {
            val result = CarServer_HvacSeatCoolerActions.HvacSeatCoolerAction()
            result.seatCoolerLevel = this.seatCoolerLevel
            result.seatPosition = this.seatPosition
            result.unknownFields = this.unknownFields
            return result
        }

        companion object {
            const val protoMessageName: String = "CarServer.HvacSeatCoolerActions.HvacSeatCoolerAction"

            fun with(block: CarServer_HvacSeatCoolerActions.HvacSeatCoolerAction.() -> Unit): CarServer_HvacSeatCoolerActions.HvacSeatCoolerAction =
                CarServer_HvacSeatCoolerActions.HvacSeatCoolerAction().apply(block)
        }
    }

    private var _hvacSeatCoolerAction: ProtobufList<CarServer_HvacSeatCoolerActions.HvacSeatCoolerAction> = ProtobufList<CarServer_HvacSeatCoolerActions.HvacSeatCoolerAction>(this) { it.copy() }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeRepeatedMessageField(this._hvacSeatCoolerAction) { CarServer_HvacSeatCoolerActions.HvacSeatCoolerAction() }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this._hvacSeatCoolerAction.isNotEmpty()) {
            visitor.visitRepeatedMessageField(this._hvacSeatCoolerAction, 1)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_HvacSeatCoolerActions) return false
        if (this._hvacSeatCoolerAction != other._hvacSeatCoolerAction) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this._hvacSeatCoolerAction.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_HvacSeatCoolerActions {
        val result = CarServer_HvacSeatCoolerActions()
        result._hvacSeatCoolerAction.addAll(this._hvacSeatCoolerAction)
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.HvacSeatCoolerActions"

        fun with(block: CarServer_HvacSeatCoolerActions.() -> Unit): CarServer_HvacSeatCoolerActions =
            CarServer_HvacSeatCoolerActions().apply(block)
    }
}

class CarServer_HvacSetPreconditioningMaxAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var on: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    var manualOverride: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    var manualOverrideMode: MutableList<CarServer_HvacSetPreconditioningMaxAction.ManualOverrideMode_E>
        get() = this._manualOverrideMode
        set(value) {
            this._manualOverrideMode = ProtobufList<CarServer_HvacSetPreconditioningMaxAction.ManualOverrideMode_E>(this, CarServer_HvacSetPreconditioningMaxAction.ManualOverrideMode_E.UNRECOGNIZED, 3) { it }.apply { addAll(value) }
            _protobufMutated()
        }

    enum class ManualOverrideMode_E(val rawValue: Int) {
        dogMode(0),
        soc(1),
        doors(2),
        UNRECOGNIZED(-1),
        ;

        companion object {
            val allCases: List<CarServer_HvacSetPreconditioningMaxAction.ManualOverrideMode_E> = listOf(
                dogMode,
                soc,
                doors,
            )

            fun fromRawValue(rawValue: Int): CarServer_HvacSetPreconditioningMaxAction.ManualOverrideMode_E =
                when (rawValue) {
                    0 -> dogMode
                    1 -> soc
                    2 -> doors
                    else -> UNRECOGNIZED
                }
        }
    }

    private var _manualOverrideMode: ProtobufList<CarServer_HvacSetPreconditioningMaxAction.ManualOverrideMode_E> = ProtobufList<CarServer_HvacSetPreconditioningMaxAction.ManualOverrideMode_E>(this, CarServer_HvacSetPreconditioningMaxAction.ManualOverrideMode_E.UNRECOGNIZED, 3) { it }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularBoolField()?.let { this.on = it }
                2 -> decoder.decodeSingularBoolField()?.let { this.manualOverride = it }
                3 -> decoder.decodeRepeatedOpenEnumField(this._manualOverrideMode, CarServer_HvacSetPreconditioningMaxAction.ManualOverrideMode_E.UNRECOGNIZED) { CarServer_HvacSetPreconditioningMaxAction.ManualOverrideMode_E.fromRawValue(it) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.on) {
            visitor.visitSingularBoolField(this.on, 1)
        }
        if (this.manualOverride) {
            visitor.visitSingularBoolField(this.manualOverride, 2)
        }
        if (this._manualOverrideMode.isNotEmpty()) {
            visitor.visitPackedEnumField(this._manualOverrideMode.filter { it != CarServer_HvacSetPreconditioningMaxAction.ManualOverrideMode_E.UNRECOGNIZED }.map { it.rawValue }, 3)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_HvacSetPreconditioningMaxAction) return false
        if (this.on != other.on) return false
        if (this.manualOverride != other.manualOverride) return false
        if (this._manualOverrideMode != other._manualOverrideMode) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.on.hashCode()
        hash = 31 * hash + this.manualOverride.hashCode()
        hash = 31 * hash + this._manualOverrideMode.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_HvacSetPreconditioningMaxAction {
        val result = CarServer_HvacSetPreconditioningMaxAction()
        result.on = this.on
        result.manualOverride = this.manualOverride
        result._manualOverrideMode.addAll(this._manualOverrideMode)
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.HvacSetPreconditioningMaxAction"

        fun with(block: CarServer_HvacSetPreconditioningMaxAction.() -> Unit): CarServer_HvacSetPreconditioningMaxAction =
            CarServer_HvacSetPreconditioningMaxAction().apply(block)
    }
}

class CarServer_HvacSteeringWheelHeaterAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var powerOn: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularBoolField()?.let { this.powerOn = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.powerOn) {
            visitor.visitSingularBoolField(this.powerOn, 1)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_HvacSteeringWheelHeaterAction) return false
        if (this.powerOn != other.powerOn) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.powerOn.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_HvacSteeringWheelHeaterAction {
        val result = CarServer_HvacSteeringWheelHeaterAction()
        result.powerOn = this.powerOn
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.HvacSteeringWheelHeaterAction"

        fun with(block: CarServer_HvacSteeringWheelHeaterAction.() -> Unit): CarServer_HvacSteeringWheelHeaterAction =
            CarServer_HvacSteeringWheelHeaterAction().apply(block)
    }
}

class CarServer_HvacTemperatureAdjustmentAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var deltaCelsius: Float = 0f
        set(value) {
            field = value
            _protobufMutated()
        }

    var deltaPercent: Int = 0
        set(value) {
            field = value
            _protobufMutated()
        }

    var absoluteCelsius: Float = 0f
        set(value) {
            field = value
            _protobufMutated()
        }

    var level: CarServer_HvacTemperatureAdjustmentAction.Temperature
        get() = this._level ?: _protobufPending(5, { CarServer_HvacTemperatureAdjustmentAction.Temperature() }) { this._level = it }
        set(value) {
            _protobufDropPending(5)
            this._level = value.copy()
            _protobufMutated()
        }

    val hasLevel: Boolean
        get() = this._level != null

    fun clearLevel() {
        _protobufDropPending(5)
        this._level = null
        _protobufMutated()
    }

    var hvacTemperatureZone: MutableList<CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone>
        get() = this._hvacTemperatureZone
        set(value) {
            this._hvacTemperatureZone = ProtobufList<CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone>(this) { it.copy() }.apply { addAll(value) }
            _protobufMutated()
        }

    var driverTempCelsius: Float = 0f
        set(value) {
            field = value
            _protobufMutated()
        }

    var passengerTempCelsius: Float = 0f
        set(value) {
            field = value
            _protobufMutated()
        }

    class Temperature() : GeneratedMessage() {
        constructor(serializedBytes: ByteArray) : this() {
            merge(serializedBytes)
        }

        var type: CarServer_HvacTemperatureAdjustmentAction.Temperature.OneOf_Type?
            get() = this._type
            set(value) {
                this._protobufStore_type(this._protobufCopy_type(value))
                _protobufMutated()
            }

        var tempUnknown: CarServer_Void
            get() = (this._type as? CarServer_HvacTemperatureAdjustmentAction.Temperature.OneOf_Type.tempUnknown)?.value
                ?: _protobufPending(1, { CarServer_Void() }) { this._protobufStore_type(CarServer_HvacTemperatureAdjustmentAction.Temperature.OneOf_Type.tempUnknown(it)) }
            set(value) {
                this.type = CarServer_HvacTemperatureAdjustmentAction.Temperature.OneOf_Type.tempUnknown(value)
            }

        var tempMin: CarServer_Void
            get() = (this._type as? CarServer_HvacTemperatureAdjustmentAction.Temperature.OneOf_Type.tempMin)?.value
                ?: _protobufPending(2, { CarServer_Void() }) { this._protobufStore_type(CarServer_HvacTemperatureAdjustmentAction.Temperature.OneOf_Type.tempMin(it)) }
            set(value) {
                this.type = CarServer_HvacTemperatureAdjustmentAction.Temperature.OneOf_Type.tempMin(value)
            }

        var tempMax: CarServer_Void
            get() = (this._type as? CarServer_HvacTemperatureAdjustmentAction.Temperature.OneOf_Type.tempMax)?.value
                ?: _protobufPending(3, { CarServer_Void() }) { this._protobufStore_type(CarServer_HvacTemperatureAdjustmentAction.Temperature.OneOf_Type.tempMax(it)) }
            set(value) {
                this.type = CarServer_HvacTemperatureAdjustmentAction.Temperature.OneOf_Type.tempMax(value)
            }

        sealed class OneOf_Type(value: Any) : ProtobufOneofCase(value) {
            class tempUnknown(val value: CarServer_Void) : OneOf_Type(value)
            class tempMin(val value: CarServer_Void) : OneOf_Type(value)
            class tempMax(val value: CarServer_Void) : OneOf_Type(value)
        }

        private var _type: CarServer_HvacTemperatureAdjustmentAction.Temperature.OneOf_Type? = null

        private fun _protobufStore_type(value: CarServer_HvacTemperatureAdjustmentAction.Temperature.OneOf_Type?) {
            _protobufDropPending(1)
            _protobufDropPending(2)
            _protobufDropPending(3)
            this._type = value
        }

        private fun _protobufCopy_type(value: CarServer_HvacTemperatureAdjustmentAction.Temperature.OneOf_Type?): CarServer_HvacTemperatureAdjustmentAction.Temperature.OneOf_Type? =
            when (value) {
                is CarServer_HvacTemperatureAdjustmentAction.Temperature.OneOf_Type.tempUnknown -> CarServer_HvacTemperatureAdjustmentAction.Temperature.OneOf_Type.tempUnknown(value.value.copy())
                is CarServer_HvacTemperatureAdjustmentAction.Temperature.OneOf_Type.tempMin -> CarServer_HvacTemperatureAdjustmentAction.Temperature.OneOf_Type.tempMin(value.value.copy())
                is CarServer_HvacTemperatureAdjustmentAction.Temperature.OneOf_Type.tempMax -> CarServer_HvacTemperatureAdjustmentAction.Temperature.OneOf_Type.tempMax(value.value.copy())
                else -> value
            }

        override fun decodeMessage(decoder: BinaryDecoder) {
            while (true) {
                when (decoder.nextFieldNumber() ?: return) {
                    1 -> decoder.decodeSingularMessageField((this._type as? CarServer_HvacTemperatureAdjustmentAction.Temperature.OneOf_Type.tempUnknown)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_HvacTemperatureAdjustmentAction.Temperature.OneOf_Type.tempUnknown(it)) }
                    2 -> decoder.decodeSingularMessageField((this._type as? CarServer_HvacTemperatureAdjustmentAction.Temperature.OneOf_Type.tempMin)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_HvacTemperatureAdjustmentAction.Temperature.OneOf_Type.tempMin(it)) }
                    3 -> decoder.decodeSingularMessageField((this._type as? CarServer_HvacTemperatureAdjustmentAction.Temperature.OneOf_Type.tempMax)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_HvacTemperatureAdjustmentAction.Temperature.OneOf_Type.tempMax(it)) }
                }
            }
        }

        override fun traverse(visitor: BinaryEncodingVisitor) {
            (this._type as? CarServer_HvacTemperatureAdjustmentAction.Temperature.OneOf_Type.tempUnknown)?.let {
                visitor.visitSingularMessageField(it.value, 1)
            }
            (this._type as? CarServer_HvacTemperatureAdjustmentAction.Temperature.OneOf_Type.tempMin)?.let {
                visitor.visitSingularMessageField(it.value, 2)
            }
            (this._type as? CarServer_HvacTemperatureAdjustmentAction.Temperature.OneOf_Type.tempMax)?.let {
                visitor.visitSingularMessageField(it.value, 3)
            }
            visitor.visitUnknown(unknownFields)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CarServer_HvacTemperatureAdjustmentAction.Temperature) return false
            if (this._type != other._type) return false
            return this.unknownFields.contentEquals(other.unknownFields)
        }

        override fun hashCode(): Int {
            var hash = 0
            hash = 31 * hash + (this._type?.hashCode() ?: 0)
            hash = 31 * hash + this.unknownFields.contentHashCode()
            return hash
        }

        override fun copy(): CarServer_HvacTemperatureAdjustmentAction.Temperature {
            val result = CarServer_HvacTemperatureAdjustmentAction.Temperature()
            result._type = this._protobufCopy_type(this._type)
            result.unknownFields = this.unknownFields
            return result
        }

        companion object {
            const val protoMessageName: String = "CarServer.HvacTemperatureAdjustmentAction.Temperature"

            fun with(block: CarServer_HvacTemperatureAdjustmentAction.Temperature.() -> Unit): CarServer_HvacTemperatureAdjustmentAction.Temperature =
                CarServer_HvacTemperatureAdjustmentAction.Temperature().apply(block)
        }
    }

    class HvacTemperatureZone() : GeneratedMessage() {
        constructor(serializedBytes: ByteArray) : this() {
            merge(serializedBytes)
        }

        var type: CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type?
            get() = this._type
            set(value) {
                this._protobufStore_type(this._protobufCopy_type(value))
                _protobufMutated()
            }

        var tempZoneUnknown: CarServer_Void
            get() = (this._type as? CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneUnknown)?.value
                ?: _protobufPending(1, { CarServer_Void() }) { this._protobufStore_type(CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneUnknown(it)) }
            set(value) {
                this.type = CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneUnknown(value)
            }

        var tempZoneFrontLeft: CarServer_Void
            get() = (this._type as? CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneFrontLeft)?.value
                ?: _protobufPending(2, { CarServer_Void() }) { this._protobufStore_type(CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneFrontLeft(it)) }
            set(value) {
                this.type = CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneFrontLeft(value)
            }

        var tempZoneFrontRight: CarServer_Void
            get() = (this._type as? CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneFrontRight)?.value
                ?: _protobufPending(3, { CarServer_Void() }) { this._protobufStore_type(CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneFrontRight(it)) }
            set(value) {
                this.type = CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneFrontRight(value)
            }

        var tempZoneRear: CarServer_Void
            get() = (this._type as? CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneRear)?.value
                ?: _protobufPending(4, { CarServer_Void() }) { this._protobufStore_type(CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneRear(it)) }
            set(value) {
                this.type = CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneRear(value)
            }

        sealed class OneOf_Type(value: Any) : ProtobufOneofCase(value) {
            class tempZoneUnknown(val value: CarServer_Void) : OneOf_Type(value)
            class tempZoneFrontLeft(val value: CarServer_Void) : OneOf_Type(value)
            class tempZoneFrontRight(val value: CarServer_Void) : OneOf_Type(value)
            class tempZoneRear(val value: CarServer_Void) : OneOf_Type(value)
        }

        private var _type: CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type? = null

        private fun _protobufStore_type(value: CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type?) {
            _protobufDropPending(1)
            _protobufDropPending(2)
            _protobufDropPending(3)
            _protobufDropPending(4)
            this._type = value
        }

        private fun _protobufCopy_type(value: CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type?): CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type? =
            when (value) {
                is CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneUnknown -> CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneUnknown(value.value.copy())
                is CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneFrontLeft -> CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneFrontLeft(value.value.copy())
                is CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneFrontRight -> CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneFrontRight(value.value.copy())
                is CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneRear -> CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneRear(value.value.copy())
                else -> value
            }

        override fun decodeMessage(decoder: BinaryDecoder) {
            while (true) {
                when (decoder.nextFieldNumber() ?: return) {
                    1 -> decoder.decodeSingularMessageField((this._type as? CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneUnknown)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneUnknown(it)) }
                    2 -> decoder.decodeSingularMessageField((this._type as? CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneFrontLeft)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneFrontLeft(it)) }
                    3 -> decoder.decodeSingularMessageField((this._type as? CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneFrontRight)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneFrontRight(it)) }
                    4 -> decoder.decodeSingularMessageField((this._type as? CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneRear)?.value) { CarServer_Void() }
                        ?.let { this._protobufStore_type(CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneRear(it)) }
                }
            }
        }

        override fun traverse(visitor: BinaryEncodingVisitor) {
            (this._type as? CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneUnknown)?.let {
                visitor.visitSingularMessageField(it.value, 1)
            }
            (this._type as? CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneFrontLeft)?.let {
                visitor.visitSingularMessageField(it.value, 2)
            }
            (this._type as? CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneFrontRight)?.let {
                visitor.visitSingularMessageField(it.value, 3)
            }
            (this._type as? CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.OneOf_Type.tempZoneRear)?.let {
                visitor.visitSingularMessageField(it.value, 4)
            }
            visitor.visitUnknown(unknownFields)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone) return false
            if (this._type != other._type) return false
            return this.unknownFields.contentEquals(other.unknownFields)
        }

        override fun hashCode(): Int {
            var hash = 0
            hash = 31 * hash + (this._type?.hashCode() ?: 0)
            hash = 31 * hash + this.unknownFields.contentHashCode()
            return hash
        }

        override fun copy(): CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone {
            val result = CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone()
            result._type = this._protobufCopy_type(this._type)
            result.unknownFields = this.unknownFields
            return result
        }

        companion object {
            const val protoMessageName: String = "CarServer.HvacTemperatureAdjustmentAction.HvacTemperatureZone"

            fun with(block: CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone.() -> Unit): CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone =
                CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone().apply(block)
        }
    }

    private var _level: CarServer_HvacTemperatureAdjustmentAction.Temperature? = null
    private var _hvacTemperatureZone: ProtobufList<CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone> = ProtobufList<CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone>(this) { it.copy() }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularFloatField()?.let { this.deltaCelsius = it }
                2 -> decoder.decodeSingularSInt32Field()?.let { this.deltaPercent = it }
                3 -> decoder.decodeSingularFloatField()?.let { this.absoluteCelsius = it }
                4 -> decoder.decodeRepeatedMessageField(this._hvacTemperatureZone) { CarServer_HvacTemperatureAdjustmentAction.HvacTemperatureZone() }
                5 -> decoder.decodeSingularMessageField(this._level) { CarServer_HvacTemperatureAdjustmentAction.Temperature() }?.let {
                    _protobufDropPending(5)
                    this._level = it
                }
                6 -> decoder.decodeSingularFloatField()?.let { this.driverTempCelsius = it }
                7 -> decoder.decodeSingularFloatField()?.let { this.passengerTempCelsius = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.deltaCelsius.toRawBits() != 0) {
            visitor.visitSingularFloatField(this.deltaCelsius, 1)
        }
        if (this.deltaPercent != 0) {
            visitor.visitSingularSInt32Field(this.deltaPercent, 2)
        }
        if (this.absoluteCelsius.toRawBits() != 0) {
            visitor.visitSingularFloatField(this.absoluteCelsius, 3)
        }
        if (this._hvacTemperatureZone.isNotEmpty()) {
            visitor.visitRepeatedMessageField(this._hvacTemperatureZone, 4)
        }
        this._level?.let {
            visitor.visitSingularMessageField(it, 5)
        }
        if (this.driverTempCelsius.toRawBits() != 0) {
            visitor.visitSingularFloatField(this.driverTempCelsius, 6)
        }
        if (this.passengerTempCelsius.toRawBits() != 0) {
            visitor.visitSingularFloatField(this.passengerTempCelsius, 7)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_HvacTemperatureAdjustmentAction) return false
        if (this.deltaCelsius != other.deltaCelsius) return false
        if (this.deltaPercent != other.deltaPercent) return false
        if (this.absoluteCelsius != other.absoluteCelsius) return false
        if (this._level != other._level) return false
        if (this._hvacTemperatureZone != other._hvacTemperatureZone) return false
        if (this.driverTempCelsius != other.driverTempCelsius) return false
        if (this.passengerTempCelsius != other.passengerTempCelsius) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + protobufHash(this.deltaCelsius)
        hash = 31 * hash + this.deltaPercent.hashCode()
        hash = 31 * hash + protobufHash(this.absoluteCelsius)
        hash = 31 * hash + (this._level?.hashCode() ?: 0)
        hash = 31 * hash + this._hvacTemperatureZone.hashCode()
        hash = 31 * hash + protobufHash(this.driverTempCelsius)
        hash = 31 * hash + protobufHash(this.passengerTempCelsius)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_HvacTemperatureAdjustmentAction {
        val result = CarServer_HvacTemperatureAdjustmentAction()
        result.deltaCelsius = this.deltaCelsius
        result.deltaPercent = this.deltaPercent
        result.absoluteCelsius = this.absoluteCelsius
        result._level = this._level?.copy()
        result._hvacTemperatureZone.addAll(this._hvacTemperatureZone)
        result.driverTempCelsius = this.driverTempCelsius
        result.passengerTempCelsius = this.passengerTempCelsius
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.HvacTemperatureAdjustmentAction"

        fun with(block: CarServer_HvacTemperatureAdjustmentAction.() -> Unit): CarServer_HvacTemperatureAdjustmentAction =
            CarServer_HvacTemperatureAdjustmentAction().apply(block)
    }
}

class CarServer_GetNearbyChargingSites() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var includeMetaData: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    var radius: Int = 0
        set(value) {
            field = value
            _protobufMutated()
        }

    var count: Int = 0
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularBoolField()?.let { this.includeMetaData = it }
                2 -> decoder.decodeSingularInt32Field()?.let { this.radius = it }
                3 -> decoder.decodeSingularInt32Field()?.let { this.count = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.includeMetaData) {
            visitor.visitSingularBoolField(this.includeMetaData, 1)
        }
        if (this.radius != 0) {
            visitor.visitSingularInt32Field(this.radius, 2)
        }
        if (this.count != 0) {
            visitor.visitSingularInt32Field(this.count, 3)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_GetNearbyChargingSites) return false
        if (this.includeMetaData != other.includeMetaData) return false
        if (this.radius != other.radius) return false
        if (this.count != other.count) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.includeMetaData.hashCode()
        hash = 31 * hash + this.radius.hashCode()
        hash = 31 * hash + this.count.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_GetNearbyChargingSites {
        val result = CarServer_GetNearbyChargingSites()
        result.includeMetaData = this.includeMetaData
        result.radius = this.radius
        result.count = this.count
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.GetNearbyChargingSites"

        fun with(block: CarServer_GetNearbyChargingSites.() -> Unit): CarServer_GetNearbyChargingSites =
            CarServer_GetNearbyChargingSites().apply(block)
    }
}

class CarServer_NearbyChargingSites() : GeneratedMessage() {
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

    var superchargers: MutableList<CarServer_Superchargers>
        get() = this._superchargers
        set(value) {
            this._superchargers = ProtobufList<CarServer_Superchargers>(this) { it.copy() }.apply { addAll(value) }
            _protobufMutated()
        }

    var congestionSyncTimeUtcSecs: Long = 0L
        set(value) {
            field = value
            _protobufMutated()
        }

    private var _timestamp: Google_Protobuf_Timestamp? = null
    private var _superchargers: ProtobufList<CarServer_Superchargers> = ProtobufList<CarServer_Superchargers>(this) { it.copy() }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularMessageField(this._timestamp) { Google_Protobuf_Timestamp() }?.let {
                    _protobufDropPending(1)
                    this._timestamp = it
                }
                3 -> decoder.decodeRepeatedMessageField(this._superchargers) { CarServer_Superchargers() }
                4 -> decoder.decodeSingularInt64Field()?.let { this.congestionSyncTimeUtcSecs = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        this._timestamp?.let {
            visitor.visitSingularMessageField(it, 1)
        }
        if (this._superchargers.isNotEmpty()) {
            visitor.visitRepeatedMessageField(this._superchargers, 3)
        }
        if (this.congestionSyncTimeUtcSecs != 0L) {
            visitor.visitSingularInt64Field(this.congestionSyncTimeUtcSecs, 4)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_NearbyChargingSites) return false
        if (this._timestamp != other._timestamp) return false
        if (this._superchargers != other._superchargers) return false
        if (this.congestionSyncTimeUtcSecs != other.congestionSyncTimeUtcSecs) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._timestamp?.hashCode() ?: 0)
        hash = 31 * hash + this._superchargers.hashCode()
        hash = 31 * hash + this.congestionSyncTimeUtcSecs.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_NearbyChargingSites {
        val result = CarServer_NearbyChargingSites()
        result._timestamp = this._timestamp?.copy()
        result._superchargers.addAll(this._superchargers)
        result.congestionSyncTimeUtcSecs = this.congestionSyncTimeUtcSecs
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.NearbyChargingSites"

        fun with(block: CarServer_NearbyChargingSites.() -> Unit): CarServer_NearbyChargingSites =
            CarServer_NearbyChargingSites().apply(block)
    }
}

class CarServer_Superchargers() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var id: Long = 0L
        set(value) {
            field = value
            _protobufMutated()
        }

    var amenities: String = ""
        set(value) {
            field = value
            _protobufMutated()
        }

    var availableStalls: Int = 0
        set(value) {
            field = value
            _protobufMutated()
        }

    var billingInfo: String = ""
        set(value) {
            field = value
            _protobufMutated()
        }

    var billingTime: String = ""
        set(value) {
            field = value
            _protobufMutated()
        }

    var city: String = ""
        set(value) {
            field = value
            _protobufMutated()
        }

    var country: String = ""
        set(value) {
            field = value
            _protobufMutated()
        }

    var distanceMiles: Float = 0f
        set(value) {
            field = value
            _protobufMutated()
        }

    var district: String = ""
        set(value) {
            field = value
            _protobufMutated()
        }

    var location: CarServer_LatLong
        get() = this._location ?: _protobufPending(10, { CarServer_LatLong() }) { this._location = it }
        set(value) {
            _protobufDropPending(10)
            this._location = value.copy()
            _protobufMutated()
        }

    val hasLocation: Boolean
        get() = this._location != null

    fun clearLocation() {
        _protobufDropPending(10)
        this._location = null
        _protobufMutated()
    }

    var name: String = ""
        set(value) {
            field = value
            _protobufMutated()
        }

    var postalCode: String = ""
        set(value) {
            field = value
            _protobufMutated()
        }

    var siteClosed: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    var state: String = ""
        set(value) {
            field = value
            _protobufMutated()
        }

    var streetAddress: String = ""
        set(value) {
            field = value
            _protobufMutated()
        }

    var totalStalls: Int = 0
        set(value) {
            field = value
            _protobufMutated()
        }

    var withinRange: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    var maxPowerKw: Int = 0
        set(value) {
            field = value
            _protobufMutated()
        }

    var outOfOrderStallsNumber: Int = 0
        set(value) {
            field = value
            _protobufMutated()
        }

    var outOfOrderStallsNames: String = ""
        set(value) {
            field = value
            _protobufMutated()
        }

    private var _location: CarServer_LatLong? = null

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularInt64Field()?.let { this.id = it }
                2 -> decoder.decodeSingularStringField()?.let { this.amenities = it }
                3 -> decoder.decodeSingularInt32Field()?.let { this.availableStalls = it }
                4 -> decoder.decodeSingularStringField()?.let { this.billingInfo = it }
                5 -> decoder.decodeSingularStringField()?.let { this.billingTime = it }
                6 -> decoder.decodeSingularStringField()?.let { this.city = it }
                7 -> decoder.decodeSingularStringField()?.let { this.country = it }
                8 -> decoder.decodeSingularFloatField()?.let { this.distanceMiles = it }
                9 -> decoder.decodeSingularStringField()?.let { this.district = it }
                10 -> decoder.decodeSingularMessageField(this._location) { CarServer_LatLong() }?.let {
                    _protobufDropPending(10)
                    this._location = it
                }
                11 -> decoder.decodeSingularStringField()?.let { this.name = it }
                12 -> decoder.decodeSingularStringField()?.let { this.postalCode = it }
                13 -> decoder.decodeSingularBoolField()?.let { this.siteClosed = it }
                14 -> decoder.decodeSingularStringField()?.let { this.state = it }
                15 -> decoder.decodeSingularStringField()?.let { this.streetAddress = it }
                16 -> decoder.decodeSingularInt32Field()?.let { this.totalStalls = it }
                17 -> decoder.decodeSingularBoolField()?.let { this.withinRange = it }
                18 -> decoder.decodeSingularInt32Field()?.let { this.maxPowerKw = it }
                19 -> decoder.decodeSingularInt32Field()?.let { this.outOfOrderStallsNumber = it }
                20 -> decoder.decodeSingularStringField()?.let { this.outOfOrderStallsNames = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.id != 0L) {
            visitor.visitSingularInt64Field(this.id, 1)
        }
        if (this.amenities.isNotEmpty()) {
            visitor.visitSingularStringField(this.amenities, 2)
        }
        if (this.availableStalls != 0) {
            visitor.visitSingularInt32Field(this.availableStalls, 3)
        }
        if (this.billingInfo.isNotEmpty()) {
            visitor.visitSingularStringField(this.billingInfo, 4)
        }
        if (this.billingTime.isNotEmpty()) {
            visitor.visitSingularStringField(this.billingTime, 5)
        }
        if (this.city.isNotEmpty()) {
            visitor.visitSingularStringField(this.city, 6)
        }
        if (this.country.isNotEmpty()) {
            visitor.visitSingularStringField(this.country, 7)
        }
        if (this.distanceMiles.toRawBits() != 0) {
            visitor.visitSingularFloatField(this.distanceMiles, 8)
        }
        if (this.district.isNotEmpty()) {
            visitor.visitSingularStringField(this.district, 9)
        }
        this._location?.let {
            visitor.visitSingularMessageField(it, 10)
        }
        if (this.name.isNotEmpty()) {
            visitor.visitSingularStringField(this.name, 11)
        }
        if (this.postalCode.isNotEmpty()) {
            visitor.visitSingularStringField(this.postalCode, 12)
        }
        if (this.siteClosed) {
            visitor.visitSingularBoolField(this.siteClosed, 13)
        }
        if (this.state.isNotEmpty()) {
            visitor.visitSingularStringField(this.state, 14)
        }
        if (this.streetAddress.isNotEmpty()) {
            visitor.visitSingularStringField(this.streetAddress, 15)
        }
        if (this.totalStalls != 0) {
            visitor.visitSingularInt32Field(this.totalStalls, 16)
        }
        if (this.withinRange) {
            visitor.visitSingularBoolField(this.withinRange, 17)
        }
        if (this.maxPowerKw != 0) {
            visitor.visitSingularInt32Field(this.maxPowerKw, 18)
        }
        if (this.outOfOrderStallsNumber != 0) {
            visitor.visitSingularInt32Field(this.outOfOrderStallsNumber, 19)
        }
        if (this.outOfOrderStallsNames.isNotEmpty()) {
            visitor.visitSingularStringField(this.outOfOrderStallsNames, 20)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_Superchargers) return false
        if (this.id != other.id) return false
        if (this.amenities != other.amenities) return false
        if (this.availableStalls != other.availableStalls) return false
        if (this.billingInfo != other.billingInfo) return false
        if (this.billingTime != other.billingTime) return false
        if (this.city != other.city) return false
        if (this.country != other.country) return false
        if (this.distanceMiles != other.distanceMiles) return false
        if (this.district != other.district) return false
        if (this._location != other._location) return false
        if (this.name != other.name) return false
        if (this.postalCode != other.postalCode) return false
        if (this.siteClosed != other.siteClosed) return false
        if (this.state != other.state) return false
        if (this.streetAddress != other.streetAddress) return false
        if (this.totalStalls != other.totalStalls) return false
        if (this.withinRange != other.withinRange) return false
        if (this.maxPowerKw != other.maxPowerKw) return false
        if (this.outOfOrderStallsNumber != other.outOfOrderStallsNumber) return false
        if (this.outOfOrderStallsNames != other.outOfOrderStallsNames) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.id.hashCode()
        hash = 31 * hash + this.amenities.hashCode()
        hash = 31 * hash + this.availableStalls.hashCode()
        hash = 31 * hash + this.billingInfo.hashCode()
        hash = 31 * hash + this.billingTime.hashCode()
        hash = 31 * hash + this.city.hashCode()
        hash = 31 * hash + this.country.hashCode()
        hash = 31 * hash + protobufHash(this.distanceMiles)
        hash = 31 * hash + this.district.hashCode()
        hash = 31 * hash + (this._location?.hashCode() ?: 0)
        hash = 31 * hash + this.name.hashCode()
        hash = 31 * hash + this.postalCode.hashCode()
        hash = 31 * hash + this.siteClosed.hashCode()
        hash = 31 * hash + this.state.hashCode()
        hash = 31 * hash + this.streetAddress.hashCode()
        hash = 31 * hash + this.totalStalls.hashCode()
        hash = 31 * hash + this.withinRange.hashCode()
        hash = 31 * hash + this.maxPowerKw.hashCode()
        hash = 31 * hash + this.outOfOrderStallsNumber.hashCode()
        hash = 31 * hash + this.outOfOrderStallsNames.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_Superchargers {
        val result = CarServer_Superchargers()
        result.id = this.id
        result.amenities = this.amenities
        result.availableStalls = this.availableStalls
        result.billingInfo = this.billingInfo
        result.billingTime = this.billingTime
        result.city = this.city
        result.country = this.country
        result.distanceMiles = this.distanceMiles
        result.district = this.district
        result._location = this._location?.copy()
        result.name = this.name
        result.postalCode = this.postalCode
        result.siteClosed = this.siteClosed
        result.state = this.state
        result.streetAddress = this.streetAddress
        result.totalStalls = this.totalStalls
        result.withinRange = this.withinRange
        result.maxPowerKw = this.maxPowerKw
        result.outOfOrderStallsNumber = this.outOfOrderStallsNumber
        result.outOfOrderStallsNames = this.outOfOrderStallsNames
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.Superchargers"

        fun with(block: CarServer_Superchargers.() -> Unit): CarServer_Superchargers =
            CarServer_Superchargers().apply(block)
    }
}

class CarServer_MediaPlayAction() : GeneratedMessage() {
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
        if (other !is CarServer_MediaPlayAction) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_MediaPlayAction {
        val result = CarServer_MediaPlayAction()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.MediaPlayAction"

        fun with(block: CarServer_MediaPlayAction.() -> Unit): CarServer_MediaPlayAction =
            CarServer_MediaPlayAction().apply(block)
    }
}

class CarServer_MediaUpdateVolume() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var mediaVolume: CarServer_MediaUpdateVolume.OneOf_MediaVolume?
        get() = this._mediaVolume
        set(value) {
            this._mediaVolume = value
            _protobufMutated()
        }

    var volumeDelta: Int
        get() = (this._mediaVolume as? CarServer_MediaUpdateVolume.OneOf_MediaVolume.volumeDelta)?.value ?: 0
        set(value) {
            this.mediaVolume = CarServer_MediaUpdateVolume.OneOf_MediaVolume.volumeDelta(value)
        }

    var volumeAbsoluteFloat: Float
        get() = (this._mediaVolume as? CarServer_MediaUpdateVolume.OneOf_MediaVolume.volumeAbsoluteFloat)?.value ?: 0f
        set(value) {
            this.mediaVolume = CarServer_MediaUpdateVolume.OneOf_MediaVolume.volumeAbsoluteFloat(value)
        }

    sealed class OneOf_MediaVolume(value: Any) : ProtobufOneofCase(value) {
        class volumeDelta(val value: Int) : OneOf_MediaVolume(value)
        class volumeAbsoluteFloat(val value: Float) : OneOf_MediaVolume(value)
    }

    private var _mediaVolume: CarServer_MediaUpdateVolume.OneOf_MediaVolume? = null

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularSInt32Field()?.let { this._mediaVolume = CarServer_MediaUpdateVolume.OneOf_MediaVolume.volumeDelta(it) }
                3 -> decoder.decodeSingularFloatField()?.let { this._mediaVolume = CarServer_MediaUpdateVolume.OneOf_MediaVolume.volumeAbsoluteFloat(it) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        (this._mediaVolume as? CarServer_MediaUpdateVolume.OneOf_MediaVolume.volumeDelta)?.let {
            visitor.visitSingularSInt32Field(it.value, 1)
        }
        (this._mediaVolume as? CarServer_MediaUpdateVolume.OneOf_MediaVolume.volumeAbsoluteFloat)?.let {
            visitor.visitSingularFloatField(it.value, 3)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_MediaUpdateVolume) return false
        if (this._mediaVolume != other._mediaVolume) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._mediaVolume?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_MediaUpdateVolume {
        val result = CarServer_MediaUpdateVolume()
        result._mediaVolume = this._mediaVolume
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.MediaUpdateVolume"

        fun with(block: CarServer_MediaUpdateVolume.() -> Unit): CarServer_MediaUpdateVolume =
            CarServer_MediaUpdateVolume().apply(block)
    }
}

class CarServer_MediaNextFavorite() : GeneratedMessage() {
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
        if (other !is CarServer_MediaNextFavorite) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_MediaNextFavorite {
        val result = CarServer_MediaNextFavorite()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.MediaNextFavorite"

        fun with(block: CarServer_MediaNextFavorite.() -> Unit): CarServer_MediaNextFavorite =
            CarServer_MediaNextFavorite().apply(block)
    }
}

class CarServer_MediaPreviousFavorite() : GeneratedMessage() {
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
        if (other !is CarServer_MediaPreviousFavorite) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_MediaPreviousFavorite {
        val result = CarServer_MediaPreviousFavorite()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.MediaPreviousFavorite"

        fun with(block: CarServer_MediaPreviousFavorite.() -> Unit): CarServer_MediaPreviousFavorite =
            CarServer_MediaPreviousFavorite().apply(block)
    }
}

class CarServer_MediaNextTrack() : GeneratedMessage() {
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
        if (other !is CarServer_MediaNextTrack) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_MediaNextTrack {
        val result = CarServer_MediaNextTrack()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.MediaNextTrack"

        fun with(block: CarServer_MediaNextTrack.() -> Unit): CarServer_MediaNextTrack =
            CarServer_MediaNextTrack().apply(block)
    }
}

class CarServer_MediaPreviousTrack() : GeneratedMessage() {
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
        if (other !is CarServer_MediaPreviousTrack) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_MediaPreviousTrack {
        val result = CarServer_MediaPreviousTrack()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.MediaPreviousTrack"

        fun with(block: CarServer_MediaPreviousTrack.() -> Unit): CarServer_MediaPreviousTrack =
            CarServer_MediaPreviousTrack().apply(block)
    }
}

class CarServer_VehicleControlCancelSoftwareUpdateAction() : GeneratedMessage() {
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
        if (other !is CarServer_VehicleControlCancelSoftwareUpdateAction) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_VehicleControlCancelSoftwareUpdateAction {
        val result = CarServer_VehicleControlCancelSoftwareUpdateAction()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.VehicleControlCancelSoftwareUpdateAction"

        fun with(block: CarServer_VehicleControlCancelSoftwareUpdateAction.() -> Unit): CarServer_VehicleControlCancelSoftwareUpdateAction =
            CarServer_VehicleControlCancelSoftwareUpdateAction().apply(block)
    }
}

class CarServer_VehicleControlFlashLightsAction() : GeneratedMessage() {
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
        if (other !is CarServer_VehicleControlFlashLightsAction) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_VehicleControlFlashLightsAction {
        val result = CarServer_VehicleControlFlashLightsAction()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.VehicleControlFlashLightsAction"

        fun with(block: CarServer_VehicleControlFlashLightsAction.() -> Unit): CarServer_VehicleControlFlashLightsAction =
            CarServer_VehicleControlFlashLightsAction().apply(block)
    }
}

class CarServer_VehicleControlHonkHornAction() : GeneratedMessage() {
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
        if (other !is CarServer_VehicleControlHonkHornAction) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_VehicleControlHonkHornAction {
        val result = CarServer_VehicleControlHonkHornAction()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.VehicleControlHonkHornAction"

        fun with(block: CarServer_VehicleControlHonkHornAction.() -> Unit): CarServer_VehicleControlHonkHornAction =
            CarServer_VehicleControlHonkHornAction().apply(block)
    }
}

class CarServer_VehicleControlResetValetPinAction() : GeneratedMessage() {
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
        if (other !is CarServer_VehicleControlResetValetPinAction) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_VehicleControlResetValetPinAction {
        val result = CarServer_VehicleControlResetValetPinAction()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.VehicleControlResetValetPinAction"

        fun with(block: CarServer_VehicleControlResetValetPinAction.() -> Unit): CarServer_VehicleControlResetValetPinAction =
            CarServer_VehicleControlResetValetPinAction().apply(block)
    }
}

class CarServer_VehicleControlScheduleSoftwareUpdateAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var offsetSec: Int = 0
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularInt32Field()?.let { this.offsetSec = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.offsetSec != 0) {
            visitor.visitSingularInt32Field(this.offsetSec, 1)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_VehicleControlScheduleSoftwareUpdateAction) return false
        if (this.offsetSec != other.offsetSec) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.offsetSec.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_VehicleControlScheduleSoftwareUpdateAction {
        val result = CarServer_VehicleControlScheduleSoftwareUpdateAction()
        result.offsetSec = this.offsetSec
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.VehicleControlScheduleSoftwareUpdateAction"

        fun with(block: CarServer_VehicleControlScheduleSoftwareUpdateAction.() -> Unit): CarServer_VehicleControlScheduleSoftwareUpdateAction =
            CarServer_VehicleControlScheduleSoftwareUpdateAction().apply(block)
    }
}

class CarServer_VehicleControlSetSentryModeAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var on: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularBoolField()?.let { this.on = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.on) {
            visitor.visitSingularBoolField(this.on, 1)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_VehicleControlSetSentryModeAction) return false
        if (this.on != other.on) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.on.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_VehicleControlSetSentryModeAction {
        val result = CarServer_VehicleControlSetSentryModeAction()
        result.on = this.on
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.VehicleControlSetSentryModeAction"

        fun with(block: CarServer_VehicleControlSetSentryModeAction.() -> Unit): CarServer_VehicleControlSetSentryModeAction =
            CarServer_VehicleControlSetSentryModeAction().apply(block)
    }
}

class CarServer_VehicleControlSetValetModeAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var on: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    var password: String = ""
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularBoolField()?.let { this.on = it }
                2 -> decoder.decodeSingularStringField()?.let { this.password = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.on) {
            visitor.visitSingularBoolField(this.on, 1)
        }
        if (this.password.isNotEmpty()) {
            visitor.visitSingularStringField(this.password, 2)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_VehicleControlSetValetModeAction) return false
        if (this.on != other.on) return false
        if (this.password != other.password) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.on.hashCode()
        hash = 31 * hash + this.password.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_VehicleControlSetValetModeAction {
        val result = CarServer_VehicleControlSetValetModeAction()
        result.on = this.on
        result.password = this.password
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.VehicleControlSetValetModeAction"

        fun with(block: CarServer_VehicleControlSetValetModeAction.() -> Unit): CarServer_VehicleControlSetValetModeAction =
            CarServer_VehicleControlSetValetModeAction().apply(block)
    }
}

class CarServer_VehicleControlSunroofOpenCloseAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var sunroofLevel: CarServer_VehicleControlSunroofOpenCloseAction.OneOf_SunroofLevel?
        get() = this._sunroofLevel
        set(value) {
            this._sunroofLevel = value
            _protobufMutated()
        }

    var absoluteLevel: Int
        get() = (this._sunroofLevel as? CarServer_VehicleControlSunroofOpenCloseAction.OneOf_SunroofLevel.absoluteLevel)?.value ?: 0
        set(value) {
            this.sunroofLevel = CarServer_VehicleControlSunroofOpenCloseAction.OneOf_SunroofLevel.absoluteLevel(value)
        }

    var deltaLevel: Int
        get() = (this._sunroofLevel as? CarServer_VehicleControlSunroofOpenCloseAction.OneOf_SunroofLevel.deltaLevel)?.value ?: 0
        set(value) {
            this.sunroofLevel = CarServer_VehicleControlSunroofOpenCloseAction.OneOf_SunroofLevel.deltaLevel(value)
        }

    var action: CarServer_VehicleControlSunroofOpenCloseAction.OneOf_Action?
        get() = this._action
        set(value) {
            this._protobufStore_action(this._protobufCopy_action(value))
            _protobufMutated()
        }

    var vent: CarServer_Void
        get() = (this._action as? CarServer_VehicleControlSunroofOpenCloseAction.OneOf_Action.vent)?.value
            ?: _protobufPending(3, { CarServer_Void() }) { this._protobufStore_action(CarServer_VehicleControlSunroofOpenCloseAction.OneOf_Action.vent(it)) }
        set(value) {
            this.action = CarServer_VehicleControlSunroofOpenCloseAction.OneOf_Action.vent(value)
        }

    var close: CarServer_Void
        get() = (this._action as? CarServer_VehicleControlSunroofOpenCloseAction.OneOf_Action.close)?.value
            ?: _protobufPending(4, { CarServer_Void() }) { this._protobufStore_action(CarServer_VehicleControlSunroofOpenCloseAction.OneOf_Action.close(it)) }
        set(value) {
            this.action = CarServer_VehicleControlSunroofOpenCloseAction.OneOf_Action.close(value)
        }

    var `open`: CarServer_Void
        get() = (this._action as? CarServer_VehicleControlSunroofOpenCloseAction.OneOf_Action.`open`)?.value
            ?: _protobufPending(5, { CarServer_Void() }) { this._protobufStore_action(CarServer_VehicleControlSunroofOpenCloseAction.OneOf_Action.`open`(it)) }
        set(value) {
            this.action = CarServer_VehicleControlSunroofOpenCloseAction.OneOf_Action.`open`(value)
        }

    sealed class OneOf_SunroofLevel(value: Any) : ProtobufOneofCase(value) {
        class absoluteLevel(val value: Int) : OneOf_SunroofLevel(value)
        class deltaLevel(val value: Int) : OneOf_SunroofLevel(value)
    }

    sealed class OneOf_Action(value: Any) : ProtobufOneofCase(value) {
        class vent(val value: CarServer_Void) : OneOf_Action(value)
        class close(val value: CarServer_Void) : OneOf_Action(value)
        class `open`(val value: CarServer_Void) : OneOf_Action(value)
    }

    private var _sunroofLevel: CarServer_VehicleControlSunroofOpenCloseAction.OneOf_SunroofLevel? = null
    private var _action: CarServer_VehicleControlSunroofOpenCloseAction.OneOf_Action? = null

    private fun _protobufStore_action(value: CarServer_VehicleControlSunroofOpenCloseAction.OneOf_Action?) {
        _protobufDropPending(3)
        _protobufDropPending(4)
        _protobufDropPending(5)
        this._action = value
    }

    private fun _protobufCopy_action(value: CarServer_VehicleControlSunroofOpenCloseAction.OneOf_Action?): CarServer_VehicleControlSunroofOpenCloseAction.OneOf_Action? =
        when (value) {
            is CarServer_VehicleControlSunroofOpenCloseAction.OneOf_Action.vent -> CarServer_VehicleControlSunroofOpenCloseAction.OneOf_Action.vent(value.value.copy())
            is CarServer_VehicleControlSunroofOpenCloseAction.OneOf_Action.close -> CarServer_VehicleControlSunroofOpenCloseAction.OneOf_Action.close(value.value.copy())
            is CarServer_VehicleControlSunroofOpenCloseAction.OneOf_Action.`open` -> CarServer_VehicleControlSunroofOpenCloseAction.OneOf_Action.`open`(value.value.copy())
            else -> value
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularInt32Field()?.let { this._sunroofLevel = CarServer_VehicleControlSunroofOpenCloseAction.OneOf_SunroofLevel.absoluteLevel(it) }
                2 -> decoder.decodeSingularSInt32Field()?.let { this._sunroofLevel = CarServer_VehicleControlSunroofOpenCloseAction.OneOf_SunroofLevel.deltaLevel(it) }
                3 -> decoder.decodeSingularMessageField((this._action as? CarServer_VehicleControlSunroofOpenCloseAction.OneOf_Action.vent)?.value) { CarServer_Void() }
                    ?.let { this._protobufStore_action(CarServer_VehicleControlSunroofOpenCloseAction.OneOf_Action.vent(it)) }
                4 -> decoder.decodeSingularMessageField((this._action as? CarServer_VehicleControlSunroofOpenCloseAction.OneOf_Action.close)?.value) { CarServer_Void() }
                    ?.let { this._protobufStore_action(CarServer_VehicleControlSunroofOpenCloseAction.OneOf_Action.close(it)) }
                5 -> decoder.decodeSingularMessageField((this._action as? CarServer_VehicleControlSunroofOpenCloseAction.OneOf_Action.`open`)?.value) { CarServer_Void() }
                    ?.let { this._protobufStore_action(CarServer_VehicleControlSunroofOpenCloseAction.OneOf_Action.`open`(it)) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        (this._sunroofLevel as? CarServer_VehicleControlSunroofOpenCloseAction.OneOf_SunroofLevel.absoluteLevel)?.let {
            visitor.visitSingularInt32Field(it.value, 1)
        }
        (this._sunroofLevel as? CarServer_VehicleControlSunroofOpenCloseAction.OneOf_SunroofLevel.deltaLevel)?.let {
            visitor.visitSingularSInt32Field(it.value, 2)
        }
        (this._action as? CarServer_VehicleControlSunroofOpenCloseAction.OneOf_Action.vent)?.let {
            visitor.visitSingularMessageField(it.value, 3)
        }
        (this._action as? CarServer_VehicleControlSunroofOpenCloseAction.OneOf_Action.close)?.let {
            visitor.visitSingularMessageField(it.value, 4)
        }
        (this._action as? CarServer_VehicleControlSunroofOpenCloseAction.OneOf_Action.`open`)?.let {
            visitor.visitSingularMessageField(it.value, 5)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_VehicleControlSunroofOpenCloseAction) return false
        if (this._sunroofLevel != other._sunroofLevel) return false
        if (this._action != other._action) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._sunroofLevel?.hashCode() ?: 0)
        hash = 31 * hash + (this._action?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_VehicleControlSunroofOpenCloseAction {
        val result = CarServer_VehicleControlSunroofOpenCloseAction()
        result._sunroofLevel = this._sunroofLevel
        result._action = this._protobufCopy_action(this._action)
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.VehicleControlSunroofOpenCloseAction"

        fun with(block: CarServer_VehicleControlSunroofOpenCloseAction.() -> Unit): CarServer_VehicleControlSunroofOpenCloseAction =
            CarServer_VehicleControlSunroofOpenCloseAction().apply(block)
    }
}

class CarServer_VehicleControlTriggerHomelinkAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var location: CarServer_LatLong
        get() = this._location ?: _protobufPending(1, { CarServer_LatLong() }) { this._location = it }
        set(value) {
            _protobufDropPending(1)
            this._location = value.copy()
            _protobufMutated()
        }

    val hasLocation: Boolean
        get() = this._location != null

    fun clearLocation() {
        _protobufDropPending(1)
        this._location = null
        _protobufMutated()
    }

    var token: String = ""
        set(value) {
            field = value
            _protobufMutated()
        }

    private var _location: CarServer_LatLong? = null

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularMessageField(this._location) { CarServer_LatLong() }?.let {
                    _protobufDropPending(1)
                    this._location = it
                }
                2 -> decoder.decodeSingularStringField()?.let { this.token = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        this._location?.let {
            visitor.visitSingularMessageField(it, 1)
        }
        if (this.token.isNotEmpty()) {
            visitor.visitSingularStringField(this.token, 2)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_VehicleControlTriggerHomelinkAction) return false
        if (this._location != other._location) return false
        if (this.token != other.token) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._location?.hashCode() ?: 0)
        hash = 31 * hash + this.token.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_VehicleControlTriggerHomelinkAction {
        val result = CarServer_VehicleControlTriggerHomelinkAction()
        result._location = this._location?.copy()
        result.token = this.token
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.VehicleControlTriggerHomelinkAction"

        fun with(block: CarServer_VehicleControlTriggerHomelinkAction.() -> Unit): CarServer_VehicleControlTriggerHomelinkAction =
            CarServer_VehicleControlTriggerHomelinkAction().apply(block)
    }
}

class CarServer_VehicleControlWindowAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var action: CarServer_VehicleControlWindowAction.OneOf_Action?
        get() = this._action
        set(value) {
            this._protobufStore_action(this._protobufCopy_action(value))
            _protobufMutated()
        }

    var unknown: CarServer_Void
        get() = (this._action as? CarServer_VehicleControlWindowAction.OneOf_Action.unknown)?.value
            ?: _protobufPending(2, { CarServer_Void() }) { this._protobufStore_action(CarServer_VehicleControlWindowAction.OneOf_Action.unknown(it)) }
        set(value) {
            this.action = CarServer_VehicleControlWindowAction.OneOf_Action.unknown(value)
        }

    var vent: CarServer_Void
        get() = (this._action as? CarServer_VehicleControlWindowAction.OneOf_Action.vent)?.value
            ?: _protobufPending(3, { CarServer_Void() }) { this._protobufStore_action(CarServer_VehicleControlWindowAction.OneOf_Action.vent(it)) }
        set(value) {
            this.action = CarServer_VehicleControlWindowAction.OneOf_Action.vent(value)
        }

    var close: CarServer_Void
        get() = (this._action as? CarServer_VehicleControlWindowAction.OneOf_Action.close)?.value
            ?: _protobufPending(4, { CarServer_Void() }) { this._protobufStore_action(CarServer_VehicleControlWindowAction.OneOf_Action.close(it)) }
        set(value) {
            this.action = CarServer_VehicleControlWindowAction.OneOf_Action.close(value)
        }

    sealed class OneOf_Action(value: Any) : ProtobufOneofCase(value) {
        class unknown(val value: CarServer_Void) : OneOf_Action(value)
        class vent(val value: CarServer_Void) : OneOf_Action(value)
        class close(val value: CarServer_Void) : OneOf_Action(value)
    }

    private var _action: CarServer_VehicleControlWindowAction.OneOf_Action? = null

    private fun _protobufStore_action(value: CarServer_VehicleControlWindowAction.OneOf_Action?) {
        _protobufDropPending(2)
        _protobufDropPending(3)
        _protobufDropPending(4)
        this._action = value
    }

    private fun _protobufCopy_action(value: CarServer_VehicleControlWindowAction.OneOf_Action?): CarServer_VehicleControlWindowAction.OneOf_Action? =
        when (value) {
            is CarServer_VehicleControlWindowAction.OneOf_Action.unknown -> CarServer_VehicleControlWindowAction.OneOf_Action.unknown(value.value.copy())
            is CarServer_VehicleControlWindowAction.OneOf_Action.vent -> CarServer_VehicleControlWindowAction.OneOf_Action.vent(value.value.copy())
            is CarServer_VehicleControlWindowAction.OneOf_Action.close -> CarServer_VehicleControlWindowAction.OneOf_Action.close(value.value.copy())
            else -> value
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                2 -> decoder.decodeSingularMessageField((this._action as? CarServer_VehicleControlWindowAction.OneOf_Action.unknown)?.value) { CarServer_Void() }
                    ?.let { this._protobufStore_action(CarServer_VehicleControlWindowAction.OneOf_Action.unknown(it)) }
                3 -> decoder.decodeSingularMessageField((this._action as? CarServer_VehicleControlWindowAction.OneOf_Action.vent)?.value) { CarServer_Void() }
                    ?.let { this._protobufStore_action(CarServer_VehicleControlWindowAction.OneOf_Action.vent(it)) }
                4 -> decoder.decodeSingularMessageField((this._action as? CarServer_VehicleControlWindowAction.OneOf_Action.close)?.value) { CarServer_Void() }
                    ?.let { this._protobufStore_action(CarServer_VehicleControlWindowAction.OneOf_Action.close(it)) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        (this._action as? CarServer_VehicleControlWindowAction.OneOf_Action.unknown)?.let {
            visitor.visitSingularMessageField(it.value, 2)
        }
        (this._action as? CarServer_VehicleControlWindowAction.OneOf_Action.vent)?.let {
            visitor.visitSingularMessageField(it.value, 3)
        }
        (this._action as? CarServer_VehicleControlWindowAction.OneOf_Action.close)?.let {
            visitor.visitSingularMessageField(it.value, 4)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_VehicleControlWindowAction) return false
        if (this._action != other._action) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._action?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_VehicleControlWindowAction {
        val result = CarServer_VehicleControlWindowAction()
        result._action = this._protobufCopy_action(this._action)
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.VehicleControlWindowAction"

        fun with(block: CarServer_VehicleControlWindowAction.() -> Unit): CarServer_VehicleControlWindowAction =
            CarServer_VehicleControlWindowAction().apply(block)
    }
}

class CarServer_HvacBioweaponModeAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var on: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    var manualOverride: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularBoolField()?.let { this.on = it }
                2 -> decoder.decodeSingularBoolField()?.let { this.manualOverride = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.on) {
            visitor.visitSingularBoolField(this.on, 1)
        }
        if (this.manualOverride) {
            visitor.visitSingularBoolField(this.manualOverride, 2)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_HvacBioweaponModeAction) return false
        if (this.on != other.on) return false
        if (this.manualOverride != other.manualOverride) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.on.hashCode()
        hash = 31 * hash + this.manualOverride.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_HvacBioweaponModeAction {
        val result = CarServer_HvacBioweaponModeAction()
        result.on = this.on
        result.manualOverride = this.manualOverride
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.HvacBioweaponModeAction"

        fun with(block: CarServer_HvacBioweaponModeAction.() -> Unit): CarServer_HvacBioweaponModeAction =
            CarServer_HvacBioweaponModeAction().apply(block)
    }
}

class CarServer_AutoSeatClimateAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var carseat: MutableList<CarServer_AutoSeatClimateAction.CarSeat>
        get() = this._carseat
        set(value) {
            this._carseat = ProtobufList<CarServer_AutoSeatClimateAction.CarSeat>(this) { it.copy() }.apply { addAll(value) }
            _protobufMutated()
        }

    enum class AutoSeatPosition_E(val rawValue: Int) {
        autoSeatPositionUnknown(0),
        autoSeatPositionFrontLeft(1),
        autoSeatPositionFrontRight(2),
        UNRECOGNIZED(-1),
        ;

        companion object {
            val allCases: List<CarServer_AutoSeatClimateAction.AutoSeatPosition_E> = listOf(
                autoSeatPositionUnknown,
                autoSeatPositionFrontLeft,
                autoSeatPositionFrontRight,
            )

            fun fromRawValue(rawValue: Int): CarServer_AutoSeatClimateAction.AutoSeatPosition_E =
                when (rawValue) {
                    0 -> autoSeatPositionUnknown
                    1 -> autoSeatPositionFrontLeft
                    2 -> autoSeatPositionFrontRight
                    else -> UNRECOGNIZED
                }
        }
    }

    class CarSeat() : GeneratedMessage() {
        constructor(serializedBytes: ByteArray) : this() {
            merge(serializedBytes)
        }

        var on: Boolean = false
            set(value) {
                field = value
                _protobufMutated()
            }

        var seatPosition: CarServer_AutoSeatClimateAction.AutoSeatPosition_E = CarServer_AutoSeatClimateAction.AutoSeatPosition_E.autoSeatPositionUnknown
            set(value) {
                field = value
                _protobufForgetUnrecognized(2)
                _protobufMutated()
            }

        override fun decodeMessage(decoder: BinaryDecoder) {
            while (true) {
                when (decoder.nextFieldNumber() ?: return) {
                    1 -> decoder.decodeSingularBoolField()?.let { this.on = it }
                    2 -> decoder.decodeSingularOpenEnumField(CarServer_AutoSeatClimateAction.AutoSeatPosition_E.UNRECOGNIZED) { CarServer_AutoSeatClimateAction.AutoSeatPosition_E.fromRawValue(it) }
                        ?.let { this.seatPosition = it }
                }
            }
        }

        override fun traverse(visitor: BinaryEncodingVisitor) {
            if (this.on) {
                visitor.visitSingularBoolField(this.on, 1)
            }
            if (this.seatPosition != CarServer_AutoSeatClimateAction.AutoSeatPosition_E.autoSeatPositionUnknown && this.seatPosition != CarServer_AutoSeatClimateAction.AutoSeatPosition_E.UNRECOGNIZED) {
                visitor.visitSingularEnumField(this.seatPosition.rawValue, 2)
            }
            visitor.visitUnknown(unknownFields)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CarServer_AutoSeatClimateAction.CarSeat) return false
            if (this.on != other.on) return false
            if (this.seatPosition != other.seatPosition) return false
            return this.unknownFields.contentEquals(other.unknownFields)
        }

        override fun hashCode(): Int {
            var hash = 0
            hash = 31 * hash + this.on.hashCode()
            hash = 31 * hash + this.seatPosition.hashCode()
            hash = 31 * hash + this.unknownFields.contentHashCode()
            return hash
        }

        override fun copy(): CarServer_AutoSeatClimateAction.CarSeat {
            val result = CarServer_AutoSeatClimateAction.CarSeat()
            result.on = this.on
            result.seatPosition = this.seatPosition
            result.unknownFields = this.unknownFields
            return result
        }

        companion object {
            const val protoMessageName: String = "CarServer.AutoSeatClimateAction.CarSeat"

            fun with(block: CarServer_AutoSeatClimateAction.CarSeat.() -> Unit): CarServer_AutoSeatClimateAction.CarSeat =
                CarServer_AutoSeatClimateAction.CarSeat().apply(block)
        }
    }

    private var _carseat: ProtobufList<CarServer_AutoSeatClimateAction.CarSeat> = ProtobufList<CarServer_AutoSeatClimateAction.CarSeat>(this) { it.copy() }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeRepeatedMessageField(this._carseat) { CarServer_AutoSeatClimateAction.CarSeat() }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this._carseat.isNotEmpty()) {
            visitor.visitRepeatedMessageField(this._carseat, 1)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_AutoSeatClimateAction) return false
        if (this._carseat != other._carseat) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this._carseat.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_AutoSeatClimateAction {
        val result = CarServer_AutoSeatClimateAction()
        result._carseat.addAll(this._carseat)
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.AutoSeatClimateAction"

        fun with(block: CarServer_AutoSeatClimateAction.() -> Unit): CarServer_AutoSeatClimateAction =
            CarServer_AutoSeatClimateAction().apply(block)
    }
}

class CarServer_Ping() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var pingID: Int = 0
        set(value) {
            field = value
            _protobufMutated()
        }

    var localTimestamp: Google_Protobuf_Timestamp
        get() = this._localTimestamp ?: _protobufPending(2, { Google_Protobuf_Timestamp() }) { this._localTimestamp = it }
        set(value) {
            _protobufDropPending(2)
            this._localTimestamp = value.copy()
            _protobufMutated()
        }

    val hasLocalTimestamp: Boolean
        get() = this._localTimestamp != null

    fun clearLocalTimestamp() {
        _protobufDropPending(2)
        this._localTimestamp = null
        _protobufMutated()
    }

    var lastRemoteTimestamp: Google_Protobuf_Timestamp
        get() = this._lastRemoteTimestamp ?: _protobufPending(3, { Google_Protobuf_Timestamp() }) { this._lastRemoteTimestamp = it }
        set(value) {
            _protobufDropPending(3)
            this._lastRemoteTimestamp = value.copy()
            _protobufMutated()
        }

    val hasLastRemoteTimestamp: Boolean
        get() = this._lastRemoteTimestamp != null

    fun clearLastRemoteTimestamp() {
        _protobufDropPending(3)
        this._lastRemoteTimestamp = null
        _protobufMutated()
    }

    private var _localTimestamp: Google_Protobuf_Timestamp? = null
    private var _lastRemoteTimestamp: Google_Protobuf_Timestamp? = null

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularInt32Field()?.let { this.pingID = it }
                2 -> decoder.decodeSingularMessageField(this._localTimestamp) { Google_Protobuf_Timestamp() }?.let {
                    _protobufDropPending(2)
                    this._localTimestamp = it
                }
                3 -> decoder.decodeSingularMessageField(this._lastRemoteTimestamp) { Google_Protobuf_Timestamp() }?.let {
                    _protobufDropPending(3)
                    this._lastRemoteTimestamp = it
                }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.pingID != 0) {
            visitor.visitSingularInt32Field(this.pingID, 1)
        }
        this._localTimestamp?.let {
            visitor.visitSingularMessageField(it, 2)
        }
        this._lastRemoteTimestamp?.let {
            visitor.visitSingularMessageField(it, 3)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_Ping) return false
        if (this.pingID != other.pingID) return false
        if (this._localTimestamp != other._localTimestamp) return false
        if (this._lastRemoteTimestamp != other._lastRemoteTimestamp) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.pingID.hashCode()
        hash = 31 * hash + (this._localTimestamp?.hashCode() ?: 0)
        hash = 31 * hash + (this._lastRemoteTimestamp?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_Ping {
        val result = CarServer_Ping()
        result.pingID = this.pingID
        result._localTimestamp = this._localTimestamp?.copy()
        result._lastRemoteTimestamp = this._lastRemoteTimestamp?.copy()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.Ping"

        fun with(block: CarServer_Ping.() -> Unit): CarServer_Ping =
            CarServer_Ping().apply(block)
    }
}

class CarServer_ScheduledChargingAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var enabled: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    var chargingTime: Int = 0
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularBoolField()?.let { this.enabled = it }
                2 -> decoder.decodeSingularInt32Field()?.let { this.chargingTime = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.enabled) {
            visitor.visitSingularBoolField(this.enabled, 1)
        }
        if (this.chargingTime != 0) {
            visitor.visitSingularInt32Field(this.chargingTime, 2)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_ScheduledChargingAction) return false
        if (this.enabled != other.enabled) return false
        if (this.chargingTime != other.chargingTime) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.enabled.hashCode()
        hash = 31 * hash + this.chargingTime.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ScheduledChargingAction {
        val result = CarServer_ScheduledChargingAction()
        result.enabled = this.enabled
        result.chargingTime = this.chargingTime
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ScheduledChargingAction"

        fun with(block: CarServer_ScheduledChargingAction.() -> Unit): CarServer_ScheduledChargingAction =
            CarServer_ScheduledChargingAction().apply(block)
    }
}

class CarServer_ScheduledDepartureAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var enabled: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    var departureTime: Int = 0
        set(value) {
            field = value
            _protobufMutated()
        }

    var preconditioningTimes: CarServer_PreconditioningTimes
        get() = this._preconditioningTimes ?: _protobufPending(3, { CarServer_PreconditioningTimes() }) { this._preconditioningTimes = it }
        set(value) {
            _protobufDropPending(3)
            this._preconditioningTimes = value.copy()
            _protobufMutated()
        }

    val hasPreconditioningTimes: Boolean
        get() = this._preconditioningTimes != null

    fun clearPreconditioningTimes() {
        _protobufDropPending(3)
        this._preconditioningTimes = null
        _protobufMutated()
    }

    var offPeakChargingTimes: CarServer_OffPeakChargingTimes
        get() = this._offPeakChargingTimes ?: _protobufPending(4, { CarServer_OffPeakChargingTimes() }) { this._offPeakChargingTimes = it }
        set(value) {
            _protobufDropPending(4)
            this._offPeakChargingTimes = value.copy()
            _protobufMutated()
        }

    val hasOffPeakChargingTimes: Boolean
        get() = this._offPeakChargingTimes != null

    fun clearOffPeakChargingTimes() {
        _protobufDropPending(4)
        this._offPeakChargingTimes = null
        _protobufMutated()
    }

    var offPeakHoursEndTime: Int = 0
        set(value) {
            field = value
            _protobufMutated()
        }

    private var _preconditioningTimes: CarServer_PreconditioningTimes? = null
    private var _offPeakChargingTimes: CarServer_OffPeakChargingTimes? = null

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularBoolField()?.let { this.enabled = it }
                2 -> decoder.decodeSingularInt32Field()?.let { this.departureTime = it }
                3 -> decoder.decodeSingularMessageField(this._preconditioningTimes) { CarServer_PreconditioningTimes() }?.let {
                    _protobufDropPending(3)
                    this._preconditioningTimes = it
                }
                4 -> decoder.decodeSingularMessageField(this._offPeakChargingTimes) { CarServer_OffPeakChargingTimes() }?.let {
                    _protobufDropPending(4)
                    this._offPeakChargingTimes = it
                }
                5 -> decoder.decodeSingularInt32Field()?.let { this.offPeakHoursEndTime = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.enabled) {
            visitor.visitSingularBoolField(this.enabled, 1)
        }
        if (this.departureTime != 0) {
            visitor.visitSingularInt32Field(this.departureTime, 2)
        }
        this._preconditioningTimes?.let {
            visitor.visitSingularMessageField(it, 3)
        }
        this._offPeakChargingTimes?.let {
            visitor.visitSingularMessageField(it, 4)
        }
        if (this.offPeakHoursEndTime != 0) {
            visitor.visitSingularInt32Field(this.offPeakHoursEndTime, 5)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_ScheduledDepartureAction) return false
        if (this.enabled != other.enabled) return false
        if (this.departureTime != other.departureTime) return false
        if (this._preconditioningTimes != other._preconditioningTimes) return false
        if (this._offPeakChargingTimes != other._offPeakChargingTimes) return false
        if (this.offPeakHoursEndTime != other.offPeakHoursEndTime) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.enabled.hashCode()
        hash = 31 * hash + this.departureTime.hashCode()
        hash = 31 * hash + (this._preconditioningTimes?.hashCode() ?: 0)
        hash = 31 * hash + (this._offPeakChargingTimes?.hashCode() ?: 0)
        hash = 31 * hash + this.offPeakHoursEndTime.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ScheduledDepartureAction {
        val result = CarServer_ScheduledDepartureAction()
        result.enabled = this.enabled
        result.departureTime = this.departureTime
        result._preconditioningTimes = this._preconditioningTimes?.copy()
        result._offPeakChargingTimes = this._offPeakChargingTimes?.copy()
        result.offPeakHoursEndTime = this.offPeakHoursEndTime
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ScheduledDepartureAction"

        fun with(block: CarServer_ScheduledDepartureAction.() -> Unit): CarServer_ScheduledDepartureAction =
            CarServer_ScheduledDepartureAction().apply(block)
    }
}

class CarServer_HvacClimateKeeperAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var climateKeeperAction: CarServer_HvacClimateKeeperAction.ClimateKeeperAction_E = CarServer_HvacClimateKeeperAction.ClimateKeeperAction_E.climateKeeperActionOff
        set(value) {
            field = value
            _protobufForgetUnrecognized(1)
            _protobufMutated()
        }

    var manualOverride: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    enum class ClimateKeeperAction_E(val rawValue: Int) {
        climateKeeperActionOff(0),
        climateKeeperActionOn(1),
        climateKeeperActionDog(2),
        climateKeeperActionCamp(3),
        UNRECOGNIZED(-1),
        ;

        companion object {
            val allCases: List<CarServer_HvacClimateKeeperAction.ClimateKeeperAction_E> = listOf(
                climateKeeperActionOff,
                climateKeeperActionOn,
                climateKeeperActionDog,
                climateKeeperActionCamp,
            )

            fun fromRawValue(rawValue: Int): CarServer_HvacClimateKeeperAction.ClimateKeeperAction_E =
                when (rawValue) {
                    0 -> climateKeeperActionOff
                    1 -> climateKeeperActionOn
                    2 -> climateKeeperActionDog
                    3 -> climateKeeperActionCamp
                    else -> UNRECOGNIZED
                }
        }
    }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularOpenEnumField(CarServer_HvacClimateKeeperAction.ClimateKeeperAction_E.UNRECOGNIZED) { CarServer_HvacClimateKeeperAction.ClimateKeeperAction_E.fromRawValue(it) }
                    ?.let { this.climateKeeperAction = it }
                2 -> decoder.decodeSingularBoolField()?.let { this.manualOverride = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.climateKeeperAction != CarServer_HvacClimateKeeperAction.ClimateKeeperAction_E.climateKeeperActionOff && this.climateKeeperAction != CarServer_HvacClimateKeeperAction.ClimateKeeperAction_E.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.climateKeeperAction.rawValue, 1)
        }
        if (this.manualOverride) {
            visitor.visitSingularBoolField(this.manualOverride, 2)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_HvacClimateKeeperAction) return false
        if (this.climateKeeperAction != other.climateKeeperAction) return false
        if (this.manualOverride != other.manualOverride) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.climateKeeperAction.hashCode()
        hash = 31 * hash + this.manualOverride.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_HvacClimateKeeperAction {
        val result = CarServer_HvacClimateKeeperAction()
        result.climateKeeperAction = this.climateKeeperAction
        result.manualOverride = this.manualOverride
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.HvacClimateKeeperAction"

        fun with(block: CarServer_HvacClimateKeeperAction.() -> Unit): CarServer_HvacClimateKeeperAction =
            CarServer_HvacClimateKeeperAction().apply(block)
    }
}

class CarServer_SetChargingAmpsAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var chargingAmps: Int = 0
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularInt32Field()?.let { this.chargingAmps = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.chargingAmps != 0) {
            visitor.visitSingularInt32Field(this.chargingAmps, 1)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_SetChargingAmpsAction) return false
        if (this.chargingAmps != other.chargingAmps) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.chargingAmps.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_SetChargingAmpsAction {
        val result = CarServer_SetChargingAmpsAction()
        result.chargingAmps = this.chargingAmps
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.SetChargingAmpsAction"

        fun with(block: CarServer_SetChargingAmpsAction.() -> Unit): CarServer_SetChargingAmpsAction =
            CarServer_SetChargingAmpsAction().apply(block)
    }
}

class CarServer_RemoveChargeScheduleAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var id: ULong = 0uL
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularUInt64Field()?.let { this.id = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.id != 0uL) {
            visitor.visitSingularUInt64Field(this.id, 1)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_RemoveChargeScheduleAction) return false
        if (this.id != other.id) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.id.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_RemoveChargeScheduleAction {
        val result = CarServer_RemoveChargeScheduleAction()
        result.id = this.id
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.RemoveChargeScheduleAction"

        fun with(block: CarServer_RemoveChargeScheduleAction.() -> Unit): CarServer_RemoveChargeScheduleAction =
            CarServer_RemoveChargeScheduleAction().apply(block)
    }
}

class CarServer_BatchRemoveChargeSchedulesAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var home: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    var work: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    var other: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularBoolField()?.let { this.home = it }
                2 -> decoder.decodeSingularBoolField()?.let { this.work = it }
                3 -> decoder.decodeSingularBoolField()?.let { this.other = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.home) {
            visitor.visitSingularBoolField(this.home, 1)
        }
        if (this.work) {
            visitor.visitSingularBoolField(this.work, 2)
        }
        if (this.other) {
            visitor.visitSingularBoolField(this.other, 3)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_BatchRemoveChargeSchedulesAction) return false
        if (this.home != other.home) return false
        if (this.work != other.work) return false
        if (this.other != other.other) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.home.hashCode()
        hash = 31 * hash + this.work.hashCode()
        hash = 31 * hash + this.other.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_BatchRemoveChargeSchedulesAction {
        val result = CarServer_BatchRemoveChargeSchedulesAction()
        result.home = this.home
        result.work = this.work
        result.other = this.other
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.BatchRemoveChargeSchedulesAction"

        fun with(block: CarServer_BatchRemoveChargeSchedulesAction.() -> Unit): CarServer_BatchRemoveChargeSchedulesAction =
            CarServer_BatchRemoveChargeSchedulesAction().apply(block)
    }
}

class CarServer_BatchRemovePreconditionSchedulesAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var home: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    var work: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    var other: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularBoolField()?.let { this.home = it }
                2 -> decoder.decodeSingularBoolField()?.let { this.work = it }
                3 -> decoder.decodeSingularBoolField()?.let { this.other = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.home) {
            visitor.visitSingularBoolField(this.home, 1)
        }
        if (this.work) {
            visitor.visitSingularBoolField(this.work, 2)
        }
        if (this.other) {
            visitor.visitSingularBoolField(this.other, 3)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_BatchRemovePreconditionSchedulesAction) return false
        if (this.home != other.home) return false
        if (this.work != other.work) return false
        if (this.other != other.other) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.home.hashCode()
        hash = 31 * hash + this.work.hashCode()
        hash = 31 * hash + this.other.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_BatchRemovePreconditionSchedulesAction {
        val result = CarServer_BatchRemovePreconditionSchedulesAction()
        result.home = this.home
        result.work = this.work
        result.other = this.other
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.BatchRemovePreconditionSchedulesAction"

        fun with(block: CarServer_BatchRemovePreconditionSchedulesAction.() -> Unit): CarServer_BatchRemovePreconditionSchedulesAction =
            CarServer_BatchRemovePreconditionSchedulesAction().apply(block)
    }
}

class CarServer_RemovePreconditionScheduleAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var id: ULong = 0uL
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularUInt64Field()?.let { this.id = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.id != 0uL) {
            visitor.visitSingularUInt64Field(this.id, 1)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_RemovePreconditionScheduleAction) return false
        if (this.id != other.id) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.id.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_RemovePreconditionScheduleAction {
        val result = CarServer_RemovePreconditionScheduleAction()
        result.id = this.id
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.RemovePreconditionScheduleAction"

        fun with(block: CarServer_RemovePreconditionScheduleAction.() -> Unit): CarServer_RemovePreconditionScheduleAction =
            CarServer_RemovePreconditionScheduleAction().apply(block)
    }
}

class CarServer_SetCabinOverheatProtectionAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var on: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    var fanOnly: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularBoolField()?.let { this.on = it }
                2 -> decoder.decodeSingularBoolField()?.let { this.fanOnly = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.on) {
            visitor.visitSingularBoolField(this.on, 1)
        }
        if (this.fanOnly) {
            visitor.visitSingularBoolField(this.fanOnly, 2)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_SetCabinOverheatProtectionAction) return false
        if (this.on != other.on) return false
        if (this.fanOnly != other.fanOnly) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.on.hashCode()
        hash = 31 * hash + this.fanOnly.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_SetCabinOverheatProtectionAction {
        val result = CarServer_SetCabinOverheatProtectionAction()
        result.on = this.on
        result.fanOnly = this.fanOnly
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.SetCabinOverheatProtectionAction"

        fun with(block: CarServer_SetCabinOverheatProtectionAction.() -> Unit): CarServer_SetCabinOverheatProtectionAction =
            CarServer_SetCabinOverheatProtectionAction().apply(block)
    }
}

class CarServer_SetVehicleNameAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var vehicleName: String = ""
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularStringField()?.let { this.vehicleName = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.vehicleName.isNotEmpty()) {
            visitor.visitSingularStringField(this.vehicleName, 1)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_SetVehicleNameAction) return false
        if (this.vehicleName != other.vehicleName) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.vehicleName.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_SetVehicleNameAction {
        val result = CarServer_SetVehicleNameAction()
        result.vehicleName = this.vehicleName
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.SetVehicleNameAction"

        fun with(block: CarServer_SetVehicleNameAction.() -> Unit): CarServer_SetVehicleNameAction =
            CarServer_SetVehicleNameAction().apply(block)
    }
}

class CarServer_ChargePortDoorClose() : GeneratedMessage() {
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
        if (other !is CarServer_ChargePortDoorClose) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ChargePortDoorClose {
        val result = CarServer_ChargePortDoorClose()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ChargePortDoorClose"

        fun with(block: CarServer_ChargePortDoorClose.() -> Unit): CarServer_ChargePortDoorClose =
            CarServer_ChargePortDoorClose().apply(block)
    }
}

class CarServer_ChargePortDoorOpen() : GeneratedMessage() {
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
        if (other !is CarServer_ChargePortDoorOpen) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ChargePortDoorOpen {
        val result = CarServer_ChargePortDoorOpen()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ChargePortDoorOpen"

        fun with(block: CarServer_ChargePortDoorOpen.() -> Unit): CarServer_ChargePortDoorOpen =
            CarServer_ChargePortDoorOpen().apply(block)
    }
}

class CarServer_SetCopTempAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var copActivationTemp: CarServer_ClimateState.CopActivationTemp = CarServer_ClimateState.CopActivationTemp.unspecified
        set(value) {
            field = value
            _protobufForgetUnrecognized(1)
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularOpenEnumField(CarServer_ClimateState.CopActivationTemp.UNRECOGNIZED) { CarServer_ClimateState.CopActivationTemp.fromRawValue(it) }
                    ?.let { this.copActivationTemp = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.copActivationTemp != CarServer_ClimateState.CopActivationTemp.unspecified && this.copActivationTemp != CarServer_ClimateState.CopActivationTemp.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.copActivationTemp.rawValue, 1)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_SetCopTempAction) return false
        if (this.copActivationTemp != other.copActivationTemp) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.copActivationTemp.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_SetCopTempAction {
        val result = CarServer_SetCopTempAction()
        result.copActivationTemp = this.copActivationTemp
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.SetCopTempAction"

        fun with(block: CarServer_SetCopTempAction.() -> Unit): CarServer_SetCopTempAction =
            CarServer_SetCopTempAction().apply(block)
    }
}

class CarServer_VehicleControlSetPinToDriveAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var on: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    var password: String = ""
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularBoolField()?.let { this.on = it }
                2 -> decoder.decodeSingularStringField()?.let { this.password = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.on) {
            visitor.visitSingularBoolField(this.on, 1)
        }
        if (this.password.isNotEmpty()) {
            visitor.visitSingularStringField(this.password, 2)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_VehicleControlSetPinToDriveAction) return false
        if (this.on != other.on) return false
        if (this.password != other.password) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.on.hashCode()
        hash = 31 * hash + this.password.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_VehicleControlSetPinToDriveAction {
        val result = CarServer_VehicleControlSetPinToDriveAction()
        result.on = this.on
        result.password = this.password
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.VehicleControlSetPinToDriveAction"

        fun with(block: CarServer_VehicleControlSetPinToDriveAction.() -> Unit): CarServer_VehicleControlSetPinToDriveAction =
            CarServer_VehicleControlSetPinToDriveAction().apply(block)
    }
}

class CarServer_VehicleControlResetPinToDriveAction() : GeneratedMessage() {
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
        if (other !is CarServer_VehicleControlResetPinToDriveAction) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_VehicleControlResetPinToDriveAction {
        val result = CarServer_VehicleControlResetPinToDriveAction()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.VehicleControlResetPinToDriveAction"

        fun with(block: CarServer_VehicleControlResetPinToDriveAction.() -> Unit): CarServer_VehicleControlResetPinToDriveAction =
            CarServer_VehicleControlResetPinToDriveAction().apply(block)
    }
}

class CarServer_ParentalControlsClearPinAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var pin: String = ""
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularStringField()?.let { this.pin = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.pin.isNotEmpty()) {
            visitor.visitSingularStringField(this.pin, 1)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_ParentalControlsClearPinAction) return false
        if (this.pin != other.pin) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.pin.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ParentalControlsClearPinAction {
        val result = CarServer_ParentalControlsClearPinAction()
        result.pin = this.pin
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ParentalControlsClearPinAction"

        fun with(block: CarServer_ParentalControlsClearPinAction.() -> Unit): CarServer_ParentalControlsClearPinAction =
            CarServer_ParentalControlsClearPinAction().apply(block)
    }
}

class CarServer_ParentalControlsClearPinAdminAction() : GeneratedMessage() {
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
        if (other !is CarServer_ParentalControlsClearPinAdminAction) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ParentalControlsClearPinAdminAction {
        val result = CarServer_ParentalControlsClearPinAdminAction()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ParentalControlsClearPinAdminAction"

        fun with(block: CarServer_ParentalControlsClearPinAdminAction.() -> Unit): CarServer_ParentalControlsClearPinAdminAction =
            CarServer_ParentalControlsClearPinAdminAction().apply(block)
    }
}

class CarServer_ParentalControlsAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var activate: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    var pin: String = ""
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularBoolField()?.let { this.activate = it }
                2 -> decoder.decodeSingularStringField()?.let { this.pin = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.activate) {
            visitor.visitSingularBoolField(this.activate, 1)
        }
        if (this.pin.isNotEmpty()) {
            visitor.visitSingularStringField(this.pin, 2)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_ParentalControlsAction) return false
        if (this.activate != other.activate) return false
        if (this.pin != other.pin) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.activate.hashCode()
        hash = 31 * hash + this.pin.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ParentalControlsAction {
        val result = CarServer_ParentalControlsAction()
        result.activate = this.activate
        result.pin = this.pin
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ParentalControlsAction"

        fun with(block: CarServer_ParentalControlsAction.() -> Unit): CarServer_ParentalControlsAction =
            CarServer_ParentalControlsAction().apply(block)
    }
}

class CarServer_ParentalControlsEnableSettingsAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var setting: CarServer_ParentalControlsEnableSettingsAction.ParentalControlsSetting_E = CarServer_ParentalControlsEnableSettingsAction.ParentalControlsSetting_E.speedLimit
        set(value) {
            field = value
            _protobufForgetUnrecognized(1)
            _protobufMutated()
        }

    var enable: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    enum class ParentalControlsSetting_E(val rawValue: Int) {
        speedLimit(0),
        acceleration(1),
        safetyFeatures(2),
        curfew(3),
        browserBlocked(4),
        theaterBlocked(5),
        arcadeBlocked(6),
        UNRECOGNIZED(-1),
        ;

        companion object {
            val allCases: List<CarServer_ParentalControlsEnableSettingsAction.ParentalControlsSetting_E> = listOf(
                speedLimit,
                acceleration,
                safetyFeatures,
                curfew,
                browserBlocked,
                theaterBlocked,
                arcadeBlocked,
            )

            fun fromRawValue(rawValue: Int): CarServer_ParentalControlsEnableSettingsAction.ParentalControlsSetting_E =
                when (rawValue) {
                    0 -> speedLimit
                    1 -> acceleration
                    2 -> safetyFeatures
                    3 -> curfew
                    4 -> browserBlocked
                    5 -> theaterBlocked
                    6 -> arcadeBlocked
                    else -> UNRECOGNIZED
                }
        }
    }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularOpenEnumField(CarServer_ParentalControlsEnableSettingsAction.ParentalControlsSetting_E.UNRECOGNIZED) { CarServer_ParentalControlsEnableSettingsAction.ParentalControlsSetting_E.fromRawValue(it) }
                    ?.let { this.setting = it }
                2 -> decoder.decodeSingularBoolField()?.let { this.enable = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.setting != CarServer_ParentalControlsEnableSettingsAction.ParentalControlsSetting_E.speedLimit && this.setting != CarServer_ParentalControlsEnableSettingsAction.ParentalControlsSetting_E.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.setting.rawValue, 1)
        }
        if (this.enable) {
            visitor.visitSingularBoolField(this.enable, 2)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_ParentalControlsEnableSettingsAction) return false
        if (this.setting != other.setting) return false
        if (this.enable != other.enable) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.setting.hashCode()
        hash = 31 * hash + this.enable.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ParentalControlsEnableSettingsAction {
        val result = CarServer_ParentalControlsEnableSettingsAction()
        result.setting = this.setting
        result.enable = this.enable
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ParentalControlsEnableSettingsAction"

        fun with(block: CarServer_ParentalControlsEnableSettingsAction.() -> Unit): CarServer_ParentalControlsEnableSettingsAction =
            CarServer_ParentalControlsEnableSettingsAction().apply(block)
    }
}

class CarServer_ParentalControlsSetSpeedLimitAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var limitMph: Double = 0.0
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularDoubleField()?.let { this.limitMph = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.limitMph.toRawBits() != 0L) {
            visitor.visitSingularDoubleField(this.limitMph, 1)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_ParentalControlsSetSpeedLimitAction) return false
        if (this.limitMph != other.limitMph) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + protobufHash(this.limitMph)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_ParentalControlsSetSpeedLimitAction {
        val result = CarServer_ParentalControlsSetSpeedLimitAction()
        result.limitMph = this.limitMph
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.ParentalControlsSetSpeedLimitAction"

        fun with(block: CarServer_ParentalControlsSetSpeedLimitAction.() -> Unit): CarServer_ParentalControlsSetSpeedLimitAction =
            CarServer_ParentalControlsSetSpeedLimitAction().apply(block)
    }
}

class CarServer_VehicleControlResetPinToDriveAdminAction() : GeneratedMessage() {
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
        if (other !is CarServer_VehicleControlResetPinToDriveAdminAction) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_VehicleControlResetPinToDriveAdminAction {
        val result = CarServer_VehicleControlResetPinToDriveAdminAction()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.VehicleControlResetPinToDriveAdminAction"

        fun with(block: CarServer_VehicleControlResetPinToDriveAdminAction.() -> Unit): CarServer_VehicleControlResetPinToDriveAdminAction =
            CarServer_VehicleControlResetPinToDriveAdminAction().apply(block)
    }
}

class CarServer_SetLowPowerModeAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var lowPowerMode: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularBoolField()?.let { this.lowPowerMode = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.lowPowerMode) {
            visitor.visitSingularBoolField(this.lowPowerMode, 1)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_SetLowPowerModeAction) return false
        if (this.lowPowerMode != other.lowPowerMode) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.lowPowerMode.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_SetLowPowerModeAction {
        val result = CarServer_SetLowPowerModeAction()
        result.lowPowerMode = this.lowPowerMode
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.SetLowPowerModeAction"

        fun with(block: CarServer_SetLowPowerModeAction.() -> Unit): CarServer_SetLowPowerModeAction =
            CarServer_SetLowPowerModeAction().apply(block)
    }
}

class CarServer_SetKeepAccessoryPowerModeAction() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var keepAccessoryPowerMode: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularBoolField()?.let { this.keepAccessoryPowerMode = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.keepAccessoryPowerMode) {
            visitor.visitSingularBoolField(this.keepAccessoryPowerMode, 1)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CarServer_SetKeepAccessoryPowerModeAction) return false
        if (this.keepAccessoryPowerMode != other.keepAccessoryPowerMode) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.keepAccessoryPowerMode.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): CarServer_SetKeepAccessoryPowerModeAction {
        val result = CarServer_SetKeepAccessoryPowerModeAction()
        result.keepAccessoryPowerMode = this.keepAccessoryPowerMode
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "CarServer.SetKeepAccessoryPowerModeAction"

        fun with(block: CarServer_SetKeepAccessoryPowerModeAction.() -> Unit): CarServer_SetKeepAccessoryPowerModeAction =
            CarServer_SetKeepAccessoryPowerModeAction().apply(block)
    }
}
