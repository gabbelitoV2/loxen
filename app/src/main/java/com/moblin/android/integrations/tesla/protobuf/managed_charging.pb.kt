package com.moblin.android.integrations.tesla.protobuf

enum class ManagedCharging_ChargeOnSolarNoChargeReason(val rawValue: Int) {
    invalid(0),
    powerwallChargePriority(1),
    insufficientSolar(2),
    gridExportPriority(3),
    alternateVehicleChargePriority(4),
    UNRECOGNIZED(-1),
    ;

    companion object {
        val allCases: List<ManagedCharging_ChargeOnSolarNoChargeReason> = listOf(
            invalid,
            powerwallChargePriority,
            insufficientSolar,
            gridExportPriority,
            alternateVehicleChargePriority,
        )

        fun fromRawValue(rawValue: Int): ManagedCharging_ChargeOnSolarNoChargeReason =
            when (rawValue) {
                0 -> invalid
                1 -> powerwallChargePriority
                2 -> insufficientSolar
                3 -> gridExportPriority
                4 -> alternateVehicleChargePriority
                else -> UNRECOGNIZED
            }
    }
}
