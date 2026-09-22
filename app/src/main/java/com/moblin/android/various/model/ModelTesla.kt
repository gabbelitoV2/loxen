package com.moblin.android.various.model

import com.moblin.android.integrations.tesla.TeslaVehicle
import com.moblin.android.integrations.tesla.TeslaVehicleState
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
    if (database.tesla.enabled && database.tesla.vin != "" && database.tesla.privateKey != "") {
        val peripheralId = database.tesla.bluetoothPeripheralId ?: return
        tesla.vehicle = TeslaVehicle(
            vin = database.tesla.vin,
            privateKeyPem = database.tesla.privateKey,
            peripheralId = peripheralId,
        )
        tesla.vehicle?.delegate = this
        tesla.vehicleState.value = TeslaVehicleState.IDLE
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
            CarServer_DriveState.ShiftState.Type.INVALID -> teslaDrive = "-"
            CarServer_DriveState.ShiftState.Type.P -> teslaDrive = "P"
            CarServer_DriveState.ShiftState.Type.R -> teslaDrive = "R"
            CarServer_DriveState.ShiftState.Type.N -> teslaDrive = "N"
            CarServer_DriveState.ShiftState.Type.D -> teslaDrive = "D"
            CarServer_DriveState.ShiftState.Type.SNA -> teslaDrive = "SNA"
        }
        if (teslaDrive != "P") {
            val speed = (tesla.driveState.optionalSpeed as? CarServer_DriveState.OptionalSpeed.Speed)?.value
            if (speed != null) {
                val metersPerSecond = speed * 0.44704
                teslaDrive += " ${format(metersPerSecond)}"
            }
            val power = (tesla.driveState.optionalPower as? CarServer_DriveState.OptionalPower.Power)?.value
            if (power != null) {
                teslaDrive += " $power kW"
            }
        }
    }
    return teslaDrive
}

fun Model.textEffectTeslaMedia(): String {
    var teslaMedia = "-"
    val artist = (tesla.mediaState.optionalNowPlayingArtist
        as? CarServer_MediaState.OptionalNowPlayingArtist.NowPlayingArtist)?.value
    val title = (tesla.mediaState.optionalNowPlayingTitle
        as? CarServer_MediaState.OptionalNowPlayingTitle.NowPlayingTitle)?.value
    if (artist != null && title != null) {
        teslaMedia = if (artist.isEmpty()) {
            title
        } else {
            "$artist - $title"
        }
    }
    return teslaMedia
}

fun Model.teslaVehicleState(vehicle: TeslaVehicle, state: TeslaVehicleState) {
    when (state) {
        TeslaVehicleState.IDLE -> reloadTeslaVehicle()
        TeslaVehicleState.CONNECTING -> {
            tesla.vehicleVehicleSecurityConnected.value = false
            tesla.vehicleInfotainmentConnected.value = false
        }
        TeslaVehicleState.CONNECTED -> makeToast(title = localized("Connected to your Tesla"))
    }
    tesla.vehicleState.value = state
}

fun Model.teslaVehicleVehicleSecurityConnected(vehicle: TeslaVehicle) {
    tesla.vehicleVehicleSecurityConnected.value = true
}

fun Model.teslaVehicleInfotainmentConnected(vehicle: TeslaVehicle) {
    tesla.vehicleInfotainmentConnected.value = true
}
