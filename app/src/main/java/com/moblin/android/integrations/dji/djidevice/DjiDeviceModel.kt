package com.moblin.android.integrations.dji.djidevice

import com.moblin.android.various.settings.SettingsDjiDeviceModel

private val djiTechnologyCoLtd = byteArrayOf(0xAA.toByte(), 0x08)
private val xtraLtd = byteArrayOf(0xAA.toByte(), 0xF7.toByte())
private val djiDeviceModelOsmoAction2 = byteArrayOf(0x10, 0x00)
private val djiDeviceModelOsmoAction3 = byteArrayOf(0x12, 0x00)
private val djiDeviceModelOsmoAction4 = byteArrayOf(0x14, 0x00)
private val djiDeviceModelOsmoAction5Pro = byteArrayOf(0x15, 0x00)
private val djiDeviceModelOsmo360 = byteArrayOf(0x17, 0x00)
private val djiDeviceModelOsmoAction6 = byteArrayOf(0x18, 0x00)
private val djiDeviceModelOsmoPocket3 = byteArrayOf(0x20, 0x00)
private val djiDeviceModelOsmoPocket4 = byteArrayOf(0x21, 0x00)
private val djiDeviceModelNone = byteArrayOf(0x00, 0x00)
private val djiProductTypeOsmoPocket4Pro = byteArrayOf(0xDA.toByte(), 0x00)

fun djiModelFromManufacturerData(data: ByteArray): SettingsDjiDeviceModel {
    if (data.size < 4) {
        return SettingsDjiDeviceModel.unknown
    }
    val model = data.copyOfRange(2, 4)
    return when {
        model.contentEquals(djiDeviceModelOsmoAction2) -> SettingsDjiDeviceModel.osmoAction2
        model.contentEquals(djiDeviceModelOsmoAction3) -> SettingsDjiDeviceModel.osmoAction3
        model.contentEquals(djiDeviceModelOsmoAction4) -> SettingsDjiDeviceModel.osmoAction4
        model.contentEquals(djiDeviceModelOsmoPocket3) -> SettingsDjiDeviceModel.osmoPocket3
        model.contentEquals(djiDeviceModelOsmoPocket4) -> SettingsDjiDeviceModel.osmoPocket4
        model.contentEquals(djiDeviceModelOsmoAction5Pro) -> SettingsDjiDeviceModel.osmoAction5Pro
        model.contentEquals(djiDeviceModelOsmo360) -> SettingsDjiDeviceModel.osmo360
        model.contentEquals(djiDeviceModelOsmoAction6) -> SettingsDjiDeviceModel.osmoAction6
        model.contentEquals(djiDeviceModelNone) -> fromNoneDevice(data)
        else -> SettingsDjiDeviceModel.unknown
    }
}

fun isDjiDevice(manufacturerData: ByteArray): Boolean {
    val companyId = manufacturerData.take(2).toByteArray()
    return companyId.contentEquals(djiTechnologyCoLtd) || companyId.contentEquals(xtraLtd)
}

private fun fromNoneDevice(data: ByteArray): SettingsDjiDeviceModel {
    if (data.size < 14) {
        return SettingsDjiDeviceModel.unknown
    }
    val productType = data.copyOfRange(12, 14)
    return when {
        productType.contentEquals(djiProductTypeOsmoPocket4Pro) -> SettingsDjiDeviceModel.osmoPocket4Pro
        else -> SettingsDjiDeviceModel.unknown
    }
}
