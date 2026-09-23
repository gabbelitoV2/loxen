package com.moblin.android.view.settings.display.networkinterfacenames

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.view.utils.TextEditNavigationView

@Composable
fun LocalOverlaysNetworkInterfaceNamesSettingsView(
    model: Model = LocalModel.current,
    database: Database
) {
    val palette = formPalette()
    Form(title = "Network interface names") {
        Section {
            if (database.networkInterfaceNames.isEmpty()) {
                Text("No known Ethernet network interfaces.")
            } else {
                database.networkInterfaceNames.forEach { interfaceName ->
                    key(interfaceName.id) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                TextEditNavigationView(
                                    title = interfaceName.interfaceName,
                                    value = interfaceName.name,
                                    onSubmit = { interfaceName.name = it },
                                    capitalize = true
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        val offset = database.networkInterfaceNames
                                            .indexOfFirst { it.id == interfaceName.id }
                                        if (offset != -1) {
                                            deleteNetworkInterface(model, database, offset)
                                        }
                                    }
                            ) {
                                SystemImage(
                                    name = "trash",
                                    fontSize = 20.sp,
                                    tint = palette.red
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun deleteNetworkInterface(
    model: Model,
    database: Database,
    offset: Int
) {
    if (offset in database.networkInterfaceNames.indices) {
        database.networkInterfaceNames.removeAt(offset)
    }
    model.networkInterfaceNamesUpdated()
}
