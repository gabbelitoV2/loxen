package com.moblin.android.common.view

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moblin.android.various.storages.ThermalState

@Composable
fun ThermalStateView(thermalState: ThermalState) {
    Icon(
        imageVector = Icons.Default.LocalFireDepartment,
        contentDescription = null,
        modifier = Modifier.size(11.dp),
        tint = thermalState.color()
    )
}

fun ThermalState.color(): Color = when (this) {
    ThermalState.NOMINAL -> Color(0xFF34C759)
    ThermalState.FAIR -> Color(0xFFFFCC00)
    ThermalState.SERIOUS -> Color(0xFFFF9500)
    ThermalState.CRITICAL -> Color(0xFFFF3B30)
    else -> Color(0xFF8E8E93)
}
