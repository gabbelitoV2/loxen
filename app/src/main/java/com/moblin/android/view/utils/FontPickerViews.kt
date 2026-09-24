package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.SwiftUIFonts
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.settings.SettingsFont
import com.moblin.android.view.settings.streams.stream.GrayTextView
import java.text.BreakIterator

@Composable
private fun FontFamilyPickerView(font: MutableState<SettingsFont>, onChange: () -> Unit) {
    var fontFamilies by remember { mutableStateOf(emptyList<String>()) }
    val palette = formPalette()
    Form(title = "Family") {
        Section {
            FormRow(onClick = {
                font.value = SettingsFont()
                onChange()
            }) {
                Text(localized("System"))
                Spacer(modifier = Modifier.weight(1f))
                if (font.value.family == null) {
                    SystemImage(name = "checkmark", fontSize = 17.sp, tint = palette.accent)
                }
            }
            fontFamilies.forEach { family ->
                key(family) {
                    FormRow(onClick = {
                        font.value = SettingsFont(
                            family = family,
                            style = fontStyles(family).firstOrNull() ?: "",
                        )
                        onChange()
                    }) {
                        Text(text = family, style = SwiftUIFonts.custom(name = family, size = 17f))
                        Spacer(modifier = Modifier.weight(1f))
                        if (font.value.family == family) {
                            SystemImage(name = "checkmark", fontSize = 17.sp, tint = palette.accent)
                        }
                    }
                }
            }
        }
    }
    DisposableEffect(Unit) {
        if (fontFamilies.isEmpty()) {
            fontFamilies = loadFontFamilies()
        }
        onDispose {}
    }
}

private fun loadFontFamilies(): List<String> =
    emptyList()

fun fontStyleName(family: String, fontName: String): String {
    val prefix = family.replace(" ", "")
    val name = fontName.replace("-", "")
    if (name.startsWith(prefix)) {
        val suffix = name.dropFirstGraphemes(prefix.graphemeCount())
        return if (suffix.isEmpty()) "Regular" else suffix
    }
    return fontName
}

private fun String.graphemeCount(): Int {
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(this)
    var count = 0
    var end = iterator.next()
    while (end != BreakIterator.DONE) {
        count++
        end = iterator.next()
    }
    return count
}

private fun String.dropFirstGraphemes(count: Int): String {
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(this)
    var start = iterator.first()
    var index = 0
    var end = iterator.next()
    while (index < count && end != BreakIterator.DONE) {
        start = end
        index++
        end = iterator.next()
    }
    return substring(start)
}

@Composable
private fun FontStylePickerView(
    fontFamily: String,
    font: MutableState<SettingsFont>,
    onChange: () -> Unit,
) {
    val palette = formPalette()
    Form(title = "Style") {
        Section {
            fontStyles(fontFamily).forEach { style ->
                key(style) {
                    FormRow(onClick = {
                        font.value = font.value.copy(style = style)
                        onChange()
                    }) {
                        Text(
                            text = fontStyleName(family = fontFamily, fontName = style),
                            style = SwiftUIFonts.custom(name = style, size = 17f),
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (font.value.style == style) {
                            SystemImage(name = "checkmark", fontSize = 17.sp, tint = palette.accent)
                        }
                    }
                }
            }
        }
    }
}

private fun fontStyles(fontFamily: String): List<String> =
    emptyList()

@Composable
fun FontSettingsView(font: MutableState<SettingsFont>, onChange: () -> Unit) {
    NavigationLink(destination = { FontFamilyPickerView(font = font, onChange = onChange) }) {
        Text(localized("Family"))
        Spacer(modifier = Modifier.weight(1f))
        GrayTextView(text = font.value.familyString())
    }
    val family = font.value.family
    if (family != null) {
        NavigationLink(
            destination = { FontStylePickerView(fontFamily = family, font = font, onChange = onChange) },
            enabled = fontStyles(family).size != 1,
        ) {
            Text(localized("Style"))
            Spacer(modifier = Modifier.weight(1f))
            GrayTextView(text = font.value.styleString())
        }
    }
}
