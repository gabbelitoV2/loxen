package com.moblin.android.platform.core

import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsMicsMic
import com.moblin.android.various.settings.SettingsScene
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MicIdsSuite {
    private val lower = "01a0ef9a-4139-77e5-b439-168a47f0ae38"
    private val upper = "01A0EF9A-4139-77E5-B439-168A47F0AE38"

    @Test
    fun micIdsSavedInLowerCaseBecomeSwiftsUpperCase() {
        val database = Database()
        database.mics._mics.value = listOf(
            SettingsMicsMic().apply { inputUid = lower },
            SettingsMicsMic().apply { inputUid = "BuiltInMic" },
        )
        database.mics.defaultMic = "$lower 0"
        database.scenes = mutableListOf(SettingsScene().apply { micId = "$lower 0" })
        database.talkback.micId.value = "$lower 0"
        upperCaseMicIds(database)
        assertEquals(listOf(upper, "BuiltInMic"), database.mics.mics.value.map { it.inputUid })
        assertEquals("$upper 0", database.mics.defaultMic)
        assertEquals("$upper 0", database.scenes[0].micId)
        assertEquals("$upper 0", database.talkback.micId.value)
    }
}
