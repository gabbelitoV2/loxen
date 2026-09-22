package com.moblin.android.integrations.gopro

import android.graphics.Bitmap
import com.moblin.android.various.settings.SettingsGoProLaunchLiveStreamResolution
import com.moblin.android.various.utils.generateQrCode

object GoPro {
    fun generateLaunchLiveStream(
        isHero12Or13: Boolean,
        resolution: SettingsGoProLaunchLiveStreamResolution,
    ): Bitmap? {
        val suffix = when (resolution) {
            SettingsGoProLaunchLiveStreamResolution.R1080P -> "!GL"
            SettingsGoProLaunchLiveStreamResolution.R720P -> "!GM"
            SettingsGoProLaunchLiveStreamResolution.R480P -> "!GS"
        }
        return if (isHero12Or13) {
            generateQrCode(suffix)
        } else {
            generateQrCode("oW1mVr1080!W$suffix")
        }
    }

    fun generateWifiCredentialsQrCode(ssid: String, password: String): Bitmap? {
        return generateQrCode("!MJOIN=\"$ssid:$password\"")
    }

    fun generateRtmpUrlQrCode(url: String): Bitmap? {
        return generateQrCode("!MRTMP=\"$url\"")
    }
}
