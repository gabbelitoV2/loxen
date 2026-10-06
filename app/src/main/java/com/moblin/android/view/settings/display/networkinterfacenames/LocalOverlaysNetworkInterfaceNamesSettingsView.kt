package com.moblin.android.view.settings.display.networkinterfacenames

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.IndexSet
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.remove
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.LocalModel

@Composable
fun LocalOverlaysNetworkInterfaceNamesSettingsView(
    model: Model = LocalModel.current,
    database: Database
) {
    Form(title = "Network interface names") {
        Section {
            if (database.networkInterfaceNames.isEmpty()) {
                Text(localized("No known Ethernet network interfaces."))
            } else {
                ForEach(
                    database.networkInterfaceNames,
                    id = { it.id },
                    onDelete = { deleteNetworkInterface(model, database, it) },
                ) { interfaceName ->
                    ContextMenuDeleteButton(action = {
                        val offset = database.networkInterfaceNames
                            .indexOfFirst { it.id == interfaceName.id }
                        if (offset != -1) {
                            deleteNetworkInterface(model, database, setOf(offset))
                        }
                    }) {
                        TextEditNavigationView(
                            title = interfaceName.interfaceName,
                            value = interfaceName.name,
                            onSubmit = { interfaceName.name = it },
                            capitalize = true
                        )
                    }
                }
            }
        }
    }
}

private fun deleteNetworkInterface(
    model: Model,
    database: Database,
    offsets: IndexSet
) {
    database.networkInterfaceNames.remove(atOffsets = offsets)
    model.networkInterfaceNamesUpdated()
}
