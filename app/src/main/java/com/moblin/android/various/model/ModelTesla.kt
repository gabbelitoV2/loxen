package com.moblin.android.various.model

import com.moblin.android.integrations.tesla.TeslaVehicle
import com.moblin.android.integrations.tesla.TeslaVehicleDelegate
import com.moblin.android.integrations.tesla.TeslaVehicleState
import com.moblin.android.localized
import kotlinx.coroutines.flow.MutableStateFlow

class Tesla {
    var vehicle: TeslaVehicle? = null
    var chargeState: Any? = null
    var driveState: Any? = null
    var mediaState: Any? = null
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
        val model = this
        tesla.vehicle?.delegate = object :
            TeslaVehicleDelegate {
            override fun teslaVehicleState(vehicle: TeslaVehicle, state: TeslaVehicleState) {
                model.teslaVehicleState(vehicle, state)
            }

            override fun teslaVehicleVehicleSecurityConnected(vehicle: TeslaVehicle) {
                model.teslaVehicleVehicleSecurityConnected(vehicle)
            }

            override fun teslaVehicleInfotainmentConnected(vehicle: TeslaVehicle) {
                model.teslaVehicleInfotainmentConnected(vehicle)
            }
        }
        tesla.vehicleState.value = TeslaVehicleState.idle
        tesla.vehicle?.start()
    }
}

fun Model.stopTeslaVehicle() {
    tesla.vehicle?.delegate = null
    tesla.vehicle?.stop()
    tesla.vehicle = null
    tesla.vehicleState.value = null
    tesla.chargeState = null
    tesla.driveState = null
    tesla.mediaState = null
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
    TODO("no Android counterpart for CarServer_ChargeState")
}

fun Model.teslaGetDriveState() {
    TODO("no Android counterpart for CarServer_DriveState")
}

fun Model.teslaGetMediaState() {
    TODO("no Android counterpart for CarServer_MediaState")
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
    TODO("no Android counterpart for CarServer_ChargeState")
}

fun Model.textEffectTeslaDrive(): String {
    TODO("no Android counterpart for CarServer_DriveState")
}

fun Model.textEffectTeslaMedia(): String {
    TODO("no Android counterpart for CarServer_MediaState")
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
