package com.moblin.android.view.settings.display.networkinterfacenames

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.view.utils.TextEditNavigationView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalOverlaysNetworkInterfaceNamesSettingsView(
    model: Model,
    database: Database
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Network interface names") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (database.networkInterfaceNames.isEmpty()) {
                item {
                    Text("No known Ethernet network interfaces.")
                }
            } else {
                items(database.networkInterfaceNames) { interfaceName ->
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
                        IconButton(
                            onClick = {
                                val offsets = makeOffsets(database.networkInterfaceNames, interfaceName.id)
                                if (offsets != null) {
                                    deleteNetworkInterface(model, database, offsets)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null
                            )
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
    offsets: List<Int>
) {
    for (offset in offsets.sortedDescending()) {
        if (offset in database.networkInterfaceNames.indices) {
            database.networkInterfaceNames.removeAt(offset)
        }
    }
    model.networkInterfaceNamesUpdated()
}
