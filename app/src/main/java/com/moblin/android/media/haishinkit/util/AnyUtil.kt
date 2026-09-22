package com.moblin.android.media.haishinkit.util

fun isZero(value: Any?): Boolean {
    if (value is Int) {
        return value == 0
    }
    if (value is Double) {
        return value == 0.0
    }
    return false
}
