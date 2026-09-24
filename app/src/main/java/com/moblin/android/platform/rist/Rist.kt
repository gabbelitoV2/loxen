package com.moblin.android.platform.rist

typealias rist_profile = Int

const val RIST_PROFILE_SIMPLE: rist_profile = 0
const val RIST_PROFILE_MAIN: rist_profile = 1
const val RIST_PROFILE_ADVANCED: rist_profile = 2

fun ristVersion(): String {
    if (!RistNative.loaded) {
        return ""
    }
    return RistNative.librist_version()
}
