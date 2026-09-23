package com.moblin.android.view.settings.httpproxy

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.moblin.android.LocalModel
import com.moblin.android.common.various.isValidPort
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StatusOther
import com.moblin.android.various.network.DefaultTcpPorts
import com.moblin.android.various.settings.SettingsHttpProxy
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.view.utils.UrlsView
import com.moblin.android.various.model.httpProxyServerChanged
import com.moblin.android.various.model.reloadHttpProxyServer

@Composable
fun HttpProxySettingsView(
    model: Model = LocalModel.current,
    status: StatusOther,
    httpProxy: SettingsHttpProxy,
) {
    val port by httpProxy.port.collectAsState()
    val enabled by httpProxy.enabled.collectAsState()
    val localNetwork by httpProxy.localNetwork.collectAsState()

    fun submitPort(value: String) {
        val newPort = value.trim().toIntOrNull()
        if (newPort == null || newPort < 0 || newPort > 65535) {
            model.makePortErrorToast(port = value)
            return
        }
        httpProxy.port.value = newPort
        model.reloadHttpProxyServer()
    }

    Form(title = "HTTP proxy") {
        Section {
            Text(
                localized(
                    "The HTTP proxy executes HTTP requests over the network interface that is " +
                        "most likely to have internet connectivity.",
                ),
            )
        }
        Section {
            TextEditNavigationView(
                title = localized("Port"),
                value = port.toString(),
                onChange = { isValidPort(it) },
                onSubmit = { submitPort(it) },
                placeholder = DefaultTcpPorts.httpProxy.toString(),
            )
        }
        Section(
            header = "This device",
            footer = "Moblin's web browser and browser widgets use the proxy.",
        ) {
            Toggle(
                title = "Enabled",
                isOn = enabled,
                onChange = {
                    httpProxy.enabled.value = it
                    model.httpProxyServerChanged()
                },
            )
        }
        Section(
            header = "Local network",
            footer = "Allow other devices on the local network to execute HTTP requests " +
                "through this proxy. Anyone on the local network can use it, so only enable " +
                "it on networks you trust.",
        ) {
            Toggle(
                title = "Enabled",
                isOn = localNetwork,
                onChange = {
                    httpProxy.localNetwork.value = it
                    model.httpProxyServerChanged()
                },
            )
        }
        if (localNetwork) {
            Section(footer = "Configure one of the URL:s as HTTP proxy in the other device.") {
                UrlsView(status = status, formatUrl = { "http://$it:$port" })
            }
        }
    }
}
