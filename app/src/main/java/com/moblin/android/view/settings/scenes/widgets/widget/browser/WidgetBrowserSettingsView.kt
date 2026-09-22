package com.moblin.android.view.settings.scenes.widgets.widget.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.common.various.isValidHttpUrl
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetBrowser
import com.moblin.android.various.settings.SettingsWidgetBrowserMode
import com.moblin.android.view.settings.scenes.widgets.widget.effects.WidgetEffectsView
import com.moblin.android.view.utils.MultiLineTextFieldNavigationView
import com.moblin.android.view.utils.SliderView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

private fun submitUrl(model: Model, browser: SettingsWidgetBrowser, value: String) {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) {
        return
    }
    if (runCatching { java.net.URI(trimmed) }.isFailure) {
        return
    }
    browser.url = trimmed
    Unit
}

private fun submitStyleSheet(model: Model, browser: SettingsWidgetBrowser, value: String) {
    browser.styleSheet = value.trim()
    Unit
}

private fun changeWidthHeight(value: String): String? {
    val width = value.toIntOrNull() ?: return localized("Not a number")
    if (width <= 0) {
        return localized("Too small")
    }
    if (width >= 4000) {
        return localized("Too big")
    }
    return null
}

private fun submitWidth(model: Model, browser: SettingsWidgetBrowser, value: String) {
    val width = value.toIntOrNull() ?: return
    browser.width = width
    Unit
}

private fun submitHeight(model: Model, browser: SettingsWidgetBrowser, value: String) {
    val height = value.toIntOrNull() ?: return
    browser.height = height
    Unit
}

private fun submitFps(model: Model, browser: SettingsWidgetBrowser, value: Float) {
    browser.baseFps = value
    Unit
}

private fun formatFps(value: Float): String {
    return formatOneDecimal(value.toDouble())
}

@Composable
fun WidgetBrowserSettingsView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    browser: SettingsWidgetBrowser,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val url = browser.url
    val styleSheet = browser.styleSheet
    val width = browser.width
    val height = browser.height
    val baseFps = browser.baseFps
    val mode = browser.mode
    val localOnly = browser.localOnly
    val moblinAccess = browser.moblinAccess
    val speechToText = browser.speechToText
    val interactiveBrowsers by model.interactiveBrowsers.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        TextEditNavigationView(
            title = "URL",
            value = url,
            onChange = { isValidHttpUrl(it) },
            onSubmit = { submitUrl(model, browser, it) },
        )
        MultiLineTextFieldNavigationView(
            title = localized("Style sheet"),
            placeholder = "body {}",
            value = styleSheet,
            onSubmit = { submitStyleSheet(model, browser, it) },
            onValueChange = { browser.styleSheet = it },
            footers = listOf(
                localized("For example:"),
                "",
                "body {background-color: powderblue;}",
                "h1 {color: blue;}",
                "p {color: red;}",
            ),
        )
        TextEditNavigationView(
            title = localized("Width"),
            value = width.toString(),
            onChange = { changeWidthHeight(it) },
            onSubmit = { submitWidth(model, browser, it) },
            keyboardType = KeyboardType.Number,
        )
        TextEditNavigationView(
            title = localized("Height"),
            value = height.toString(),
            onChange = { changeWidthHeight(it) },
            onSubmit = { submitHeight(model, browser, it) },
            keyboardType = KeyboardType.Number,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Local only", modifier = Modifier.weight(1f))
            Switch(
                checked = localOnly,
                onCheckedChange = {
                    browser.localOnly = it
                    Unit
                },
            )
        }
        if (localOnly) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.TouchApp, contentDescription = null)
                Text(" Interactive browser widgets", modifier = Modifier.weight(1f))
                Switch(
                    checked = interactiveBrowsers,
                    onCheckedChange = { model.setInteractiveBrowserWidgets(on = it) },
                )
            }
            if (!interactiveBrowsers) {
                Text(
                    "⚠️ Tap the Interactive browser widgets quick button to hide/show browsers, " +
                        "or enable the toggle above.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (!localOnly) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("InlinePickerView") },
            ) {
                Text("Mode")
                Spacer(Modifier.weight(1f))
                Text(mode.toString(), color = Color.Gray)
            }
            if (mode == SettingsWidgetBrowserMode.periodicAudioAndVideo) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Base FPS")
                    SliderView(
                        value = baseFps,
                        minimum = 1f,
                        maximum = 15f,
                        step = 1f,
                        onSubmit = { submitFps(model, browser, it) },
                        width = 60f,
                        format = { formatFps(it) },
                        onChange = { browser.baseFps = it },
                    )
                }
            }
            Column {
                Text(
                    "When \"Audio and video only\" mode is selected, images, text, GIFs etc. " +
                        "will only be shown when a video (.mp4/.mov) is playing, reducing overall " +
                        "energy consumption.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text("", style = MaterialTheme.typography.bodySmall)
                Text(
                    "When \"Audio only\" mode is selected, no video will be rendered at all. " +
                        "Only audio will play.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Moblin access", modifier = Modifier.weight(1f))
            Switch(
                checked = moblinAccess,
                onCheckedChange = {
                    browser.moblinAccess = it
                    Unit
                },
            )
        }
        if (moblinAccess) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Speech to text", modifier = Modifier.weight(1f))
                Switch(
                    checked = speechToText,
                    onCheckedChange = {
                        browser.speechToText = it
                        Unit
                    },
                )
            }
        }
        Text(
            "Give the webpage access to various data in Moblin, for example chat messages " +
                "and your location.",
            style = MaterialTheme.typography.bodySmall,
        )

        WidgetEffectsView(model = model, widget = widget)
    }
}
