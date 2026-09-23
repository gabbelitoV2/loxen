package com.moblin.android.various.settings

import androidx.compose.ui.text.font.FontWeight
import com.moblin.android.view.utils.FontDesign
import kotlin.test.assertEquals
import org.junit.Test

class FontDesignOrderTest {
    @Test
    fun settingsFontDesignAndFontDesignHaveTheSameCasesInTheSameOrder() {
        assertEquals(
            SettingsFontDesign.entries.map { it.name.lowercase() },
            FontDesign.entries.map { it.name.lowercase() },
        )
    }

    @Test
    fun toUiKitMapsEveryFontDesign() {
        assertEquals(FontDesign.Default, SettingsFontDesign.`default`.toUiKit())
        assertEquals(FontDesign.Serif, SettingsFontDesign.serif.toUiKit())
        assertEquals(FontDesign.Rounded, SettingsFontDesign.rounded.toUiKit())
        assertEquals(FontDesign.Monospaced, SettingsFontDesign.monospaced.toUiKit())
    }

    @Test
    fun toUiKitMapsEveryFontWeight() {
        assertEquals(FontWeight.Normal, SettingsFontWeight.regular.toUiKit())
        assertEquals(FontWeight.Light, SettingsFontWeight.light.toUiKit())
        assertEquals(FontWeight.Bold, SettingsFontWeight.bold.toUiKit())
    }
}
