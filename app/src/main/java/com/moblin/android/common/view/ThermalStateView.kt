package com.moblin.android.common.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.core.ProcessInfo
import com.moblin.android.common.various.color

@Composable
fun ThermalStateView(thermalState: ProcessInfo.ThermalState, modifier: Modifier = Modifier) {
    SystemImage(name = "flame", fontSize = 11.sp, modifier = modifier, tint = thermalState.color())
}
