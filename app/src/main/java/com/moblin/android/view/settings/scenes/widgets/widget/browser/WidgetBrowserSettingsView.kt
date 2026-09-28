package com.moblin.android.view.settings.scenes.widgets.widget.browser

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.common.various.isValidHttpUrl
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.formFootnoteStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetBrowser
import com.moblin.android.various.settings.SettingsWidgetBrowserMode
import com.moblin.android.view.settings.scenes.widgets.widget.effects.WidgetEffectsView
import com.moblin.android.view.utils.InlinePickerItem
import com.moblin.android.view.utils.InlinePickerView
import com.moblin.android.view.utils.MultiLineTextFieldNavigationView
import com.moblin.android.view.utils.SliderView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.various.model.resetSelectedScene

private fun submitUrl(model: Model, browser: SettingsWidgetBrowser, value: String) {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) {
        return
    }
    if (runCatching { java.net.URI(trimmed) }.isFailure) {
        return
    }
    browser.url = trimmed
    model.resetSelectedScene(false)
}

private fun submitStyleSheet(model: Model, browser: SettingsWidgetBrowser, value: String) {
    browser.styleSheet = value.trim()
    model.resetSelectedScene(false)
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
    model.resetSelectedScene(false)
}

private fun submitHeight(model: Model, browser: SettingsWidgetBrowser, value: String) {
    val height = value.toIntOrNull() ?: return
    browser.height = height
    model.resetSelectedScene(false)
}

private fun submitFps(model: Model, browser: SettingsWidgetBrowser, value: Float) {
    browser.baseFps = value
    model.resetSelectedScene(false)
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
    val interactiveBrowsers by model.interactiveBrowsers.collectAsState()

    Section {
        TextEditNavigationView(
            title = "URL",
            value = browser.url,
            onChange = { isValidHttpUrl(it) },
            onSubmit = { submitUrl(model, browser, it) },
        )
        MultiLineTextFieldNavigationView(
            title = localized("Style sheet"),
            placeholder = "body {}",
            value = browser.styleSheet,
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
            value = browser.width.toString(),
            onChange = { changeWidthHeight(it) },
            onSubmit = { submitWidth(model, browser, it) },
            keyboardType = KeyboardType.Number,
        )
        TextEditNavigationView(
            title = localized("Height"),
            value = browser.height.toString(),
            onChange = { changeWidthHeight(it) },
            onSubmit = { submitHeight(model, browser, it) },
            keyboardType = KeyboardType.Number,
        )
    }

    Section {
        Toggle(
            title = "Local only",
            isOn = browser.localOnly,
            onChange = {
                browser.localOnly = it
                model.resetSelectedScene(false)
            },
        )
        if (browser.localOnly) {
            Toggle(
                isOn = interactiveBrowsers,
                onChange = { model.setInteractiveBrowserWidgets(on = it) },
            ) {
                SystemImage("hand.tap", fontSize = 17.sp)
                Text("Interactive browser widgets")
            }
            if (!interactiveBrowsers) {
                FormRow {
                    Text("⚠️ Tap the")
                    SystemImage("hand.tap", fontSize = 17.sp)
                    Text(
                        "Interactive browser widgets quick button to hide/show browsers, " +
                            "or enable the toggle above."
                    )
                }
            }
        }
    }

    if (!browser.localOnly) {
        Section(footerContent = {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    "When \"Audio and video only\" mode is selected, images, text, GIFs etc. " +
                        "will only be shown when a video (.mp4/.mov) is playing, reducing overall " +
                        "energy consumption.",
                    style = formFootnoteStyle,
                )
                Text("", style = formFootnoteStyle)
                Text(
                    "When \"Audio only\" mode is selected, no video will be rendered at all. " +
                        "Only audio will play.",
                    style = formFootnoteStyle,
                )
            }
        }) {
            NavigationLink(destination = {
                InlinePickerView(
                    title = "Mode",
                    onChange = { value ->
                        browser.mode = SettingsWidgetBrowserMode.entries
                            .firstOrNull { it.name == value }
                            ?: SettingsWidgetBrowserMode.periodicAudioAndVideo
                        model.resetSelectedScene(false)
                    },
                    items = SettingsWidgetBrowserMode.entries.map {
                        InlinePickerItem(id = it.name, text = it.toString())
                    },
                    initialSelectedId = browser.mode.name,
                )
            }) {
                Text("Mode")
                Spacer(Modifier.weight(1f))
                Text(browser.mode.toString(), color = formPalette().gray)
            }
            if (browser.mode == SettingsWidgetBrowserMode.periodicAudioAndVideo) {
                FormRow {
                    Text("Base FPS")
                    SliderView(
                        value = browser.baseFps,
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
        }
    }

    Section(footerContent = {
        Text(
            com.moblin.android.localized("Give the webpage access to various data in Moblin, for example chat messages ") +
                "and your location.",
            style = formFootnoteStyle,
        )
    }) {
        Toggle(
            title = "Moblin access",
            isOn = browser.moblinAccess,
            onChange = {
                browser.moblinAccess = it
                model.resetSelectedScene(false)
            },
        )
        if (browser.moblinAccess) {
            Toggle(
                title = "Speech to text",
                isOn = browser.speechToText,
                onChange = {
                    browser.speechToText = it
                    model.resetSelectedScene(false)
                },
            )
        }
    }

    WidgetEffectsView(model = model, widget = widget)
}
