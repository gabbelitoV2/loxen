package com.moblin.android.platform.swiftui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import com.moblin.android.platform.systemImage

private val symbolsMappedToFallback = setOf("circle")

private val symbolsWithFillVariant = setOf(
    "sun.max",
    "sun.min",
    "sunrise",
    "sunset",
    "sun.horizon",
    "sun.dust",
    "sun.haze",
    "sun.rain",
    "sun.snow",
    "sun.max.trianglebadge.exclamationmark",
    "moon",
    "moon.stars",
    "moon.haze",
    "moon.dust",
    "moon.circle",
    "moon.zzz",
    "cloud",
    "cloud.drizzle",
    "cloud.rain",
    "cloud.heavyrain",
    "cloud.fog",
    "cloud.hail",
    "cloud.snow",
    "cloud.sleet",
    "cloud.bolt",
    "cloud.bolt.rain",
    "cloud.sun",
    "cloud.sun.rain",
    "cloud.sun.bolt",
    "cloud.moon",
    "cloud.moon.rain",
    "cloud.moon.bolt",
    "smoke",
    "thermometer.sun",
    "humidity",
    "drop",
    "flame",
    "bolt",
    "umbrella",
    "mic",
    "mic.slash",
    "square",
    "checkmark.square",
    "circle",
    "star",
    "heart",
    "person",
    "house",
    "bell",
    "gearshape",
    "camera",
    "video",
    "speaker",
    "play",
    "pause",
    "stop",
    "record.circle",
    "xmark.circle",
    "checkmark.circle",
    "info.circle",
    "exclamationmark.triangle",
    "flag",
    "bookmark",
    "trash",
    "message",
    "bubble.left",
    "phone",
    "envelope",
    "location",
    "map",
    "car",
    "clock",
    "stopwatch",
    "film",
    "lock",
    "eye",
    "photo",
    "tv",
    "cart",
    "shield",
    "gamecontroller",
    "pawprint",
    "cup.and.saucer",
    "hare",
    "tortoise",
)

private val symbolsWithoutFillVariant = setOf(
    "wind",
    "wind.snow",
    "snowflake",
    "tornado",
    "hurricane",
    "tropicalstorm",
    "thermometer.snowflake",
    "checkmark",
    "xmark",
    "plus",
    "minus",
    "arrow.up",
    "arrow.down",
    "arrow.left",
    "arrow.right",
    "chevron.left",
    "chevron.right",
    "ellipsis",
    "magnifyingglass",
    "dpad.up.fill",
    "dpad.down.fill",
    "dpad.left.fill",
    "dpad.right.fill",
    "face.smiling.inverse",
)

fun hasSystemImage(name: String): Boolean {
    if (name.endsWith(".fill")) {
        val base = name.removeSuffix(".fill")
        if (base in symbolsWithFillVariant) {
            return true
        }
        if (base in symbolsWithoutFillVariant) {
            return false
        }
    } else if (name in symbolsWithFillVariant || name in symbolsWithoutFillVariant) {
        return true
    }
    if (systemImage(name) !== Icons.Filled.RadioButtonUnchecked) {
        return true
    }
    return name.removeSuffix(".fill") in symbolsMappedToFallback
}
