package com.moblin.android.view.settings.httpproxy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.isValidPort
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StatusOther
import com.moblin.android.various.network.DefaultTcpPorts
import com.moblin.android.various.settings.SettingsHttpProxy
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.view.utils.UrlsView

@Composable
fun HttpProxySettingsView(
    model: Model,
    status: StatusOther,
    httpProxy: SettingsHttpProxy,
) {
    val port = httpProxy.port.collectAsState().value
    val enabled = httpProxy.enabled.collectAsState().value
    val localNetwork = httpProxy.localNetwork.collectAsState().value

    fun submitPort(value: String) {
        val newPort = value.trim().toUShortOrNull()
        if (newPort == null) {
            model.makePortErrorToast(port = value)
            return
        }
        httpProxy.setPort(newPort)
        model.reloadHttpProxyServer()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(text = "HTTP proxy") })
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Text(
                    text = "The HTTP proxy executes HTTP requests over the network interface " +
                        "that is most likely to have internet connectivity.",
                    modifier = Modifier.padding(16.dp),
                )
            }
            item {
                TextEditNavigationView(
                    title = localized("Port"),
                    value = port.toString(),
                    onChange = { isValidPort(it) },
                    onSubmit = { submitPort(it) },
                    placeholder = DefaultTcpPorts.httpProxy.toString(),
                )
            }
            item {
                Text(
                    text = "This device",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Text(text = "Enabled", modifier = Modifier.weight(1f))
                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            httpProxy.setEnabled(it)
                            model.httpProxyServerChanged()
                        },
                    )
                }
            }
            item {
                Text(
                    text = "Moblin's web browser and browser widgets use the proxy.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item {
                Text(
                    text = "Local network",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Text(text = "Enabled", modifier = Modifier.weight(1f))
                    Switch(
                        checked = localNetwork,
                        onCheckedChange = {
                            httpProxy.setLocalNetwork(it)
                            model.httpProxyServerChanged()
                        },
                    )
                }
            }
            item {
                Text(
                    text = "Allow other devices on the local network to execute HTTP " +
                        "requests through this proxy. Anyone on the local network can use " +
                        "it, so only enable it on networks you trust.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (localNetwork) {
                item {
                    UrlsView(
                        status = status,
                        formatUrl = { "http://$it:$port" },
                    )
                }
                item {
                    Text(
                        text = "Configure one of the URL:s as HTTP proxy in the other device.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}
