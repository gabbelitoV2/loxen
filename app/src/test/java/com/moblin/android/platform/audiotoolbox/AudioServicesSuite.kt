package com.moblin.android.platform.audiotoolbox

import android.os.Vibrator
import com.moblin.android.various.utils.UIDevice
import com.moblin.android.various.utils.isPad
import com.moblin.android.various.utils.isPhone
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class AudioServicesSuite {
    private val vibrator: Vibrator
        get() = RuntimeEnvironment.getApplication().getSystemService(Vibrator::class.java)

    @Test
    fun vibrateSoundVibratesLikeTheIphone() {
        assertFalse(shadowOf(vibrator).isVibrating)
        UIDevice.vibrate()
        assertTrue(shadowOf(vibrator).isVibrating)
        assertEquals(400L, shadowOf(vibrator).milliseconds)
    }

    @Test
    fun otherSystemSoundsDoNotVibrate() {
        AudioServicesPlaySystemSound(1000)
        assertFalse(shadowOf(vibrator).isVibrating)
    }

    @Test
    fun aNormalScreenIsAPhone() {
        assertTrue(isPhone())
        assertFalse(isPad())
    }

    @Test
    @Config(qualifiers = "xlarge")
    fun aLargeScreenIsAPad() {
        assertTrue(isPad())
        assertFalse(isPhone())
    }
}
