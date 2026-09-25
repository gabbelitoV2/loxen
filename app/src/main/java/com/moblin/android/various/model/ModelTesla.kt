package com.moblin.android.various.model

import com.moblin.android.common.various.Measurement
import com.moblin.android.common.various.UnitSpeed
import com.moblin.android.common.various.converted
import com.moblin.android.common.various.formatSpeed
import com.moblin.android.integrations.tesla.TeslaVehicle
import com.moblin.android.integrations.tesla.TeslaVehicleDelegate
import com.moblin.android.integrations.tesla.TeslaVehicleState
import com.moblin.android.integrations.tesla.protobuf.CarServer_ChargeState
import com.moblin.android.integrations.tesla.protobuf.CarServer_DriveState
import com.moblin.android.integrations.tesla.protobuf.CarServer_MediaState
import com.moblin.android.integrations.tesla.protobuf.CarServer_ShiftState
import com.moblin.android.localized
import kotlinx.coroutines.flow.MutableStateFlow

class Tesla {
    var vehicle: TeslaVehicle? = null
    var chargeState = CarServer_ChargeState()
    var driveState = CarServer_DriveState()
    var mediaState = CarServer_MediaState()
    val vehicleState = MutableStateFlow<TeslaVehicleState?>(null)
    val vehicleVehicleSecurityConnected = MutableStateFlow(false)
    val vehicleInfotainmentConnected = MutableStateFlow(false)
}

fun Model.reloadTeslaVehicle() {
    stopTeslaVehicle()
    val peripheralId = database.tesla.bluetoothPeripheralId
    if (database.tesla.enabled && database.tesla.vin != "" && database.tesla.privateKey != "" &&
        peripheralId != null
    ) {
        tesla.vehicle = TeslaVehicle(
            vin = database.tesla.vin,
            privateKeyPem = database.tesla.privateKey,
            peripheralId = peripheralId,
        )
        tesla.vehicle?.delegate = ModelTeslaVehicleDelegate(model = this)
        tesla.vehicleState.value = TeslaVehicleState.idle
        tesla.vehicle?.start()
    }
}

fun Model.stopTeslaVehicle() {
    tesla.vehicle?.delegate = null
    tesla.vehicle?.stop()
    tesla.vehicle = null
    tesla.vehicleState.value = null
    tesla.chargeState = CarServer_ChargeState()
    tesla.driveState = CarServer_DriveState()
    tesla.mediaState = CarServer_MediaState()
    tesla.vehicleVehicleSecurityConnected.value = false
    tesla.vehicleInfotainmentConnected.value = false
}

fun Model.teslaAddKeyToVehicle() {
    tesla.vehicle?.addKeyRequestWithRole(privateKeyPem = database.tesla.privateKey)
    makeToast(title = localized("Tap Locks → Add Key in your Tesla and tap your key card"))
}

fun Model.teslaFlashLights() {
    tesla.vehicle?.flashLights()
}

fun Model.teslaHonk() {
    tesla.vehicle?.honk()
}

fun Model.teslaGetChargeState() {
    tesla.vehicle?.getChargeState { state ->
        tesla.chargeState = state
    }
}

fun Model.teslaGetDriveState() {
    tesla.vehicle?.getDriveState { state ->
        tesla.driveState = state
    }
}

fun Model.teslaGetMediaState() {
    tesla.vehicle?.getMediaState { state ->
        tesla.mediaState = state
    }
}

fun Model.teslaOpenTrunk() {
    tesla.vehicle?.openTrunk()
}

fun Model.teslaCloseTrunk() {
    tesla.vehicle?.closeTrunk()
}

fun Model.mediaNextTrack() {
    tesla.vehicle?.mediaNextTrack()
}

fun Model.mediaPreviousTrack() {
    tesla.vehicle?.mediaPreviousTrack()
}

fun Model.mediaTogglePlayback() {
    tesla.vehicle?.mediaTogglePlayback()
}

