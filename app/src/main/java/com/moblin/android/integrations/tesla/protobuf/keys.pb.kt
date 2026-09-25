package com.moblin.android.integrations.tesla.protobuf

enum class Keys_Role(val rawValue: Int) {
    none(0),
    service(1),
    owner(2),
    driver(3),
    fm(4),
    vehicleMonitor(5),
    chargingManager(6),
    guest(8),
    UNRECOGNIZED(-1),
    ;

    companion object {
        val allCases: List<Keys_Role> = listOf(
            none,
            service,
            owner,
            driver,
            fm,
            vehicleMonitor,
            chargingManager,
            guest,
        )

        fun fromRawValue(rawValue: Int): Keys_Role =
            when (rawValue) {
                0 -> none
                1 -> service
                2 -> owner
                3 -> driver
                4 -> fm
                5 -> vehicleMonitor
                6 -> chargingManager
                8 -> guest
                else -> UNRECOGNIZED
            }
    }
}
