package com.moblin.android.platform.swiftui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import com.moblin.android.platform.systemImage

private val symbolsMappedToFallback = setOf("circle")

fun hasSystemImage(name: String): Boolean =
    systemImage(name) !== Icons.Filled.RadioButtonUnchecked || name.removeSuffix(".fill") in symbolsMappedToFallback