fun Model.textEffectTeslaBatteryLevel(): String {
    var teslaBatteryLevel = "-"
    if (tesla.chargeState.optionalBatteryLevel != null) {
        teslaBatteryLevel = "${tesla.chargeState.batteryLevel}%"
        if (tesla.chargeState.chargerPower != 0) {
            teslaBatteryLevel += " ${tesla.chargeState.chargerPower} kW"
        }
        if (tesla.chargeState.optionalMinutesToChargeLimit != null) {
            teslaBatteryLevel += " ${tesla.chargeState.minutesToChargeLimit} minutes left"
        }
    }
    return teslaBatteryLevel
}

fun Model.textEffectTeslaDrive(): String {
    var teslaDrive = "-"
    val shift = tesla.driveState.shiftState.type
    if (shift != null) {
        when (shift) {
            is CarServer_ShiftState.OneOf_Type.invalid -> teslaDrive = "-"
            is CarServer_ShiftState.OneOf_Type.p -> teslaDrive = "P"
            is CarServer_ShiftState.OneOf_Type.r -> teslaDrive = "R"
            is CarServer_ShiftState.OneOf_Type.n -> teslaDrive = "N"
            is CarServer_ShiftState.OneOf_Type.d -> teslaDrive = "D"
            is CarServer_ShiftState.OneOf_Type.sna -> teslaDrive = "SNA"
        }
        if (teslaDrive != "P") {
            val speed = (tesla.driveState.optionalSpeed as? CarServer_DriveState.OneOf_OptionalSpeed.speed)?.value
            if (speed != null) {
                val metersPerSecond = Measurement(value = speed.toDouble(), unit = UnitSpeed.milesPerHour)
                    .converted(to = UnitSpeed.metersPerSecond)
                    .value
                teslaDrive += " ${formatSpeed(speed = metersPerSecond)}"
            }
            val power = (tesla.driveState.optionalPower as? CarServer_DriveState.OneOf_OptionalPower.power)?.value
            if (power != null) {
                teslaDrive += " $power kW"
            }
        }
    }
    return teslaDrive
}

fun Model.textEffectTeslaMedia(): String {
    var teslaMedia = "-"
    val artist = (tesla.mediaState.optionalNowPlayingArtist as? CarServer_MediaState.OneOf_OptionalNowPlayingArtist.nowPlayingArtist)?.value
    val title = (tesla.mediaState.optionalNowPlayingTitle as? CarServer_MediaState.OneOf_OptionalNowPlayingTitle.nowPlayingTitle)?.value
    if (artist != null && title != null) {
        if (artist.isEmpty()) {
            teslaMedia = title
        } else {
            teslaMedia = "$artist - $title"
        }
    }
    return teslaMedia
}

fun Model.teslaVehicleState(vehicle: TeslaVehicle, state: TeslaVehicleState) {
    when (state) {
        TeslaVehicleState.idle -> reloadTeslaVehicle()
        TeslaVehicleState.connecting -> {
            tesla.vehicleVehicleSecurityConnected.value = false
            tesla.vehicleInfotainmentConnected.value = false
        }
        TeslaVehicleState.connected -> makeToast(title = localized("Connected to your Tesla"))
    }
    tesla.vehicleState.value = state
}

fun Model.teslaVehicleVehicleSecurityConnected(vehicle: TeslaVehicle) {
    tesla.vehicleVehicleSecurityConnected.value = true
}

fun Model.teslaVehicleInfotainmentConnected(vehicle: TeslaVehicle) {
    tesla.vehicleInfotainmentConnected.value = true
}

private class ModelTeslaVehicleDelegate(private val model: Model) : TeslaVehicleDelegate {
    override fun teslaVehicleState(vehicle: TeslaVehicle, state: TeslaVehicleState) {
        model.teslaVehicleState(vehicle = vehicle, state = state)
    }

    override fun teslaVehicleVehicleSecurityConnected(vehicle: TeslaVehicle) {
        model.teslaVehicleVehicleSecurityConnected(vehicle = vehicle)
    }

    override fun teslaVehicleInfotainmentConnected(vehicle: TeslaVehicle) {
        model.teslaVehicleInfotainmentConnected(vehicle = vehicle)
    }
}
