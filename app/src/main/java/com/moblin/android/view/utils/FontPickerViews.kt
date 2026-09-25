package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.coretext.CTFontCollectionCreateFromAvailableFonts
import com.moblin.android.platform.coretext.CTFontCollectionCreateMatchingFontDescriptors
import com.moblin.android.platform.coretext.CTFontDescriptorCopyAttribute
import com.moblin.android.platform.coretext.kCTFontFamilyNameAttribute
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.SwiftUIFonts
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.platform.uikit.UIFont
import com.moblin.android.various.settings.SettingsFont
import com.moblin.android.view.settings.streams.stream.GrayTextView
import java.text.BreakIterator

@Composable
private fun FontFamilyPickerView(font: MutableState<SettingsFont>, onChange: () -> Unit) {
    var fontFamilies by remember { mutableStateOf(emptyList<String>()) }
    Form(title = "Family") {
        Section {
            FormRow(onClick = {
                font.value = SettingsFont()
                onChange()
            }) {
                Text(localized("System"))
                Spacer(Modifier.weight(1f))
                if (font.value.family == null) {
                    SystemImage(name = "checkmark", fontSize = 17.sp, tint = formPalette().accent)
                }
            }
            ForEach(fontFamilies, id = { it }) { family ->
                FormRow(onClick = {
                    font.value = SettingsFont(
                        family = family,
                        style = UIFont.fontNames(forFamilyName = family).firstOrNull() ?: "",
                    )
                    onChange()
                }) {
                    Text(family, style = SwiftUIFonts.custom(name = family, size = 17f))
                    Spacer(Modifier.weight(1f))
                    if (font.value.family == family) {
                        SystemImage(name = "checkmark", fontSize = 17.sp, tint = formPalette().accent)
                    }
                }
            }
        }
    }
    DisposableEffect(Unit) {
        if (fontFamilies.isEmpty()) {
            fontFamilies = loadFontFamilies()
        }
        onDispose { }
    }
}

private fun loadFontFamilies(): List<String> {
    val families = UIFont.familyNames.toMutableSet()
    val collection = CTFontCollectionCreateFromAvailableFonts(null)
    val descriptors = CTFontCollectionCreateMatchingFontDescriptors(collection)
    if (descriptors != null) {
        for (descriptor in descriptors) {
            val family = CTFontDescriptorCopyAttribute(descriptor, kCTFontFamilyNameAttribute) as? String
            if (family != null) {
                families.add(family)
            }
        }
    }
    return families.sorted()
}

fun fontStyleName(family: String, fontName: String): String {
    val prefix = family.replace(" ", "")
    val name = fontName.replace("-", "")
    if (name.startsWith(prefix)) {
        val suffix = dropFirstGraphemes(name, graphemeCount(prefix))
        return if (suffix.isEmpty()) "Regular" else suffix
    }
    return fontName
}

private fun fontStyles(fontFamily: String): List<String> {
    return UIFont.fontNames(forFamilyName = fontFamily)
}

@Composable
private fun FontStylePickerView(fontFamily: String, font: MutableState<SettingsFont>, onChange: () -> Unit) {
    Form(title = "Style") {
        Section {
            ForEach(fontStyles(fontFamily), id = { it }) { style ->
                FormRow(onClick = {
                    font.value = font.value.copy(style = style)
                    onChange()
                }) {
                    Text(
                        text = fontStyleName(family = fontFamily, fontName = style),
                        style = SwiftUIFonts.custom(name = style, size = 17f),
                    )
                    Spacer(Modifier.weight(1f))
                    if (font.value.style == style) {
                        SystemImage(name = "checkmark", fontSize = 17.sp, tint = formPalette().accent)
                    }
                }
            }
        }
    }
}

@Composable
fun FontSettingsView(font: MutableState<SettingsFont>, onChange: () -> Unit) {
    NavigationLink(destination = {
        FontFamilyPickerView(font = font, onChange = onChange)
    }) {
        Text(localized("Family"))
        Spacer(Modifier.weight(1f))
        GrayTextView(text = font.value.familyString())
    }
    val family = font.value.family
    if (family != null) {
        NavigationLink(
            destination = {
                FontStylePickerView(fontFamily = family, font = font, onChange = onChange)
            },
            enabled = UIFont.fontNames(forFamilyName = family).size != 1,
        ) {
            Text(localized("Style"))
            Spacer(Modifier.weight(1f))
            GrayTextView(text = font.value.styleString())
        }
    }
}

private fun graphemeCount(value: String): Int {
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(value)
    var count = 0
    while (iterator.next() != BreakIterator.DONE) {
        count++
    }
    return count
}

private fun dropFirstGraphemes(value: String, count: Int): String {
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(value)
    var index = 0
    var dropped = 0
    while (dropped < count) {
        val next = iterator.next()
        if (next == BreakIterator.DONE) {
            return ""
        }
        index = next
        dropped++
    }
    return value.substring(index)
}
